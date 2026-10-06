package com.asdflj.wtct.common.parts;

import static appeng.util.item.AEFluidStackType.FLUID_STACK_TYPE;
import static appeng.util.item.AEItemStackType.ITEM_STACK_TYPE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.util.ForgeDirection;

import com.asdflj.wtct.common.item.ItemExBusPart;
import com.asdflj.wtct.inventory.InventoryHandler;
import com.asdflj.wtct.inventory.gui.GuiType;
import com.asdflj.wtct.loader.ItemAndBlockHolder;
import com.asdflj.wtct.util.BlockPos;
import com.glodblock.github.common.item.ItemFluidPacket;

import appeng.api.AEApi;
import appeng.api.config.AccessRestriction;
import appeng.api.config.ExtractionMode;
import appeng.api.config.FuzzyMode;
import appeng.api.config.IncludeExclude;
import appeng.api.config.Settings;
import appeng.api.config.StorageFilter;
import appeng.api.config.Upgrades;
import appeng.api.networking.IGridNode;
import appeng.api.networking.events.MENetworkCellArrayUpdate;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.security.MachineSource;
import appeng.api.networking.storage.IBaseMonitor;
import appeng.api.networking.ticking.ITickManager;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.parts.IPartHost;
import appeng.api.storage.IExternalStorageHandler;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.IStorageBusMonitor;
import appeng.api.storage.StorageName;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IAEStackType;
import appeng.api.storage.data.IItemList;
import appeng.api.util.IConfigManager;
import appeng.core.settings.TickRates;
import appeng.helpers.Reflected;
import appeng.hooks.TickHandler;
import appeng.me.GridAccessException;
import appeng.me.storage.MEInventoryHandler;
import appeng.me.storage.MEMonitorPassThrough;
import appeng.me.storage.StorageBusInventoryHandler;
import appeng.parts.misc.PartStorageBus;
import appeng.tile.inventory.IAEStackInventory;
import appeng.util.IterationCounter;
import appeng.util.Platform;
import appeng.util.prioitylist.FuzzyPriorityList;
import appeng.util.prioitylist.OreFilteredList;
import appeng.util.prioitylist.PrecisePriorityList;

/**
 * The ME extended storage bus: AE2's storage bus with a filter grid that grows to 63 slots and which exposes the faced
 * inventory as item storage and fluid storage at the same time, the way AE2 1.21's bus does.
 *
 * <p>
 * rv3's {@link PartStorageBus} hardwires its single handler to the item stack type, and the whole machinery around it
 * (the memoized handler build, the delayed cache resets, the change forwarding) hangs off private fields. This class
 * therefore keeps the parent's one - which stays inert, since every path that would run it is overridden - and carries
 * the machinery over once per stack type: one state for the item view of the faced inventory, one for the fluid view.
 * The shared 63 slot config grid feeds both partitions; slots parked with a fluid become part of the fluid partition
 * only, everything else the parent's logic already does is inherited as is.
 */
public class PartExStorageBus extends PartStorageBus implements ExBusScreenHost {

    /**
     * One view of the faced inventory. Mirrors the private state {@code PartStorageBus} keeps for its single handler:
     * the handler itself, its monitor when the target offers one, the tile hash the rebuild memo compares, the
     * once-pass flag for the first change report after a rebuild, and the two-phase rebuild request.
     */
    private static final class ExSideState {

        final IAEStackType<?> type;
        MEInventoryHandler handler;
        IStorageBusMonitor<?> monitor;
        int handlerHash;
        boolean readOncePass;
        boolean cached;
        byte resetLogic;
        boolean pendingDelayedCacheReset;

        ExSideState(final IAEStackType<?> type) {
            this.type = type;
        }
    }

    private final BaseActionSource mySrc = new MachineSource(this);
    private final ExSideState itemState = new ExSideState(ITEM_STACK_TYPE);
    private final ExSideState fluidState = new ExSideState(FLUID_STACK_TYPE);

    @Reflected
    public PartExStorageBus(final ItemStack is) {
        super(is);
    }

    @Override
    protected int getUpgradeSlots() {
        return ExBusSlots.UPGRADE_SLOTS;
    }

    @Override
    public ItemStack getPrimaryGuiIcon() {
        return new ItemStack(ItemAndBlockHolder.ITEM_EX_BUS, 1, ItemExBusPart.META_STORAGE_BUS);
    }

