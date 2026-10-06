package com.asdflj.wtct.common.item.card;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerPickupXpEvent;

import com.asdflj.wtct.api.Constants;
import com.asdflj.wtct.api.WirelessObject;
import com.asdflj.wtct.common.Config;
import com.asdflj.wtct.inventory.item.WirelessTerminal;

import appeng.api.AEApi;
import appeng.api.config.FuzzyMode;
import appeng.api.definitions.IItemDefinition;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEItemStack;
import appeng.core.AELog;
import appeng.util.Platform;

/**
 * Runs a terminal's upgrade cards while the terminal sits in a player's inventory.
 *
 * <p>
 * This is the 1.7.10 counterpart of AE2ImportExportCard's {@code WirelessTerminalItem#inventoryTick}
 * and AE2WTLib's magnet ticker: the terminal item itself drives the cards, so nothing has to be
 * ticked by the container - the player can keep the terminal in the hotbar, the main inventory or a
 * bauble and the cards keep working.
 *
 * <p>
 * Everything here is server side and runs on a slow interval (the same idea as the backpack
 * terminal's magnet). The first thing checked is the terminal's NBT, so a terminal without cards
 * costs nothing.
 */
public final class CardTicker {

    /** How often the cards run, in ticks. */
    private static final int INTERVAL = 10;

    /**
     * Diagnostics for the magnet card, the way the pick-block card had one while it was being made to
     * work. Off unless {@code -Dwtct.debug.magnet} is passed to the JVM, so it costs nothing normally.
     */
    private static final boolean DEBUG_MAGNET = Boolean.getBoolean("wtct.debug.magnet");

    private CardTicker() {}

    /**
     * Entry point from {@code Item#onUpdate}. Returns immediately unless the terminal really holds
     * cards and we are on the server.
     */
    public static void onUpdate(final ItemStack terminal, final World world, final Entity entity, final int slot) {
        if (world == null || world.isRemote || !(entity instanceof final EntityPlayer player)) {
            return;
        }
        if (player.ticksExisted % INTERVAL != 0) {
            return;
        }
        final boolean magnet = ItemWcwtUpgradeCard.isInstalled(terminal, ItemWcwtUpgradeCard.Kind.MAGNET);
        final boolean importer = ItemWcwtUpgradeCard.isInstalled(terminal, ItemWcwtUpgradeCard.Kind.IMPORT);
        final boolean exporter = ItemWcwtUpgradeCard.isInstalled(terminal, ItemWcwtUpgradeCard.Kind.EXPORT);
        if (DEBUG_MAGNET) {
            System.out.println(
                "[wtct-magnet] tick slot=" + slot
                    + " magnet=" + magnet
                    + " import=" + importer
                    + " export=" + exporter
                    + " mode=" + MagnetMode.of(terminal)
                    + " sneak=" + player.isSneaking()
                    + " switch=" + switchEnabled(terminal, "MagnetCard")
                    + " range=" + Config.magnetRange
                    + " nbt=" + Platform.openNbtData(terminal));
        }
        if (magnet) {
            final ItemStack card = ItemWcwtUpgradeCard.firstInstalled(terminal, ItemWcwtUpgradeCard.Kind.MAGNET);
            if (card != null) {
                run("magnet", () -> tickMagnet(terminal, card, world, player, slot));
            }
        }
        if (!importer && !exporter) {
            return;
        }
        final WirelessObject network = openTerminal(terminal, world, player, slot);
        if (network == null) {
            return;
        }
        if (importer) {
            final ItemStack card = ItemWcwtUpgradeCard.firstInstalled(terminal, ItemWcwtUpgradeCard.Kind.IMPORT);
            if (card != null) {
                run("import", () -> runImport(card, terminal, player, network));
            }
        }
        if (exporter) {
            final ItemStack card = ItemWcwtUpgradeCard.firstInstalled(terminal, ItemWcwtUpgradeCard.Kind.EXPORT);
            if (card != null) {
                run("export", () -> runExport(card, terminal, player, network));
            }
        }
    }

