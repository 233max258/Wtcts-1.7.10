package com.asdflj.wtct.api;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;

import com.asdflj.wtct.api.adapter.terminal.item.FCBaseItemTerminal;
import com.asdflj.wtct.api.adapter.terminal.item.FCUltraTerminal;
import com.asdflj.wtct.api.adapter.terminal.item.IItemTerminal;
import com.asdflj.wtct.api.adapter.terminal.item.TerminalItems;
import com.asdflj.wtct.api.adapter.terminal.item.WCTWirelessCraftingTerminal;
import com.asdflj.wtct.client.event.OpenTerminalEvent;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class TerminalMenu {

    public static List<IItemTerminal> terminalHandlers = new ArrayList<>();
    private final List<TerminalItems> terminalItems = new ArrayList<>();
    private final List<ItemStack> items = new ArrayList<>();

    public TerminalMenu() {
        for (IItemTerminal handler : terminalHandlers) {
            terminalItems.addAll(handler.getTerminalItems());
        }
        for (TerminalItems t : terminalItems) {
            items.add(t.getTargetItem());
        }
    }

    public List<ItemStack> getItems() {
        return items;
    }

    public List<TerminalItems> getTerminalItems() {
        return terminalItems;
    }

    public void openTerminal(int index) {
        try {
            if (index < 0 || index >= terminalItems.size()) {
                return;
            }
            TerminalItems terminal = terminalItems.get(index);
            MinecraftForge.EVENT_BUS.post(new OpenTerminalEvent(terminal));
        } catch (Exception ignored) {}
    }

    static {
        terminalHandlers.add(FCUltraTerminal.instance);
        terminalHandlers.add(FCBaseItemTerminal.instance);
        terminalHandlers.add(WCTWirelessCraftingTerminal.instance);
    }
}
