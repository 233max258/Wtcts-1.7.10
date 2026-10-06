package com.asdflj.wtct.common.nethub;

import javax.annotation.Nullable;

/** The little bit of a network or a user the network hub's list control needs to key and label a row. */
public interface IListObject {

    @Nullable
    Object getUuid();

    @Nullable
    String getName();
}
