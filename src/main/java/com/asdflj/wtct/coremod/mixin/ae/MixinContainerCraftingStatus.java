package com.asdflj.wtct.coremod.mixin.ae;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.asdflj.wtct.inventory.item.WirelessDualInterfaceTerminalInventory;
import com.asdflj.wtct.loader.ItemAndBlockHolder;

import appeng.api.storage.ITerminalHost;
import appeng.container.PrimaryGui;
import appeng.container.implementations.ContainerCraftingStatus;
import appeng.util.Platform;

@Mixin(ContainerCraftingStatus.class)
public abstract class MixinContainerCraftingStatus {

    /**
     * AE2 only draws the "back to terminal" tab when {@code getPrimaryGuiIcon()} is non-null, and those two
     * methods are backed by a real (inaccessible) container slot, so the value has to be filled in when the
     * container is built - that is, server side, before the icon slot is synchronised to the client.
     *
     * <p>
     * Server only, on purpose. On the client this call would run during the gui constructor, before
     * {@code GuiCraftingStatus} has wired its {@code guiLink} into the container (it does so right after
     * {@code new ContainerCraftingStatus}); the resulting {@code putStack} would then reach
     * {@code onSlotChange}, which dereferences {@code guiLink} and crashes. On the client the icon simply
     * arrives through the regular slot synchronisation instead, at a point where {@code guiLink} is set and
     * AE2 creates the button itself via {@code IGuiSub#initPrimaryGuiButton}.
     *
     * <p>
     * The primary gui object is deliberately given the host rather than a {@code GuiBridge}: this mod's
     * terminal containers are not in AE2's bridge table (a lookup would return null and crash
     * {@code PacketSwitchGuis} later). {@code PrimaryGui#open} silently ignores non-bridge objects, and the
     * actual reopen is routed through this mod's own packet in {@code GuiCraftingStatus#actionPerformed},
     * which is the only way to reopen an item hosted terminal that may live in a Baubles slot.
     */
    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void wtct$setPrimaryGui(InventoryPlayer ip, ITerminalHost host, CallbackInfo ci) {
        if (Platform.isClient()) {
            return;
        }
        ItemStack icon = null;
        if (host instanceof WirelessDualInterfaceTerminalInventory) {
            // See GuiCraftAmount: the dual interface inventory only ever belongs to the comprehensive terminal now.
            icon = ItemAndBlockHolder.ITEM_COMPREHENSIVE_WORK_TERMINAL.stack();
        }
        if (icon == null) {
            return;
        }
        ((ContainerCraftingStatus) (Object) this)
            .setPrimaryGui(new PrimaryGui(host, icon, null, ForgeDirection.UNKNOWN));
    }
}
