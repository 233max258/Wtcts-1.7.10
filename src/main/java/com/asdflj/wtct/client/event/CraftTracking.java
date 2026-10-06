package com.asdflj.wtct.client.event;

import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;

import com.asdflj.wtct.api.WtctAPI;

import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.eventhandler.Event;

public class CraftTracking extends Event {

    public CraftTracking() {}

    public CraftTracking(IItemList<IAEItemStack> items) {
        WtctAPI.instance()
            .terminal()
            .clearTrackingMissingItems();
        for (IAEItemStack stack : items) {
            WtctAPI.instance()
                .terminal()
                .addTrackingMissingItem(stack);
        }
    }

    public CraftTracking(IAEItemStack stack) {
        WtctAPI.instance()
            .terminal()
            .clearTrackingMissingItems();
        WtctAPI.instance()
            .terminal()
            .addTrackingMissingItem(stack);
    }

    public CraftTracking(ItemStack stack) {
        this(AEItemStack.create(stack));
    }

    public IItemList<IAEItemStack> getItems() {
        return WtctAPI.instance()
            .terminal()
            .getTrackingMissingItems();
    }

    public static void postEvent() {
        MinecraftForge.EVENT_BUS.post(new CraftTracking());
    }
}
