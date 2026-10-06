package com.asdflj.wtct.common.nethub;

/**
 * The four levels a player can hold on a network, weakest first. {@link Group#hasPermission} compares
 * ordinals to decide whether a level is enough, so the order must not be rearranged.
 */
public enum Perm {

    NONE,
    USER,
    ADMIN,
    OWNER
}
