package com.asdflj.wtct.client.gui.widget;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;

/**
 * WCWT's pattern-management text fields ({@code 供应器搜索} / {@code 映射编辑}), drawn the way WCWT
 * 1.3.9 draws them through AE2's own {@code AETextField}.
 *
 * <p>
 * Both fields are borderless: the recessed box comes from the screen's background art, which is
 * byte-for-byte WCWT's own texture - and the art at (230,181) and (230,192) is twelve rows of
 * {@code guis/text_field.png}'s idle variant (1px 0xF2F2F2 edge, 2px 0x696D88 liner, 8px
 * 0x9A9FB4 interior, 1px edge). So this class only paints the text, and every constant below is
 * taken from the reference rather than guessed:
 *
 * <ul>
 * <li><b>2px in on both axes.</b> AE2's {@code AETextField} builds its inner {@code EditBox} at
 * {@code (x + PADDING, y + PADDING)} with {@code PADDING = 2}, and a borderless {@code EditBox}
 * draws its text at that position verbatim. At 3 the nine-pixel line ends on the box's bottom
 * border and the field reads as though the text were falling out of it.
 * <li><b>Caret and text 0xF2F2F2, placeholder 0xDEDFE3.</b> Those are AE2's
 * {@code PaletteColor.TEXTFIELD_TEXT} and {@code PaletteColor.TEXTFIELD_PLACEHOLDER} defaults
 * ({@code assets/ae2/screens/common/palette.json}); WCWT's own palette only overrides the two
 * general text colours, so the fields inherit these.
 * <li><b>Focusing changes nothing.</b> AE2 ships three variants of {@code text_field.png} - idle,
 * disabled and focused - and its idle and focused variants are pixel-identical, so WCWT darkens
 * nothing while a field holds the caret. The caret and the typed shadowed text are the feedback.
 * <li><b>The placeholder is flat</b> (WCWT draws it with {@code shadow = false}) and shows only
 * while the field is empty and unfocused, at the very position the typed text would occupy.
 * </ul>
 */
public class GuiWcwtTextField extends GuiTextField {

    /** AE2's {@code AETextField.PADDING}, applied to both axes by its constructor. */
    private static final int PADDING = 2;
    /** AE2's {@code PaletteColor.TEXTFIELD_TEXT} (#f2f2f2). */
    private static final int TEXT_COLOR = 0xFFF2F2F2;
    /** AE2's {@code PaletteColor.TEXTFIELD_PLACEHOLDER} (#dedfe3). */
    private static final int PLACEHOLDER_COLOR = 0xFFDEDFE3;

    private final FontRenderer font;
    /** Left edge / top edge of the field, kept so the text can be re-derived every frame. */
    private final int fieldX;
    private final int fieldY;
    private String placeholder = "";

    public GuiWcwtTextField(final FontRenderer font, final int x, final int y, final int width, final int height) {
        super(font, x, y, width, height);
        this.font = font;
        this.fieldX = x;
        this.fieldY = y;
        this.setEnableBackgroundDrawing(false);
        this.setMaxStringLength(64);
        this.setTextColor(TEXT_COLOR);
    }

    public void setPlaceholder(final String placeholder) {
        this.placeholder = placeholder == null ? "" : placeholder;
    }

    @Override
    public void drawTextBox() {
        if (!this.getVisible()) {
            return;
        }
        // A borderless GuiTextField draws the text, the caret and the selection at
        // xPosition/yPosition, so AE2's padding goes there. The caret then spans y+1 .. y+11,
        // exactly the box's interior, instead of poking one pixel through the bottom edge.
        this.xPosition = this.fieldX + PADDING;
        this.yPosition = this.fieldY + PADDING;
        super.drawTextBox();
        this.xPosition = this.fieldX;
        this.yPosition = this.fieldY;
        if (this.placeholder.isEmpty() || this.isFocused()
            || !this.getText()
                .isEmpty()) {
            return;
        }
        // Empty and idle: AE2 draws the placeholder exactly where the typed text would sit.
        this.font.drawString(this.placeholder, this.fieldX + PADDING, this.fieldY + PADDING, PLACEHOLDER_COLOR);
    }
}
