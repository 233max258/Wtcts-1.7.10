package com.asdflj.wtct.nei;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import com.asdflj.wtct.Wtct;
import com.asdflj.wtct.client.gui.GuiComprehensiveWorkTerminal;
import com.asdflj.wtct.client.me.AdvItemRepo;
import com.asdflj.wtct.network.CPacketNEIRecipe;

import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import codechicken.lib.gui.GuiDraw;
import codechicken.nei.ItemsTooltipLineHandler;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.GuiOverlayButton;
import codechicken.nei.recipe.RecipeHandlerRef;

/**
 * WCWT's hammer as a real button next to NEI's "+": the "+" encodes the recipe into the encoding
 * area, the hammer pulls the recipe's materials out of the network into the terminal's own manual
 * crafting grid and returns to the terminal. Ctrl+click additionally has the network craft what the
 * pull could not fill; the cells themselves are left as they are (no auto-fill watch for now).
 *
 * <p>
 * The button extends {@link GuiOverlayButton} because that is what NEI's recipe widget drives: in
 * widget mode only an overlay button's {@code drawItemOverlay} runs, and only the first overlay
 * button's. Being that button is what gets the WCWT's ingredient tints drawn at all: green for
 * stored, blue for "missing but craftable" (the terminal's own overlay state), red for missing.
 */
public class HammerRecipeButton extends GuiOverlayButton {

    private static final ResourceLocation ICON = new ResourceLocation(
        Wtct.MODID,
        "textures/guis/wcwt/craft_hammer.png");
    private static final int ICON_W = 10;
    private static final int ICON_H = 11;
    /** The preview is rebuilt a few times a second, so inventory/network changes show up live. */
    private static final long PREVIEW_TTL_MS = 250;

    private Preview preview;
    private long previewAt;
    /** Ctrl captured when the press began - the classic panel delivers the click on mouse-up. */
    private boolean ctrlAtPress;

    /**
     * Which ingredients the hammer would leave behind: the cells the network and the inventory cannot
     * cover whole, split into missing / missing-but-craftable, plus how much of each is short - the
     * deficit per item, which is what NEI's "missing" line is handed.
     */
    private static final class Preview {

        final Set<Integer> missing = new HashSet<>();
        final Set<Integer> craftable = new HashSet<>();
        final List<ItemStack> missingItems = new ArrayList<>();
    }

    public HammerRecipeButton(final GuiContainer firstGui, final RecipeHandlerRef handlerRef, final int x,
        final int y) {
        super(firstGui, handlerRef, x, y);
    }

    /** True when this button applies: the terminal grid with a crafting recipe on display. */
    public static boolean appliesTo(final GuiOverlayButton overlay) {
        return overlay.firstGui instanceof GuiComprehensiveWorkTerminal && isCraftingRecipe(overlay.handlerRef);
    }

    public static boolean isCraftingRecipe(final RecipeHandlerRef handlerRef) {
        if (handlerRef == null || !(handlerRef.handler instanceof codechicken.nei.recipe.TemplateRecipeHandler t)) {
            return false;
        }
        return PatternTerminalRecipeTransferHandler.craftSet.contains(t.getOverlayIdentifier());
    }

