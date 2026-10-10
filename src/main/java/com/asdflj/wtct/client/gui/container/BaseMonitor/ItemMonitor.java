package com.asdflj.wtct.client.gui.container.BaseMonitor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.ICrafting;

import com.asdflj.wtct.Wtct;
import com.asdflj.wtct.common.storage.RefreshableStorageMonitor;
import com.asdflj.wtct.network.SPacketMEItemInvUpdate;
import com.glodblock.github.common.item.ItemFluidDrop;

import appeng.api.AEApi;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.storage.IBaseMonitor;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.IMEMonitorHandlerReceiver;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.util.Platform;

public class ItemMonitor implements IMEMonitorHandlerReceiver<IAEItemStack>, IProcessItemList {

    private IMEMonitor<IAEItemStack> itemMonitor;
    private final IItemList<IAEItemStack> items = AEApi.instance()
        .storage()
        .createItemList();
    private final List<ICrafting> crafters;
    /**
     * The craftable entries this terminal has already told the client about. AE2 only ever announces the
     * craftable list, never what drops out of it, so this is what lets the removal be sent later on.
     */
    private final IItemList<IAEItemStack> announcedCraftable = AEApi.instance()
        .storage()
        .createItemList();
    private FluidMonitor fluidMonitorObject = null;
    /**
     * "Can the network craft this, right now?" - asked of the crafting grid cache itself rather than of the
     * storage list, whose craftable flags are what this class exists to distrust. Retracting is only done
     * when both say no, so a terminal can never lose an entry the network still has a pattern for.
     */
    private java.util.function.Predicate<IAEItemStack> craftableCheck = null;

    public ItemMonitor(List<ICrafting> crafters) {
        this.crafters = crafters;
    }

    public void setMonitor(IMEMonitor<IAEItemStack> itemMonitor) {
        this.itemMonitor = itemMonitor;
    }

    public void setFluidMonitorObject(FluidMonitor objectMonitor) {
        this.fluidMonitorObject = objectMonitor;
    }

    /** Hands this monitor the network's live craftable lookup - see {@link #craftableCheck}. */
    public void setCraftableCheck(java.util.function.Predicate<IAEItemStack> check) {
        this.craftableCheck = check;
    }

    @Override
    public void addListener() {
        this.itemMonitor.addListener(this, null);
    }

    @Override
    public boolean isValid(Object verificationToken) {
        return this.itemMonitor != null;
    }

    @Override
    public void postChange(IBaseMonitor<IAEItemStack> monitor, Iterable<IAEItemStack> change,
        BaseActionSource actionSource) {
        for (final IAEItemStack is : change) {
            this.items.add(is);
        }
    }

    @Override
    public void onListUpdate() {
        for (final Object c : this.crafters) {
            if (c instanceof final ICrafting cr) {
                this.queueInventory(cr);
            }
        }
    }

    private void fluidHandler(IAEItemStack send) {
        if (this.fluidMonitorObject != null && send.getStackSize() == 0 && send.getItem() instanceof ItemFluidDrop) {
            this.fluidMonitorObject.addItemCraftingFluid(send);
        }
    }

    @Override
    public void processItemList() {
        if (this.itemMonitor == null) {
            return;
        }
        IItemList<IAEItemStack> monitorCache = null;
        if (this.itemMonitor instanceof RefreshableStorageMonitor refreshable) {
            monitorCache = refreshable.refreshExternalChanges(null, false);
        }
        if (monitorCache == null) {
            monitorCache = this.itemMonitor.getStorageList();
        }
        if (!this.items.isEmpty()) {
            List<IAEItemStack> toSend = new ArrayList<>();
            for (final IAEItemStack is : this.items) {
                IAEItemStack send = monitorCache.findPrecise(is);
                if (send != null) {
                    rememberCraftable(send);
                    fluidHandler(send.copy());
                    toSend.add(send);
                } else {
                    // Not in the storage list at all: nothing is stored and nothing can craft it, so the
                    // absolute state is what the client is told - the diff this entry came in with says
                    // nothing about how much is left.
                    is.setStackSize(0);
                    is.setCraftable(false);
                    toSend.add(is);
                }
            }
            sendToCrafters(toSend);
            this.items.resetStatus();
        }
        retractLostCraftables(monitorCache);
    }

