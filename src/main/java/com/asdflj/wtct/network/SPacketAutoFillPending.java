package com.asdflj.wtct.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import com.asdflj.wtct.client.gui.AutoFillWatch;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * Server -> client: the crafting cells the Ctrl+hammer could not fill but the network has patterns
 * for, each with the ingredient variants its pattern accepts. ae2helpers' {@code setPending} - the
 * client owns the wait list and polls its own view of the network until the crafted item shows up,
 * the server only ever says which cells are worth waiting for.
 *
 * <p>
 * The payload is a 3x3 grid like {@link CPacketNEIRecipe}'s ({@code "#<slot>"} lists of item stacks),
 * and it is written straight into the static {@link AutoFillWatch}: the terminal's screen is gone by
 * the time this arrives - Ctrl+hammer opens AE2's craft plan over it - so there is no screen to
 * hand it to, and the watch it arms outlives the plan.
 */
public class SPacketAutoFillPending implements IMessage {

    /** The crafting matrix is 3x3 on both terminals. */
    private static final int CELLS = 9;

    private Map<Integer, ItemStack[]> pending = new LinkedHashMap<>();

    public SPacketAutoFillPending() {}

    public SPacketAutoFillPending(final Map<Integer, ItemStack[]> pending) {
        if (pending != null) {
            this.pending.putAll(pending);
        }
    }

    @Override
    public void fromBytes(final ByteBuf buf) {
        try {
            // The first byte of the backing array is the channel's discriminator, as in CPacketNEIRecipe.
            final ByteArrayInputStream bytes = new ByteArrayInputStream(buf.array());
            bytes.skip(1);
            final NBTTagCompound comp = CompressedStreamTools.readCompressed(bytes);
            if (comp == null) {
                return;
            }
            for (int x = 0; x < CELLS; x++) {
                final NBTTagList list = comp.getTagList("#" + x, 10);
                if (list.tagCount() == 0) {
                    continue;
                }
                final ItemStack[] variants = new ItemStack[list.tagCount()];
                for (int y = 0; y < list.tagCount(); y++) {
                    variants[y] = ItemStack.loadItemStackFromNBT(list.getCompoundTagAt(y));
                }
                this.pending.put(x, variants);
            }
        } catch (final IOException ignored) {}
    }

    @Override
    public void toBytes(final ByteBuf buf) {
        try {
            final NBTTagCompound comp = new NBTTagCompound();
            for (int x = 0; x < CELLS; x++) {
                final ItemStack[] variants = this.pending.get(x);
                if (variants == null || variants.length == 0) {
                    continue;
                }
                final NBTTagList list = new NBTTagList();
                for (final ItemStack variant : variants) {
                    if (variant != null) {
                        final NBTTagCompound tag = new NBTTagCompound();
                        variant.writeToNBT(tag);
                        list.appendTag(tag);
                    }
                }
                comp.setTag("#" + x, list);
            }
            final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            CompressedStreamTools.writeCompressed(comp, new DataOutputStream(bytes));
            buf.writeBytes(bytes.toByteArray());
        } catch (final IOException ignored) {}
    }

    public static class Handler implements IMessageHandler<SPacketAutoFillPending, IMessage> {

        @Override
        public IMessage onMessage(final SPacketAutoFillPending message, final MessageContext ctx) {
            AutoFillWatch.INSTANCE.setPending(message.pending);
            return null;
        }
    }
}
