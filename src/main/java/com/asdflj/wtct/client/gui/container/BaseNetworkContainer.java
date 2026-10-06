package com.asdflj.wtct.client.gui.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import com.asdflj.wtct.client.gui.container.widget.IWidgetPatternContainer;
import com.asdflj.wtct.inventory.item.INetworkTerminal;
import com.asdflj.wtct.inventory.item.WirelessTerminal;
import com.asdflj.wtct.util.BaublesUtil;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.networking.IGridNode;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.storage.ITerminalHost;
import appeng.container.AEBaseContainer;
import appeng.container.guisync.GuiSync;
import appeng.container.interfaces.IInventorySlotAware;
import appeng.items.misc.ItemEncodedPattern;
import appeng.me.helpers.ChannelPowerSrc;
import appeng.util.Platform;

public class BaseNetworkContainer extends AEBaseContainer {

    protected final EntityPlayer player;
    protected WirelessTerminal terminal;
    protected int ticks;
    protected final double powerMultiplier = 0.5;

    @GuiSync(1)
    public boolean hasPower = false;

    /**
     * The container slot the opened terminal item sits in, or {@code -1} for containers that are not opened on a
     * portable terminal.
     *
     * <p>
     * A terminal's own screen is bound to the item it was opened from, so that item must stay where it is while the
     * screen is up: AE2 locks the slot itself, but only with the raw index it is handed, and this mod opens terminals
     * through a bridge-encoded coordinate ({@code GuiBridgeInvType}) that never matches a slot. The slot is resolved
     * here - on demand, because the subclasses are the ones that bind the player inventory - so the container can
     * refuse to move it either way.
     */
    private int findTerminalSlot() {
        if (!(this.getTarget() instanceof IInventorySlotAware slotAware)) {
            return -1;
        }
        final int inventorySlot = BaublesUtil.decodedPlayerSlot(slotAware.getInventorySlot());
        if (inventorySlot < 0) {
            return -1;
        }
        for (int i = 0; i < this.inventorySlots.size(); i++) {
            final Slot slot = this.inventorySlots.get(i);
            if (slot.inventory == this.getPlayerInv() && slot.getSlotIndex() == inventorySlot) {
                return i;
            }
        }
        return -1;
    }

    public BaseNetworkContainer(InventoryPlayer ip, ITerminalHost host) {
        super(ip, host);
        this.player = ip.player;
        if (Platform.isClient()) return;
        if (host instanceof WirelessTerminal) {
            this.terminal = (WirelessTerminal) host;
            this.setPowerSource(this.terminal);
        } else if (this instanceof INetworkTerminal it) {
            this.setPowerSource(
                new ChannelPowerSrc(
                    it.getGridNode(),
                    it.getGrid()
                        .getCache(IEnergyGrid.class)));
        }
    }

    /** True when {@code slotIndex} is the slot holding the terminal this container was opened from. */
    protected boolean isTerminalSlot(final int slotIndex) {
        final int terminalSlot = this.findTerminalSlot();
        return terminalSlot >= 0 && slotIndex == terminalSlot;
    }

    @Override
    public ItemStack slotClick(final int slotId, final int clickedButton, final int mode, final EntityPlayer player) {
        // Whether the click would pick the terminal up, put something onto it or drag across it: none of it may
        // happen while the terminal's own screen is open.
        if (this.isTerminalSlot(slotId)) {
            return null;
        }
        return super.slotClick(slotId, clickedButton, mode, player);
    }

    private boolean transferPatternToSlot(EntityPlayer p, int idx, IPatternContainer container) {
        Slot clickSlot = this.inventorySlots.get(idx);
        ItemStack is = clickSlot.getStack();
        if (is != null && !container.getPatternOutputSlot()
            .getHasStack() && is.stackSize == 1 && is.getItem() instanceof ItemEncodedPattern) {
            // Take the pattern out through the clicked slot so it leaves whichever inventory it is
            // in. Clearing p.inventory by slot index only holds for the player's own slots - for a
            // terminal slot (cache, crafting grid, ...) it would wipe an unrelated player item and
            // leave the pattern where it was, i.e. duplicate it into the edit slot.
            ItemStack output = clickSlot.decrStackSize(1);
            if (output == null) {
                return false; // a ghost slot has nothing to hand over
            }
            container.getPatternOutputSlot()
                .putStack(output);
            return true;
        }
        return false;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer p, int idx) {
        if (Platform.isClient()) {
            return null;
        }
        // Shift-clicking the terminal must not carry it out of its slot either.
        if (this.isTerminalSlot(idx)) {
            return null;
        }
        boolean didSomething = false;
        if (this instanceof IPatternContainer patternContainer) {
            didSomething = transferPatternToSlot(p, idx, patternContainer);
        } else if (this instanceof IWidgetPatternContainer w) {
            didSomething = transferPatternToSlot(p, idx, w.getContainer());
        }
        if (didSomething) {
            return null;
        }
        final ItemStack leftover = super.transferStackInSlot(p, idx);
        if (leftover == null && this.storeShiftClickedStack(p, idx)) {
            return null;
        }
        return leftover;
    }

    /**
     * Hook for "none of the container's own slots wanted this stack". A terminal container overrides
     * it to hand the stack to the ME network, which is the last step of AE2's own
     * {@code ContainerMEMonitorable#transferStackInSlot}; without it a shift-clicked stack would just
     * stay where it was.
     */
    protected boolean storeShiftClickedStack(EntityPlayer p, int idx) {
        return false;
    }

    public BaseNetworkContainer(InventoryPlayer ip, Object anchor) {
        super(ip, anchor);
        this.player = ip.player;
    }

    protected void updatePowerStatus() {
        try {
            if (this.getNetworkNode() != null) {
                this.setPowered(
                    this.getNetworkNode()
                        .isActive());
            } else if (this.getPowerSource() instanceof IEnergyGrid) {
                this.setPowered(((IEnergyGrid) this.getPowerSource()).isNetworkPowered());
            } else {
                this.setPowered(
                    this.getPowerSource()
                        .extractAEPower(1, Actionable.SIMULATE, PowerMultiplier.CONFIG) > 0.8);
            }
        } catch (final Throwable ignore) {}
    }

    private IGridNode getNetworkNode() {
        if (this.terminal == null) return null;
        return this.terminal.getGridNode();
    }

    @Override
    public void addCraftingToCrafters(ICrafting crafting) {
        updatePowerStatus();
        super.addCraftingToCrafters(crafting);
    }

    protected void setPowered(final boolean isPowered) {
        this.hasPower = isPowered;
    }

    @Override
    public void detectAndSendChanges() {
        if (Platform.isServer()) {
            if (this.terminal != null && this.hasPower) {
                ticks = this.terminal.getWirelessObject()
                    .extractPower(getPowerMultiplier() * ticks, Actionable.MODULATE, PowerMultiplier.CONFIG, ticks);
            }
        }
        super.detectAndSendChanges();
    }

    public double getPowerMultiplier() {
        return this.powerMultiplier;
    }
}
