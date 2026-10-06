package com.asdflj.wtct.client.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import com.asdflj.wtct.Wtct;

import appeng.client.gui.widgets.ITooltip;

/**
 * WCWT's pattern multiplier button (样板倍增器), a 17x16 plate taken from WCWT's own sprite sheet:
 * the resting plate is blitted from (192,160) and the hovered one from (224,160), with the label
 * (×2, ÷3, ... ) centred on top in white - grey while hovered. Exactly what
 * {@code PatternMultiplierButton} renders on 1.21.
 *
 * <p>
 * The swap button (⇄, main/secondary output rotation) has no usable font glyph in 1.7.10 - the
 * unicode arrow pages are not shipped - so it draws two opposing arrows with quads instead.
 */
public class GuiMultiplierButton extends GuiButton implements ITooltip {

    private static final ResourceLocation TEXTURE = new ResourceLocation(
        Wtct.MODID,
        "textures/guis/wcwt/wcwt_states.png");
    /** Plate sprites in wcwt_states.png (WCWT's own button backgrounds). */
    private static final int BG_NORMAL_U = 192;
    private static final int BG_HOVER_U = 224;
    private static final int BG_V = 160;
    private static final int COLOR_TEXT = 0xFFFFFF;
    private static final int COLOR_HOVER = 0xA0A0A0;
    private static final int COLOR_GLYPH = 0xF2F2F2;

    /** Label shown on the plate; {@code null} on the swap button, which draws arrows instead. */
    private final String label;
    private final boolean swap;
    /** Lang key of the tooltip (keys must stay ASCII, so they cannot be derived from the label). */
    private final String tooltipKey;

    /** A normal multiplier/divisor button (label such as "×2" or "÷3"). */
    public GuiMultiplierButton(final int x, final int y, final String label, final String tooltipKey) {
        super(0, x, y, 17, 16, label);
        this.label = label;
        this.swap = false;
        this.tooltipKey = tooltipKey;
    }

    /** The swap button (⇄): rotates the processing outputs between main and secondary. */
    public static GuiMultiplierButton swap(final int x, final int y, final String tooltipKey) {
        return new GuiMultiplierButton(x, y, tooltipKey);
    }

    private GuiMultiplierButton(final int x, final int y, final String tooltipKey) {
        super(0, x, y, 17, 16, "");
        this.label = null;
        this.swap = true;
        this.tooltipKey = tooltipKey;
    }

    @Override
    public void drawButton(final Minecraft mc, final int mouseX, final int mouseY) {
        if (!this.visible) {
            return;
        }
        this.field_146123_n = mouseX >= this.xPosition && mouseY >= this.yPosition
            && mouseX < this.xPosition + this.width
            && mouseY < this.yPosition + this.height;
        final boolean hovered = this.field_146123_n;
        // Holding the button down sinks the plate (and its label) two pixels against the hover's one,
        // so a click has a press of its own instead of looking like the hover that precedes it.
        final boolean pressed = hovered && org.lwjgl.input.Mouse.isButtonDown(0);
        final int dy = pressed ? 2 : (hovered ? 1 : 0);
        mc.getTextureManager()
            .bindTexture(TEXTURE);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        drawTexturedModalRect(
            this.xPosition,
            this.yPosition + dy,
            hovered ? BG_HOVER_U : BG_NORMAL_U,
            BG_V,
            this.width,
            this.height);
        if (this.swap) {
            drawSwapGlyph(hovered ? COLOR_HOVER : COLOR_GLYPH, dy);
        } else {
            final int color = hovered ? COLOR_HOVER : COLOR_TEXT;
            mc.fontRenderer.drawStringWithShadow(
                this.displayString,
                this.xPosition + (this.width - mc.fontRenderer.getStringWidth(this.displayString)) / 2,
                this.yPosition + (this.height - 8) / 2 + dy,
                color);
        }
    }

    /** Two opposing arrows (top one pointing right, bottom one pointing left), like ⇄. */
    private void drawSwapGlyph(final int rgb, final int dy) {
        final float r = (rgb >> 16 & 0xFF) / 255.0F;
        final float g = (rgb >> 8 & 0xFF) / 255.0F;
        final float b = (rgb & 0xFF) / 255.0F;
        final int x = this.xPosition;
        final int y = this.yPosition + dy;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(r, g, b, 1.0F);
        GL11.glTranslatef(0.0F, 0.0F, 200.0F);
        final net.minecraft.client.renderer.Tessellator tess = net.minecraft.client.renderer.Tessellator.instance;
        tess.startDrawingQuads();
        // Top shaft, arrow head on the right.
        quad(tess, x + 4, y + 5, x + 10, y + 7);
        quad(tess, x + 8, y + 3, x + 10, y + 9);
        quad(tess, x + 10, y + 4, x + 12, y + 8);
        quad(tess, x + 12, y + 5, x + 13, y + 7);
        // Bottom shaft, arrow head on the left.
        quad(tess, x + 7, y + 10, x + 13, y + 12);
        quad(tess, x + 7, y + 8, x + 9, y + 14);
        quad(tess, x + 5, y + 9, x + 7, y + 13);
        quad(tess, x + 4, y + 10, x + 5, y + 12);
        tess.draw();
        GL11.glTranslatef(0.0F, 0.0F, -200.0F);
        GL11.glPopAttrib();
    }

    private static void quad(final net.minecraft.client.renderer.Tessellator tess, final float x0, final float y0,
        final float x1, final float y1) {
        tess.addVertex(x0, y1, 0.0D);
        tess.addVertex(x1, y1, 0.0D);
        tess.addVertex(x1, y0, 0.0D);
        tess.addVertex(x0, y0, 0.0D);
    }

    @Override
    public String getMessage() {
        // WCWT's tooltip is only the symbol; the second line spells out what the button does, which
        // "=1" in particular needs - it reduces the pattern to its smallest ratio, it does not set
        // every amount to one.
        return StatCollector.translateToLocal(this.tooltipKey) + "\n"
            + StatCollector.translateToLocal(this.tooltipKey + ".desc");
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
