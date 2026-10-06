package com.asdflj.wtct.client.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.asdflj.wtct.Wtct;

import appeng.client.gui.widgets.ITooltip;

/**
 * AE2 1.21's {@code TabButton}: a 22x22 tab whose backdrop sprite comes from a per-style trio in the
 * states sheet - plain, {@code selected} and the focused variant - with the 16x16 icon nested inside it.
 *
 * <p>
 * 1.21's focus is keyboard focus; the mouse-side equivalent here is the hover. The terminal's pattern
 * mode tabs are {@code HORIZONTAL} (both on screen, the active one {@code selected}), the extended
 * storage bus's priority tab is the plain {@code BOX}.
 */
public class GuiWcwtTabButton extends GuiButton implements ITooltip {

    /** AE2 1.21's pristine icon sheet. */
    private static final ResourceLocation ICONS = new ResourceLocation(
        Wtct.MODID,
        "textures/guis/wcwt/ae2_121_states.png");

    /** {@code Icon.TAB_CRAFTING} (0,32) and {@code Icon.TAB_PROCESSING} (16,32) share their V. */
    public static final int CRAFT_ICON_U = 0;
    public static final int PROCESS_ICON_U = 16;
    private static final int MODE_ICON_V = 32;

    /** {@code Icon.TAB_BUTTON_BACKGROUND}: 20x20 at (160,192), focus variant 20x20 at (160,224). */
    private static final int BOX_U = 160, BOX_V = 192, BOX_W = 20, BOX_H = 20;
    /**
     * The focus backdrop is the same 20x20 plate at (160,224), not the 22x22 the 1.21 sheet advertises:
     * running the blit out to 22 samples the two transparent columns right of the plate, which land on
     * the framebuffer as black once blending is off.
     */
    private static final int BOX_FOCUS_U = 160, BOX_FOCUS_V = 224, BOX_FOCUS_SIZE = 20;
    /** {@code Icon.HORIZONTAL_TAB}: 22x22 at (128,128), selected at (128,150), focus at (150,128). */
    private static final int HORIZONTAL_U = 128, HORIZONTAL_V = 128, TAB_SIZE = 22;
    private static final int HORIZONTAL_SELECTED_V = 150;
    private static final int HORIZONTAL_FOCUS_U = 150;

    private static final int ICON_SIZE = 16;

    /** The styles 1.21's TabButton ships; each names its own backdrop sprite trio. */
    public enum Style {
        BOX,
        HORIZONTAL
    }

    private Style style = Style.BOX;
    private boolean selected;

    private final int iconU;
    private final int iconV;
    private final String tooltip;

    /** A tab with an explicitly placed icon, in {@link Style#BOX} - 1.21's own default. */
    public GuiWcwtTabButton(final int x, final int y, final int iconU, final int iconV, final String tooltip) {
        super(0, x, y, TAB_SIZE, TAB_SIZE, "");
        this.iconU = iconU;
        this.iconV = iconV;
        this.tooltip = tooltip;
    }

    /** A pattern mode tab: the icon rides {@code Icon.TAB_CRAFTING}/{@code TAB_PROCESSING}'s shared V. */
    public GuiWcwtTabButton(final int x, final int y, final int iconU, final String tooltip) {
        this(x, y, iconU, MODE_ICON_V, tooltip);
        this.style = Style.HORIZONTAL;
    }

    public void setSelected(final boolean selected) {
        this.selected = selected;
    }

    @Override
    public void drawButton(final Minecraft mc, final int mouseX, final int mouseY) {
        if (!this.visible) {
            return;
        }
        this.field_146123_n = mouseX >= this.xPosition && mouseY >= this.yPosition
            && mouseX < this.xPosition + this.width
            && mouseY < this.yPosition + this.height;
        final TextureManager manager = mc.getTextureManager();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        manager.bindTexture(ICONS);
        // The plates carry transparent corners and margins, so they have to be blended onto whatever is
        // behind them; without this the transparent texels land on the framebuffer as opaque black.
        final boolean blendWasOn = GL11.glIsEnabled(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        // 1.21's TabButton: focus wins over selected for the HORIZONTAL backdrop, and the icon nests
        // at the style's own offset - (3,2) horizontal, (2,1) box - per its extractContents.
        if (this.style == Style.HORIZONTAL) {
            int u = HORIZONTAL_U, v = HORIZONTAL_V;
            if (this.field_146123_n) {
                u = HORIZONTAL_FOCUS_U;
            } else if (this.selected) {
                v = HORIZONTAL_SELECTED_V;
            }
            drawTexturedModalRect(this.xPosition, this.yPosition, u, v, TAB_SIZE, TAB_SIZE);
            drawTexturedModalRect(this.xPosition + 3, this.yPosition + 2, this.iconU, this.iconV, ICON_SIZE, ICON_SIZE);
        } else {
            if (this.field_146123_n) {
                drawTexturedModalRect(
                    this.xPosition,
                    this.yPosition,
                    BOX_FOCUS_U,
                    BOX_FOCUS_V,
                    BOX_FOCUS_SIZE,
                    BOX_FOCUS_SIZE);
            } else {
                drawTexturedModalRect(this.xPosition, this.yPosition, BOX_U, BOX_V, BOX_W, BOX_H);
            }
            drawTexturedModalRect(this.xPosition + 2, this.yPosition + 1, this.iconU, this.iconV, ICON_SIZE, ICON_SIZE);
        }
        if (!blendWasOn) {
            GL11.glDisable(GL11.GL_BLEND);
        }
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
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
