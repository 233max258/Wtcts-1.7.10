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
 * The swap button (⇄, main/secondary output rotation) draws the ⇄ character itself (U+21C4): the
 * shipped unicode font pages do carry it.
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
    /** The swap arrows on the hovered (light) plate - the frame's dark slate, so they stay readable. */
    private static final int COLOR_GLYPH_ON_HOVER = 0x413F54;

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
        // The plate is a blit, so it needs the texture unit on, blending on and no tint - and it cannot
        // assume them. The swap button draws its arrows with the texture disabled, and whatever it leaves
        // behind lands on the *next* button: the plate is then written straight from a transparent part of
        // the sheet without blending, which comes out as a solid white square (the "×2 turned all white"
        // report). Asking for the state here makes every button independent of its neighbours.
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager()
            .bindTexture(TEXTURE);
        drawTexturedModalRect(
            this.xPosition,
            this.yPosition + dy,
            hovered ? BG_HOVER_U : BG_NORMAL_U,
            BG_V,
            this.width,
            this.height);
        if (this.swap) {
            // The swap glyph is the ⇄ character itself (U+21C4): 1.7.10 ships the unicode font pages, and
            // page 0x21 carries the same pixel-art double arrow the reference mod shows - the first
            // attempt here hand-drew the arrows from quads and came out a smudge. Hovering swaps the
            // plate for the light-blue one, so the glyph turns dark on it (the labels go grey the same
            // way).
            final String glyph = "\u21C4";
            final int color = hovered ? COLOR_GLYPH_ON_HOVER : COLOR_GLYPH;
            mc.fontRenderer.drawStringWithShadow(
                glyph,
                this.xPosition + (this.width - mc.fontRenderer.getStringWidth(glyph)) / 2,
                this.yPosition + (this.height - 8) / 2 + dy,
                color);
        } else {
            final int color = hovered ? COLOR_HOVER : COLOR_TEXT;
            mc.fontRenderer.drawStringWithShadow(
                this.displayString,
                this.xPosition + (this.width - mc.fontRenderer.getStringWidth(this.displayString)) / 2,
                this.yPosition + (this.height - 8) / 2 + dy,
                color);
        }
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
