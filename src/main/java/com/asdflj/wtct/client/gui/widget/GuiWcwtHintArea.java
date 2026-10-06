package com.asdflj.wtct.client.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;

import appeng.client.gui.widgets.ITooltip;

/**
 * An invisible rectangle that only exists to show a tooltip - AE2 1.21's composite widgets do this
 * for their hover areas ({@code UpgradesPanel.getTooltip}, the "Compatible Upgrades" list over the
 * upgrade column), and 1.7.10 has no composite-widget hook to hang it on, so it rides the button
 * list. It draws nothing and its click is a no-op: {@code GuiContainer.mouseClicked} handles buttons
 * before slots, and a button that does nothing leaves the slot click intact.
 */
public class GuiWcwtHintArea extends GuiButton implements ITooltip {

    private String tooltip;

    public GuiWcwtHintArea(final int x, final int y, final int width, final int height, final String tooltip) {
        super(4711, x, y, width, height, "");
        this.tooltip = tooltip;
    }

    public void setTooltip(final String tooltip) {
        this.tooltip = tooltip;
    }

    /** Deliberately blank - this widget is a hover area, not a picture. */
    @Override
    public void drawButton(final Minecraft mc, final int mouseX, final int mouseY) {}

    /**
     * Never claims the click: claiming it would play the button click sound over the panel's slots
     * and make this widget the screen's selected button. The tooltip does not depend on it.
     */
    @Override
    public boolean mousePressed(final Minecraft mc, final int mouseX, final int mouseY) {
        return false;
    }

    @Override
    public String getMessage() {
        return this.tooltip;
    }

    @Override
    public int xPos() {
        return this.xPosition;
    }

    @Override
    public int yPos() {
        return this.yPosition;
    }

    @Override
    public int getWidth() {
        return this.width;
    }

    @Override
    public int getHeight() {
        return this.height;
    }

    @Override
    public boolean isVisible() {
        return this.visible && this.tooltip != null && !this.tooltip.isEmpty();
    }
}
