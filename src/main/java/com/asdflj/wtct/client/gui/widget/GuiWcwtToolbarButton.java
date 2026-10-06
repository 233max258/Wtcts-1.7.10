package com.asdflj.wtct.client.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.asdflj.wtct.Wtct;

import appeng.api.config.Settings;
import appeng.client.gui.widgets.GuiImgButton;

/**
 * The terminal sidebar, drawn exactly the way AE2 1.21 draws it.
 *
 * <p>
 * AE2 1.21's {@code IconButton} is what WCWT's sidebar looks like, so this is a transcription of it
 * rather than an imitation:
 *
 * <pre>
 * var yOffset = isHovered() ? 1 : 0;
 * Icon bg = isHovered() ? TOOLBAR_BUTTON_BACKGROUND_HOVER
 *     : isFocused() ? TOOLBAR_BUTTON_BACKGROUND_FOCUS : TOOLBAR_BUTTON_BACKGROUND;
 * bg.getBlitter()
 *     .dest(getX() - 1, getY() + yOffset, 18, 20);
 * icon.getBlitter()
 *     .dest(getX(), getY() + 1 + yOffset);
 * </pre>
 *
 * The button itself stays 16x16; the background is the 18x20 sprite and pokes one pixel out to the
 * left and four below, which is what gives the sidebar its overlapping plates.
 *
 * <p>
 * Verbatim AE2 1.21, hover sink included: the plate and the icon drop one pixel while the cursor is on
 * them, exactly as {@code IconButton} does. Nothing is drawn behind the column - the plates stand on the
 * panel's own surface inside its frame, which is what puts the panel's white binding around them.
 *
 * <p>
 * Both sprites come from AE2 1.21's own {@code states.png} (shipped here as
 * {@code ae2_toolbar_states.png}): the plates at (176,128) idle, (194,128) focused and (212,128)
 * hovered, and the icons at the coordinates of AE2's {@code Icon} enum - {@code SORT_BY_NAME} at
 * (0,64), {@code ARROW_UP}/{@code ARROW_DOWN} at (0,48)/(16,48), {@code COG} at (32,64),
 * {@code CRAFT_HAMMER} at (48,144) and so on. Reading the coordinates off the enum is what keeps the
 * sidebar identical to AE2 1.21 instead of merely close to it.
 *
 * <p>
 * Everything else - the click behaviour that opens AE2's settings popup, the tooltip built from the
 * setting and its value - is inherited from {@link GuiImgButton} untouched.
 */
public class GuiWcwtToolbarButton extends GuiImgButton {

    /** AE2 1.21's icon sheet. Kept separate: WCWT's own copy overwrites several of these sprites. */
    private static final ResourceLocation ICONS = new ResourceLocation(
        Wtct.MODID,
        "textures/guis/wcwt/ae2_toolbar_states.png");

    /**
     * {@code Icon.TOOLBAR_BUTTON_BACKGROUND[_FOCUS|_HOVER]}, 18x20 at y=128 - the plate WCWT's left toolbar
     * shows, drawn at {@code x - 1} so it pokes one column out left of the 16x16 button and four rows below.
     */
    private static final int BG_V = 128, BG_W = 18, BG_H = 20;
    private static final int BG_U_IDLE = 176, BG_U_FOCUS = 194, BG_U_HOVER = 212;
    private static final int BG_DX = -1;

    /** The plate's height - how far a row of the column reaches below its button's y. */
    public static final int PLATE_H = BG_H;

    /** Spacing between two sidebar buttons: the 20px plate plus AE2 1.21's 2px gap. */
    public static final int ROW_PITCH = 22;

    private final Settings setting;
    /** Which sheet the plate and icons come from; WCWT's copy unless a screen asks for the pristine art. */
    private ResourceLocation iconAtlas = ICONS;

    public GuiWcwtToolbarButton(final int x, final int y, final Settings setting, final Enum<?> value) {
        super(x, y, setting, value);
        this.setting = setting;
    }

