package com.asdflj.wtct.client.gui.container;

import net.minecraft.entity.player.InventoryPlayer;

import appeng.api.storage.ITerminalHost;
import appeng.container.AEBaseContainer;

public class ContainerRenamer extends AEBaseContainer {

    public ContainerRenamer(InventoryPlayer ip, ITerminalHost monitorable) {
        super(ip, monitorable);
    }
}
