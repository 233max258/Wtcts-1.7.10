package com.asdflj.wtct.network;

import net.minecraft.entity.player.EntityPlayerMP;

import com.asdflj.wtct.client.gui.container.ContainerMonitor;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * "Send me the network's item and fluid lists again."
 *
 * <p>
 * Both are pushed while the container is still opening - {@code ContainerMonitor.addCraftingToCrafters}
 * hands {@link SPacketMEItemInvUpdate} and {@link SPacketMEFluidInvUpdate} to the server's network
 * wrapper the moment the window is built - and their handlers only apply a list while the terminal's
 * screen is the one on display. A list that arrives a moment early used to be dropped on the floor, and
 * the terminal then drew an empty item grid until the network happened to post its next change. The
 * client now parks such a list and applies it from its {@code initGui} (see {@code EarlyTerminalLists});
 * this request stays as the backstop and is answered by the container's next tick.
 *
 * <p>
 * The handler must not push the lists itself: simpleimpl runs it on a Netty IO thread, where FML's
 * {@code getEffectiveSide()} answers CLIENT for anything not named "Server thread", so
 * {@code ItemMonitor.queueInventory}'s {@code Platform.isServer()} guard would silently no-op (which is
 * exactly what the first attempt at this fix did). It only raises a flag; {@code ContainerMonitor}'s
 * {@code detectAndSendChanges} consumes it on the server thread and re-sends from there.
 *
 * <p>
 * No payload: the container that answers is the player's open one, which is also what decides whether
 * the request belongs to anybody (the window id is not trustworthy for AE2's item GUIs).
 */
public class CPacketInventoryRequest implements IMessage {

    public CPacketInventoryRequest() {
        // NO-OP
    }

    @Override
    public void fromBytes(final ByteBuf buf) {
        // NO-OP
    }

    @Override
    public void toBytes(final ByteBuf buf) {
        // NO-OP
    }

    public static class Handler implements IMessageHandler<CPacketInventoryRequest, IMessage> {

        @Override
        public IMessage onMessage(final CPacketInventoryRequest message, final MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            if (player.openContainer instanceof final ContainerMonitor monitor) {
                // Flag only - see the class javadoc for why this must not push from the Netty thread.
                monitor.requestInventoryResend();
            }
            return null;
        }
    }
}
