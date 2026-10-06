package com.asdflj.wtct.client.gui.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import com.asdflj.wtct.api.Constants;
import com.asdflj.wtct.common.item.card.CardInventory;
import com.asdflj.wtct.common.item.card.ItemWcwtUpgradeCard;
import com.asdflj.wtct.inventory.IPatternTerminal;

import appeng.api.implementations.guiobjects.IGuiItemObject;
import appeng.api.storage.ITerminalHost;
import appeng.container.AEBaseContainer;
import appeng.container.slot.AppEngSlot;
import appeng.container.slot.SlotFake;

/**
 * The import / export card's configuration screen: the card's ghost filter on top, the card's own
 * upgrade slots on the right, and the player's inventory below with a mark on every slot the card
 * works on.
 *
 * <p>
 * A direct port of AE2ImportExportCard's {@code UpgradeContainerMenu} and {@code UpgradeScreen}
 * geometry: the filter is {@code 9 x 2} base slots at (23, 29) growing by a row per capacity card
 * (up to 45), the player's rows sit under it ({@code bottom 84} / {@code bottom 26}), the armour
 * column runs down the left edge at x=2, and the card's upgrade column is the same WTLib panel the
 * terminal itself uses, anchored at {@code right 2, top 0}.
 *
 * <p>
 * Everything the screen changes lives on the <em>card</em>, under the same NBT the addon keeps in
 * its data components: the marked slots and the ghost filter. The card is an item inside the
 * terminal's upgrade column, so every write is followed by a {@code markDirty()} on that column -
 * which is what pushes the card's new NBT into the terminal item.
 */
public class ContainerCardConfig extends AEBaseContainer {

    // ---------------------------------------------------------------------------------------------
    // Layout, straight from the addon's import_card.json / export_card.json.
    // ---------------------------------------------------------------------------------------------

    public static final int GUI_WIDTH = 191;
    /** Screen height with no capacity card; one row is added per capacity card. */
    public static final int BASE_HEIGHT = 163;
    public static final int HEIGHT_PER_ROW = 18;

    public static final int FILTER_X = 23;
    public static final int FILTER_Y = 29;
    public static final int FILTER_COLS = 9;

    /** Player rows: {@code bottom 84}, hotbar {@code bottom 26} - measured from the screen bottom. */
    public static final int PLAYER_X = 23;
    public static final int PLAYER_INV_BOTTOM = 84;
    public static final int PLAYER_HOTBAR_BOTTOM = 26;

    /** Armour column: x=2, running down from {@code 70 + capacity rows}. */
    public static final int ARMOR_X = 2;
    public static final int ARMOR_TOP = 70;

    private final ItemWcwtUpgradeCard.Kind kind;
    private final ItemStack terminal;
    private final ItemStack card;
    /** The terminal's upgrade column, so a change to the card can be written back to the terminal. */
    private final IInventory cardColumn;
    private final CardInventory filter;
    private final CardInventory cardUpgrades;

    private final Slot[] filterSlots = new Slot[ItemWcwtUpgradeCard.MAX_FILTER_SLOTS];
    private final Slot[] cardUpgradeSlots;
    private final Slot[] armorSlots = new Slot[4];

    // ---------------------------------------------------------------------------------------------

    public ContainerCardConfig(final InventoryPlayer ip, final ITerminalHost host,
        final ItemWcwtUpgradeCard.Kind kind) {
        super(ip, host);
        this.kind = kind;
        this.terminal = host instanceof final IGuiItemObject gui ? gui.getItemStack() : null;
        this.cardColumn = host instanceof final IPatternTerminal pattern
            ? pattern.getInventoryByName(Constants.UPGRADES)
            : null;
        // The card has to come from the column inventory, not from the terminal's NBT: the NBT reader
        // hands back a detached copy, and every change to it would be thrown away when the column
        // saves its own stacks back - the marks and filter would silently do nothing.
        this.card = ItemWcwtUpgradeCard.liveCard(this.cardColumn, kind);
        this.filter = this.card == null ? null : ItemWcwtUpgradeCard.filter(this.card);
        this.cardUpgrades = this.card == null ? null : ItemWcwtUpgradeCard.upgrades(this.card);

        this.setupFilterSlots();
        this.setupPlayerSlots(ip);
        this.setupArmorSlots(ip);
        this.cardUpgradeSlots = new Slot[this.cardUpgrades == null ? 0 : this.cardUpgrades.getSizeInventory()];
        this.setupCardUpgradeSlots();
    }

    private void setupFilterSlots() {
        if (this.filter == null) {
            return;
        }
        for (int i = 0; i < ItemWcwtUpgradeCard.MAX_FILTER_SLOTS; i++) {
            this.addSlotToContainer(
                this.filterSlots[i] = new SlotFake(
                    this.filter,
                    i,
                    FILTER_X + i % FILTER_COLS * 18,
                    FILTER_Y + i / FILTER_COLS * HEIGHT_PER_ROW));
        }
    }

    private void setupPlayerSlots(final InventoryPlayer ip) {
        this.bindPlayerInventory(ip, PLAYER_X - 8, 0);
    }

    private void setupArmorSlots(final InventoryPlayer ip) {
        // InventoryPlayer's IInventory view keeps the armour in its last four indices:
        // 39=helm, 38=chest, 37=legs, 36=boots - the same view the comprehensive terminal uses.
        for (int i = 0; i < 4; i++) {
            this.addSlotToContainer(
                this.armorSlots[i] = new SlotPlayerArmor(
                    ip,
                    ip.getSizeInventory() - 1 - i,
                    ARMOR_X,
                    ARMOR_TOP + i * HEIGHT_PER_ROW,
                    i));
        }
    }

