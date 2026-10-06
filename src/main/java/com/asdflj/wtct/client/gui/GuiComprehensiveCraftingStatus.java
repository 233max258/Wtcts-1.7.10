package com.asdflj.wtct.client.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;

import com.asdflj.wtct.inventory.InventoryHandler;
import com.asdflj.wtct.inventory.gui.GuiType;
import com.asdflj.wtct.util.Ae2ReflectClient;

import appeng.api.storage.ITerminalHost;

/**
 * The crafting status screen as opened from the comprehensive work terminal.
 *
 * <p>
 * AE2 draws a "back to terminal" tab on this screen, and that tab has to return to the comprehensive
 * terminal. The shared {@link GuiCraftingStatus} cannot work that out by itself: the comprehensive and the
 * wireless dual-interface terminal host their inventory through the same class
 * ({@code WirelessDualInterfaceTerminalInventory}), so the host gives no clue which of the two the screen was
 * opened from. Giving the comprehensive terminal its own {@link GuiType} answers the question up front, with
 * no inspection of the host at all.
 */
public class GuiComprehensiveCraftingStatus extends appeng.client.gui.implementations.GuiCraftingStatus {

    public GuiComprehensiveCraftingStatus(InventoryPlayer inventoryPlayer, ITerminalHost te) {
        super(inventoryPlayer, te);
    }

    @Override
    protected void actionPerformed(final GuiButton btn) {
        // Read the tab fresh from AE2 rather than caching it in initGui: AE2 only creates it once the icon
        // slot has synchronised from the server, which is well after initGui runs.
        if (btn == Ae2ReflectClient.getOriginalGuiButton(this)) {
            InventoryHandler.switchGui(GuiType.COMPREHENSIVE_WORK_TERMINAL);
            return;
        }
        super.actionPerformed(btn);
    }

    @Override
    public void drawScreen(final int mouseX, final int mouseY, final float partialTicks) {
        // Keep the corner tab's transparent margin from landing on the framebuffer as black; see
        // CraftingStatusTabBlend.
        final boolean blendWasOn = CraftingStatusTabBlend.enable();
        try {
            super.drawScreen(mouseX, mouseY, partialTicks);
        } finally {
            CraftingStatusTabBlend.restore(blendWasOn);
        }
    }
}
