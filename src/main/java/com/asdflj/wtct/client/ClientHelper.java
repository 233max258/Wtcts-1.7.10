package com.asdflj.wtct.client;

import java.util.Arrays;

import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;

import com.asdflj.wtct.client.icon.Fluids;
import com.asdflj.wtct.client.textures.BlockTexture;
import com.asdflj.wtct.client.textures.ItemTexture;
import com.asdflj.wtct.util.NameConst;

import appeng.api.util.AEColor;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class ClientHelper {

    /**
     * Watches the pick-block key for the block picker card. Nothing else in the mod needs a client
     * tick, so this is registered here rather than through a listener of its own.
     */
    @SubscribeEvent
    public void onClientTick(final cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent event) {
        if (event.phase == cpw.mods.fml.common.gameevent.TickEvent.Phase.END) {
            com.asdflj.wtct.client.event.BlockPickerKeyHandler.tick();
        }
    }

    @SubscribeEvent
    public void updateTextureSheet(final TextureStitchEvent.Pre ev) {
        if (ev.map.getTextureType() == 0) {
            for (int i = 0; i < AEColor.values().length; i++) {
                BlockTexture.registerIcon(
                    ev.map,
                    NameConst.BLOCK_WIRELESS_DISTRIBUTOR,
                    NameConst.RES_KEY + "wireless_distributor/side_on" + i);
                BlockTexture.registerIcon(
                    ev.map,
                    NameConst.BLOCK_WIRELESS_DISTRIBUTOR,
                    NameConst.RES_KEY + "wireless_distributor/side_off" + i,
                    false);
            }

            // The unpowered icon has to go in first: adding an icon always puts it in the "off" list, and only the
            // powered flag additionally puts it in "on". Registering them the other way round would make the block
            // draw the lit icon while it is disconnected.
            BlockTexture.registerIcon(
                ev.map,
                NameConst.BLOCK_NETWORK_HUB,
                NameConst.RES_KEY + NameConst.BLOCK_NETWORK_HUB + "_side_off",
                false);
            BlockTexture.registerIcon(
                ev.map,
                NameConst.BLOCK_NETWORK_HUB,
                NameConst.RES_KEY + NameConst.BLOCK_NETWORK_HUB + "_side_on");

            for (final BlockTexture cb : BlockTexture.values()) {
                cb.registerIcon(ev.map);
            }
            Arrays.stream(Fluids.values())
                .forEach(fluids -> fluids.registerIcon(ev.map));
        }
        if (ev.map.getTextureType() == 1) {
            for (final ItemTexture cb : ItemTexture.values()) {
                cb.registerIcon(ev.map);
            }
        }
    }

    public static void register() {
        ClientHelper handler = new ClientHelper();
        MinecraftForge.EVENT_BUS.register(handler);
        FMLCommonHandler.instance()
            .bus()
            .register(handler);
    }
}