    /**
     * Runs one card and keeps its failure to itself.
     *
     * <p>
     * This is called from {@code Item#onUpdate} inside the player's own inventory tick, so anything
     * that escaped would abort the rest of that tick - and, worse, would abort every card after it.
     * That is exactly what happened while the magnet's energy source was broken: the magnet threw
     * before the import and export cards ever ran, so all three looked dead at once.
     */
    private static void run(final String what, final Runnable card) {
        try {
            card.run();
        } catch (final Throwable t) {
            AELog.debug(t);
            AELog.warn("wtct: the " + what + " card threw and was skipped for this tick");
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Magnet card
    // ---------------------------------------------------------------------------------------------

    /**
     * Pulls nearby drops to the player and, in the "into the network" mode, straight into the ME
     * system. The mode is stored on the terminal under the same key the backpack terminal uses, so
     * the two share their setting; the magnet screen's own button cycles it.
     *
     * <p>
     * Both of the card's filters are applied the way WTLib applies them
     * ({@code MagnetHandler.handleMagnet} and {@code canInsert}): the <em>pickup</em> filter decides
     * whether a drop is pulled at all, and the <em>insert</em> filter decides whether it goes into
     * the network instead of the inventory. WTLib also skips the whole thing while sneaking, so a
     * player can pick something up by hand without the magnet stealing it.
     */
    private static void tickMagnet(final ItemStack terminal, final ItemStack card, final World world,
        final EntityPlayer player, final int slot) {
        final MagnetMode mode = MagnetMode.of(terminal);
        if (mode == MagnetMode.OFF || player.isSneaking() || !switchEnabled(terminal, "MagnetCard")) {
            return;
        }
        final CardInventory pickupFilter = ItemWcwtUpgradeCard.magnetPickupFilter(card);
        final CardInventory insertFilter = ItemWcwtUpgradeCard.magnetInsertFilter(card);
        final boolean pickupWhitelist = ItemWcwtUpgradeCard.magnetPickupWhitelist(card);
        final boolean insertWhitelist = ItemWcwtUpgradeCard.magnetInsertWhitelist(card);
        final int range = Config.magnetRange;
        final List<EntityItem> drops = world.getEntitiesWithinAABB(EntityItem.class, around(player, range));
        final WirelessObject network = mode == MagnetMode.ME ? openTerminal(terminal, world, player, slot) : null;
        if (DEBUG_MAGNET) {
            System.out.println(
                "[wtct-magnet] tickMagnet mode=" + mode
                    + " drops="
                    + drops.size()
                    + " network="
                    + (network != null)
                    + " host="
                    + hostOf(network)
                    + " storage="
                    + (network == null ? null : network.getItemInventory()));
        }

        for (final EntityItem drop : drops) {
            final ItemStack stack = drop.getEntityItem();
            if (stack == null) {
                continue;
            }
            if (!ItemWcwtUpgradeCard.passesFilter(pickupFilter, stack, pickupWhitelist)) {
                continue;
            }
            if (network != null && ItemWcwtUpgradeCard.passesFilter(insertFilter, stack, insertWhitelist)) {
                final long remaining = insertIntoNetwork(network, stack);
                if (remaining == INSERT_FAILED) {
                    // The transfer itself failed (no power source, no storage, a dimension change...).
                    // Do not let one bad drop kill the whole tick: fall through and pull it onto the
                    // player instead, which is what the inventory mode does anyway.
                } else if (remaining <= 0) {
                    drop.setDead();
                    continue;
                } else {
                    stack.stackSize = (int) remaining;
                }
            }
            // Whatever is left (or everything, in the inventory mode) is pulled onto the player.
            drop.delayBeforeCanPickup = 0;
            drop.motionX = 0;
            drop.motionY = 0;
            drop.motionZ = 0;
            drop.setPosition(
                player.posX - 0.2 + world.rand.nextDouble() * 0.4,
                player.posY - 0.6,
                player.posZ - 0.2 + world.rand.nextDouble() * 0.4);
        }
        // Experience comes along too, the way this mod's own MagnetObject does it.
        for (final EntityXPOrb orb : world.getEntitiesWithinAABB(EntityXPOrb.class, around(player, 4))) {
            if (orb.field_70532_c != 0 || !orb.isEntityAlive()) {
                continue;
            }
            if (MinecraftForge.EVENT_BUS.post(new PlayerPickupXpEvent(player, orb))) {
                continue;
            }
            world.playSoundAtEntity(player, "random.orb", 0.1F, 0.5F * (world.rand.nextFloat() * 0.7F + 1.8F));
            player.onItemPickup(orb, 1);
            player.addExperience(orb.xpValue);
            orb.setDead();
        }
    }

    private static AxisAlignedBB around(final EntityPlayer player, final int range) {
        return AxisAlignedBB
            .getBoundingBox(player.posX, player.posY, player.posZ, player.posX, player.posY, player.posZ)
            .expand(range, range, range);
    }

    /** Returned by {@link #insertIntoNetwork} when the transfer could not even be attempted. */
    private static final long INSERT_FAILED = -1L;

    /**
     * Pushes one drop into the terminal's network, returning what is left over (0 when it all went
     * in) or {@link #INSERT_FAILED} when the attempt itself blew up.
     *
     * <p>
     * AE2's {@code poweredInsert} bills the transfer to the energy source on its very first
     * instruction ({@code energySource.extractAEPower(...)}, with no null check), so a null source is
     * an immediate NPE - and that NPE used to escape {@link #tickMagnet} and abort the whole card
     * tick, which is what made the magnet appear to do nothing at all. Everything here is wrapped so
     * one bad drop can never take the tick down with it.
     */
    private static long insertIntoNetwork(final WirelessObject network, final ItemStack stack) {
        final IEnergySource host = hostOf(network);
        final IMEMonitor<IAEItemStack> storage = network.getItemInventory();
        if (host == null || storage == null) {
            return INSERT_FAILED;
        }
        try {
            final IAEItemStack request = AEApi.instance()
                .storage()
                .createItemStack(stack);
            final IAEItemStack leftover = AEApi.instance()
                .storage()
                .poweredInsert(host, storage, request, network.getSource());
            return leftover == null ? 0L : leftover.getStackSize();
        } catch (final Exception e) {
            AELog.debug(e);
            return INSERT_FAILED;
        }
    }

    /**
     * The mirror of {@link #insertIntoNetwork}: pulls {@code amount} of {@code wanted} out of the
     * network, or null when the attempt failed. Same null-source guard, same reason.
     */
    private static IAEItemStack extractFromNetwork(final WirelessObject network, final ItemStack wanted,
        final int amount) {
        final IEnergySource host = hostOf(network);
        final IMEMonitor<IAEItemStack> storage = network == null ? null : network.getItemInventory();
        if (host == null || storage == null || amount <= 0) {
            return null;
        }
        try {
            final IAEItemStack request = AEApi.instance()
                .storage()
                .createItemStack(wanted);
            request.setStackSize(amount);
            return AEApi.instance()
                .storage()
                .poweredExtraction(host, storage, request, network.getSource());
        } catch (final Exception e) {
            AELog.debug(e);
            return null;
        }
    }

    /** The magnet's three modes, stored on the terminal like the backpack terminal's. */
    public enum MagnetMode {

        OFF,
        INV,
        ME;

        public static MagnetMode of(final ItemStack terminal) {
            final NBTTagCompound data = Platform.openNbtData(terminal);
            final int ordinal = data.getByte(Constants.MAGNET_MODE_KEY);
            final MagnetMode[] values = values();
            return ordinal >= 0 && ordinal < values.length ? values[ordinal] : OFF;
        }

        public static void set(final ItemStack terminal, final MagnetMode mode) {
            Platform.openNbtData(terminal)
                .setByte(Constants.MAGNET_MODE_KEY, (byte) mode.ordinal());
        }

        public MagnetMode next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Import / export cards
    // ---------------------------------------------------------------------------------------------

    /**
     * Import: every marked player slot whose item passes the filter is emptied into the network,
     * mirroring {@code ae2importexportcard$importItem}.
     */
    private static void runImport(final ItemStack card, final ItemStack terminal, final EntityPlayer player,
        final WirelessObject network) {
        final int[] selected = ItemWcwtUpgradeCard.selectedSlots(card);
        final CardInventory filter = ItemWcwtUpgradeCard.filter(card);
        final int active = Math.min(ItemWcwtUpgradeCard.filterSlotCount(card), filter.getSizeInventory());
        final boolean fuzzy = ItemWcwtUpgradeCard.hasUpgrade(card, fuzzyCard());
        final FuzzyMode fuzzyMode = ItemWcwtUpgradeCard.fuzzyMode(card);
        final boolean invert = ItemWcwtUpgradeCard.hasUpgrade(card, inverterCard());
        final IInventory inventory = player.inventory;

        for (int i = 0; i < selected.length && i < inventory.getSizeInventory(); i++) {
            if (selected[i] < 1) {
                continue;
            }
            final ItemStack inSlot = inventory.getStackInSlot(i);
            if (inSlot == null) {
                continue;
            }
            // Never import the terminal itself.
            if (Platform.isSameItemPrecise(inSlot, terminal)) {
                continue;
            }
            if (!passesFilter(inSlot, filter, active, fuzzy, fuzzyMode, invert)) {
                continue;
            }
            final long leftover = insertIntoNetwork(network, inSlot);
            if (leftover == INSERT_FAILED) {
                continue;
            }
            final long inserted = inSlot.stackSize - leftover;
            if (inserted <= 0) {
                continue;
            }
            inSlot.stackSize -= (int) inserted;
            if (inSlot.stackSize <= 0) {
                inventory.setInventorySlotContents(i, null);
            }
        }
    }

    /**
     * Export: every player slot whose mark points at a filter slot gets that filter's item pulled
     * out of the network. A speed card in the card's own upgrade slots raises the transfer from one
     * item to a full stack, exactly like the addon.
     */
    private static void runExport(final ItemStack card, final ItemStack terminal, final EntityPlayer player,
        final WirelessObject network) {
        final int[] selected = ItemWcwtUpgradeCard.selectedSlots(card);
        final CardInventory filter = ItemWcwtUpgradeCard.filter(card);
        final int batch = ItemWcwtUpgradeCard.hasUpgrade(card, speedCard()) ? 64 : 1;
        final IInventory inventory = player.inventory;

        for (int i = 0; i < selected.length && i < inventory.getSizeInventory(); i++) {
            final int filterIndex = selected[i] - 1;
            if (filterIndex < 0 || filterIndex >= filter.getSizeInventory()) {
                continue;
            }
            final ItemStack wanted = filter.getStackInSlot(filterIndex);
            if (wanted == null) {
                continue;
            }
            final int space = spaceFor(inventory, i, wanted, batch);
            if (space <= 0) {
                continue;
            }
            final IAEItemStack extracted = extractFromNetwork(network, wanted, space);
            if (extracted == null || extracted.getStackSize() <= 0) {
                continue;
            }
            final ItemStack current = inventory.getStackInSlot(i);
            if (current != null && Platform.isSameItemPrecise(current, wanted)) {
                current.stackSize += (int) extracted.getStackSize();
                inventory.setInventorySlotContents(i, current);
            } else if (current == null) {
                final ItemStack fresh = wanted.copy();
                fresh.stackSize = (int) extracted.getStackSize();
                inventory.setInventorySlotContents(i, fresh);
            }
        }
    }

    /** How much of {@code wanted} player slot {@code index} will take. */
    private static int spaceFor(final IInventory inventory, final int index, final ItemStack wanted, final int batch) {
        final ItemStack current = inventory.getStackInSlot(index);
        if (current == null) {
            return Math.min(batch, wanted.getMaxStackSize());
        }
        if (!Platform.isSameItemPrecise(current, wanted)) {
            return 0;
        }
        return Math.min(batch, Math.max(0, current.getMaxStackSize() - current.stackSize));
    }

    /**
     * The card's ghost filter, with the addon's own rule
     * ({@code AEKeyFilterUtil.passesFilter}): the item must match one of the activated ghost slots,
     * and an inverter card flips the answer - so an empty filter imports nothing, and an empty
     * filter with an inverter imports everything.
     *
     * <p>
     * Without a fuzzy card the ghost has to be the very same stack. With one, the card's own
     * {@code FuzzyMode} takes over - the addon's {@code CardConfigManager} registers
     * {@code Settings.FUZZY_MODE} for exactly this: {@code IGNORE_ALL} compares the item alone,
     * the percentage modes also accept a damaged tool whose damage sits in the same
     * damage-percentage bucket as the ghost. NBT never matters, which is what the guide's "ignore
     * item NBT" means.
     */
    private static boolean passesFilter(final ItemStack stack, final CardInventory filter, final int active,
        final boolean fuzzyCard, final FuzzyMode fuzzyMode, final boolean invert) {
        boolean matches = false;
        for (int i = 0; i < active; i++) {
            final ItemStack ghost = filter.getStackInSlot(i);
            if (ghost == null) {
                continue;
            }
            if (matchesGhost(ghost, stack, fuzzyCard, fuzzyMode)) {
                matches = true;
                break;
            }
        }
        return invert != matches;
    }

    private static boolean matchesGhost(final ItemStack ghost, final ItemStack stack, final boolean fuzzyCard,
        final FuzzyMode fuzzyMode) {
        if (ghost.getItem() != stack.getItem()) {
            return false;
        }
        if (!fuzzyCard) {
            return Platform.isSameItemPrecise(ghost, stack);
        }
        if (fuzzyMode == FuzzyMode.IGNORE_ALL) {
            return true;
        }
        // AE2's damage buckets: the ghost and the stack match when their damage falls into the same
        // percentage bucket of the tool's max damage. Undamageable items have one bucket.
        final int maxDamage = Math.max(ghost.getMaxDamage(), 1);
        if (ghost.getMaxDamage() <= 0 || stack.getMaxDamage() <= 0) {
            return true;
        }
        final int breakPoint = Math.max(1, fuzzyMode.calculateBreakPoint(maxDamage));
        return ghost.getItemDamage() / breakPoint == stack.getItemDamage() / breakPoint;
    }

    // ---------------------------------------------------------------------------------------------
    // Shared plumbing
    // ---------------------------------------------------------------------------------------------

    /** Opens the terminal's network, or null when it is unlinked, unpowered or out of range. */
    private static WirelessObject openTerminal(final ItemStack terminal, final World world, final EntityPlayer player,
        final int slot) {
        if (terminal == null || !(terminal.getItem() instanceof com.asdflj.wtct.inventory.item.IItemInventory)) {
            return null;
        }
        try {
            final WirelessObject obj = new WirelessObject(terminal, world, slot, 0, 0, player);
            // The item's own inventory subclass, the way the terminal items open themselves.
            // WirelessTerminal is abstract, so asking for it throws and every card silently saw no
            // network - which is exactly why the magnet's "into the network" mode did nothing.
            obj.getInventory(inventoryClass());
            // Building the host hands the object its energy source, so the transfers below draw power
            // from the terminal exactly like the terminal's own screens do.
            return obj.getItemInventory() == null ? null : obj;
        } catch (final Exception e) {
            // Not "ignored": an unlinked or out-of-range terminal is normal, but a genuine defect
            // here used to be invisible, which is how a broken energy source went unnoticed.
            AELog.debug(e);
            return null;
        }
    }

    /** The inventory class a terminal item opens on itself - see {@link #openTerminal}. */
    private static Class<? extends WirelessTerminal> inventoryClass() {
        return com.asdflj.wtct.inventory.item.WirelessDualInterfaceTerminalInventory.class;
    }

    /**
     * The energy source behind an opened terminal - what the transfers are billed to.
     *
     * <p>
     * This has to ask for the <em>concrete</em> inventory class, not {@link WirelessTerminal}: that
     * one is abstract, so {@code getConstructor(...).newInstance(...)} - which is all
     * {@code WirelessObject.getInventory} does - always threw {@code InstantiationException}. The
     * old code swallowed it and returned null, and AE2's {@code poweredInsert} dereferences the
     * source immediately, so every network transfer NPE'd out of the item's own tick. Asking for the
     * class {@link #openTerminal} already built also means this returns the very object whose
     * constructor ran {@code obj.setEnergySource(this)}.
     */
    private static IEnergySource hostOf(final WirelessObject network) {
        if (network == null) {
            return null;
        }
        try {
            return (WirelessTerminal) network.getInventory(inventoryClass());
        } catch (final Exception e) {
            AELog.debug(e);
            return null;
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Block picker card
    // ---------------------------------------------------------------------------------------------

    /**
     * How many blocks a pick-block pulls. The addon keeps this on the terminal item and gives it its
     * own screen; here the card's header button cycles through the same kind of presets, so the
     * setting needs no separate screen.
     */
    private static final int[] PICKER_AMOUNTS = { 1, 8, 16, 32, 64 };

    /** NBT key of the block picker amount, on the terminal item. */
    private static final String TAG_PICKER_AMOUNT = "BlockPickerAmount";

    public static int pickerAmount(final ItemStack terminal) {
        final NBTTagCompound data = Platform.openNbtData(terminal);
        return data.hasKey(TAG_PICKER_AMOUNT) ? data.getInteger(TAG_PICKER_AMOUNT) : PICKER_AMOUNTS[0];
    }

    /** Steps to the next preset and returns the new value. */
    public static int nextPickerAmount(final ItemStack terminal) {
        final int current = pickerAmount(terminal);
        int index = 0;
        for (int i = 0; i < PICKER_AMOUNTS.length; i++) {
            if (PICKER_AMOUNTS[i] == current) {
                index = i;
                break;
            }
        }
        final int next = PICKER_AMOUNTS[(index + 1) % PICKER_AMOUNTS.length];
        Platform.openNbtData(terminal)
            .setInteger(TAG_PICKER_AMOUNT, next);
        return next;
    }

    /**
     * Writes an exact amount, the addon's {@code BlockPickerAmountScreen} writing its number entry
     * ({@code ae2ImportExportCard$setBlockPickerAmount}). Kept within AE2's own bounds for a stack,
     * which is what the picker can actually pull in one go.
     */
    public static int setPickerAmount(final ItemStack terminal, final long amount) {
        final int clamped = (int) Math.max(1L, Math.min(Integer.MAX_VALUE, amount));
        Platform.openNbtData(terminal)
            .setInteger(TAG_PICKER_AMOUNT, clamped);
        return clamped;
    }

    /**
     * The block picker's server half, mirroring {@code BlockPickerHandler.handle}: only in survival,
     * only within reach, only when the player does not already carry the block, and only through a
     * terminal with the card that is actually linked to a reachable network.
     */
    public static void pickBlock(final EntityPlayer player, final int x, final int y, final int z) {
        if (player == null || player.worldObj == null || player.capabilities.isCreativeMode) {
            return;
        }
        final World world = player.worldObj;
        if (!world.blockExists(x, y, z)) {
            return;
        }
        final double reach = 5.0D;
        final double dx = x + 0.5D - player.posX;
        final double dy = y + 0.5D - (player.posY + player.getEyeHeight());
        final double dz = z + 0.5D - player.posZ;
        if (dx * dx + dy * dy + dz * dz > reach * reach) {
            return;
        }
        final net.minecraft.block.Block block = world.getBlock(x, y, z);
        if (block == null || block.isAir(world, x, y, z)) {
            return;
        }
        final net.minecraft.util.MovingObjectPosition target = new net.minecraft.util.MovingObjectPosition(
            x,
            y,
            z,
            0,
            net.minecraft.util.Vec3.createVectorHelper(x + 0.5D, y + 0.5D, z + 0.5D),
            false);
        final ItemStack picked = block.getPickBlock(target, world, x, y, z, player);
        if (picked == null) {
            return;
        }
        final IInventory inventory = player.inventory;
        if (containsStack(inventory, picked)) {
            return;
        }
        final int destination = player.inventory.getFirstEmptyStack();
        if (destination < 0) {
            return;
        }
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            final ItemStack terminal = inventory.getStackInSlot(slot);
            if (terminal == null || !ItemWcwtUpgradeCard.isInstalled(terminal, ItemWcwtUpgradeCard.Kind.BLOCK_PICKER)) {
                continue;
            }
            // The settings screen's "pick block" switch is the card's master switch.
            if (!switchEnabled(terminal, "PickBlock")) {
                continue;
            }
            if (extractPick(player, terminal, picked, destination)) {
                player.inventory.currentItem = destination < 9 ? destination : player.inventory.currentItem;
                player.inventoryContainer.detectAndSendChanges();
                return;
            }
        }
    }

    /**
     * One of the settings screen's master switches: every card feature defaults to on, and the
     * switch is a plain boolean on the terminal's NBT - the same "settings live on the item" rule
     * WCWT's own settings screen follows.
     */
    private static boolean switchEnabled(final ItemStack terminal, final String key) {
        final NBTTagCompound tag = Platform.openNbtData(terminal);
        return !tag.hasKey(key) || tag.getBoolean(key);
    }

    private static boolean containsStack(final IInventory inventory, final ItemStack wanted) {
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            final ItemStack is = inventory.getStackInSlot(i);
            if (is != null && Platform.isSameItemPrecise(is, wanted)) {
                return true;
            }
        }
        return false;
    }

    /** Pulls up to the configured amount out of the network and puts it in the player's inventory. */
    private static boolean extractPick(final EntityPlayer player, final ItemStack terminal, final ItemStack wanted,
        final int destination) {
        final WirelessObject network = openTerminal(terminal, player.worldObj, player, 0);
        if (network == null) {
            return false;
        }
        final int capacity = freeSpaceFor(player.inventory, wanted);
        final int amount = Math.min(pickerAmount(terminal), capacity);
        if (amount <= 0) {
            return false;
        }
        final IAEItemStack extracted = extractFromNetwork(network, wanted, amount);
        if (extracted == null || extracted.getStackSize() <= 0) {
            return false;
        }
        insertInto(player.inventory, wanted, (int) extracted.getStackSize(), destination);
        return true;
    }

    /** Room for {@code wanted} across the player's 36 inventory slots. */
    private static int freeSpaceFor(final IInventory inventory, final ItemStack wanted) {
        int space = 0;
        for (int i = 0; i < 36 && i < inventory.getSizeInventory(); i++) {
            final ItemStack is = inventory.getStackInSlot(i);
            if (is == null) {
                space += wanted.getMaxStackSize();
            } else if (Platform.isSameItemPrecise(is, wanted)) {
                space += Math.max(0, wanted.getMaxStackSize() - is.stackSize);
            }
        }
        return space;
    }

    /** Fills the destination slot first, then tops up the rest of the inventory. */
    private static void insertInto(final IInventory inventory, final ItemStack wanted, final int amount,
        final int firstSlot) {
        int remaining = insertIntoSlot(inventory, wanted, amount, firstSlot);
        for (int i = 0; i < 36 && i < inventory.getSizeInventory() && remaining > 0; i++) {
            if (i != firstSlot) {
                remaining = insertIntoSlot(inventory, wanted, remaining, i);
            }
        }
    }

    private static int insertIntoSlot(final IInventory inventory, final ItemStack wanted, final int amount,
        final int slot) {
        final ItemStack existing = inventory.getStackInSlot(slot);
        if (existing != null && !Platform.isSameItemPrecise(existing, wanted)) {
            return amount;
        }
        final int current = existing == null ? 0 : existing.stackSize;
        final int insert = Math.min(amount, wanted.getMaxStackSize() - current);
        if (insert <= 0) {
            return amount;
        }
        if (existing == null) {
            final ItemStack fresh = wanted.copy();
            fresh.stackSize = insert;
            inventory.setInventorySlotContents(slot, fresh);
        } else {
            existing.stackSize += insert;
            inventory.setInventorySlotContents(slot, existing);
        }
        return amount - insert;
    }

    private static IItemDefinition fuzzyCard() {
        return AEApi.instance()
            .definitions()
            .materials()
            .cardFuzzy();
    }

    private static IItemDefinition inverterCard() {
        return AEApi.instance()
            .definitions()
            .materials()
            .cardInverter();
    }

    private static IItemDefinition speedCard() {
        return AEApi.instance()
            .definitions()
            .materials()
            .cardSpeed();
    }
}
