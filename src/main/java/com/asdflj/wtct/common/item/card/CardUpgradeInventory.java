package com.asdflj.wtct.common.item.card;

import net.minecraft.item.ItemStack;

import appeng.api.AEApi;
import appeng.api.definitions.IItemDefinition;

/**
 * The card's own upgrade slots (the cards inside the card).
 *
 * <p>
 * Validity is AE2's card rule, so fuzzy, inverter, speed and capacity cards can all be nested; which
 * of them do anything is up to the card that reads them. The counts are the ones
 * AE2ImportExportCard registers with AE2's own upgrade registry
 * ({@code Upgrades.add(AEItems.CAPACITY_CARD, CARD, 3)}, {@code FUZZY/INVERTER/CRAFTING/SPEED ... 1}),
 * which is what AE2 would enforce for it: three capacity cards, one of everything else. Without that
 * registry in 1.7.10 the limits have to be checked here instead.
 */
public class CardUpgradeInventory extends CardInventory {

    /** What the addon allows per card, from its {@code Upgrades.add(..., max)} calls. */
    private static final int CAPACITY_LIMIT = 3;

    public CardUpgradeInventory(final ItemStack card, final int size) {
        super(card, ItemWcwtUpgradeCard.INV_UPGRADES, Math.max(size, 0), 1);
    }

    @Override
    public boolean isItemValidForSlot(final int slot, final ItemStack stack) {
        if (!ItemWcwtUpgradeCard.isNestedUpgrade(stack)) {
            return false;
        }
        return this.installed(stack) < limitFor(stack);
    }

    /** How many of that card are already nested, ignoring the slot being asked about. */
    private int installed(final ItemStack wanted) {
        int count = 0;
        for (int i = 0; i < this.getSizeInventory(); i++) {
            final ItemStack is = this.getStackInSlot(i);
            if (is != null && is.getItem() == wanted.getItem()) {
                count++;
            }
        }
        return count;
    }

    private static int limitFor(final ItemStack stack) {
        final IItemDefinition capacity = AEApi.instance()
            .definitions()
            .materials()
            .cardCapacity();
        return capacity.isSameAs(stack) ? CAPACITY_LIMIT : 1;
    }
}
