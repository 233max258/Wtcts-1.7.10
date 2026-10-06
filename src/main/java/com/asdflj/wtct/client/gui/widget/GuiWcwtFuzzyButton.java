package com.asdflj.wtct.client.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.asdflj.wtct.Wtct;

import appeng.api.config.FuzzyMode;
import appeng.client.gui.widgets.ITooltip;

/**
 * The fuzzy card's matching-mode toggle - AE2ImportExportCard's fuzzy button (its
 * {@code ServerSettingToggleButton<Settings.FUZZY_MODE>} on the left toolbar), which the guide's
 * "filter by damage level and/or ignore item NBT" line describes.
 *
 * <p>
 * Drawn as AE2's toolbar plate with AE2 1.21's own fuzzy sprites from {@code states.png}
 * ({@code FUZZY_IGNORE} at (64,96) and the {@code FUZZY_PERCENT_*} row at y=96), so the button reads
 * as the same widget the addon shows. The modes without a 1.21 sprite - {@code PERCENT_10} and
 * {@code PERCENT_1} - are not part of the cycle.
 */
public class GuiWcwtFuzzyButton extends GuiButton implements ITooltip {

    private static final ResourceLocation ICONS = new ResourceLocation(
        Wtct.MODID,
        "textures/guis/wcwt/ae2_toolbar_states.png");

    /** AE2 1.21's toolbar plate: 18x20 at y=128, drawn at {@code x - 1} like every sidebar button. */
    private static final int BG_V = 128, BG_W = 18, BG_H = 20;
    private static final int BG_U_IDLE = 176, BG_U_HOVER = 212;

    /** The cycle, in AE2 1.21's icon order. */
    private static final FuzzyMode[] MODES = { FuzzyMode.IGNORE_ALL, FuzzyMode.PERCENT_99, FuzzyMode.PERCENT_75,
        FuzzyMode.PERCENT_50, FuzzyMode.PERCENT_25 };

    /** {@code Icon.FUZZY_IGNORE} then {@code Icon.FUZZY_PERCENT_99..25}, all in states.png's y=96 row. */
    private static final int[][] MODE_ICONS = { { 64, 96 }, { 48, 96 }, { 32, 96 }, { 16, 96 }, { 0, 96 } };

    private FuzzyMode mode = FuzzyMode.IGNORE_ALL;
    private String tooltip;

    public GuiWcwtFuzzyButton(final int x, final int y, final String tooltip) {
        super(0, x, y, 16, 16, "");
        this.tooltip = tooltip;
    }

    public void setMode(final FuzzyMode mode) {
        this.mode = mode == null ? FuzzyMode.IGNORE_ALL : mode;
    }

    public FuzzyMode getMode() {
        return this.mode;
    }

    public void setTooltip(final String tooltip) {
        this.tooltip = tooltip;
    }

    /** The index of {@code mode} in the cycle, or 0 for anything without a sprite. */
    private static int indexOf(final FuzzyMode mode) {
        for (int i = 0; i < MODES.length; i++) {
            if (MODES[i] == mode) {
                return i;
            }
        }
        return 0;
    }

    @Override
    public void drawButton(final Minecraft mc, final int mouseX, final int mouseY) {
        if (!this.visible) {
            return;
        }
        this.field_146123_n = mouseX >= this.xPosition && mouseY >= this.yPosition
            && mouseX < this.xPosition + this.width
            && mouseY < this.yPosition + this.height;
        final int dy = this.field_146123_n ? 1 : 0;

        mc.getTextureManager()
            .bindTexture(ICONS);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        drawTexturedModalRect(
            this.xPosition - 1,
            this.yPosition + dy,
            this.field_146123_n ? BG_U_HOVER : BG_U_IDLE,
            BG_V,
            BG_W,
            BG_H);
        final int[] icon = MODE_ICONS[indexOf(this.mode)];
        drawTexturedModalRect(this.xPosition, this.yPosition + 1 + dy, icon[0], icon[1], 16, 16);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
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
        return this.visible;
    }
}
