package com.asdflj.wtct.network;

import java.util.Objects;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.asdflj.wtct.inventory.InventoryHandler;
import com.asdflj.wtct.inventory.gui.GuiType;
import com.asdflj.wtct.util.BaublesUtil;
import com.asdflj.wtct.util.BlockPos;
import com.asdflj.wtct.util.Util;

import appeng.client.gui.AEBaseGui;
import appeng.container.AEBaseContainer;
import appeng.container.ContainerOpenContext;
import appeng.container.interfaces.IInventorySlotAware;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class CPacketSwitchGuis implements IMessage {

    private GuiType guiType;

    public CPacketSwitchGuis() {}

    public CPacketSwitchGuis(GuiType guiType) {
        this.guiType = guiType;
        AEBaseGui.setSwitchingGuis(true);
    }

    @Override
    public void fromBytes(ByteBuf byteBuf) {
        guiType = GuiType.getByOrdinal(byteBuf.readByte());
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(guiType != null ? guiType.ordinal() : 0);
    }

    public static class Handler implements IMessageHandler<CPacketSwitchGuis, IMessage> {

        @Nullable
        @Override
        public IMessage onMessage(CPacketSwitchGuis message, MessageContext ctx) {
            if (message.guiType == null) {
                return null;
            }
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            Container cont = player.openContainer;
            World w = player.worldObj;
            if (message.guiType == GuiType.COMPREHENSIVE_WORK_TERMINAL
                || message.guiType == GuiType.COMPREHENSIVE_WORK_TERMINAL_BRIDGE) {
                // Opened from the keybinding, which always routes through the bridge factory: the
                // terminal may sit in a Baubles slot, and only the bridge factory can decode a bridge
                // coordinate. Resolved here, server side, since the client copy of a Baubles slot is
                // not reliably synced.
                int s = Util.findComprehensiveWorkTerminal(player);
                if (s != -1) {
                    InventoryHandler.openGui(
                        player,
                        w,
                        new BlockPos(s, 0, 0),
                        ForgeDirection.UNKNOWN,
                        GuiType.COMPREHENSIVE_WORK_TERMINAL_BRIDGE);
                }
                return null;
            } else if (message.guiType == GuiType.TERMINAL_MENU) {
                InventoryHandler.openGui(player, w, new BlockPos(0, 0, 0), ForgeDirection.UNKNOWN, message.guiType);
                return null;
            }
            if (cont instanceof AEBaseContainer c) {
                ContainerOpenContext context = ((AEBaseContainer) cont).getOpenContext();
                if (context == null) {
                    return null;
                }
                TileEntity te = context.getTile();
                if (te != null) {
                    InventoryHandler.openGui(
                        player,
                        player.worldObj,
                        new BlockPos(te),
                        Objects.requireNonNull(context.getSide()),
                        message.guiType);
                } else {
                    // The target is an item-hosted container, so the coordinate is an inventory slot. When the
                    // host is one of our wireless terminals the slot is bridge-encoded (it may point at a
                    // Baubles slot), and only the bridge factory under this same gui type can resolve it.
                    final int slot = ((IInventorySlotAware) (c.getTarget())).getInventorySlot();
                    final GuiType type = BaublesUtil.isBridgeSlot(slot) ? message.guiType.bridgeVariant()
                        : message.guiType;
                    InventoryHandler.openGui(
                        player,
                        player.getEntityWorld(),
                        new BlockPos(slot, 0, 0),
                        Objects.requireNonNull(context.getSide()),
                        type != null ? type : message.guiType);
                }
            }
            return null;
        }

    }

}
