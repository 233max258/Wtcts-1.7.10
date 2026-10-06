package com.asdflj.wtct.loader;

import net.minecraft.inventory.IInventory;

import com.asdflj.wtct.api.Constants;
import com.asdflj.wtct.api.WtctAPI;
import com.asdflj.wtct.api.adapter.pattern.THComprehensiveWorkTerminal;
import com.asdflj.wtct.client.gui.container.ContainerComprehensiveWorkTerminal;
import com.asdflj.wtct.integration.Mods;
import com.asdflj.wtct.nei.NEIUtils;

public class PatternTerminalLoader implements Runnable {

    @Override
    public void run() {
        // ME Comprehensive Work Terminal: the encoding area lives on CRAFTING_EX / OUTPUT_EX, so a
        // NEI recipe is written straight into the same inventories the pattern terminal uses.
        WtctAPI.instance()
            .terminal()
            .registerPatternTerminal(new THComprehensiveWorkTerminal())
            .registerIdentifier(Constants.NEI_DEFAULT, (container, inputs, outputs, identifier, adapter, message) -> {
                if (container instanceof ContainerComprehensiveWorkTerminal cwt) {
                    final boolean combine = cwt.combine;
                    cwt.setCraftingMode(message.isCraft);
                    final IInventory inputSlot = cwt.getInventoryByName(Constants.CRAFTING_EX);
                    final IInventory outputSlot = cwt.getInventoryByName(Constants.OUTPUT_EX);
                    for (int i = 0; i < inputSlot.getSizeInventory(); i++) {
                        inputSlot.setInventorySlotContents(i, null);
                    }
                    for (int i = 0; i < outputSlot.getSizeInventory(); i++) {
                        outputSlot.setInventorySlotContents(i, null);
                    }
                    if (!message.isCraft) {
                        if (combine) {
                            inputs = NEIUtils.compress(inputs);
                            outputs = NEIUtils.compress(outputs);
                        }
                        inputs = NEIUtils.clearNull(inputs);
                        outputs = NEIUtils.clearNull(outputs);
                    }
                    adapter.transferPack(inputs, inputSlot);
                    adapter.transferPack(outputs, outputSlot);
                    cwt.saveChanges();
                }
            });
        if (Mods.THAUMIC_ENERGISTICS.isModLoaded()) {}
    }
}
