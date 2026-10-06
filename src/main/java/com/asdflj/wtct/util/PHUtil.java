package com.asdflj.wtct.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import net.minecraft.item.ItemStack;

import com.asdflj.wtct.integration.Mods;
import com.asdflj.wtct.nei.object.OrderStack;

import reobf.proghatches.item.ItemProgrammingCircuit;
import reobf.proghatches.item.ItemProgrammingToolkit;

public class PHUtil {

    /**
     * True for programmablehatches' programming circuit (编程器电路), the item a processing pattern
     * carries to pick the machine's recipe. The mod is a soft dependency, so the class is only touched
     * when it is actually loaded - the same guard the recipe transfer below is called under.
     */
    public static boolean isProgrammingCircuit(final ItemStack stack) {
        return stack != null && Mods.PROGRAMMABLE_HATCHES.isModLoaded()
            && stack.getItem() instanceof ItemProgrammingCircuit;
    }

    public static List<OrderStack<?>> transfer(List<OrderStack<?>> inputs) {
        AtomicBoolean circuit = new AtomicBoolean(false);
        if (!ItemProgrammingToolkit.holding()) {
            return inputs;
        }
        AtomicInteger i = new AtomicInteger(0);
        ArrayList<OrderStack<?>> spec = new ArrayList<>();
        List<OrderStack<?>> ret = inputs.stream()
            .filter(Objects::nonNull)
            .sorted(Comparator.comparingInt(OrderStack::getIndex))
            .filter(orderStack -> {
                boolean regular = !(orderStack.getStack() != null && orderStack.getStack() instanceof ItemStack
                    && ((ItemStack) orderStack.getStack()).stackSize == 0);
                if (!regular) {
                    circuit.set(true);
                    spec.add(
                        new OrderStack<>(
                            ItemProgrammingCircuit.wrap(((ItemStack) orderStack.getStack())),
                            orderStack.getIndex()));
                    return false;
                }

                return true;
            })
            .collect(Collectors.toList());

        if (!circuit.get() && ItemProgrammingToolkit.addEmptyProgCiruit()) {
            spec.add(0, new OrderStack<>(ItemProgrammingCircuit.wrap(null), 0));
        }

        spec.addAll(ret);
        spec.forEach((orderStack -> orderStack.setIndex(i.getAndIncrement())));

        return spec;
    }
}
