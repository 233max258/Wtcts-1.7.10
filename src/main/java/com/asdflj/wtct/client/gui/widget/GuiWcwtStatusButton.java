package com.asdflj.wtct.client.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import com.asdflj.wtct.Wtct;

import appeng.client.gui.widgets.ITooltip;

/**
 * WCWT's crafting-status button: a 20x20 cell at the terminal's top-right, drawn entirely from one
 * sprite - plate and hammer are part of the image, there is no vanilla button background. The sprite
 * lives in its own sheet because {@code wcwt_states.png} has no free 20x20 area.
 */
public class GuiWcwtStatusButton extends GuiButton implements ITooltip {

    private static final ResourceLocation TEXTURE = new ResourceLocation(
        Wtct.MODID,
        "textures/guis/wcwt/wcwt_status_icon.png");
    private static final int HOVER = 0xFFFFFFFF;

    private final String title;
    private final String desc;

    public GuiWcwtStatusButton(final int x, final int y, final int size, final String titleKey, final String descKey) {
        super(0, x, y, size, size, "");
        this.title = StatCollector.translateToLocal(titleKey);
        this.desc = StatCollector.translateToLocal(descKey);
    }

    @Override
    public void drawButton(final Minecraft mc, final int mouseX, final int mouseY) {
        if (!this.visible) {
            return;
        }
        this.field_146123_n = mouseX >= this.xPosition && mouseY >= this.yPosition
            && mouseX < this.xPosition + this.width
            && mouseY < this.yPosition + this.height;
        mc.getTextureManager()
            .bindTexture(TEXTURE);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        drawTexturedModalRect(this.xPosition, this.yPosition, 0, 0, this.width, this.height);
        if (this.field_146123_n) {
            // Opaque 1px frame: drawRect does not blend, so a translucent overlay would not work.
            drawRect(this.xPosition, this.yPosition, this.xPosition + this.width, this.yPosition + 1, HOVER);
            drawRect(
                this.xPosition,
                this.yPosition + this.height - 1,
                this.xPosition + this.width,
                this.yPosition + this.height,
                HOVER);
            drawRect(this.xPosition, this.yPosition, this.xPosition + 1, this.yPosition + this.height, HOVER);
            drawRect(
                this.xPosition + this.width - 1,
                this.yPosition,
                this.xPosition + this.width,
                this.yPosition + this.height,
                HOVER);
        }
    }

    @Override
    public String getMessage() {
        return this.title + "\n" + this.desc;
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
