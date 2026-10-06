package com.asdflj.wtct.client.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import com.asdflj.wtct.Wtct;

import appeng.client.gui.widgets.ITooltip;

/**
 * A WCWT/AE2 1.21-style state button: a small sprite blitted from our copy of the AE2 1.21
 * {@code states.png} icons ({@code wcwt_states.png}), with the toggle state expressed by two
 * different sprites - no vanilla button background, exactly like AE2 1.21's small icons.
 *
 * <p>
 * The sprite coordinates are the sheet positions of AE2 1.21's own small icons, which were copied
 * over 1:1: S_CLEAR at (224,200), S_SUBSTITUTION_ENABLED/DISABLED at (224/232,208) - plus WCWT's
 * merge pair at (0/16,16). Implementing AE2's {@link ITooltip} lets {@code AEBaseGui} draw the
 * title + description tooltip without extra plumbing.
 */
public class GuiWcwtStateButton extends GuiButton implements ITooltip {

    private static final ResourceLocation TEXTURE = new ResourceLocation(
        Wtct.MODID,
        "textures/guis/wcwt/wcwt_states.png");
    /** AE2 1.21's widget sprites: TOOLBAR_BUTTON_BACKGROUND 18x20 at (0,0). */
    private static final ResourceLocation WIDGETS_TEXTURE = new ResourceLocation(
        Wtct.MODID,
        "textures/guis/wcwt/wcwt_widgets.png");
    /** wcwt_states.png is 256x256, which is also what {@code drawTexturedModalRect} divides by. */
    private static final int SHEET_SIZE = 256;
    private static final int HOVER = 0xFFFFFFFF;

    private final int size;
    /** Sprite for the "on" state; for single-state buttons it is the only sprite. */
    private final int uOn;
    private final int vOn;
    /** Sprite for the "off" state; equal to the on sprite for single-state buttons. */
    private final int uOff;
    private final int vOff;
    private final String titleOn;
    private final String descOn;
    private final String titleOff;
    private final String descOff;
    /** True draws AE2 1.21's dark toolbar backdrop (18x20) behind the small icon. */
    private final boolean toolbarBg;
    private boolean toggled;

    /** A toggle button that swaps between two sprites and two tooltip texts. */
    public GuiWcwtStateButton(final int x, final int y, final int uOn, final int vOn, final int uOff, final int vOff,
        final String titleOn, final String descOn, final String titleOff, final String descOff) {
        this(x, y, 8, false, uOn, vOn, uOff, vOff, titleOn, descOn, titleOff, descOff);
    }

    /** A single-state button (fixed sprite and tooltip). */
    public GuiWcwtStateButton(final int x, final int y, final int size, final int u, final int v, final String titleKey,
        final String descKey) {
        this(x, y, size, false, u, v, u, v, titleKey, descKey, titleKey, descKey);
    }

    /**
     * Single-state button with AE2 1.21's toolbar backdrop - the look of its half-size ActionButtons:
     * the 18x20 dark sprite is blitted at the icon's position and pokes out to the right and below.
     */
    public GuiWcwtStateButton(final int x, final int y, final int size, final boolean toolbarBg, final int u,
        final int v, final String titleKey, final String descKey) {
        this(x, y, size, toolbarBg, u, v, u, v, titleKey, descKey, titleKey, descKey);
    }

    /** A toggle button that carries the toolbar backdrop behind both state sprites. */
    public GuiWcwtStateButton(final int x, final int y, final boolean toolbarBg, final int uOn, final int vOn,
        final int uOff, final int vOff, final String titleOn, final String descOn, final String titleOff,
        final String descOff) {
        this(x, y, 8, toolbarBg, uOn, vOn, uOff, vOff, titleOn, descOn, titleOff, descOff);
    }

    private GuiWcwtStateButton(final int x, final int y, final int size, final boolean toolbarBg, final int uOn,
        final int vOn, final int uOff, final int vOff, final String titleOn, final String descOn, final String titleOff,
        final String descOff) {
        super(0, x, y, size, size, "");
        this.size = size;
        this.toolbarBg = toolbarBg;
        this.uOn = uOn;
        this.vOn = vOn;
        this.uOff = uOff;
        this.vOff = vOff;
        this.titleOn = titleOn;
        this.descOn = descOn;
        this.titleOff = titleOff;
        this.descOff = descOff;
    }

    public void setToggled(final boolean toggled) {
        this.toggled = toggled;
    }

    public boolean isToggled() {
        return this.toggled;
    }

    @Override
    public void drawButton(final Minecraft mc, final int mouseX, final int mouseY) {
        if (!this.visible) {
            return;
        }
        this.field_146123_n = mouseX >= this.xPosition && mouseY >= this.yPosition
            && mouseX < this.xPosition + this.width
            && mouseY < this.yPosition + this.height;
        if (this.toolbarBg) {
            // AE2 1.21's half-size IconButton: full 18x20 backdrop at the icon's position.
            mc.getTextureManager()
                .bindTexture(WIDGETS_TEXTURE);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            drawTexturedModalRect(this.xPosition, this.yPosition, 0, 0, 18, 20);
        }
        mc.getTextureManager()
            .bindTexture(TEXTURE);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        // drawTexturedModalRect normalises UVs against 256, which is the sheet's real size.
        drawTexturedModalRect(
            this.xPosition,
            this.yPosition,
            this.toggled ? this.uOn : this.uOff,
            this.toggled ? this.vOn : this.vOff,
            this.size,
            this.size);
        if (this.field_146123_n) {
            // Opaque 1px frame: drawRect does not blend, so a translucent overlay would not work.
            drawRect(this.xPosition, this.yPosition, this.xPosition + this.size, this.yPosition + 1, HOVER);
            drawRect(
                this.xPosition,
                this.yPosition + this.size - 1,
                this.xPosition + this.size,
                this.yPosition + this.size,
                HOVER);
            drawRect(this.xPosition, this.yPosition, this.xPosition + 1, this.yPosition + this.size, HOVER);
            drawRect(
                this.xPosition + this.size - 1,
                this.yPosition,
                this.xPosition + this.size,
                this.yPosition + this.size,
                HOVER);
        }
    }

    @Override
    public String getMessage() {
        if (this.toggled) {
            return StatCollector.translateToLocal(this.titleOn) + "\n" + StatCollector.translateToLocal(this.descOn);
        }
        return StatCollector.translateToLocal(this.titleOff) + "\n" + StatCollector.translateToLocal(this.descOff);
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
