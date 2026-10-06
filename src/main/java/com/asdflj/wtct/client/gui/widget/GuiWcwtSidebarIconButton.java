package com.asdflj.wtct.client.gui.widget;

import net.minecraft.util.ResourceLocation;

/**
 * A button of the terminal's left toolbar that shows a fixed icon instead of cycling an AE2 setting:
 * the same 18x20 plate and 16x16 icon as every other button in that column (see
 * {@link GuiWcwtAeIconButton}), made its own type so the sidebar's surface can be sized around the
 * column - {@code GuiComprehensiveWorkTerminal} counts these together with the AE2 settings buttons
 * and carries the terminal's background to the last one.
 *
 * <p>
 * {@link GuiWcwtToolbarButton} is the settings flavour of the same column; this one is for the card
 * buttons the user pins there (import / export / block picker), whose icons are their own small
 * textures rather than entries of AE2's icon sheet.
 */
public class GuiWcwtSidebarIconButton extends GuiWcwtAeIconButton {

    public GuiWcwtSidebarIconButton(final int x, final int y, final ResourceLocation iconTexture,
        final String tooltip) {
        super(x, y, iconTexture, tooltip);
    }
}
