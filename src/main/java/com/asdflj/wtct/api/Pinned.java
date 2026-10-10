package com.asdflj.wtct.api;

import static com.asdflj.wtct.nei.NEI_TH_Config.getConfigValue;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import com.asdflj.wtct.Wtct;
import com.asdflj.wtct.nei.ButtonConstants;
import com.asdflj.wtct.network.CPacketNetworkCraftingItems;

import appeng.api.AEApi;
import appeng.api.storage.IItemDisplayRegistry;
import appeng.api.storage.data.IAEItemStack;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class Pinned {

    private final HashMap<PinKey, PinInfo> pinInfo = new HashMap<>();
    /**
     * How many items may be pinned at once - one full row of the terminal's item list (18 cells), so
     * the pinned block is a row of its own instead of a half row the ordinary items continue on.
     */
    public static int MAX_PINNED = 18;
    public static Pinned INSTANCE = new Pinned();
    private long lastRunTime;
    private static final int interval = 1000;
    private static final Comparator<Map.Entry<PinKey, PinInfo>> TIME_COMPARATOR = Comparator
        .comparing(e -> e.getValue().since);
    private static final IItemDisplayRegistry registry = AEApi.instance()
        .registries()
        .itemDisplay();

    /**
     * Where the favourites live between sessions: a small NBT file beside the game's config folder.
     * Only {@link PinReason#FAVORITE} pins are written - the crafting pins are the server's, restated
     * every second a job runs.
     */
    private static final String FAVOURITE_STORE = "config/wtct_favorites.dat";
    /** Guards the one-time read of {@link #FAVOURITE_STORE}; see {@link #ensureStoreLoaded}. */
    private boolean storeLoaded;

    /**
     * The key of the pin table. An {@code AEItemStack} cannot serve as the key on its own: AE2 makes two of
     * them equal only while they carry the very {@code AESharedNBT} instance it interned for that item, and
     * it hashes them by that instance's identity. A pin that has been through a packet therefore stops
     * matching the copy the terminal's grid holds - the lookup misses, so the pin can neither be un-pinned
     * nor drawn as pinned, and every further click on it files one more entry, which is what made the pinned
     * block grow a duplicate per click.
     *
     * <p>
     * This key compares the way the grid's own list lookups do - item, damage value and NBT contents - and
     * hashes only on the cheap half of that, so the hash and equals can never disagree.
     */
    private static final class PinKey {

        private final IAEItemStack stack;
        private final int hash;

        PinKey(IAEItemStack stack) {
            this.stack = stack;
            this.hash = stack.getItem() == null ? 0
                : stack.getItem()
                    .hashCode() * 31 + stack.getItemDamage();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PinKey other)) return false;
            return Platform.isSameItemPrecise(this.stack.getItemStack(), other.stack.getItemStack());
        }

        @Override
        public int hashCode() {
            return this.hash;
        }
    }

    /** Every pinned stack. */
    public Set<IAEItemStack> getPinnedItems() {
        this.ensureStoreLoaded();
        Set<IAEItemStack> items = new LinkedHashSet<>();
        for (PinInfo info : pinInfo.values()) {
            items.add(info.stack);
        }
        return items;
    }

    public boolean isEmpty() {
        this.ensureStoreLoaded();
        return pinInfo.isEmpty();
    }

    public void add(IAEItemStack item) {
        // "Pin items obtained from autocrafting" - off means a started job no longer pins its output.
        if (!getConfigValue(ButtonConstants.PINNED_AUTO_CRAFT)) return;
        this.add(item, PinReason.CRAFTING);
    }

    public void add(IAEItemStack item, PinReason reason) {
        this.ensureStoreLoaded();
        if (item == null) return;
        if (registry.isBlacklisted(item.getItem()) || registry.isBlacklisted(
            item.getItem()
                .getClass())) {
            return;
        }
        PinInfo info = pinInfo.get(new PinKey(item));
        if (info != null) {
            // A favourite keeps the place the player gave it. Both rows are ordered by this stamp, and
            // autocrafting restates its pin every time a job for that item starts - so refreshing the
            // stamp unconditionally reshuffled the whole favourites row as soon as one of the pinned
            // items was crafted again. Only a pin that is new to the favourites takes a fresh stamp,
            // which is what puts a promoted crafting pin at the end of the row.
            final boolean promoting = reason == PinReason.FAVORITE && info.reason != PinReason.FAVORITE;
            if (info.reason != PinReason.FAVORITE || promoting) {
                info.since = Instant.now();
            }
            info.canPrune = false;
            // Hold on to the instance the grid currently knows, so the pinned cell keeps rendering.
            info.stack = item;
            // The reason has to move with an explicit favourite click: a shift + middle click on a
            // stack that autocrafting had already pinned used to refresh that crafting pin instead of
            // making a favourite, so nothing was ever written to the store.
            if (reason == PinReason.FAVORITE) {
                info.reason = PinReason.FAVORITE;
            }
        } else {
            pinInfo.put(new PinKey(item), new PinInfo(reason, item));
        }

        if (pinInfo.size() > MAX_PINNED) {
            // Oldest first, but the autocrafting pins go before the player's own list. They are the ones
            // restated - and so re-stamped - constantly, and ordering them together with the favourites
            // made the player's oldest favourites the first thing dropped when the row filled up.
            final List<Map.Entry<PinKey, PinInfo>> toRemove = new ArrayList<>(pinInfo.entrySet());
            toRemove.sort(
                Comparator.<Map.Entry<PinKey, PinInfo>, Integer>comparing(
                    e -> e.getValue().reason == PinReason.FAVORITE ? 1 : 0)
                    .thenComparing(TIME_COMPARATOR));
            for (int i = 0, drop = toRemove.size() - MAX_PINNED; i < drop; i++) {
                pinInfo.remove(
                    toRemove.get(i)
                        .getKey());
            }
        }
        // A favourite just changed hands - write the block back, or a game restart forgets it.
        if (reason == PinReason.FAVORITE) {
            this.saveFavourites();
        }
    }

    public PinInfo remove(IAEItemStack item) {
        this.ensureStoreLoaded();
        if (item == null) return null;
        final PinInfo removed = this.pinInfo.remove(new PinKey(item));
        if (removed != null && removed.reason == PinReason.FAVORITE) {
            this.saveFavourites();
        }
        return removed;
    }

    public boolean isPinnedItem(IAEItemStack item) {
        this.ensureStoreLoaded();
        if (item == null) return false;
        return pinInfo.containsKey(new PinKey(item));
    }

    /**
     * The pinned items of one reason, oldest first. The terminal lays its pinned block out in two
     * parts - the autocrafting pins take a row of their own, the player's favourites follow them
     * compactly - so each part asks for its own list instead of the two being mixed by time.
     */
    public List<IAEItemStack> getPinnedItems(PinReason reason) {
        this.ensureStoreLoaded();
        List<Map.Entry<PinKey, PinInfo>> list = new ArrayList<>(pinInfo.entrySet());
        list.removeIf(e -> e.getValue().reason != reason);
        list.sort(TIME_COMPARATOR);
        List<IAEItemStack> pinnedItems = new ArrayList<>(list.size());
        for (Map.Entry<PinKey, PinInfo> entry : list) {
            pinnedItems.add(entry.getValue().stack);
        }
        return pinnedItems;
    }

    /** Every pin, both reasons, oldest first - what the decorations walk. */
    public List<IAEItemStack> getSortedPinnedItems() {
        this.ensureStoreLoaded();
        List<Map.Entry<PinKey, PinInfo>> list = new ArrayList<>(pinInfo.entrySet());
        list.sort(TIME_COMPARATOR);
        List<IAEItemStack> pinnedItems = new ArrayList<>(list.size());
        for (Map.Entry<PinKey, PinInfo> entry : list) {
            pinnedItems.add(entry.getValue().stack);
        }
        return pinnedItems;
    }

    @Nullable
    public PinInfo getPinInfo(IAEItemStack item) {
        this.ensureStoreLoaded();
        if (item == null) return null;
        return this.pinInfo.get(new PinKey(item));
    }

    public void togglePinnedItems(IAEItemStack stack) {
        this.ensureStoreLoaded();
        if (stack == null || this.remove(stack) != null) return;
        this.add(stack);
    }

    /**
     * AEF-style favourite: a pin the user asked for by hand (shift + middle click in the item grid).
     * Unlike the crafting pins pushed by the server it is never pruned, and it is written to the
     * store on every change, so it survives a game restart.
     *
     * <p>
     * The toggle is favourite-aware rather than a plain remove-then-add: a stack autocrafting had
     * already pinned is <em>promoted</em> to a favourite by the click, while a second click on a
     * favourite unpins it. The old shape removed any pin it found first, which turned the click on a
     * crafting-pinned stack into an unpin - the favourite was never made at all.
     */
    public void toggleFavorite(IAEItemStack stack) {
        this.ensureStoreLoaded();
        if (stack == null) return;
        final PinInfo existing = this.getPinInfo(stack);
        if (existing != null && existing.reason == PinReason.FAVORITE) {
            this.remove(stack);
            return;
        }
        this.add(stack, PinReason.FAVORITE);
    }

    public int getMaxPinSize() {
        return MAX_PINNED;
    }

    public void updatePinnedItems(List<IAEItemStack> items) {
        this.ensureStoreLoaded();
        GuiScreen gui = Minecraft.getMinecraft().currentScreen;
        if (!WtctAPI.instance()
            .terminal()
            .isPinTerminal(gui)) return;
        if (items == null || items.isEmpty()) {
            for (PinInfo info : pinInfo.values()) {
                if (info.reason == PinReason.FAVORITE) continue;
                info.canPrune = true;
            }
            return;
        }
        HashMap<PinKey, IAEItemStack> incoming = new HashMap<>();
        for (IAEItemStack item : items) {
            if (item != null) incoming.put(new PinKey(item), item);
        }
        for (Map.Entry<PinKey, PinInfo> entry : this.pinInfo.entrySet()) {
            PinInfo info = entry.getValue();
            if (info.reason == PinReason.FAVORITE) continue;
            IAEItemStack latest = incoming.get(entry.getKey());
            if (latest == null) {
                info.canPrune = true;
            } else {
                // A still-running job re-states its output every second; take its copy so the pinned cell
                // keeps pointing at a stack the grid's list can find.
                info.stack = latest;
            }
        }
    }

    public void prune() {
        this.ensureStoreLoaded();
        if (getConfigValue(ButtonConstants.PINNED_BAR_REMOVE)) {
            pinInfo.values()
                .removeIf(v -> v.canPrune);
        }
    }

    public void clear() {
        this.ensureStoreLoaded();
        pinInfo.clear();
        this.saveFavourites();
    }

    /** Drops the pins that came from autocrafting, leaving the player's own favourites alone. */
    public void clearCraftingPins() {
        this.ensureStoreLoaded();
        pinInfo.entrySet()
            .removeIf(e -> e.getValue().reason == PinReason.CRAFTING);
    }

    public void updateCraftingItems(boolean force) {
        if (!getConfigValue(ButtonConstants.PINNED_AUTO_CRAFT)) return;
        GuiScreen gui = Minecraft.getMinecraft().currentScreen;
        if (gui == null) return;
        if (!WtctAPI.instance()
            .terminal()
            .isPinTerminal(gui)) return;
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastRunTime >= interval || force) {
            CPacketNetworkCraftingItems p = new CPacketNetworkCraftingItems();
            Wtct.proxy.netHandler.sendToServer(p);
            lastRunTime = currentTime;
        }
    }

    public void updateCraftingItems() {
        updateCraftingItems(false);
    }

    /**
     * Reads the stored favourites, once. The pins only matter once a terminal GUI is open, so the
     * read is deferred to the first call rather than the class's static init - that keeps
     * {@link Minecraft} out of any path that might run before the game has started.
     */
    private void ensureStoreLoaded() {
        if (this.storeLoaded) return;
        this.storeLoaded = true;
        try {
            final File file = new File(Minecraft.getMinecraft().mcDataDir, FAVOURITE_STORE);
            if (!file.isFile()) return;
            final NBTTagCompound root = CompressedStreamTools.read(file);
            if (root == null) return;
            final NBTTagList list = root.getTagList("favorites", 10);
            for (int i = 0; i < list.tagCount() && this.pinInfo.size() < MAX_PINNED; i++) {
                final ItemStack stack = ItemStack.loadItemStackFromNBT(list.getCompoundTagAt(i));
                if (stack == null || stack.getItem() == null) continue;
                // The stack comes back as a plain IAEItemStack; the pin mark still finds its cell in
                // the grid because PinKey compares item, damage and NBT contents.
                final IAEItemStack ae = AEItemStack.create(stack);
                this.pinInfo.put(new PinKey(ae), new PinInfo(PinReason.FAVORITE, ae));
            }
        } catch (final Throwable ignored) {
            // A broken store costs the favourites and nothing else - the file is rewritten on the
            // next change anyway.
        }
    }

    /** Writes the favourites back; called after every change that touched a {@link PinReason#FAVORITE} pin. */
    private void saveFavourites() {
        try {
            final NBTTagList list = new NBTTagList();
            for (final PinInfo info : this.pinInfo.values()) {
                if (info.reason != PinReason.FAVORITE) continue;
                final ItemStack stack = info.stack == null ? null : info.stack.getItemStack();
                if (stack == null || stack.getItem() == null) continue;
                list.appendTag(stack.writeToNBT(new NBTTagCompound()));
            }
            final NBTTagCompound root = new NBTTagCompound();
            root.setTag("favorites", list);
            final File file = new File(Minecraft.getMinecraft().mcDataDir, FAVOURITE_STORE);
            file.getParentFile()
                .mkdirs();
            CompressedStreamTools.safeWrite(root, file);
        } catch (final Throwable ignored) {
            // Losing a save is the same as losing the favourites: annoying, not fatal.
        }
    }

    public static class PinInfo {

        public Instant since;
        public PinReason reason;
        public boolean canPrune;
        /**
         * The stack this pin is shown as. It is refreshed whenever the pin is stated again - by the click
         * that made it, or by the server re-sending the output of a running job - because the terminal's
         * grid can only paint a pinned cell from a stack its own list still holds.
         */
        private IAEItemStack stack;

        PinInfo(PinReason reason, IAEItemStack stack) {
            this.reason = reason;
            this.stack = stack;
            this.since = Instant.now();
        }
    }

    public enum PinReason {
        CRAFTING,
        FAVORITE
    }

}
