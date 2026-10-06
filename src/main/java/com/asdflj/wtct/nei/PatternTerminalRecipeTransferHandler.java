package com.asdflj.wtct.nei;

import static com.asdflj.wtct.proxy.ClientProxy.mouseHandlers;
import static net.minecraft.client.gui.GuiScreen.isCtrlKeyDown;
import static net.minecraft.client.gui.GuiScreen.isShiftKeyDown;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.asdflj.wtct.Wtct;
import com.asdflj.wtct.api.Constants;
import com.asdflj.wtct.client.gui.GuiComprehensiveWorkTerminal;
import com.asdflj.wtct.client.gui.container.ContainerComprehensiveWorkTerminal;
import com.asdflj.wtct.integration.Mods;
import com.asdflj.wtct.nei.object.OrderStack;
import com.asdflj.wtct.nei.recipes.FluidRecipe;
import com.asdflj.wtct.network.CPacketNEIRecipe;
import com.asdflj.wtct.network.CPacketTransferRecipe;
import com.asdflj.wtct.proxy.ClientProxy;
import com.asdflj.wtct.util.PHUtil;
import com.asdflj.wtct.util.PatternScaling;

import appeng.client.gui.AEBaseGui;
import appeng.container.slot.SlotFake;
import appeng.util.Platform;
import codechicken.nei.PositionedStack;
import codechicken.nei.api.IOverlayHandler;
import codechicken.nei.recipe.GuiOverlayButton;
import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.TemplateRecipeHandler;

public class PatternTerminalRecipeTransferHandler implements IOverlayHandler {

    private static final Logger WTCT_NEI_LOG = LogManager.getLogger("Wtct|NEIOverlay");

    public static final PatternTerminalRecipeTransferHandler INSTANCE = new PatternTerminalRecipeTransferHandler();

    public static final HashSet<String> notOtherSet = new HashSet<>();
    public static final HashSet<String> craftSet = new HashSet<>();

    static {
        notOtherSet.add("smelting");
        notOtherSet.add("brewing");
        craftSet.add("crafting");
        craftSet.add("crafting2x2");
    }

    private static ItemStack findSameItem(ItemStack[] items, ItemStack item, Constants.MouseWheel wheel) {
        for (int i = 0; i < items.length; i++) {
            if (sameIngredient(item, items[i])) {
                int index = i + wheel.direction;
                return items[index < 0 ? items.length - 1 : index % items.length];
            }
        }
        return null;
    }

    /**
     * True when the cell holds the ingredient the recipe offers here, or another carrier of the same
     * fluid: a fluid cell is held as the fluid packet while the recipe offers GregTech's display item
     * for that fluid, and without this the wheel would find nothing to cycle on such a cell.
     */
    private static boolean sameIngredient(final ItemStack cell, final ItemStack offered) {
        if (Platform.isSameItemPrecise(cell, offered)) {
            return true;
        }
        final FluidStack held = PatternScaling.fluidCarriedBy(cell);
        final FluidStack wanted = PatternScaling.fluidCarriedBy(offered);
        return held != null && wanted != null && held.isFluidEqual(wanted);
    }

    public PatternTerminalRecipeTransferHandler() {
        mouseHandlers.add((event, overlayButton) -> {
            GuiScreen screen = Minecraft.getMinecraft().currentScreen;
            if (screen instanceof AEBaseGui g && overlayButton != null && GuiScreen.isShiftKeyDown()) {
                GuiOverlayButton btn = ClientProxy.getOverlayButton();
                if (btn != null && g.theSlot instanceof SlotFake slot) {
                    ItemStack slotItem = slot.getStack();
                    if (slotItem == null) return false;

                    List<PositionedStack> list = btn.handlerRef.handler.getIngredientStacks(btn.handlerRef.recipeIndex);
                    for (PositionedStack stack : list) {
                        ItemStack result = findSameItem(
                            stack.items,
                            slotItem,
                            event.scrollAmount == -1 ? Constants.MouseWheel.NEXT : Constants.MouseWheel.PREVIEW);
                        if (result != null) {
                            List<OrderStack<?>> in = new ArrayList<>();
                            List<OrderStack<?>> out = new ArrayList<>();
                            in.add(new OrderStack<>(slotItem, 0));
                            // Cycling a fluid keeps the number the cell was asking for; the picker
                            // offers the recipe's own display item, so the cell is built here, on the
                            // screen, where a fluid can be read out of that display item.
                            out.add(
                                new OrderStack<>(
                                    ContainerComprehensiveWorkTerminal.cellKeepingAmount(slotItem, result),
                                    0));
                            Wtct.proxy.netHandler.sendToServer(
                                new CPacketTransferRecipe(
                                    in,
                                    out,
                                    shouldCraft(btn.handlerRef.handler),
                                    isShiftKeyDown(),
                                    Constants.NEI_MOUSE_WHEEL));
                            return true;
                        }
                    }
                }
            }
            return false;
        });
    }

