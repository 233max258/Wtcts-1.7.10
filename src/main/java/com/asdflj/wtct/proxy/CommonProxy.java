package com.asdflj.wtct.proxy;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldSavedData;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fluids.FluidRegistry;

import com.asdflj.wtct.Wtct;
import com.asdflj.wtct.api.WtctAPI;
import com.asdflj.wtct.api.adapter.crafting.AECraftingTerminal;
import com.asdflj.wtct.api.adapter.crafting.WCTCraftingTerminal;
import com.asdflj.wtct.api.adapter.findit.FluidStorageBusAdapter;
import com.asdflj.wtct.api.adapter.findit.MEChestAdapter;
import com.asdflj.wtct.api.adapter.findit.MEDriverAdapter;
import com.asdflj.wtct.api.adapter.findit.StorageBusAdapter;
import com.asdflj.wtct.api.adapter.terminal.item.FCBaseTerminalHandler;
import com.asdflj.wtct.api.adapter.terminal.item.UltraTerminalHandler;
import com.asdflj.wtct.common.event.NetworkHubHeadProtection;
import com.asdflj.wtct.common.storage.StorageManager;
import com.asdflj.wtct.common.storage.backpack.OKBackpackHandler;
import com.asdflj.wtct.integration.Mods;
import com.asdflj.wtct.loader.BRLoader;
import com.asdflj.wtct.loader.InvLoader;
import com.asdflj.wtct.loader.PatternTerminalLoader;
import com.asdflj.wtct.loader.PatternTerminalMouseWheelLoader;
import com.asdflj.wtct.network.wrapper.WtctNetworkWrapper;
import com.asdflj.wtct.util.ModAndClassUtil;
import com.darkona.adventurebackpack.item.ItemAdventureBackpack;
import com.glodblock.github.common.item.ItemWirelessInterfaceTerminal;
import com.glodblock.github.common.item.ItemWirelessLevelTerminal;
import com.glodblock.github.common.item.ItemWirelessPatternTerminal;
import com.glodblock.github.common.item.ItemWirelessUltraTerminal;

import appeng.api.implementations.ICraftingPatternItem;
import appeng.util.Platform;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import de.eydamos.backpack.item.ItemBackpackBase;
import forestry.storage.items.ItemBackpack;
import ic2.core.Ic2Items;

public class CommonProxy {

    public WtctNetworkWrapper netHandler = new WtctNetworkWrapper(Wtct.MODID);

    public void preInit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new NetworkHubHeadProtection());
        FMLCommonHandler.instance()
            .bus()
            .register(this);
        ModAndClassUtil.init();
        if (Mods.BOTANIA.isModLoaded()) {
            FluidRegistry.registerFluid(
                WtctAPI.instance()
                    .getMana());
        }
    }

    @SubscribeEvent
    public void worldLoad(WorldEvent.Load event) {
        if (Platform.isServer() && event.world.provider.dimensionId == 0) {
            WorldSavedData w = event.world.mapStorage.loadData(StorageManager.class, Wtct.MODID);
            if (w == null) {
                StorageManager s = new StorageManager(Wtct.MODID);
                event.world.mapStorage.setData(Wtct.MODID, s);
            }
        }
    }

    public void init(FMLInitializationEvent event) {
        WtctAPI.instance()
            .terminal()
            .registerCraftingTerminal(new AECraftingTerminal());
        if (Mods.WIRELESS_CRAFTING_TERMINAL.isModLoaded()) {
            WtctAPI.instance()
                .terminal()
                .registerCraftingTerminal(new WCTCraftingTerminal());
        }
        if (Mods.BLOCK_RENDERER.isModLoaded()) {
            new BRLoader().run();
        }
        new PatternTerminalMouseWheelLoader().run();
        new PatternTerminalLoader().run();
        new InvLoader().run();

    }

    public void postInit(FMLPostInitializationEvent event) {
        if (Mods.BACKPACK.isModLoaded()) {
            WtctAPI.instance()
                .addBackpackItem(ItemBackpackBase.class);
        }
        if (Mods.FORESTRY.isModLoaded()) {
            WtctAPI.instance()
                .addBackpackItem(ItemBackpack.class);
        }
        if (Mods.ADVENTURE_BACKPACK.isModLoaded()) {
            WtctAPI.instance()
                .addBackpackItem(ItemAdventureBackpack.class);
        }
        if (Mods.OK_BACKPACK.isModLoaded()) {
            WtctAPI.instance()
                .addBackpackItem(OKBackpackHandler.getBackpackItemClass());
        }
        // The infinity cells, the infusion interface, the thaumatorium interface and the extended IO port are no
        // longer part of this mod, so nothing takes those upgrades any more.
        if (Mods.IC2.isModLoaded()) {
            WtctAPI.instance()
                .setDefaultFluidContainer(Ic2Items.cell);
        }
        // The mana import / export buses are no longer part of this mod either.
        WtctAPI.instance()
            .terminal()
            .registerFindItStorageProvider(new MEChestAdapter());
        WtctAPI.instance()
            .terminal()
            .registerFindItStorageProvider(new MEDriverAdapter());
        WtctAPI.instance()
            .terminal()
            .registerFindItStorageProvider(new StorageBusAdapter());
        WtctAPI.instance()
            .terminal()
            .registerFindItStorageProvider(new FluidStorageBusAdapter());
        // The essentia storage bus adapter went with the rest of the Thaumcraft bridge.
        WtctAPI.instance()
            .terminal()
            .registerTerminalItem(ItemWirelessUltraTerminal.class, new UltraTerminalHandler());
        FCBaseTerminalHandler h = new FCBaseTerminalHandler();
        WtctAPI.instance()
            .terminal()
            .registerTerminalItem(ItemWirelessLevelTerminal.class, h);
        WtctAPI.instance()
            .terminal()
            .registerTerminalItem(ItemWirelessInterfaceTerminal.class, h);
        WtctAPI.instance()
            .terminal()
            .registerTerminalItem(ItemWirelessPatternTerminal.class, h);
    }

    @SubscribeEvent
    public void pickUpEvent(EntityItemPickupEvent event) {
        if (Platform.isClient() || event.entityPlayer == null) return;
        try {
            EntityPlayer player = event.entityPlayer;
            ItemStack pattern = event.item.getEntityItem();
            if (pattern.getItem() != null && pattern.getItem() instanceof ICraftingPatternItem) {
                if (player.inventory.addItemStackToInventory(pattern)) {
                    event.item.setDead();
                    event.setCanceled(true);
                } else if (event.item.isEntityAlive()) {
                    event.item.delayBeforeCanPickup = 20;
                }
            }
        } catch (Exception ignored) {}

    }

    public void onLoadComplete(FMLLoadCompleteEvent event) {

    }

    public void serverStarting(FMLServerStartingEvent event) {

    }

    public void serverStopping(FMLServerStoppingEvent event) {
        StorageManager m = WtctAPI.instance()
            .getStorageManager();
        if (m != null) m.setDirty(true);
    }
}
