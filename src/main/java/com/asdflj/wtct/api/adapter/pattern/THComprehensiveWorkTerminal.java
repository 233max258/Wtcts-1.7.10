package com.asdflj.wtct.api.adapter.pattern;

import net.minecraft.inventory.Container;

import com.asdflj.wtct.api.Constants;
import com.asdflj.wtct.client.gui.container.ContainerComprehensiveWorkTerminal;

/**
 * NEI recipe transfer for the ME Comprehensive Work Terminal: the encoding inputs and outputs live on
 * the terminal's CRAFTING_EX / OUTPUT_EX inventories, exactly like the wireless dual interface
 * terminal - so only the container class differs here.
 */
public class THComprehensiveWorkTerminal implements IPatternTerminalAdapter {

    @Override
    public boolean supportFluid() {
        return true;
    }

    @Override
    public Class<? extends Container> getContainer() {
        return ContainerComprehensiveWorkTerminal.class;
    }

    @Override
    public String getOutputInvName() {
        return Constants.OUTPUT_EX;
    }

    @Override
    public String getCraftingInvName() {
        return Constants.CRAFTING_EX;
    }
}
