package com.asdflj.wtct.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;

import com.asdflj.wtct.Wtct;
import com.asdflj.wtct.client.gui.container.ITypeFilterContainer;

import appeng.api.storage.ITerminalTypeFilterProvider;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * "Send me the type filter this terminal was saved with."
 *
 * <p>
 * The server pushes the filter once on its own through {@link SPacketTypeFilter}, but that happens while the container
 * is still opening - if the screen is not up yet the client drops it and keeps its default "everything enabled", so a
 * terminal's saved 物品 / 流体 / 源质 selection looked like it was never remembered. The GUI asks again once it is
 * actually on screen.
 */
public class CPacketTypeFilterRequest implements IMessage {

    private int windowId;

    public CPacketTypeFilterRequest() {
        // NO-OP
    }

    public CPacketTypeFilterRequest(int windowId) {
        this.windowId = windowId;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.windowId = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.windowId);
    }

    public static class Handler implements IMessageHandler<CPacketTypeFilterRequest, IMessage> {

        @Override
        public IMessage onMessage(CPacketTypeFilterRequest message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            final Container c = player.openContainer;
            // See CPacketTypeFilter: the window id cannot be trusted for AE2's item GUIs, the open container is the
            // thing that decides whether this request belongs to anyone.
            if (c instanceof ITypeFilterContainer container) {
                final ITerminalTypeFilterProvider host = container.getTypeFilterHost();
                if (host != null) {
                    Wtct.proxy.netHandler.sendTo(new SPacketTypeFilter(host.getTypeFilter(player)), player);
                }
            }
            return null;
        }
    }
}
