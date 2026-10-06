package com.asdflj.wtct.client.gui.container;

import static appeng.util.Platform.isServer;
import static appeng.util.item.AEFluidStackType.FLUID_STACK_TYPE;
import static appeng.util.item.AEItemStackType.ITEM_STACK_TYPE;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;

import com.asdflj.wtct.common.parts.ExBusSlots;
import com.asdflj.wtct.common.parts.PartExStorageBus;
import com.asdflj.wtct.inventory.gui.BusPrimaryGui;
import com.asdflj.wtct.inventory.gui.GuiType;

import appeng.api.config.ActionItems;
import appeng.api.storage.StorageName;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IAEStackType;
import appeng.api.storage.data.IItemList;
import appeng.container.PrimaryGui;
import appeng.container.implementations.ContainerStorageBus;
import appeng.container.slot.SlotRestrictedInput;
import appeng.me.storage.MEInventoryHandler;
import appeng.tile.inventory.IAEStackInventory;
import appeng.util.IterationCounter;
import appeng.util.prioitylist.PrecisePriorityList;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

/**
 * The container behind the ME extended storage bus screen.
 *
 * <p>
 * It is rv3's storage bus container with two differences. The upgrade strip holds eight cards at the 1.21 panel's
 * coordinates - the parent builds five at the 1.7.10 strip - and the partition sweep works through both views of
 * the faced inventory: one click fills the grid with the neighbor's items, keeps filling with its fluids once the
 * items run out, and only then parks the button back at the wrench. The rv3 iterator it replaces is single type
 * and private, so this carries its own - keyed by player the same way, with a round flag where rv3 keyed its map
 * by stack type.
 */
public class ContainerExStorageBus extends ContainerStorageBus {

    private static final HashMap<EntityPlayer, PartitionIterators> PARTITION_ITERATORS = new HashMap<>();

    private final PartExStorageBus exBus;

    public ContainerExStorageBus(final InventoryPlayer ip, final PartExStorageBus te) {
        super(ip, te);
        this.exBus = te;
        // The parent just reset the mode off its own (always empty) iterator map; this is where a session
        // that survived the screen being closed and reopened says it is still walking.
        if (PARTITION_ITERATORS.containsKey(ip.player)) {
            this.partitionMode = ActionItems.NEXT_PARTITION;
        }
    }

    @Override
    protected void setupConfig() {
        final IInventory upgrades = this.getUpgradeable()
            .getInventoryByName("upgrades");
        for (int i = 0; i < ExBusSlots.UPGRADE_SLOTS; i++) {
            final SlotRestrictedInput slot = new SlotRestrictedInput(
                SlotRestrictedInput.PlacableItemType.UPGRADES,
                upgrades,
                i,
                ExBusSlots.UPGRADE_SLOT_X,
                ExBusSlots.UPGRADE_SLOT_Y + ExBusSlots.SLOT_SIZE * i,
                this.getInventoryPlayer());
            // setNotDraggable returns the vanilla Slot, not the restricted one - no chaining.
            slot.setNotDraggable();
            // The 1.21 panel art carries its own slot holes; the 1.7.10 card-with-arrow icon would be
            // stamped over them by AEBaseGui.drawTextureOnSlot, which skips slots whose icon is negative.
            slot.setIIcon(-1);
            this.addSlotToContainer(slot);
        }
    }

    @Override
    public int availableUpgrades() {
        return ExBusSlots.UPGRADE_SLOTS;
    }

    /** The upgrade strip is already eight slots tall - no room for the network tool panel beside it. */
    @Override
    public boolean hasToolbox() {
        return false;
    }

    @Override
    public void receiveSlotStacks(final StorageName invName, final Int2ObjectMap<IAEStack<?>> slotStacks) {
        super.receiveSlotStacks(invName, slotStacks);
        // Writing a slot runs the inherited config hook, which only wakes the device; the rebuild itself is
        // requested here, where the grid actually changed.
        if (isServer()) {
            this.exBus.resetExCache();
        }
    }

    @Override
    public void clear() {
        super.clear();
        this.exBus.resetExCache();
    }

