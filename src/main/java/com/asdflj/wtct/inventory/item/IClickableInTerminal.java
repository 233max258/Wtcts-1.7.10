package com.asdflj.wtct.inventory.item;

import com.asdflj.wtct.util.Util;

public interface IClickableInTerminal {

    void setClickedInterface(Util.DimensionalCoordSide tile);

    Util.DimensionalCoordSide getClickedInterface();
}
