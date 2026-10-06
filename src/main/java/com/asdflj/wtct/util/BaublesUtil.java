package com.asdflj.wtct.util;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import org.apache.commons.lang3.tuple.ImmutablePair;

import com.asdflj.wtct.api.Constants;
import com.asdflj.wtct.api.adapter.terminal.item.TerminalItems;
import com.asdflj.wtct.integration.Mods;
import com.asdflj.wtct.inventory.gui.GuiBridgeInvType;

import appeng.util.Platform;
import baubles.api.BaublesApi;
import baubles.common.lib.PlayerHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;

public class BaublesUtil {

    public static IInventory getBaublesInv(EntityPlayer player) {
        return BaublesApi.getBaubles(player);
    }

    /**
     * Resolves the item stack a bridge-encoded coordinate points at. The value stored in the GUI-open x
     * coordinate may address either the player's main inventory ({@link GuiBridgeInvType#PLAYER_INV}) or
     * the Baubles inventory ({@link GuiBridgeInvType#PLAYER_BAUBLES}).
     */
    public static ItemStack getStackFromBridge(EntityPlayer player, int x) {
        ImmutablePair<GuiBridgeInvType, Integer> result = GuiBridgeInvType.decode(x);
        if (result.left == GuiBridgeInvType.PLAYER_BAUBLES) {
            if (!Mods.BAUBLES.isModLoaded()) return null;
            IInventory inv = getBaublesInv(player);
            return inv == null ? null : inv.getStackInSlot(result.right);
        }
        return player.inventory.getStackInSlot(result.right);
    }

    /**
     * Writes an item stack back to the slot a bridge-encoded coordinate points at. Main-inventory slots are
     * written through {@code InventoryPlayer}; bauble slots need the Baubles inventory plus a sync so the
     * client sees the change (Baubles does not replicate its container by itself).
     */
    public static void setStackFromBridge(EntityPlayer player, int x, ItemStack is) {
        ImmutablePair<GuiBridgeInvType, Integer> result = GuiBridgeInvType.decode(x);
        if (result.left == GuiBridgeInvType.PLAYER_BAUBLES) {
            if (!Mods.BAUBLES.isModLoaded()) return;
            IInventory inv = getBaublesInv(player);
            if (inv == null) return;
            inv.setInventorySlotContents(result.right, is);
            inv.markDirty();
            syncBaubles(player);
            return;
        }
        player.inventory.setInventorySlotContents(result.right, is);
    }

    /** True when the bridge-encoded coordinate addresses a Baubles slot. */
    public static boolean isBaublesSlot(int x) {
        return GuiBridgeInvType.decode(x).left == GuiBridgeInvType.PLAYER_BAUBLES;
    }

    /**
     * True when {@code x} is a bridge-encoded coordinate rather than a plain inventory slot index. Used by
     * code that may receive either form and must not feed a bridge coordinate to raw inventory lookups.
     */
    public static boolean isBridgeSlot(int x) {
        return Math.abs(x) > GuiBridgeInvType.LIMIT;
    }

    /**
     * The plain player-inventory index a bridge coordinate addresses, or {@code -1} when it does not address one
     * (a Baubles slot, or a coordinate that is out of the inventory's range).
     *
     * <p>
     * AE2 locks the slot a portable terminal lives in by feeding it to {@code lockPlayerInventorySlot}, which the
     * slot binder compares against raw indices - so a bridge-encoded coordinate has to be unwrapped first, or the
     * terminal stays movable while its own screen is open.
     */
    public static int decodedPlayerSlot(int x) {
        final ImmutablePair<GuiBridgeInvType, Integer> decoded = GuiBridgeInvType.decode(x);
        if (decoded.left != GuiBridgeInvType.PLAYER_INV) {
            return -1;
        }
        final int slot = decoded.right;
        return slot >= 0 && slot < 36 ? slot : -1;
    }

    /**
     * Where the GUI factory can find {@code stack}: its main-inventory index where it has one, and otherwise -
     * for a terminal worn in a Baubles slot, which has no main-inventory index at all - the bridge-encoded
     * coordinate {@link #getStackFromBridge} reads back. {@code -1} when it is in neither place.
     *
     * <p>
     * The code that reopens a terminal's own screen from something that only knows the stack needs this: the
     * plain main-inventory scan it used before only ever found a terminal carried in the main inventory, so a
     * terminal worn in a Baubles slot silently had that half of the gesture skipped.
     */
    public static int findBridgeSlot(EntityPlayer player, ItemStack stack) {
        if (player == null || stack == null) {
            return -1;
        }
        for (int i = 0; i < player.inventory.mainInventory.length; i++) {
            if (player.inventory.mainInventory[i] == stack) {
                return i;
            }
        }
        if (!Mods.BAUBLES.isModLoaded()) {
            return -1;
        }
        IInventory baubles = getBaublesInv(player);
        if (baubles == null) {
            return -1;
        }
        for (int i = 0; i < baubles.getSizeInventory(); i++) {
            if (baubles.getStackInSlot(i) == stack) {
                return GuiBridgeInvType.encode(i, GuiBridgeInvType.PLAYER_BAUBLES);
            }
        }
        return -1;
    }

    public static void syncBaubles(EntityPlayer player) {
        Side side = FMLCommonHandler.instance()
            .getEffectiveSide();
        if (side == Side.SERVER) {
            for (int a = 0; a < 4; a++) {
                PlayerHandler.getPlayerBaubles(player)
                    .syncSlotToClients(a);
            }
        }
    }

    public static boolean isSameItemPrecise(ItemStack is1, ItemStack is2, int slotIndex, TerminalItems terminalItems) {
        // baubles can't sync inv to client side,so i use slot to make sure is same item
        if (Platform.isSameItem(is1, is2)) {
            int slot = terminalItems.getData()
                .getInteger(Constants.SLOT);
            return slotIndex == slot;
        }
        return false;
    }
}
