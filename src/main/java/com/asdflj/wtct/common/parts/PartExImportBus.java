package com.asdflj.wtct.common.parts;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Vec3;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

import com.asdflj.wtct.inventory.InventoryHandler;
import com.asdflj.wtct.inventory.gui.GuiType;
import com.asdflj.wtct.util.BlockPos;

import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.config.PowerMultiplier;
import appeng.api.config.Settings;
import appeng.api.config.Upgrades;
import appeng.api.networking.IGridNode;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.StorageName;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IAEStackType;
import appeng.helpers.Reflected;
import appeng.me.GridAccessException;
import appeng.me.storage.MEMonitorIFluidHandler;
import appeng.parts.automation.PartImportBus;
import appeng.tile.inventory.IAEStackInventory;
import appeng.util.InventoryAdaptor;
import appeng.util.Platform;
import appeng.util.item.AEFluidStack;

/**
 * The ME extended import bus: AE2's import bus with a filter grid that grows to 63 slots and a configurable speed
 * multiplier.
 *
 * <p>
 * The item import is inherited verbatim, but the ticking loop itself has to be carried over: the parent's
 * {@code doBusWork} casts every config slot straight to {@code IAEItemStack}, which would throw on a fluid slot.
 * The carried-over loop routes item slots to the inherited {@code importStuff} and fluid slots to a drain path
 * built on AE2's own tank adapter - the same machinery a fluid storage bus uses.
 */
public class PartExImportBus extends PartImportBus implements ExBusScreenHost {

    /** Own NBT key: the parent writes its own nine-slot inventory under {@code "config"}. */
    private static final String CONFIG_TAG = "config_ex";

    private final IAEStackInventory exConfig = new IAEStackInventory(this, ExBusSlots.MAX_SLOTS, StorageName.CONFIG);

    @Reflected
    public PartExImportBus(final ItemStack is) {
        super(is);
    }

    /**
     * Widen the stock bus window instead of ticking flat out. Inheritance would give it {@code TickRates.ImportBus} -
     * a floor of five ticks between operations, stretching to forty while the source has nothing to give - so a stock
     * import bus drains in pulses and a quiet one can sit for seconds before it notices that something arrived. Here
     * the window is one tick at the fastest and sixty at the slowest, and {@link #doBusWork} reports {@code FASTER} on
     * any pass that drained something: the tracker then walks the rate down two ticks at a time, so a bus with a
     * backlog eases onto a one-tick cadence rather than snapping straight onto it, while one with nothing to drain
     * falls back a tick at a time until it is looking once every sixty ticks. The speed multiplier keeps scaling how
     * much each pass moves rather than how often the bus looks. Sleep still follows the parent's rule (a bus with no
     * target sleeps, and a block update wakes it).
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
     * The parent already narrows the flags to extracts only; this grid carries fluids too, so the target lookup
     * may resolve to an {@code AdaptorFluidHandler} instead of stopping at the item adapters.
     */
    @Override
    protected int getAdaptorFlags() {
        return InventoryAdaptor.ALLOW_ITEMS | InventoryAdaptor.ALLOW_FLUIDS | InventoryAdaptor.FOR_EXTRACTS;
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
    }

    @Override
    public void writeToNBT(final NBTTagCompound extra) {
        super.writeToNBT(extra);
        this.exConfig.writeToNBT(extra, CONFIG_TAG);
    }

    @Override
    public int calculateAmountToSend() {
        return ExBusSlots.scaleSpeed(super.calculateAmountToSend());
    }