    @Override
    public boolean onPartActivate(final EntityPlayer player, final Vec3 pos) {
        if (player.isSneaking()) {
            return false;
        }

        if (Platform.isClient()) {
            return true;
        }

        InventoryHandler.openGui(
            player,
            this.getTile()
                .getWorldObj(),
            new BlockPos(this.getTile()),
            this.getSide(),
            GuiType.EX_STORAGE_BUS);
        return true;
    }

    /**
     * The network asks for the handlers per stack type: the item view of the faced inventory and the fluid view are two
     * independent cells on the same provider, each present only while the target offers that kind of storage.
     */
    @Override
    public List<IMEInventoryHandler> getCellArray(final IAEStackType<?> type) {
        final ExSideState state = this.stateFor(type);
        if (state != null) {
            final MEInventoryHandler out = this.getProxy()
                .isActive() ? this.getExHandler(state) : null;
            if (out != null) {
                return Collections.singletonList(out);
            }
        }

        return Collections.emptyList();
    }

    /**
     * The inherited single-type hook, kept for anything that still asks rv3-style: it is the item view. The partition
     * screen works off {@link #getExHandler(IAEStackType)} instead, which can address both views.
     */
    @Override
    public MEInventoryHandler getInternalHandler() {
        return this.getExHandler(this.itemState);
    }

    /** The handler exposing the faced inventory as the given stack type, built on first ask. */
    public MEInventoryHandler getExHandler(final IAEStackType<?> type) {
        final ExSideState state = this.stateFor(type);
        return state == null ? null : this.getExHandler(state);
    }

    @Override
    public boolean isValid(final Object verificationToken) {
        return this.itemState.handler == verificationToken || this.fluidState.handler == verificationToken;
    }

    /**
     * Change reports come from either view's monitor; the type of the first changed stack tells which view speaks, and
     * the alteration is posted to the network under that type after the view's extract partition had its say.
     */
    @Override
    public void postChange(final IBaseMonitor<IAEStack<?>> monitor, final Iterable<IAEStack<?>> change,
        final BaseActionSource source) {
        try {
            if (!this.getProxy()
                .isActive()) {
                return;
            }

            IAEStack<?> first = null;
            for (final IAEStack<?> is : change) {
                first = is;
                break;
            }
            if (first == null) {
                return;
            }

            final IAEStackType<?> type = first.getStackType();
            final ExSideState state = this.stateFor(type);
            if (state == null) {
                return;
            }

            if (!state.readOncePass) {
                final AccessRestriction currentAccess = (AccessRestriction) this.getConfigManager()
                    .getSetting(Settings.ACCESS);
                final MEInventoryHandler handler = state.handler;
                if (!currentAccess.hasPermission(AccessRestriction.READ) && (handler == null || !handler.isVisible())) {
                    return;
                }
            }

            final Iterable<IAEStack<?>> filteredChanges = this.filterChanges(state, change, state.readOncePass);
            state.readOncePass = false;
            if (filteredChanges == null) {
                return;
            }
            this.getProxy()
                .getStorage()
                .postAlterationOfStoredItems(type, filteredChanges, this.mySrc);
        } catch (final GridAccessException e) {
            // :(
        }
    }

    /**
     * Filters the changes to only include stacks that pass the view's extract filter. Will return null if none of the
     * changes match the filter.
     */
    @Nullable
    private Iterable<IAEStack<?>> filterChanges(final ExSideState state, final Iterable<IAEStack<?>> change,
        final boolean readOncePass) {
        if (readOncePass) {
            return change;
        }

        final MEInventoryHandler handler = state.handler;
        if (handler != null && handler.isExtractFilterActive()
            && !handler.getExtractPartitionList()
                .isEmpty()) {
            final List<IAEStack<?>> filteredChanges = new ArrayList<>();
            final Predicate<IAEStack<?>> extractFilterCondition = handler.getExtractFilterCondition();
            for (final IAEStack<?> changedItem : change) {
                if (extractFilterCondition.test(changedItem)) {
                    filteredChanges.add(changedItem);
                }
            }
            return filteredChanges.isEmpty() ? null : Collections.unmodifiableList(filteredChanges);
        }
        return change;
    }

    @Override
    public void onNeighborChanged() {
        super.onNeighborChanged();
        this.resetEx(false);
    }

