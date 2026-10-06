package com.asdflj.wtct.client.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;

import com.asdflj.wtct.Wtct;
import com.asdflj.wtct.client.gui.container.ContainerPatternValueAmount;
import com.asdflj.wtct.inventory.gui.GuiType;
import com.asdflj.wtct.inventory.item.WirelessDualInterfaceTerminalInventory;
import com.asdflj.wtct.loader.ItemAndBlockHolder;
import com.asdflj.wtct.network.CPacketPatternValueSet;

import appeng.api.storage.ITerminalHost;
import appeng.core.localization.GuiText;

public class GuiPatternValueAmount extends GuiAmount {

    public GuiPatternValueAmount(InventoryPlayer inventoryPlayer, ITerminalHost te) {
        super(new ContainerPatternValueAmount(inventoryPlayer, te));
    }

    @Override
    protected void actionPerformed(final GuiButton btn) {
        super.actionPerformed(btn);
        try {
            if (btn == this.submit && btn.enabled) {
                Wtct.proxy.netHandler.sendToServer(
                    new CPacketPatternValueSet(
                        originalGui.ordinal(),
                        getAmount(),
                        ((ContainerPatternValueAmount) this.inventorySlots).getValueIndex()));
            }
        } catch (final NumberFormatException e) {
            this.amountBox.setText("1");
        }
    }

    protected void setOriginGUI(Object target) {
        if (target instanceof WirelessDualInterfaceTerminalInventory) {
            // See GuiCraftAmount: the dual interface inventory only ever belongs to the comprehensive terminal now.
            this.myIcon = ItemAndBlockHolder.ITEM_COMPREHENSIVE_WORK_TERMINAL.stack();
            this.originalGui = GuiType.COMPREHENSIVE_WORK_TERMINAL;
        }
    }

    @Override
    public void initGui() {
        super.initGui();
        this.submit.displayString = GuiText.Set.getLocal();
    }

    @Override
    public void drawFG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        this.fontRendererObj.drawString(GuiText.SelectAmount.getLocal(), 8, 6, 0x404040);
    }

    @Override
    public void drawBG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        super.drawBG(offsetX, offsetY, mouseX, mouseY);
        try {
            int result = getAmount();
            this.submit.enabled = result > 0;
        } catch (final NumberFormatException e) {
            this.submit.enabled = false;
        }
        this.amountBox.drawTextBox();
    }

    protected String getBackground() {
        return "guis/craftAmt.png";
    }

    @Override
    public void setAmount(int amount) {
        this.amountBox.setText(String.valueOf(amount));
        this.amountBox.setCursorPositionEnd();
        this.amountBox.setSelectionPos(0);
    }
}
