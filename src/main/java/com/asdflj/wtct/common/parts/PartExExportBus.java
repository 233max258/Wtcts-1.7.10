package com.asdflj.wtct.common.parts;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.util.Constants.NBT;

import com.asdflj.wtct.inventory.InventoryHandler;
import com.asdflj.wtct.inventory.gui.GuiType;
import com.asdflj.wtct.util.BlockPos;
import com.google.common.collect.ImmutableSet;

import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.config.InsertionMode;
import appeng.api.config.SchedulingMode;
import appeng.api.config.Settings;
import appeng.api.config.Upgrades;
import appeng.api.config.YesNo;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.StorageName;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IAEStackType;
import appeng.helpers.MultiCraftingTracker;
import appeng.helpers.Reflected;
import appeng.me.GridAccessException;
import appeng.parts.automation.PartExportBus;
import appeng.tile.inventory.IAEStackInventory;
import appeng.util.InventoryAdaptor;
import appeng.util.Platform;

/**
 * The ME extended export bus: AE2's export bus with a filter grid that grows to 63 slots and a configurable speed
 * multiplier.
 *
 * <p>
 * Unlike the import side, the export loop cannot simply be inherited. {@code PartBaseExportBus} keeps its crafting
 * tracker sized to nine slots and its slot-selection state private, so a 63-slot grid would index straight past the end
 * of that tracker. The ticking loop is therefore carried over here against a tracker of its own; everything it calls
 * ({@code pushItemIntoTarget}, {@code doFuzzy}, {@code doOreDict}, {@code canInjectStackToTarget}) is inherited as is.
 */
public class PartExExportBus extends PartExportBus implements ExBusScreenHost {

    /**
     * Own NBT keys: the parent writes its own nine-slot inventory and tracker under {@code "config"}/{@code "links-N"}.
     */
    private static final String CONFIG_TAG = "config_ex";
    private static final String TRACKER_TAG = "ex_crafting";
    private static final String NEXT_SLOT_TAG = "ex_next_slot";

    private final IAEStackInventory exConfig = new IAEStackInventory(this, ExBusSlots.MAX_SLOTS, StorageName.CONFIG);
    private final MultiCraftingTracker exTracker = new MultiCraftingTracker(this, ExBusSlots.MAX_SLOTS);
    private boolean didSomething = false;
    private int nextSlot = 0;

    @Reflected
    public PartExExportBus(final ItemStack is) {
        super(is);
    }

    /**
     * Widen the stock bus window instead of ticking flat out. Inheritance would give it {@code TickRates.ExportBus} -
     * a floor of five ticks between operations, stretching to sixty while it has nothing to move - so a stock export
     * bus delivers in pulses and a quiet one can sit for seconds before it notices work. Here the window is one tick
     * at the fastest and sixty at the slowest, and {@link #doBusWork} reports {@code FASTER} on any pass that moved
     * something: the tracker then walks the rate down two ticks at a time, so a bus with a backlog eases onto a
     * one-tick cadence rather than snapping straight onto it, while one with nothing to do falls back a tick at a time
     * until it is looking once every sixty ticks. The speed multiplier keeps scaling how much each pass moves rather
     * than how often the bus looks. Sleep still follows the parent's rule (a bus with no target sleeps, and a block
     * update wakes it).
     */
    @Override
    public TickingRequest getTickingRequest(final IGridNode node) {
        return new TickingRequest(1, 60, this.isSleeping(), false);
    }

    @Override
    public String getBusDisplayName() {
        return this.getItemStack()
            .getDisplayName();
    }

    @Override
    public IAEStackInventory getExConfig() {
        return this.exConfig;
    }

    @Override
    public IAEStackType<?> getGuiStackType() {
        return this.getStackType();
    }

    @Override
    protected int getUpgradeSlots() {
        return ExBusSlots.UPGRADE_SLOTS;
    }

    /**
     * The parent's flags strip fluids so the plain bus never touches tanks; this grid carries both kinds, so the
     * target lookup may resolve to an {@code AdaptorFluidHandler} whose generic addStack routes items into the
     * inventory and fluids into {@code IFluidHandler.fill}.
     */
    @Override
    protected int getAdaptorFlags() {
        return InventoryAdaptor.ALLOW_ITEMS | InventoryAdaptor.ALLOW_FLUIDS | InventoryAdaptor.FOR_INSERTS;
    }

    @Override
    protected int availableSlots() {
        return ExBusSlots.slots(this.getInstalledUpgrades(Upgrades.CAPACITY));
    }

    @Override
    public IAEStackInventory getAEInventoryByName(final StorageName name) {
        return name == StorageName.CONFIG ? this.exConfig : super.getAEInventoryByName(name);
    }

    @Override
    public void readFromNBT(final NBTTagCompound extra) {
        super.readFromNBT(extra);
        this.exConfig.readFromNBT(extra, CONFIG_TAG);
        if (extra.hasKey(TRACKER_TAG, NBT.TAG_COMPOUND)) {
            this.exTracker.readFromNBT(extra.getCompoundTag(TRACKER_TAG));
        }
        this.nextSlot = extra.getInteger(NEXT_SLOT_TAG);
    }

