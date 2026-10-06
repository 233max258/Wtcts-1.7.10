package com.asdflj.wtct.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import appeng.container.implementations.ContainerInterfaceTerminal;
import appeng.util.Platform;

/**
 * Keeps the pattern slots a terminal shows for the network's pattern providers in step with the server.
 *
 * <p>
 * AE2's interface terminal reports a row's <em>contents</em> only from its own private
 * {@code syncIfaceSlot}, which just its slot actions call; {@code updateList} compares a row's name,
 * size, priority and online state and never the patterns in its slots. A provider therefore looked
 * unchanged in an open terminal whenever the pattern was put in or taken out by anything else - this
 * mod's upload buttons, the interface's own GUI, an import bus feeding patterns, another player - and
 * the list only caught up when the terminal was reopened.
 *
 * <p>
 * One instance per container holds what the client has been told so far, and every tick hands each
 * difference to AE2's own overwrite entry ({@link Ae2Reflect#pushTrackedSlot}, the entry type
 * {@code syncIfaceSlot} builds), which the client applies through the handler it already has. Slots are
 * compared by identity every tick - empty or not, item, damage, count, which is what a put in or a take
 * out changes - and by full stack equality on a rotating slice, so a slot swapped for a different pattern
 * of the same item is caught within a second while the NBT work stays spread over the ticks.
 */
public final class ProviderSlotSync {

    /** Ticks it takes to work through every slot for the full stack comparison. */
    private static final int DEEP_DIFF_TICKS = 20;

    private final ContainerInterfaceTerminal registry;
    /** Per provider id, the slot contents the client is known to hold. */
    private final Map<Long, ItemStack[]> sent = new HashMap<>();
    private int ticks;

    public ProviderSlotSync(final ContainerInterfaceTerminal registry) {
        this.registry = registry;
    }

    /**
     * Pushes everything the client has not been told about. Called every tick from the container whose
     * terminal is showing the list, on the server, before that registry's own {@code detectAndSendChanges}.
     *
     * <p>
     * The identity comparison runs over every slot every tick, which costs nothing but a few field reads:
     * a put in or a take out is seen on the tick it happens. The full-stack comparison is what looks at
     * NBT, so a slice of the slots - slot {@code i} where {@code i % DEEP_DIFF_TICKS} matches the tick -
     * is compared each tick and each slot is therefore covered once every {@code DEEP_DIFF_TICKS} ticks.
     * A large network pays a little on every tick instead of one spike per second.
     */
    public void tick() {
        if (this.registry == null || Platform.isClient()) {
            return;
        }
        final Map<?, ?> tracked = Ae2Reflect.getTracked(this.registry);
        final int slice = (int) (this.ticks++ % DEEP_DIFF_TICKS);
        final Set<Long> live = new HashSet<>(tracked.size());
        for (final Object tracker : tracked.values()) {
            final long id = Ae2Reflect.getTrackedId(tracker);
            final IInventory patterns = id < 0 ? null : Ae2Reflect.getTrackedPatterns(tracker);
            if (patterns == null) {
                continue;
            }
            live.add(id);
            final int size = patterns.getSizeInventory();
            final ItemStack[] known = this.sent.get(id);
            if (known == null || known.length != size) {
                // First sight of this row: its contents travelled with the row itself, so there is
                // nothing to push - only a note of what the client is holding.
                final ItemStack[] snapshot = new ItemStack[size];
                for (int i = 0; i < size; i++) {
                    snapshot[i] = copy(patterns.getStackInSlot(i));
                }
                this.sent.put(id, snapshot);
                continue;
            }
            for (int i = 0; i < size; i++) {
                final ItemStack now = patterns.getStackInSlot(i);
                if (changed(known[i], now, i % DEEP_DIFF_TICKS == slice)) {
                    this.push(id, i, now);
                }
            }
        }
        this.sent.keySet()
            .retainAll(live);
    }

    /** Sends one slot right away and records it, so the next comparison does not repeat it. */
    public void push(final long id, final int slot, final ItemStack stack) {
        if (this.registry == null) {
            return;
        }
        Ae2Reflect.pushTrackedSlot(this.registry, id, slot, stack);
        final ItemStack[] known = this.sent.get(id);
        if (known != null && slot >= 0 && slot < known.length) {
            known[slot] = copy(stack);
        }
    }

    private static ItemStack copy(final ItemStack stack) {
        return stack == null ? null : stack.copy();
    }

    /**
     * Identity first - empty or not, item, damage, count, which is what a put in or a take out changes -
     * and the full stack only on the slot's deep slice. Empty slots never reach the NBT comparison, so a
     * network that is mostly empty costs almost nothing to keep an eye on.
     */
    private static boolean changed(final ItemStack known, final ItemStack now, final boolean deep) {
        if (known == null || now == null) {
            return known != now;
        }
        if (known.getItem() != now.getItem() || known.getItemDamage() != now.getItemDamage()
            || known.stackSize != now.stackSize) {
            return true;
        }
        return deep && !ItemStack.areItemStacksEqual(known, now);
    }
}
