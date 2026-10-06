package com.asdflj.wtct.client.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.asdflj.wtct.Wtct;

import appeng.client.gui.widgets.ITooltip;

/**
 * WTLib's {@code IconButton}: a 16x17 plate with a 16x16 icon on it, drawn from the WTLib icon sheet.
 *
 * <p>
 * The sprites are WTLib's own ({@code Icon.BUTTON_BACKGROUND} and friends, at (63,0) / (79,0) / (95,1)
 * of the 128x128 sheet) and so is the one-pixel quirk WTLib draws with: the plate goes to
 * {@code x - 1}, which is why the hit box reaches one pixel further left than the declared column.
 * The screen passes WTLib's JSON coordinates unchanged, and the shift is applied here.
 *
 * <p>
 * The icon is swappable, because the magnet screen's two mode buttons show WTLib's YES / NO icon for
 * whitelist / blacklist.
 */
public class GuiWcwtIconButton extends GuiButton implements ITooltip {

    private static final ResourceLocation ICONS = new ResourceLocation(
        Wtct.MODID,
        "textures/guis/wcwt/wtlib_icons.png");

    private static final int BG_U = 63;
    private static final int BG_V = 0;
    private static final int BG_FOCUS_U = 79;
    private static final int BG_FOCUS_V = 0;
    private static final int BG_HOVER_U = 95;
    private static final int BG_HOVER_V = 1;

    /** WTLib's plate: 16 wide and 17 tall, the extra row being its bottom border. */
    private static final int PLATE_W = 16;
    private static final int PLATE_H = 17;

    /** WTLib's {@code Icon.YES} / {@code Icon.NO}. */
    public static final int ICON_YES_U = 32;
    public static final int ICON_YES_V = 16;
    public static final int ICON_NO_U = 32;
    public static final int ICON_NO_V = 0;

    private int iconU;
    private int iconV;
    private String tooltip;

    public GuiWcwtIconButton(final int wtlibX, final int wtlibY, final int iconU, final int iconV,
        final String tooltip) {
        super(0, wtlibX - 1, wtlibY, PLATE_W, PLATE_H, "");
        this.iconU = iconU;
        this.iconV = iconV;
        this.tooltip = tooltip;
    }

    /** Swaps the icon - WTLib's mode buttons change theirs with the mode. */
    public void setIcon(final int u, final int v) {
        this.iconU = u;
        this.iconV = v;
    }

    public void setTooltip(final String tooltip) {
        this.tooltip = tooltip;
    }

    @Override
    public void drawButton(final Minecraft mc, final int mouseX, final int mouseY) {
        if (!this.visible) {
            return;
        }
        this.field_146123_n = mouseX >= this.xPosition && mouseY >= this.yPosition
            && mouseX < this.xPosition + PLATE_W
            && mouseY < this.yPosition + PLATE_H;

        final boolean hovered = this.field_146123_n;
        final int dy = hovered ? 1 : 0;
        final int u;
        final int v;
        final int h;
        if (hovered) {
            u = BG_HOVER_U;
            v = BG_HOVER_V;
            h = PLATE_H - 1;
        } else if (!this.enabled) {
            u = BG_FOCUS_U;
            v = BG_FOCUS_V;
            h = PLATE_H;
        } else {
            u = BG_U;
            v = BG_V;
            h = PLATE_H;
        }

        mc.getTextureManager()
            .bindTexture(ICONS);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        blit(this.xPosition, this.yPosition + dy, u, v, PLATE_W, h);
        // WTLib draws the icon onto the same spot as the plate - also one pixel left of the column.
        blit(this.xPosition, this.yPosition + dy, this.iconU, this.iconV, 16, 16);
    }

    /**
     * drawTexturedModalRect with 128-based UV normalisation: 1.7.10's own helper always divides by
     * 256, which would sample the wrong quarter of this sheet.
     */
    private static void blit(final int x, final int y, final int srcX, final int srcY, final int w, final int h) {
        final float f = 1.0F / 128.0F;
        final Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();
        tess.addVertexWithUV(x, y + h, 0.0F, srcX * f, (srcY + h) * f);
        tess.addVertexWithUV(x + w, y + h, 0.0F, (srcX + w) * f, (srcY + h) * f);
        tess.addVertexWithUV(x + w, y, 0.0F, (srcX + w) * f, srcY * f);
        tess.addVertexWithUV(x, y, 0.0F, srcX * f, srcY * f);
        tess.draw();
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
