package com.asdflj.wtct;

import net.minecraft.util.ResourceLocation;

import com.asdflj.wtct.common.Config;
import com.asdflj.wtct.common.storage.CellHandler;
import com.asdflj.wtct.crossmod.waila.WailaInit;
import com.asdflj.wtct.integration.Mods;
import com.asdflj.wtct.inventory.InventoryHandler;
import com.asdflj.wtct.loader.ChannelLoader;
import com.asdflj.wtct.loader.ItemAndBlockHolder;
import com.asdflj.wtct.loader.RecipeLoader;
import com.asdflj.wtct.proxy.CommonProxy;

import appeng.api.AEApi;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import cpw.mods.fml.common.network.NetworkRegistry;

@Mod(
    modid = Wtct.MODID,
    version = Tags.VERSION,
    name = Wtct.NAME,
    dependencies = "required-after:appliedenergistics2;required-after:ae2fc;after:ae2stuff;after:thaumicenergistics;after:ic2")
public class Wtct {

    public static final String MODID = "wtct";
    /**
     * Display name shown in the mod list. The Forge config category uses {@link #MODID} instead, so no non-ASCII
     * string ever reaches the .cfg file (1.7.10's Configuration writes it with the platform encoding).
     */
    public static final String NAME = "AE2实用";

    @Mod.Instance(MODID)
    public static Wtct INSTANCE;

    @SidedProxy(clientSide = "com.asdflj.wtct.proxy.ClientProxy", serverSide = "com.asdflj.wtct.proxy.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    // preInit "Run before anything else. Read your config, create blocks, items, etc, and register them with the
    // GameRegistry." (Remove if not needed)
    public void preInit(FMLPreInitializationEvent event) {
        Config.run();
        ChannelLoader.INSTANCE.run();
        proxy.preInit(event);
        ItemAndBlockHolder.INSTANCE.run();
    }

    @Mod.EventHandler
    // load "Do your mod setup. Build whatever data structures you care about. Register recipes." (Remove if not needed)
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
        if (Mods.WAILA.isModLoaded()) {
            WailaInit.run();
        }
    }

    @Mod.EventHandler
    public void onLoadComplete(FMLLoadCompleteEvent event) {
        proxy.onLoadComplete(event);
    }

    @Mod.EventHandler
    // postInit "Handle interaction with other mods, complete your setup based on this." (Remove if not needed)
    public void postInit(FMLPostInitializationEvent event) {
        NetworkRegistry.INSTANCE.registerGuiHandler(Wtct.INSTANCE, new InventoryHandler());
        AEApi.instance()
            .registries()
            .cell()
            .addCellHandler(new CellHandler());
        RecipeLoader.INSTANCE.run();
        proxy.postInit(event);
    }

    @Mod.EventHandler
    // register server commands in this event handler (Remove if not needed)
    public void serverStarting(FMLServerStartingEvent event) {
        proxy.serverStarting(event);
    }

    @Mod.EventHandler
    // register server commands in this event handler (Remove if not needed)
    public void serverStopping(FMLServerStoppingEvent event) {
        proxy.serverStopping(event);
    }

    public static ResourceLocation resource(String path) {
        return new ResourceLocation(MODID, path);
    }
}
