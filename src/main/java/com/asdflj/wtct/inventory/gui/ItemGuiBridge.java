package com.asdflj.wtct.inventory.gui;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import org.apache.commons.lang3.tuple.ImmutablePair;

import com.asdflj.wtct.integration.Mods;
import com.asdflj.wtct.util.BaublesUtil;

public abstract class ItemGuiBridge<T> implements IGuiFactory {

    protected final Class<T> invClass;

    /**
     * The raw bridge-encoded coordinate the current {@code createXxxGui} call resolves from. Subclasses use
     * it to hand the coordinate to the inventory object (which needs it to write the terminal NBT back to
     * the correct slot, bauble or main inventory).
     */
    protected int bridgeSlot;

    ItemGuiBridge(Class<T> invClass) {
        this.invClass = invClass;
    }

    protected T getInventory(Object inv) {
        return invClass.isInstance(inv) ? invClass.cast(inv) : null;
    }

    @Override
    public Object createServerGui(EntityPlayer player, World world, int x, int y, int z, ForgeDirection face) {
        ImmutablePair<GuiBridgeInvType, Integer> result = GuiBridgeInvType.decode(x);
        this.bridgeSlot = x;
        ItemStack is = null;
        if (result.left == GuiBridgeInvType.PLAYER_INV) {
            is = player.inventory.getStackInSlot(result.right);
        } else if (Mods.BAUBLES.isModLoaded()) {
            is = BaublesUtil.getBaublesInv(player)
                .getStackInSlot(result.right);
        }
        if (is == null) return null;
        T obj = getInventory(is.getItem());
        if (obj == null) return null;
        return createServerGui(player, obj, is);
    }

    @Nullable
    @Override
    public Object createClientGui(EntityPlayer player, World world, int x, int y, int z, ForgeDirection face) {
        ImmutablePair<GuiBridgeInvType, Integer> result = GuiBridgeInvType.decode(x);
        this.bridgeSlot = x;
        ItemStack is = null;
        if (result.left == GuiBridgeInvType.PLAYER_INV) {
            is = player.inventory.getStackInSlot(result.right);
        } else if (Mods.BAUBLES.isModLoaded()) {
            is = BaublesUtil.getBaublesInv(player)
                .getStackInSlot(result.right);
        }
        if (is == null) return null;
        T obj = getInventory(is.getItem());
        // No explicit closeScreen() here: vanilla replaces the current screen by itself when the server's
        // open-window packet arrives (Minecraft#displayGuiScreen closes the old one). Closing it a second time
        // sends a "close container" packet while the server is still switching, which tears the freshly opened
        // container straight back down; the gui then reopens on the next tick and repeats, producing noticeable
        // input lag and the mouse getting recaptured into the centre of the screen.
        return obj != null ? createClientGui(player, obj, is) : null;
    }

    @Nullable
    protected abstract Object createClientGui(EntityPlayer player, T inv, ItemStack item);

    @Nullable
    protected abstract Object createServerGui(EntityPlayer player, T inv, ItemStack item);
}
