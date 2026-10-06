package com.asdflj.wtct.client.event;

import com.asdflj.wtct.Wtct;
import com.asdflj.wtct.api.adapter.terminal.item.TerminalItems;
import com.asdflj.wtct.network.CPacketOpenTerminal;

import cpw.mods.fml.common.eventhandler.Event;

public class OpenTerminalEvent extends Event {

    private final TerminalItems items;

    public OpenTerminalEvent(TerminalItems terminalItems) {
        items = terminalItems;
    }

    public void openTerminal() {
        Wtct.proxy.netHandler.sendToServer(new CPacketOpenTerminal(items));
    }
}
