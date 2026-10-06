package com.asdflj.wtct.client.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.asdflj.wtct.Wtct;

import appeng.client.gui.widgets.ITooltip;

/**
 * AE2's checkbox as WTLib draws it on its settings dialogs: the 22x12 slider sprite from
 * {@code guis/checkbox.png} - the knob sits right when the switch is on, left when it is off, and the
 * hovered variant is the sprite next to it. It is the same sprite the terminal header wears
 * ({@link GuiWcwtTypeButton}), just without an icon on top.
 *
 * <p>
 * A dead switch (WTLib greys out "craft if missing" while "pick block" is off) is drawn at half
 * opacity and does not take clicks, which vanilla's own {@code enabled} flag already handles.
 */
public class GuiWcwtCheckbox extends GuiButton implements ITooltip {

    private static final ResourceLocation SWITCH = new ResourceLocation(Wtct.MODID, "textures/guis/wcwt/checkbox.png");
    private static final int SHEET = 64;
    private static final int W = 22, H = 12;
    private static final int V_OFF = 28, V_ON = 40, U_HOVER = 22;
    /** How far a switch fades out while it is disabled, as AE2 fades a dead checkbox. */
    private static final float DISABLED_ALPHA = 0.5F;

    private boolean checked;

    public GuiWcwtCheckbox(final int x, final int y) {
        super(0, x, y, W, H, "");
    }

    public void setChecked(final boolean checked) {
        this.checked = checked;
    }

    public boolean isChecked() {
        return this.checked;
    }

    @Override
    public void drawButton(final Minecraft mc, final int mouseX, final int mouseY) {
        if (!this.visible) {
            return;
        }
        final boolean hover = this.enabled && mouseX >= this.xPosition
            && mouseY >= this.yPosition
            && mouseX < this.xPosition + W
            && mouseY < this.yPosition + H;
        this.field_146123_n = hover;
        final int srcX = hover ? U_HOVER : 0;
        final int srcY = this.checked ? V_ON : V_OFF;

        mc.getTextureManager()
            .bindTexture(SWITCH);
        final float alpha = this.enabled ? 1.0F : DISABLED_ALPHA;
        GL11.glColor4f(1.0F, 1.0F, 1.0F, alpha);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        final float f = 1.0F / SHEET;
        final float u0 = srcX * f;
        final float v0 = srcY * f;
        final float u1 = (srcX + W) * f;
        final float v1 = (srcY + H) * f;
        final Tessellator tes = Tessellator.instance;
        tes.startDrawingQuads();
        tes.addVertexWithUV(this.xPosition, this.yPosition + H, this.zLevel, u0, v1);
        tes.addVertexWithUV(this.xPosition + W, this.yPosition + H, this.zLevel, u1, v1);
        tes.addVertexWithUV(this.xPosition + W, this.yPosition, this.zLevel, u1, v0);
        tes.addVertexWithUV(this.xPosition, this.yPosition, this.zLevel, u0, v0);
        tes.draw();
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** Set by the GUI so the tooltip can explain the switch. */
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
