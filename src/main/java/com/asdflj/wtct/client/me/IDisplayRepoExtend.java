package com.asdflj.wtct.client.me;

import appeng.api.storage.data.IAEItemStack;

public interface IDisplayRepoExtend {

    void addEntriesToView(Iterable<IAEItemStack> entries);

    void setAdvRepoPause(boolean pause);
}
