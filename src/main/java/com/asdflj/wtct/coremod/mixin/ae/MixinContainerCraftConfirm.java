package com.asdflj.wtct.coremod.mixin.ae;

import static com.asdflj.wtct.api.Constants.MessageType.ADD_PINNED_ITEM;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.asdflj.wtct.Wtct;
import com.asdflj.wtct.inventory.item.WirelessDualInterfaceTerminalInventory;
import com.asdflj.wtct.loader.ItemAndBlockHolder;
import com.asdflj.wtct.network.SPacketMEItemInvUpdate;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingJob;
import appeng.api.storage.ITerminalHost;
import appeng.api.storage.data.IAEStack;
import appeng.container.AEBaseContainer;
import appeng.container.PrimaryGui;
import appeng.container.implementations.ContainerCraftConfirm;
import appeng.util.Platform;

@Mixin(ContainerCraftConfirm.class)
public abstract class MixinContainerCraftConfirm extends AEBaseContainer {

    @Shadow(remap = false)
    protected ICraftingJob result;

    @Shadow(remap = false)
    public abstract boolean isSimulation();

    @Shadow(remap = false)
    public abstract IGrid getGrid();

    private IAEStack<?> is = null;

    public MixinContainerCraftConfirm(InventoryPlayer ip, ITerminalHost anchor) {
        super(ip, anchor);
    }

    /**
     * AE2 normally fills in the primary gui of a freshly opened sub-gui container by calling
     * {@code bc.createPrimaryGui()} on the <b>old</b> container, which looks the container class up in AE2's own
     * GuiBridge table. This mod's terminal containers are not in that table, so the lookup returns null and the
     * cancel button of {@code GuiCraftConfirm} (which sends {@code new PacketSwitchGuis()} -> {@code
     * getPrimaryGui().open(player)}) crashes the integrated server with an NPE. Fill in a dummy PrimaryGui
     * server side while the container is built; {@code PrimaryGui#open} silently ignores the non-bridge gui,
     * and the real reopen is routed through this mod's own packet by {@code MixinGuiCraftConfirm}.
     *
     * <p>
     * Server only: on the client {@code setPrimaryGui} would reach {@code onSlotChange} before
     * {@code GuiCraftConfirm} has wired its guiLink (see MixinContainerCraftingStatus for the full reasoning).
     */
    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void wtct$setPrimaryGui(InventoryPlayer ip, ITerminalHost anchor, CallbackInfo ci) {
        if (Platform.isClient()) {
            return;
        }
        ItemStack icon = null;
        if (anchor instanceof WirelessDualInterfaceTerminalInventory) {
            // See GuiCraftAmount: the dual interface inventory only ever belongs to the comprehensive terminal now.
            icon = ItemAndBlockHolder.ITEM_COMPREHENSIVE_WORK_TERMINAL.stack();
        }
        if (icon == null) {
            return;
        }
        setPrimaryGui(new PrimaryGui(anchor, icon, null, ForgeDirection.UNKNOWN));
    }

    @Inject(method = "setItemToCraft", at = @At("HEAD"), remap = false)
    public void setItemToCraft(IAEStack<?> itemToCraft, CallbackInfo ci) {
        if (itemToCraft != null) {
            is = itemToCraft.copy();
        }
    }

    @Inject(method = "startJob()V", at = @At("HEAD"), remap = false)
    public void startJob(CallbackInfo ci) {
        if (this.result != null && !this.isSimulation() && getGrid() != null && is != null) {
            SPacketMEItemInvUpdate piu = new SPacketMEItemInvUpdate(ADD_PINNED_ITEM);
            piu.appendStack(is);
            Wtct.proxy.netHandler.sendTo(piu, (EntityPlayerMP) this.getPlayerInv().player);
            is = null;
        }
    }
}
