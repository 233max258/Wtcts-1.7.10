package com.asdflj.wtct.loader;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import com.asdflj.wtct.Wtct;
import com.asdflj.wtct.api.InventoryActionExtend;
import com.asdflj.wtct.api.WtctAPI;
import com.asdflj.wtct.network.CPacketInventoryActionExtend;
import com.asdflj.wtct.util.BlockPos;
import com.asdflj.wtct.util.Util;

import appeng.util.item.AEItemStack;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class KeybindLoader implements Runnable {

    public static KeyBinding openTerminalMenu;
    public static KeyBinding openComprehensiveWorkTerminal;

    @Override
    public void run() {
        // The backpack terminal and the dual interface terminal are no longer part of this mod, so neither is
        // their keybinding; only the terminal menu and the comprehensive terminal itself remain.
        openTerminalMenu = new KeyBinding(
            Wtct.MODID + ".key.open_terminal",
            Keyboard.CHAR_NONE,
            "itemGroup." + Wtct.MODID);
        ClientRegistry.registerKeyBinding(openTerminalMenu);
        openComprehensiveWorkTerminal = new KeyBinding(
            Wtct.MODID + ".key.open_comprehensive_work_terminal",
            Keyboard.CHAR_NONE,
            "itemGroup." + Wtct.MODID);
        ClientRegistry.registerKeyBinding(openComprehensiveWorkTerminal);
        FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent event) {
        if (Minecraft.getMinecraft().currentScreen != null) return;
        EntityClientPlayerMP p = Minecraft.getMinecraft().thePlayer;
        // middle click
        // The gesture asks the network for the block under the crosshair, and no longer insists on an
        // empty hand: carrying something used to make the click a silent no-op, so the block you were
        // looking at could not be ordered - nor offered for crafting - unless you first emptied your
        // hand. What the request actually needs is somewhere to put the stack, which is what
        // findReceivingSlot answers: the hand when it is free (unchanged for a player who is not
        // holding anything), otherwise the first empty slot. A full inventory still has nowhere to put
        // it, and the click stays a no-op there.
        if (Mouse.isButtonDown(2) && !p.capabilities.isCreativeMode) {
            // request item
            ItemStack block = getTargetBlock(p.getEntityWorld(), p);
            if (block != null && Util.findItemStack(p, block) == -1) {
                int slot = Util.findReceivingSlot(p);
                if (slot >= 0) {
                    Wtct.proxy.netHandler.sendToServer(
                        new CPacketInventoryActionExtend(
                            InventoryActionExtend.REQUEST_ITEM,
                            slot,
                            0,
                            AEItemStack.create(block)));
                }
            }
            return;
        }
        if (!(event instanceof InputEvent.KeyInputEvent) && !(event instanceof InputEvent.MouseInputEvent)) return;
        if (p.openContainer == null) {
            return;
        }
        if (openTerminalMenu.isPressed()) {
            WtctAPI.instance()
                .openTerminalMenu();
        }
        if (openComprehensiveWorkTerminal.isPressed()) {
            WtctAPI.instance()
                .openComprehensiveWorkTerminal();
        }

    }

    private static ItemStack getTargetBlock(World world, EntityPlayer player) {
        Vec3 position = Vec3.createVectorHelper(player.posX, player.posY + player.getEyeHeight(), player.posZ);
        Vec3 look = player.getLookVec();
        Vec3 end = position.addVector(look.xCoord * 5.0, look.yCoord * 5.0, look.zCoord * 5.0);
        MovingObjectPosition hit = world.rayTraceBlocks(position, end);

        if (hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            return new BlockPos(hit, world).getPickBlock(hit, world, player);
        }
        return null;
    }
}
