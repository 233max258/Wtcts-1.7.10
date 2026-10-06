package com.asdflj.wtct.common.item.card;

import java.util.List;

import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;

import com.asdflj.wtct.Wtct;
import com.asdflj.wtct.api.Constants;
import com.asdflj.wtct.common.item.BaseItem;
import com.asdflj.wtct.common.tabs.WtctTabs;
import com.asdflj.wtct.inventory.WcwtUpgradesInventory;
import com.asdflj.wtct.loader.IRegister;
import com.asdflj.wtct.util.NameConst;

import appeng.api.AEApi;
import appeng.api.config.FuzzyMode;
import appeng.api.config.Upgrades;
import appeng.api.definitions.IItemDefinition;
import appeng.api.implementations.items.IUpgradeModule;
import appeng.util.Platform;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * One of the terminal's upgrade cards.
 *
 * <p>
 * The five cards and where they come from:
 * <ul>
 * <li>{@link Kind#IMPORT} / {@link Kind#EXPORT} / {@link Kind#BLOCK_PICKER}: AE2ImportExportCard
 * (1.21, MIT) - the addon the comprehensive terminal's card panel was modelled on. Import dumps
 * marked player slots into the ME system, export pushes configured items the other way, and the
 * block picker pulls the block you are looking at out of the network.
 * <li>{@link Kind#MAGNET}: AE2WTLib's magnet card - pulls nearby drops to the player or into the
 * network. Reuses this mod's own {@code MagnetObject}, which the backpack terminal already uses.
 * <li>{@link Kind#QUANTUM_BRIDGE}: AE2WTLib's quantum bridge card - ties the terminal to a quantum
 * network bridge, which in 1.7.10 is the terminal's existing "infinity booster" link.
 * </ul>
 *
 * <p>
 * A card is a plain {@link IUpgradeModule} that reports <em>no</em> AE2 upgrade type: cards are for
 * this terminal's upgrade column, and reporting a type would let players stuff them into any AE2
 * machine. {@code WcwtUpgradesInventory} recognises them by class instead.
 *
 * <p>
 * The two configured cards keep their settings in their own item NBT, mirroring the addon's data
 * components: the marked player slots, the ghost filter, and the card's own upgrade slots (the cards
 * inside the card - fuzzy, inverter, speed, crafting, capacity).
 */
public class ItemWcwtUpgradeCard extends BaseItem implements IUpgradeModule, IRegister<ItemWcwtUpgradeCard> {

    /** NBT key of the marked player-inventory slots. */
    private static final String TAG_SELECTED_SLOTS = "SelectedSlots";
    /** NBT name of the ghost filter inventory. */
    public static final String INV_FILTER = "Filter";
    /** NBT name of the card's own upgrade slots. */
    public static final String INV_UPGRADES = "CardUpgrades";
    /** NBT name of the magnet card's pickup filter (which drops the magnet pulls at all). */
    public static final String INV_MAGNET_PICKUP = "MagnetPickup";
    /** NBT name of the magnet card's insert filter (which of those go straight into the network). */
    public static final String INV_MAGNET_INSERT = "MagnetInsert";
    /** The two filters' include/exclude flag: true = whitelist, false = blacklist. */
    private static final String TAG_MAGNET_PICKUP_MODE = "MagnetPickupMode";
    private static final String TAG_MAGNET_INSERT_MODE = "MagnetInsertMode";
    /**
     * The fuzzy card's matching mode, under AE2's own key - {@code FuzzyMode.fromItemStack} reads
     * exactly this tag, so the value round-trips through AE2's own helper.
     */
    public static final String TAG_FUZZY_MODE = "FuzzyMode";
    /** WTLib's magnet filter size: three rows of nine. */
    public static final int MAGNET_FILTER_SLOTS = 27;

    /**
     * The card's identity. {@code texture} doubles as the item's unlocalised name so the two can
     * never drift apart.
     */
    public enum Kind {

        IMPORT("wcwt_card_import", 4),
        EXPORT("wcwt_card_export", 5),
        BLOCK_PICKER("wcwt_card_block_picker", 0),
        MAGNET("wcwt_card_magnet", 0),
        QUANTUM_BRIDGE("wcwt_card_quantum_bridge", 0),
        /**
         * WCWT's 能源卡: the one card a terminal wants several of. Each one raises the terminal's battery cap by
         * {@link #ENERGY_POWER_FACTOR}, up to {@link #MAX_ENERGY_CARDS} of them.
         */
        ENERGY("wcwt_card_energy", 0);

        public final String texture;
        /** Slots the card's own upgrade column has; 0 = the card takes no upgrades. */
        public final int upgradeSlots;

        Kind(final String texture, final int upgradeSlots) {
            this.texture = texture;
            this.upgradeSlots = upgradeSlots;
        }

        /** True for the cards that carry a ghost filter and marked player slots. */
        public boolean isConfigured() {
            return this == IMPORT || this == EXPORT;
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Filter geometry, copied from AE2ImportExportCard's UpgradeHost.
    // ---------------------------------------------------------------------------------------------

    /** Filter slots without any capacity card: two rows of nine. */
    public static final int BASE_FILTER_SLOTS = 18;
    /** Extra filter slots per capacity card. */
    public static final int FILTER_SLOTS_PER_CAPACITY_CARD = 9;
    public static final int MAX_CAPACITY_CARDS = 3;
    public static final int MAX_FILTER_SLOTS = BASE_FILTER_SLOTS + MAX_CAPACITY_CARDS * FILTER_SLOTS_PER_CAPACITY_CARD;
    /** Marked-slot array length: the player's 36 inventory slots plus the four armour slots. */
    public static final int SELECTED_SLOT_COUNT = 40;

    private final Kind kind;

    public ItemWcwtUpgradeCard(final Kind kind) {
        this.kind = kind;
        setUnlocalizedName(kind.texture);
        setTextureName(
            Wtct.resource(kind.texture)
                .toString());
    }

    public Kind getKind() {
        return this.kind;
    }

    @Override
    public ItemWcwtUpgradeCard register() {
        GameRegistry.registerItem(this, this.kind.texture, Wtct.MODID);
        setCreativeTab(WtctTabs.INSTANCE);
        return this;
    }

    /** This terminal's cards declare no AE2 upgrade type - see the class comment. */
    @Override
    public Upgrades getType(final ItemStack is) {
        return null;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addCheckedInformation(final ItemStack stack, final EntityPlayer player, final List<String> toolTip,
        final boolean displayMoreInfo) {
        toolTip.add(EnumChatFormatting.GRAY + I18n.format(NameConst.TT_KEY + this.kind.texture + ".desc"));
        // Every card gets its usage line: these cards do nothing on their own, so the tooltip is the
        // only place that explains where they go and what button drives them.
        toolTip.add(EnumChatFormatting.DARK_GRAY + I18n.format(NameConst.TT_KEY + this.kind.texture + ".hint"));
    }

    // ---------------------------------------------------------------------------------------------
    // Static helpers - the cards are read by the container, the screen and the ticker, so everything
    // is reached through the item stack.
    // ---------------------------------------------------------------------------------------------

    /** The card kind of a stack, or null when the stack is not one of these cards. */
    public static Kind kindOf(final ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof ItemWcwtUpgradeCard card)) {
            return null;
        }
        return card.kind;
    }

    public static boolean is(final ItemStack stack, final Kind kind) {
        return kindOf(stack) == kind;
    }

    /** The ghost filter inventory of a configured card. */
    public static CardInventory filter(final ItemStack card) {
        return new CardInventory(card, INV_FILTER, MAX_FILTER_SLOTS, 1);
    }

    /** The magnet card's pickup filter - the drops the magnet is willing to pull. */
    public static CardInventory magnetPickupFilter(final ItemStack card) {
        return new CardInventory(card, INV_MAGNET_PICKUP, MAGNET_FILTER_SLOTS, 1);
    }

    /**
     * The magnet card's insert filter - which of the pulled drops go straight into the ME network
     * instead of the player's inventory. WTLib keeps the two filters and their two include/exclude
     * flags on the terminal; this port keeps them on the card, so the configuration travels with the
     * card the way the import/export filters do.
     */
    public static CardInventory magnetInsertFilter(final ItemStack card) {
        return new CardInventory(card, INV_MAGNET_INSERT, MAGNET_FILTER_SLOTS, 1);
    }

    /** True when the pickup filter is a whitelist; the default is WTLib's blacklist. */
    public static boolean magnetPickupWhitelist(final ItemStack card) {
        return flag(card, TAG_MAGNET_PICKUP_MODE);
    }

    public static void setMagnetPickupWhitelist(final ItemStack card, final boolean whitelist) {
        setFlag(card, TAG_MAGNET_PICKUP_MODE, whitelist);
    }

    public static boolean magnetInsertWhitelist(final ItemStack card) {
        return flag(card, TAG_MAGNET_INSERT_MODE);
    }

    public static void setMagnetInsertWhitelist(final ItemStack card, final boolean whitelist) {
        setFlag(card, TAG_MAGNET_INSERT_MODE, whitelist);
    }

    private static boolean flag(final ItemStack card, final String key) {
        return card != null && Platform.openNbtData(card)
            .getBoolean(key);
    }

    private static void setFlag(final ItemStack card, final String key, final boolean value) {
        if (card != null) {
            Platform.openNbtData(card)
                .setBoolean(key, value);
        }
    }

    /**
     * WTLib's filter rule ({@code IPartitionList.matchesFilter} with an {@code IncludeExclude}): an
     * empty whitelist matches nothing and an empty blacklist matches everything, which is what
     * {@code whitelist == matches} gives on its own.
     *
     * <p>
     * The entry matches on the item <em>and its damage</em>, ignoring NBT - which is what WTLib's
     * IGNORE_ALL means for an {@code AEItemKey} on a platform whose items are one class each. 1.7.10
     * GTNH is not that platform: nearly everything is {@code gt.metaitem.01} and only the damage tells
     * tungsten from sulfur, so an item-only test made every entry match every material. A ghost with
     * the ore dictionary's wildcard damage keeps matching any damage of its item, the way AE2's own
     * {@code Platform.isSameItem} treats it.
     */
    public static boolean passesFilter(final CardInventory filter, final ItemStack stack, final boolean whitelist) {
        if (filter == null || stack == null) {
            return false;
        }
        boolean matches = false;
        for (int i = 0; i < filter.getSizeInventory(); i++) {
            final ItemStack ghost = filter.getStackInSlot(i);
            if (ghost != null && sameFilterItem(ghost, stack)) {
                matches = true;
                break;
            }
        }
        return whitelist == matches;
    }

    /** The filter's item identity: the same item and damage, wildcard damage matching any damage. */
    private static boolean sameFilterItem(final ItemStack ghost, final ItemStack stack) {
        if (ghost.getItem() != stack.getItem()) {
            return false;
        }
        if (ghost.getItemDamage() == stack.getItemDamage()) {
            return true;
        }
        return ghost.getItemDamage() == net.minecraftforge.oredict.OreDictionary.WILDCARD_VALUE
            || stack.getItemDamage() == net.minecraftforge.oredict.OreDictionary.WILDCARD_VALUE;
    }

    /**
     * The card's own upgrade slots. Empty inventory for the card kinds that take no upgrades, so
     * callers never have to null check.
     */
    public static CardInventory upgrades(final ItemStack card) {
        final Kind kind = kindOf(card);
        final int size = kind == null ? 0 : kind.upgradeSlots;
        return new CardUpgradeInventory(card, Math.max(size, 0));
    }

    /** How many capacity cards are installed in the card's own upgrade slots (capped at 3). */
    public static int capacityCards(final ItemStack card) {
        final CardInventory inv = upgrades(card);
        int count = 0;
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            final ItemStack is = inv.getStackInSlot(i);
            if (is != null && capacityCard().isSameAs(is)) {
                count++;
            }
        }
        return Math.min(MAX_CAPACITY_CARDS, count);
    }

    /** Active filter size: 18 without capacity cards, 27/36/45 with one/two/three. */
    public static int filterSlotCount(final ItemStack card) {
        return BASE_FILTER_SLOTS + capacityCards(card) * FILTER_SLOTS_PER_CAPACITY_CARD;
    }

    /** True when an AE2 card is installed in the card's own upgrade slots. */
    public static boolean hasUpgrade(final ItemStack card, final IItemDefinition definition) {
        final CardInventory inv = upgrades(card);
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            final ItemStack is = inv.getStackInSlot(i);
            if (is != null && definition.isSameAs(is)) {
                return true;
            }
        }
        return false;
    }

    /** The fuzzy card's matching mode; {@code IGNORE_ALL} unless the card says otherwise. */
    public static FuzzyMode fuzzyMode(final ItemStack card) {
        return card == null ? FuzzyMode.IGNORE_ALL : FuzzyMode.fromItemStack(card);
    }

    public static void setFuzzyMode(final ItemStack card, final FuzzyMode mode) {
        if (card != null) {
            Platform.openNbtData(card)
                .setString(TAG_FUZZY_MODE, mode.name());
        }
    }

    /** The marked player slots - a 0 marks nothing, and export uses it as a filter index. */
    public static int[] selectedSlots(final ItemStack card) {
        final NBTTagCompound data = Platform.openNbtData(card);
        final int[] slots = data.getIntArray(TAG_SELECTED_SLOTS);
        if (slots.length == SELECTED_SLOT_COUNT) {
            return slots;
        }
        final int[] fresh = new int[SELECTED_SLOT_COUNT];
        System.arraycopy(slots, 0, fresh, 0, Math.min(slots.length, SELECTED_SLOT_COUNT));
        return fresh;
    }

    public static void setSelectedSlots(final ItemStack card, final int[] slots) {
        final NBTTagCompound data = Platform.openNbtData(card);
        final int[] trimmed = new int[SELECTED_SLOT_COUNT];
        System.arraycopy(slots, 0, trimmed, 0, Math.min(slots.length, SELECTED_SLOT_COUNT));
        data.setIntArray(TAG_SELECTED_SLOTS, trimmed);
    }

    /**
     * True when the stack may go into a card's own upgrade slot: AE2's own card rule, which is what
     * the cards inside the card are.
     */
    public static boolean isNestedUpgrade(final ItemStack stack) {
        return stack != null && stack.getItem() instanceof IUpgradeModule module && module.getType(stack) != null;
    }

    /** Convenience for the two places that need AE2's capacity card definition. */
    public static IItemDefinition capacityCard() {
        return AEApi.instance()
            .definitions()
            .materials()
            .cardCapacity();
    }

    /** Convenience for the fuzzy card, which the card's matching mode belongs to. */
    public static IItemDefinition fuzzyCard() {
        return AEApi.instance()
            .definitions()
            .materials()
            .cardFuzzy();
    }

    // ---------------------------------------------------------------------------------------------
    // Reading a terminal's card column from the terminal item alone.
    // ---------------------------------------------------------------------------------------------

    /**
     * Cards installed in a terminal, straight from the terminal item's NBT.
     *
     * <p>
     * The terminal's upgrade column is persisted under {@code UPGRADES} as {@code #0..#n} (see
     * {@code WcwtUpgradesInventory}), so anything holding the terminal item - the client tick, a
     * screen, a range check - can see its cards without a container. The live inventory and this
     * view are the same data, written back on every change.
     */
    private static NBTTagCompound upgradesTag(final ItemStack terminal) {
        if (terminal == null) {
            return null;
        }
        final NBTTagCompound data = Platform.openNbtData(terminal);
        if (!data.hasKey(Constants.UPGRADES)) {
            return null;
        }
        return data.getCompoundTag(Constants.UPGRADES);
    }

    /** How many cards of that kind the terminal holds. */
    public static int installedCount(final ItemStack terminal, final Kind kind) {
        final NBTTagCompound tag = upgradesTag(terminal);
        if (tag == null) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < WcwtUpgradesInventory.CARD_SLOTS; i++) {
            final ItemStack is = Platform.loadItemStackFromNBT(tag.getCompoundTag("#" + i));
            if (is != null && kindOf(is) == kind) {
                count++;
            }
        }
        return count;
    }

    public static boolean isInstalled(final ItemStack terminal, final Kind kind) {
        return installedCount(terminal, kind) > 0;
    }

    // ---------------------------------------------------------------------------------------------
    // 能源卡: the terminal's power cap.
    // ---------------------------------------------------------------------------------------------

    /** How many energy cards a terminal takes. */
    public static final int MAX_ENERGY_CARDS = 5;
    /** What each of them does to the terminal's power cap. */
    public static final int ENERGY_POWER_FACTOR = 10;

    /**
     * The multiplier the installed energy cards put on the terminal's battery cap: {@code 10^cards}, so 1 / 10 / 100 /
     * 1k / 10k / 100k for 0..5 cards.
     *
     * <p>
     * Read straight out of the terminal's NBT instead of going through {@link #installedCount}: AE2 asks for the cap
     * from the item's durability bar, which is drawn every frame, and materialising the card stacks that often is not
     * worth it. The stack's id is looked at in both shapes it takes on this version - a numeric short, or the registry
     * name once GTNH's item ids run past what a short can hold.
     */
    public static int energyPowerMultiplier(final ItemStack terminal) {
        final NBTTagCompound tag = upgradesTag(terminal);
        if (tag == null) {
            return 1;
        }
        final String energyCardName = Wtct.MODID + ":" + Kind.ENERGY.texture;
        final int energyCardItemId = energyCardItemId();
        int cards = 0;
        for (int i = 0; i < WcwtUpgradesInventory.CARD_SLOTS; i++) {
            final NBTTagCompound slot = tag.getCompoundTag("#" + i);
            if (slot == null) {
                continue;
            }
            final boolean energyCard = slot.hasKey("id", net.minecraftforge.common.util.Constants.NBT.TAG_STRING)
                ? energyCardName.equals(slot.getString("id"))
                : energyCardItemId >= 0 && (slot.getShort("id") & 0xFFFF) == energyCardItemId;
            if (energyCard && ++cards >= MAX_ENERGY_CARDS) {
                break;
            }
        }
        int multiplier = 1;
        for (int i = 0; i < cards; i++) {
            multiplier *= ENERGY_POWER_FACTOR;
        }
        return multiplier;
    }

    /** The energy card's numeric item id, resolved once; -1 while the item is not registered (yet). */
    private static int energyCardItemId = Integer.MIN_VALUE;

    private static int energyCardItemId() {
        if (energyCardItemId == Integer.MIN_VALUE) {
            final Item card = GameRegistry.findItem(Wtct.MODID, Kind.ENERGY.texture);
            energyCardItemId = card == null ? -1 : Item.getIdFromItem(card);
        }
        return energyCardItemId;
    }

    /** The first card of that kind, or null - the one the ticker works from. */
    public static ItemStack firstInstalled(final ItemStack terminal, final Kind kind) {
        final NBTTagCompound tag = upgradesTag(terminal);
        if (tag == null) {
            return null;
        }
        for (int i = 0; i < WcwtUpgradesInventory.CARD_SLOTS; i++) {
            final ItemStack is = Platform.loadItemStackFromNBT(tag.getCompoundTag("#" + i));
            if (is != null && kindOf(is) == kind) {
                return is;
            }
        }
        return null;
    }

    /**
     * The live card stack inside a terminal's upgrade column, or null when the terminal is not
     * hosted or holds no such card.
     *
     * <p>
     * A screen that <em>edits</em> a card has to take the stack from the column inventory and not
     * from the terminal's NBT: {@link #firstInstalled} deserialises a detached copy, and every write
     * to its NBT is thrown away when the column saves its own (unchanged) stacks back. The column's
     * stacks are live objects, so changing this one and calling {@code markDirty()} on the column is
     * what makes the configuration stick.
     */
    public static ItemStack liveCard(final IInventory column, final Kind kind) {
        if (column == null) {
            return null;
        }
        final int cards = Math.min(column.getSizeInventory(), WcwtUpgradesInventory.CARD_SLOTS);
        for (int i = 0; i < cards; i++) {
            final ItemStack is = column.getStackInSlot(i);
            if (is != null && kindOf(is) == kind) {
                return is;
            }
        }
        return null;
    }

    /**
     * The live card stack inside the terminal's upgrade inventory, if the terminal is currently
     * hosted. Used by the container/screen so a change to the card's configuration is written back
     * into the terminal item.
     */
    public static boolean isCardColumn(final ItemStack stack) {
        return kindOf(stack) != null;
    }
}