    private void setupCardUpgradeSlots() {
        for (int i = 0; i < this.cardUpgradeSlots.length; i++) {
            // SlotWcwtCard, so the nested-card rules in CardUpgradeInventory are actually enforced -
            // a plain AppEngSlot accepts anything.
            this.addSlotToContainer(
                this.cardUpgradeSlots[i] = new com.asdflj.wtct.client.gui.container.slot.SlotWcwtCard(
                    this.cardUpgrades,
                    i,
                    0,
                    0));
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Clicking
    // ---------------------------------------------------------------------------------------------

    /**
     * The filter slots are ghosts: a click with a carried item sets that item as the filter without
     * consuming it, and a right click clears the slot. AE2's own fake slots only accept a
     * shift-click, which would make the card's filter much harder to fill than the addon's
     * drag-and-drop.
     */
    @Override
    public ItemStack slotClick(final int slotId, final int clickedButton, final int mode, final EntityPlayer player) {
        if (this.filter != null && slotId >= 0 && slotId < this.inventorySlots.size()) {
            final Slot slot = this.inventorySlots.get(slotId);
            if (slot instanceof SlotFake && slot.inventory == this.filter) {
                final ItemStack carried = player.inventory.getItemStack();
                if (clickedButton == 1 || carried == null) {
                    slot.putStack(null);
                } else {
                    final ItemStack ghost = carried.copy();
                    ghost.stackSize = 1;
                    slot.putStack(ghost);
                }
                this.markCardDirty();
                this.detectAndSendChanges();
                return null;
            }
        }
        return super.slotClick(slotId, clickedButton, mode, player);
    }

    // ---------------------------------------------------------------------------------------------
    // The card's configuration
    // ---------------------------------------------------------------------------------------------

    public ItemWcwtUpgradeCard.Kind getKind() {
        return this.kind;
    }

    public ItemStack getCard() {
        return this.card;
    }

    /** The marks the screen is showing - import: 0/1, export: 0 = none, otherwise a filter index. */
    public int[] getSelectedSlots() {
        return this.card == null ? new int[ItemWcwtUpgradeCard.SELECTED_SLOT_COUNT]
            : ItemWcwtUpgradeCard.selectedSlots(this.card);
    }

    /** Applies the marks the client sent and writes them onto the card. */
    public void setSelectedSlots(final int[] slots) {
        if (this.card == null) {
            return;
        }
        ItemWcwtUpgradeCard.setSelectedSlots(this.card, slots);
        this.markCardDirty();
    }

    /** Number of filter slots the card currently offers (18 + 9 per capacity card). */
    public int getActiveFilterSlotCount() {
        return this.card == null ? ItemWcwtUpgradeCard.BASE_FILTER_SLOTS
            : ItemWcwtUpgradeCard.filterSlotCount(this.card);
    }

    /** The fuzzy card's matching mode as the card stores it. */
    public appeng.api.config.FuzzyMode getFuzzyMode() {
        return ItemWcwtUpgradeCard.fuzzyMode(this.card);
    }

    /**
     * AE2ImportExportCard's fuzzy toggle: the button on the left toolbar, which cycles AE2's
     * {@code Settings.FUZZY_MODE} for the card. Only reachable with a fuzzy card nested, which is
     * exactly when the screen shows it.
     */
    public void cycleFuzzyMode() {
        if (this.card == null || !ItemWcwtUpgradeCard.hasUpgrade(this.card, ItemWcwtUpgradeCard.fuzzyCard())) {
            return;
        }
        final appeng.api.config.FuzzyMode[] values = appeng.api.config.FuzzyMode.values();
        final appeng.api.config.FuzzyMode current = ItemWcwtUpgradeCard.fuzzyMode(this.card);
        final appeng.api.config.FuzzyMode next = values[(current.ordinal() + 1) % values.length];
        ItemWcwtUpgradeCard.setFuzzyMode(this.card, next);
        this.markCardDirty();
        this.detectAndSendChanges();
    }

    public CardInventory getFilter() {
        return this.filter;
    }

    public Slot[] getFilterSlots() {
        return this.filterSlots;
    }

    public Slot[] getCardUpgradeSlots() {
        return this.cardUpgradeSlots;
    }

    public Slot[] getArmorSlots() {
        return this.armorSlots;
    }

    /** Pushes the card's new NBT into the terminal item, which owns the card column. */
    private void markCardDirty() {
        if (this.cardColumn != null) {
            this.cardColumn.markDirty();
        }
    }

    /**
     * The screen's armour cell: AE2's own rule for what fits, with a single-item limit - the addon
     * shows the player's four armour slots so an export target can be a piece of armour.
     */
    public final class SlotPlayerArmor extends AppEngSlot {

        private final int armorType;

        SlotPlayerArmor(final IInventory inv, final int index, final int x, final int y, final int armorType) {
            super(inv, index, x, y);
            this.armorType = armorType;
        }

        /** 0=helm, 1=chest, 2=legs, 3=boots - for the empty-slot placeholder icon. */
        public int armorTypeIndex() {
            return this.armorType;
        }

        @Override
        public boolean isItemValid(final ItemStack stack) {
            return stack != null && stack.getItem()
                .isValidArmor(stack, this.armorType, getPlayerInv().player);
        }

        @Override
        public int getSlotStackLimit() {
            return 1;
        }
    }
}