    /**
     * Points the icons at another sheet of the same layout. WCWT's copy of AE2 1.21's states.png redraws a
     * few sprites for the terminal's own sidebar; a screen that wants the stock 1.21 pictures instead names
     * the untouched sheet here.
     */
    public GuiWcwtToolbarButton withIconAtlas(final ResourceLocation atlas) {
        this.iconAtlas = atlas;
        return this;
    }

    @Override
    public void drawButton(final Minecraft mc, final int mouseX, final int mouseY) {
        if (!this.visible) {
            return;
        }
        this.field_146123_n = mouseX >= this.xPosition && mouseY >= this.yPosition
            && mouseX < this.xPosition + this.width
            && mouseY < this.yPosition + this.height;
        final int yOffset = this.field_146123_n ? 1 : 0;
        final TextureManager manager = mc.getTextureManager();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        // AE2 1.21 IconButton.extractContents, verbatim: the 18x20 plate at (x - 1, y + yOffset), then the
        // icon at (x, y + 1 + yOffset) in its native size - hovered swaps in the hover plate and sinks
        // plate and icon one row together.
        manager.bindTexture(this.iconAtlas);
        // A setting the current access mode forbids: the vanilla button grays itself out, and so does this
        // one - both plate and icon drawn dark until the state that enables it comes back.
        if (!this.enabled) {
            GL11.glColor4f(0.5F, 0.5F, 0.5F, 1.0F);
        }
        drawTexturedModalRect(
            this.xPosition + BG_DX,
            this.yPosition + yOffset,
            this.field_146123_n ? BG_U_HOVER : BG_U_IDLE,
            BG_V,
            BG_W,
            BG_H);
        // The live value, not the construction-time one: GuiImgButton cycles the setting on click and
        // stores the new value in its own currentValue (set()), so reading that is what makes the icon
        // follow the presses. The captured field went stale after the first click.
        final int[] icon = iconFor(this.setting, this.getCurrentValue());
        drawTexturedModalRect(
            this.xPosition,
            this.yPosition + 1 + yOffset,
            icon[0],
            icon[1],
            icon.length > 2 ? icon[2] : 16,
            icon.length > 3 ? icon[3] : 16);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /**
     * AE2 1.21's {@code Icon} coordinates for this setting's current value, so the sidebar shows the
     * same picture AE2 1.21 would. Names are matched rather than ordinals: the two mods have
     * different enums with the same members.
     */
    private static int[] iconFor(final Settings setting, final Enum<?> value) {
        final String name = value == null ? "" : value.name();
        switch (setting) {
            case SORT_BY:
                return switch (name) {
                    case "AMOUNT" -> new int[] { 16, 64 }; // SORT_BY_AMOUNT
                    case "MOD" -> new int[] { 96, 64 }; // SORT_BY_MOD
                    case "INVENTORY_TWEAKS" -> new int[] { 80, 64 };
                    default -> new int[] { 0, 64 }; // SORT_BY_NAME
                };
            case VIEW_MODE:
                return switch (name) {
                    case "ALL" -> new int[] { 32, 16 }; // VIEW_MODE_ALL
                    case "CRAFTING" -> new int[] { 48, 16 }; // VIEW_MODE_CRAFTING
                    default -> new int[] { 0, 16 }; // VIEW_MODE_STORED
                };
            case SORT_DIRECTION:
                // ARROW_DOWN(16,48) / ARROW_UP(0,48)
                return "DESCENDING".equals(name) ? new int[] { 16, 48 } : new int[] { 0, 48 };
            case SEARCH_MODE:
                // AE2 1.21's SEARCH_* row (y=32) is pink placeholder art except for one dim magnifier,
                // which read as a smudge next to the other glyphs. The settings cog - what the reference
                // sidebar shows for its settings button - is drawn and reads as "search settings".
                return new int[] { 32, 64 }; // COG
            case SAVE_SEARCH:
                return "YES".equals(name) ? new int[] { 208, 224, 10, 10 } // S_STORAGE
                    : new int[] { 96, 0 }; // CLEAR
            case TERMINAL_STYLE:
                return switch (name) {
                    case "MEDIUM" -> new int[] { 16, 208 };
                    case "TALL" -> new int[] { 32, 208 };
                    case "FULL" -> new int[] { 48, 208 };
                    default -> new int[] { 0, 208 }; // TERMINAL_STYLE_SMALL
                };
            case CRAFTING_STATUS:
                return new int[] { 48, 144 }; // CRAFT_HAMMER
            case REDSTONE_CONTROLLED:
                return switch (name) {
                    case "LOW_SIGNAL" -> new int[] { 0, 0 }; // REDSTONE_LOW
                    case "HIGH_SIGNAL" -> new int[] { 16, 0 }; // REDSTONE_HIGH
                    case "SIGNAL_PULSE" -> new int[] { 32, 0 }; // REDSTONE_PULSE
                    default -> new int[] { 48, 0 }; // REDSTONE_IGNORE
                };
            case FUZZY_MODE:
                return switch (name) {
                    case "PERCENT_50" -> new int[] { 16, 96 }; // FUZZY_PERCENT_50
                    case "PERCENT_75" -> new int[] { 32, 96 }; // FUZZY_PERCENT_75
                    case "PERCENT_99" -> new int[] { 48, 96 }; // FUZZY_PERCENT_99
                    case "IGNORE_ALL" -> new int[] { 64, 96 }; // FUZZY_IGNORE
                    default -> new int[] { 0, 96 }; // FUZZY_PERCENT_25, also stands in for GTNH's 10%/1%
                };
            case CRAFT_ONLY:
                // 1.21's own mapping: YES -> VIEW_MODE_CRAFTING, NO -> VIEW_MODE_ALL
                return "YES".equals(name) ? new int[] { 48, 16 } : new int[] { 32, 16 };
            case SCHEDULING_MODE:
                return switch (name) {
                    case "ROUNDROBIN" -> new int[] { 16, 240 }; // SCHEDULING_ROUND_ROBIN
                    case "RANDOM" -> new int[] { 32, 240 }; // SCHEDULING_RANDOM
                    default -> new int[] { 0, 240 }; // SCHEDULING_DEFAULT
                };
            case ACCESS:
                // 1.21's own row: ACCESS_WRITE(0,144) / ACCESS_READ(16,144) / ACCESS_READ_WRITE(32,144)
                return switch (name) {
                    case "READ" -> new int[] { 16, 144 };
                    case "WRITE" -> new int[] { 0, 144 };
                    default -> new int[] { 32, 144 };
                };
            case STORAGE_FILTER:
                // STORAGE_FILTER_EXTRACTABLE_ONLY(80,48) / STORAGE_FILTER_EXTRACTABLE_NONE(96,48)
                return "NONE".equals(name) ? new int[] { 96, 48 } : new int[] { 80, 48 };
            case EXTRACTION_MODE:
                // 1.21 has no extract-mode button; rv3's STRICT/LOOSE borrow its FILTER_ON_EXTRACT pair
                // - STRICT reads as "filter gates extraction", LOOSE as "everything extractable".
                return "STRICT".equals(name) ? new int[] { 160, 48 } : new int[] { 176, 48 };
            case ACTIONS:
                // The values the extended storage bus parks here: CLOSE is the CLEAR glyph, the partition
                // button the wrench the enum's own comment points at, and the ore filter - with no 1.21
                // counterpart - keeps TYPE_FILTER_ALL, which reads as "filter".
                return switch (name) {
                    case "CLOSE" -> new int[] { 96, 0 }; // CLEAR
                    case "WRENCH", "NEXT_PARTITION" -> new int[] { 32, 64 }; // the wrench glyph
                    default -> new int[] { 160, 16 }; // ORE_FILTER -> TYPE_FILTER_ALL
                };
            default:
                return new int[] { 96, 0 }; // Icon.CLEAR
        }
    }
}
