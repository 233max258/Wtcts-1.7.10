package com.asdflj.wtct.common.storage.infinityCell;

import com.asdflj.wtct.api.WtctAPI;
import com.asdflj.wtct.common.storage.DataStorage;

import appeng.api.implementations.guiobjects.IGuiItemObject;
import appeng.util.Platform;

public interface BaseInventory extends IGuiItemObject {

    default String getUUID() {
        return "";
    }

    default DataStorage getStorage() {
        if (Platform.isServer()) {
            return WtctAPI.instance()
                .getStorageManager()
                .getStorage(this.getItemStack());
        }
        return null;
    }
}
