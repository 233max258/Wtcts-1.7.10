package com.asdflj.wtct.client.gui.nethub;

import net.minecraft.client.gui.FontRenderer;

import org.lwjgl.opengl.GL11;

/**
 * Scaled text for the screens whose labels have to shrink to fit a fixed panel.
 *
 * <p>
 * 1.7.10's {@link FontRenderer#drawString} only takes integer coordinates, so the scaled position is rounded on the way
 * in - the matrix does the real scaling.
 */
public final class StringRenderUtil {

    private StringRenderUtil() {}

    public static void drawCenteredString(FontRenderer fontRendererIn, String text, int x, int y, int color,
        float scale) {
        GL11.glPushMatrix();
        GL11.glScalef(scale, scale, 1.0F);
        fontRendererIn.drawStringWithShadow(
            text,
            (int) (x / scale - (float) fontRendererIn.getStringWidth(text) / 2),
            (int) ((float) y / scale),
            color);
        GL11.glPopMatrix();
    }

    public static int drawString(FontRenderer fontRendererIn, String text, int x, int y, int color, float scale) {
        GL11.glPushMatrix();
        GL11.glScalef(scale, scale, 1.0F);
        int r = fontRendererIn.drawString(text, (int) (x / scale), (int) ((float) y / scale), color, false);
        GL11.glPopMatrix();
        return r;
    }
}
