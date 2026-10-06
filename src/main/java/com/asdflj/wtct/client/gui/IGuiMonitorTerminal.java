package com.asdflj.wtct.client.gui;

import net.minecraft.item.ItemStack;

import com.asdflj.wtct.client.gui.widget.IGuiMonitor;
import com.asdflj.wtct.client.gui.widget.THGuiTextField;

public interface IGuiMonitorTerminal extends IGuiMonitor {

    void setPlayerInv(ItemStack is);

    THGuiTextField getSearchField();
}
