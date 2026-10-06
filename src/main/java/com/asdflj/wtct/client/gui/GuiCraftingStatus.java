package com.asdflj.wtct.client.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;

import com.asdflj.wtct.inventory.InventoryHandler;
import com.asdflj.wtct.inventory.gui.GuiType;
import com.asdflj.wtct.inventory.item.WirelessDualInterfaceTerminalInventory;
import com.asdflj.wtct.util.Ae2ReflectClient;

import appeng.api.storage.ITerminalHost;

public class GuiCraftingStatus extends appeng.client.gui.implementations.GuiCraftingStatus {

    private final ITerminalHost host;

    public GuiCraftingStatus(InventoryPlayer inventoryPlayer, ITerminalHost te) {
        super(inventoryPlayer, te);
        host = te;
    }

    @Override
    protected void actionPerformed(final GuiButton btn) {
        // The tab is created by AE2 through IGuiSub#initPrimaryGuiButton once the icon slot synchronises from
        // the server, which can be well after initGui - so the button reference has to be read fresh instead
        // of being cached during initGui. Reading AE2's own field also keeps this check in sync with
        // super#actionPerformed, which would otherwise handle the click by sending AE2's generic switch packet,
        // whose PrimaryGui cannot address an item hosted terminal living in a Baubles slot.
        if (btn == Ae2ReflectClient.getOriginalGuiButton(this)) {
            if (host instanceof WirelessDualInterfaceTerminalInventory) {
                // That inventory is the comprehensive work terminal's own host.
                InventoryHandler.switchGui(GuiType.COMPREHENSIVE_WORK_TERMINAL);
            }
        } else {
            super.actionPerformed(btn);
        }
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
