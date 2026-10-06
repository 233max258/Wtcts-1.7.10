package com.asdflj.wtct.common.tabs;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

import com.asdflj.wtct.Wtct;
import com.asdflj.wtct.loader.ItemAndBlockHolder;

public class WtctTabs extends CreativeTabs {

    public static final WtctTabs INSTANCE = new WtctTabs(Wtct.MODID);

    public WtctTabs(String name) {
        super(name);
    }

    @Override
    public Item getTabIconItem() {
        return ItemAndBlockHolder.ITEM_COMPREHENSIVE_WORK_TERMINAL;
    }

}
