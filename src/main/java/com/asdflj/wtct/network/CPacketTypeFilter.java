package com.asdflj.wtct.network;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;

import com.asdflj.wtct.client.gui.container.ITypeFilterContainer;

import appeng.api.storage.data.AEStackTypeRegistry;
import appeng.api.storage.data.IAEStackType;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * One type toggle click: the id of the type that flipped and its new value.
 *
 * <p>
 * Deliberately a single entry instead of a whole map: the client's copy of the map starts out as "everything enabled"
 * and is only corrected once the server pushes the saved filter, so a whole-map upload could overwrite the settings a
 * terminal was saved with before that push ever arrives.
 */
public class CPacketTypeFilter implements IMessage {

    private int windowId;
    private String typeId;
    private boolean value;

    public CPacketTypeFilter() {
        // NO-OP
    }

    public CPacketTypeFilter(int windowId, String typeId, boolean value) {
        this.windowId = windowId;
        this.typeId = typeId;
        this.value = value;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.windowId = buf.readInt();
        this.typeId = ByteBufUtils.readUTF8String(buf);
        this.value = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.windowId);
        ByteBufUtils.writeUTF8String(buf, this.typeId == null ? "" : this.typeId);
        buf.writeBoolean(this.value);
    }

    public static class Handler implements IMessageHandler<CPacketTypeFilter, IMessage> {

        @Override
        public IMessage onMessage(CPacketTypeFilter message, MessageContext ctx) {
            final EntityPlayer player = ctx.getServerHandler().playerEntity;
            final Container c = player.openContainer;
            // No windowId gate: an AE2 item GUI builds its container on the client as well, and the two sides do not
            // necessarily agree on the window id, so the check silently ate every toggle. The container type is the
            // real guard - a packet only lands on the terminal that is open right now.
            final IAEStackType<?> type = AEStackTypeRegistry.getType(message.typeId);
            if (type != null && c instanceof ITypeFilterContainer container) {
                container.updateTypeFilter(type, message.value, player);
            }
            return null;
        }
    }
}
