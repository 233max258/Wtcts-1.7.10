package com.asdflj.wtct.client.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import com.asdflj.wtct.Wtct;

import appeng.client.gui.widgets.ITooltip;

/**
 * WCWT's batch property radio (物品替换 / 可作为替换), a 14x14 AE2 1.21 radio sprite in three states -
 * unchecked, unchecked-hover and checked - copied from ae2's checkbox.png into wcwt_widgets.png.
 * Exactly like WCWT's {@code renderBatchPropertyButton}: hovering or being selected pushes the
 * sprite down one pixel. The tooltip is ours rather than AE2 1.21's {@code gui.tooltips.ae2.*} keys,
 * which 1.7.10 does not ship - it would have printed the key itself.
 */
public class GuiWcwtRadio extends GuiButton implements ITooltip {

    private static final ResourceLocation WIDGETS = new ResourceLocation(
        Wtct.MODID,
        "textures/guis/wcwt/wcwt_widgets.png");

    private static final int SIZE = 14;
    private static final int UNCHECKED_U = 200;
    private static final int UNCHECKED_V = 0;
    private static final int UNCHECKED_FOCUS_V = 16;
    private static final int CHECKED_FOCUS_V = 32;

    private boolean selected;
    private boolean hovered;
    /** Base lang key of the tooltip; {@code .on} / {@code .off} is appended for the state. */
    private final String tooltipKey;

    public GuiWcwtRadio(final int x, final int y, final String tooltipKey) {
        super(0, x, y, SIZE, SIZE, "");
        this.tooltipKey = tooltipKey;
    }

    public void setSelected(final boolean selected) {
        this.selected = selected;
    }

    public boolean isSelected() {
        return this.selected;
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
        // Held down on the plate: the sprite sinks a second pixel, so a press reads as a press and not
        // as the hover that precedes it (the checked/focus sprite alone is only one pixel of shading).
        final boolean pressed = this.hovered && org.lwjgl.input.Mouse.isButtonDown(0);
        mc.getTextureManager()
            .bindTexture(WIDGETS);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        // WCWT: selected -> checked sprite, hover -> focus sprite, and either one sinks 1px.
        final int v = this.selected ? CHECKED_FOCUS_V : (this.hovered ? UNCHECKED_FOCUS_V : UNCHECKED_V);
        final int dy = pressed ? 2 : (this.hovered || this.selected ? 1 : 0);
        drawTexturedModalRect(this.xPosition, this.yPosition + dy, UNCHECKED_U, v, SIZE, SIZE);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    public String getMessage() {
        // 1.7.10's language files keep "\n" as two characters and AE2's tooltip splits on a real
        // newline, so the escape has to be unfolded here - the same replace the terminal's own
        // multi-line tooltips do. Without it the tooltip prints the escape itself.
        return StatCollector.translateToLocal(this.tooltipKey + (this.selected ? ".on" : ".off"))
            .replace("\\n", "\n");
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
