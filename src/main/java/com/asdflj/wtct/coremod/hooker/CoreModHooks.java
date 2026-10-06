package com.asdflj.wtct.coremod.hooker;

import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import appeng.tile.storage.TileIOPort;
import appeng.util.InventoryAdaptor;
import thaumcraft.common.tiles.TileThaumatorium;

public class CoreModHooks {

    public static InventoryAdaptor getAdaptor(TileEntity tile, ForgeDirection face) {
        if (tile == null) return null;
        // The Thaumcraft bridge this used to special-case (the infusion interface and the thaumatorium adapter) has
        // been removed, so every tile now goes through AE2's own adaptor. The hook itself stays put - the coremod
        // transformer calls it from patched AE2 code.
        return InventoryAdaptor.getAdaptor(tile, face);
    }

    public static void getConnectableTile(TileThaumatorium tile, int y, ForgeDirection face) {
        // The thaumatorium interface that used to pull essentia through this hook is gone. The hook stays because the
        // coremod transformer patches a call to it into Thaumcraft's thaumatorium; there is simply nothing left to do.
    }

    public static long getItemsToMove(TileIOPort ioPort, long base) {
        // The extended IO port is gone, so items move at AE2's own rate.
        return base;
    }

}
