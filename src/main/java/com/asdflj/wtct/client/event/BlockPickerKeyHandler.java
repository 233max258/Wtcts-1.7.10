package com.asdflj.wtct.client.event;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import com.asdflj.wtct.Wtct;
import com.asdflj.wtct.network.CPacketTerminalBtns;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The block picker card's client half: watches the "Pick Block" key and, when the block being looked
 * at is not already in the player's inventory, asks the server to pull it out of the network.
 *
 * <p>
 * AE2ImportExportCard mixes into {@code Minecraft#pickBlockOrEntity} for this. 1.7.10's equivalent
 * is a private method reached only from the key binding, so the key is watched from the client tick
 * instead - the same edge that runs vanilla's own pick-block, minus the mixin.
 *
 * <p>
 * The server re-derives everything from the coordinates, so a client that asked for something out of
 * reach gets nothing.
 */
@SideOnly(Side.CLIENT)
public final class BlockPickerKeyHandler {

    /** 1.7.10 binds the pick key as a mouse button, encoded as {@code button - 100}. */
    private static final int MOUSE_KEY_OFFSET = 100;

    private static boolean wasDown;

    private BlockPickerKeyHandler() {}

    public static void tick() {
        final Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null) {
            wasDown = false;
            return;
        }
        final boolean down = isPickKeyDown(mc);
        if (!down) {
            wasDown = false;
            return;
        }
        if (wasDown) {
            return;
        }
        wasDown = true;
        if (mc.playerController == null || mc.playerController.isInCreativeMode()) {
            return;
        }
        final MovingObjectPosition target = mc.objectMouseOver;
        if (target == null || target.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            return;
        }
        final World world = mc.theWorld;
        final ItemStack picked = world.getBlock(target.blockX, target.blockY, target.blockZ)
            .getPickBlock(target, world, target.blockX, target.blockY, target.blockZ, mc.thePlayer);
        if (picked == null || carries(mc, picked) || mc.thePlayer.inventory.getFirstEmptyStack() < 0) {
            return;
        }
        // A terminal with the card has to be carried, but which one is the server's business; the
        // client only sends the block it is looking at.
        final NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("x", target.blockX);
        tag.setInteger("y", target.blockY);
        tag.setInteger("z", target.blockZ);
        Wtct.proxy.netHandler.sendToServer(new CPacketTerminalBtns("CardConfig.PickBlock", 0, tag));
    }

    /** The pick key can be bound to a key or to a mouse button. */
    private static boolean isPickKeyDown(final Minecraft mc) {
        final int code = mc.gameSettings.keyBindPickBlock.getKeyCode();
        return code < 0 ? Mouse.isButtonDown(code + MOUSE_KEY_OFFSET) : Keyboard.isKeyDown(code);
    }

    private static boolean carries(final Minecraft mc, final ItemStack wanted) {
        for (int i = 0; i < mc.thePlayer.inventory.getSizeInventory(); i++) {
            final ItemStack is = mc.thePlayer.inventory.getStackInSlot(i);
            if (is != null && is.isItemEqual(wanted) && ItemStack.areItemStackTagsEqual(is, wanted)) {
                return true;
            }
        }
        return false;
    }
}
