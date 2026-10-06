package com.asdflj.wtct.loader;

import com.asdflj.wtct.integration.Mods;
import com.asdflj.wtct.util.BaublesUtil;
import com.asdflj.wtct.util.InvUtil;

public class InvLoader implements Runnable {

    @Override
    public void run() {
        InvUtil.INVENTORY.add(player -> player.inventory);
        if (Mods.BAUBLES.isModLoaded()) {
            InvUtil.INVENTORY.add(BaublesUtil::getBaublesInv);
        }
    }
}
