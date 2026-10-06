package com.asdflj.wtct.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;

import com.asdflj.wtct.client.gui.container.ContainerComprehensiveWorkTerminal;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * ae2helpers' {@code FillCraftingSlotPacket}: the client's auto-fill watch spotted the material a
 * watched crafting cell waits for in the network (its repo holds at least one), and asks the server
 * to move a single one into that cell. The exact stack the client saw travels along, so the server
 * can extract that precise entry rather than guessing between recipe variants.
 */
public class CPacketFillCraftingSlot implements IMessage {

    private int slotIndex;
    private ItemStack stack;

    public CPacketFillCraftingSlot() {}

    public CPacketFillCraftingSlot(final int slotIndex, final ItemStack stack) {
        this.slotIndex = slotIndex;
        this.stack = stack == null ? null : stack.copy();
    }

    @Override
    public void fromBytes(final ByteBuf buf) {
        try {
            final ByteArrayInputStream bytes = new ByteArrayInputStream(buf.array());
            bytes.skip(1);
            final NBTTagCompound comp = CompressedStreamTools.readCompressed(bytes);
            if (comp != null) {
                this.slotIndex = comp.getInteger("slot");
                this.stack = ItemStack.loadItemStackFromNBT(comp.getCompoundTag("item"));
            }
        } catch (final IOException ignored) {
            // NO-OP - a malformed packet must never break the container.
        }
    }

    @Override
    public void toBytes(final ByteBuf buf) {
        try {
            final NBTTagCompound comp = new NBTTagCompound();
            comp.setInteger("slot", this.slotIndex);
            if (this.stack != null) {
                final NBTTagCompound item = new NBTTagCompound();
                this.stack.writeToNBT(item);
                comp.setTag("item", item);
            }
            final ByteBuf data = Unpooled.buffer();
            final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            final DataOutputStream outputStream = new DataOutputStream(bytes);
            CompressedStreamTools.writeCompressed(comp, outputStream);
            data.writeBytes(bytes.toByteArray());
            data.capacity(data.readableBytes());
            buf.writeBytes(data);
        } catch (final IOException ignored) {
            // NO-OP
        }
    }

    public static class Handler implements IMessageHandler<CPacketFillCraftingSlot, IMessage> {

        @Override
        public IMessage onMessage(final CPacketFillCraftingSlot message, final MessageContext ctx) {
            if (message.stack == null) {
                return null;
            }
            if (ctx.getServerHandler().playerEntity.openContainer instanceof ContainerComprehensiveWorkTerminal wcwt) {
                wcwt.fillCraftingSlot(message.slotIndex, message.stack);
            }
            return null;
        }
    }
}
