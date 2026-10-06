package com.asdflj.wtct.client.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.asdflj.wtct.Wtct;

import appeng.client.gui.widgets.ITooltip;

/**
 * WCWT's pattern-management header toggle (显示模式 / 显示样板槽 / 搜索模式 / 自动上传): a 14x14 AE2
 * checkbox plate - the same sprite the batch radios use - with a 14x14 icon blitted over it. Copied
 * from WCWT's {@code renderPatternManagementToggleButton}: the plate is the plain radio sprite,
 * switching to the focused one while hovered or active, and everything sinks one pixel then; the
 * icon is scaled down into the plate from its source size (16x16 for AE2 icons, 12x12 for
 * ExtendedAE's).
 */
public class GuiWcwtMgmtToggle extends GuiButton implements ITooltip {

    private static final ResourceLocation WIDGETS = new ResourceLocation(
        Wtct.MODID,
        "textures/guis/wcwt/wcwt_widgets.png");

    /** Plate sprites (the AE2 checkbox radios), 14x14: plain, focused, and the checked box. */
    private static final int PLATE_U = 200;
    private static final int PLATE_NORMAL_V = 0;
    private static final int PLATE_FOCUS_V = 16;
    private static final int PLATE_CHECKED_V = 32;
    private static final int SIZE = 14;

    private int iconU;
    private int iconV;
    private int iconSourceSize;

    private boolean active;
    private boolean checked;
    private boolean hovered;
    /**
     * Whether the checked state sinks the plate as well. The management row's toggles do - a pressed
     * checkbox is what WCWT shows for "on" there - but a toggle that sits in a row of plates which
     * rest flush (the sidebar strip) has to stay flush itself, or it reads as misaligned rather than
     * as pressed.
     */
    private boolean sinkWhenChecked = true;

    /**
     * @param iconU/iconV    sprite position inside wcwt_widgets.png
     * @param iconSourceSize 16 for the AE2 icons, 12 for the ExtendedAE ones
     * @param active         true while this toggle reads as "pressed" (hover or a set state)
     */
    public GuiWcwtMgmtToggle(final int x, final int y, final int iconU, final int iconV, final int iconSourceSize) {
        super(0, x, y, SIZE, SIZE, "");
        this.iconU = iconU;
        this.iconV = iconV;
        this.iconSourceSize = iconSourceSize;
    }

    public void setActive(final boolean active) {
        this.active = active;
    }

    /**
     * Switches a real toggle (WCWT's automatic upload) to its checked plate - the third sprite row,
     * which is the filled radio box. It reads as pressed whether or not the mouse is over it, exactly
     * like WCWT's {@code renderPatternManagementUploadToggleButton} ({@code AE2_RADIO_CHECKED_FOCUS}
     * while the switch is on, {@code AE2_RADIO_UNCHECKED_FOCUS} while merely hovered).
     */
    public void setChecked(final boolean checked) {
        this.checked = checked;
    }

    /** Drops the sink that the checked state otherwise adds; see {@link #sinkWhenChecked}. */
    public void setSinkWhenChecked(final boolean sink) {
        this.sinkWhenChecked = sink;
    }

    /** Swaps the icon (the cyclers change sprite with their mode). */
    public void setIcon(final int iconU, final int iconV, final int iconSourceSize) {
        this.iconU = iconU;
        this.iconV = iconV;
        this.iconSourceSize = iconSourceSize;
    }

    @Override
    public void drawButton(final Minecraft mc, final int mouseX, final int mouseY) {
        if (!this.visible) {
            return;
        }
        this.hovered = mouseX >= this.xPosition && mouseY >= this.yPosition
            && mouseX < this.xPosition + SIZE
            && mouseY < this.yPosition + SIZE;
        this.field_146123_n = this.hovered;
        // Held down: one more pixel than the hover sink, so the press is visible on its own.
        final boolean pressed = this.hovered && org.lwjgl.input.Mouse.isButtonDown(0);
        final boolean sunk = this.hovered || this.active || (this.checked && this.sinkWhenChecked);
        // The checked sprite is drawn one row lower inside its own cell - AE2 bakes the press into
        // the artwork (its opaque area is cell row 2..12 where the plain one is 1..12). A toggle that
        // has to keep the same top edge as the plates beside it takes that row back again, so "on"
        // changes the sprite without dropping the plate out of the row.
        final int artLift = this.checked && !this.sinkWhenChecked ? 1 : 0;
        final int dy = (pressed ? 2 : (sunk ? 1 : 0)) - artLift;
        mc.getTextureManager()
            .bindTexture(WIDGETS);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        drawTexturedModalRect(
            this.xPosition,
            this.yPosition + dy,
            PLATE_U,
            this.checked ? PLATE_CHECKED_V : this.hovered || this.active ? PLATE_FOCUS_V : PLATE_NORMAL_V,
            SIZE,
            SIZE);
        if (this.iconSourceSize > 0) {
            // The icon is scaled into the plate, like WCWT blits a 16x16 (or 12x12) source into the
            // 14x14 button.
            scaledBlit(this.iconU, this.iconV, this.iconSourceSize, this.xPosition, this.yPosition + dy);
        }
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** Blits a square source region of the widget sheet, scaled to fill the button. */
    private void scaledBlit(final int srcX, final int srcY, final int srcSize, final int destX, final int destY) {
        final float f = 1.0F / 256.0F;
        final Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();
        tess.addVertexWithUV(destX, destY + SIZE, this.zLevel, srcX * f, (srcY + srcSize) * f);
        tess.addVertexWithUV(destX + SIZE, destY + SIZE, this.zLevel, (srcX + srcSize) * f, (srcY + srcSize) * f);
        tess.addVertexWithUV(destX + SIZE, destY, this.zLevel, (srcX + srcSize) * f, srcY * f);
        tess.addVertexWithUV(destX, destY, this.zLevel, srcX * f, srcY * f);
        tess.draw();
    }

    /** Set by the GUI so the tooltip can name the state the button currently cycles to. */
    private String tooltip = "";

    public void setTooltip(final String tooltip) {
        this.tooltip = tooltip;
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
