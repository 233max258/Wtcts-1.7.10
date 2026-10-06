package com.asdflj.wtct.network;

import java.util.Objects;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

import com.asdflj.wtct.client.gui.container.ContainerComprehensiveWorkTerminal;
import com.asdflj.wtct.client.gui.container.ContainerPatternValueAmount;
import com.asdflj.wtct.client.gui.container.IPatternValueContainer;
import com.asdflj.wtct.inventory.InventoryHandler;
import com.asdflj.wtct.inventory.gui.GuiType;
import com.asdflj.wtct.util.BlockPos;

import appeng.container.ContainerOpenContext;
import appeng.container.interfaces.IInventorySlotAware;
import appeng.container.slot.SlotFake;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class CPacketPatternValueSet implements IMessage {

    private GuiType originGui;
    private int amount;
    private int valueIndex;

    public CPacketPatternValueSet() {
        // NO-OP
    }

    public CPacketPatternValueSet(int originalGui, int amount, int valueIndex) {
        this.originGui = GuiType.getByOrdinal(originalGui);
        this.amount = amount;
        this.valueIndex = valueIndex;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(originGui.ordinal());
        buf.writeInt(amount);
        buf.writeInt(valueIndex);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.originGui = GuiType.getByOrdinal(buf.readInt());
        this.amount = buf.readInt();
        this.valueIndex = buf.readInt();
    }

    public static class Handler implements IMessageHandler<CPacketPatternValueSet, IMessage> {

        @Override
        public IMessage onMessage(CPacketPatternValueSet message, MessageContext ctx) {
            EntityPlayer player = ctx.getServerHandler().playerEntity;
            if (player.openContainer instanceof ContainerPatternValueAmount cpv) {
                final Object target = cpv.getTarget();
                final ContainerOpenContext context = cpv.getOpenContext();
                if (context != null) {
                    final TileEntity te = context.getTile();
                    if (te != null) {
                        InventoryHandler.openGui(
                            player,
                            player.worldObj,
                            new BlockPos(te),
                            Objects.requireNonNull(context.getSide()),
                            message.originGui);
                    } else {
                        InventoryHandler.openGui(
                            player,
                            player.getEntityWorld(),
                            new BlockPos(((IInventorySlotAware) target).getInventorySlot(), 0, 0),
                            Objects.requireNonNull(context.getSide()),
                            message.originGui);
                    }
                    if (player.openContainer instanceof IPatternValueContainer) {
                        Slot slot = player.openContainer.getSlot(message.valueIndex);
                        final ItemStack current = slot instanceof SlotFake ? slot.getStack() : null;
                        // A cell that has been emptied since the number was typed has nothing to give
                        // it to, and a number the screen could not read is not an instruction to write
                        // a zero-sized stack into it.
                        if (current != null && message.amount > 0) {
                            // cellWithAmount is the one writer for cell amounts: a fluid cell keeps its
                            // number inside the fluid and comes back as the packet, whatever carrier it
                            // arrived in - packet, GregTech display item, NEI phantom, fluid drop. The
                            // item/fluid split this used to make by itself wrote the typed number into a
                            // display item's *stack size* and left the fluid's own amount alone, so the
                            // cell went on showing the old number: the "changed the amount and nothing
                            // happened" report, until a multiplier rewrite normalised the cell.
                            final ItemStack updated = ContainerComprehensiveWorkTerminal
                                .cellWithAmount(current, message.amount);
                            if (updated != null) {
                                slot.putStack(updated);
                            }
                        }
                    }
                }
            }
            return null;
        }
    }
}
