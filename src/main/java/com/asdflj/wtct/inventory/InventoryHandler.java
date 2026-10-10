package com.asdflj.wtct.inventory;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.asdflj.wtct.Wtct;
import com.asdflj.wtct.client.gui.container.ContainerMonitor;
import com.asdflj.wtct.inventory.gui.GuiType;
import com.asdflj.wtct.network.CPacketSwitchGuis;
import com.asdflj.wtct.util.BlockPos;

import appeng.util.Platform;
import cpw.mods.fml.common.network.IGuiHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class InventoryHandler implements IGuiHandler {

    public static void openGui(EntityPlayer player, World world, BlockPos pos, ForgeDirection face, GuiType guiType) {
        if (Platform.isClient()) {
            return;
        }
        player.openGui(
            Wtct.INSTANCE,
            (guiType.ordinal() << 3) | face.ordinal(),
            world,
            pos.getX(),
            pos.getY(),
            pos.getZ());
    }

    public static void switchGui(GuiType guiType) {
        Wtct.proxy.netHandler.sendToServer(new CPacketSwitchGuis(guiType));
    }

    @Nullable
    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        int faceOrd = id & 0x7;
        if (faceOrd > ForgeDirection.values().length) {
            return null;
        }
        ForgeDirection face = ForgeDirection.getOrientation(faceOrd);
        GuiType type = GuiType.getByOrdinal(id >>> 3);
        final Object gui = type != null ? type.guiFactory.createServerGui(player, world, x, y, z, face) : null;
        // FML sends the open-window packet before Container.addCraftingToCrafters, so the client's
        // initGui runs before the server has pushed the network's item and fluid lists: every window
        // used to come up on an empty grid until the round trip finished. This hook is the one thing
        // that runs before that packet, so the lists are pushed from here - the client receives them
        // first and parks them in EarlyTerminalLists, which initGui applies, so the grid is full on the
        // very first frame of every window, not just the ones that raced ahead.
        if (gui instanceof ContainerMonitor monitor && player instanceof EntityPlayerMP playerMP) {
            monitor.resendInventory(playerMP);
        }
        return gui;
    }

    @SideOnly(Side.CLIENT)
    @Nullable
    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        int faceOrd = id & 0x7;
        if (faceOrd > ForgeDirection.values().length) {
            return null;
        }
        ForgeDirection face = ForgeDirection.getOrientation(faceOrd);
        GuiType type = GuiType.getByOrdinal(id >>> 3);
        return type != null ? type.guiFactory.createClientGui(player, world, x, y, z, face) : null;
    }
}
