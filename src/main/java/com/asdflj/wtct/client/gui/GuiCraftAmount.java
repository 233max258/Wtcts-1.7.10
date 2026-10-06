package com.asdflj.wtct.client.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;

import org.lwjgl.input.Mouse;

import com.asdflj.wtct.Wtct;
import com.asdflj.wtct.inventory.gui.GuiType;
import com.asdflj.wtct.inventory.item.WirelessDualInterfaceTerminalInventory;
import com.asdflj.wtct.loader.ItemAndBlockHolder;
import com.asdflj.wtct.network.CPacketCraftRequest;

import appeng.api.config.CraftingMode;
import appeng.api.config.Settings;
import appeng.api.storage.ITerminalHost;
import appeng.api.storage.StorageName;
import appeng.api.storage.data.IAEStack;
import appeng.client.gui.slots.VirtualMESlotSingle;
import appeng.client.gui.widgets.GuiImgButton;
import appeng.container.implementations.ContainerCraftAmount;
import appeng.container.interfaces.IVirtualSlotHolder;
import appeng.core.localization.GuiText;
import appeng.util.Platform;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

public class GuiCraftAmount extends GuiAmount implements IVirtualSlotHolder {

    // The virtual slot that renders the item/fluid about to be crafted. The server
    // sends its contents through PacketVirtualSlot, which is received by
    // receiveSlotStacks below (same mechanism as the vanilla AE2 GuiCraftAmount).
    private final VirtualMESlotSingle slot = new VirtualMESlotSingle(34, 53, 0, null);

    protected GuiImgButton craftingMode;

    public GuiCraftAmount(final InventoryPlayer inventoryPlayer, final ITerminalHost te) {
        super(new ContainerCraftAmount(inventoryPlayer, te));
    }

    @Override
    public void initGui() {
        super.initGui();
        this.registerVirtualSlots(this.slot);
        this.buttonList.add(
            this.craftingMode = new GuiImgButton(
                this.guiLeft + 10,
                this.guiTop + 53,
                Settings.CRAFTING_MODE,
                CraftingMode.STANDARD));
        this.amountBox.setText("1");
        this.amountBox.setCursorPositionEnd();
    }

    @Override
    public void receiveSlotStacks(StorageName invName, Int2ObjectMap<IAEStack<?>> slotStacks) {
        this.slot.setAEStack(slotStacks == null ? null : slotStacks.get(0));
    }

    @Override
    public void drawFG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        this.fontRendererObj.drawString(GuiText.SelectAmount.getLocal(), 8, 6, 0x404040);
    }

    @Override
    public void drawBG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        super.drawBG(offsetX, offsetY, mouseX, mouseY);
        this.submit.displayString = isShiftKeyDown() ? GuiText.Start.getLocal() : GuiText.Next.getLocal();
        try {
            this.submit.enabled = getAmount() > 0;
        } catch (final NumberFormatException e) {
            this.submit.enabled = false;
        }
        this.amountBox.drawTextBox();
    }

    @Override
    protected void actionPerformed(final GuiButton btn) {
        super.actionPerformed(btn);
        try {
            if (btn == this.craftingMode) {
                GuiImgButton iBtn = (GuiImgButton) btn;

                final Enum cv = iBtn.getCurrentValue();
                final boolean backwards = Mouse.isButtonDown(1);
                final Enum next = Platform.rotateEnum(
                    cv,
                    backwards,
                    iBtn.getSetting()
                        .getPossibleValues());

                iBtn.set(next);
            }
            if (btn == this.submit && this.submit.enabled) {
                Wtct.proxy.netHandler.sendToServer(
                    new CPacketCraftRequest(
                        getAmount(),
                        isShiftKeyDown(),
                        isCtrlKeyDown(),
                        (CraftingMode) this.craftingMode.getCurrentValue()));
            }
        } catch (final NumberFormatException e) {
            this.amountBox.setText("1");
        }
    }

    @Override
    protected void setOriginGUI(Object target) {
        if (target instanceof WirelessDualInterfaceTerminalInventory) {
            // The dual interface terminal inventory is the comprehensive terminal's own host; it has no other owner
            // now that the dual interface terminal itself is not registered.
            this.myIcon = ItemAndBlockHolder.ITEM_COMPREHENSIVE_WORK_TERMINAL.stack();
            this.originalGui = GuiType.COMPREHENSIVE_WORK_TERMINAL;
        }
    }

    @Override
    protected String getBackground() {
        return "guis/craftAmt.png";
    }

    @Override
    public void setAmount(int amount) {
        this.amountBox.setText(String.valueOf(amount));
        this.amountBox.setCursorPositionEnd();
    }
}
