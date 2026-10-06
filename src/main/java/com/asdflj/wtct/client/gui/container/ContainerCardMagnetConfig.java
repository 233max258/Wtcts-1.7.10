package com.asdflj.wtct.client.gui.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import com.asdflj.wtct.api.Constants;
import com.asdflj.wtct.common.item.card.CardInventory;
import com.asdflj.wtct.common.item.card.CardTicker;
import com.asdflj.wtct.common.item.card.ItemWcwtUpgradeCard;
import com.asdflj.wtct.inventory.IPatternTerminal;
import com.asdflj.wtct.inventory.ItemPatternRefillInventory;

import appeng.api.implementations.guiobjects.IGuiItemObject;
import appeng.api.storage.ITerminalHost;
import appeng.container.AEBaseContainer;
import appeng.container.slot.SlotFake;
import appeng.util.Platform;

/**
 * The magnet card's configuration screen - AE2WTLib's {@code MagnetMenu} in the 1.7.10 flavour.
 *
 * <p>
 * Two ghost filters of three rows of nine ({@code ConfigInventory.configTypes(27)} in WTLib) and the
 * player's inventory under them:
 * <ul>
 * <li><b>pickup</b> at (8, 22) - which drops the magnet is willing to pull at all;
 * <li><b>insert</b> at (8, 103) - which of the pulled drops go straight into the ME network rather
 * than the player's inventory.
 * </ul>
 *
 * <p>
 * Both filters carry WTLib's own include/exclude flag, blacklist by default - so with empty filters
 * everything is picked up and everything goes to the network, which is WTLib's out-of-the-box
 * behaviour. The five buttons between the two grids are WTLib's: the two mode toggles, copy each
 * filter onto the other, and swap them.
 */
public class ContainerCardMagnetConfig extends AEBaseContainer {

    /** Set {@code -Dwtct.debug.magnet} to trace the magnet chain from the click to the tick. */
    private static final boolean DEBUG = Boolean.getBoolean("wtct.debug.magnet");

    // ---------------------------------------------------------------------------------------------
    // Layout, straight from WTLib's magnet.json (a 176x256 screen).
    // ---------------------------------------------------------------------------------------------

    public static final int GUI_WIDTH = 176;
    public static final int GUI_HEIGHT = 256;

    public static final int FILTER_COLS = 9;
    /** Rows WTLib shows per filter - three, which is what makes the screen 256 tall. */
    public static final int FILTER_ROWS = 3;

    public static final int PICKUP_X = 8;
    public static final int PICKUP_Y = 22;
    public static final int INSERT_X = 8;
    public static final int INSERT_Y = 103;

    /** Player rows: {@code bottom 84}, hotbar {@code bottom 26} - measured from the screen bottom. */
    public static final int PLAYER_X = 8;
    public static final int PLAYER_INV_BOTTOM = 84;
    public static final int PLAYER_HOTBAR_BOTTOM = 26;

    // ---------------------------------------------------------------------------------------------

    /**
     * The stack the server's writes actually land in: the upgrade column owns the object the Baubles
     * bridge hands back to the terminal's slot, so {@code MagnetMode} read from here is always current
     * on the side that flipped it. Everything that only needs "what item is this screen editing" -
     * the tooltip above all - has to go through {@link #getTerminalStack()} so it sees the same object.
     */
    private final ItemStack terminal;
    private final ItemStack card;
    /** The terminal's upgrade column, so a change to the card can be written back to the terminal. */
    private final IInventory cardColumn;
    private final CardInventory pickupFilter;
    private final CardInventory insertFilter;

    private final Slot[] pickupSlots = new Slot[ItemWcwtUpgradeCard.MAGNET_FILTER_SLOTS];
    private final Slot[] insertSlots = new Slot[ItemWcwtUpgradeCard.MAGNET_FILTER_SLOTS];

    public ContainerCardMagnetConfig(final InventoryPlayer ip, final ITerminalHost host) {
        super(ip, host);
        this.cardColumn = host instanceof final IPatternTerminal pattern ? pattern.getInventoryByName(Constants.UPGRADES)
            : null;
        // The column's own stack is the live one (see the field comment); the host's copy is only the
        // fallback for a host that has no column at all.
        this.terminal = this.cardColumn instanceof final ItemPatternRefillInventory column ? column.getHostStack()
            : host instanceof final IGuiItemObject gui ? gui.getItemStack() : null;
        // The live card from the column, not an NBT copy - see ContainerCardConfig for why the copy
        // would silently drop every change this screen makes.
        this.card = ItemWcwtUpgradeCard.liveCard(this.cardColumn, ItemWcwtUpgradeCard.Kind.MAGNET);
        this.pickupFilter = this.card == null ? null : ItemWcwtUpgradeCard.magnetPickupFilter(this.card);
        this.insertFilter = this.card == null ? null : ItemWcwtUpgradeCard.magnetInsertFilter(this.card);

        this.setupFilterSlots();
        // The player's rows sit at x=8, which is AE2's own binding - no offset needed.
        this.bindPlayerInventory(ip, 0, 0);
    }

