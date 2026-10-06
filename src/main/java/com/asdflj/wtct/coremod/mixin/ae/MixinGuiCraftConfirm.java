package com.asdflj.wtct.coremod.mixin.ae;

import java.util.Iterator;
import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Container;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.asdflj.wtct.Wtct;
import com.asdflj.wtct.inventory.InventoryHandler;
import com.asdflj.wtct.inventory.gui.GuiType;
import com.asdflj.wtct.inventory.item.WirelessDualInterfaceTerminalInventory;
import com.asdflj.wtct.network.CPacketTerminalBtns;
import com.asdflj.wtct.util.NameConst;

import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.implementations.GuiCraftConfirm;
import appeng.client.gui.widgets.GuiAeButton;
import appeng.container.implementations.ContainerCraftConfirm;

@Mixin(GuiCraftConfirm.class)
public abstract class MixinGuiCraftConfirm extends AEBaseGui {

    @Shadow(remap = false)
    private GuiAeButton start;

    @Shadow(remap = false)
    private GuiButton cancel;

    @Shadow(remap = false)
    private void addMissingItemsToBookMark() {}

    @Shadow(remap = false)
    @Final
    private IItemList<IAEItemStack> storage;
    @Shadow(remap = false)
    @Final
    private IItemList<IAEItemStack> pending;
    @Shadow(remap = false)
    @Final
    private IItemList<IAEItemStack> missing;
    @Shadow(remap = false)
    @Final
    private List<IAEItemStack> visual;
    private GuiAeButton replan = null;
    private boolean clickStart = false;

    public MixinGuiCraftConfirm(Container container) {
        super(container);
    }

    @Inject(method = "actionPerformed", at = @At(value = "HEAD"), cancellable = true)
    private void actionPerformed(GuiButton btn, CallbackInfo ci) {
        if (btn == cancel) {
            // Keep AE2's shift-to-bookmark behaviour, then return through this mod's own switch packet.
            // AE2 would send PacketSwitchGuis() (no gui); its PrimaryGui cannot address an item hosted
            // terminal that may live in a Baubles slot, so route the reopen the same way as the
            // "back to terminal" tab of GuiCraftingStatus does.
            addMissingItemsToBookMark();
            if (this.inventorySlots instanceof ContainerCraftConfirm ccc) {
                final Object target = ccc.getTarget();
                if (target instanceof WirelessDualInterfaceTerminalInventory) {
                    // That inventory is the comprehensive work terminal's own host.
                    InventoryHandler.switchGui(GuiType.COMPREHENSIVE_WORK_TERMINAL);
                    ci.cancel();
                }
            }
            // Any other host falls through to AE2's original handling.
        }
        if (btn == start) {
            clickStart = true;
        } else if (btn == replan) {
            clickStart = false;
            start.enabled = false;
            replan.visible = false;
            clearList(this.storage);
            clearList(this.pending);
            clearList(this.missing);
            this.visual.clear();
            Wtct.proxy.netHandler.sendToServer(new CPacketTerminalBtns("GuiCraftConfirm.replan", true));
        }
    }

    /**
     * AE2 rv3-beta-1050 起 GuiCraftConfirm 的 storage/pending/missing 运行时类型是 IAEStackList（统一物品/流体
     * 栈列表），不再是旧的 ItemList，直接强转会 ClassCastException。IItemList/IItemContainer 接口没有 clear()，
     * 用迭代器 remove 清空，对两种实现都兼容。
     */
    private static void clearList(IItemList<?> list) {
        final Iterator<?> it = list.iterator();
        while (it.hasNext()) {
            it.next();
            it.remove();
        }
    }

    @Inject(method = "initGui", at = @At("TAIL"))
    public void initGui(CallbackInfo ci) {
        this.buttonList.add(
            replan = new GuiAeButton(
                0,
                start.xPosition,
                start.yPosition,
                start.width,
                start.height,
                I18n.format(NameConst.GUI_BUTTON_REPLAN),
                ""));
        this.replan.visible = false;
    }

    @Inject(method = "drawFG", at = @At("HEAD"), remap = false)
    public void drawFG(CallbackInfo ci) {
        try {
            if (clickStart || !start.enabled) {
                replan.visible = true;
                start.visible = false;
            } else {
                replan.visible = false;
                start.visible = true;
            }
        } catch (Exception ignored) {}

    }
}
