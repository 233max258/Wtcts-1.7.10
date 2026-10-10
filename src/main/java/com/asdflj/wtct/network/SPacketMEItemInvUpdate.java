package com.asdflj.wtct.network;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;

import com.asdflj.wtct.api.Constants;
import com.asdflj.wtct.api.WtctAPI;
import com.asdflj.wtct.client.gui.EarlyTerminalLists;
import com.asdflj.wtct.client.gui.IGuiMonitorTerminal;

import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

/**
 * Packet dedicated to item inventory update.
 */
public class SPacketMEItemInvUpdate extends SPacketMEBaseInvUpdate implements IMessage {

    public SPacketMEItemInvUpdate() {
        super();
    }

    /**
     * Used for the GUI to confirm crafting. 0 = available 1 = pending 2 = missing
     */
    public SPacketMEItemInvUpdate(byte b) {
        super(b);
    }

    public SPacketMEItemInvUpdate(Constants.MessageType type) {
        this(type.type);
    }

    public void appendItem(final IAEItemStack is) {
        list.add(is);
    }

    public List<IAEItemStack> getItemStacks() {
        List<IAEItemStack> items = new ArrayList<>();
        for (IAEStack<?> stack : this.list) {
            if (stack instanceof IAEItemStack item) {
                items.add(item);
            }
        }
        return items;
    }

    public static class Handler implements IMessageHandler<SPacketMEItemInvUpdate, IMessage> {

        @Override
        public IMessage onMessage(SPacketMEItemInvUpdate message, MessageContext ctx) {
            final GuiScreen gs = Minecraft.getMinecraft().currentScreen;
            if (message.ref == Constants.MessageType.UPDATE_ITEMS.type) {
                // TEMP DIAGNOSTIC (1.0.35, remove once the terminal-open delay is pinned down).
                final long t = System.currentTimeMillis();
                final String thread = Thread.currentThread()
                    .getName();
                if (gs instanceof IGuiMonitorTerminal gmt) {
                    cpw.mods.fml.common.FMLLog.info(
                        "[wtct-diag] client item list APPLIED n=%d t=%d thread=%s screen=%s",
                        message.list.size(),
                        t,
                        thread,
                        gs.getClass()
                            .getSimpleName());
                    gmt.postStackUpdate(message.list);
                } else {
                    // The screen swap runs on the client thread while this packet is applied on the
                    // network thread, so a full list that loses the race used to be dropped and the
                    // terminal opened on an empty grid until the network happened to move. Park it:
                    // the GUI's initGui applies it before the first frame draws (EarlyTerminalLists).
                    cpw.mods.fml.common.FMLLog.info(
                        "[wtct-diag] client item list STASHED n=%d t=%d thread=%s screen=%s",
                        message.list.size(),
                        t,
                        thread,
                        gs == null ? "null"
                            : gs.getClass()
                                .getSimpleName());
                    EarlyTerminalLists.stashStacks(message.list);
                }
            } else if (message.ref == Constants.MessageType.UPDATE_PLAYER_ITEM.type) {
                ItemStack is = null;
                if (!message.isEmpty() && message.list.get(0) instanceof IAEItemStack item) {
                    is = item.getItemStack();
                }
                if (gs instanceof IGuiMonitorTerminal gmt) {
                    gmt.setPlayerInv(is);
                } else {
                    EarlyTerminalLists.stashPlayerItem(is);
                }
            } else if (message.ref == Constants.MessageType.UPDATE_PLAYER_CURRENT_ITEM.type) {
                if (gs == null) {
                    Minecraft mc = Minecraft.getMinecraft();
                    EntityClientPlayerMP player = mc.thePlayer;
                    if (message.isEmpty() || !(message.list.get(0) instanceof IAEItemStack item)) return null;
                    player.inventory.setInventorySlotContents(player.inventory.currentItem, item.getItemStack());
                }
            } else if (message.ref == Constants.MessageType.UPDATE_PINNED_ITEMS.type) {
                WtctAPI.instance()
                    .getPinned()
                    .updatePinnedItems(message.getItemStacks());
            } else if (message.ref == Constants.MessageType.ADD_PINNED_ITEM.type) {
                if (!message.isEmpty() && message.list.get(0) instanceof IAEItemStack item) {
                    WtctAPI.instance()
                        .getPinned()
                        .add(item);
                }
            } else if (message.ref == Constants.MessageType.NOTIFICATION.type) {
                if (!message.isEmpty() && message.list.get(0) instanceof IAEItemStack item) {
                    WtctAPI.instance()
                        .addCraftingCompleteNotification(item);
                }
            }
            return null;
        }
    }
}
