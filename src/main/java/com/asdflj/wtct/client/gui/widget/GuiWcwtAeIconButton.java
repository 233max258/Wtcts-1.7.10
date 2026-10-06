package com.asdflj.wtct.client.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.asdflj.wtct.Wtct;

import appeng.client.gui.widgets.ITooltip;

/**
 * AE2's own icon button: the 18x20 {@code TOOLBAR_BUTTON_BACKGROUND} plate drawn one pixel left of the
 * icon's box, a 16x16 icon blitted at its native size at {@code (x, y + 1)}, and the pair sinking one
 * pixel while hovered.
 *
 * <p>
 * These are AE2 1.21's {@code IconButton} numbers, and they are the ones the addon's card buttons use
 * too - {@code UpgradeItemButton extends IconButton} and, at full size, blits
 * {@code Icon.TOOLBAR_BUTTON_BACKGROUND} at {@code (x - 1, y)} with the card's own 16x16 texture at
 * {@code (x, y + 1)}. Drawing a button with this widget is what makes it read as part of AE2's button
 * language wherever it sits: the icon is never scaled (so it stays crisp and centred) and the plate is
 * the same one the rest of the window uses.
 *
 * <p>
 * This is the generic form - the left toolbar's own buttons are {@link GuiWcwtSidebarIconButton},
 * which is this widget plus the fact that the sidebar's surface has to grow around it.
 */
public class GuiWcwtAeIconButton extends GuiButton implements ITooltip {

    private static final ResourceLocation ICONS = new ResourceLocation(
        Wtct.MODID,
        "textures/guis/wcwt/ae2_toolbar_states.png");

    /** The plate sprite in the states sheet: 18x20, idle and hovered side by side. */
    private static final int PLATE_W = 18, PLATE_H = 20, PLATE_V = 128;
    private static final int PLATE_U_IDLE = 176, PLATE_U_HOVER = 212;
    /** AE2 pokes the plate one pixel out on the left. */
    private static final int PLATE_DX = -1;

    /** The icon: a 16x16 sprite of the button's own texture, drawn unscaled. */
    private static final int ICON_SIZE = 16;
    private static final int ICON_DY = 1;

    private final ResourceLocation iconTexture;
    private String tooltip;
    /** The icon's own place inside {@link #iconTexture}; negative = the texture is the icon. */
    private int iconU = -1, iconV = -1;

    public GuiWcwtAeIconButton(final int x, final int y, final ResourceLocation iconTexture, final String tooltip) {
        super(0, x, y, PLATE_W, PLATE_H, "");
        this.iconTexture = iconTexture;
        this.tooltip = tooltip;
    }

    /**
     * Points the icon at one {@value #ICON_SIZE}x{@value #ICON_SIZE} sprite of a shared sheet - AE2's
     * own {@code states.png}/{@code ae2_121_states.png} hold dozens of them side by side, and a button
     * that swaps its sprite (a switch, say) needs to name the one it wants.
     */
    public void setIconRegion(final int u, final int v) {
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
        this.field_146123_n = mouseX >= this.xPosition + PLATE_DX && mouseY >= this.yPosition
            && mouseX < this.xPosition + PLATE_DX + PLATE_W
            && mouseY < this.yPosition + PLATE_H;
        final int dy = this.field_146123_n ? 1 : 0;

        final TextureManager manager = mc.getTextureManager();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        manager.bindTexture(ICONS);
        drawTexturedModalRect(
            this.xPosition + PLATE_DX,
            this.yPosition + dy,
            this.field_146123_n ? PLATE_U_HOVER : PLATE_U_IDLE,
            PLATE_V,
            PLATE_W,
            PLATE_H);
        manager.bindTexture(this.iconTexture);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        // A standalone 16x16 texture is blitted with its own 0..1 UVs: drawTexturedModalRect would
        // divide by 256 and sample a sixteenth of it, which is why the card buttons had no icon at
        // all. A sprite of a shared sheet goes through drawTexturedModalRect's own 1/256 rule.
        if (this.iconU < 0) {
            blitIcon(this.xPosition, this.yPosition + ICON_DY + dy);
        } else {
            drawTexturedModalRect(
                this.xPosition,
                this.yPosition + ICON_DY + dy,
                this.iconU,
                this.iconV,
                ICON_SIZE,
                ICON_SIZE);
        }
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** Blits the bound 16x16 icon texture at its native size, using 0..1 UVs. */
    static void blitIcon(final int x, final int y) {
        final Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();
        tess.addVertexWithUV(x, y + ICON_SIZE, 0.0F, 0.0F, 1.0F);
        tess.addVertexWithUV(x + ICON_SIZE, y + ICON_SIZE, 0.0F, 1.0F, 1.0F);
        tess.addVertexWithUV(x + ICON_SIZE, y, 0.0F, 1.0F, 0.0F);
        tess.addVertexWithUV(x, y, 0.0F, 0.0F, 0.0F);
        tess.draw();
    }

    @Override
    public String getMessage() {
        return this.tooltip;
    }

    @Override
    public int xPos() {
        return this.xPosition + PLATE_DX;
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