    @Override
    public void writeToNBT(final NBTTagCompound extra) {
        super.writeToNBT(extra);
        this.exConfig.writeToNBT(extra, CONFIG_TAG);

        final NBTTagCompound tracker = new NBTTagCompound();
        this.exTracker.writeToNBT(tracker);
        if (tracker.hasNoTags()) {
            extra.removeTag(TRACKER_TAG);
        } else {
            extra.setTag(TRACKER_TAG, tracker);
        }
        extra.setInteger(NEXT_SLOT_TAG, this.nextSlot);
    }

    @Override
    public int calculateAmountToSend() {
        return ExBusSlots.scaleSpeed(super.calculateAmountToSend());
    }

    @Override
    public ImmutableSet<ICraftingLink> getRequestedJobs() {
        return this.exTracker.getRequestedJobs();
    }

    @Override
    public void jobStateChange(final ICraftingLink link) {
        this.exTracker.jobStateChange(link);
    }

    @Override
    protected TickRateModulation doBusWork() {
        if (!this.getProxy()
            .isActive() || !this.canDoBusWork()) {
            return TickRateModulation.IDLE;
        }

        this.itemToSend = this.calculateAmountToSend();
        // Every inherited push path records its own success in PartBaseExportBus' private copy of
        // "didSomething", which this class cannot read - the field of the same name declared below shadows it.
        // Reading the parent's flag therefore always yields false for items, fuzzy matches and ore-dict hits,
        // which used to make the bus report SLOWER forever and walk its tick rate up to the ceiling. The
        // opening budget is the dependable evidence instead: a pass that moved something spent part of it.
        final long openingBudget = this.itemToSend;
        this.didSomething = false;

        try {
            final Object target = this.getTarget();
            if (target == null) {
                return TickRateModulation.SLEEP;
            }

            final IMEMonitor<IAEItemStack> gridInv = this.getMonitor();
            final IEnergyGrid energy = this.getProxy()
                .getEnergy();
            final ICraftingGrid cg = this.getProxy()
                .getCrafting();
            final FuzzyMode fuzzyMode = supportFuzzy() ? (FuzzyMode) this.getConfigManager()
                .getSetting(Settings.FUZZY_MODE) : null;
            final SchedulingMode schedulingMode = (SchedulingMode) this.getConfigManager()
                .getSetting(Settings.SCHEDULING_MODE);

            if (this.getInstalledUpgrades(Upgrades.ORE_FILTER) == 0) {
                int x;

                for (x = 0; x < this.availableSlots() && this.itemToSend > 0; x++) {
                    final int slotToExport = this.getStartingSlot(schedulingMode, x);

                    final IAEStack<?> raw = this.getAEInventoryByName(StorageName.CONFIG)
                        .getAEStackInSlot(slotToExport);

                    // Fluid slots skip fuzzy (1.7.10 fluids have no fuzzy partitioning) and go through their
                    // own push against the fluid monitor; items keep the inherited path untouched.
                    if (raw instanceof IAEFluidStack afs) {
                        this.exportFluid(energy, slotToExport, afs, cg);
                        continue;
                    }

                    final IAEItemStack aes = (IAEItemStack) raw;

                    if (aes == null || this.itemToSend <= 0 || this.craftOnly()) {
                        if (this.isCraftingEnabled() && this.canInjectStackToTarget(aes)) {
                            this.didSomething = this.exTracker.handleCrafting(
                                slotToExport,
                                this.itemToSend,
                                aes,
                                this.getTile()
                                    .getWorldObj(),
                                this.getProxy()
                                    .getGrid(),
                                cg,
                                this.mySrc) || this.didSomething;
                        }
                        continue;
                    }

                    final long before = this.itemToSend;

                    if (supportFuzzy() && this.getInstalledUpgrades(Upgrades.FUZZY) > 0) {
                        doFuzzy(aes, fuzzyMode, energy, gridInv);
                    } else {
                        this.pushItemIntoTarget(energy, gridInv, aes);
                    }

                    if (this.itemToSend == before && this.isCraftingEnabled() && this.canInjectStackToTarget(aes)) {
                        this.didSomething = this.exTracker.handleCrafting(
                            slotToExport,
                            this.itemToSend,
                            aes,
                            this.getTile()
                                .getWorldObj(),
                            this.getProxy()
                                .getGrid(),
                            cg,
                            this.mySrc) || this.didSomething;
                    }
                }

                this.updateSchedulingMode(schedulingMode, x);
            } else if (supportOreDict()) {
                doOreDict(energy, gridInv);
            }
        } catch (final GridAccessException e) {
            // :P
        }

        if (this.itemToSend < openingBudget) {
            this.didSomething = true;
        }

        // FASTER walks the tracker down two ticks per productive pass instead of pinning it onto the floor in
        // one go: dropping a slow bus straight onto a one-tick cadence makes the whole grid hitch as the work
        // arrives in a single lump, so the rate is allowed to come down a step at a time instead. A bus that
        // moved nothing keeps falling back a tick at a time towards the sixty-tick ceiling.
        return this.didSomething ? TickRateModulation.FASTER : TickRateModulation.SLOWER;
    }

