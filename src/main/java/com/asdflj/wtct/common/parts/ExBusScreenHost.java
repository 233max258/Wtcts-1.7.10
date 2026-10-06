package com.asdflj.wtct.common.parts;

import appeng.api.implementations.IUpgradeableHost;
import appeng.api.storage.data.IAEStackType;
import appeng.tile.inventory.IAEStackInventory;

/**
 * The screen-side view of an extended bus part: how its window titles it, which filter grid the phantom slots
 * edit, and which stack type the grid defaults to.
 *
 * <p>
 * The three extended buses - import, export and storage - share one screen and one container built on AE2's
 * {@code GuiUpgradeable} machinery, but they keep those pieces of themselves private: the name lives on the item
 * stack (rv3's {@code getBusName} is client-only), each part owns its own sixty-three slot grid, and the storage
 * bus answers for items where the IO buses answer for their own type. This is the one interface that lets the
 * shared screen work against all three without caring which.
 */
public interface ExBusScreenHost extends IUpgradeableHost {

    /** The name the window is titled with - the part item's display name, on both sides. */
    String getBusDisplayName();

    /** The filter grid the virtual phantom slots read and write. */
    IAEStackInventory getExConfig();

    /** The stack type the bus is built around; the grid also lets fluids through beside it. */
    IAEStackType<?> getGuiStackType();
}
