package com.asdflj.wtct.api;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

public interface IBackpackItem {

    IInventory getInventory(ItemStack is);
}
