package com.asdflj.wtct.api.adapter.terminal;

import java.util.List;

import net.minecraft.item.Item;

public interface ITerminal {

    List<Class<? extends Item>> getClasses();

    void openCraftAmount();

}
