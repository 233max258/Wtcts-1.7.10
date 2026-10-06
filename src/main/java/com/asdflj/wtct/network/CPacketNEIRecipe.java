package com.asdflj.wtct.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import com.asdflj.wtct.client.gui.container.ContainerComprehensiveWorkTerminal;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * NEI's hammer: the client packed the recipe's ingredients (every accepted variant per cell) and
 * the server pulls them out of the network into the crafting grid. Serves the crafting terminal's
 * grid and the comprehensive work terminal's manual grid; with {@code auto} (Ctrl held) the
 * comprehensive terminal also keeps watching the slots it could not fill, ae2helpers-style.
 */
public class CPacketNEIRecipe implements IMessage {

    /** Marks the Ctrl+hammer flag inside the recipe tag; the rest of the tag stays "#<slot>" lists. */
    private static final String TAG_AUTO = "__auto";

    private ItemStack[][] recipe;
    private NBTTagCompound r;
    private boolean auto;

    public CPacketNEIRecipe() {}

    public CPacketNEIRecipe(NBTTagCompound r) {
        this(r, false);
    }

    public CPacketNEIRecipe(NBTTagCompound r, boolean auto) {
        // A copy, so the caller's tag (NEI's own packed recipe) is not polluted with our flag.
        this.r = r == null ? null : (NBTTagCompound) r.copy();
        this.auto = auto;
        if (this.r != null) {
            this.r.setBoolean(TAG_AUTO, auto);
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        try {
            ByteArrayInputStream bytes = new ByteArrayInputStream(buf.array());
            bytes.skip(1);
            final NBTTagCompound comp = CompressedStreamTools.readCompressed(bytes);
            if (comp != null) {
                this.auto = comp.getBoolean(TAG_AUTO);
                this.recipe = new ItemStack[9][];
                for (int x = 0; x < this.recipe.length; x++) {
                    final NBTTagList list = comp.getTagList("#" + x, 10);
                    if (list.tagCount() > 0) {
                        this.recipe[x] = new ItemStack[list.tagCount()];
                        for (int y = 0; y < list.tagCount(); y++) {
                            NBTTagCompound tag = list.getCompoundTagAt(y);
                            ItemStack itemStack = ItemStack.loadItemStackFromNBT(tag);
                            // Set the stack size again, but load it as a short
                            if (itemStack != null) {
                                itemStack.stackSize = tag.getShort("Count");
                            }

                            this.recipe[x][y] = itemStack;
                        }
                    }
                }
            }
        } catch (IOException ignored) {

        }

    }

    @Override
    public void toBytes(ByteBuf buf) {
        try {
            final ByteBuf data = Unpooled.buffer();
            final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            final DataOutputStream outputStream = new DataOutputStream(bytes);

            CompressedStreamTools.writeCompressed(r, outputStream);
            data.writeBytes(bytes.toByteArray());
            data.capacity(data.readableBytes());
            buf.writeBytes(data);

        } catch (IOException ignored) {

        }

    }

    public static class Handler implements IMessageHandler<CPacketNEIRecipe, IMessage> {

        @Override
        public IMessage onMessage(CPacketNEIRecipe message, MessageContext ctx) {
            EntityPlayer player = ctx.getServerHandler().playerEntity;
            if (message.recipe == null) {
                return null;
            }
            if (player.openContainer instanceof ContainerComprehensiveWorkTerminal wcwt) {
                wcwt.fillCraftGridFromRecipe(player, message.recipe, message.auto);
            }
            return null;
        }
    }

}
