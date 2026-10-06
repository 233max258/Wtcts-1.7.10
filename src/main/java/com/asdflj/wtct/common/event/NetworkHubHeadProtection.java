package com.asdflj.wtct.common.event;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraftforge.event.world.BlockEvent;

import com.asdflj.wtct.common.block.BlockNetworkHub;
import com.asdflj.wtct.common.tile.TileNetworkHub;
import com.asdflj.wtct.util.NameConst;

import appeng.util.Platform;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * Stops a network's head from being dug up by accident. The head owns the stored network, so mining it without
 * deleting the network first would leave every receiver pointing at a block that is no longer there. Sneak-wrenching
 * is still allowed, which is the one way the block is meant to be taken back.
 */
public class NetworkHubHeadProtection {

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.world.isRemote) return;
        if (event.block != BlockNetworkHub.INSTANCE) return;
        if (event.getPlayer() == null) return;
        if (!(event.world.getTileEntity(event.x, event.y, event.z) instanceof TileNetworkHub hub)) return;
        if (!hub.isHead()) return;

        ItemStack held = event.getPlayer()
            .getHeldItem();
        if (event.getPlayer()
            .isSneaking() && held != null
            && Platform.isWrench(event.getPlayer(), held, event.x, event.y, event.z)) {
            return;
        }

        event.getPlayer()
            .addChatMessage(new ChatComponentTranslation(NameConst.MESSAGE_NETWORK_HUB_HEAD_PROTECTION));
        event.setCanceled(true);
    }
}
