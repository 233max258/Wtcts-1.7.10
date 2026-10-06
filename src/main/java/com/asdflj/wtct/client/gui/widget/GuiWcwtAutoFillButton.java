package com.asdflj.wtct.client.gui.widget;

import net.minecraft.util.ResourceLocation;

import com.asdflj.wtct.Wtct;

/**
 * The terminal's auto-fill switch as a button of its left toolbar - ae2helpers' own
 * {@code AutoInsertButton}, which sits in the crafting terminal's sidebar and shows the setting by
 * swapping its icon. It is the only place that flips {@code ContainerWcwtSettings.KEY_CRAFT_IF_MISSING}
 * now that the settings screen has moved on to the middle-click switch.
 *
 * <p>
 * The icon pair is AE2 1.21's {@code ACCESS_WRITE} / {@code ACCESS_READ} sprites of its states sheet,
 * which is what the reference mod puts on its button for on and off.
 */
public class GuiWcwtAutoFillButton extends GuiWcwtSidebarIconButton {

    /** AE2 1.21's redrawn {@code states.png}, the sheet both sprites come from. */
    private static final ResourceLocation ICONS = new ResourceLocation(
        Wtct.MODID,
        "textures/guis/wcwt/ae2_121_states.png");

    /** {@code Icon.ACCESS_WRITE} and {@code Icon.ACCESS_READ} in that sheet. */
    private static final int ON_U = 0, ON_V = 144;
    private static final int OFF_U = 16, OFF_V = 144;

    private boolean on;

    public GuiWcwtAutoFillButton(final int x, final int y, final String tooltip) {
        super(x, y, ICONS, tooltip);
        // The sheet is a sheet, not an icon: without a sprite named there is nothing sane to draw.
        this.setOn(false);
    }

    /** Shows the switch as on or off; the tooltip naming the state is the caller's business. */
    public void setOn(final boolean on) {
        this.on = on;
        this.setIconRegion(on ? ON_U : OFF_U, on ? ON_V : OFF_V);
    }

    public boolean isOn() {
        return this.on;
    }
}
