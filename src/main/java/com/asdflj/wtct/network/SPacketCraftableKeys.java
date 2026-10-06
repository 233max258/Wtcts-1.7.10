package com.asdflj.wtct.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.Item;

import com.asdflj.wtct.client.gui.GuiComprehensiveWorkTerminal;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * The craftable item keys of the whole network, computed server-side and pushed to the terminal's
 * screen. GTNH AE serves the client item repo paginated/searched, so the client only ever holds a
 * slice of the network - the craftable markers (blue diamonds) on the encoding cells and the
 * hammer's preview need the complete picture, so the container sends it.
 *
 * <p>
 * A key is {@code (itemId << 16) | (damage & 0xFFFF)} - the same shape on both ends.
 */
public class SPacketCraftableKeys implements IMessage {

    private int[] keys = new int[0];

    public SPacketCraftableKeys() {}

    public SPacketCraftableKeys(final int[] keys) {
        this.keys = keys == null ? new int[0] : keys;
    }

    @Override
    public void fromBytes(final ByteBuf buf) {
        final int n = buf.readInt();
        this.keys = new int[n];
        for (int i = 0; i < n; i++) {
            this.keys[i] = buf.readInt();
        }
    }

    @Override
    public void toBytes(final ByteBuf buf) {
        buf.writeInt(this.keys.length);
        for (final int key : this.keys) {
            buf.writeInt(key);
        }
    }

    public static int keyOf(final Item item, final int damage) {
        return (Item.getIdFromItem(item) << 16) | (damage & 0xFFFF);
    }

    public static class Handler implements IMessageHandler<SPacketCraftableKeys, IMessage> {

        @Override
        public IMessage onMessage(final SPacketCraftableKeys message, final MessageContext ctx) {
            final GuiScreen gs = Minecraft.getMinecraft().currentScreen;
            if (gs instanceof GuiComprehensiveWorkTerminal) {
                ((GuiComprehensiveWorkTerminal) gs).postCraftableKeys(message.keys);
            }
            return null;
        }
    }
}
