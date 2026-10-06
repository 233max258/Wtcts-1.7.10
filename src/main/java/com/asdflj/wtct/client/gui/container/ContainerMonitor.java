package com.asdflj.wtct.client.gui.container;

import static com.asdflj.wtct.api.Constants.MessageType.UPDATE_PLAYER_ITEM;

import java.io.IOException;

import javax.annotation.Nonnull;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S2FPacketSetSlot;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;

import org.apache.commons.lang3.tuple.MutablePair;

import com.asdflj.wtct.Wtct;
import com.asdflj.wtct.api.WtctAPI;
import com.asdflj.wtct.client.gui.container.BaseMonitor.FluidMonitor;
import com.asdflj.wtct.client.gui.container.BaseMonitor.ItemMonitor;
import com.asdflj.wtct.integration.Mods;
import com.asdflj.wtct.inventory.item.INetworkTerminal;
import com.asdflj.wtct.network.SPacketMEItemInvUpdate;
import com.asdflj.wtct.network.SPacketTypeFilter;
import com.asdflj.wtct.util.HBMAeAddonUtil;
import com.glodblock.github.common.item.ItemFluidPacket;
import com.glodblock.github.util.Util;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.config.Settings;
import appeng.api.config.SortDir;
import appeng.api.config.SortOrder;
import appeng.api.config.ViewItems;
import appeng.api.networking.IGridNode;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.ITerminalHost;
import appeng.api.storage.ITerminalTypeFilterProvider;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.api.util.IConfigManager;
import appeng.api.util.IConfigurableObject;
import appeng.container.slot.AppEngSlot;
import appeng.core.AELog;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketValueConfig;
import appeng.helpers.IContainerCraftingPacket;
import appeng.helpers.MonitorableAction;
import appeng.tile.inventory.IAEAppEngInventory;
import appeng.util.ConfigManager;
import appeng.util.IConfigManagerHost;
import appeng.util.InventoryAdaptor;
import appeng.util.Platform;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;

