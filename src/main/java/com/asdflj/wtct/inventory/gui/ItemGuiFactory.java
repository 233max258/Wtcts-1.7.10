package com.asdflj.wtct.inventory.gui;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.asdflj.wtct.inventory.item.IItemInventory;
import com.asdflj.wtct.util.BaublesUtil;

import appeng.container.AEBaseContainer;
import appeng.container.ContainerOpenContext;

public abstract class ItemGuiFactory<T> implements IGuiFactory {

    protected final Class<T> invClass;

    ItemGuiFactory(Class<T> invClass) {
        this.invClass = invClass;
    }

    @Nullable
    protected T getInventory(Object inv) {
        return invClass.isInstance(inv) ? invClass.cast(inv) : null;
    }

    @Nullable
    @Override
    public Object createServerGui(EntityPlayer player, World world, int x, int y, int z, ForgeDirection face) {
        ItemStack item = getItem(player, x);
        if (item == null || !(item.getItem() instanceof IItemInventory)) {
            return null;
        }
        T inv = getInventory(((IItemInventory) item.getItem()).getInventory(item, world, x, y, z, player));
        if (inv == null) {
            return null;
        }
        Object gui = createServerGui(player, inv);
        if (gui instanceof AEBaseContainer) {
            ContainerOpenContext ctx = new ContainerOpenContext(inv);
            ctx.setWorld(world);
            ctx.setX(x);
            ctx.setY(y);
            ctx.setZ(z);
            ctx.setSide(face);
            ((AEBaseContainer) gui).setOpenContext(ctx);
        }
        return gui;
    }

    private ItemStack getItem(EntityPlayer player, int x) {
        if (x == -1) {
            return player.getCurrentEquippedItem();
        }
        // A bridge-encoded coordinate can reach this plain factory through AE2's own gui-switching machinery
        // (PrimaryGui / PacketSwitchGuis), where the coordinate is treated as an inventory slot index. Reading
        // it blindly would throw ArrayIndexOutOfBounds, so route it through the bridge aware helper instead.
        // Without this, clicking "back to terminal" on a terminal that lives in a Baubles slot kills the server.
        if (BaublesUtil.isBridgeSlot(x)) {
            return BaublesUtil.getStackFromBridge(player, x);
        }
        if (x < 0 || x >= player.inventory.mainInventory.length) {
            return null;
        }
        return player.inventory.getStackInSlot(x);
    }

    @Nullable
    protected abstract Object createServerGui(EntityPlayer player, T inv);

    @Nullable
    @Override
    public Object createClientGui(EntityPlayer player, World world, int x, int y, int z, ForgeDirection face) {
        ItemStack item = getItem(player, x);
        if (item == null || !(item.getItem() instanceof IItemInventory)) {
            return null;
        }
        T inv = getInventory(((IItemInventory) item.getItem()).getInventory(item, world, x, y, z, player));
        // No explicit closeScreen() here: see ItemGuiBridge#createClientGui. Vanilla already replaces the
        // current screen when the open-window packet arrives, and closing it again mid-switch bounces the
        // container and traps the gui in an open/close loop.
        return inv != null ? createClientGui(player, inv) : null;
    }

    @Nullable
    protected abstract Object createClientGui(EntityPlayer player, T inv);
}
