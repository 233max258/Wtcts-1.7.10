package com.asdflj.wtct.client.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.asdflj.wtct.Wtct;

import appeng.client.gui.widgets.ITooltip;

/**
 * A small action button for the strip between the terminal's manual crafting area and its multiplier
 * row: AE2's checkbox radio as the plate with the button's own 16x16 icon on top of it, sinking a pixel
 * while hovered.
 *
 * <p>
 * The strip is bounded by the texture's two white lines (rows 135 and 149), so the 12x12 visible part
 * of the radio sprite is all that fits - AE2's {@code TOOLBAR_BUTTON_BACKGROUND} is 20 rows tall and
 * crosses both lines, which is why this is not {@link GuiWcwtAeIconButton}. The icon is still drawn at
 * its native 16x16 and centred on the plate (its artwork sits in a 10x11 box inside the sprite, so it
 * lands one pixel inside the plate on every side), which keeps it crisp instead of scaling it into the
 * plate's inner face the way WCWT's own 13x12 button does.
 */
public class GuiWcwtStripIconButton extends GuiButton implements ITooltip {

    private static final ResourceLocation WIDGETS = new ResourceLocation(
        Wtct.MODID,
        "textures/guis/wcwt/wcwt_widgets.png");

    /** AE2's checkbox radios in wcwt_widgets.png: 14x14 sprites whose visible plate is 12x12 at +1. */
    private static final int PLATE_U = 200;
    private static final int PLATE_PLAIN_V = 0;
    private static final int PLATE_FOCUS_V = 16;
    private static final int PLATE_SRC_OFFSET = 1;
    private static final int PLATE_SIZE = 12;

    /** The icon: a standalone 16x16 texture, centred on the plate and drawn unscaled. */
    private static final int ICON_SIZE = 16;
    private static final int ICON_DX = (PLATE_SIZE - ICON_SIZE) / 2;
    private static final int ICON_DY = (PLATE_SIZE - ICON_SIZE) / 2;

    /** One pixel of slack around the plate, so the small buttons are still easy to hit. */
    private static final int HIT_PAD = 1;

    private final ResourceLocation iconTexture;
    private String tooltip;

    public GuiWcwtStripIconButton(final int x, final int y, final ResourceLocation iconTexture, final String tooltip) {
        super(0, x, y, PLATE_SIZE, PLATE_SIZE, "");
        this.iconTexture = iconTexture;
        this.tooltip = tooltip;
    }

    public void setTooltip(final String tooltip) {
        this.tooltip = tooltip;
    }

    @Override
    public void drawButton(final Minecraft mc, final int mouseX, final int mouseY) {
        if (!this.visible) {
            return;
        }
        this.field_146123_n = mouseX >= this.xPosition - HIT_PAD && mouseY >= this.yPosition - HIT_PAD
            && mouseX < this.xPosition + PLATE_SIZE + HIT_PAD
            && mouseY < this.yPosition + PLATE_SIZE + HIT_PAD;
        // Press feedback: the whole control - plate and icon alike - rides the same one-pixel sink
        // while hovered, and holding the left button on it keeps it sunk, so the icon never sits
        // still while the plate dips under it.
        final boolean held = this.field_146123_n && org.lwjgl.input.Mouse.isButtonDown(0);
        final int dy = this.field_146123_n || held ? 1 : 0;

        final TextureManager manager = mc.getTextureManager();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        manager.bindTexture(WIDGETS);
        drawTexturedModalRect(
            this.xPosition,
            this.yPosition + dy,
            PLATE_U + PLATE_SRC_OFFSET,
            (this.field_146123_n || held ? PLATE_FOCUS_V : PLATE_PLAIN_V) + PLATE_SRC_OFFSET,
            PLATE_SIZE,
            PLATE_SIZE);
        manager.bindTexture(this.iconTexture);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GuiWcwtAeIconButton.blitIcon(this.xPosition + ICON_DX, this.yPosition + ICON_DY + dy);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    public String getMessage() {
        return this.tooltip;
    }

    @Override
    public int xPos() {
        return this.xPosition - HIT_PAD;
    }

    @Override
    public int yPos() {
        return this.yPosition - HIT_PAD;
    }

    @Override
    public int getWidth() {
        return PLATE_SIZE + 2 * HIT_PAD;
    }

    @Override
    public int getHeight() {
        return PLATE_SIZE + 2 * HIT_PAD;
    }

    @Override
    public boolean isVisible() {
        return this.visible;
    }
}
