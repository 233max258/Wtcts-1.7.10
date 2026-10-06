package com.asdflj.wtct.api.adapter.pattern;

import java.util.List;

import net.minecraft.inventory.Container;

import com.asdflj.wtct.nei.object.OrderStack;
import com.asdflj.wtct.network.CPacketTransferRecipe;

@FunctionalInterface
public interface IRecipeHandler {

    void transferPack(Container container, List<OrderStack<?>> inputs, List<OrderStack<?>> outputs, String identifier,
        IPatternTerminalAdapter adapter, CPacketTransferRecipe message);
}