public abstract class ContainerMonitor extends BaseNetworkContainer implements IConfigurableObject, IConfigManagerHost,
    IAEAppEngInventory, IContainerCraftingPacket, ITypeFilterContainer {

    protected final IItemList<IAEItemStack> items = AEApi.instance()
        .storage()
        .createItemList();
    protected final IConfigManager clientCM;
    protected final ItemMonitor monitor;
    protected final FluidMonitor fluidMonitor;
    protected ITerminalHost host;
    protected IConfigManagerHost gui;
    protected IConfigManager serverCM;
    protected IGridNode networkNode;
    private boolean typeFilterSynced = false;

    public ContainerMonitor(InventoryPlayer ip, ITerminalHost monitorable) {
        super(ip, monitorable);
        this.host = monitorable;
        this.clientCM = new ConfigManager(this);
        this.clientCM.registerSetting(Settings.SORT_BY, SortOrder.NAME);
        this.clientCM.registerSetting(Settings.VIEW_MODE, ViewItems.ALL);
        this.clientCM.registerSetting(Settings.SORT_DIRECTION, SortDir.ASCENDING);
        this.monitor = new ItemMonitor(this.crafters);
        this.fluidMonitor = new FluidMonitor(this.crafters);
        if (Platform.isServer()) {
            if (monitorable instanceof INetworkTerminal) {
                this.networkNode = ((INetworkTerminal) monitorable).getGridNode();
            }
            this.serverCM = monitorable.getConfigManager();
            this.setMonitor();
        }
    }

    protected void dropItem(ItemStack is) {
        if (is == null || is.stackSize <= 0) return;
        ItemStack itemStack = is.copy();
        int i = itemStack.getMaxStackSize();
        while (itemStack.stackSize > 0) {
            if (i > itemStack.stackSize) {
                if (!getPlayerInv().addItemStackToInventory(itemStack.copy())) {
                    getPlayerInv().player.entityDropItem(itemStack.copy(), 0);
                }
                break;
            } else {
                itemStack.stackSize -= i;
                ItemStack item = itemStack.copy();
                item.stackSize = i;
                if (!getPlayerInv().addItemStackToInventory(item)) {
                    getPlayerInv().player.entityDropItem(item, 0);
                }
            }
        }
    }

    protected void dropItem(ItemStack itemStack, int stackSize) {
        if (itemStack == null || itemStack.stackSize <= 0) return;
        ItemStack is = itemStack.copy();
        is.stackSize = stackSize;
        this.dropItem(is);
    }

    protected void adjustStack(ItemStack stack) {
        if (stack != null && stack.stackSize > stack.getMaxStackSize()) {
            dropItem(stack, stack.stackSize - stack.getMaxStackSize());
            stack.stackSize = stack.getMaxStackSize();
        }
    }

    abstract void setMonitor();

    public void setGui(@Nonnull final IConfigManagerHost gui) {
        this.gui = gui;
    }

    public IMEMonitor<IAEItemStack> getMonitor() {
        return this.monitor.getMonitor();
    }

    @Override
    public ITerminalTypeFilterProvider getTypeFilterHost() {
        return this.host instanceof ITerminalTypeFilterProvider provider ? provider : null;
    }

    protected boolean isInvalid() {
        return !this.monitor.isValid(null);
    }

    protected void processItemList() {
        this.monitor.processItemList();
    }

    @Override
    public void detectAndSendChanges() {
        if (Platform.isServer()) {
            if (isInvalid()) {
                this.setValidContainer(false);
            }
            if (this.serverCM != null) {
                for (final Settings set : this.serverCM.getSettings()) {
                    final Enum<?> sideLocal = this.serverCM.getSetting(set);
                    final Enum<?> sideRemote = this.clientCM.getSetting(set);

                    if (sideLocal != sideRemote) {
                        this.clientCM.putSetting(set, sideLocal);
                        for (final Object crafter : this.crafters) {
                            try {
                                NetworkHandler.instance.sendTo(
                                    new PacketValueConfig(set.name(), sideLocal.name()),
                                    (EntityPlayerMP) crafter);
                            } catch (final IOException e) {
                                AELog.debug(e);
                            }
                        }
                    }
                }
            }
            processItemList();
            syncTypeFilter();
            super.detectAndSendChanges();
        }
    }

    private void syncTypeFilter() {
        if (this.typeFilterSynced) {
            return;
        }
        final ITerminalTypeFilterProvider provider = this.getTypeFilterHost();
        if (provider == null) {
            this.typeFilterSynced = true;
            return;
        }
        for (final Object crafter : this.crafters) {
            if (crafter instanceof EntityPlayerMP playerMP) {
                Wtct.proxy.netHandler.sendTo(new SPacketTypeFilter(provider.getTypeFilter(playerMP)), playerMP);
            }
        }
        this.typeFilterSynced = true;
    }

    @Override
    public IConfigManager getConfigManager() {
        if (Platform.isServer()) {
            return this.serverCM;
        }
        return this.clientCM;
    }

    protected IConfigManagerHost getGui() {
        return this.gui;
    }

    @Override
    public void updateSetting(IConfigManager manager, Enum settingName, Enum newValue) {
        if (this.getGui() != null) {
            this.getGui()
                .updateSetting(manager, settingName, newValue);
        }
    }

    @Override
    public void addCraftingToCrafters(final ICrafting c) {
        super.addCraftingToCrafters(c);
        this.monitor.queueInventory(c);
        // The fluid channel needs the same kick as the item one: without it the client only learns about
        // fluids from later changes (FluidMonitor sends the difference), so a terminal opened on a network
        // whose fluids never move shows an empty fluid list - water already stored simply is not there.
        this.fluidMonitor.queueInventory(c);
    }

    @Override
    public void removeCraftingFromCrafters(final ICrafting c) {
        super.removeCraftingFromCrafters(c);
        this.monitor.removeCraftingFromCrafters(c);
    }

    @Override
    public void onContainerClosed(final EntityPlayer player) {
        super.onContainerClosed(player);
        if (this.monitor.getMonitor() != null) this.monitor.removeListener();
    }

    private void extractPlayerInventoryItemStack(EntityPlayer player, ItemStack itemStack, int stackSize) {
        for (int x = 0; x < player.inventory.mainInventory.length; x++) {
            ItemStack is = player.inventory.mainInventory[x];
            if (is == null) continue;
            if (Platform.isSameItemPrecise(is, itemStack)) {
                ItemStack tmp = is.copy();
                if (is.stackSize < stackSize) {
                    stackSize = is.stackSize;
                }
                is.stackSize -= stackSize;
                tmp.stackSize = stackSize;
                if (is.stackSize == 0) {
                    player.inventory.setInventorySlotContents(x, null);
                }
                player.inventory.setItemStack(tmp);
                player.inventory.markDirty();
                return;
            }
        }
    }

    private boolean canFillDefaultContainer(IAEFluidStack ifs) {
        if (ifs == null) return false;
        MutablePair<Integer, ItemStack> result = null;
        ItemStack container = WtctAPI.instance()
            .getFluidContainer(ifs);
        if (Util.FluidUtil.isFluidContainer(
            WtctAPI.instance()
                .getFluidContainer(ifs))) {
            result = Util.FluidUtil.fillStack(container, ifs.getFluidStack());
        }
        return result != null && result.left != 0;
    }

    public void postChange(IAEFluidStack fluid, EntityPlayer player, int slotIndex, boolean shift) {
        ItemStack targetStack = getTargetStack(player, slotIndex);
        if (targetStack == null) {
            if (!canFillDefaultContainer(fluid)) return;
            IAEItemStack extractItem = this.monitor.getMonitor()
                .extractItems(
                    AEItemStack.create(
                        WtctAPI.instance()
                            .getFluidContainer(fluid)),
                    Actionable.MODULATE,
                    this.getActionSource());
            if (extractItem != null) {
                player.inventory.setItemStack(extractItem.getItemStack());
            } else {
                this.extractPlayerInventoryItemStack(
                    player,
                    WtctAPI.instance()
                        .getFluidContainer(fluid),
                    1);
            }
            targetStack = getTargetStack(player, slotIndex);
        }

        if (targetStack == null) return;
        // The primary output itemstack
        if (fluid != null
            && ((Mods.HBM_AE_ADDON.isModLoaded() && HBMAeAddonUtil.getItemIsEmptyContainer(targetStack, fluid))
                || Util.FluidUtil.isEmpty(targetStack))) {
            // Situation 1.a: Empty fluid container, and nonnull slot
            extractFluid(fluid, player, slotIndex, shift);
        } else if ((Util.FluidUtil.isFluidContainer(targetStack) && !Util.FluidUtil.isEmpty(targetStack))
            || (Mods.HBM_AE_ADDON.isModLoaded() && HBMAeAddonUtil.getItemHasFluidType(targetStack))) {
                // Situation 2.a: We are holding a non-empty container.
                insertFluid(player, slotIndex, shift);
                // End of situation 2.a
            }
        // No op (Any other situation)

        this.detectAndSendChanges();
    }

    private ItemStack getTargetStack(EntityPlayer player, int slotIndex) {
        if (slotIndex == -1) {
            return player.inventory.getItemStack();
        } else {
            return player.inventory.getStackInSlot(slotIndex);
        }
    }

    /**
     * The insert operation. For input, we have a filled container stack. For outputs, we have the following:
     * <ol>
     * <li>Leftover filled container stack</li>
     * <li>Empty containers</li>
     * <li>Partially filled container x1</li>
     * </ol>
     * In order above, the itemstack at `slotIndex` is transformed into the output.
     */
    private void insertFluid(EntityPlayer player, int slotIndex, boolean shift) {
        ItemStack targetStack = getTargetStack(player, slotIndex);
        final int containersRequestedToInsert = shift ? targetStack.stackSize : 1;

        // Step 1: Determine container characteristics and verify fluid to be extractable
        final int fluidPerContainer;
        final FluidStack fluidStackPerContainer;
        final boolean partialInsertSupported;
        if (targetStack.getItem() instanceof IFluidContainerItem fcItem) {
            ItemStack test = targetStack.copy();
            test.stackSize = 1;
            fluidStackPerContainer = fcItem.drain(test, Integer.MAX_VALUE, false);
            if (fluidStackPerContainer == null || fluidStackPerContainer.amount == 0) {
                return;
            }

            fluidPerContainer = fluidStackPerContainer.amount;
            partialInsertSupported = true;
        } else if (FluidContainerRegistry.isContainer(targetStack)) {
            ItemStack emptyTank = FluidContainerRegistry.drainFluidContainer(targetStack);
            if (emptyTank == null) {
                return;
            }
            fluidStackPerContainer = FluidContainerRegistry.getFluidForFilledItem(targetStack);
            fluidPerContainer = fluidStackPerContainer.amount;
            partialInsertSupported = false;
        } else if (Mods.HBM_AE_ADDON.isModLoaded() && HBMAeAddonUtil.getItemHasFluidType(targetStack)) {
            ItemStack emptyTank = com.hbm.inventory.FluidContainerRegistry.getEmptyContainer(targetStack);
            if (emptyTank == null) {
                return;
            }
            fluidStackPerContainer = HBMAeAddonUtil.getFluidPerContainer(targetStack);
            fluidPerContainer = fluidStackPerContainer.amount;
            partialInsertSupported = false;
        } else {
            return;
        }

        // Step 2: determine network capacity
        final IAEFluidStack totalFluid = AEFluidStack.create(fluidStackPerContainer);
        totalFluid.setStackSize((long) fluidPerContainer * containersRequestedToInsert);

        final IAEFluidStack notInsertable = this.injectFluids(totalFluid, Actionable.SIMULATE);

        final long insertableFluid;
        if (notInsertable == null || notInsertable.getStackSize() == 0) {
            insertableFluid = totalFluid.getStackSize();
        } else {
            long insertable = totalFluid.getStackSize() - notInsertable.getStackSize();
            if (partialInsertSupported) {
                insertableFluid = insertable;
            } else {
                // avoid remainder
                insertableFluid = insertable - (insertable % fluidPerContainer);
            }
        }
        totalFluid.setStackSize(insertableFluid);
        // Nothing the network can take - inserting a zero-size stack would fall through to the item-drop
        // route and NPE inside AE2's NetworkMonitor, so stop here and leave the containers untouched.
        if (insertableFluid <= 0) {
            return;
        }

        // Step 3: perform insert
        final long totalInserted;
        final IAEFluidStack notInserted = this.injectFluids(totalFluid, Actionable.MODULATE);
        if (notInserted != null && notInserted.getStackSize() > 0) {
            // User has a setup that causes discrepancy between simulation and modulation. Likely double storage bus.
            long total = totalFluid.getStackSize() - notInserted.getStackSize();
            if (total == 0) {
                return;
            }
            if (partialInsertSupported) {
                totalInserted = total;
            } else {
                // We cant have partially filled containers -> user will receive a fluid packet as last resort
                long overflowAmount = fluidPerContainer - (total % fluidPerContainer);
                IAEFluidStack overflow = AEFluidStack.create(fluidStackPerContainer);
                overflow.setStackSize(overflowAmount);
                dropItem(ItemFluidPacket.newStack(overflow));
                totalInserted = total + overflowAmount;
            }
        } else {
            totalInserted = totalFluid.getStackSize();
        }

        // Step 4: calculate outputs
        final int emptiedTanks = (int) (totalInserted / fluidPerContainer);
        final int partialDrain = (int) (totalInserted % fluidPerContainer);
        final int partialTanks = partialDrain > 0 && partialInsertSupported ? 1 : 0;
        final int usedTanks = emptiedTanks + partialTanks;
        final int untouchedTanks = targetStack.stackSize - usedTanks;

        ItemStack emptiedTanksStack;
        final ItemStack partialTanksStack;

        if (targetStack.getItem() instanceof IFluidContainerItem fcItem) {
            if (emptiedTanks > 0) {
                emptiedTanksStack = targetStack.copy();
                emptiedTanksStack.stackSize = 1;
                fcItem.drain(emptiedTanksStack, fluidPerContainer, true);
                emptiedTanksStack.stackSize = emptiedTanks;
            } else {
                emptiedTanksStack = null;
            }
            if (partialTanks > 0) {
                partialTanksStack = targetStack.copy();
                partialTanksStack.stackSize = 1;
                fcItem.drain(partialTanksStack, partialDrain, true);
            } else {
                partialTanksStack = null;
            }
        } else if (Mods.HBM_AE_ADDON.isModLoaded() && HBMAeAddonUtil.getItemHasFluidType(targetStack)) {
            if (emptiedTanks > 0) {
                emptiedTanksStack = com.hbm.inventory.FluidContainerRegistry.getEmptyContainer(targetStack);
                emptiedTanksStack.stackSize = emptiedTanks;
            } else {
                emptiedTanksStack = null;
            }
            // Not possible > see Step 2 and Step 3
            partialTanksStack = null;
        } else {
            if (emptiedTanks > 0) {
                emptiedTanksStack = FluidContainerRegistry.drainFluidContainer(targetStack);
                emptiedTanksStack.stackSize = emptiedTanks;
            } else {
                emptiedTanksStack = null;
            }
            // Not possible > see Step 2 and Step 3
            partialTanksStack = null;
        }

        // Done. Put the output in the inventory or ground, and update stack size.
        boolean shouldSendStack = true;
        if (slotIndex == -1) {
            // Item is in mouse hand
            if (untouchedTanks > 0) {
                targetStack.stackSize = untouchedTanks;
                adjustStack(targetStack);
                dropItem(emptiedTanksStack);
                dropItem(partialTanksStack);
            } else if (emptiedTanksStack != null) {
                adjustStack(emptiedTanksStack);
                player.inventory.setItemStack(emptiedTanksStack);
                dropItem(partialTanksStack);
            } else if (partialTanksStack != null) {
                player.inventory.setItemStack(partialTanksStack);
            } else {
                player.inventory.setItemStack(null);
                shouldSendStack = false;
            }
        } else {
            // Shift clicked in
            if (untouchedTanks > 0) {
                targetStack.stackSize = untouchedTanks;
                adjustStack(targetStack);
                dropItem(emptiedTanksStack);
                dropItem(partialTanksStack);
            } else if (emptiedTanksStack != null) {
                adjustStack(emptiedTanksStack);
                player.inventory.setInventorySlotContents(slotIndex, emptiedTanksStack);
                dropItem(partialTanksStack);
            } else if (partialTanksStack != null) {
                player.inventory.setInventorySlotContents(slotIndex, partialTanksStack);
            } else {
                player.inventory.setItemStack(null);
                shouldSendStack = false;
            }
        }
        SPacketMEItemInvUpdate packet = new SPacketMEItemInvUpdate(UPDATE_PLAYER_ITEM);
        if (shouldSendStack) {
            packet.appendItem(
                AEApi.instance()
                    .storage()
                    .createItemStack(player.inventory.getItemStack()));
        }
        Wtct.proxy.netHandler.sendTo(packet, (EntityPlayerMP) player);
    }

    /**
     * The extract operation. For input, we have an empty container stack. For outputs, we have the following:
     * <ol>
     * <li>Leftover empty container stack</li>
     * <li>Filled containers (full)</li>
     * <li>Partially filled container x1</li>
     * </ol>
     * In order above, the itemstack at `slotIndex` is transformed into the output.
     */
    private void extractFluid(IAEFluidStack clientRequestedFluid, EntityPlayer player, int slotIndex, boolean shift) {
        if (slotIndex != -1) {
            // shift-click from inventory cant fill fluids
            return;
        }
        final ItemStack targetStack = player.inventory.getItemStack();
        final int containersRequestedToExtract = shift ? targetStack.stackSize : 1;

        final FluidStack clientRequestedFluidStack = clientRequestedFluid.getFluidStack();
        clientRequestedFluidStack.amount = Integer.MAX_VALUE;

        // Step 1: Determine container characteristics and verify fluid to be insertable
        final int fluidPerContainer;
        final boolean partialInsertSupported;
        if (targetStack.getItem() instanceof IFluidContainerItem fcItem) {
            ItemStack testStack = targetStack.copy();
            testStack.stackSize = 1;
            fluidPerContainer = fcItem.fill(testStack, clientRequestedFluidStack, false);
            if (fluidPerContainer == 0) {
                return;
            }
            partialInsertSupported = true;
        } else if (FluidContainerRegistry.isContainer(targetStack)) {
            fluidPerContainer = FluidContainerRegistry.getContainerCapacity(clientRequestedFluidStack, targetStack);
            partialInsertSupported = false;
        } else if (Mods.HBM_AE_ADDON.isModLoaded()
            && HBMAeAddonUtil.getItemIsEmptyContainer(targetStack, clientRequestedFluid)) {
                fluidPerContainer = HBMAeAddonUtil.getEmptyContainerAmount(targetStack, clientRequestedFluid);
                partialInsertSupported = false;
            } else {
                return;
            }

        // Step 2: determine fluid in network
        final IAEFluidStack totalRequestedFluid = clientRequestedFluid.copy();
        totalRequestedFluid.setStackSize((long) fluidPerContainer * containersRequestedToExtract);

        final IAEFluidStack availableFluid = this.extractFluids(totalRequestedFluid, Actionable.SIMULATE);
        if (availableFluid == null || availableFluid.getStackSize() == 0) {
            return;
        }

        if (availableFluid.getStackSize() != totalRequestedFluid.getStackSize() && !partialInsertSupported) {
            availableFluid.decStackSize(availableFluid.getStackSize() % fluidPerContainer);
        }

        // Step 3: perform extract
        final IAEFluidStack extracted = this.extractFluids(availableFluid, Actionable.MODULATE);
        final long totalExtracted = extracted != null ? extracted.getStackSize() : 0;

        // Step 4: calculate outputs
        final int filledTanks = (int) (totalExtracted / fluidPerContainer);
        final int partialFill = (int) (totalExtracted % fluidPerContainer);
        final int partialTanks = partialFill > 0 && partialInsertSupported ? 1 : 0;
        final int usedTanks = filledTanks + partialTanks;
        final int untouchedTanks = targetStack.stackSize - usedTanks;

        ItemStack filledTanksStack;
        ItemStack partialTanksStack;

        if (targetStack.getItem() instanceof IFluidContainerItem fcItem) {
            if (filledTanks > 0) {
                filledTanksStack = targetStack.copy();
                filledTanksStack.stackSize = 1;
                FluidStack toInsert = extracted.getFluidStack()
                    .copy();
                toInsert.amount = fluidPerContainer;
                fcItem.fill(filledTanksStack, toInsert, true);
                filledTanksStack.stackSize = filledTanks;
            } else {
                filledTanksStack = null;
            }
            if (partialTanks > 0) {
                partialTanksStack = targetStack.copy();
                partialTanksStack.stackSize = 1;
                FluidStack toInsert = extracted.getFluidStack()
                    .copy();
                toInsert.amount = partialFill;
                fcItem.fill(partialTanksStack, toInsert, true);
            } else {
                partialTanksStack = null;
            }
        } else if (Mods.HBM_AE_ADDON.isModLoaded()
            && HBMAeAddonUtil.getItemIsEmptyContainer(targetStack, clientRequestedFluid)) {
                if (filledTanks > 0) {
                    filledTanksStack = targetStack.copy();
                    filledTanksStack.stackSize = 1;
                    FluidStack toInsert = extracted.getFluidStack()
                        .copy();
                    toInsert.amount = fluidPerContainer;
                    filledTanksStack = HBMAeAddonUtil.getFillContainer(targetStack, clientRequestedFluid);
                    filledTanksStack.stackSize = filledTanks;
                } else {
                    filledTanksStack = null;
                }
                if (partialTanks > 0) {
                    partialTanksStack = targetStack.copy();
                    partialTanksStack.stackSize = 1;
                    FluidStack toInsert = extracted.getFluidStack()
                        .copy();
                    toInsert.amount = partialFill;
                    partialTanksStack = HBMAeAddonUtil.getFillContainer(targetStack, clientRequestedFluid);
                } else {
                    partialTanksStack = null;
                }
            } else {
                if (filledTanks > 0) {
                    FluidStack toInsert = extracted.getFluidStack()
                        .copy();
                    toInsert.amount = fluidPerContainer;
                    filledTanksStack = FluidContainerRegistry.fillFluidContainer(toInsert, targetStack);
                    filledTanksStack.stackSize = filledTanks;
                } else {
                    filledTanksStack = null;
                }
                if (partialFill > 0) {
                    // User has a setup that causes discrepancy between simulation and modulation. Likely double storage
                    // bus.
                    // We cant have partially filled containers -> user will receive a fluid packet as last resort
                    IAEFluidStack overflow = extracted.copy();
                    overflow.setStackSize(partialFill);
                    dropItem(ItemFluidPacket.newStack(overflow));
                }
                partialTanksStack = null;
            }

        // Done. Put the output in the inventory or ground, and update stack size.
        // We can assume slotIndex == -1, since we don't actually allow extraction via shift click.
        boolean shouldSendStack = true;
        if (untouchedTanks > 0) {
            ItemStack emptyStack = player.inventory.getItemStack();
            emptyStack.stackSize = untouchedTanks;
            adjustStack(emptyStack);
            dropItem(filledTanksStack);
            dropItem(partialTanksStack);
        } else if (filledTanksStack != null) {
            adjustStack(filledTanksStack);
            player.inventory.setItemStack(filledTanksStack);
            dropItem(partialTanksStack);
        } else if (partialTanksStack != null) {
            player.inventory.setItemStack(partialTanksStack);
        } else {
            player.inventory.setItemStack(null);
            shouldSendStack = false;
        }
        SPacketMEItemInvUpdate packet = new SPacketMEItemInvUpdate(UPDATE_PLAYER_ITEM);
        if (shouldSendStack) {
            packet.appendItem(
                AEApi.instance()
                    .storage()
                    .createItemStack(player.inventory.getItemStack()));
        }
        Wtct.proxy.netHandler.sendTo(packet, (EntityPlayerMP) player);
    }

    protected IAEFluidStack extractFluids(IAEFluidStack ifs, Actionable mode) {
        if (ifs.getStackSize() == 0) return ifs;
        return this.host.getFluidInventory()
            .extractItems(ifs, mode, this.getActionSource());

    }

    protected IAEFluidStack injectFluids(IAEFluidStack ifs, Actionable mode) {
        return this.host.getFluidInventory()
            .injectItems(ifs, mode, this.getActionSource());
    }

    /**
     * Shift-clicking a stack that has nowhere to go inside the container hands it to the ME network -
     * the step AE2's own terminals do through {@code ContainerMEMonitorable#shiftStoreItem}. This
     * hierarchy does not extend {@code ContainerMEMonitorable}, so the step is repeated here;
     * {@link BaseNetworkContainer#storeShiftClickedStack} only reaches it for a player-side source
     * that no container slot accepted.
     *
     * <p>
     * A fluid packet goes to the fluid storage instead, like ae2fc's own terminals: storing it as an
     * item would leave the fluid unreachable to everything that reads the fluid inventory.
     */
    @Override
    protected boolean storeShiftClickedStack(EntityPlayer p, int idx) {
        if (Platform.isClient() || idx < 0 || idx >= this.inventorySlots.size()) {
            return false;
        }
        final Slot slot = this.inventorySlots.get(idx);
        if (!(slot instanceof AppEngSlot eng) || !eng.isPlayerSide() || !slot.getHasStack()) {
            return false;
        }
        final ItemStack stack = slot.getStack();
        if (stack.getItem() instanceof ItemFluidPacket) {
            return this.storeFluidPacket(slot, stack);
        }
        final IMEMonitor<IAEItemStack> monitor = this.getMonitor();
        final IAEItemStack input = monitor == null ? null : AEItemStack.create(stack);
        if (input == null) {
            return false;
        }
        final IAEItemStack rest = this.inject(monitor, this.getPowerSource(), this.getActionSource(), input);
        if (rest == null) {
            slot.putStack(null);
        } else {
            final ItemStack remainder = rest.getItemStack();
            if (remainder == null || rest.getStackSize() <= 0) {
                slot.putStack(null);
            } else {
                remainder.stackSize = (int) Math.min(Integer.MAX_VALUE, rest.getStackSize());
                slot.putStack(remainder);
            }
        }
        this.detectAndSendChanges();
        return true;
    }

    /** {@code true} when the fluid went in; {@code false} leaves the packet in the player's slot. */
    private boolean storeFluidPacket(final Slot slot, final ItemStack stack) {
        final FluidStack fluid = ItemFluidPacket.getFluidStack(stack);
        if (fluid == null || this.host == null || this.host.getFluidInventory() == null) {
            return false;
        }
        if (this.injectFluids(AEFluidStack.create(fluid), Actionable.MODULATE) != null) {
            return false; // did not fit, leave it alone
        }
        slot.putStack(null);
        this.detectAndSendChanges();
        return true;
    }

    /**
     * Handles the interaction requests sent by {@code GuiMonitor} through AE2's {@code PacketMonitorableAction}.
     *
     * AE2's own packet handler drops everything that is not a {@code ContainerMEMonitorable}, which this container
     * hierarchy is not, so the packet is routed here by {@code MixinPacketMonitorableAction} instead. The item
     * logic mirrors AE2's {@code ContainerMEMonitorable#doMonitorableAction}, but runs against this container's
     * own monitor wrapper and power source. Fluid handling keeps using the mod's own packets.
     */
    public void doMonitorableAction(MonitorableAction action, int custom, EntityPlayerMP player) {
        final IMEMonitor<IAEItemStack> monitor = this.getMonitor();
        if (monitor == null) return;

        final BaseActionSource src = this.getActionSource();
        final IEnergySource power = this.getPowerSource();

        IAEItemStack slotItem = null;
        if (this.getTargetStack() instanceof IAEItemStack target) {
            slotItem = monitor.getStorageList()
                .findPrecise(target);
        }
        final ItemStack hand = player.inventory.getItemStack();

        switch (action) {
            case PICKUP_OR_SET_DOWN: {
                if (hand == null) {
                    if (slotItem == null) return;
                    this.extractToHand(monitor, power, src, slotItem, Integer.MAX_VALUE, player);
                } else {
                    this.insertFromHand(monitor, power, src, player, hand.stackSize);
                }
                break;
            }
            case SPLIT_OR_PLACE_SINGLE: {
                if (hand == null) {
                    if (slotItem == null) return;
                    this.extractToHand(
                        monitor,
                        power,
                        src,
                        slotItem,
                        (int) ((slotItem.getStackSize() + 1) >> 1),
                        player);
                } else {
                    this.insertFromHand(monitor, power, src, player, 1);
                }
                break;
            }
            case PICKUP_SINGLE:
            case ROLL_UP: {
                if (slotItem == null) return;
                if (hand != null && (hand.stackSize >= hand.getMaxStackSize()
                    || !Platform.isSameItemPrecise(hand, slotItem.getItemStack()))) return;
                this.extractToHand(monitor, power, src, slotItem, 1, player);
                break;
            }
            case ROLL_DOWN: {
                if (hand != null) {
                    this.insertFromHand(monitor, power, src, player, 1);
                }
                break;
            }
            case SHIFT_CLICK:
            case MOVE_REGION: {
                if (slotItem == null) return;
                this.extractToInventory(monitor, power, src, slotItem, action == MonitorableAction.SHIFT_CLICK, player);
                break;
            }
            case CREATIVE_DUPLICATE: {
                if (player.capabilities.isCreativeMode && slotItem != null) {
                    final ItemStack is = slotItem.getItemStack()
                        .copy();
                    is.stackSize = is.getMaxStackSize();
                    player.inventory.setItemStack(is);
                    player.updateHeldItem();
                }
                break;
            }
            default:
                // Fluid container actions (DRAIN_*/FILL_*) are handled by this mod's own fluid packets.
        }
    }

    private IAEItemStack extract(IMEMonitor<IAEItemStack> monitor, IEnergySource power, BaseActionSource src,
        IAEItemStack request) {
        if (power != null) {
            return Platform.poweredExtraction(power, monitor, request, src);
        }
        return monitor.extractItems(request, Actionable.MODULATE, src);
    }

    private IAEItemStack inject(IMEMonitor<IAEItemStack> monitor, IEnergySource power, BaseActionSource src,
        IAEItemStack input) {
        if (power != null) {
            return Platform.poweredInsert(power, monitor, input, src);
        }
        return monitor.injectItems(input, Actionable.MODULATE, src);
    }

    private void extractToHand(IMEMonitor<IAEItemStack> monitor, IEnergySource power, BaseActionSource src,
        IAEItemStack slotItem, int max, EntityPlayerMP player) {
        if (player.inventory.getItemStack() != null) return;
        final IAEItemStack request = slotItem.copy();
        request.setStackSize(
            Math.min(
                max,
                slotItem.getItemStack()
                    .getMaxStackSize()));
        final IAEItemStack got = this.extract(monitor, power, src, request);
        if (got != null) {
            player.inventory.setItemStack(got.getItemStack());
            player.updateHeldItem();
        }
    }

    private void insertFromHand(IMEMonitor<IAEItemStack> monitor, IEnergySource power, BaseActionSource src,
        EntityPlayerMP player, int count) {
        final ItemStack hand = player.inventory.getItemStack();
        if (hand == null || count <= 0) return;
        IAEItemStack insert = AEItemStack.create(hand);
        if (insert == null) return;
        insert = insert.copy();
        insert.setStackSize(count);
        final IAEItemStack rest = this.inject(monitor, power, src, insert);
        final int inserted = rest == null ? count : count - (int) rest.getStackSize();
        if (inserted <= 0) return;
        hand.stackSize -= inserted;
        if (hand.stackSize <= 0) {
            player.inventory.setItemStack(null);
        }
        player.updateHeldItem();
    }

    private void extractToInventory(IMEMonitor<IAEItemStack> monitor, IEnergySource power, BaseActionSource src,
        IAEItemStack slotItem, boolean singleStack, EntityPlayerMP player) {
        final InventoryAdaptor adaptor = InventoryAdaptor.getAdaptor(player, ForgeDirection.UNKNOWN);
        if (adaptor == null) return;
        final long maxSize = slotItem.getItemStack()
            .getMaxStackSize();
        while (true) {
            final IAEItemStack request = slotItem.copy();
            request.setStackSize(maxSize);
            final IAEItemStack available = monitor.extractItems(request, Actionable.SIMULATE, src);
            if (available == null || available.getStackSize() <= 0) break;
            final ItemStack simulated = adaptor.simulateAdd(available.getItemStack());
            if (simulated != null) {
                if (available.getStackSize() <= simulated.stackSize) break;
                request.setStackSize(available.getStackSize() - simulated.stackSize);
            }
            final IAEItemStack got = this.extract(monitor, power, src, request);
            if (got == null || got.getStackSize() <= 0) break;
            final ItemStack fail = adaptor.addItems(got.getItemStack());
            if (fail != null) {
                this.inject(monitor, power, src, AEItemStack.create(fail));
                break;
            }
            if (singleStack) break;
        }
        this.syncPlayerInventory(player);
    }

    /**
     * This container has no player slots, so vanilla never syncs the player inventory after we modify it server
     * side; push the changed main inventory rows through the player's inventory container (window 0) manually.
     */
    private void syncPlayerInventory(EntityPlayerMP player) {
        final InventoryPlayer inv = player.inventory;
        for (int i = 0; i < inv.mainInventory.length; i++) {
            // inventoryContainer layout: hotbar = slots 36..44, main inventory = slots 9..35
            final int containerSlot = i < 9 ? 36 + i : i;
            player.playerNetServerHandler.sendPacket(
                new S2FPacketSetSlot(player.inventoryContainer.windowId, containerSlot, inv.mainInventory[i]));
        }
    }
}
