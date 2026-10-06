package com.asdflj.wtct.client.gui.nethub;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.asdflj.wtct.Wtct;

/**
 * A wide button whose skin comes with the screen rather than from vanilla's {@code widgets.png}: the sheet holds four
 * 14px rows - disabled, idle, hovered and the text field frame the create box borrows.
 */
public class MyButton extends GuiButton {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Wtct.MODID, "textures/gui/nethub/widgets.png");

    public MyButton(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText) {
        super(buttonId, x, y, widthIn, heightIn, buttonText);
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!this.visible) return;
        FontRenderer fontrenderer = mc.fontRenderer;
        mc.getTextureManager()
            .bindTexture(TEXTURE);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.field_146123_n = mouseX >= this.xPosition && mouseY >= this.yPosition
            && mouseX < this.xPosition + this.width
            && mouseY < this.yPosition + this.height;
        int i = this.getHoverState(this.field_146123_n);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        drawTexturedModalRect(this.xPosition, this.yPosition, 0, i * this.height, this.width / 2, this.height);
        drawTexturedModalRect(
            this.xPosition + this.width / 2,
            this.yPosition,
            200 - this.width / 2,
            i * this.height,
            this.width / 2,
            this.height);
        this.mouseDragged(mc, mouseX, mouseY);
        int j = 14737632;
        if (packedFGColour != 0) {
            j = packedFGColour;
        } else if (!this.enabled) {
            j = 10526880;
        } else if (this.field_146123_n) {
            j = 16777120;
        }
        float scale = 1.0F;
        if (!"zh_cn".equals(mc.gameSettings.language)) {
            scale = 0.6f;
        }
        StringRenderUtil.drawCenteredString(
            fontrenderer,
            this.displayString,
            this.xPosition + this.width / 2,
            (int) (this.yPosition + (this.height - 8 * scale) / 2),
            j,
            scale);
    }
}