    /**
     * The client-side mirror of the server pull: for every ingredient, is it in the player's
     * inventory or stored in the network (filled), or missing - and if missing, does the network
     * have a pattern for it (craftable)? One per crafting cell, the same the server will pull.
     *
     * <p>
     * Three mutually exclusive states, WCWT's pull-overlay semantics: filled = no tint, missing but
     * craftable = blue, missing with nothing to make it = red. "Stored" means the network holds at
     * least one - a craftable-only entry has none, which is exactly the blue case.
     */
    private Preview preview() {
        final long now = System.currentTimeMillis();
        if (this.preview != null && now - this.previewAt < PREVIEW_TTL_MS) {
            return this.preview;
        }
        final Preview p = new Preview();
        this.preview = p;
        this.previewAt = now;
        if (!(this.firstGui instanceof GuiComprehensiveWorkTerminal gui)) {
            return p;
        }
        final List<PositionedStack> ingredients = this.handlerRef.handler
            .getIngredientStacks(this.handlerRef.recipeIndex);
        final ItemStack[] playerInv = Minecraft.getMinecraft().thePlayer.inventory.mainInventory;
        final AdvItemRepo repo = gui.getRepo();
        final Set<Integer> craftableKeys = gui.getCraftableKeys();
        // Stock, spent cell by cell like NEI spends the inventory: two cells of the same ingredient
        // cannot both claim the same stored stack.
        final Map<Integer, Long> stock = new HashMap<>();
        for (int index = 0; index < ingredients.size(); index++) {
            final PositionedStack stack = ingredients.get(index);
            if (stack == null || stack.items == null || stack.items.length == 0) {
                continue;
            }
            final int needed = Math.max(1, stack.items[0].stackSize);
            final int key = GuiComprehensiveWorkTerminal.craftableKey(stack.items[0]);
            final long available = stock.computeIfAbsent(key, ignored -> available(stack, playerInv, repo));
            final long used = Math.min(needed, available);
            final int shortBy = (int) (needed - used);
            stock.put(key, available - used);
            if (shortBy <= 0) {
                continue;
            }
            if (isCraftable(stack, craftableKeys, repo)) {
                p.craftable.add(index);
            } else {
                p.missing.add(index);
            }
            // Whatever the inventory and the network cannot cover is what the pull leaves behind - and
            // how much of it, not just that it is short: NEI aggregates these by item, so a cell that
            // wants 8 and finds 3 shows the 5 the player is actually out.
            p.missingItems.add(deficit(stack.items[0], shortBy));
        }
        return p;
    }

    /** How many of the ingredient the player's inventory and the network hold together. */
    private static long available(final PositionedStack stack, final ItemStack[] playerInv, final AdvItemRepo repo) {
        long count = 0;
        for (final ItemStack invStack : playerInv) {
            if (invStack != null && stack.contains(invStack)) {
                count += invStack.stackSize;
            }
        }
        if (repo == null) {
            return count;
        }
        for (final IAEStack<?> entry : repo.getAllStacks()) {
            if (entry instanceof IAEItemStack ais && ais.getStackSize() > 0 && matchesAny(stack, ais)) {
                count += ais.getStackSize();
            }
        }
        return count;
    }

    /** One copy of the ingredient carrying only the amount that is short, for NEI's missing line. */
    private static ItemStack deficit(final ItemStack stack, final int amount) {
        final ItemStack copy = stack.copy();
        copy.stackSize = amount;
        return copy;
    }