    @Override
    public void updateSetting(final IConfigManager manager, final Enum settingName, final Enum newValue) {
        super.updateSetting(manager, settingName, newValue);
        this.resetEx(true);
    }

    @Override
    public void upgradesChanged() {
        super.upgradesChanged();
        this.resetEx(true);
    }

    @Override
    public void setPriority(final int newValue) {
        super.setPriority(newValue);
        this.resetEx(true);
    }

    @Override
    public void setFilter(final String filter) {
        super.setFilter(filter);
        this.resetEx(true);
    }

    @Override
    public TickingRequest getTickingRequest(final IGridNode node) {
        return new TickingRequest(
            TickRates.StorageBus.getMin(),
            TickRates.StorageBus.getMax(),
            this.itemState.monitor == null && this.fluidState.monitor == null,
            true);
    }

    /**
     * Both views tick on the one device: pending rebuilds run per side, both monitors poll, and the more awake of the
     * two tick verdicts wins so one sleeping view cannot starve the other.
     */
    @Override
    public TickRateModulation tickingRequest(final IGridNode node, final int ticksSinceLastCall) {
        if (this.itemState.resetLogic != 0) {
            this.resetExCache(this.itemState);
        }
        if (this.fluidState.resetLogic != 0) {
            this.resetExCache(this.fluidState);
        }

        TickRateModulation result = TickRateModulation.SLEEP;
        if (this.itemState.monitor != null) {
            result = this.moreAwake(result, this.itemState.monitor.onTick());
        }
        if (this.fluidState.monitor != null) {
            result = this.moreAwake(result, this.fluidState.monitor.onTick());
        }
        return result;
    }

    private TickRateModulation moreAwake(final TickRateModulation a, final TickRateModulation b) {
        return b.ordinal() > a.ordinal() ? b : a;
    }

    /**
     * The per-view twin of rv3's delayed rebuild: GT tanks and friends may not expose storage until their placement
     * initialization finishes, so a neighbor with no view yet is retried instead of being cached as empty.
     */
    private void scheduleDelayedCacheReset(final ExSideState state) {
        if (state.pendingDelayedCacheReset) {
            return;
        }

        final IPartHost host = this.getHost();
        if (host == null) {
            return;
        }

        final TileEntity self = host.getTile();
        if (self == null) {
            return;
        }

        final net.minecraft.world.World world = self.getWorldObj();
        if (world == null || world.isRemote) {
            return;
        }

        state.pendingDelayedCacheReset = true;
        TickHandler.INSTANCE.addCallable(world, w -> {
            try {
                this.resetExCache(state);
            } finally {
                state.pendingDelayedCacheReset = false;
            }
            return true;
        });
    }

    /**
     * Requests a rebuild of both views and wakes the device. {@code fullReset} also drops the memo hashes; a neighbor
     * update must not downgrade a pending configuration-driven full reset.
     */
    private void resetEx(final boolean fullReset) {
        if (this.getHost() == null || this.getHost()
            .getTile() == null
            || this.getHost()
                .getTile()
                .getWorldObj() == null
            || this.getHost()
                .getTile()
                .getWorldObj().isRemote) {
            return;
        }

        this.bumpLogic(this.itemState, fullReset);
        this.bumpLogic(this.fluidState, fullReset);

        try {
            this.getProxy()
                .getTick()
                .alertDevice(
                    this.getProxy()
                        .getNode());
        } catch (final GridAccessException e) {
            // :P
        }
    }

    /** Lets the container tell the part that config slots changed - the parent's own hook is inert here. */
    public void resetExCache() {
        this.resetEx(true);
    }

    private void bumpLogic(final ExSideState state, final boolean fullReset) {
        if (fullReset) {
            state.resetLogic = 2;
        } else if (state.resetLogic == 0) {
            state.resetLogic = 1;
        }
    }

