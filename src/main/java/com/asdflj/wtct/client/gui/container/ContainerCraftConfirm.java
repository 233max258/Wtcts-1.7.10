package com.asdflj.wtct.client.gui.container;

import java.util.List;
import java.util.Objects;

import net.minecraft.entity.player.InventoryPlayer;

import com.asdflj.wtct.common.parts.THPart;
import com.asdflj.wtct.inventory.InventoryHandler;
import com.asdflj.wtct.inventory.gui.GuiType;
import com.asdflj.wtct.inventory.item.WirelessDualInterfaceTerminalInventory;
import com.asdflj.wtct.inventory.item.WirelessTerminal;
import com.asdflj.wtct.util.BlockPos;

import appeng.api.networking.security.IActionHost;
import appeng.api.storage.ITerminalHost;

public class ContainerCraftConfirm extends appeng.container.implementations.ContainerCraftConfirm {

    /**
     * The other crafts the Ctrl+hammer asked for. A confirm runs exactly one job, so the terminal hands
     * the rest over with the screen and they start together with the one on display.
     */
    private final List<ContainerComprehensiveWorkTerminal.PendingCraft> followUps = new java.util.ArrayList<>();

    public ContainerCraftConfirm(final InventoryPlayer ip, final ITerminalHost te) {
        super(ip, te);
    }

    public void setFollowUps(final List<ContainerComprehensiveWorkTerminal.PendingCraft> jobs) {
        this.followUps.clear();
        if (jobs != null) {
            this.followUps.addAll(jobs);
        }
    }

    /**
     * Every job this terminal starts is asked to "follow", whatever AE2 passed in.
     *
     * <p>
     * AE2 hands the completion notice only to the players on the CPU's following list, and the plain start
     * - a manual click on Start, or this screen's Shift auto-start, both of which go through the no-argument
     * {@code startJob()} and pass {@code false} - therefore finishes in silence; only AE2's Ctrl auto-start
     * announces itself. A player sitting on this screen has just watched the plan being worked out and is
     * exactly who wants to hear that the network finished it, so the flag is ignored here and the job always
     * follows. The no-argument entry point is deliberately left to AE2, because this mod pins the crafted
     * item from an injection into it.
     */
    @Override
    public void startJob(final boolean followCraft) {
        applyFollowUps();
        super.startJob(true);
    }

    /**
     * Hands the follow-ups to the terminal this screen is about to switch back to - only now that the
     * player approved the plan do they get submitted, and that terminal is the one that does it.
     */
    private void applyFollowUps() {
        if (this.followUps.isEmpty() || appeng.util.Platform.isClient()) {
            return;
        }
        final List<ContainerComprehensiveWorkTerminal.PendingCraft> jobs = new java.util.ArrayList<>(this.followUps);
        this.followUps.clear();
        ContainerComprehensiveWorkTerminal.approveFollowUps(this.getPlayerInv().player, jobs);
    }

    @Override
    public void switchToOriginalGUI() {
        GuiType originalGui = null;

        final IActionHost ah = this.getActionHost();
        if (ah instanceof WirelessDualInterfaceTerminalInventory) {
            // That inventory is the comprehensive work terminal's own host.
            originalGui = GuiType.COMPREHENSIVE_WORK_TERMINAL;
        }

        if (this.getOpenContext() != null && ah instanceof THPart) {
            InventoryHandler.openGui(
                this.getInventoryPlayer().player,
                getWorld(),
                new BlockPos(
                    this.getOpenContext()
                        .getTile()),
                Objects.requireNonNull(
                    this.getOpenContext()
                        .getSide()),
                originalGui);
        } else if (ah instanceof WirelessTerminal) {
            // getInventorySlot() is bridge-encoded (main inventory *or* a Baubles slot), so the coordinate
            // must go through the bridge factory instead of the plain item factory.
            InventoryHandler.openGui(
                this.getInventoryPlayer().player,
                getWorld(),
                new BlockPos(((WirelessTerminal) ah).getInventorySlot(), 0, 0),
                Objects.requireNonNull(
                    this.getOpenContext()
                        .getSide()),
                originalGui);
        }
    }
}
