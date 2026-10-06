package com.asdflj.wtct.api;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;

import com.asdflj.wtct.Wtct;
import com.asdflj.wtct.network.SPacketCraftingDebugCardUpdate;
import com.asdflj.wtct.util.NameConst;

import appeng.me.Grid;
import appeng.tile.networking.TileController;
import appeng.util.Platform;
import appeng.util.ReadableNumberConverter;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class CraftingDebugCardObject {

    private final NBTTagCompound data;
    private Mode currentMode;

    public enum Mode {
        Everything,
        Player,
        Machine
    }

    public CraftingDebugCardObject(ItemStack itemStack) {
        this.data = Platform.openNbtData(itemStack);
        this.currentMode = Mode.values()[this.data.getByte(Constants.DEBUG_CARD_MODE)];
    }

    public Mode getMode() {
        return currentMode;
    }

    public void setMode(Mode mode) {
        this.currentMode = mode;
        data.setByte(Constants.DEBUG_CARD_MODE, (byte) mode.ordinal());
    }

    public Mode getNextMode() {
        return Mode.values()[(this.getMode()
            .ordinal() + 1) % Mode.values().length];
    }

    @SideOnly(Side.CLIENT)
    public static void sendMessageToPlayer(long networkID, Mode mode) {
        LimitedSizeLinkedList<CraftingDebugHelper.CraftingInfo> infos = WtctAPI.instance()
            .getHistory(networkID);
        EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
        if (infos.isEmpty()) {
            player.addChatComponentMessage(
                new ChatComponentText(I18n.format(NameConst.MESSAGE_CRAFTING_DEBUG_CARD_NO_HISTORY)));
            return;
        }
        List<String> text = getHistoryText(infos, mode);
        if (!text.isEmpty()) {
            for (String s : text) {
                player.addChatComponentMessage(new ChatComponentText(s));
            }
        } else {
            player.addChatComponentMessage(
                new ChatComponentText(I18n.format(NameConst.MESSAGE_CRAFTING_DEBUG_CARD_NO_HISTORY)));
        }
    }

    public void sendRecordToPlayer(Grid grid, EntityPlayer player) {
        if (grid == null) return;
        long id = WtctAPI.instance()
            .getStorageMyID(grid);
        if (FMLCommonHandler.instance()
            .getSide() == Side.SERVER) {
            Wtct.proxy.netHandler.sendTo(
                new SPacketCraftingDebugCardUpdate(
                    id,
                    WtctAPI.instance()
                        .getHistory(grid),
                    this.getMode()),
                (EntityPlayerMP) player);
        } else {
            sendMessageToPlayer(id, this.getMode());
            WtctAPI.instance()
                .saveHistory();
        }
    }

    public Grid getGrid(TileController tc) {
        try {
            return (Grid) tc.getProxy()
                .getGrid();
        } catch (Exception ignored) {}
        return null;
    }

    @SideOnly(Side.CLIENT)
    private static List<String> getHistoryText(LimitedSizeLinkedList<CraftingDebugHelper.CraftingInfo> history,
        Mode mode) {
        List<String> msg = new ArrayList<>();
        if (history.isEmpty()) return msg;
        for (CraftingDebugHelper.CraftingInfo info : history) {
            if (mode == Mode.Player && !info.isPlayer) continue;
            if (mode == Mode.Machine && info.isPlayer) continue;
            msg.add(I18n.format(NameConst.MESSAGE_CRAFTING_DEBUG_CARD_NETWORK_ID) + info.getNetworkID());
            msg.add(
                I18n.format(NameConst.MESSAGE_CRAFTING_DEBUG_CARD_REQUEST_TYPE) + I18n.format(
                    info.isPlayer ? NameConst.MESSAGE_CRAFTING_DEBUG_CARD_REQUEST_TYPE_PLAYER
                        : NameConst.MESSAGE_CRAFTING_DEBUG_CARD_REQUEST_TYPE_MACHINE));
            if (!info.isPlayer && info.pos != null) {
                msg.add(
                    I18n.format(
                        NameConst.MESSAGE_CRAFTING_DEBUG_CARD_POS,
                        info.pos.x,
                        info.pos.y,
                        info.pos.z,
                        info.pos.getDimension()));
            }
            msg.add(I18n.format(NameConst.MESSAGE_CRAFTING_DEBUG_CARD_NAME) + info.name);
            msg.add(I18n.format(NameConst.MESSAGE_CRAFTING_DEBUG_CARD_START_TIME) + info.getFormatStartTime());
            if (info.isFinish()) {
                msg.add(I18n.format(NameConst.MESSAGE_CRAFTING_DEBUG_CARD_USAGE_TIME) + info.getUsageTime());
            }
            switch (info.getState()) {
                case FINISHED -> msg.add(
                    I18n.format(NameConst.MESSAGE_CRAFTING_DEBUG_CARD_STATE)
                        + I18n.format(NameConst.MESSAGE_CRAFTING_DEBUG_CARD_FINISH));
                case CANCELLED -> msg.add(
                    I18n.format(NameConst.MESSAGE_CRAFTING_DEBUG_CARD_STATE)
                        + I18n.format(NameConst.MESSAGE_CRAFTING_DEBUG_CARD_CANCEL));
                case RUNNING -> msg.add(
                    I18n.format(NameConst.MESSAGE_CRAFTING_DEBUG_CARD_STATE)
                        + I18n.format(NameConst.MESSAGE_CRAFTING_DEBUG_CARD_RUNNING));
            }
            if (!info.getErrorMessage()
                .isEmpty()) {
                msg.add(I18n.format(NameConst.MESSAGE_CRAFTING_DEBUG_CARD_ERROR_MESSAGE, info.getErrorMessage()));
            }
            if (info.isSimulation()) {
                msg.add(I18n.format(NameConst.MESSAGE_CRAFTING_DEBUG_CARD_SIMULATION));
            }

            msg.add(
                I18n.format(
                    NameConst.MESSAGE_CRAFTING_DEBUG_CARD_STACK,
                    info.itemName,
                    ReadableNumberConverter.INSTANCE.toWideReadableForm(info.requestSize)));
            msg.add("------------------------------------------------");
        }
        if (!msg.isEmpty()) {
            msg.add(0, "------------------------------------------------");
        }
        return msg;
    }
}
