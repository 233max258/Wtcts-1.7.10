package com.asdflj.wtct.client.gui.nethub;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.asdflj.wtct.Wtct;

/** A 20x20 icon taken from one sprite sheet, with a second row for the hovered/selected look. */
public class IconButton extends GuiButton {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Wtct.MODID, "textures/gui/nethub/widgets.png");

    private final int textureX;
    private final int textureY;

    private boolean selected;

    public IconButton(int buttonId, int x, int y, int textureX, int textureY) {
        super(buttonId, x, y, 20, 20, "");
        this.textureX = textureX;
        this.textureY = textureY;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!this.visible) return;
        mc.getTextureManager()
            .bindTexture(TEXTURE);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.field_146123_n = mouseX >= this.xPosition && mouseY >= this.yPosition
            && mouseX < this.xPosition + this.width
            && mouseY < this.yPosition + this.height;
        int v = this.textureY + (this.selected || this.field_146123_n ? 20 : 0);
        drawTexturedModalRect(this.xPosition, this.yPosition, this.textureX, v, this.width, this.height);
    }
}
