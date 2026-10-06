package com.asdflj.wtct.coremod.mixin.vanilla;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.server.S2FPacketSetSlot;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Guard against the vanilla NPE in {@code NetHandlerPlayServer#processPlayerBlockPlacement}:
 *
 * <pre>
 * Slot slot = player.openContainer.getSlotFromInventory(player.inventory, player.inventory.currentItem);
 * ...
 * this.sendPacket(new S2FPacketSetSlot(openContainer.windowId, slot.slotNumber, ...)); // slot may be null
 * </pre>
 *
 * {@code slot} is null whenever the server side container has no slot bound to the real
 * {@code InventoryPlayer} instance (e.g. a container using a wrapped player inventory, or a container
 * without any player slots). Right clicking an AE2 access point with a wireless terminal always changes
 * the held itemstack NBT, so the forced resync path is always taken and the server crashes.
 *
 * When that happens we log the offending container and resync the held item through the player's
 * {@code inventoryContainer} instead, which always owns the player's hotbar slots.
 */
@Mixin(NetHandlerPlayServer.class)
public abstract class MixinNetHandlerPlayServer {

    private static final Logger WTCT_NHPS_LOG = LogManager.getLogger("Wtct|NetHandlerPlayServer");

    @Shadow
    private EntityPlayerMP playerEntity;

    @Inject(
        method = "processPlayerBlockPlacement",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/item/ItemStack;areItemStacksEqual(Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemStack;)Z"),
        cancellable = true)
    private void wtct$fixMissingPlayerSlot(C08PacketPlayerBlockPlacement packetIn, CallbackInfo ci) {
        final EntityPlayerMP player = this.playerEntity;
        if (player == null || player.inventory == null) return;

        final Container open = player.openContainer;
        if (open == null) return;

        final int currentItem = player.inventory.currentItem;
        if (open.getSlotFromInventory(player.inventory, currentItem) != null) return;

        final ItemStack held = player.inventory.getCurrentItem();
        WTCT_NHPS_LOG.error(
            "[Wtct] Prevented vanilla NPE in processPlayerBlockPlacement: openContainer={} windowId={} slots={} player={} held={} pos={},{},{}",
            open.getClass()
                .getName(),
            open.windowId,
            open.inventorySlots.size(),
            player.getCommandSenderName(),
            held,
            player.posX,
            player.posY,
            player.posZ);

        final Container inventoryContainer = player.inventoryContainer;
        if (inventoryContainer != null) {
            final Slot slot = inventoryContainer.getSlotFromInventory(player.inventory, currentItem);
            if (slot != null) {
                ((NetHandlerPlayServer) (Object) this).sendPacket(
                    new S2FPacketSetSlot(
                        inventoryContainer.windowId,
                        slot.slotNumber,
                        player.inventory.getCurrentItem()));
            }
        }
        ci.cancel();
    }
}
