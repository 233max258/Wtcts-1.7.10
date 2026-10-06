package com.asdflj.wtct.inventory;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.asdflj.wtct.util.BaublesUtil;

import appeng.api.AEApi;
import appeng.core.AELog;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.util.Platform;

public class ItemPatternRefillInventory extends AppEngInternalInventory {

    private final ItemStack is;
    private final String name;
    private final EntityPlayer player;
    private final int slot;

    public ItemPatternRefillInventory(ItemStack is, String name, int size, int maxStack, EntityPlayer player,
        int slot) {
        super(null, size, maxStack);
        this.name = name;
        this.is = is;
        this.player = player;
        this.slot = slot;
        this.readFromNBT(Platform.openNbtData(this.is), this.name);
    }

    /**
     * The terminal item this column reads its contents from and writes them back to. The screens keep
     * a reference to a stack of their own, but that one is captured when the container is built and
     * never sees the server's later writes; this is the live stack the column actually owns, so a
     * screen that wants the current state (the magnet's mode tooltip, above all) has to ask here.
     */
    public ItemStack getHostStack() {
        return this.is;
    }

    @Override
    protected void writeToNBT(final NBTTagCompound target) {
        for (int x = 0; x < this.getSizeInventory(); x++) {
            try {
                final NBTTagCompound c = new NBTTagCompound();

                if (this.inv[x] != null) {
                    Platform.writeItemStackToNBT(this.inv[x], c);
                }

                target.setTag("#" + x, c);
            } catch (final Exception ignored) {}
        }
    }

    @Override
    public void readFromNBT(final NBTTagCompound target) {
        for (int x = 0; x < this.getSizeInventory(); x++) {
            try {
                final NBTTagCompound c = target.getCompoundTag("#" + x);

                if (c != null) {
                    this.inv[x] = Platform.loadItemStackFromNBT(c);
                }
            } catch (final Exception e) {
                AELog.debug(e);
            }
        }
    }

    @Override
    public void markDirty() {
        this.writeToNBT(Platform.openNbtData(is), this.name);
        if (Platform.isServer()) BaublesUtil.setStackFromBridge(this.player, this.slot, this.is);
    }

    @Override
    public boolean isItemValidForSlot(final int i, final ItemStack itemstack) {
        return i == 0 && getStackInSlot(0) == null
            && AEApi.instance()
                .definitions()
                .materials()
                .basicCard()
                .isSameAs(itemstack);
    }
}
