package com.asdflj.wtct.common.item.card;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import appeng.core.AELog;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.util.Platform;

/**
 * An inventory that lives in an {@link ItemStack}'s NBT - the card's own storage.
 *
 * <p>
 * The two card types that carry configuration (import / export) keep three things on the card item
 * itself, exactly where AE2ImportExportCard keeps its data components: the marked player-inventory
 * slots, the ghost filter, and the card's own upgrade slots. Nothing here is tied to a player slot,
 * so unlike {@code ItemBiggerAppEngInventory} there is no bridge write - the card item is serialised
 * by whoever holds it (the terminal's upgrade inventory) when that inventory is saved.
 */
public class CardInventory extends AppEngInternalInventory {

    private final ItemStack card;
    private final String name;

    public CardInventory(final ItemStack card, final String name, final int size, final int maxStack) {
        super(null, size, maxStack);
        this.card = card;
        this.name = name;
        this.readFromNBT(Platform.openNbtData(card), name);
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
        this.writeToNBT(Platform.openNbtData(this.card), this.name);
    }

    @Override
    public void markDirty(final int slotIndex) {
        this.markDirty();
    }
}
