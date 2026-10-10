package com.asdflj.wtct.client.gui;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;

import appeng.api.storage.data.IAEStack;
import appeng.client.gui.IInterfaceTerminalPostUpdate;
import appeng.core.sync.packets.PacketInterfaceTerminalUpdate;

/**
 * Holds the terminal's list packets that arrive while the window is still opening.
 *
 * <p>
 * Every list a terminal needs - the network's items, its fluids, the pattern providers - is pushed by
 * the server the moment the container opens, and each push is gated on
 * {@code Minecraft.currentScreen} already being the terminal. The screen swap happens on the client
 * thread while these packets are applied on the network thread, so a push that loses that race used to
 * be dropped outright: the terminal opened with an empty grid and stayed that way until something in
 * the network happened to move (a few tenths of a second later).
 *
 * <p>
 * Instead of dropping, the handlers now park the late arrivals here; {@link #applyTo} is called from
 * the GUI's {@code initGui} - at which point the screen is guaranteed to be the terminal - so the very
 * first frame already draws the full lists. A stash is only trusted for a moment ({@link #MAX_AGE_MS});
 * anything older is garbage from an abandoned window and is discarded rather than shown.
 *
 * <p>
 * Written from the network thread, read from the client thread - hence the synchronization.
 */
public final class EarlyTerminalLists {

    /** A stash older than this no longer belongs to a window that is about to open. */
    private static final long MAX_AGE_MS = 2000L;

    private static long s_stashedAt;
    /**
     * The item and fluid lists are pushed as two separate packets; they must not share one slot, or
     * whichever arrives second overwrites the first and the grid comes up missing half of its
     * contents - the rest then trickles in a moment later and re-sorts everything under the player.
     */
    @Nullable
    private static List<? extends IAEStack<?>> s_stacks;
    @Nullable
    private static List<? extends IAEStack<?>> s_fluidStacks;
    private static boolean s_hasPlayerItem;
    @Nullable
    private static ItemStack s_playerItem;
    @Nullable
    private static List<PacketInterfaceTerminalUpdate.PacketEntry> s_interfaceCommands;
    private static int s_interfaceFlags;
    private static boolean s_hasInterfaceUpdate;

    private EarlyTerminalLists() {}

    /** Parks the item list (the carriers the terminal merges into its repo). */
    public static synchronized void stashStacks(final List<? extends IAEStack<?>> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        s_stacks = list;
        touch();
    }

    /** Parks the fluid list; it is pushed by its own packet and must not clobber the item stash. */
    public static synchronized void stashFluidStacks(final List<? extends IAEStack<?>> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        s_fluidStacks = list;
        touch();
    }

    /** Parks the single "this is what the player is holding" stack; {@code null} clears it too. */
    public static synchronized void stashPlayerItem(final ItemStack is) {
        s_playerItem = is;
        s_hasPlayerItem = true;
        touch();
    }

    /** Parks AE2's pattern-provider list so the management area is filled on the first frame. */
    public static synchronized void stashInterfaceUpdate(final List<PacketInterfaceTerminalUpdate.PacketEntry> commands,
        final int statusFlags) {
        s_interfaceCommands = commands;
        s_interfaceFlags = statusFlags;
        s_hasInterfaceUpdate = true;
        touch();
    }

    /**
     * Applies everything a terminal's {@code initGui} finds stashed, then empties the holder. Only a
     * fresh stash is applied; a stale one is dropped without touching the GUI.
     */
    public static synchronized void applyTo(final IGuiMonitorTerminal gui) {
        final long now = System.currentTimeMillis();
        // TEMP DIAGNOSTIC (1.0.35, remove once the terminal-open delay is pinned down).
        cpw.mods.fml.common.FMLLog.info(
            "[wtct-diag] client initGui applyTo t=%d stacks=%s fluids=%s player=%s iface=%s age=%dms",
            now, s_stacks != null, s_fluidStacks != null, s_hasPlayerItem, s_hasInterfaceUpdate,
            s_stashedAt == 0 ? -1 : now - s_stashedAt);
        final boolean fresh = s_stashedAt != 0 && now - s_stashedAt <= MAX_AGE_MS;
        if (fresh) {
            if (s_stacks != null) {
                final long t0 = now;
                gui.postStackUpdate(s_stacks);
                cpw.mods.fml.common.FMLLog.info(
                    "[wtct-diag] client postStackUpdate took %dms", System.currentTimeMillis() - t0);
            }
            if (s_fluidStacks != null) {
                gui.postStackUpdate(s_fluidStacks);
            }
            if (s_hasPlayerItem) {
                gui.setPlayerInv(s_playerItem);
            }
            if (s_hasInterfaceUpdate && gui instanceof final IInterfaceTerminalPostUpdate post) {
                post.postUpdate(s_interfaceCommands, s_interfaceFlags);
            }
        }
        clear();
    }

    private static void touch() {
        s_stashedAt = System.currentTimeMillis();
    }

    private static void clear() {
        s_stacks = null;
        s_fluidStacks = null;
        s_playerItem = null;
        s_hasPlayerItem = false;
        s_interfaceCommands = null;
        s_hasInterfaceUpdate = false;
        s_stashedAt = 0;
    }
}
