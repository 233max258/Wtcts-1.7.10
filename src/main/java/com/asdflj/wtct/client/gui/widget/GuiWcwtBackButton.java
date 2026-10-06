package com.asdflj.wtct.client.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import appeng.client.gui.widgets.ITooltip;

/**
 * AE2 1.21's back tab: the 20x20 tab plate with its 16x16 back arrow inside it, and the 22x22 hovered
 * plate drawn from the same corner instead of centred - the sprite pair and offsets are the ones the
 * wireless terminal's own dialogs close with ({@code GuiWcwtAmount}, {@code GuiWcwtSettings}), taken
 * from AE2's {@code states.png}. Every screen of this mod that goes "back" uses it, so the gesture
 * looks the same everywhere rather than being a text button on some screens and a tab on others.
 *
 * <p>
 * The button's box is the 20x20 plate; clicking anywhere on it (or the two extra hovered pixels) is
 * the back action the owning screen reacts to by id.
 */
public class GuiWcwtBackButton extends GuiButton implements ITooltip {

    private static final ResourceLocation TEXTURE = new ResourceLocation(
        "wtct",
        "textures/guis/wcwt/ae2_toolbar_states.png");
    /** AE2's states.png: the idle 20x20 tab, the 22x22 focused one, the 16x16 arrow inside it. */
    private static final int TAB_U = 160;
    private static final int TAB_V = 192;
    private static final int TAB_SIZE = 20;
    private static final int FOCUS_U = 160;
    private static final int FOCUS_V = 224;
    private static final int FOCUS_SIZE = 22;
    private static final int ICON_U = 96;
    private static final int ICON_V = 16;
    private static final int ICON_SIZE = 16;
    /** Where the arrow sits inside the tab, AE2's own offsets. */
    private static final int ICON_DX = 2;
    private static final int ICON_DY = 1;

    public GuiWcwtBackButton(final int id, final int xPosition, final int yPosition) {
        super(id, xPosition, yPosition, TAB_SIZE, TAB_SIZE, StatCollector.translateToLocal("gui.back"));
    }

    /** The label WCWT's own back tab carries; the arrow alone says nothing on hover. */
    @Override
    public String getMessage() {
        return StatCollector.translateToLocal("gui.back");
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

    @Override
    public void drawButton(final Minecraft mc, final int mouseX, final int mouseY) {
        if (!this.visible) {
            return;
        }
        // The hovered plate is two pixels larger; the hit area stays the tab's own 20x20.
        this.field_146123_n = mouseX >= this.xPosition && mouseX < this.xPosition + this.width
            && mouseY >= this.yPosition
            && mouseY < this.yPosition + this.height;
        final int size = this.field_146123_n ? FOCUS_SIZE : TAB_SIZE;
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager()
            .bindTexture(TEXTURE);
        // The atlas is 256x256, which is exactly the divisor drawTexturedModalRect uses.
        this.drawTexturedModalRect(
            this.xPosition,
            this.yPosition,
            this.field_146123_n ? FOCUS_U : TAB_U,
            this.field_146123_n ? FOCUS_V : TAB_V,
            size,
            size);
        this.drawTexturedModalRect(
            this.xPosition + ICON_DX,
            this.yPosition + ICON_DY,
            ICON_U,
            ICON_V,
            ICON_SIZE,
            ICON_SIZE);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