    /**
     * The per-view twin of rv3's cache reset: rebuild the handler, post a cell array update when it changed, tick the
     * monitor and diff the view's contents for the network - before against the old handler, after against the new.
     */
    private void resetExCache(final ExSideState state) {
        final boolean fullReset = state.resetLogic == 2;
        state.resetLogic = 0;

        final MEInventoryHandler previousHandler = state.handler;
        final int previousHandlerHash = state.handlerHash;

        final IMEInventory in = this.getExHandler(state);

        IItemList before = state.type.createList();
        if (in != null) {
            before = in.getAvailableItems(before, IterationCounter.fetchNewId());
        }

        state.cached = false;
        if (fullReset) {
            state.handlerHash = 0;
        }

        final IMEInventory out = this.getExHandler(state);
        if (!fullReset && out == null && state.handlerHash != 0 && state.handlerHash != previousHandlerHash) {
            // Some newly placed neighbors, such as GT tanks, expose storage only after placement initialization.
            this.scheduleDelayedCacheReset(state);
        }
        if (state.handler != previousHandler || state.handlerHash != previousHandlerHash) {
            try {
                this.getProxy()
                    .getGrid()
                    .postEvent(new MENetworkCellArrayUpdate());
            } catch (final GridAccessException ignored) {}
        }

        if (state.monitor != null) {
            state.monitor.onTick();
        }

        IItemList after = state.type.createList();
        if (out != null) {
            after = out.getAvailableItems(after, IterationCounter.fetchNewId());
        }

        Platform.postListChanges(before, after, this, this.mySrc);
    }

    /**
     * The per-view twin of rv3's handler build: wrap whatever the target offers for this stack type in the storage bus
     * handler, apply access, priority, sticky and the partition built from the shared config grid, and listen on the
     * target's monitor when it has one. The result is memoized until a reset drops the flag.
     */
    private MEInventoryHandler getExHandler(final ExSideState state) {
        if (state.cached) {
            return state.handler;
        }

        final boolean wasSleeping = this.itemState.monitor == null && this.fluidState.monitor == null;

        state.cached = true;
        final TileEntity self = this.getHost()
            .getTile();

        final ForgeDirection side = this.getSide();
        final TileEntity target = self.getWorldObj()
            .getTileEntity(self.xCoord + side.offsetX, self.yCoord + side.offsetY, self.zCoord + side.offsetZ);

        final int newHandlerHash = Platform.generateTileHash(target);

        if (state.handlerHash == newHandlerHash && state.handlerHash != 0 && state.handler != null) {
            return state.handler;
        }

        state.handlerHash = newHandlerHash;
        state.handler = null;
        state.monitor = null;
        state.readOncePass = true;
        if (target != null) {
            final IExternalStorageHandler esh = AEApi.instance()
                .registries()
                .externalStorage()
                .getHandler(target, side.getOpposite(), state.type, this.mySrc);
            if (esh != null) {
                final IMEInventory inv = esh.getInventory(target, side.getOpposite(), state.type, this.mySrc);

                if (inv instanceof IStorageBusMonitor<?>m) {
                    m.setMode(
                        (StorageFilter) this.getConfigManager()
                            .getSetting(Settings.STORAGE_FILTER));
                    m.setActionSource(new MachineSource(this));
                    state.monitor = m;
                }

                if (inv instanceof MEMonitorPassThrough<?>h) {
                    h.setMode(
                        (StorageFilter) this.getConfigManager()
                            .getSetting(Settings.STORAGE_FILTER));
                }

                if (inv != null) {
                    final MEInventoryHandler handler = new StorageBusInventoryHandler<>(inv, state.type);

                    final AccessRestriction currentAccess = (AccessRestriction) this.getConfigManager()
                        .getSetting(Settings.ACCESS);
                    handler.setBaseAccess(currentAccess);
                    handler.setWhitelist(
                        this.getInstalledUpgrades(Upgrades.INVERTER) > 0 ? IncludeExclude.BLACKLIST
                            : IncludeExclude.WHITELIST);
                    handler.setPriority(this.getPriority());

                    boolean extractRights = currentAccess == AccessRestriction.READ;
                    final ExtractionMode currentExtractionMode = (ExtractionMode) this.getConfigManager()
                        .getSetting(Settings.EXTRACTION_MODE);
                    if (currentExtractionMode == ExtractionMode.STRICT) {
                        extractRights |= currentAccess == AccessRestriction.READ_WRITE;
                    }

                    handler.setIsExtractFilterActive(extractRights);

                    if (this.getInstalledUpgrades(Upgrades.STICKY) > 0) {
                        handler.setSticky(true);
                    }

                    this.applyPartition(state, handler);

                    if (inv instanceof IMEMonitor<?>m) {
                        m.addListener(this, handler);
                    }

                    state.handler = handler;
                }
            }
        }

        if (state.handler == null && target != null) {
            // The neighbor exists but may not expose storage until its placement initialization finishes.
            state.cached = false;
            this.scheduleDelayedCacheReset(state);
        }

        // update sleep state... the device sleeps only when both views are monitor-less, and wakes the
        // moment either grows one.
        final boolean sleeping = this.itemState.monitor == null && this.fluidState.monitor == null;
        if (wasSleeping != sleeping) {
            try {
                final ITickManager tm = this.getProxy()
                    .getTick();
                if (sleeping) {
                    tm.sleepDevice(
                        this.getProxy()
                            .getNode());
                } else {
                    tm.wakeDevice(
                        this.getProxy()
                            .getNode());
                }
            } catch (final GridAccessException e) {
                // :(
            }
        }

        return state.handler;
    }