    /**
     * One fluid slot's export pass: push first, and when the push made no progress fall back to crafting, the
     * same order the item path follows. {@code craftOnly} skips the push entirely.
     */
    private void exportFluid(final IEnergyGrid energy, final int slotToExport, final IAEFluidStack afs,
        final ICraftingGrid cg) {
        if (!(this.getTarget() instanceof InventoryAdaptor d)) {
            return;
        }

        if (this.craftOnly()) {
            this.craftFluid(slotToExport, d, afs, cg);
            return;
        }

        final long before = this.itemToSend;
        this.pushFluidIntoTarget(energy, d, afs);

        if (this.itemToSend == before) {
            this.craftFluid(slotToExport, d, afs, cg);
        }
    }

    /**
     * The generic twin of {@code PartBaseExportBus.pushItemIntoTarget}, which is fixed to {@link IAEItemStack}:
     * simulate how much of the slot's fluid the target can take, extract exactly that from the ME network under
     * power, then hand it to the adaptor - whose fluid path does {@code IFluidHandler.fill}. Whatever the target
     * refuses goes back into the network.
     */
    private void pushFluidIntoTarget(final IEnergyGrid energy, final InventoryAdaptor d, IAEFluidStack afs) {
        final IMEMonitor<IAEFluidStack> monitor = this.getFluidMonitor();
        if (monitor == null) {
            return;
        }

        // Fluid works in bucket units - one item unit per bucket - so a base-speed bus moves one bucket per
        // tick and speed cards lift the fluid budget by the same factor they lift the item budget; spending
        // the budget down past zero is what stops the slot loop.
        final long budget = ExBusSlots.fluidTransferBudget(this.itemToSend);
        final IAEFluidStack ins = afs.copy();
        ins.setStackSize(budget);

        final IAEStack<?> o = d.simulateAddStack(ins, InsertionMode.DEFAULT);
        final long canFit = o == null ? budget : budget - o.getStackSize();
        if (canFit <= 0) {
            return;
        }

        afs = afs.copy();
        afs.setStackSize(canFit);
        final IAEFluidStack drained = Platform.poweredExtraction(energy, monitor, afs, this.mySrc);
        if (drained == null) {
            return;
        }

        this.itemToSend -= drained.getStackSize();

        final IAEStack<?> failed = d.addStack(drained, InsertionMode.DEFAULT);
        if (failed != null) {
            afs.setStackSize(failed.getStackSize());
            monitor.injectItems(afs, Actionable.MODULATE, this.mySrc);
        } else {
            this.didSomething = true;
        }
    }

    /**
     * Crafting attempt for one fluid slot. The tracker's {@code IAEStack} overload hands GTNH fluid patterns to
     * {@code beginCraftingJob}, which is generic since rv3; acceptance is simulated against the adaptor because
     * the item-only {@code canInjectStackToTarget} is fixed to {@link IAEItemStack}.
     */
    private void craftFluid(final int slotToExport, final InventoryAdaptor d, final IAEFluidStack afs,
        final ICraftingGrid cg) {
        if (!this.isCraftingEnabled() || afs == null || d.simulateAddStack(afs, InsertionMode.DEFAULT) != null) {
            return;
        }

        try {
            this.didSomething = this.exTracker.handleCrafting(
                slotToExport,
                this.itemToSend,
                afs,
                this.getTile()
                    .getWorldObj(),
                this.getProxy()
                    .getGrid(),
                cg,
                this.mySrc) || this.didSomething;
        } catch (final GridAccessException ignored) {} // same escape hatch the caller's loop uses
    }

    /** The ME network's fluid storage; null when the grid is unreachable, which skips fluid slots this tick. */
    private IMEMonitor<IAEFluidStack> getFluidMonitor() {
        try {
            return this.getProxy()
                .getStorage()
                .getFluidInventory();
        } catch (final GridAccessException ignored) {}
        return null;
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
            GuiType.EX_EXPORT_BUS);
        return true;
    }

    private boolean craftOnly() {
        return this.getConfigManager()
            .getSetting(Settings.CRAFT_ONLY) == YesNo.YES;
    }

    private boolean isCraftingEnabled() {
        return this.getInstalledUpgrades(Upgrades.CRAFTING) > 0;
    }

    private int getStartingSlot(final SchedulingMode schedulingMode, final int x) {
        if (schedulingMode == SchedulingMode.RANDOM) {
            return Platform.getRandom()
                .nextInt(this.availableSlots());
        }

        if (schedulingMode == SchedulingMode.ROUNDROBIN) {
            return (this.nextSlot + x) % this.availableSlots();
        }

        return x;
    }

    private void updateSchedulingMode(final SchedulingMode schedulingMode, final int x) {
        if (schedulingMode == SchedulingMode.ROUNDROBIN) {
            this.nextSlot = (this.nextSlot + x) % this.availableSlots();
        }
    }
}
