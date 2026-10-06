package com.asdflj.wtct.client.gui;

import java.util.function.LongConsumer;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import com.asdflj.wtct.Wtct;
import com.asdflj.wtct.client.gui.widget.GuiWcwtTextField;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * AE2 1.21's {@code SetProcessingPatternAmountScreen} as a screen of its own - the amount a pattern
 * cell holds, or the block picker's amount, edited on a panel that temporarily replaces the terminal
 * (the reference does the same: its {@code AESubScreen} is opened client-side on top of the parent
 * and hands control back when it is done, without touching the menu behind it).
 *
 * <p>
 * Every measurement is the reference's, straight out of its {@code set_stock_amount.json} and its
 * widget code, and so is the art - the 176x107 background is AE2 1.21's own {@code craft_amt.png},
 * the keys are {@code AE2Button}'s 200x20 nine-slice sprite (border 3, its label centred, lifted a
 * pixel while idle, turning 0x517497 under the cursor), the field is {@code AETextField}'s three
 * twelve-row slices out of {@code text_field.png}, and the back tab is AE2's states.png art with the
 * back arrow in it. Nothing here is redrawn by hand.
 *
 * <p>
 * The panel is centred in the window, the way a sub-screen is: title at (8,6), the edited stack in
 * its 18x18 slot at (23,53), the number at (48,55) 66x12, the two quick-set rows ("+1 +10 +100
 * +1000" over "-1 -10 -100 -1000", 20px high, turning 64-based while shift or control is held) at
 * y=30 and y=72, the Set key at (120,51) 38x20, and the back tab at (152,-5) hanging over the top
 * edge. Escape and the tab both go back to the terminal; Enter, the Set key and the tab's own
 * confirm all send the number on.
 */
@SideOnly(Side.CLIENT)
public class GuiWcwtAmount extends GuiScreen {

    // ---------------------------------------------------------------- geometry

    private static final int PANEL_W = 176;
    private static final int PANEL_H = 107;
    private static final int TITLE_X = 8;
    private static final int TITLE_Y = 6;
    private static final int ICON_X = 23;
    private static final int ICON_Y = 53;
    private static final int FIELD_X = 48;
    private static final int FIELD_Y = 55;
    private static final int FIELD_W = 66;
    private static final int FIELD_H = 12;
    private static final int STEP_X = 20;
    private static final int STEP_PLUS_Y = 30;
    private static final int STEP_MINUS_Y = 72;
    private static final int STEP_H = 20;
    /** The four keys of a quick-set row: their offsets and widths, per the reference's button row. */
    private static final int[] STEP_DX = { 0, 28, 62, 100 };
    private static final int[] STEP_W = { 22, 28, 32, 38 };
    /** {@code NumberEntryWidget}'s two step sets - the second while shift or control is held. */
    private static final int[] STEPS_1000 = { 1, 10, 100, 1000 };
    private static final int[] STEPS_64 = { 1, 16, 32, 64 };
    private static final int SET_X = 120;
    private static final int SET_Y = 51;
    private static final int SET_W = 38;
    private static final int SET_H = 20;
    private static final int TAB_X = PANEL_W - 24;
    private static final int TAB_Y = -5;
    private static final int TAB_SIZE = 20;
    /** AE2's states.png: the tab's 20x20 backdrop, its 22x22 focused variant, the 16x16 back arrow. */
    private static final int TAB_BG_U = 160;
    private static final int TAB_BG_V = 192;
    private static final int TAB_BG_FOCUS_U = 160;
    private static final int TAB_BG_FOCUS_V = 224;
    private static final int TAB_BG_FOCUS_SIZE = 22;
    private static final int TAB_ICON_U = 96;
    private static final int TAB_ICON_V = 16;
    private static final int TAB_ICON_SIZE = 16;
    private static final int TAB_ICON_DX = 2;
    private static final int TAB_ICON_DY = 1;
    /** The field art's twelve rows and the 1px caps at either end of them. */
    private static final int FIELD_ART_MID_X = 1;
    private static final int FIELD_ART_RIGHT_X = 127;
    /** The number's ceiling, as the reference clamps it. */
    private static final long MAX_AMOUNT = 999999999L;
    /**
     * The number's floor for the quick-set keys, the wheel and the up/down keys: a cell asking for nothing is not
     * a cell, and the reference's own stepper never walks a count down to zero either.
     */
    private static final long MIN_AMOUNT = 1L;
    private static final int MAX_DIGITS = 9;
    /**
     * The amount stamped on the edited stack: {@code StackSizeRenderer.renderSizeLabel}'s two-thirds
     * scale, and {@code AmountFormat.SLOT}'s four-character budget for the abbreviated number.
     */
    private static final float LABEL_SCALE = 0.666f;
    private static final int LABEL_WIDTH = 4;
    private static final char[] LABEL_SUFFIXES = "KMGTPE".toCharArray();

    // ------------------------------------------------------------------- art

    private static final String BG_TEXTURE = "guis/wcwt/craft_amt.png";
    private static final String BUTTON_TEXTURE = "guis/wcwt/amount_button.png";
    private static final String BUTTON_HOVER_TEXTURE = "guis/wcwt/amount_button_highlighted.png";
    private static final String FIELD_TEXTURE = "guis/wcwt/amount_text_field.png";
    private static final String TAB_TEXTURE = "guis/wcwt/ae2_toolbar_states.png";
    /** The button sprite is 200x20 with a three-pixel nine-slice border. */
    private static final int BUTTON_SRC_W = 200;
    private static final int BUTTON_SRC_H = 20;
    private static final int BUTTON_BORDER = 3;
    /** The reference's text colours: the palette's default text, and {@code AE2Button}'s label. */
    private static final int TITLE_COLOR = 0x413F54;
    private static final int LABEL_COLOR = 0xF2F2F2;
    private static final int LABEL_HOVER_COLOR = 0x517497;

    // ---------------------------------------------------------------- state

    private final GuiScreen parent;
    private final String title;
    /** The stack being edited, or null for the block picker, which shows its card's icon instead. */
    private final ItemStack stack;
    /** The picker's icon, used in place of {@link #stack} when there is none. */
    private final ResourceLocation icon;
    private final long initialAmount;
    /** Called with the number the player settled on; the screen closes itself right afterwards. */
    private final LongConsumer onApply;

    private GuiWcwtTextField field;
    private int panelX;
    private int panelY;

    public GuiWcwtAmount(final GuiScreen parent, final String title, final ItemStack stack, final ResourceLocation icon,
        final long amount, final LongConsumer onApply) {
        this.parent = parent;
        this.title = title;
        this.stack = stack;
        this.icon = icon;
        this.initialAmount = amount;
        this.onApply = onApply;
    }

    @Override
    public void initGui() {
        super.initGui();
        this.panelX = (this.width - PANEL_W) / 2;
        this.panelY = (this.height - PANEL_H) / 2;
        // A resize rebuilds the widgets: the field's art is anchored where it is built, so it is
        // rebuilt with whatever has been typed so far and keeps the caret.
        final boolean opened = this.field == null;
        final String text = opened ? Long.toString(this.initialAmount) : this.field.getText();
        this.field = new GuiWcwtTextField(
            this.fontRendererObj,
            this.panelX + FIELD_X,
            this.panelY + FIELD_Y,
            FIELD_W,
            FIELD_H);
        this.field.setMaxStringLength(MAX_DIGITS);
        this.field.setText(text);
        this.field.setFocused(true);
        this.field.setCursorPositionEnd();
        // The number comes up selected: the dialog is opened to type a new amount, and without the
        // selection the digits land after the old number - a cell holding 8, typed into as "64", read
        // back as 864 and that is what Enter wrote. Only on the first build, so a resize does not
        // throw the selection over what has been typed since.
        if (opened) {
            this.field.setSelectionPos(0);
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (this.field != null) {
            this.field.updateCursorCounter();
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    // ------------------------------------------------------------------ input

    @Override
    protected void keyTyped(final char character, final int key) {
        if (key == Keyboard.KEY_ESCAPE) {
            returnToParent();
            return;
        }
        if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) {
            // The reference's text field confirms on Enter, and only while the number parses.
            if (this.amount() > 0 || hasDigits()) {
                apply();
            }
            return;
        }
        if (key == Keyboard.KEY_UP) {
            bump(isShiftKeyDown() ? 8 : 1);
            return;
        }
        if (key == Keyboard.KEY_DOWN) {
            bump(isShiftKeyDown() ? -8 : -1);
            return;
        }
        if (this.field != null) {
            this.field.textboxKeyTyped(character, key);
        }
    }

    @Override
    protected void mouseClicked(final int mouseX, final int mouseY, final int mouseButton) {
        if (isOnTab(mouseX, mouseY)) {
            returnToParent();
            return;
        }
        final int[] steps = steps();
        for (int i = 0; i < steps.length; i++) {
            if (isOnStep(mouseX, mouseY, i, true)) {
                bump(steps[i]);
                return;
            }
            if (isOnStep(mouseX, mouseY, i, false)) {
                bump(-steps[i]);
                return;
            }
        }
        if (isOnSet(mouseX, mouseY)) {
            apply();
            return;
        }
        if (this.field != null) {
            // A plain screen holds absolute coordinates throughout, so the click goes straight in.
            final boolean inside = mouseX >= this.field.xPosition - 2 && mouseX < this.field.xPosition + FIELD_W
                && mouseY >= this.field.yPosition - 2
                && mouseY < this.field.yPosition + FIELD_H;
            this.field.setFocused(inside);
            if (inside) {
                this.field.mouseClicked(mouseX, mouseY, mouseButton);
            }
        }
    }

    /** The wheel nudges the number, shift taking eight at a time, as AE2's number widget does. */
    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        final int wheel = Mouse.getEventDWheel();
        if (wheel == 0) {
            return;
        }
        final int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
        final int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
        if (isOnPanel(mouseX, mouseY)) {
            bump(Integer.signum(wheel) * (isShiftKeyDown() ? 8 : 1));
        }
    }

    // -------------------------------------------------------------- behaviour

    /** Hands the terminal back its screen; the menu behind was never closed, so it is simply shown. */
    private void returnToParent() {
        this.mc.displayGuiScreen(this.parent);
    }

    private void apply() {
        if (this.onApply != null) {
            this.onApply.accept(this.amount());
        }
        returnToParent();
    }

    /**
     * Adds {@code delta} to the number in the field, never below {@link #MIN_AMOUNT} and never past the ceiling:
     * walking a cell's count down to zero with the "-" keys is not an edit anybody wants, and the dialog has no
     * way to put an empty cell back.
     */
    private void bump(final int delta) {
        setAmount(Math.max(MIN_AMOUNT, Math.min(MAX_AMOUNT, this.amount() + delta)));
    }

    private void setAmount(final long amount) {
        if (this.field == null) {
            return;
        }
        this.field.setText(Long.toString(amount));
        this.field.setCursorPositionEnd();
    }

    /** The field's text as a number: anything that is not a digit is dropped, and it is clamped. */
    private long amount() {
        if (this.field == null) {
            return 0L;
        }
        final String digits = this.field.getText()
            .replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return 0L;
        }
        try {
            return Math.min(Long.parseLong(digits), MAX_AMOUNT);
        } catch (final NumberFormatException e) {
            return 0L;
        }
    }

    /** True when the field holds a digit at all, so that a typed 0 is still worth sending. */
    private boolean hasDigits() {
        return this.field != null && !this.field.getText()
            .replaceAll("[^0-9]", "")
            .isEmpty();
    }

    private static int[] steps() {
        return isShiftKeyDown() || Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)
            || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL) ? STEPS_64 : STEPS_1000;
    }

    private boolean isOnPanel(final int mouseX, final int mouseY) {
        return mouseX >= this.panelX && mouseX < this.panelX + PANEL_W
            && mouseY >= this.panelY
            && mouseY < this.panelY + PANEL_H;
    }

    /** One of the two quick-set rows: {@code plus} picks the "+" row over the "-" one below it. */
    private boolean isOnStep(final int mouseX, final int mouseY, final int index, final boolean plus) {
        final int x = this.panelX + STEP_X + STEP_DX[index];
        final int y = this.panelY + (plus ? STEP_PLUS_Y : STEP_MINUS_Y);
        return mouseX >= x && mouseX < x + STEP_W[index] && mouseY >= y && mouseY < y + STEP_H;
    }

    private boolean isOnSet(final int mouseX, final int mouseY) {
        final int x = this.panelX + SET_X;
        final int y = this.panelY + SET_Y;
        return mouseX >= x && mouseX < x + SET_W && mouseY >= y && mouseY < y + SET_H;
    }

    private boolean isOnTab(final int mouseX, final int mouseY) {
        final int x = this.panelX + TAB_X;
        final int y = this.panelY + TAB_Y;
        return mouseX >= x && mouseX < x + TAB_SIZE && mouseY >= y && mouseY < y + TAB_SIZE;
    }

    // ---------------------------------------------------------------- drawing

    @Override
    public void drawScreen(final int mouseX, final int mouseY, final float partialTicks) {
        // A sub-screen replaces its parent, so what is behind this one is the usual dimmed backdrop.
        drawDefaultBackground();
        this.drawPanel(mouseX, mouseY);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void drawPanel(final int mouseX, final int mouseY) {
        final int left = this.panelX;
        final int top = this.panelY;
        // The reference's own background, at its JSON srcRect: frame, slot art and all. Its title has
        // no shadow, in the palette's default text colour, exactly as the reference draws it.
        blit(tex(BG_TEXTURE), left, top, 0, 0, PANEL_W, PANEL_H, PANEL_W, PANEL_H, 256, 256);
        this.fontRendererObj.drawString(this.title, left + TITLE_X, top + TITLE_Y, TITLE_COLOR, false);
        if (this.stack != null) {
            // The reference's client-side display slot: the item in the slot's 16x16 interior, its
            // count on top of it.
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            final RenderItem renderItem = RenderItem.getInstance();
            RenderHelper.enableGUIStandardItemLighting();
            renderItem.renderItemAndEffectIntoGUI(
                this.fontRendererObj,
                this.mc.getTextureManager(),
                this.stack,
                left + ICON_X,
                top + ICON_Y);
            RenderHelper.disableStandardItemLighting();
            // The reference puts the amount on the stack itself, at two-thirds scale and right-aligned
            // inside the slot - not the vanilla stack-size overlay, which is half again as large and
            // spills out of the 18x18 slot.
            drawAmountLabel(this.stack.stackSize, left + ICON_X, top + ICON_Y);
        } else if (this.icon != null) {
            // The picker's own card icon stands where the cell's item would.
            blit(this.icon, left + ICON_X, top + ICON_Y, 0, 0, 16, 16, 16, 16, 16, 16);
        }
        // The field's own art, three 12-row slices out of the reference's text_field.png: a 1px cap at
        // either end with the middle stretched between them, which is how AETextField draws it.
        final int fieldLeft = left + FIELD_X;
        final int fieldTop = top + FIELD_Y;
        blit(tex(FIELD_TEXTURE), fieldLeft, fieldTop, 0, 0, 1, FIELD_H, 1, FIELD_H, 128, 128);
        blit(
            tex(FIELD_TEXTURE),
            fieldLeft + 1,
            fieldTop,
            FIELD_ART_MID_X,
            0,
            FIELD_W - 2,
            FIELD_H,
            FIELD_W - 2,
            FIELD_H,
            128,
            128);
        blit(
            tex(FIELD_TEXTURE),
            fieldLeft + FIELD_W - 1,
            fieldTop,
            FIELD_ART_RIGHT_X,
            0,
            1,
            FIELD_H,
            1,
            FIELD_H,
            128,
            128);
        if (this.field != null) {
            this.field.drawTextBox();
        }
        // AE2's quick-set rows: "+1 +10 +100 +1000" over "-1 -10 -100 -1000", the steps turning
        // 64-based while shift or control is held.
        final int[] steps = steps();
        for (int i = 0; i < steps.length; i++) {
            final int x = left + STEP_X + STEP_DX[i];
            drawKey(x, top + STEP_PLUS_Y, STEP_W[i], STEP_H, mouseX, mouseY, "+" + steps[i]);
            drawKey(x, top + STEP_MINUS_Y, STEP_W[i], STEP_H, mouseX, mouseY, "-" + steps[i]);
        }
        drawKey(
            left + SET_X,
            top + SET_Y,
            SET_W,
            SET_H,
            mouseX,
            mouseY,
            StatCollector.translateToLocal("wtct.pattern.amount.set"));
        // The back tab, overhanging the panel's top edge: AE2's own tab art with the back arrow in it.
        // Its hovered art is 22x22 and, as in the reference, is drawn from the same corner rather than
        // centred on the 20x20 one.
        final boolean tabHover = isOnTab(mouseX, mouseY);
        final int tabSize = tabHover ? TAB_BG_FOCUS_SIZE : TAB_SIZE;
        blit(
            tex(TAB_TEXTURE),
            left + TAB_X,
            top + TAB_Y,
            tabHover ? TAB_BG_FOCUS_U : TAB_BG_U,
            tabHover ? TAB_BG_FOCUS_V : TAB_BG_V,
            tabSize,
            tabSize,
            tabSize,
            tabSize,
            256,
            256);
        blit(
            tex(TAB_TEXTURE),
            left + TAB_X + TAB_ICON_DX,
            top + TAB_Y + TAB_ICON_DY,
            TAB_ICON_U,
            TAB_ICON_V,
            TAB_ICON_SIZE,
            TAB_ICON_SIZE,
            TAB_ICON_SIZE,
            TAB_ICON_SIZE,
            256,
            256);
    }

    /**
     * One of the dialog's keys: the reference's button sprite stretched as its {@code nine_slice}
     * metadata asks - a three-pixel border that stays put around a stretched middle - with its label
     * drawn the way {@code AE2Button} draws it, centred and lifted a pixel while idle.
     */
    private void drawKey(final int x, final int y, final int w, final int h, final int mouseX, final int mouseY,
        final String label) {
        final boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        blitNineSlice(hover ? BUTTON_HOVER_TEXTURE : BUTTON_TEXTURE, x, y, w, h);
        this.fontRendererObj.drawString(
            label,
            x + (w - this.fontRendererObj.getStringWidth(label)) / 2,
            y + (h - 9) / 2 + (hover ? 1 : 0),
            hover ? LABEL_HOVER_COLOR : LABEL_COLOR,
            false);
    }

    /**
     * The amount on the edited stack, exactly as AE2 1.21's {@code StackSizeRenderer} draws it: at
     * two-thirds scale, right-aligned one pixel past the icon and sitting on the slot's floor.
     */
    private void drawAmountLabel(final long amount, final int x, final int y) {
        final String text = formatAmount(amount);
        final float inverseScale = 1.0F / LABEL_SCALE;
        final int textX = (int) ((x - 1 + 16.0F + 2.0F - this.fontRendererObj.getStringWidth(text) * LABEL_SCALE)
            * inverseScale);
        final int textY = (int) ((y - 1 + 16.0F - 5.0F * LABEL_SCALE) * inverseScale);
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT);
        GL11.glPushMatrix();
        GL11.glTranslatef(0.0F, 0.0F, 300.0F);
        GL11.glScalef(LABEL_SCALE, LABEL_SCALE, 1.0F);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        this.fontRendererObj.drawStringWithShadow(text, textX, textY, 0xFFFFFF);
        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }

    /**
     * AE2 1.21's {@code ReadableNumberConverter.format(number, 4)} - the {@code AmountFormat.SLOT}
     * budget: short numbers verbatim, long ones cut down to a suffix ("12K", "1M") that fits.
     */
    private static String formatAmount(final long amount) {
        final String numberString = Long.toString(amount);
        if (numberString.length() <= LABEL_WIDTH) {
            return numberString;
        }
        long base = amount;
        long last = amount * 1000L;
        int exponent = -1;
        String suffix = "";
        int size = numberString.length();
        while (size > LABEL_WIDTH) {
            last = base;
            base /= 1000L;
            exponent++;
            // One character more than the number itself, for the suffix.
            size = Long.toString(base)
                .length() + 1;
            suffix = String.valueOf(LABEL_SUFFIXES[exponent]);
        }
        final String withPrecision = trimmed(last / 1000.0) + suffix;
        return withPrecision.length() <= LABEL_WIDTH ? withPrecision : base + suffix;
    }

    /** The reference's {@code ".#;0.#"} decimal format: at most one decimal, rounded down, no ".0". */
    private static String trimmed(final double value) {
        final long whole = (long) Math.floor(value);
        final int decimal = (int) ((value - whole) * 10.0);
        return decimal == 0 ? Long.toString(whole) : whole + "." + decimal;
    }

    /**
     * One nine-slice out of a sheet: its border stays at its own size where the sprite's metadata says
     * it is, while the middle stretches to fill the rest - which is how the reference's buttons keep
     * their rounded frame at every width. The sheet is taken as a whole (200x20 here), so the UVs are
     * normalised against it rather than against the usual 256.
     */
    private void blitNineSlice(final String file, final int x, final int y, final int w, final int h) {
        bind(file);
        // The sprites are cut out of their sheets, so their corners are transparent. The screen's
        // dimmed backdrop turns blending off before this runs, and without it those texels - alpha 0,
        // black - land on the framebuffer as solid black blocks around every key.
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        final Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();
        for (int col = 0; col < 3; col++) {
            for (int row = 0; row < 3; row++) {
                // 0 = the leading border, 1 = the stretched middle, 2 = the trailing border.
                final int u = col == 0 ? 0 : (col == 1 ? BUTTON_BORDER : BUTTON_SRC_W - BUTTON_BORDER);
                final int v = row == 0 ? 0 : (row == 1 ? BUTTON_BORDER : BUTTON_SRC_H - BUTTON_BORDER);
                final int srcW = col == 1 ? BUTTON_SRC_W - 2 * BUTTON_BORDER : BUTTON_BORDER;
                final int srcH = row == 1 ? BUTTON_SRC_H - 2 * BUTTON_BORDER : BUTTON_BORDER;
                final int dx = x + (col == 0 ? 0 : (col == 1 ? BUTTON_BORDER : w - BUTTON_BORDER));
                final int dy = y + (row == 0 ? 0 : (row == 1 ? BUTTON_BORDER : h - BUTTON_BORDER));
                final int dw = col == 1 ? w - 2 * BUTTON_BORDER : BUTTON_BORDER;
                final int dh = row == 1 ? h - 2 * BUTTON_BORDER : BUTTON_BORDER;
                final float su = (float) u / BUTTON_SRC_W;
                final float sv = (float) v / BUTTON_SRC_H;
                final float eu = (float) (u + srcW) / BUTTON_SRC_W;
                final float ev = (float) (v + srcH) / BUTTON_SRC_H;
                tess.addVertexWithUV(dx, dy + dh, this.zLevel, su, ev);
                tess.addVertexWithUV(dx + dw, dy + dh, this.zLevel, eu, ev);
                tess.addVertexWithUV(dx + dw, dy, this.zLevel, eu, sv);
                tess.addVertexWithUV(dx, dy, this.zLevel, su, sv);
            }
        }
        tess.draw();
        GL11.glDisable(GL11.GL_BLEND);
    }

    /** One sprite out of a sheet, positioned and sized freely; the UVs are the sheet's own. */
    private void blit(final ResourceLocation texture, final int x, final int y, final int u, final int v, final int w,
        final int h, final int srcW, final int srcH, final int texW, final int texH) {
        this.mc.getTextureManager()
            .bindTexture(texture);
        // See blitNineSlice: the tab and the card icon keep their transparency only with blending on.
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        final float su = (float) u / texW;
        final float sv = (float) v / texH;
        final float eu = (float) (u + srcW) / texW;
        final float ev = (float) (v + srcH) / texH;
        final Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();
        tess.addVertexWithUV(x, y + h, this.zLevel, su, ev);
        tess.addVertexWithUV(x + w, y + h, this.zLevel, eu, ev);
        tess.addVertexWithUV(x + w, y, this.zLevel, eu, sv);
        tess.addVertexWithUV(x, y, this.zLevel, su, sv);
        tess.draw();
        GL11.glDisable(GL11.GL_BLEND);
    }

    private void bind(final String file) {
        this.mc.getTextureManager()
            .bindTexture(tex(file));
    }

    private static ResourceLocation tex(final String file) {
        return new ResourceLocation(Wtct.MODID, "textures/" + file);
    }
}
