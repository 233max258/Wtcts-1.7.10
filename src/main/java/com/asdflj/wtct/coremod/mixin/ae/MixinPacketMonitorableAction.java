package com.asdflj.wtct.coremod.mixin.ae;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.asdflj.wtct.client.gui.container.ContainerMonitor;

import appeng.core.sync.AppEngPacket;
import appeng.core.sync.network.INetworkInfo;
import appeng.core.sync.packets.PacketMonitorableAction;
import appeng.helpers.MonitorableAction;

/**
 * AE2's {@code PacketMonitorableAction} only honors containers that extend
 * {@code ContainerMEMonitorable} and silently drops every other request
 * ("item can't be taken out of the terminal, no error anywhere").
 *
 * This mod's terminals ({@link ContainerMonitor} subclasses) implement their own
 * monitor wrappers instead, so route the packet to
 * {@link ContainerMonitor#doMonitorableAction(MonitorableAction, int, EntityPlayerMP)}.
 */
@Mixin(PacketMonitorableAction.class)
public abstract class MixinPacketMonitorableAction {

    @Shadow(remap = false)
    @Final
    private MonitorableAction action;

    @Shadow(remap = false)
    @Final
    private int custom;

    @Inject(method = "serverPacketData", at = @At("HEAD"), cancellable = true, remap = false)
    private void wtct$routeToContainerMonitor(INetworkInfo manager, AppEngPacket packet, EntityPlayer player,
        CallbackInfo ci) {
        if (player.openContainer instanceof ContainerMonitor monitor) {
            monitor.doMonitorableAction(this.action, this.custom, (EntityPlayerMP) player);
            ci.cancel();
        }
    }
}
