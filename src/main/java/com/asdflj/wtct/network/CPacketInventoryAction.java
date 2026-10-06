package com.asdflj.wtct.network;

import static com.asdflj.wtct.api.Constants.DISPLAY_ONLY;

import java.io.IOException;
import java.util.Objects;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import com.asdflj.wtct.inventory.InventoryHandler;
import com.asdflj.wtct.inventory.gui.GuiType;
import com.asdflj.wtct.inventory.item.WirelessTerminal;
import com.asdflj.wtct.util.BlockPos;
import com.glodblock.github.common.item.ItemFluidDrop;

import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.container.AEBaseContainer;
import appeng.container.ContainerOpenContext;
import appeng.container.implementations.ContainerCraftAmount;
import appeng.helpers.InventoryAction;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class CPacketInventoryAction implements IMessage {

    private InventoryAction action;
    private int slot;
    private long id;
    private IAEItemStack stack;
    private IAEFluidStack fluid;

    public CPacketInventoryAction() {}

    public CPacketInventoryAction(final InventoryAction action, final int slot, final int id) {
        this.action = action;
        this.slot = slot;
        this.id = id;
        this.stack = null;
        this.fluid = null;
    }

    public CPacketInventoryAction(final InventoryAction action, final int slot, final int id, IAEItemStack stack) {
        this.action = action;
        this.slot = slot;
        this.id = id;
        this.stack = stack;
        this.fluid = null;
    }

    public CPacketInventoryAction(final InventoryAction action, final int slot, final int id, IAEFluidStack fluid) {
        this.action = action;
        this.slot = slot;
        this.id = id;
        this.stack = null;
        this.fluid = fluid;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(action.ordinal());
        buf.writeInt(slot);
        buf.writeLong(id);
        // 0 = nothing, 1 = item stack, 2 = fluid stack
        if (this.stack != null) {
            buf.writeByte(1);
        } else if (this.fluid != null) {
            buf.writeByte(2);
        } else {
            buf.writeByte(0);
        }
        try {
            if (this.stack != null) {
                this.stack.writeToPacket(buf);
            } else if (this.fluid != null) {
                this.fluid.writeToPacket(buf);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        action = InventoryAction.values()[buf.readInt()];
        slot = buf.readInt();
        id = buf.readLong();
        final int kind = buf.readByte();
        try {
            if (kind == 1) {
                stack = AEItemStack.loadItemStackFromPacket(buf);
            } else if (kind == 2) {
                fluid = AEFluidStack.loadFluidStackFromPacket(buf);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class Handler implements IMessageHandler<CPacketInventoryAction, IMessage> {

        @Nullable
        @Override
        public IMessage onMessage(CPacketInventoryAction message, MessageContext ctx) {
            final EntityPlayerMP sender = ctx.getServerHandler().playerEntity;
            // The crafting terminal this used to bail out on is gone; every remaining container is handled below.
            if (sender.openContainer instanceof final AEBaseContainer baseContainer) {
                Object target = baseContainer.getTarget();
                if (message.action == InventoryAction.AUTO_CRAFT) {
                    final ContainerOpenContext context = baseContainer.getOpenContext();
                    if (context != null) {
                        final TileEntity te = context.getTile();
                        if (te != null || target instanceof WirelessTerminal) {
                            if (message.fluid != null) {
                                baseContainer.setTargetStack(message.fluid);
                            } else {
                                if (message.stack == null){
                                    if (baseContainer.getTargetStack() instanceof IAEItemStack ais) {
                                        message.stack = ais;
                                    }
                                }
                                if(message.stack != null && message.stack.getItem() instanceof ItemFluidDrop){
                                    ItemStack is = message.stack.getItemStack().copy();
                                    NBTTagCompound data = is.getTagCompound();
                                    data.removeTag(DISPLAY_ONLY);
                                    is.setTagCompound(data);
                                    baseContainer.setTargetStack(AEItemStack.create(is));
                                }else{
                                    baseContainer.setTargetStack(message.stack);
                                }
                            }
                            if(te != null){
                                InventoryHandler.openGui(
                                    sender,
                                    te.getWorldObj(),
                                    new BlockPos(te),
                                    Objects.requireNonNull(baseContainer.getOpenContext().getSide()),
                                    GuiType.CRAFTING_AMOUNT);
                            }else{
                                InventoryHandler.openGui(
                                    sender,
                                    sender.getEntityWorld(),
                                    new BlockPos(((WirelessTerminal) target).getInventorySlot(),0,0),
                                    Objects.requireNonNull(baseContainer.getOpenContext().getSide()),
                                    GuiType.CRAFTING_AMOUNT_ITEM);
                            }
                        }
                        if (sender.openContainer instanceof final ContainerCraftAmount cca) {
                            final IAEStack<?> toCraft = baseContainer.getTargetStack();
                            if (toCraft != null) {
                                // Sends PacketVirtualSlot to the client, which renders the
                                // item/fluid icon in the amount selection GUI.
                                cca.setItemToCraft(toCraft);
                            }
                            cca.detectAndSendChanges();
                        }
                    }
                } else {
                    baseContainer.doAction(sender, message.action, message.slot, message.id);
                }
            }
            return null;
        }
    }
}