    /** Notes a craftable entry as announced, so its removal can be sent once the pattern is gone. */
    private void rememberCraftable(final IAEItemStack is) {
        if (is == null || !is.isCraftable() || this.announcedCraftable.findPrecise(is) != null) {
            return;
        }
        final IAEItemStack marker = is.copy();
        marker.reset();
        marker.setCraftable(true);
        this.announcedCraftable.add(marker);
    }

    /**
     * Takes back the craftable entries the network no longer has a pattern for. Pulling a pattern out of a
     * provider only ever makes AE2 post the new craftable list - the entries that dropped out are never
     * mentioned again - so without this the client keeps the item for ever, with no stock and no way to
     * craft it. An entry the client holds without a count and without the craftable flag is dropped from
     * the terminal's list; one that is still stored survives with its real count, minus the marker.
     */
    private void retractLostCraftables(final IItemList<IAEItemStack> monitorCache) {
        if (this.craftableCheck == null || this.announcedCraftable.isEmpty()) {
            return;
        }
        final List<IAEItemStack> toSend = new ArrayList<>();
        for (final Iterator<IAEItemStack> it = this.announcedCraftable.iterator(); it.hasNext();) {
            final IAEItemStack announced = it.next();
            final IAEItemStack stored = monitorCache.findPrecise(announced);
            if ((stored != null && stored.isCraftable()) || this.craftableCheck.test(announced)) {
                continue;
            }
            it.remove();
            final IAEItemStack gone = announced.copy();
            gone.setCraftable(false);
            gone.setStackSize(stored == null ? 0 : stored.getStackSize());
            toSend.add(gone);
        }
        sendToCrafters(toSend);
    }

    private void sendToCrafters(final List<IAEItemStack> toSend) {
        if (toSend.isEmpty()) {
            return;
        }
        SPacketMEItemInvUpdate piu = new SPacketMEItemInvUpdate();
        piu.addAll(toSend);
        for (final Object c : this.crafters) {
            if (c instanceof EntityPlayer) {
                Wtct.proxy.netHandler.sendTo(piu, (EntityPlayerMP) c);
            }
        }
    }

    @Override
    public void queueInventory(ICrafting c) {
        if (Platform.isServer() && c instanceof EntityPlayer && this.itemMonitor != null) {
            // TEMP DIAGNOSTIC (1.0.35, remove once the terminal-open delay is pinned down).
            final long t0 = System.currentTimeMillis();
            final IItemList<IAEItemStack> monitorCache = this.itemMonitor instanceof RefreshableStorageMonitor refreshable
                ? refreshable.refreshExternalChanges(null, true)
                : this.itemMonitor.getStorageList();
            List<IAEItemStack> toSend = new ArrayList<>();
            for (final IAEItemStack is : monitorCache) {
                rememberCraftable(is);
                fluidHandler(is.copy());
                toSend.add(is);
            }
            final long t1 = System.currentTimeMillis();
            SPacketMEItemInvUpdate piu = new SPacketMEItemInvUpdate();
            piu.addAll(toSend);
            Wtct.proxy.netHandler.sendTo(piu, (EntityPlayerMP) c);
            cpw.mods.fml.common.FMLLog.info(
                "[wtct-diag] server queueInventory items=%d build=%dms t=%d thread=%s",
                toSend.size(),
                t1 - t0,
                t0,
                Thread.currentThread()
                    .getName());
        }
    }

    @Override
    public void removeCraftingFromCrafters(ICrafting c) {
        if (this.crafters.isEmpty() && this.itemMonitor != null) {
            this.itemMonitor.removeListener(this);
        }
    }

    @Override
    public void removeListener() {
        if (this.itemMonitor != null) this.itemMonitor.removeListener(this);
    }

    public IMEMonitor<IAEItemStack> getMonitor() {
        return this.itemMonitor;
    }
}