    private void setupFilterSlots() {
        for (int i = 0; i < ItemWcwtUpgradeCard.MAGNET_FILTER_SLOTS; i++) {
            final int x = i % FILTER_COLS * 18;
            final int y = i / FILTER_COLS * 18;
            if (this.pickupFilter != null) {
                this.addSlotToContainer(
                    this.pickupSlots[i] = new SlotFake(this.pickupFilter, i, PICKUP_X + x, PICKUP_Y + y));
            }
            if (this.insertFilter != null) {
                this.addSlotToContainer(
                    this.insertSlots[i] = new SlotFake(this.insertFilter, i, INSERT_X + x, INSERT_Y + y));
            }
        }
    }

    /**
     * Both filter grids are ghosts: a click with a carried item sets that item as the filter without
     * consuming it and a right click clears the slot. AE2's fake slots only take a shift-click, which
     * would make these much harder to fill than WTLib's drag-and-drop.
     */
    @Override
    public ItemStack slotClick(final int slotId, final int clickedButton, final int mode, final EntityPlayer player) {
        if (slotId >= 0 && slotId < this.inventorySlots.size()) {
            final Slot slot = this.inventorySlots.get(slotId);
            if (slot instanceof SlotFake
                && (slot.inventory == this.pickupFilter || slot.inventory == this.insertFilter)) {
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

    public ItemStack getCard() {
        return this.card;
    }

    /**
     * The terminal item. The magnet's own on/off-plus-destination setting lives there rather than on
     * the card, because the backpack terminal's magnet reads the same key.
     *
     * <p>
     * This is the column's <em>live</em> stack, not a copy taken when the container was built - see
     * the field comment. The distinction is the whole reason the magnet screen's tooltip used to be
     * one step behind: a copy keeps the mode it had at open time and never sees a flip.
     */
    public ItemStack getTerminalStack() {
        return this.terminal;
    }

    public Slot[] getPickupSlots() {
        return this.pickupSlots;
    }

    public Slot[] getInsertSlots() {
        return this.insertSlots;
    }

    public boolean isPickupWhitelist() {
        return this.card != null && ItemWcwtUpgradeCard.magnetPickupWhitelist(this.card);
    }

    public boolean isInsertWhitelist() {
        return this.card != null && ItemWcwtUpgradeCard.magnetInsertWhitelist(this.card);
    }

    /**
     * Whether the magnet card's master switch on the settings screen is on. The mode and the switch
     * are two independent things on the terminal ({@code MagnetMode} names a destination, the switch
     * says whether the card runs at all), and {@code CardTicker.tickMagnet} returns on the switch
     * before it ever looks at the mode - so an "off" switch makes a perfectly good mode do nothing.
     * The screen shows this so that state is not invisible.
     */
    public boolean isMagnetSwitchOn() {
        return ContainerWcwtSettings.isOn(this.getTerminalStack(), ContainerWcwtSettings.KEY_MAGNET_CARD);
    }

    /**
     * Turns the master switch on. Called when the magnet button is used: a click there says "I want
     * the magnet to work", and one that lands on a terminal whose switch was turned off on the
     * settings screen would otherwise step the mode through perfectly valid values with nothing
     * happening, which reads as a broken button.
     */
    public void enableMagnetSwitch() {
        final ItemStack terminal = this.getTerminalStack();
        if (terminal == null) {
            return;
        }
        Platform.openNbtData(terminal)
            .setBoolean(ContainerWcwtSettings.KEY_MAGNET_CARD, true);
    }

    /** WTLib's {@code togglePickupMode}: the pickup filter flips between whitelist and blacklist. */
    public void togglePickupMode() {
        if (this.card == null) {
            return;
        }
        ItemWcwtUpgradeCard.setMagnetPickupWhitelist(this.card, !ItemWcwtUpgradeCard.magnetPickupWhitelist(this.card));
        this.markCardDirty();
        this.detectAndSendChanges();
    }

    public void toggleInsertMode() {
        if (this.card == null) {
            return;
        }
        ItemWcwtUpgradeCard.setMagnetInsertWhitelist(this.card, !ItemWcwtUpgradeCard.magnetInsertWhitelist(this.card));
        this.markCardDirty();
        this.detectAndSendChanges();
    }

    /**
     * Steps the magnet's mode (off / to inventory / into the network). Like the comprehensive
     * terminal's button: the mode lives on the terminal item under the key the backpack terminal
     * uses, and the card column is marked dirty afterwards so the item is written back.
     *
     * <p>
     * Only ever runs on the side that owns the setting. The screen no longer flips its own copy on
     * the way out - see {@link #flipMagnetModeLocally()} for what it does instead - so this flip is
     * the only one the authoritative stack sees, and one click stays one step.
     */
    public void cycleMagnetMode() {
        final ItemStack terminal = this.getTerminalStack();
        if (DEBUG) {
            System.out.println(
                "[wtct-magnet] server cycleMagnetMode: terminal=" + terminal
                    + " card="
                    + this.card
                    + " cardColumn="
                    + this.cardColumn);
        }
        if (terminal == null) {
            return;
        }
        final CardTicker.MagnetMode before = CardTicker.MagnetMode.of(terminal);
        CardTicker.MagnetMode.set(terminal, before.next());
        if (DEBUG) {
            System.out
                .println("[wtct-magnet] server magnet mode " + before + " -> " + CardTicker.MagnetMode.of(terminal));
        }
        this.markCardDirty();
        this.detectAndSendChanges();
    }

    /**
     * Flips the mode on this side's stack, for the screen that has to redraw before the server's
     * answer can arrive.
     *
     * <p>
     * The magnet screen's button went through the server alone and waited for the flip to travel back
     * through the container. That works while the terminal sits in the player's main inventory - the
     * slot is synced every tick - but <em>not</em> while it is worn in a Baubles slot, which is the
     * case the magnet is for: no slot of this container maps to that column, so the server's write
     * never reaches the screen and the tooltip keeps naming the mode it was opened on. Flipping
     * locally makes the button answer the click immediately, and because the packet carries no
     * payload the server flips its own stack from the same old value, so the two still agree
     * afterwards instead of drifting two steps apart.
     *
     * <p>
     * Client-only on purpose: the server has {@link #cycleMagnetMode()} and must not run both.
     */
    public void flipMagnetModeLocally() {
        final ItemStack terminal = this.getTerminalStack();
        if (terminal == null) {
            return;
        }
        CardTicker.MagnetMode.set(
            terminal,
            CardTicker.MagnetMode.of(terminal)
                .next());
    }

    /** WTLib's {@code copyUp}: the insert filter is copied up onto the pickup filter. */
    public void copyUp() {
        this.copy(this.insertFilter, this.pickupFilter);
    }

    /** WTLib's {@code copyDown}: the pickup filter is copied down onto the insert filter. */
    public void copyDown() {
        this.copy(this.pickupFilter, this.insertFilter);
    }

    /** WTLib's {@code switchInsertPickup}: the two filters trade places. */
    public void switchFilters() {
        if (this.pickupFilter == null || this.insertFilter == null) {
            return;
        }
        final ItemStack[] pickup = this.snapshot(this.pickupFilter);
        this.restore(this.pickupFilter, this.snapshot(this.insertFilter));
        this.restore(this.insertFilter, pickup);
        this.markCardDirty();
        this.detectAndSendChanges();
    }

    private void copy(final CardInventory from, final CardInventory to) {
        if (from == null || to == null) {
            return;
        }
        this.restore(to, this.snapshot(from));
        this.markCardDirty();
        this.detectAndSendChanges();
    }

    private ItemStack[] snapshot(final CardInventory inventory) {
        final ItemStack[] copy = new ItemStack[inventory.getSizeInventory()];
        for (int i = 0; i < copy.length; i++) {
            final ItemStack is = inventory.getStackInSlot(i);
            copy[i] = is == null ? null : is.copy();
        }
        return copy;
    }

    private void restore(final CardInventory inventory, final ItemStack[] contents) {
        for (int i = 0; i < contents.length && i < inventory.getSizeInventory(); i++) {
            inventory.setInventorySlotContents(i, contents[i]);
        }
    }

    /** Pushes the card's new NBT into the terminal item, which owns the card column. */
    private void markCardDirty() {
        if (this.cardColumn != null) {
            this.cardColumn.markDirty();
        }
    }
}