    /** True when the network has a pattern for the ingredient, so it can be made on demand. */
    private static boolean isCraftable(final PositionedStack stack, final Set<Integer> craftableKeys,
        final AdvItemRepo repo) {
        for (final ItemStack variant : stack.items) {
            if (craftableKeys.contains(GuiComprehensiveWorkTerminal.craftableKey(variant))) {
                return true;
            }
        }
        if (repo == null) {
            return false;
        }
        for (final IAEStack<?> entry : repo.getAllStacks()) {
            if (entry instanceof IAEItemStack ais && ais.isCraftable() && matchesAny(stack, ais)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesAny(final PositionedStack stack, final IAEItemStack entry) {
        for (final ItemStack variant : stack.items) {
            if (entry.isSameType(variant)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<String> handleTooltip(final List<String> currenttip) {
        currenttip.add(StatCollector.translateToLocal("wtct.nei.hammer.tooltip"));
        final Preview p = preview();
        if (!p.craftable.isEmpty()) {
            final String key = GuiScreen.isCtrlKeyDown() ? "wtct.nei.hammer.will_craft" : "wtct.nei.hammer.ctrl";
            currenttip.add(EnumChatFormatting.BLUE + StatCollector.translateToLocal(key));
        }
        if (!p.missingItems.isEmpty()) {
            currenttip.add(EnumChatFormatting.RED + StatCollector.translateToLocal("wtct.nei.hammer.missing"));
            // NEI's own missing-materials line: the stacks drawn right in the tooltip, counts and all.
            currenttip.add(
                GuiDraw.TOOLTIP_HANDLER
                    + GuiDraw.getTipLineId(new ItemsTooltipLineHandler("", p.missingItems, true, Integer.MAX_VALUE)));
        }
        return currenttip;
    }

    @Override
    public Map<String, String> handleHotkeys(final int mousex, final int mousey, final Map<String, String> hotkeys) {
        hotkeys.put(
            codechicken.nei.util.NEIMouseUtils.getKeyName(codechicken.nei.util.NEIMouseUtils.MOUSE_BTN_LMB),
            StatCollector.translateToLocal("wtct.nei.hammer.tooltip"));
        hotkeys.put(
            codechicken.nei.NEIClientUtils
                .getKeyName(codechicken.nei.NEIClientUtils.CTRL_HASH, codechicken.nei.util.NEIMouseUtils.MOUSE_BTN_LMB),
            StatCollector.translateToLocal("wtct.nei.hammer.ctrl"));
        return hotkeys;
    }

    @Override
    protected void drawContent(final Minecraft minecraft, final int y, final int x, final boolean mouseOver) {
        // The hammer icon instead of the inherited "+": the icon is a standalone texture, so it is
        // blitted with 0..1 UVs - drawTexturedModalRect would divide by 256 and sample a sliver.
        GL11.glColor4f(1.0F, 1.0F, 1.0F, this.enabled ? 1.0F : 0.5F);
        minecraft.renderEngine.bindTexture(ICON);
        final int iconX = this.xPosition + (this.width - ICON_W) / 2;
        final int iconY = this.yPosition + (this.height - ICON_H) / 2;
        final Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();
        tess.addVertexWithUV(iconX, iconY + ICON_H, 0.0D, 0.0D, 1.0D);
        tess.addVertexWithUV(iconX + ICON_W, iconY + ICON_H, 0.0D, 1.0D, 1.0D);
        tess.addVertexWithUV(iconX + ICON_W, iconY, 0.0D, 1.0D, 0.0D);
        tess.addVertexWithUV(iconX, iconY, 0.0D, 0.0D, 0.0D);
        tess.draw();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    public boolean mousePressed(final Minecraft mc, final int mouseX, final int mouseY) {
        // The classic recipe panel delivers the click on mouse-up, so a fast click that lets go of
        // Ctrl before the button reads it must still count - capture the modifier at press.
        this.ctrlAtPress = GuiScreen.isCtrlKeyDown();
        return super.mousePressed(mc, mouseX, mouseY);
    }

    @Override
    public void mouseReleased(final int mouseX, final int mouseY) {
        // NOT calling super: the inherited release is the "+" overlay - ours is the hammer pull.
        if (this.firstGui == null) {
            return;
        }
        final boolean ctrl = this.ctrlAtPress || GuiScreen.isCtrlKeyDown();
        // Back to the terminal first, like NEI's own overlay button does - the server still holds
        // the terminal's container, and the screen the player looks at becomes the terminal again.
        this.firstGui.mc.displayGuiScreen(this.firstGui);
        final List<PositionedStack> ingredients = this.handlerRef.handler
            .getIngredientStacks(this.handlerRef.recipeIndex);
        try {
            final net.minecraft.nbt.NBTTagCompound tag = CraftingTransferHandler
                .packIngredients(this.firstGui, ingredients);
            Wtct.proxy.netHandler.sendToServer(new CPacketNEIRecipe(tag, ctrl));
        } catch (final Exception ignored) {
            // NO-OP - a malformed overlay must never break the screen.
        }
    }
}
