package com.asdflj.wtct.api;

import appeng.api.events.GuiScrollEvent;
import codechicken.nei.recipe.GuiOverlayButton;

@FunctionalInterface
public interface MouseWheelHandler {

    boolean handleMouseWheel(GuiScrollEvent event, GuiOverlayButton overlayButton);
}
