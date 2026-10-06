package com.asdflj.wtct.client.gui;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.item.ItemStack;

/**
 * The crafting cells a Ctrl+hammer left empty but the network can craft, and the pause in front of
 * the fills - ae2helpers' {@code AutoCraftingWatcher}, whose state is deliberately not a field of the
 * terminal's screen: the hammer opens AE2's craft plan on top of that screen (the same screen WCWT's
 * Ctrl+hammer shows), so the terminal's GUI instance is discarded and rebuilt while the player is
 * still waiting for the crafted materials. Nothing here touches the client's classes, so the packet
 * that arms it can be loaded on either side.
 *
 * <p>
 * The terminal's own tick walks {@link #pending()} and takes the cells it completes; the server is
 * only ever asked to move an item the client has confirmed is in the network.
 */
public final class AutoFillWatch {

    public static final AutoFillWatch INSTANCE = new AutoFillWatch();

    /** ae2helpers' delays: let AE2 finish its own moves first, then one fill every few ticks. */
    private static final int FIRST_DELAY = 15;
    private static final int RETRY_DELAY = 5;

    /** Crafting-cell slot -> the ingredient variants it waits for; empty = nothing watched. */
    private final Map<Integer, ItemStack[]> pending = new LinkedHashMap<>();
    private int delay;

    private AutoFillWatch() {}

    /** Replaces the wait list - a new hammer, or the server's answer to one. */
    public void setPending(final Map<Integer, ItemStack[]> cells) {
        this.pending.clear();
        if (cells != null) {
            this.pending.putAll(cells);
        }
        this.delay = this.pending.isEmpty() ? 0 : FIRST_DELAY;
    }

    public boolean isEmpty() {
        return this.pending.isEmpty();
    }

    /** The live wait list: the terminal's tick removes the cells it has handed to the server. */
    public Map<Integer, ItemStack[]> pending() {
        return this.pending;
    }

    /** The variants a watched cell waits for, or {@code null} when it is not watched. */
    public ItemStack[] variantsAt(final int slot) {
        return this.pending.get(slot);
    }

    public void clear() {
        this.pending.clear();
        this.delay = 0;
    }

    /** True while the opening pause still has ticks left; counts one down per call. */
    public boolean pause() {
        if (this.delay <= 0) {
            return false;
        }
        this.delay--;
        return true;
    }

    /** Ticks left in the pause - for the diagnostic log that says whether the screen is ticking at all. */
    public int delay() {
        return this.delay;
    }

    /** Holds the watch back after a fill, so the crafting grid can catch up with it. */
    public void holdRetry() {
        this.delay = RETRY_DELAY;
    }
}
