package com.asdflj.wtct.loader;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import com.asdflj.wtct.api.Constants;
import com.asdflj.wtct.api.WtctAPI;
import com.asdflj.wtct.api.adapter.pattern.FCPatternTerminal;
import com.asdflj.wtct.api.adapter.pattern.IRecipeHandler;
import com.asdflj.wtct.api.adapter.pattern.THComprehensiveWorkTerminal;
import com.asdflj.wtct.client.gui.container.ContainerComprehensiveWorkTerminal;
import com.asdflj.wtct.integration.Mods;

import appeng.container.implementations.ContainerPatternTerm;
import appeng.container.implementations.ContainerPatternTermEx;
import appeng.tile.inventory.IAEAppEngInventory;
import appeng.util.Platform;

public class PatternTerminalMouseWheelLoader implements Runnable {

    @Override
    public void run() {
        IRecipeHandler handler = (container, inputs, outputs, identifier, adapter, message) -> {
            if (container instanceof IAEAppEngInventory inventory) {
                ItemStack in = (ItemStack) inputs.get(0)
                    .getStack();
                ItemStack out = (ItemStack) outputs.get(0)
                    .getStack();
                IInventory inv = adapter.getInventoryByName(container, adapter.getCraftingInvName());
                for (int i = 0; i < inv.getSizeInventory(); i++) {
                    if (Platform.isSameItemPrecise(inv.getStackInSlot(i), in)) {
                        inv.setInventorySlotContents(i, out);
                    }
                }
                container.onCraftMatrixChanged(inv);
                inventory.saveChanges();
            }
        };

        // ME Comprehensive Work Terminal: its encoding area lives on CRAFTING_EX / OUTPUT_EX, the same
        // inventories a NEI recipe is written into. Its cells carry amounts, so the terminal itself
        // does the write: a fluid keeps the number it was asking for while the picker cycles which
        // fluid goes in it.
        WtctAPI.instance()
            .terminal()
            .registerPatternTerminal(new THComprehensiveWorkTerminal())
            .registerIdentifier(
                Constants.NEI_MOUSE_WHEEL,
                (container, inputs, outputs, identifier, adapter, message) -> {
                    if (container instanceof ContainerComprehensiveWorkTerminal c) {
                        c.cycleEncodingIngredient(
                            (ItemStack) inputs.get(0)
                                .getStack(),
                            (ItemStack) outputs.get(0)
                                .getStack());
                    }
                });
        WtctAPI.instance()
            .terminal()
            .registerPatternTerminal(new FCPatternTerminal(ContainerPatternTerm.class))
            .registerIdentifier(Constants.NEI_MOUSE_WHEEL, handler);
        WtctAPI.instance()
            .terminal()
            .registerPatternTerminal(new FCPatternTerminal(ContainerPatternTermEx.class))
            .registerIdentifier(Constants.NEI_MOUSE_WHEEL, handler);

        if (Mods.THAUMIC_ENERGISTICS.isModLoaded()) {}
    }

}