    @Override
    public void overlayRecipe(GuiContainer firstGui, IRecipeHandler recipe, int recipeIndex, boolean shift) {
        WTCT_NEI_LOG.info(
            "[Wtct] NEI overlayRecipe: gui={} recipeHandler={} recipe={} shift={}",
            firstGui == null ? "null"
                : firstGui.getClass()
                    .getName(),
            recipe == null ? "null"
                : recipe.getClass()
                    .getName(),
            recipe instanceof TemplateRecipeHandler t ? t.getOverlayIdentifier() : "n/a",
            shift);
        // Recipes without a dedicated extractor are handled through FluidRecipe's generic fallback, which requires
        // a TemplateRecipeHandler.
        if (!(recipe instanceof TemplateRecipeHandler)) {
            return;
        }
        if (firstGui instanceof GuiComprehensiveWorkTerminal gui) {
            boolean craft = shouldCraft(recipe);
            // WCWT's hammer: Shift+click on a crafting recipe pulls the materials out of the
            // network into this terminal's own manual crafting grid, and the terminal stays open -
            // the grid lives here, so there is no other screen to return to. The + (a plain click)
            // stays the encode button. Ctrl+Shift additionally asks the terminal to watch the
            // cells it could not fill and drop the item in once it shows up (ae2helpers' watcher).
            if (isShiftKeyDown() && craft) {
                try {
                    Wtct.proxy.netHandler.sendToServer(
                        new CPacketNEIRecipe(
                            CraftingTransferHandler.packIngredients(firstGui, ingredients(recipe, recipeIndex)),
                            isCtrlKeyDown()));
                } catch (final Exception ignored) {
                    // NO-OP - a malformed overlay must never break the screen.
                }
                return;
            }
            // The comprehensive work terminal encodes into CRAFTING_EX / OUTPUT_EX exactly like the
            // dual interface terminal - it just has no interface list to search, so the recipe is
            // only written into the encoding area.
            // WCWT/Plus record the recipe type here and the terminal shows it at once; the encode key
            // later turns it into the provider search term through the mapping table. NEI's own
            // recipe name is passed along as the localised fallback for types the table does not know.
            ((GuiComprehensiveWorkTerminal) firstGui).noteTransferredRecipe(
                craft,
                recipe instanceof TemplateRecipeHandler template ? template.getOverlayIdentifier() : null,
                recipe.getRecipeName());
            List<OrderStack<?>> in = FluidRecipe.getPackageInputs(recipe, recipeIndex, false);
            // Programming toolbox (programmablehatches): a processing pattern must carry its programming
            // circuit the same way the dual interface terminal encodes it, so the circuit slot is filled
            // with proghatches' circuit item instead of the raw recipe ingredient.
            if (Mods.PROGRAMMABLE_HATCHES.isModLoaded() && !craft) {
                in = PHUtil.transfer(in);
            }
            List<OrderStack<?>> out = FluidRecipe.getPackageOutputs(recipe, recipeIndex, !notUseOther(recipe));
            Wtct.proxy.netHandler.sendToServer(new CPacketTransferRecipe(in, out, craft, shift));
        }
    }

    private static List<PositionedStack> ingredients(IRecipeHandler recipe, int recipeIndex) {
        return recipe.getIngredientStacks(recipeIndex);
    }

    private boolean notUseOther(IRecipeHandler recipeHandler) {
        if (!(recipeHandler instanceof TemplateRecipeHandler tRecipe)) {
            return false;
        }
        return notOtherSet.contains(tRecipe.getOverlayIdentifier());
    }

    private boolean shouldCraft(IRecipeHandler recipeHandler) {
        if (!(recipeHandler instanceof TemplateRecipeHandler tRecipe)) {
            return false;
        }
        return craftSet.contains(tRecipe.getOverlayIdentifier());
    }

}
