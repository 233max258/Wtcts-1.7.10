package com.asdflj.wtct.client.gui.nethub;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.asdflj.wtct.Wtct;

/**
 * The network-name box that replaces the create button while a name is being typed. It draws the sheet's fourth row as
 * its frame, which is why the field itself is inset by three by two.
 */
public class MyTextField extends GuiTextField {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Wtct.MODID, "textures/gui/nethub/widgets.png");

    private final Minecraft mc;

    public MyTextField(Minecraft mc, FontRenderer fontrendererObj, int x, int y, int width, int height) {
        super(fontrendererObj, x + 3, y + 2, width, height);
        this.mc = mc;
    }

    @Override
    public void drawTextBox() {
        if (this.getVisible()) {
            this.mc.getTextureManager()
                .bindTexture(TEXTURE);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            drawTexturedModalRect(
                this.xPosition - 3,
                this.yPosition - 2,
                0,
                3 * this.height,
                this.width / 2,
                this.height);
            drawTexturedModalRect(
                (this.xPosition + this.width / 2) - 3,
                this.yPosition - 2,
                200 - this.width / 2,
                3 * this.height,
                this.width / 2,
                this.height);
        }
        super.drawTextBox();
    }
}
