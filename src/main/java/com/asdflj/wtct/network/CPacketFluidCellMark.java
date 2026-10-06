package com.asdflj.wtct.network;

import net.minecraft.entity.player.EntityPlayerMP;

import com.asdflj.wtct.client.gui.container.ContainerComprehensiveWorkTerminal;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * "Mark the fluid of the cell I am holding into this encoder cell". A storage cell's contents only
 * exist in the server's storage manager, so the client cannot read them - the Ctrl+click gesture on
 * a fluid cell is therefore decided by the container, which does have the fluid list.
 */
public class CPacketFluidCellMark implements IMessage {

    private int slotNumber;
    /** true = convert what the CELL already holds; false = take the fluid out of the held item. */
    private boolean fromCell;

    public CPacketFluidCellMark() {}

    public CPacketFluidCellMark(final int slotNumber, final boolean fromCell) {
        this.slotNumber = slotNumber;
        this.fromCell = fromCell;
    }

    @Override
    public void fromBytes(final ByteBuf buf) {
        this.slotNumber = buf.readInt();
        this.fromCell = buf.readBoolean();
    }

    @Override
    public void toBytes(final ByteBuf buf) {
        buf.writeInt(this.slotNumber);
        buf.writeBoolean(this.fromCell);
    }

    public static class Handler implements IMessageHandler<CPacketFluidCellMark, IMessage> {

        @Override
        public IMessage onMessage(final CPacketFluidCellMark message, final MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            if (player.openContainer instanceof final ContainerComprehensiveWorkTerminal cwt) {
                cwt.markFluidFromHeldCell(player, message.slotNumber, message.fromCell);
            }
            return null;
        }
    }
}
