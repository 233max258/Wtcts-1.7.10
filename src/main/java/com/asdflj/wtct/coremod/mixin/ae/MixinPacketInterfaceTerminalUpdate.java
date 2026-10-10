package com.asdflj.wtct.coremod.mixin.ae;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.asdflj.wtct.client.gui.EarlyTerminalLists;

import appeng.client.gui.IInterfaceTerminalPostUpdate;
import appeng.core.sync.AppEngPacket;
import appeng.core.sync.network.INetworkInfo;
import appeng.core.sync.packets.PacketInterfaceTerminalUpdate;

/**
 * AE2's pattern-provider list is pushed from the interface-terminal container's very first tick - while
 * the terminal's window is still opening - and {@code clientPacketData} applies it only while the screen
 * is an {@link IInterfaceTerminalPostUpdate}. The screen swap happens on the client thread while packets
 * are applied on the network thread, so the push that loses that race used to be dropped silently and the
 * freshly opened terminal's management area stayed empty until AE2 re-sent the list for some other reason
 * (a few tenths of a second, sometimes never for a quiet network).
 *
 * <p>
 * Instead of dropping, park the commands in {@link EarlyTerminalLists}: the terminal's {@code initGui}
 * applies them before the first frame draws.
 */
@Mixin(PacketInterfaceTerminalUpdate.class)
public abstract class MixinPacketInterfaceTerminalUpdate {

    @Shadow(remap = false)
    @Final
    private List<PacketInterfaceTerminalUpdate.PacketEntry> commands;

    @Shadow(remap = false)
    private int statusFlags;

    @Inject(method = "clientPacketData", at = @At("HEAD"), cancellable = true, remap = false)
    private void wtct$stashEarlyProviderList(final INetworkInfo network, final AppEngPacket packet,
        final EntityPlayer player, final CallbackInfo ci) {
        if (!(Minecraft.getMinecraft().currentScreen instanceof IInterfaceTerminalPostUpdate)) {
            // TEMP DIAGNOSTIC (1.0.35, remove once the terminal-open delay is pinned down).
            cpw.mods.fml.common.FMLLog.info(
                "[wtct-diag] client iface list STASHED commands=%d t=%d thread=%s",
                this.commands.size(),
                System.currentTimeMillis(),
                Thread.currentThread()
                    .getName());
            EarlyTerminalLists.stashInterfaceUpdate(this.commands, this.statusFlags);
            ci.cancel();
        }
    }
}