    /**
     * Carried over from {@code PartBaseImportBus.doBusWork} with one addition: a config slot holding a fluid goes
     * to the tank drain path instead of the item path. The power budget, the ore-dict branch and the unconfigured
     * "import anything" fallback keep the parent's structure; the fallback now also pulls any fluid the tank
     * offers, mirroring the item semantics.
     */
    @Override
    protected TickRateModulation doBusWork() {
        if (!this.getProxy()
            .isActive() || !this.canDoBusWork()) {
            return TickRateModulation.IDLE;
        }

        this.worked = false;

        final Object myTarget = this.getTarget();
        final FuzzyMode fzMode = (FuzzyMode) this.getConfigManager()
            .getSetting(Settings.FUZZY_MODE);

        if (myTarget == null) {
            return TickRateModulation.SLEEP;
        }

        try {
            this.itemToSend = this.calculateAmountToSend();
            final int powerMultiplier = this.getPowerMultiplier();
            final double availablePower = this.getProxy()
                .getEnergy()
                .extractAEPower(
                    Platform.ceilDiv(this.itemToSend, powerMultiplier),
                    Actionable.SIMULATE,
                    PowerMultiplier.CONFIG);
            this.itemToSend = Math.min(this.itemToSend, (int) (availablePower * powerMultiplier + 0.01));

            final IMEMonitor<IAEItemStack> inv = this.getMonitor();
            final IEnergyGrid energy = this.getProxy()
                .getEnergy();

            boolean configured = false;
            if (this.getInstalledUpgrades(Upgrades.ORE_FILTER) == 0) {
                for (int x = 0; x < this.availableSlots(); x++) {
                    final IAEStack<?> raw = this.getAEInventoryByName(StorageName.CONFIG)
                        .getAEStackInSlot(x);
                    if (raw != null && this.itemToSend > 0) {
                        configured = true;
                        if (raw instanceof IAEFluidStack afs) {
                            // Fuzzy has no meaning for 1.7.10 fluids - the drain match stays exact.
                            while (this.itemToSend > 0) {
                                if (this.importFluid(energy, afs)) {
                                    break;
                                }
                            }
                        } else {
                            while (this.itemToSend > 0) {
                                if (this.importStuff(myTarget, (IAEItemStack) raw, inv, energy, fzMode)) {
                                    break;
                                }
                            }
                        }
                    }
                }
            } else if (supportOreDict()) {
                configured = this.doOreDict(myTarget, inv, energy, fzMode);
            }

            if (!configured) {
                while (this.itemToSend > 0) {
                    final boolean itemsDone = this.importStuff(myTarget, null, inv, energy, fzMode);
                    final boolean fluidsDone = this.importAnyFluid(energy);
                    if (itemsDone && fluidsDone) {
                        break;
                    }
                }
            }
        } catch (final GridAccessException e) {
            // :3
        }

        // FASTER walks the tracker down two ticks per productive pass instead of pinning it onto the floor in
        // one go: dropping a slow bus straight onto a one-tick cadence makes the whole grid hitch as the work
        // arrives in a single lump, so the rate is allowed to come down a step at a time instead. A bus that
        // drained nothing keeps falling back a tick at a time towards the sixty-tick ceiling.
        return this.worked ? TickRateModulation.FASTER : TickRateModulation.SLOWER;
    }

    /**
     * One drain pass on a fluid slot. The tank is wrapped in AE2's {@link MEMonitorIFluidHandler} - the same
     * adapter a fluid storage bus uses - so the drain goes through {@code poweredExtraction} and the network
     * insert through {@code poweredInsert}; whatever the network refuses goes back into the tank it just came
     * from.
     *
     * @return true when this slot is done for this tick (nothing to drain, network full, or budget spent)
     */
    private boolean importFluid(final IEnergySource energy, final IAEFluidStack afs) {
        final TileEntity target = this.getTargetTile();
        if (!(target instanceof IFluidHandler tank)) {
            return true;
        }

        final IMEMonitor<IAEFluidStack> fluidInv = this.getFluidMonitor();
        if (fluidInv == null) {
            return true;
        }

        final var side = this.getSide()
            .getOpposite();
        final IAEFluidStack request = afs.copy();
        // Fluid works in bucket units - one item unit per bucket - so a base-speed bus drains one bucket per
        // tick and speed cards lift the fluid budget by the same factor they lift the item budget; the drain
        // spending the budget down past zero is what ends the slot's loop.
        request.setStackSize(Math.min(afs.getStackSize(), ExBusSlots.fluidTransferBudget(this.itemToSend)));

        final IAEFluidStack drained = Platform
            .poweredExtraction(energy, new MEMonitorIFluidHandler(tank, side), request, this.mySrc);
        if (drained == null) {
            return true;
        }

        final IAEFluidStack failed = Platform.poweredInsert(energy, fluidInv, drained, this.mySrc);
        if (failed != null) {
            tank.fill(side, failed.getFluidStack(), true);
            return true;
        }

        this.itemToSend -= (int) drained.getStackSize();
        this.worked = true;
        return this.itemToSend <= 0 || drained.getStackSize() < request.getStackSize();
    }

    /**
     * The unconfigured fallback: pull the first fluid the tank holds, in the spirit of the item bus importing
     * everything when no slot is filled in.
     *
     * @return true when this pass is done (no tank, dry tank, or the drain hit a stop)
     */
    private boolean importAnyFluid(final IEnergySource energy) {
        final TileEntity target = this.getTargetTile();
        if (!(target instanceof IFluidHandler tank)) {
            return true;
        }

        final FluidTankInfo[] infos = tank.getTankInfo(
            this.getSide()
                .getOpposite());
        if (infos == null) {
            return true;
        }

        for (final FluidTankInfo info : infos) {
            if (info.fluid != null && info.fluid.amount > 0) {
                return this.importFluid(energy, AEFluidStack.create(info.fluid));
            }
        }
        return true;
    }

    /** The tile the bus faces, the same lookup {@code getTarget} performs before wrapping it in an adaptor. */
    private TileEntity getTargetTile() {
        final TileEntity self = this.getHost()
            .getTile();
        return this.getTileEntity(
            self,
            self.xCoord + this.getSide().offsetX,
            self.yCoord + this.getSide().offsetY,
            self.zCoord + this.getSide().offsetZ);
    }

    /** The ME network's fluid storage; null when the grid is unreachable, which stops fluid imports this tick. */
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
            GuiType.EX_IMPORT_BUS);
        return true;
    }
}
