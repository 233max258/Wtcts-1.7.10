package com.asdflj.wtct.inventory;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.asdflj.wtct.api.Constants;
import com.asdflj.wtct.common.item.card.ItemWcwtUpgradeCard;

import appeng.api.AEApi;
import appeng.util.Platform;

/**
 * The upgrade inventory behind the comprehensive work terminal's right-hand upgrade panel: a column of
 * card slots plus one singularity slot, exactly what WCWT/WTLib's {@code ScrollingUpgradesPanel} shows.
 *
 * <p>
 * WCWT stores the singularity first and the cards after it ({@code allUpgradeSlots} = the
 * {@code SINGULARITY} slots plus the {@code UPGRADE} slots, singularity first). This port keeps the
 * <em>opposite</em> data order on purpose: the slots the terminal already had as "the pattern refill
 * card" stay at index 0 upward, so a terminal saved before the panel existed keeps its card where it
 * was instead of landing in the singularity slot. The panel draws them in WCWT's order (singularity
 * on top), which is a presentation detail - see {@code GuiWcwtUpgradePanel}.
 *
 * <p>
 * Capacities follow WCWT's shape, not its numbers: WTLib registers 20 upgrade slots because it ships
 * 20 cards, while this port only has the cards 1.7.10 can offer, so the column is
 * {@link #CARD_SLOTS} long. The panel shows the first {@code max(2, terminal rows)} of them and
 * scrolls for the rest, which is WTLib's own {@code setMaxRows(Math.max(2, getVisibleRows()))}.
 */
public class WcwtUpgradesInventory extends ItemPatternRefillInventory {

    /** Card slots: indices 0..CARD_SLOTS-1. */
    public static final int CARD_SLOTS = 8;
    /** The quantum-entangled singularity, after the cards (see the class comment on the ordering). */
    public static final int SINGULARITY_SLOT = CARD_SLOTS;
    public static final int SIZE = CARD_SLOTS + 1;

    public WcwtUpgradesInventory(final ItemStack is, final EntityPlayer player, final int slot) {
        super(is, Constants.UPGRADES, SIZE, 1, player, slot);
    }

    @Override
    public boolean isItemValidForSlot(final int slot, final ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return false;
        }
        if (slot == SINGULARITY_SLOT) {
            return AEApi.instance()
                .definitions()
                .materials()
                .qESingularity()
                .isSameAs(stack);
        }
        if (slot < 0 || slot >= CARD_SLOTS || !isUpgradeCard(stack)) {
            return false;
        }
        // One card of each kind per terminal: an import card is not something a terminal wants two of,
        // and the card's own screen assumes it edits "the" card of that kind. The energy card is the
        // exception - a terminal takes up to MAX_ENERGY_CARDS of them, each raising its power cap.
        if (stack.getItem() instanceof ItemWcwtUpgradeCard) {
            if (ItemWcwtUpgradeCard.kindOf(stack) == ItemWcwtUpgradeCard.Kind.ENERGY) {
                return cardsOfKind(ItemWcwtUpgradeCard.Kind.ENERGY, slot) < ItemWcwtUpgradeCard.MAX_ENERGY_CARDS;
            }
            return !hasCardOfKind(stack, slot);
        }
        return true;
    }

    /** How many cards of that kind sit in this column, ignoring one slot (the one being validated). */
    private int cardsOfKind(final ItemWcwtUpgradeCard.Kind kind, final int exceptSlot) {
        int count = 0;
        for (int i = 0; i < CARD_SLOTS; i++) {
            if (i == exceptSlot) {
                continue;
            }
            final ItemStack other = this.getStackInSlot(i);
            if (other != null && ItemWcwtUpgradeCard.kindOf(other) == kind) {
                count++;
            }
        }
        return count;
    }

    /**
     * True when an equal card already sits in another slot of this column. This mod's cards report no
     * AE2 upgrade type (they belong to this terminal alone), so their per-terminal maximum - one - is
     * spelled out here rather than looked up in AE2's registry.
     */
    private boolean hasCardOfKind(final ItemStack stack, final int exceptSlot) {
        for (int i = 0; i < CARD_SLOTS; i++) {
            if (i == exceptSlot) {
                continue;
            }
            final ItemStack other = this.getStackInSlot(i);
            if (other == null) {
                continue;
            }
            if (sameKind(stack, other)) {
                return true;
            }
        }
        return false;
    }

    /** Card identity: the same item, damage and NBT - two import cards are the same kind. */
    private static boolean sameKind(final ItemStack a, final ItemStack b) {
        return a.getItem() == b.getItem() && a.getItemDamage() == b.getItemDamage()
            && ItemStack.areItemStackTagsEqual(a, b);
    }

    /**
     * The only cards this column accepts: the mod's terminal cards ({@link ItemWcwtUpgradeCard}) and
     * AE2's <em>Basic Card</em>, which is the pattern refill card the terminal already uses (see
     * {@link #hasPatternRefillCard()}).
     *
     * <p>
     * AE2's other cards are refused on purpose. They are real upgrades for AE2's own machines, but a
     * wireless terminal item honours none of them: energy and infinity boosting are NBT flags baked in
     * by recipes (see {@code WirelessObject#hasEnergyCard}), the settings of an import/export card live
     * in <em>its own</em> upgrade slots, and nothing in this terminal reads acceleration, capacity,
     * redstone or crafting cards. Accepting them only filled the column with cards that did nothing.
     */
    private static boolean isUpgradeCard(final ItemStack stack) {
        if (stack.getItem() instanceof ItemWcwtUpgradeCard) {
            return true;
        }
        return AEApi.instance()
            .definitions()
            .materials()
            .basicCard()
            .isSameAs(stack);
    }

    /** True when a pattern refill card (AE2's Basic Card) sits in any card slot. */
    public boolean hasPatternRefillCard() {
        for (int i = 0; i < CARD_SLOTS; i++) {
            final ItemStack is = this.getStackInSlot(i);
            if (is != null && AEApi.instance()
                .definitions()
                .materials()
                .basicCard()
                .isSameAs(is)) {
                return true;
            }
        }
        return false;
    }

    /** The singularity currently installed, or null. */
    public ItemStack getSingularity() {
        return this.getStackInSlot(SINGULARITY_SLOT);
    }

    /**
     * Whether the terminal item itself carries the entangled singularity, read straight from its NBT - the
     * same {@code #SINGULARITY_SLOT} entry {@link #getSingularity()} returns, seen without a container.
     *
     * <p>
     * The quantum bridge card is only one half of the link: the card bridges the terminal to the quantum
     * network ring, and the singularity is what it bridges to (see the card's tooltip). So anything asking
     * "can this terminal reach its network from anywhere" - the range check above all - has to ask for both,
     * otherwise a card alone would hand out a range the terminal has nothing to reach with.
     */
    public static boolean hasSingularity(final ItemStack terminal) {
        if (terminal == null) {
            return false;
        }
        final NBTTagCompound data = Platform.openNbtData(terminal);
        if (!data.hasKey(Constants.UPGRADES)) {
            return false;
        }
        final ItemStack is = Platform.loadItemStackFromNBT(
            data.getCompoundTag(Constants.UPGRADES)
                .getCompoundTag("#" + SINGULARITY_SLOT));
        return is != null && AEApi.instance()
            .definitions()
            .materials()
            .qESingularity()
            .isSameAs(is);
    }

    /** Card slot {@code i} of the panel, in panel order (index 0 is the first card). */
    public ItemStack getCard(final int i) {
        return i >= 0 && i < CARD_SLOTS ? this.getStackInSlot(i) : null;
    }
}