    /**
     * Builds the view's partition from the shared config grid. The two views read different slots out of the same grid:
     * fluid slots - parked as fluid stacks, however they arrived - belong to the fluid partition, everything else to
     * the item one. An ore filter replaces the item partition wholesale, as rv3's does; fluids keep their exact
     * partition since 1.7.10 fluids have no fuzzy or ore-dict notion.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void applyPartition(final ExSideState state, final MEInventoryHandler handler) {
        final boolean hasOreFilter = this.getInstalledUpgrades(Upgrades.ORE_FILTER) > 0;

        if (!hasOreFilter) {
            final IItemList priorityList = state.type.createList();

            final int slotsToUse = 18 + this.getInstalledUpgrades(Upgrades.CAPACITY) * 9;
            final IAEStackInventory config = this.getAEInventoryByName(StorageName.CONFIG);
            for (int x = 0; x < config.getSizeInventory() && x < slotsToUse; x++) {
                final IAEStack<?> is = this.convertFor(state.type, config.getAEStackInSlot(x));
                if (is != null) {
                    priorityList.add(is);
                }
            }

            if (state.type == ITEM_STACK_TYPE && this.getInstalledUpgrades(Upgrades.FUZZY) > 0) {
                final FuzzyPriorityList<IAEItemStack> partitionList = new FuzzyPriorityList<>(
                    priorityList,
                    (FuzzyMode) this.getConfigManager()
                        .getSetting(Settings.FUZZY_MODE));
                handler.setPartitionList(partitionList);
                handler.setExtractPartitionList(partitionList);
            } else {
                final PrecisePriorityList partitionList = new PrecisePriorityList(priorityList);
                handler.setPartitionList(partitionList);
                handler.setExtractPartitionList(partitionList);
            }
        } else {
            final OreFilteredList partitionList = new OreFilteredList(this.getFilter());
            handler.setPartitionList(partitionList);
            handler.setExtractPartitionList(partitionList);
        }
    }

    /**
     * Picks the slots that belong to one view's partition. The item view ignores fluids however they are parked
     * (real fluid stacks or item-wrapped fluid packets); the fluid view takes exactly those.
     */
    @Nullable
    private IAEStack<?> convertFor(final IAEStackType<?> type, @Nullable final IAEStack<?> is) {
        if (is == null) {
            return null;
        }
        if (type == ITEM_STACK_TYPE) {
            if (is instanceof IAEFluidStack) {
                return null;
            }
            return is instanceof IAEItemStack ais && ais.getItem() instanceof ItemFluidPacket ? null : is;
        }
        if (type == FLUID_STACK_TYPE) {
            if (is instanceof IAEFluidStack) {
                return is;
            }
            return is instanceof IAEItemStack ais && ais.getItem() instanceof ItemFluidPacket
                ? ItemFluidPacket.getFluidAEStack(ais)
                : null;
        }
        return null;
    }

    @Nullable
    private ExSideState stateFor(final IAEStackType<?> type) {
        if (type == ITEM_STACK_TYPE) {
            return this.itemState;
        }
        if (type == FLUID_STACK_TYPE) {
            return this.fluidState;
        }
        return null;
    }

    @Override
    public String getBusDisplayName() {
        return this.getItemStack()
            .getDisplayName();
    }

    @Override
    public IAEStackInventory getExConfig() {
        return this.getAEInventoryByName(StorageName.CONFIG);
    }

    @Override
    public IAEStackType<?> getGuiStackType() {
        return this.getStackType();
    }
}
