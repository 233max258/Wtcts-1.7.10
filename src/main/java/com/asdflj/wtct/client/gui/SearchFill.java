package com.asdflj.wtct.client.gui;

import java.awt.Point;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import com.asdflj.wtct.integration.Mods;

import appeng.api.storage.data.IAEStack;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.slots.VirtualMESlot;
import codechicken.nei.ItemPanels;
import codechicken.nei.guihook.GuiContainerManager;

/**
 * The "F fills the search box" gesture, ported from AE2 Auto Pattern Upload: whatever the cursor is
 * over, its display name goes into the terminal's search box with one key press.
 *
 * <p>
 * That mod (1.12.2) asks JEI for the hovered ingredient first and falls back to the container slot.
 * 1.7.10's recipe viewer is NEI, so the lookup order here is NEI's own - its {@code ItemZoom} reads
 * the panels in exactly this order: item panel, bookmark panel, item history, and then whatever NEI's
 * container manager reports (NEI's overlay handlers before the slot itself).
 *
 * <p>
 * One source is added on top of that: AE2's ME item grid is made of virtual slots, which are not
 * container slots and are therefore invisible to NEI. {@link AEBaseGui} tracks the virtual slot the
 * cursor is over, and that is read here too, so hovering a stack in the terminal's own grid behaves
 * like hovering it in NEI.
 *
 * <p>
 * Only the name lookup lives here. Every terminal applies it through its own search box - each one
 * has its own field and its own way of refreshing the list - so this class deliberately knows nothing
 * about either and just answers "what is under the cursor".
 */
public final class SearchFill {

    private SearchFill() {}

    /**
     * Display name of the stack under the cursor, or {@code null} when there is nothing to name.
     *
     * <p>
     * Meant to be called from a GUI's {@code keyTyped} for the F key: {@code null} back means "no
     * gesture here", so the key falls through and types into the search box as usual.
     */
    public static String hoveredName(final GuiContainer gui) {
        if (gui == null) {
            return null;
        }
        if (Mods.NOT_ENOUGH_ITEMS.isModLoaded()) {
            final String fromPanel = neiPanelName(mousePosition());
            if (fromPanel != null) {
                return fromPanel;
            }
        }
        final String fromMeSlot = meSlotName(gui);
        if (fromMeSlot != null) {
            return fromMeSlot;
        }
        if (Mods.NOT_ENOUGH_ITEMS.isModLoaded()) {
            // NEI's own order: object handlers (its overlays and the recipe view) first, then the
            // container slot under the cursor.
            return name(GuiContainerManager.getStackMouseOver(gui));
        }
        return null;
    }

    /** Searchable name of an item stack, or {@code null}. */
    public static String name(final ItemStack stack) {
        return stack == null ? null : clean(stack.getDisplayName());
    }

    /**
     * True while a modifier is held. The gesture leaves those combinations alone, so shift/ctrl/alt
     * typing (and any mod that binds one of them) keeps working.
     */
    public static boolean modifierDown() {
        return Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)
            || Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)
            || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)
            || Keyboard.isKeyDown(Keyboard.KEY_LMENU)
            || Keyboard.isKeyDown(Keyboard.KEY_RMENU);
    }

    /** Whatever NEI's item panel, bookmark panel or item history has under the cursor. */
    private static String neiPanelName(final Point mouse) {
        ItemStack stack = ItemPanels.itemPanel.getStackMouseOver(mouse.x, mouse.y);
        if (stack == null) {
            stack = ItemPanels.bookmarkPanel.getStackMouseOver(mouse.x, mouse.y);
        }
        if (stack == null) {
            stack = ItemPanels.itemPanel.historyPanel.getStackMouseOver(mouse.x, mouse.y);
        }
        return name(stack);
    }

    /**
     * The virtual ME slot under the cursor - the terminal's own grid, the pinned rows, a pattern
     * terminal's ghost slots. {@code AEBaseGui} clears its hovered slot before every pass over them,
     * so a non-null slot here is the slot under the cursor right now, not the last one that was.
     */
    private static String meSlotName(final GuiContainer gui) {
        if (!(gui instanceof AEBaseGui base)) {
            return null;
        }
        final VirtualMESlot slot = base.getVirtualMESlotUnderMouse();
        if (slot == null || slot.isHidden()) {
            return null;
        }
        final IAEStack<?> stack = slot.getAEStack();
        return stack == null ? null : clean(stack.getDisplayName());
    }

    /**
     * Display name, minus what the terminal's search cannot use: AE2's own {@code §} colour codes, and
     * the whitespace around a name that a texture or a translation left behind.
     */
    private static String clean(final String displayName) {
        if (displayName == null) {
            return null;
        }
        final String cleaned = displayName.replaceAll("§[0-9a-fk-or]", "")
            .trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    /**
     * Cursor position in the coordinates the NEI panels use - the same conversion NEI's own
     * {@code GuiDraw.getMousePosition()} performs (note the {@code - 1}: the panels count from the
     * bottom-left of the screen).
     */
    private static Point mousePosition() {
        final Minecraft mc = Minecraft.getMinecraft();
        final ScaledResolution scaled = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        return new Point(
            Mouse.getX() * scaled.getScaledWidth() / mc.displayWidth,
            scaled.getScaledHeight() - Mouse.getY() * scaled.getScaledHeight() / mc.displayHeight - 1);
    }
}