    /**
     * The ore filter screen and the priority editor are entered from this screen and return through the primary gui
     * handed to them here; AE2's own factory builds that from a {@code GuiBridge} lookup this container cannot
     * appear in, so without this override both screens could be entered but never left.
     */
    @Override
    public PrimaryGui createPrimaryGui() {
        return BusPrimaryGui.create(this, GuiType.EX_STORAGE_BUS);
    }

    private void clearPartitionIterator(final EntityPlayer player) {
        PARTITION_ITERATORS.remove(player);
        this.partitionMode = ActionItems.WRENCH;
    }

    @Override
    public void partition(final boolean clearIterator) {
        final EntityPlayer player = this.getInventoryPlayer().player;
        if (clearIterator) {
            this.clearPartitionIterator(player);
            return;
        }

        final IAEStackInventory inv = this.exBus.getExConfig();

        PartitionIterators state = PARTITION_ITERATORS.get(player);
        if (state == null) {
            state = new PartitionIterators();
            state.it = this.buildIterator(ITEM_STACK_TYPE);
            PARTITION_ITERATORS.put(player, state);
            this.partitionMode = ActionItems.NEXT_PARTITION;
        }

        boolean skip = false;
        for (int x = 0; x < inv.getSizeInventory(); x++) {
            if (skip) {
                inv.putAEStackInSlot(x, null);
                continue;
            }
            if (this.isSlotEnabled(x / 9)) {
                final IAEStack<?> aeis = this.nextPartitionStack(state);
                if (aeis == null) {
                    this.clearPartitionIterator(player);
                    skip = true;
                    inv.putAEStackInSlot(x, null);
                    continue;
                }
                final IAEStack<?> temp = aeis.copy();
                temp.setStackSize(1);
                inv.putAEStackInSlot(x, temp);
            } else {
                skip = true;
                inv.putAEStackInSlot(x, null);
            }
        }
        if (state.it != null && !state.it.hasNext) {
            this.clearPartitionIterator(player);
        }

        // The sweep reads the handlers with their partitions stripped; put them back from the new grid.
        this.exBus.resetExCache();
        this.detectAndSendChanges();
    }

    /**
     * The next stack of the sweep, switching from the item round to the fluid one the moment the items run
     * dry - the fluids continue into the same page instead of starting one of their own, so the two views
     * never overwrite each other's slots.
     */
    private IAEStack<?> nextPartitionStack(final PartitionIterators state) {
        IAEStack<?> is = state.it.next();
        if (is != null || state.fluidRound) {
            return is;
        }
        state.fluidRound = true;
        state.it = this.buildIterator(FLUID_STACK_TYPE);
        return state.it.next();
    }

    /**
     * The view's contents as a plain list: the handler's own partition is stripped first - it is rebuilt from
     * the grid after the sweep - so the iterator walks everything the neighbor offers.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private IteratorState buildIterator(final IAEStackType<?> type) {
        final MEInventoryHandler cellInv = this.exBus.getExHandler(type);
        if (cellInv == null) {
            return new IteratorState(Collections.emptyIterator());
        }
        cellInv.setPartitionList(new PrecisePriorityList<>(type.createList()));
        final IItemList list = cellInv.getAvailableItems(type.createList(), IterationCounter.fetchNewId());
        return new IteratorState(list.iterator());
    }

    /** rv3's iterator state, private there: a cached {@code hasNext} walked one step at a time. */
    private static final class IteratorState {

        private final Iterator<IAEStack<?>> it;
        private boolean hasNext;

        IteratorState(final Iterator<IAEStack<?>> it) {
            this.it = it;
            this.hasNext = it.hasNext();
        }

        IAEStack<?> next() {
            if (this.hasNext) {
                final IAEStack<?> is = this.it.next();
                this.hasNext = this.it.hasNext();
                return is;
            }
            return null;
        }
    }

    /** One player's partition sweep: where the walk stands and which view it is currently reading. */
    private static final class PartitionIterators {

        IteratorState it;
        boolean fluidRound;
    }
}
