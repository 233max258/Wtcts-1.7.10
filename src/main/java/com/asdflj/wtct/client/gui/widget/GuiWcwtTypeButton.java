package com.asdflj.wtct.client.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.asdflj.wtct.Wtct;

import appeng.client.gui.widgets.TypeToggleButton;

/**
 * AE2's {@link TypeToggleButton} drawn as WCWT's header switch.
 *
 * <p>
 * The plain toggle draws nothing but the type's own item icon, which in the terminal header reads as a
 * bare item floating on the panel. WCWT's display toggles ({@code ItemDisplayButton} & co.) are 22x12
 * switches instead: AE2 1.21's {@code guis/checkbox.png} carries the switch in four variants -
 * unchecked/checked, normal/hovered - and WCWT's screen JSON anchors its three toggles at 22x12 cells
 * (left 79, 104, 129, top 4). The cell here is exactly that 22x12, so the sprite is blitted 1:1:
 * squeezing it into a differently shaped cell stretched the switch out of shape.
 */
public class GuiWcwtTypeButton extends TypeToggleButton {

    private static final ResourceLocation SWITCH = new ResourceLocation(Wtct.MODID, "textures/guis/wcwt/checkbox.png");
    /** Switch sprite in the 64x64 sheet: 22x12, unchecked row at v=28, checked row at v=40, hover +22 u. */
    private static final int SWITCH_W = 22, SWITCH_H = 12;
    private static final int SHEET = 64;
    private static final int V_OFF = 28, V_ON = 40, U_HOVER = 22;
    /** Our cell: the sprite's own 22x12, the size WCWT gives these toggles. */
    private static final int CELL_W = SWITCH_W, CELL_H = SWITCH_H;

    private boolean checked;

    public GuiWcwtTypeButton(final int x, final int y, final ResourceLocation texture, final IIcon icon,
        final String name) {
        super(x, y, texture, icon, name);
        // Click and tooltip bounds follow the cell, not AE2's 16x16 default.
        this.width = CELL_W;
        this.height = CELL_H;
    }

    @Override
    public void setEnabled(final boolean enabled) {
        super.setEnabled(enabled);
        this.checked = enabled;
    }

    @Override
    public void drawButton(final Minecraft mc, final int mouseX, final int mouseY) {
        if (!this.visible) {
            return;
        }
        final boolean hover = mouseX >= this.xPosition && mouseY >= this.yPosition
            && mouseX < this.xPosition + this.width
            && mouseY < this.yPosition + this.height;
        final int srcX = hover ? U_HOVER : 0;
        final int srcY = this.checked ? V_ON : V_OFF;

        final TextureManager manager = mc.getTextureManager();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        manager.bindTexture(SWITCH);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        final float f = 1.0F / SHEET;
        final float u0 = srcX * f;
        final float v0 = srcY * f;
        final float u1 = (srcX + SWITCH_W) * f;
        final float v1 = (srcY + SWITCH_H) * f;
        final int x0 = this.xPosition;
        final int y0 = this.yPosition;
        final Tessellator tes = Tessellator.instance;
        tes.startDrawingQuads();
        tes.addVertexWithUV(x0, y0 + CELL_H, this.zLevel, u0, v1);
        tes.addVertexWithUV(x0 + CELL_W, y0 + CELL_H, this.zLevel, u1, v1);
        tes.addVertexWithUV(x0 + CELL_W, y0, this.zLevel, u1, v0);
        tes.addVertexWithUV(x0, y0, this.zLevel, u0, v0);
        tes.draw();
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
