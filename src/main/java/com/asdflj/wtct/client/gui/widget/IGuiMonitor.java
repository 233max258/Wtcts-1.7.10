package com.asdflj.wtct.client.gui.widget;

import java.util.List;

import com.asdflj.wtct.client.me.AdvItemRepo;

import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.client.gui.widgets.ISortSource;

public interface IGuiMonitor extends ISortSource {

    @Deprecated
    default void postFluidUpdate(List<IAEFluidStack> list) {
        postStackUpdate(list);
    }

    @Deprecated
    default void postUpdate(List<IAEItemStack> list) {
        postStackUpdate(list);
    }

    void postStackUpdate(List<? extends IAEStack<?>> list);

    void setScrollBar();

    AdvItemRepo getRepo();

    void handleKeyboardInput();
}
