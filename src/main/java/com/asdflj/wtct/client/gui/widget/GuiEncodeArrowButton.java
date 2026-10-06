package com.asdflj.wtct.client.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import com.asdflj.wtct.Wtct;

import appeng.client.gui.widgets.ITooltip;

/**
 * The "encode pattern" button of the comprehensive work terminal, drawn exactly like AE2 1.21's
 * {@code ActionButton(ActionItems.ENCODE)} (WCWT's EncodePatternButton): the dark
 * {@code TOOLBAR_BUTTON_BACKGROUND} sprite (18x20) sits one pixel up-left of the
 * {@code WHITE_ARROW_DOWN} icon (16x16), so the backdrop pokes out on the top and left edges.
 *
 * <p>
 * Both sprites live in {@code wcwt_widgets.png}, copied 1:1 from AE2 1.21.1's {@code states.png}.
 * Implementing {@link ITooltip} lets {@code AEBaseGui} draw the tooltip.
 */
public class GuiEncodeArrowButton extends GuiButton implements ITooltip {

    private static final ResourceLocation WIDGETS = new ResourceLocation(
        Wtct.MODID,
        "textures/guis/wcwt/wcwt_widgets.png");

    public GuiEncodeArrowButton(int x, int y) {
        super(0, x, y, 16, 16, "");
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!this.visible) {
            return;
        }
        final boolean hovered = mouseX >= this.xPosition && mouseY >= this.yPosition
            && mouseX < this.xPosition + this.width
            && mouseY < this.yPosition + this.height;
        mc.getTextureManager()
            .bindTexture(WIDGETS);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        // AE2 1.21 full-size IconButton: backdrop 18x20 at (x-1, y+yOffset), icon 16x16 at
        // (x, y+1+yOffset). Hovering swaps in the hover backdrop and pushes everything down one
        // pixel, which is the "sink" the user expects when the button is pressed.
        final int yOffset = hovered ? 1 : 0;
        drawTexturedModalRect(this.xPosition - 1, this.yPosition + yOffset, hovered ? 100 : 0, 0, 18, 20);
        if (!this.enabled) {
            GL11.glColor4f(0.5F, 0.5F, 0.5F, 1.0F);
        }
        drawTexturedModalRect(this.xPosition, this.yPosition + 1 + yOffset, 20, 0, 16, 16);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    public String getMessage() {
        return StatCollector.translateToLocal("wtct.gui.encode.title") + "\n"
            + StatCollector.translateToLocal("wtct.gui.encode.desc");
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
