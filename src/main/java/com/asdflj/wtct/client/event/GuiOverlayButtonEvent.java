package com.asdflj.wtct.client.event;

import net.minecraftforge.client.event.GuiScreenEvent;

import codechicken.nei.recipe.GuiOverlayButton;

public class GuiOverlayButtonEvent extends GuiScreenEvent {

    private final GuiOverlayButton button;

    public GuiOverlayButtonEvent(GuiOverlayButton btn) {
        super(btn.firstGui);
        button = btn;
    }

    public GuiOverlayButton getButton() {
        return button;
    }
}
