package com.asdflj.wtct.client.gui.nethub;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.asdflj.wtct.Wtct;

/**
 * The padlock that switches a network between private and public.
 *
 * <p>
 * 1.8+ ships this as {@code GuiLockIconButton}; 1.7.10 does not, so the three states and their hovered variants are
 * painted straight off the sheet here.
 */
public class MyLockIconButton extends GuiButton {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Wtct.MODID, "textures/gui/nethub/widgets.png");

    private boolean locked;

    public MyLockIconButton(int buttonId, int x, int y) {
        super(buttonId, x, y, 20, 20, "");
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!this.visible) return;
        mc.getTextureManager()
            .bindTexture(TEXTURE);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        boolean hovered = mouseX >= this.xPosition && mouseY >= this.yPosition
            && mouseX < this.xPosition + this.width
            && mouseY < this.yPosition + this.height;
        Icon icon;
        if (this.locked) {
            if (!this.enabled) {
                icon = Icon.LOCKED_DISABLED;
            } else if (hovered) {
                icon = Icon.LOCKED_HOVER;
            } else {
                icon = Icon.LOCKED;
            }
        } else if (!this.enabled) {
            icon = Icon.UNLOCKED_DISABLED;
        } else if (hovered) {
            icon = Icon.UNLOCKED_HOVER;
        } else {
            icon = Icon.UNLOCKED;
        }
        drawTexturedModalRect(this.xPosition, this.yPosition, icon.getX(), icon.getY(), this.width, this.height);
    }

    private enum Icon {

        LOCKED(0, 146),
        LOCKED_HOVER(0, 166),
        LOCKED_DISABLED(0, 186),
        UNLOCKED(20, 146),
        UNLOCKED_HOVER(20, 166),
        UNLOCKED_DISABLED(20, 186);

        private final int x;
        private final int y;

        Icon(int xIn, int yIn) {
            this.x = xIn;
            this.y = yIn;
        }

        public int getX() {
            return this.x;
        }

        public int getY() {
            return this.y;
        }
    }
}
