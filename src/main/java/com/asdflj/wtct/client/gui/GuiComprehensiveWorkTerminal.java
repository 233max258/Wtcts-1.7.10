package com.asdflj.wtct.client.gui;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.asdflj.wtct.Wtct;
import com.asdflj.wtct.api.InventoryActionExtend;
import com.asdflj.wtct.api.Pinned;
import com.asdflj.wtct.api.WtctAPI;
import com.asdflj.wtct.client.gui.container.ContainerComprehensiveWorkTerminal;
import com.asdflj.wtct.client.gui.container.ContainerWcwtSettings;
import com.asdflj.wtct.client.gui.widget.GuiEncodeArrowButton;
import com.asdflj.wtct.client.gui.widget.GuiMultiplierButton;
import com.asdflj.wtct.client.gui.widget.GuiWcwtAutoFillButton;
import com.asdflj.wtct.client.gui.widget.GuiWcwtMgmtToggle;
import com.asdflj.wtct.client.gui.widget.GuiWcwtRadio;
import com.asdflj.wtct.client.gui.widget.GuiWcwtSidebarIconButton;
import com.asdflj.wtct.client.gui.widget.GuiWcwtStateButton;
import com.asdflj.wtct.client.gui.widget.GuiWcwtStatusButton;
import com.asdflj.wtct.client.gui.widget.GuiWcwtStripIconButton;
import com.asdflj.wtct.client.gui.widget.GuiWcwtTabButton;
import com.asdflj.wtct.client.gui.widget.GuiWcwtTextField;
import com.asdflj.wtct.client.gui.widget.GuiWcwtToolbarButton;
import com.asdflj.wtct.client.gui.widget.GuiWcwtUpgradePanel;
import com.asdflj.wtct.client.me.AdvItemRepo;
import com.asdflj.wtct.common.item.card.CardTicker;
import com.asdflj.wtct.common.item.card.ItemWcwtUpgradeCard;
import com.asdflj.wtct.inventory.InventoryHandler;
import com.asdflj.wtct.inventory.gui.GuiType;
import com.asdflj.wtct.network.CPacketFillCraftingSlot;
import com.asdflj.wtct.network.CPacketFluidCellMark;
import com.asdflj.wtct.network.CPacketInventoryActionExtend;
import com.asdflj.wtct.network.CPacketSwitchGuis;
import com.asdflj.wtct.network.CPacketTerminalBtns;
import com.asdflj.wtct.util.Ae2ReflectClient;
import com.asdflj.wtct.util.ModAndClassUtil;
import com.asdflj.wtct.util.NeCharUtil;
import com.asdflj.wtct.util.PatternMappingStore;
import com.asdflj.wtct.util.PatternScaling;
import com.glodblock.github.common.item.ItemFluidDrop;
import com.glodblock.github.common.item.ItemFluidPacket;

import appeng.api.AEApi;
import appeng.api.config.PatternBeSubstitution;
import appeng.api.config.Settings;
import appeng.api.config.TerminalFontSize;
import appeng.api.storage.ITerminalHost;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import appeng.client.gui.IInterfaceTerminalPostUpdate;
import appeng.client.gui.slots.VirtualMESlot;
import appeng.client.gui.widgets.GuiImgButton;
import appeng.client.gui.widgets.GuiScrollbar;
import appeng.client.render.StackSizeRenderer;
import appeng.container.slot.AppEngSlot;
import appeng.container.slot.SlotDisabled;
import appeng.container.slot.SlotFake;
import appeng.container.slot.SlotPlayerHotBar;
import appeng.container.slot.SlotPlayerInv;
import appeng.core.AEConfig;
import appeng.core.localization.ButtonToolTips;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketClickOrDragFakeSlot;
import appeng.core.sync.packets.PacketInterfaceTerminalUpdate;
import appeng.items.misc.ItemEncodedPattern;
import appeng.util.Platform;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import codechicken.nei.recipe.StackInfo;
import cpw.mods.fml.common.Loader;

/**
 * ME Comprehensive Work Terminal GUI - WCWT 1.21-style layout on top of the GuiMonitor ME-repo
 * machinery. All geometry is derived from WCWT's own screen JSON and is identical to what AE2 1.21
 * does with it:
 *
 * <ul>
 * <li>Background is assembled from the terminalStyle slices: header(17) + rows(18 each) +
 * bottom(235). The reference layout is 288px tall, so the GUI height is {@code 288 + (rows-2)*18}.
 * <li>Row count = {@code (height - 2*margin - header - bottom) / rowHeight}, i.e. AE2 1.21's
 * {@code TerminalStyle.getPossibleRows()} with its {@code terminalMargin} client setting.
 * <li>Slots are positioned in <b>guiTop-relative</b> slot space - vanilla draws slots after
 * {@code glTranslatef(guiLeft, guiTop)}, so adding guiTop here would double-count it.
 * <li>Container slots carry their WCWT reference Y (see {@link ContainerComprehensiveWorkTerminal});
 * the GUI maps them below the last item row.
 * </ul>
 *
 * The texture is 512x512, so every blit goes through {@link #blit512} (1.7.10's default
 * drawTexturedModalRect normalises UVs against 256).
 */
public class GuiComprehensiveWorkTerminal extends GuiMonitor implements IInterfaceTerminalPostUpdate {

    public static final int GUI_WIDTH = 355;
    /**
     * The sidebar: a toolbar column attached to the panel's left, built entirely from the terminal's own
     * background texture so it reads as part of the same window.
     *
     * <p>
     * Column layout, left to right: the texture's frame columns (0 = 0x413F54, 1 = 0xF2F2F2), then surface
     * (columns 2, 3 tiled) out to {@code offsetX} - that column and the repainted {@code offsetX + 1} put the
     * plates at zero distance from the terminal. The 18 plate columns sit on this surface. One surface column
     * is left between the frame's white line and the plates' left bezel - the reference's own spacing, and
     * what keeps the bezel reading as a raised ring instead of welding to the frame.
     *
     * <p>
     * Vertically the strip starts on the window's top frame and is closed {@link #CAP_H four rows} under
     * the last plate - one foot row of surface between, the same foot the buses' wrapping plate leaves -
     * with the texture's own bottom border ({@link #CAP_V}), so it ends the way the reference's window
     * ends: border rows and corner art straight from the texture, nothing hand-drawn.
     */
    private static final int SIDEBAR_W = 19;
    /** Texture y of the window's own bottom border; the sidebar closes with these same rows. */
    private static final int CAP_V = 283, CAP_H = 4;
    /**
     * Screen y one row under the last plate: how far down the terminal's background is carried over the
     * sidebar, the closing border hanging a foot row below it - the buses' wrapping plate leaves the same
     * one middle row under its last plate. Filled in by {@link #initGui} from the plates themselves - how
     * many there are depends on the terminal's settings. Carrying it to the panel's bottom instead ran a
     * column of bare surface the height of the whole terminal past the toolbar.
     */
    private int sidebarBottom;
    /**
     * Header anchors, straight from WCWT's screen JSON: the title ({@code comprehensive_work_area}) at
     * left 8, and the display toggles - item, fluid, "everything else" - at 79, 104 and 129, 25 apart, in
     * front of the search box. The sidebar is outside the panel, so nothing inside it has to make room.
     * The toggles' switch cell is 18x17, exactly the header's height, so it starts at the header's top.
     */
    private static final int TITLE_X = 8;
    /**
     * WCWT's display toggles - item, fluid, "everything else" - at the screen JSON's own anchors: left 79,
     * 104 and 129, top 4, 22x12 apart in steps of 25. The buttons wear AE2 1.21's checkbox switch
     * ({@link GuiWcwtTypeButton}), which is exactly that 22x12.
     */
    private static final int TYPE_TOGGLE_X = 79;
    private static final int TYPE_TOGGLE_Y = 4;
    private static final String BACKGROUND = "guis/wcwt/wireless_comprehensive_work_terminal_gui.png";

    /** Slice geometry from the WCWT terminalStyle JSON. */
    private static final int HEADER_H = 17;
    private static final int ROW_H = 18;
    private static final int BOTTOM_H = 235;
    /** Height of WCWT's reference layout (2 rows): the frame all slot "bottom" values refer to. */
    private static final int REF_HEIGHT = 288;
    /** Texture Y of the bottom slice in the reference frame (see REF_HEIGHT). */
    private static final int REF_BOTTOM_TOP = REF_HEIGHT - BOTTOM_H; // 53
    /** Texture X of the item-list scrollbar (AE2 1.21 widget rect); the 18-column grid ends at 332. */
    private static final int SCROLLBAR_X = 336;
    private static final int SCROLLBAR_Y = 18;
    /**
     * Vertical margin around the terminal - AE2 1.21 reserves 2x this value (its "terminalMargin"
     * client setting) before counting rows.
     */
    private static final int TERMINAL_MARGIN = 5;
    private static final int MIN_ROWS = 2;
    /** AE2 binds player slots with offsetX=14 and puts the hotbar row at offsetY + 54 + 4. */
    private static final int PLAYER_BIND_X_OFFSET = 14;
    private static final int PLAYER_BIND_HOTBAR_Y = 58;

    // ---------------------------------------------------------------------------------------------
    // Pattern encoding area (texture coordinates, taken from WCWT's screen JSON / screen code).
    // Crafting mode and processing mode occupy the same box but are shown one at a time, which is
    // exactly what WCWT does - otherwise the crafting preview slot (x=274) would collide with the
    // processing output column (x=277).
    // ---------------------------------------------------------------------------------------------
    /** First visible encoding row (PROCESSING_INPUTS/OUTPUTS bottom 216 -> texture Y 72). */
    private static final int ENC_ROW_Y = 72;
    /** Encoding rows on screen at a time (the rest are reached with the scrollbar / mouse wheel). */
    private static final int ENC_VISIBLE_ROWS = 3;
    /**
     * Encoding inputs X (PROCESSING_INPUTS left 192). Crafting and processing share this column, so
     * the cells stay in place when the mode changes - only the extra slots (recipe preview vs. the
     * output column) differ between the modes.
     */
    private static final int ENC_MATRIX_X = 192;
    /**
     * Crafting matrix X. WCWT shifts the 3x3 crafting grid 9px left of the processing input column
     * ({@code PROCESSING_INPUTS.left - 9}), so the crafting cells deliberately do not line up with
     * the processing ones.
     */
    private static final int ENC_CRAFT_X = 183;
    /** Processing outputs X (PROCESSING_OUTPUTS left 277). */
    private static final int ENC_OUTPUT_X = 277;
    /** Crafting recipe preview (pattern_crafting_result_slot: left 274, bottom 198). */
    private static final int ENC_RESULT_X = 274;
    private static final int ENC_RESULT_Y = 90;
    /** Encoding panel background: WCWT PATTERN_ENCODING_BG at (176, 65), 124x66 - mouse wheel zone. */
    private static final int ENC_PANEL_X = 176;
    private static final int ENC_PANEL_Y = 65;
    private static final int ENC_PANEL_W = 124;
    private static final int ENC_PANEL_H = 66;
    /**
     * Encoding scrollbar. WCWT's {@code processingPatternModeScrollbar} sits at (183, bottom 216)
     * - one pixel-gutter left of the input cells (which start at x=192) - and spans the three
     * visible rows.
     */
    private static final int ENC_SCROLL_X = 183;
    private static final int ENC_SCROLL_Y = 72;
    private static final int ENC_SCROLL_H = 52;
    /** AE2 1.21's Scrollbar.SMALL is 8x12, so the groove is 8 wide and the knob 6x12. */
    private static final int ENC_SCROLL_W = 8;
    /** AE2 1.21's small_scroller handle sprite (7x15), drawn instead of a hand-filled knob. */
    private static final int ENC_KNOB_W = 7;
    private static final int ENC_KNOB_H = 15;

    /**
     * WCWT's {@code renderPatternEncodingBackground}: AE2 1.21's pattern_modes.png carries the
     * encoding panel art per mode - the 3x3 grid guides plus, in crafting mode, the crafting arrow -
     * as a 124x66 region at (176, 65): crafting = srcY 0, processing = srcY 70.
     */
    private static final String PATTERN_MODES = "guis/wcwt/pattern_modes.png";
    private static final int MODES_TEX_CRAFT_Y = 0;
    private static final int MODES_TEX_PROCESS_Y = 70;
    /** AE2 1.21 widget sprites (toolbar backdrop / encode arrow / scrollbar handles). */
    private static final ResourceLocation WIDGETS_TEXTURE = new ResourceLocation(
        Wtct.MODID,
        "textures/guis/wcwt/wcwt_widgets.png");
    /**
     * Pattern cache layout: WCWT_PATTERN_CACHE is 9 columns (see the container's CACHE_X / CACHE_Y),
     * of which two rows are on screen behind the caching scrollbar.
     */
    private static final int CACHE_VISIBLE_ROWS = ContainerComprehensiveWorkTerminal.CACHE_VISIBLE_ROWS;
    /** Cache scrollbar (WCWT caching_scrollbar: left 342, bottom 143, height 34). */
    private static final int CACHE_SCROLL_X = 342;
    private static final int CACHE_SCROLL_Y = 145;
    /**
     * The handle travel equals the cache area itself - the two slot rows (texture Y 145..178) - so
     * the handle top aligns with row 1's top at scroll 0 and its bottom with row 2's bottom at max,
     * exactly like WCWT's caching_scrollbar (left 342, height 34).
     */
    private static final int CACHE_SCROLL_H = 34;
    /** Batch buttons (WCWT Double_button0..7: 17x16, x=29/48/67/86, y=154 and 171). */
    private static final int MULT_X = 29;
    private static final int MULT_Y_TOP = 154;
    private static final int MULT_Y_BOTTOM = 171;
    private static final int MULT_PITCH = 19;
    /**
     * WCWT's batch property radios (pattern_Replace1/2, 14x14 at left 152, texture Y 153/172) with
     * their labels (物品替换/可作为替换 at left 108, bottom 131/113); the batch title itself is
     * drawn vertically at left 13, bottom 136. The radios are buttons and stay on frameY - the labels
     * and the title are FG text and go through {@link #areaTitleY} (see below).
     */
    private static final int BATCH_RADIO_X = 152;
    private static final int BATCH_RADIO1_Y = 153;
    private static final int BATCH_RADIO2_Y = 172;
    private static final int BATCH_LABEL_X = 108;
    /**
     * The batch area's text anchors, WCWT's {@code renderBatchProcessingLabels} in its own terms: the
     * multiplier label is drawn by code at {@code imageHeight - 136} (one glyph every 8px, scale 0.875,
     * left 13) and {@code imageHeight - 125} (centred on x = 17, scale 0.75) when it is not CJK, the
     * two property labels at {@code imageHeight - 131} / {@code - 113} (left 108). These are "bottom"
     * anchors, so they go through {@link #areaTitleY}: the text is drawn from {@code drawFG}, which
     * vanilla runs inside its {@code translate(guiLeft, guiTop)} - {@link #frameY}'s screen Y would add
     * guiTop a second time and drop the whole block that far down. The multiplier plates and the radios
     * are buttons, drawn untranslated, so those keep frameY.
     */
    private static final int BATCH_TITLE_BOTTOM = 136;
    private static final int BATCH_TITLE_LATIN_BOTTOM = 125;
    private static final int BATCH_LABEL1_BOTTOM = 131;
    private static final int BATCH_LABEL2_BOTTOM = 113;
    private static final int BATCH_TITLE_CJK_X = 13;
    private static final int BATCH_TITLE_CENTER_X = 17;
    /**
     * WCWT's pattern-management header toggles (left 175/189/203, bottom 97 -> texture Y 191): the
     * display-mode cycler, the show-slots switch and the search-scope cycler. Their icons live in
     * wcwt_widgets.png (AE2's PATTERN_TERMINAL_* / PATTERN_ACCESS_* sprites, ExtendedAE's search
     * icons).
     */
    private static final int MGMT_TOGGLE_DISPLAY_X = 175;
    private static final int MGMT_TOGGLE_SLOTS_X = 189;
    private static final int MGMT_TOGGLE_SEARCHMODE_X = 203;
    private static final int MGMT_TOGGLE_TEX_Y = 191;
    /**
     * ExtendedAE Plus' {@code DEFAULT_CRAFTING_SEARCH_KEY}: a crafting pattern falls back to this when
     * no recipe type was recorded, and the mapping table may rename it.
     */
    private static final String DEFAULT_CRAFTING_SEARCH_KEY = PatternMappingStore.CRAFTING_KEY;
    private static final int MGMT_ICON_SLOTS_ON_U = 200;
    private static final int MGMT_ICON_SLOTS_OFF_U = 220;
    private static final int MGMT_ICON_SLOTS_V = 48;
    private static final int MGMT_ICON_DISPLAY_ALL_U = 240;
    private static final int MGMT_ICON_DISPLAY_ALL_V = 48;
    private static final int MGMT_ICON_DISPLAY_VISIBLE_U = 200;
    private static final int MGMT_ICON_DISPLAY_NOT_FULL_U = 220;
    private static final int MGMT_ICON_DISPLAY_V = 72;
    private static final int MGMT_ICON_SEARCH_OUT_U = 240;
    private static final int MGMT_ICON_SEARCH_OUT_V = 72;
    private static final int MGMT_ICON_SEARCH_IN_U = 200;
    private static final int MGMT_ICON_SEARCH_IN_OUT_U = 216;
    private static final int MGMT_ICON_SEARCH_V = 96;

    /**
     * Encoding option buttons. WCWT lays them out at runtime relative to the encoding panel's
     * origin (176, 65) - {@code getPatternEncodingBackgroundOrigin()} in its screen code - all on
     * the panel's second row (origin + 6 = 71):
     *
     * <ul>
     * <li>clear: origin + 62 in crafting mode, origin + 71 in processing mode;
     * <li>WCWT's merge toggle: 10px right of the clear button (processing only);
     * <li>item substitution: origin + 72, fluid substitution: origin + 82 (crafting only). 1.7.10
     * has no fluid substitution, so that column carries AE2's be-substitution instead.
     * </ul>
     */
    private static final int ENC_OPT_ROW_Y = 71;
    private static final int ENC_OPT_CLEAR_CRAFT_X = 238;
    private static final int ENC_OPT_CLEAR_PROCESS_X = 247;
    private static final int ENC_OPT_SUBST_X = 248;
    private static final int ENC_OPT_BESUBST_X = 258;
    private static final int ENC_OPT_MERGE_X = 257;
    /**
     * WCWT's processing-output cycle button (ActionItems.S_CYCLE_PROCESSING_OUTPUT, half size, no
     * backdrop). The screen places it at the encoding background origin + (90, 6) = (266, 71) and
     * only shows it in processing mode while more than one output is set.
     */
    private static final int ENC_OPT_CYCLE_X = 266;
    private static final int CYCLE_ICON_U = 240;
    private static final int CYCLE_ICON_V = 152;
    /**
     * AE2 1.21's small icons, copied 1:1 from its {@code states.png} into our {@code wcwt_states.png}
     * at their original sheet positions: the clear cross and the substitution on/off pair - the same
     * sprites WCWT's clear and substitution buttons show on 1.21.
     */
    private static final int ICON_CLEAR_U = 224;
    private static final int ICON_CLEAR_V = 200;
    private static final int ICON_SUBST_ON_U = 224;
    private static final int ICON_SUBST_ON_V = 208;
    private static final int ICON_SUBST_OFF_U = 232;
    private static final int ICON_SUBST_OFF_V = 208;
    /** WCWT's merge pair lives in the same sheet, one row of 8x8 icons at v=16. */
    private static final int ICON_MERGE_ON_U = 0;
    private static final int ICON_MERGE_OFF_U = 16;
    private static final int ICON_MERGE_V = 16;
    /**
     * Mode tabs. WCWT stacks both on the right edge and keeps them both on screen, marking the
     * active mode as selected: modeTabButton0 at (331, bottom 232 -> texture Y 56) for crafting and
     * modeTabButton1 at (331, bottom 211 -> texture Y 77) for processing. AE2 1.7.10's own terminals
     * do it the other way round (one spot, only the inactive mode's tab drawn), which is what made
     * the tab look misplaced here.
     */
    private static final int TAB_X = 331;
    private static final int TAB_CRAFT_Y = 56;
    private static final int TAB_PROCESS_Y = 77;
    /**
     * WCWT's crafting-status button - the one at the terminal's top-right, whose icon reads as a
     * hammer. The screen JSON pins it at left 332, top -5: five pixels of it stick out above the
     * GUI's top edge, the way AE2 hangs its status tabs on the frame.
     */
    private static final int STATUS_X = 332;
    private static final int STATUS_OVERHANG = 5;
    /** WCWT's crafting-status cell is 20x20 and the sprite fills it edge to edge. */
    private static final int STATUS_SIZE = 20;
    /**
     * The card buttons, in the addon's own arrangement: its import, export and block picker buttons
     * all go into the left toolbar (AE2 1.21's {@code addToLeftToolbar} with an 18x20 IconButton), so
     * this port keeps them there too and puts the magnet, trash and settings buttons in the horizontal
     * strip between the manual crafting area and the multiplier row instead.
     *
     * <p>
     * The strip: WCWT's texture marks it with two white lines (texture rows 135 and 149) and leaves
     * rows 136..148 between them, which is why the three buttons there use
     * {@link GuiWcwtStripIconButton} - the 12x12 plate that fits inside those lines. AE2's full icon
     * button is 20 rows tall and would cross both.
     */
    private static final int STRIP_BTN_Y = 136;
    private static final int STRIP_MAGNET_X = 8;
    private static final int STRIP_TRASH_X = 23;
    private static final int STRIP_SETTINGS_X = 38;
    /** The strip's slot pitch: 8 -> 23 -> 38, one 12px plate plus a 3px gap. */
    private static final int STRIP_BTN_PITCH = STRIP_TRASH_X - STRIP_MAGNET_X;
    /**
     * How far the two widget styles differ inside the same checkbox sprite. {@code
     * GuiWcwtStripIconButton} crops the 14x14 sprite at +1, so its visible 12x12 plate starts on the
     * button's own corner; {@code GuiWcwtMgmtToggle} blits the whole 14x14 sprite, so its plate starts
     * one pixel inside the button. A toggle standing in for a strip plate therefore has to be placed
     * one pixel up and to the left of that slot's corner for the two plates to line up - without it
     * the switch reads as hanging a pixel low and a pixel right of the three beside it.
     */
    private static final int STRIP_PLATE_INSET = 1;
    /**
     * Our own fifth plate (WCWT has no counterpart): the enlargement switch joins this strip and takes
     * the slot just right of the terminal-settings cog, one pitch along. It carries no icon - the
     * widget sheet has no up-arrow sprite to borrow and a wrong icon would read worse than none - so
     * it is the plain {@code GuiWcwtMgmtToggle} radio plate, checked while the list is pulled up over
     * the cache. This is only the widget's first placement: {@link #updateMgmtChromePositions}
     * re-anchors it every frame along with the rest of the chrome.
     */
    private static final int MGMT_TOGGLE_EXPAND_X = STRIP_SETTINGS_X + STRIP_BTN_PITCH - STRIP_PLATE_INSET;
    private static final int MGMT_TOGGLE_EXPAND_TEX_Y = STRIP_BTN_Y - STRIP_PLATE_INSET;
    /** Manual crafting grid buttons (WCWT clearCraftingGrid / clearToPlayerInv). */
    private static final int MANUAL_CLEAR_X = 134;
    private static final int MANUAL_STASH_X = 144;
    private static final int MANUAL_BTN_Y = 74;
    /**
     * "Stash to player inventory" next to the clear button (WCWT clearToPlayerInv), drawn with AE2
     * 1.21's small arrow icon copied into wcwt_states.png at (232,152).
     */
    private static final int STASH_ICON_U = 232;
    private static final int STASH_ICON_V = 152;
    /**
     * The texture's black panel, measured from the background PNG: x 25..71 (47 wide), texture Y
     * 65..134 (70 tall). The model renders centred inside it: at scale 30 a player is 1.8 blocks =
     * 54px tall, so the feet sit {@code (70 - 54) / 2 = 8px} above the panel's bottom.
     */
    private static final int PLAYER_BOX_X = 25;
    private static final int PLAYER_BOX_W = 47;
    private static final int PLAYER_BOX_TOP = 65;
    private static final int PLAYER_BOX_H = 70;
    private static final int PLAYER_SCALE = 30;
    /** Model height in GUI pixels at {@link #PLAYER_SCALE} (1.8 blocks). */
    private static final int PLAYER_MODEL_H = 54;
    /** WCWT's stash button. */
    private GuiWcwtStateButton stashBtn;
    /** WCWT's clear-grid button, drawn as its own 8x8 sprite instead of AE2 1.7.10's image button. */
    private GuiWcwtStateButton manualClearBtn;
    /** WCWT's processing-output cycle button in the encoding area. */
    private GuiWcwtStateButton cycleOutputBtn;
    /** Sentinel multiplier value of the swap (⇄) button, which rotates the processing outputs. */
    private static final int SWAP = Integer.MIN_VALUE;

    /**
     * WCWT's Double_button0..7 order: swap, ×2, ×3, ×5 on the top row and =1, ÷2, ÷3, ÷5 below. The
     * glyphs use unicode escapes so the source stays ASCII-safe.
     */
    private static final int[] MULTIPLIERS = { SWAP, 2, 3, 5, 0, -2, -3, -5 };
    private static final String[] MULT_LABELS = { null, "\u00D72", "\u00D73", "\u00D75", "=1", "\u00F72", "\u00F73",
        "\u00F75" };
    private static final String[] MULT_TIPS = { "swap", "times2", "times3", "times5", "equals1", "divide2", "divide3",
        "divide5" };

    /** Where slots go while they are not part of the active mode. */
    private static final int HIDDEN_SLOT = -9999;
    /**
     * Area titles. WCWT's texture carries no text - AE2 1.21 draws the screen JSON's {@code text}
     * nodes at runtime - so the labels have to be drawn here, at the same coordinates as the JSON
     * (left 8 or 176, with the usual {@code ySize - bottom} anchors, see {@link #areaTitleY}).
     * The nodes carry no colour, so they use AE2's default dark grey like the terminal title.
     */
    /**
     * Left-hand area titles (crafting area, player inventory), at the screen JSON's own left 8: the
     * sidebar is outside the panel, so nothing inside has to shift around it.
     */
    private static final int AREA_TITLE_LEFT_X = 8;
    private static final int AREA_TITLE_RIGHT_X = 176;
    private static final int AREA_TITLE_COLOR = 0x404040;
    /**
     * Encoding cell colours, sampled from the WCWT texture's encoding panel (panel = 0x9A9FB4).
     * AE2 1.21 paints its own Icon.SLOT_BACKGROUND sprite there at runtime, which 1.7.10 has no
     * equivalent of, so the cells are painted opaque (no GL blending involved).
     */

    public ContainerComprehensiveWorkTerminal monitorableContainer;

    private GuiEncodeArrowButton encodeBtn;
    /** Batch multiplier plates, WCWT order: swap ×2 ×3 ×5 on the top row, =1 ÷2 ÷3 ÷5 below. */
    private final GuiMultiplierButton[] multButtons = new GuiMultiplierButton[MULTIPLIERS.length];
    /**
     * Item substitution toggle, drawn with AE2 1.21's own small icons (see the ICON_* constants) so
     * it looks exactly like WCWT on 1.21 - one button that swaps sprites instead of a pair.
     */
    private GuiWcwtStateButton substBtn;
    /** WCWT's batch property radios next to the multiplier plates. */
    private GuiWcwtRadio batchItemRadio;
    private GuiWcwtRadio batchBeSubRadio;
    /** WCWT's management header toggles. */
    private GuiWcwtMgmtToggle mgmtDisplayToggle;
    private GuiWcwtMgmtToggle mgmtSlotsToggle;
    private GuiWcwtMgmtToggle mgmtSearchModeToggle;
    private GuiWcwtMgmtToggle mgmtAutoUploadToggle;
    /** Our own fifth switch: enlarge the management area by taking over the pattern cache's place. */
    private GuiWcwtMgmtToggle mgmtExpandToggle;
    /**
     * What the four switches were the last time the rows were built. They live on the terminal item and
     * reach the client through the container's field sync, so the list is rebuilt when that sync brings
     * something new - a click on a toggle, or a value the terminal was left with last session.
     */
    private int mgmtSeenSettings = -1;
    /** The enlargement switch's value at the last relayout, so flipping it rebuilds what it hides. */
    private boolean mgmtSeenExpanded;
    /** The chrome shift the two text fields were built with, so they are only rebuilt on a change. */
    private int mgmtSeenChromeShift = Integer.MIN_VALUE;
    /** "Can be substituted" toggle (AE2's beSubstitutions pair, left in the 1.7.10 style). */
    private GuiImgButton beSubstOnBtn;
    private GuiImgButton beSubstOffBtn;
    /** Pattern-area clear button (AE2 1.21's S_CLEAR icon). */
    private GuiWcwtStateButton encClearBtn;
    /** WCWT's "merge materials" toggle (processing patterns only). */
    private GuiWcwtStateButton mergeBtn;

    /**
     * Crafting / processing tabs. Both stay on screen (WCWT stacks them on the right edge) with the
     * active mode marked as selected, so the tabs never move when the mode changes.
     */
    private GuiWcwtTabButton tabCraftButton;
    private GuiWcwtTabButton tabProcessButton;
    /**
     * Crafting-status button - the "hammer" at the terminal's top-right. One 20x20 image (plate and
     * hammer baked into the sprite), no vanilla button background.
     */
    private GuiWcwtStatusButton statusBtn;
    /** Scroll offset (in rows) of the processing inputs, paged with the wheel over the panel. */
    private int encScroll;
    /** Scroll offset (in rows) of the pattern cache, same interaction as the encoding area. */
    private int cacheScroll;
    /** True while the cache scrollbar's knob is being dragged. */
    private boolean draggingCacheKnob;
    /** True while the encoding scrollbar's knob is being dragged. */
    private boolean draggingEncKnob;
    /**
     * The right-hand upgrade panel (WCWT's {@code scrollingUpgrades}): the quantum singularity slot
     * plus the terminal's card column, showing {@code max(2, rows)} of them at a time. Built in
     * {@link #initGui} from the container's slots.
     */
    private GuiWcwtUpgradePanel upgradePanel;
    /** True while the upgrade panel's handle is being dragged. */
    private boolean draggingUpgradeKnob;
    /**
     * The upgrade cards' header buttons: one per card that has something to configure, shown only
     * while that card is in the terminal's card column (WCWT does the same for its magnet card).
     */
    private GuiWcwtSidebarIconButton cardImportBtn;
    private GuiWcwtSidebarIconButton cardExportBtn;
    private GuiWcwtSidebarIconButton cardPickerBtn;
    /**
     * The "craft if missing" switch - the same setting the settings screen shows as a checkbox, put
     * in the toolbar so the player can flip it without leaving the terminal.
     */
    private GuiWcwtAutoFillButton autoFillBtn;
    /** The column's first row for the card buttons - the row under the last AE2 settings plate. */
    private int cardRowBaseY;
    private GuiWcwtStripIconButton cardMagnetBtn;
    private GuiWcwtStripIconButton trashBtn;
    private GuiWcwtStripIconButton settingsBtn;

    public GuiComprehensiveWorkTerminal(Container container) {
        super(container);
        // WCWT keeps AE2's full left toolbar, view-mode button included (the reference sidebar shows the
        // stored-view crystal between sort-by and the type filter). Only the crafting-status button is
        // replaced - this terminal pins its own 20x20 status cell to the header's right edge instead.
        this.showViewBtn = true;
        this.viewCell = false;
    }

    public GuiComprehensiveWorkTerminal(InventoryPlayer inventory, ITerminalHost inv) {
        this(new ContainerComprehensiveWorkTerminal(inventory, inv));
        this.xSize = GUI_WIDTH;
        this.standardSize = GUI_WIDTH;
        (this.monitorableContainer = (ContainerComprehensiveWorkTerminal) this.inventorySlots).setGui(this);
        // GuiMonitor's ySize formula is 115 + rows*18 + reservedSpace; 137 makes it match the WCWT
        // geometry (17 + rows*18 + 235) in case anything reads ySize before initGui runs.
        this.reservedSpace = HEADER_H + BOTTOM_H - 115; // 137
    }

    @Override
    protected int getMaxRows() {
        // GuiMonitor's default caps SMALL terminals at 6 rows and leaves TALL/FULL open; the window
        // still drives the actual row count below (like WCWT on 1.21).
        return super.getMaxRows();
    }

    /**
     * {@code IconButton} draws the plate one left of the button ({@code x - 1}, 18 wide), so a button at
     * {@code guiLeft - 15} puts the plate across panel-relative (-16..+1) - the same footprint as the
     * reference's toolbar and the extended buses' wrapping plate, which blits its own background at
     * {@code guiLeft - 19}: frame columns on -19/-18, one surface column on -17, the plate's right bezel
     * landing on the panel's own white line column. The frame's two columns sit under the plates for as
     * long as the sidebar runs - {@link #blitSidebarSlice} repaints them as surface so the bezel rests on
     * plain surface rather than on the frame art.
     *
     * <p>
     * Vertically, {@code guiTop + 3} is WCWT's own anchor: its screen JSON puts
     * {@code widgets.verticalToolbar} at {@code top = 1}, and AE2 1.21's {@code VerticalButtonBar} adds its
     * {@code MARGIN = 2} to that. The top plate therefore starts on texture row 3 - frame on rows 0 and 1,
     * one row of surface on row 2.
     */
    @Override
    protected int toolbarX() {
        return this.guiLeft - 15;
    }

    @Override
    protected int toolbarY() {
        return this.guiTop + 3;
    }

    /**
     * The type toggles are a header row, not a side column: WCWT's screen JSON anchors its display
     * toggles - item, fluid and "everything else" - at (79, 4), (104, 4) and (129, 4), 25 apart, in front
     * of the search box. Ours are AE2's 16px toggles, so they sit at 1 to stay centred in the 17px header,
     * and they step along x: stacked into a column they ran out of the header into the item grid.
     */
    @Override
    protected void initTypeFilter() {
        this.typeFilter.initRow(this.buttonList, this.guiLeft + TYPE_TOGGLE_X, this.guiTop + TYPE_TOGGLE_Y, 25);
    }

    @Override
    public void initGui() {
        super.initGui();

        // AE2 1.21: rows = min(terminalStyle rows, getPossibleRows(height - 2*margin)).
        final int possible = (this.height - 2 * TERMINAL_MARGIN - HEADER_H - BOTTOM_H) / ROW_H;
        this.rows = Math.max(MIN_ROWS, Math.min(this.maxRows, possible));
        this.ySize = HEADER_H + ROW_H + Math.max(0, this.rows - 2) * ROW_H + ROW_H + BOTTOM_H;

        // GuiMonitor anchored the sort buttons and search box against its own guiTop; re-anchor them.
        final int newTop = (this.height - this.ySize) / 2;
        final int dy = newTop - this.guiTop;
        this.guiTop = newTop;
        if (dy != 0) {
            for (final Object b : this.buttonList) {
                ((GuiButton) b).yPosition += dy;
            }
            if (this.searchField != null) {
                this.searchField.y += dy;
            }
        }
        // The sidebar's surface stops one row under the last plate; the terminal's own left frame carries on
        // below it. The card buttons add rows of their own further down, so this runs again once they
        // exist (see setupCardButtons).
        this.refreshSidebarBottom();

        // WCWT keeps its crafting-status button pinned to the terminal's top-right corner - screen JSON:
        // left 332, top -5, a 20x20 cell. GuiMonitor builds its own copy inside the `viewCell` block and
        // this terminal switches viewCell off, so nothing existed here at all; AE2's copy would have sat
        // at x=170, sized for its 195px-wide terminal.
        //
        // Drawn as one image rather than AE2's tab: WCWT's button is a single 20x20 sprite (plate and
        // hammer baked in) blitted straight to the screen, no vanilla button background.
        this.buttonList.add(
            this.statusBtn = new GuiWcwtStatusButton(
                this.guiLeft + STATUS_X,
                this.guiTop - STATUS_OVERHANG,
                STATUS_SIZE,
                "wtct.gui.status.title",
                "wtct.gui.status.desc"));

        // WCWT's provider search box ("供应器搜索"): it filters the management list and doubles as the
        // word the upload matches a provider against.
        // Both fields are rebuilt on every initGui - and returning from the mapping manager or the
        // provider chooser runs it again - so carry the text over, the way WCWT does. Without this a
        // round trip would wipe whatever term the player had set up (and the proactive fill with it).
        // The enlargement switch moves the fields too, so they are always built through the one
        // helper that knows the current chrome position - see updateMgmtChromePositions.
        this.rebuildMgmtFields();
        // Record the shift they were just built with, so the per-frame check does not rebuild them
        // once more on the very first frame.
        this.mgmtSeenChromeShift = this.mgmtChromeShift();

        // The sidebar stands outside the panel, so the item list has the whole width: 18 columns from the
        // first cell (x = 8) up to the scrollbar at 336 - exactly what the screen JSON lays out.
        this.perRow = 18;
        this.repo.setRowSize(this.perRow);
        this.getMeSlots()
            .clear();
        // AE2 draws ME stacks from its own virtual-slot list, so GuiMonitor's 9-column grid has to
        // be dropped as well - otherwise both grids render and items look doubled.
        Ae2ReflectClient.clearVirtualSlots(this);
        for (int y = 0; y < this.rows; y++) {
            for (int x = 0; x < this.perRow; x++) {
                this.registerMESlot(
                    new appeng.client.gui.slots.VirtualMEMonitorableSlot(
                        8 + x * 18,
                        ROW_H + y * ROW_H,
                        this.repo,
                        x + y * this.perRow,
                        type -> true));
            }
        }
        this.setScrollBar();
        this.repo.updateView();

        // Search box: WCWT widget rect (left 241, top 4, 90x12).
        if (this.searchField != null) {
            this.searchField.x = this.guiLeft + 241;
            this.searchField.y = this.guiTop + 4;
        }

        // Clear-crafting-grid button: WCWT widget rect (left 134, bottom 214). Drawn as WCWT's own
        // 8x8 icon (AE2 1.21's S_CLEAR at 224,200 in wcwt_states.png), no vanilla button background -
        // same style as the stash button next to it.
        this.buttonList.add(
            this.manualClearBtn = new GuiWcwtStateButton(
                this.guiLeft + MANUAL_CLEAR_X,
                frameY(MANUAL_BTN_Y),
                8,
                ICON_CLEAR_U,
                ICON_CLEAR_V,
                "wtct.gui.manual.clear.title",
                "wtct.gui.manual.clear.desc"));

        // Stash-to-player-inventory next to it (WCWT clearToPlayerInv, 8x8 icon-only like the other
        // WCWT small buttons).
        this.buttonList.add(
            this.stashBtn = new GuiWcwtStateButton(
                this.guiLeft + MANUAL_STASH_X,
                frameY(MANUAL_BTN_Y),
                8,
                STASH_ICON_U,
                STASH_ICON_V,
                "wtct.gui.manual.stash.title",
                "wtct.gui.manual.stash.desc"));

        // Pattern area, following WCWT's own widget rects (all relative to the row area's bottom):
        // encode arrow (wcwtEncodePattern: left 308, bottom 176 -> texture Y 112)
        // mode tabs (modeTabButton0/1: left 331, bottom 232/211 -> texture Y 56/77).
        // WCWT keeps *both* tabs on screen and marks the active mode as selected, so neither tab
        // ever moves - clicking either one selects that mode.
        this.buttonList.add(this.encodeBtn = new GuiEncodeArrowButton(this.guiLeft + 308, frameY(112)));
        this.buttonList.add(
            this.tabCraftButton = new GuiWcwtTabButton(
                this.guiLeft + TAB_X,
                frameY(TAB_CRAFT_Y),
                GuiWcwtTabButton.CRAFT_ICON_U,
                appeng.core.localization.GuiText.CraftingPattern.getLocal()));
        this.buttonList.add(
            this.tabProcessButton = new GuiWcwtTabButton(
                this.guiLeft + TAB_X,
                frameY(TAB_PROCESS_Y),
                GuiWcwtTabButton.PROCESS_ICON_U,
                appeng.core.localization.GuiText.ProcessingPattern.getLocal()));
        // Encoding scrollbar: WCWT's "processingPatternModeScrollbar". It is drawn by this GUI (see
        // drawEncodingScroll) because AE2 1.7.10's GuiScrollbar only blits a vanilla-style knob and
        // no groove.
        // Batch multiplier plates (WCWT Double_button0..7) under the manual crafting grid, drawn
        // with WCWT's own plate sprites in the WCWT order.
        for (int i = 0; i < MULTIPLIERS.length; i++) {
            final int col = i % 4;
            final int row = i / 4;
            final int x = this.guiLeft + MULT_X + col * MULT_PITCH;
            final int y = this.guiTop + bottomStartRel() + (row == 0 ? MULT_Y_TOP : MULT_Y_BOTTOM) - REF_BOTTOM_TOP;
            this.buttonList.add(
                this.multButtons[i] = MULTIPLIERS[i] == SWAP
                    ? GuiMultiplierButton.swap(x, y, "wtct.gui.multiplier.swap")
                    : new GuiMultiplierButton(x, y, MULT_LABELS[i], "wtct.gui.multiplier." + MULT_TIPS[i]));
        }
        // Batch property radios (WCWT pattern_Replace1/2): 物品替换 writes the pattern's "substitute",
        // 可作为替换 (AE2's own wording for "beSubstitute") its "beSubstitute".
        this.buttonList.add(
            this.batchItemRadio = new GuiWcwtRadio(
                this.guiLeft + BATCH_RADIO_X,
                frameY(BATCH_RADIO1_Y),
                "wtct.gui.batch.item_substitution"));
        this.buttonList.add(
            this.batchBeSubRadio = new GuiWcwtRadio(
                this.guiLeft + BATCH_RADIO_X,
                frameY(BATCH_RADIO2_Y),
                "wtct.gui.batch.be_substitution"));
        // Management header toggles (WCWT manage_display_mode / manage_displays_slots /
        // automatic_upload-search-mode / the icon-less automatic-upload switch), each a 14x14
        // checkbox plate; the first three carry their own icon.
        this.buttonList.add(
            this.mgmtDisplayToggle = new GuiWcwtMgmtToggle(
                this.guiLeft + MGMT_TOGGLE_DISPLAY_X,
                mgmtChromeY(MGMT_TOGGLE_TEX_Y),
                MGMT_ICON_DISPLAY_ALL_U,
                MGMT_ICON_DISPLAY_ALL_V,
                16));
        this.buttonList.add(
            this.mgmtSlotsToggle = new GuiWcwtMgmtToggle(
                this.guiLeft + MGMT_TOGGLE_SLOTS_X,
                mgmtChromeY(MGMT_TOGGLE_TEX_Y),
                MGMT_ICON_SLOTS_ON_U,
                MGMT_ICON_SLOTS_V,
                16));
        this.buttonList.add(
            this.mgmtSearchModeToggle = new GuiWcwtMgmtToggle(
                this.guiLeft + MGMT_TOGGLE_SEARCHMODE_X,
                mgmtChromeY(MGMT_TOGGLE_TEX_Y),
                MGMT_ICON_SEARCH_OUT_U,
                MGMT_ICON_SEARCH_OUT_V,
                12));
        // WCWT's automatic-upload switch sits last in that row and carries no icon: it is the plain
        // radio plate, checked while the switch is on. Being a real button it gets the same hover
        // tooltip as the three beside it - WCWT's own hint for it.
        this.buttonList.add(
            this.mgmtAutoUploadToggle = new GuiWcwtMgmtToggle(
                this.guiLeft + MGMT_AUTOUPLOAD_X,
                mgmtChromeY(MGMT_AUTOUPLOAD_TEX_Y),
                0,
                0,
                0));
        // Our own enlargement switch, out in the sidebar's strip beside the magnet/trash/settings
        // plates rather than in the management row: a real toggle (the checked plate while the list is
        // pulled up), so it reads as "on/off" rather than as one more cycler. It keeps the strip's own
        // rest position too - the plates beside it do not sink while they are merely "on" (they have
        // no such state), so this one sinks only under the cursor or a held button, like they do.
        this.mgmtExpandToggle = new GuiWcwtMgmtToggle(
            this.guiLeft + MGMT_TOGGLE_EXPAND_X,
            frameY(MGMT_TOGGLE_EXPAND_TEX_Y),
            0,
            0,
            0);
        this.mgmtExpandToggle.setSinkWhenChecked(false);
        this.buttonList.add(this.mgmtExpandToggle);
        updateMgmtToggles();
        // Encoding option buttons: WCWT lays them onto the encoding panel's second row and the X
        // follows the mode (see the ENC_OPT_* constants); updateEncodingOptionButtons() re-applies
        // the X every frame, so the values below are only the crafting-mode start positions.
        // Clear and substitution use AE2 1.21's own small icons so they look like WCWT's buttons.
        final int optY = frameY(ENC_OPT_ROW_Y);
        this.buttonList.add(
            this.encClearBtn = new GuiWcwtStateButton(
                this.guiLeft + ENC_OPT_CLEAR_CRAFT_X,
                optY,
                8,
                ICON_CLEAR_U,
                ICON_CLEAR_V,
                "wtct.gui.pattern.clear",
                "wtct.gui.pattern.clear.desc"));
        this.buttonList.add(
            this.substBtn = new GuiWcwtStateButton(
                this.guiLeft + ENC_OPT_SUBST_X,
                optY,
                ICON_SUBST_ON_U,
                ICON_SUBST_ON_V,
                ICON_SUBST_OFF_U,
                ICON_SUBST_OFF_V,
                "wtct.gui.pattern.subst.on",
                "wtct.gui.pattern.subst.desc.on",
                "wtct.gui.pattern.subst.off",
                "wtct.gui.pattern.subst.desc.off"));
        // WCWT's third crafting column is its fluid substitution, which 1.7.10 has no equivalent
        // for - AE2's be-substitution (kept in the 1.7.10 style) takes that slot instead.
        if (ModAndClassUtil.isBeSubstitutionsButton) {
            this.buttonList.add(
                this.beSubstOnBtn = new GuiImgButton(
                    this.guiLeft + ENC_OPT_BESUBST_X,
                    optY,
                    Settings.ACTIONS,
                    PatternBeSubstitution.ENABLED));
            this.beSubstOnBtn.setHalfSize(true);
            this.buttonList.add(
                this.beSubstOffBtn = new GuiImgButton(
                    this.guiLeft + ENC_OPT_BESUBST_X,
                    optY,
                    Settings.ACTIONS,
                    PatternBeSubstitution.DISABLED));
            this.beSubstOffBtn.setHalfSize(true);
        }
        // GTNH AE's "merge identical materials" toggle (the extended pattern terminal's option, ported
        // as NEIUtils.compress) is processing mode's second column, exactly where WCWT's runtime layout
        // puts its merge button (clear + 10px), while the substitution pair hides. It only affects NEI
        // recipe transfers - it never rewrites the encoder under the player's hands.
        this.buttonList.add(
            this.mergeBtn = new GuiWcwtStateButton(
                this.guiLeft + ENC_OPT_MERGE_X,
                optY,
                ICON_MERGE_ON_U,
                ICON_MERGE_V,
                ICON_MERGE_OFF_U,
                ICON_MERGE_V,
                "wtct.gui.pattern.merge.on",
                "wtct.gui.pattern.merge.desc.on",
                "wtct.gui.pattern.merge.off",
                "wtct.gui.pattern.merge.desc.off"));
        // WCWT's cycle-output button: 8x8 S_CYCLE sprite at the encoding origin + (90, 6), the only
        // extra control its encoding area carries.
        this.buttonList.add(
            this.cycleOutputBtn = new GuiWcwtStateButton(
                this.guiLeft + ENC_OPT_CYCLE_X,
                frameY(ENC_OPT_ROW_Y),
                8,
                CYCLE_ICON_U,
                CYCLE_ICON_V,
                "wtct.gui.pattern.cycle.title",
                "wtct.gui.pattern.cycle.desc"));
        updateEncodingOptionButtons();

        repositionFixedSlots();
        repositionPlayerSlots();
        // Force the encoding area to be re-placed: initGui can run again on the same instance (returning
        // from a NEI overlay or a sub-screen), and the early-out in layoutPatternArea would otherwise
        // keep whatever positions the previous pass left behind.
        this.patternAreaLaidOut = false;
        // Positions the encoding area for the current mode (and the tabs with it).
        updateModeTabs();
        // Vanilla text fields blink their caret from this counter; the management fields need it too.
        if (this.mgmtSearch != null) {
            this.mgmtSearch.updateCursorCounter();
        }
        if (this.mgmtMappingField != null) {
            this.mgmtMappingField.updateCursorCounter();
        }
        layoutPatternCache();
        setupUpgradePanel();
        setupCardButtons();
    }

    /**
     * The configurable buttons, in the user's layout: the import/export card buttons as sidebar
     * buttons in the left toolbar (the addon's own arrangement), and the magnet, trash and terminal
     * settings buttons in the horizontal strip between the crafting area and the multiplier row.
     * Only the block picker keeps a header cell. Each button is visible only while its card is
     * installed - the same "the card is the button" contract WCWT uses - except the trash and
     * settings buttons, which are terminal features and always show.
     */
    private void setupCardButtons() {
        final ItemStack terminal = this.monitorableContainer.getTerminalStack();
        // The sidebar's first free row. It is measured from the buttons that are already in the
        // column rather than from GuiMonitor's offsetY: this method runs after initGui re-anchored the
        // window ("dy"), so the settings column has moved while offsetY still holds its pre-shift
        // value - taking it verbatim used to drop these buttons a row below the last plate.
        int nextY = this.nextSidebarRowY();
        this.cardRowBaseY = nextY;
        this.cardImportBtn = new GuiWcwtSidebarIconButton(
            this.toolbarX(),
            nextY,
            cardIcon("card_icon_import"),
            StatCollector.translateToLocal("item.wcwt_card_import.name"));
        this.buttonList.add(this.cardImportBtn);
        nextY += GuiWcwtToolbarButton.ROW_PITCH;
        this.cardExportBtn = new GuiWcwtSidebarIconButton(
            this.toolbarX(),
            nextY,
            cardIcon("card_icon_export"),
            StatCollector.translateToLocal("item.wcwt_card_export.name"));
        this.buttonList.add(this.cardExportBtn);
        nextY += GuiWcwtToolbarButton.ROW_PITCH;
        // The addon puts all three of its card buttons in the left toolbar (its UpgradeItemButton is
        // AE2 1.21's IconButton with the card's own icon), so the block picker's joins them here.
        this.cardPickerBtn = new GuiWcwtSidebarIconButton(
            this.toolbarX(),
            nextY,
            cardIcon("card_icon_block_picker"),
            pickerTooltip(terminal));
        this.buttonList.add(this.cardPickerBtn);
        nextY += GuiWcwtToolbarButton.ROW_PITCH;
        // The auto-fill switch closes the card group. It is a terminal feature rather than a card's, so
        // it stays in the column whether or not any card is fitted (the trash and settings plates are
        // always there for the same reason).
        this.autoFillBtn = new GuiWcwtAutoFillButton(this.toolbarX(), nextY, autoFillTooltip(false));
        this.buttonList.add(this.autoFillBtn);
        this.offsetY = nextY + GuiWcwtToolbarButton.ROW_PITCH;
        // The column is longer now, so the sidebar's surface and its closing border have to follow it.
        this.refreshSidebarBottom();

        // The strip's three buttons: AE2's checkbox radio as the plate with the card's own icon on
        // top, at native resolution and centred - the plate is the largest one that fits between the
        // strip's two white lines.
        this.buttonList.add(
            this.cardMagnetBtn = new GuiWcwtStripIconButton(
                this.guiLeft + STRIP_MAGNET_X,
                frameY(STRIP_BTN_Y),
                cardIcon("card_icon_magnet"),
                magnetTooltip(terminal)));
        this.buttonList.add(
            this.trashBtn = new GuiWcwtStripIconButton(
                this.guiLeft + STRIP_TRASH_X,
                frameY(STRIP_BTN_Y),
                cardIcon("card_icon_trash"),
                StatCollector.translateToLocal("gui.ae2wtlib.trash")));
        this.buttonList.add(
            this.settingsBtn = new GuiWcwtStripIconButton(
                this.guiLeft + STRIP_SETTINGS_X,
                frameY(STRIP_BTN_Y),
                cardIcon("wcwt_icon_cog"),
                StatCollector.translateToLocal("gui.ae2wtlib.wireless_terminal_settings_title")));
        updateCardButtons();
    }

    private static ResourceLocation cardIcon(final String name) {
        return new ResourceLocation(Wtct.MODID, "textures/guis/wcwt/" + name + ".png");
    }

    /** The card buttons follow the terminal's card column, which can change while the screen is open. */
    private void updateCardButtons() {
        if (this.cardImportBtn == null) {
            return;
        }
        final ItemStack terminal = this.monitorableContainer.getTerminalStack();
        this.cardImportBtn.visible = ItemWcwtUpgradeCard.isInstalled(terminal, ItemWcwtUpgradeCard.Kind.IMPORT);
        this.cardExportBtn.visible = ItemWcwtUpgradeCard.isInstalled(terminal, ItemWcwtUpgradeCard.Kind.EXPORT);
        this.cardMagnetBtn.visible = ItemWcwtUpgradeCard.isInstalled(terminal, ItemWcwtUpgradeCard.Kind.MAGNET);
        this.cardPickerBtn.visible = ItemWcwtUpgradeCard.isInstalled(terminal, ItemWcwtUpgradeCard.Kind.BLOCK_PICKER);
        if (this.cardMagnetBtn.visible) {
            this.cardMagnetBtn.setTooltip(magnetTooltip(terminal));
        }
        if (this.cardPickerBtn.visible) {
            this.cardPickerBtn.setTooltip(pickerTooltip(terminal));
        }
        this.autoFillBtn.setOn(this.autoFillEnabled());
        this.autoFillBtn.setTooltip(autoFillTooltip(this.autoFillBtn.isOn()));
        this.packCardButtons();
    }

    /** The auto-fill switch's name and state, the way the reference button describes itself. */
    private String autoFillTooltip(final boolean on) {
        return StatCollector.translateToLocal("wtct.settings.auto_fill") + ": "
            + StatCollector.translateToLocal(on ? "wtct.gui.auto_fill.on" : "wtct.gui.auto_fill.off");
    }

    /**
     * Gives every fitted card one row, starting at the column's first free row. A card that is taken
     * out of the terminal leaves no empty plate behind: the buttons under it move up and the
     * sidebar's surface follows them (see {@link #refreshSidebarBottom()}), so the column is exactly
     * as long as the buttons it holds.
     */
    private void packCardButtons() {
        int y = this.cardRowBaseY;
        for (final GuiWcwtSidebarIconButton btn : new GuiWcwtSidebarIconButton[] { this.cardImportBtn,
            this.cardExportBtn, this.cardPickerBtn, this.autoFillBtn }) {
            if (!btn.visible) {
                continue;
            }
            btn.yPosition = y;
            y += GuiWcwtToolbarButton.ROW_PITCH;
        }
        this.refreshSidebarBottom();
    }

    private String magnetTooltip(final ItemStack terminal) {
        final CardTicker.MagnetMode mode = CardTicker.MagnetMode.of(terminal);
        final String key = switch (mode) {
            case INV -> "wtct.card.magnet.mode.inv";
            case ME -> "wtct.card.magnet.mode.me";
            default -> "wtct.card.magnet.mode.off";
        };
        return StatCollector.translateToLocal("item.wcwt_card_magnet.name") + ": "
            + StatCollector.translateToLocal(key)
            + "\n"
            + StatCollector.translateToLocal("wtct.card.magnet.open");
    }

    private String pickerTooltip(final ItemStack terminal) {
        return StatCollector.translateToLocal("item.wcwt_card_block_picker.name") + ": "
            + CardTicker.pickerAmount(terminal);
    }

    /**
     * Builds the right-hand upgrade panel and lays its slots out for the terminal's current height.
     *
     * <p>
     * WCWT's own numbers: the panel is anchored at {@code right 2, top 0} and gets
     * {@code setMaxRows(Math.max(2, getVisibleRows()))}, so the smallest terminal shows the
     * singularity slot plus two card slots and a taller one shows more of the column, scrolling for
     * whatever is left over.
     */
    private void setupUpgradePanel() {
        this.upgradePanel = new GuiWcwtUpgradePanel(
            this.monitorableContainer.getUpgradeSingularitySlot(),
            this.monitorableContainer.getUpgradeCardSlots(),
            ContainerComprehensiveWorkTerminal.UPGRADE_PANEL_X,
            ContainerComprehensiveWorkTerminal.UPGRADE_PANEL_Y,
            // WCWT only shows the (empty) singularity slot once a quantum bridge card is fitted; the
            // card is what gives the singularity something to talk to.
            () -> ItemWcwtUpgradeCard
                .isInstalled(this.monitorableContainer.getTerminalStack(), ItemWcwtUpgradeCard.Kind.QUANTUM_BRIDGE));
        this.upgradePanel.setMaxRows(Math.max(2, this.rows));
        // setMaxRows only re-lays out when the value really changed, so place the column once here.
        this.upgradePanel.layout();
    }

    /**
     * WCWT behaviour: both tabs stay on screen and the active mode is the one marked as selected, so
     * the tabs never move and a click always means "switch to that mode".
     */
    private void updateModeTabs() {
        if (this.tabCraftButton == null) {
            return;
        }
        final boolean crafting = this.monitorableContainer.craftingMode;
        this.tabCraftButton.setSelected(crafting);
        this.tabProcessButton.setSelected(!crafting);
        layoutPatternArea();
    }

    /**
     * Keeps the encoding option icons in step with the container's synced options: the substitution
     * and be-substitution toggles show only in crafting mode and swap sprites with their state, the
     * merge toggle - which only means something for processing patterns - follows the mode, and the
     * whole row is re-anchored to the column WCWT uses for that mode.
     */
    private void updateEncodingOptionButtons() {
        if (this.substBtn == null) {
            return;
        }
        // WCWT shows the substitution toggles in crafting mode only and swaps them for the merge
        // toggle in processing mode; the clear button stays in both - but 9px further right in
        // processing mode, with the merge toggle 10px right of it.
        final boolean crafting = this.monitorableContainer.craftingMode;
        this.encClearBtn.xPosition = this.guiLeft + (crafting ? ENC_OPT_CLEAR_CRAFT_X : ENC_OPT_CLEAR_PROCESS_X);
        this.substBtn.xPosition = this.guiLeft + ENC_OPT_SUBST_X;
        this.mergeBtn.xPosition = this.guiLeft + ENC_OPT_MERGE_X;
        if (this.beSubstOnBtn != null) {
            this.beSubstOnBtn.xPosition = this.guiLeft + ENC_OPT_BESUBST_X;
            this.beSubstOffBtn.xPosition = this.guiLeft + ENC_OPT_BESUBST_X;
        }
        this.substBtn.visible = crafting;
        this.substBtn.setToggled(this.monitorableContainer.substitute);
        if (this.beSubstOnBtn != null) {
            final boolean beSubstitute = this.monitorableContainer.beSubstitute;
            this.beSubstOnBtn.setVisibility(crafting && beSubstitute);
            this.beSubstOffBtn.setVisibility(crafting && !beSubstitute);
        }
        if (this.mergeBtn != null) {
            this.mergeBtn.visible = !crafting;
            this.mergeBtn.setToggled(this.monitorableContainer.combine);
        }
        if (this.cycleOutputBtn != null) {
            // WCWT: visible only while processing and more than one output is set.
            this.cycleOutputBtn.visible = !crafting && this.filledOutputCount() > 1;
        }
        // The two batch switches are the encoder's options as well, so their state comes from the
        // container's synced values instead of a client-only field that could sit on while the server
        // still held the old one.
        if (this.batchItemRadio != null) {
            this.batchItemRadio.setSelected(this.monitorableContainer.substitute);
        }
        if (this.batchBeSubRadio != null) {
            this.batchBeSubRadio.setSelected(this.monitorableContainer.beSubstitute);
        }
    }

    /** Number of non-empty processing outputs, mirroring WCWT's canCycleProcessingOutputs. */
    private int filledOutputCount() {
        int filled = 0;
        for (final SlotFake s : this.monitorableContainer.getOutputSlots()) {
            if (s.getStack() != null) {
                filled++;
            }
        }
        return filled;
    }

    /** How many rows the encoding scrollbar can page through (0 when everything fits). */
    private int maxEncScroll() {
        return Math.max(0, ContainerComprehensiveWorkTerminal.ENC_INPUT_SLOTS / 3 - ENC_VISIBLE_ROWS);
    }

    /**
     * Lays the encoding area out for the current mode, mirroring WCWT: the crafting matrix plus its
     * recipe preview belong to crafting mode, the scrollable inputs/outputs to processing mode. The
     * two groups use nearly the same columns - the crafting matrix sits 9px further left - and only
     * one of them is ever on screen, which is what keeps the crafting result slot (x=274) and the
     * processing output column (x=277) from overlapping.
     */
    private void layoutPatternArea() {
        final boolean crafting = this.monitorableContainer.craftingMode;
        final int scroll = crafting ? 0 : this.encScroll;
        final int bottom = this.bottomStartRel();
        // This runs from the per-frame pass as well as from the wheel and from initGui. Re-placing the
        // whole area only matters when one of the inputs the placement is built from actually moved -
        // the walk itself, which rewrites every encoding cell's position, was the stutter. Nothing
        // below can change on its own: the slot arrays only swap with the mode, and the mode is part
        // of the key.
        if (this.patternAreaLaidOut && crafting == this.laidOutCrafting
            && scroll == this.laidOutScroll
            && bottom == this.laidOutBottomStart) {
            return;
        }
        // WCWT offsets the crafting matrix 9px left of the processing input column (ENC_CRAFT_X).
        final int inputX = crafting ? ENC_CRAFT_X : ENC_MATRIX_X;

        final SlotFake[] inputs = this.monitorableContainer.getEncodingSlots();
        for (int i = 0; i < inputs.length; i++) {
            final int shownRow = i / 3 - scroll;
            final boolean visible = shownRow >= 0 && shownRow < ENC_VISIBLE_ROWS;
            placeSlot(inputs[i], visible, inputX + (i % 3) * ROW_H, ENC_ROW_Y + shownRow * ROW_H);
        }

        // The outputs follow the same scroll offset (WCWT: one scrollbar drives both columns), so a
        // processing pattern with more than three outputs pages through them with the wheel.
        final SlotFake[] outputs = this.monitorableContainer.getOutputSlots();
        for (int i = 0; i < outputs.length; i++) {
            final int shownRow = i - scroll;
            final boolean visible = !crafting && shownRow >= 0 && shownRow < ENC_VISIBLE_ROWS;
            placeSlot(outputs[i], visible, ENC_OUTPUT_X, ENC_ROW_Y + shownRow * ROW_H);
        }
        placeSlot(this.monitorableContainer.getCraftingResultSlot(), crafting, ENC_RESULT_X, ENC_RESULT_Y);

        // Clamp in case the mode or the window changed while scrolled.
        if (this.encScroll > this.maxEncScroll()) {
            this.encScroll = this.maxEncScroll();
        }
        if (this.encScroll < 0) {
            this.encScroll = 0;
        }
        this.patternAreaLaidOut = true;
        this.laidOutCrafting = crafting;
        this.laidOutScroll = crafting ? 0 : this.encScroll;
        this.laidOutBottomStart = bottom;
    }

    /** The inputs {@link #layoutPatternArea} last placed the area with; see its early-out. */
    private boolean patternAreaLaidOut;
    private boolean laidOutCrafting;
    private int laidOutScroll;
    private int laidOutBottomStart = Integer.MIN_VALUE;

    /** The encoding scrollbar belongs to processing mode and only shows when rows are hidden. */
    private boolean isEncScrollbarVisible() {
        return !this.monitorableContainer.craftingMode && this.maxEncScroll() > 0;
    }

    /** Puts a slot at a texture position inside the bottom slice, or parks it off-screen. */
    private void placeSlot(final Slot slot, final boolean visible, final int textureX, final int textureY) {
        if (slot == null) {
            return;
        }
        if (visible) {
            slot.xDisplayPosition = textureX;
            slot.yDisplayPosition = bottomStartRel() + (textureY - REF_BOTTOM_TOP);
        } else {
            slot.xDisplayPosition = HIDDEN_SLOT;
            slot.yDisplayPosition = HIDDEN_SLOT;
        }
    }

    /** True while the cursor is over the encoding panel (absolute screen coordinates). */
    private boolean isInEncodingArea(final int mouseX, final int mouseY) {
        final int left = this.guiLeft + ENC_PANEL_X;
        final int top = this.guiTop + bottomStartRel() + (ENC_PANEL_Y - REF_BOTTOM_TOP);
        return mouseX >= left && mouseX < left + ENC_PANEL_W && mouseY >= top && mouseY < top + ENC_PANEL_H;
    }

    /**
     * The mouse wheel pages the encoding inputs while the cursor is over the encoding panel, and
     * scrolls the ME item list everywhere else (WCWT behaves the same way on 1.21).
     */
    @Override
    protected boolean mouseWheelEvent(final int x, final int y, final int wheel) {
        // Shift+wheel over a pattern slot is NEI's compatible-item/fluid picker (the cell's own
        // tooltip lists what it accepts). The picker claims exactly those slots, so the wheel is
        // declined here instead of being spent on a list: letting this screen scroll its own rows
        // under the cursor made the cells - the pattern area, or the cache a pattern was being
        // picked from - jump away while the player was choosing what goes into the cell.
        if (isShiftKeyDown() && this.getSlotAtPosition(x, y) instanceof SlotFake) {
            return false;
        }
        // The panel sits outside the terminal, so nothing else can claim the wheel there.
        if (this.upgradePanel != null && this.upgradePanel.maxScroll() > 0
            && this.upgradePanel.contains(x, y, this.guiLeft, this.guiTop)) {
            // Wheel up (positive) moves towards the first slots, like every other list in the game.
            this.upgradePanel.scrollBy(-wheel);
            return true;
        }
        // The terminal's own item list is what the left column belongs to: AEBaseGui only answers the
        // wheel while the pointer is inside the item scrollbar's own rows (its position test is the
        // scrollbar's top and height), so a wheel over the crafting grid, the multiplier panel or the
        // player inventory - everything left of the management column - never reached it and died.
        // Hand it to that scrollbar explicitly instead; this is the exact call its own test would
        // have made, just without the row bounds.
        if (x > this.guiLeft && x < this.guiLeft + MGMT_X) {
            final GuiScrollbar items = this.getScrollBar();
            if (items != null) {
                items.wheel(wheel);
                return true;
            }
        }
        if (this.maxMgmtScroll() > 0 && this.isInMgmtWheelArea(x, y)) {
            this.mgmtScroll = Math.max(0, Math.min(this.maxMgmtScroll(), this.mgmtScroll - wheel * MGMT_ROW_H));
            return true;
        }
        if (this.maxCacheScroll() > 0 && this.isInCacheArea(x, y)) {
            // Wheel up (positive) moves towards the first rows, like every other list in the game.
            this.cacheScroll = Math.max(0, Math.min(this.maxCacheScroll(), this.cacheScroll - wheel));
            layoutPatternCache();
            return true;
        }
        if (this.isEncScrollbarVisible() && this.isInEncodingArea(x, y)) {
            // Wheel up (positive) moves towards the first rows, like every other list in the game.
            this.encScroll = Math.max(0, Math.min(this.maxEncScroll(), this.encScroll - wheel));
            layoutPatternArea();
            return true;
        }
        return super.mouseWheelEvent(x, y, wheel);
    }

    /** Grab the encoding scrollbar: a click jumps to that spot, then continues as a drag. */
    /**
     * A switch of the wireless terminal settings screen, read from the terminal item it writes them to
     * (see {@link ContainerWcwtSettings}).
     */
    private boolean switchOn(final String key, final boolean defaultValue) {
        final ItemStack terminal = this.monitorableContainer == null ? null
            : this.monitorableContainer.getTerminalStack();
        if (terminal == null) {
            return defaultValue;
        }
        final NBTTagCompound tag = Platform.openNbtData(terminal);
        return tag.hasKey(key) ? tag.getBoolean(key) : defaultValue;
    }

    /** True when this terminal draws its item counts in AE2's large font. */
    private boolean bigCounts() {
        return this.switchOn(ContainerWcwtSettings.KEY_BIG_COUNTS, false);
    }

    /** True when an upload that could not name a single provider puts its pattern into the cache. */
    private boolean stashAmbiguousUpload() {
        return this.switchOn(ContainerWcwtSettings.KEY_STASH_AMBIGUOUS, false);
    }

    /**
     * True when only a provider search naming exactly one provider uploads. It is a switch of the
     * wireless terminal's settings screen, and it defaults to on - which is how the upload behaved
     * before the switch existed.
     */
    private boolean uniqueMatchUploadOnly() {
        return this.switchOn(ContainerWcwtSettings.KEY_UPLOAD_UNIQUE_MATCH, true);
    }

    /**
     * Every stack size AE2 draws comes out of {@code StackSizeRenderer}, which reads the font size from
     * AE2's own client setting - and that setting's default is the small one. With the terminal's switch
     * on, the large setting is lent to the renderer for the duration of the frame (AE2's large font also
     * brings its slim 1k/1M numbers), and put back afterwards so the player's AE2 configuration is never
     * touched.
     */
    @Override
    public void drawScreen(final int mouseX, final int mouseY, final float partialTicks) {
        // TEMP DIAGNOSTIC (1.0.37, remove once the terminal-open delay is pinned down).
        final boolean diag = this.diagFrames < 30;
        final long diagT0 = System.currentTimeMillis();
        final TerminalFontSize configured = AEConfig.instance.getTerminalFontSize();
        final boolean lend = this.bigCounts() && configured != TerminalFontSize.LARGE;
        if (lend) {
            AEConfig.instance.settings.putSetting(Settings.TERMINAL_FONT_SIZE, TerminalFontSize.LARGE);
        }
        try {
            super.drawScreen(mouseX, mouseY, partialTicks);
        } finally {
            if (lend) {
                AEConfig.instance.settings.putSetting(Settings.TERMINAL_FONT_SIZE, configured);
            }
            if (diag) {
                cpw.mods.fml.common.FMLLog.info(
                    "[wtct-diag] client drawScreen frame=%d took=%dms t=%d",
                    this.diagFrames,
                    System.currentTimeMillis() - diagT0,
                    diagT0);
                this.diagFrames++;
            }
            // TEMP DIAGNOSTIC (1.0.41, remove with the rest): beyond the first 30 frames a running
            // frame summary, so a steady state that is merely slow shows up too.
            final long diagFrameMs = System.currentTimeMillis() - diagT0;
            this.diagAllFrames++;
            this.diagAllTotal += diagFrameMs;
            if (diagFrameMs > this.diagAllMax) this.diagAllMax = diagFrameMs;
            if (this.diagAllFrames % 100 == 0) {
                cpw.mods.fml.common.FMLLog.info(
                    "[wtct-diag] client frame summary frames=%d avg=%dms max=%dms t=%d",
                    this.diagAllFrames,
                    this.diagAllTotal / this.diagAllFrames,
                    this.diagAllMax,
                    diagT0);
            }
        }
    }

    /**
     * The upgrade panel hangs outside the window's own rectangle (WCWT anchors it at {@code right 2} of a
     * 355px window), so a click on it counts as a click <em>outside the window</em> - and vanilla's answer
     * to those is to throw whatever the cursor is carrying onto the ground. Picking a card out of the
     * column and clicking the panel's own background once was therefore enough to lose the card.
     *
     * <p>
     * The window rectangle is widened over the panel for the duration of the click, which is the same
     * effect AE2 1.21 gets from giving the panel its own screen bounds.
     */
    @Override
    protected void mouseClicked(final int mouseX, final int mouseY, final int mouseButton) {
        final int xSize = this.xSize;
        final int ySize = this.ySize;
        if (this.upgradePanel != null) {
            this.xSize = Math.max(
                this.xSize,
                ContainerComprehensiveWorkTerminal.UPGRADE_PANEL_X + this.upgradePanel.boundsWidth() + 1);
            this.ySize = Math.max(this.ySize, this.upgradePanel.boundsHeight());
        }
        try {
            this.mouseClickedInside(mouseX, mouseY, mouseButton);
        } finally {
            this.xSize = xSize;
            this.ySize = ySize;
        }
    }

    /** The click handling itself; {@link #mouseClicked} only widens the window around it. */
    private void mouseClickedInside(final int mouseX, final int mouseY, final int mouseButton) {
        // The fluid gesture first, before AE2's own routing gets a chance at the click. AE2 hands a
        // click its ME-grid lookup claims to the grid, so a Ctrl+click with a bucket over an encoder
        // cell never reached the cell at all - the click was logged as "no slot" and the bucket went
        // into the network instead of marking its water.
        if (isCtrlKeyDown() && this.markFluidUnderCursor(mouseX, mouseY, mouseButton)) {
            return;
        }
        // Middle click on a processing cell opens the amount entry, the way GTNH's pattern terminal
        // does it: AE2FC's GuiPatternValueAmount, reached through InventoryActionExtend.SET_PATTERN_VALUE
        // - the very action AE2Things' own PatternPanel sent for this button. Only processing mode's
        // cells carry amounts, so those are the only ones served.
        if (mouseButton == 2) {
            final Slot hovered = this.getSlotAtPosition(mouseX, mouseY);
            if (hovered != null && this.monitorableContainer.canSetCellAmount(hovered) && hovered.getStack() != null) {
                this.openPatternValuePanel(hovered);
                return;
            }
        }
        // The management header switches (显示模式 / 显示样板槽 / 搜索范围 / 自动上传) sit over the
        // terminal's bottom slice, where a click never reached the button list - the scope switch looked
        // simply dead: no icon change, no tooltip change, no list change. They are hit-tested here, at the
        // top of the click chain, the way the mapping plates and the list scrollbar already are.
        if (mouseButton == 0 && this.handleMgmtToggleClick(mouseX, mouseY)) {
            return;
        }
        // WCWT drops the field focus on any click outside the two boxes, so the caret never sticks.
        if (mouseButton == 0) {
            if (this.mgmtSearch != null && !this.isOnMgmtSearch(mouseX, mouseY)) {
                this.mgmtSearch.setFocused(false);
            }
            if (this.mgmtMappingField != null && !this.isOnMgmtMapping(mouseX, mouseY)) {
                this.mgmtMappingField.setFocused(false);
            }
        }
        // WCWT empties whichever box a right-click lands on - the fields carry no other right-click
        // action, and the click stops there rather than reaching the container underneath.
        if (mouseButton == 1) {
            if (this.mgmtSearch != null && this.isOnMgmtSearch(mouseX, mouseY)) {
                this.mgmtSearch.setText("");
                this.mgmtRowsDirty = true;
                return;
            }
            if (this.mgmtMappingField != null && this.isOnMgmtMapping(mouseX, mouseY)) {
                this.mgmtMappingField.setText("");
                return;
            }
        }
        if (mouseButton == 0 && this.handleMappingClick(mouseX, mouseY)) {
            return;
        }
        if (mouseButton == 0 && this.mgmtMappingField != null && this.isOnMgmtMapping(mouseX, mouseY)) {
            this.mgmtSearch.mouseClicked(mouseX, mouseY, mouseButton); // drop the other focus
            this.mgmtMappingField.mouseClicked(mouseX, mouseY, mouseButton);
            return;
        }
        if (mouseButton == 0 && this.mgmtSearch != null && this.isOnMgmtSearch(mouseX, mouseY)) {
            // 1.7.10's GuiTextField#mouseClicked takes the focus itself and returns nothing.
            this.mgmtSearch.mouseClicked(mouseX, mouseY, mouseButton);
            this.mgmtRowsDirty = true;
            return;
        }
        if (mouseButton == 0) {
            if (this.maxMgmtScroll() > 0 && this.isOnMgmtScrollbar(mouseX, mouseY)) {
                this.draggingMgmtKnob = true;
                this.dragMgmtScrollTo(mouseY);
                return;
            }
            if (this.handleMgmtClick(mouseX, mouseY, isShiftKeyDown())) {
                return;
            }
            if (this.maxCacheScroll() > 0 && this.isOnCacheScrollbar(mouseX, mouseY)) {
                this.draggingCacheKnob = true;
                this.dragCacheScrollTo(mouseY);
                return;
            }
            if (this.isEncScrollbarVisible() && this.isOnEncScrollbar(mouseX, mouseY)) {
                this.draggingEncKnob = true;
                this.dragEncScrollTo(mouseY);
                return;
            }
            if (this.isOnUpgradeScrollbar(mouseX, mouseY)) {
                this.draggingUpgradeKnob = true;
                this.upgradePanel.dragTo(mouseY, this.guiTop, ENC_KNOB_H);
                return;
            }
        }
        // The last way into a slot: nothing else here reaches one. While one of openOnTop's dialogs is
        // being finished with, the click that closed it must not land on whatever cell the pointer is
        // over - which is the vanilla path AE2 answers by setting or clearing a fake slot, so it needs
        // the same guard as the drag paths. See dragSuppressed.
        if (this.dragSuppressed()) {
            return;
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void keyTyped(final char character, final int key) {
        // Escape / Enter leave the fields, the way vanilla's EditBox does.
        if (key == org.lwjgl.input.Keyboard.KEY_ESCAPE || key == org.lwjgl.input.Keyboard.KEY_RETURN) {
            if (this.mgmtSearch != null && this.mgmtSearch.isFocused()) {
                this.mgmtSearch.setFocused(false);
                return;
            }
            if (this.mgmtMappingField != null && this.mgmtMappingField.isFocused()) {
                // Enter commits what the box holds: a word the table knows as a key fills the provider
                // search box, anything else is the binding to record - the same action as the 增加映射
                // plate, so a mapping can be authored without leaving the keyboard. (Before this, Enter
                // only dropped the caret, which made the field read as "it does nothing".)
                if (!this.autoFillProviderSearchFromMappingField()) {
                    this.applyMappingAdd();
                }
                this.mgmtMappingField.setFocused(false);
                return;
            }
        }
        if (this.mgmtMappingField != null && this.mgmtMappingField.isFocused()
            && this.mgmtMappingField.textboxKeyTyped(character, key)) {
            // Every keystroke re-checks the mapping table, so a key typed here fills the provider
            // search box as it is typed - the fill is the point, not a side effect of some later step.
            this.autoFillProviderSearchFromMappingField();
            return;
        }
        if (this.mgmtSearch != null && this.mgmtSearch.isFocused() && this.mgmtSearch.textboxKeyTyped(character, key)) {
            this.mgmtRowsDirty = true;
            return;
        }
        // Getting past the fields above means this key really is about to close the screen. GuiContainer
        // answers both ESC and the inventory key by sending its close-window packet first, so the clear
        // has to be asked for ahead of it - see clearGridOnClose.
        if (key == org.lwjgl.input.Keyboard.KEY_ESCAPE || key == this.mc.gameSettings.keyBindInventory.getKeyCode()) {
            this.clearGridOnClose();
        }
        super.keyTyped(character, key);
    }

    /**
     * Holds the mouse off this terminal's slots while one of {@link #openOnTop}'s dialogs is being finished with.
     *
     * <p>
     * AE2 fills or clears the fake slot under the pointer on <em>every</em> mouse path - dragging with any button
     * ({@code AEBaseGui#mouseClickMove}), clicking one ({@code handleMouseClick}) and NEI's item drag
     * ({@code handleDragNDrop}, through {@code INEIGuiHandler}) - and all of them end in
     * {@code handleClickOrDragSlot}; the right button, or an empty cursor, means "remove one". NEI hands its drags
     * to the containing {@code GuiContainer}, which is this terminal even while a menu-less dialog is the screen on
     * top, so the palette stayed live through the amount dialog and painted whatever cell the pointer crossed. Since
     * the middle click that opens that dialog is usually still held when it closes, nothing may reach a slot until
     * the mouse is free again: while a button is down, or for the short tick budget the dialog leaves behind.
     *
     * @return true when the caller must not act on the slot it was handed
     */
    private boolean dragSuppressed() {
        if (!this.ignoreDragUntilRelease) {
            return false;
        }
        final boolean held = Mouse.isButtonDown(0) || Mouse.isButtonDown(1) || Mouse.isButtonDown(2);
        if (!held && this.dragSuppressTicks <= 0) {
            this.ignoreDragUntilRelease = false;
            return false;
        }
        if (held) {
            this.dragSuppressTicks = 10;
        }
        return true;
    }

    /** The chokepoint every AE2 mouse path that fills or clears a fake slot goes through. */
    @Override
    protected boolean handleClickOrDragSlot(final Slot slot, final ItemStack stack, final int mouseButton) {
        if (this.dragSuppressed()) {
            return true;
        }
        // The middle button paints nothing, dragging with it included: it is the amount dialog's button
        // and nothing else, so a drag it starts must not set or clear the cells the pointer crosses -
        // see handlePatternCellFluidClick.
        if (mouseButton == 2) {
            return true;
        }
        return super.handleClickOrDragSlot(slot, stack, mouseButton);
    }

    /** NEI's item drag - NEI targets the containing GuiContainer, so a menu-less dialog on top does not stop it. */
    @Override
    public boolean handleDragNDrop(final GuiContainer gui, final int mouseX, final int mouseY,
        final ItemStack draggedStack, final int button) {
        if (this.dragSuppressed()) {
            return true;
        }
        return super.handleDragNDrop(gui, mouseX, mouseY, draggedStack, button);
    }

    /** The pattern slots' own drag path, guarded with the rest. */
    @Override
    protected void handleDragVirtualSlot(final VirtualMESlot slot, final int mouseButton) {
        if (this.dragSuppressed()) {
            return;
        }
        super.handleDragVirtualSlot(slot, mouseButton);
    }

    @Override
    protected void mouseClickMove(final int mouseX, final int mouseY, final int clickedMouseButton,
        final long timeSinceLastClick) {
        if (this.dragSuppressed()) {
            return;
        }
        if (this.draggingMgmtKnob) {
            this.dragMgmtScrollTo(mouseY);
            return;
        }
        if (this.draggingCacheKnob) {
            this.dragCacheScrollTo(mouseY);
            return;
        }
        if (this.draggingEncKnob) {
            this.dragEncScrollTo(mouseY);
            return;
        }
        if (this.draggingUpgradeKnob) {
            this.dragUpgradeScrollTo(mouseY);
            return;
        }
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    @Override
    protected void mouseMovedOrUp(final int mouseX, final int mouseY, final int which) {
        if (this.draggingMgmtKnob) {
            if (which < 0) {
                this.dragMgmtScrollTo(mouseY);
                return; // still dragging (button held)
            }
            this.draggingMgmtKnob = false;
        }
        if (this.draggingCacheKnob) {
            if (which < 0) {
                this.dragCacheScrollTo(mouseY);
                return; // still dragging (button held)
            }
            this.draggingCacheKnob = false;
        }
        if (this.draggingEncKnob) {
            if (which < 0) {
                this.dragEncScrollTo(mouseY);
                return;
            }
            this.draggingEncKnob = false;
        }
        if (this.draggingUpgradeKnob) {
            if (which < 0) {
                this.dragUpgradeScrollTo(mouseY);
                return;
            }
            this.draggingUpgradeKnob = false;
        }
        super.mouseMovedOrUp(mouseX, mouseY, which);
    }

    /** True while the cursor is over the upgrade panel's handle groove. */
    private boolean isOnUpgradeScrollbar(final int mouseX, final int mouseY) {
        return this.upgradePanel != null && this.upgradePanel.isOnScrollbar(mouseX, mouseY, this.guiLeft, this.guiTop);
    }

    private void dragUpgradeScrollTo(final int mouseY) {
        if (this.upgradePanel != null) {
            this.upgradePanel.dragTo(mouseY, this.guiTop, ENC_KNOB_H);
        }
    }

    /** True while the cursor is over the encoding scrollbar's groove. */
    private boolean isOnEncScrollbar(final int mouseX, final int mouseY) {
        final int x = this.guiLeft + ENC_SCROLL_X;
        final int y = this.guiTop + bottomStartRel() + (ENC_SCROLL_Y - REF_BOTTOM_TOP);
        return mouseX >= x && mouseX < x + ENC_SCROLL_W && mouseY >= y && mouseY < y + ENC_SCROLL_H;
    }

    /**
     * How many rows the cache can be scrolled by (WCWT shows two of its four rows). Zero while the
     * enlargement switch is on: the cache is shut then, so there is nothing to scroll and every
     * caller - the wheel, the scrollbar and its grab handle - falls silent with this one answer.
     */
    private int maxCacheScroll() {
        if (this.mgmtExpanded()) {
            return 0;
        }
        return Math.max(0, ContainerComprehensiveWorkTerminal.CACHE_ROWS - CACHE_VISIBLE_ROWS);
    }

    /**
     * Lays the pattern cache out: the visible two rows come from the current scroll offset, the rest
     * is parked off-screen - exactly how WCWT pages its four rows behind a two-row window.
     */
    private void layoutPatternCache() {
        final int cols = ContainerComprehensiveWorkTerminal.CACHE_COLS;
        final Slot[] cache = this.monitorableContainer.getCacheSlots();
        // As long as the enlargement switch is off the cache is WCWT's own strip: two of its four rows
        // live at CACHE_Y, the rest are parked off-screen behind the scroll offset. Once the switch is
        // on the whole area shuts down - the row is given to the management list and the title, the
        // cells and the slot art all go with it - so every slot is parked off-screen.
        final boolean shut = this.mgmtExpanded();
        for (int i = 0; i < cache.length; i++) {
            final int row = i / cols - this.cacheScroll;
            final boolean visible = !shut && row >= 0 && row < CACHE_VISIBLE_ROWS;
            placeSlot(
                cache[i],
                visible,
                ContainerComprehensiveWorkTerminal.CACHE_X + (i % cols) * ROW_H,
                ContainerComprehensiveWorkTerminal.CACHE_Y + row * ROW_H);
        }
    }

    /**
     * True while the cursor is over the pattern cache's cells. WCWT's own hit test spans
     * {@code CACHE_COLS * 18 + 12} so that the wheel also works over the scrollbar column.
     */
    private boolean isInCacheArea(final int mouseX, final int mouseY) {
        // Shut while enlarged, so the wheel never reaches the cache's cells in that layout; the test
        // below is WCWT's own and spans CACHE_COLS * 18 + 12 so the scrollbar column counts too.
        if (this.mgmtExpanded()) {
            return false;
        }
        final int left = this.guiLeft + ContainerComprehensiveWorkTerminal.CACHE_X;
        final int top = this.guiTop + bottomStartRel() + (ContainerComprehensiveWorkTerminal.CACHE_Y - REF_BOTTOM_TOP);
        return mouseX >= left && mouseX < left + ContainerComprehensiveWorkTerminal.CACHE_COLS * ROW_H + 12
            && mouseY >= top
            && mouseY < top + CACHE_VISIBLE_ROWS * ROW_H;
    }

    /**
     * Repaints the management area's own backdrop over the bottom sheet - and takes the pattern
     * cache's strip with it - while the enlargement switch is on.
     *
     * <p>
     * Two pieces of the sheet stand in the way, and both are <em>baked into it</em>, so neither can
     * be switched off by skipping a draw call. The cache's two-row grid runs texY 144..180 (nine
     * 18px cells of {@code 173,176,196} against {@code 154,159,180} separators, boxed by white rules
     * at 144 and 179..180). The management area's dark backdrop is baked in just as firmly, fixed at
     * texY 206..277 - the strip the list occupies in WCWT's own layout. Enlarged, the cache is shut
     * and the list starts at offset 103 instead, so its upper half would sit on the sheet's plain
     * panel colour while only its lower half sat on the dark backdrop, with the cache's cells
     * showing through in between: the backdrop would read as having stayed put while the list rose.
     *
     * <p>
     * So one fill of the backdrop's own colour is laid from the lifted header chrome's top edge
     * (offset 79) down to the list's bottom (offset 225), across the area's full width. The chrome's
     * own background, the list's cells and every panel the list draws afterwards then land on the
     * colour the area was designed with.
     *
     * <p>
     * The cache's slots are parked off-screen by {@link #layoutPatternCache} in the same mode, so
     * nothing is drawn on top of this fill. It rides in the background pass because the list is
     * drawn later, in the foreground one.
     */
    private void drawCacheBandMask(final int guiX, final int guiY) {
        if (!this.mgmtExpanded()) {
            return;
        }
        final int x = guiX + MGMT_X;
        final int y = guiY + bottomStartRel() + (MGMT_BACKDROP_TEX_Y - REF_BOTTOM_TOP);
        // Gui.drawRect leaves glColor set at the fill's colour and the blend state as it found it;
        // the encoding cells, the scrollbars and NEI all come after this, so every attribute is saved
        // and restored around the fill (the same reason the ghost wash does it).
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        // The fill reaches one groove-width past the area's own width: the baked scrollbar groove sits
        // in that strip (texture x 336..353) and has to be taken along with the rest of the old page.
        drawRect(x, y, x + MGMT_W + MGMT_SCROLL_STRIP_W, y + MGMT_BACKDROP_H, MGMT_BACKDROP_COLOR);
        // The header chrome's own row art belongs to the area too: the sunken slot backdrops the
        // provider search box, the mapping field and the switches are drawn against are baked into
        // the sheet at texture Y 181..204, the strip the chrome occupies in WCWT's layout. The flat
        // fill above removes them from this strip, so the whole row is lifted from there to where the
        // chrome now sits - same width, same 24 rows, so it lands one-to-one and the controls keep
        // the backdrop they were designed on. The width takes the scrollbar strip in as well: the
        // sheet keeps that column plain panel colour across the chrome row (its groove only starts at
        // the page's own top edge), and without it the flat fill would show through beside the two
        // mapping plates as a dark block where the sheet has none.
        bindTextureBack(BACKGROUND);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        blit512(x, y, MGMT_X, MGMT_CHROME_TOP_TEX_Y, MGMT_W + MGMT_SCROLL_STRIP_W, MGMT_CHROME_H);
        // The list's scrollbar groove is part of the same baked strip, so the fill removed it as
        // well - and left at that, the knob would ride up to the list's new position with no groove
        // under it. Rebuild the sheet's groove here rather than laying a flat bar: the groove is one
        // light 18px column (texture x 336..353) whose only feature along its length is the 3px dark
        // slot at x 345..347 between the white rules at 336 and 353, and that slot runs the page's
        // whole height (texture Y 208..276) without changing. So one uniform block of the column is
        // repeated down the list and the sheet's own ruled ends are put back top and bottom - the
        // recessed look then holds for all of the list's 122 rows. Copying only the end caps (as this
        // used to) left a bare light bar carrying the slot's mark at the top and nowhere else.
        final int grooveX = guiX + MGMT_GROOVE_TEX_X;
        final int grooveY = guiY + this.mgmtTop();
        final int grooveH = this.mgmtHeight();
        bindTextureBack(BACKGROUND);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        for (int done = 0; done < grooveH; done += MGMT_GROOVE_BODY_H) {
            blit512(
                grooveX,
                grooveY + done,
                MGMT_GROOVE_TEX_X,
                MGMT_GROOVE_BODY_TEX_Y,
                MGMT_SCROLL_STRIP_W,
                Math.min(MGMT_GROOVE_BODY_H, grooveH - done));
        }
        blit512(grooveX, grooveY, MGMT_GROOVE_TEX_X, MGMT_GROOVE_CAP_TEX_Y, MGMT_SCROLL_STRIP_W, MGMT_GROOVE_CAP_H);
        blit512(
            grooveX,
            grooveY + grooveH - MGMT_GROOVE_FOOT_H,
            MGMT_GROOVE_TEX_X,
            MGMT_GROOVE_FOOT_TEX_Y,
            MGMT_SCROLL_STRIP_W,
            MGMT_GROOVE_FOOT_H);
        GL11.glPopAttrib();
    }

    /** Width of the baked scrollbar groove's strip beside the management page: texture x 336..353. */
    private static final int MGMT_SCROLL_STRIP_W = 18;
    /**
     * The sheet's own scrollbar groove: the light 18px column beside the management page (texture x
     * 336..353), carrying a 3px dark slot at x 345..347 between the white rules at 336 and 353.
     * Along the page's height that column never changes (texture Y 208..276), so one uniform block of
     * it can be repeated to rebuild the groove at any height; only its ruled ends are taken apart.
     */
    private static final int MGMT_GROOVE_TEX_X = 336;
    private static final int MGMT_GROOVE_BODY_TEX_Y = 240;
    /** 32 uniform rows of the column, repeated to fill the groove's body. */
    private static final int MGMT_GROOVE_BODY_H = 32;
    /** The groove's ruled top: the white rule and the dark lip closing the slot (texture Y 205..207). */
    private static final int MGMT_GROOVE_CAP_TEX_Y = 205;
    private static final int MGMT_GROOVE_CAP_H = 3;
    /** The groove's ruled bottom: the slot closing and the white rule under it (texture Y 275..278). */
    private static final int MGMT_GROOVE_FOOT_TEX_Y = 275;
    private static final int MGMT_GROOVE_FOOT_H = 4;

    /**
     * The management area's span once the enlargement switch has lifted it, in texture space: from
     * the header chrome's new top edge (offset 79, i.e. the encoding panel's bottom plus one) down
     * to the list's unchanged bottom (offset 225, i.e. the management page's own end). Written as
     * the two offsets the layout is derived from rather than as absolutes, so moving either edge
     * carries the fill with it.
     */
    private static final int MGMT_BACKDROP_TEX_Y = 79 + REF_BOTTOM_TOP;
    private static final int MGMT_BACKDROP_H = (225 + REF_BOTTOM_TOP) - MGMT_BACKDROP_TEX_Y;
    /**
     * The bottom sheet's own management-area backdrop, {@code 154,159,180}. Reproduced rather than
     * sampled, so the fill is one flat colour with no seam against the sheet's baked strip below it.
     */
    private static final int MGMT_BACKDROP_COLOR = 0xFF9A9FB4;

    private void drawCacheScroll(final int guiX, final int guiY) {
        if (this.maxCacheScroll() <= 0) {
            return;
        }
        drawScrollbarAt(
            guiX + CACHE_SCROLL_X,
            guiY + bottomStartRel() + (CACHE_SCROLL_Y - REF_BOTTOM_TOP),
            CACHE_SCROLL_H,
            this.cacheScroll,
            this.maxCacheScroll());
    }

    /**
     * The upgrade panel's background art, plus its scrollbar handle when the column pages. The
     * handle rides on the sprite's own groove, exactly like AE2 1.21's {@code Scrollbar} - which
     * assumes the track is pre-baked in the background and draws nothing but the handle.
     */
    private void drawUpgradePanel(final int guiX, final int guiY) {
        if (this.upgradePanel == null) {
            return;
        }
        this.upgradePanel.refresh();
        this.upgradePanel.draw(this.mc, guiX, guiY);
        // The card buttons follow the card column, which can change while the screen is open.
        this.updateCardButtons();
        if (this.upgradePanel.maxScroll() > 0) {
            drawScrollbarAt(
                guiX + this.upgradePanel.scrollbarX(),
                guiY + this.upgradePanel.scrollbarY(),
                this.upgradePanel.scrollbarHeight(),
                this.upgradePanel.getScroll(),
                this.upgradePanel.maxScroll());
        }
    }

    private boolean isOnCacheScrollbar(final int mouseX, final int mouseY) {
        // Shut while enlarged: the groove is not drawn there either, so there is nothing to grab.
        if (this.maxCacheScroll() <= 0) {
            return false;
        }
        final int x = this.guiLeft + CACHE_SCROLL_X;
        final int y = this.guiTop + bottomStartRel() + (CACHE_SCROLL_Y - REF_BOTTOM_TOP);
        return mouseX >= x && mouseX < x + ENC_SCROLL_W && mouseY >= y && mouseY < y + CACHE_SCROLL_H;
    }

    private void dragCacheScrollTo(final int mouseY) {
        final int grooveTop = this.guiTop + bottomStartRel() + (CACHE_SCROLL_Y - REF_BOTTOM_TOP);
        final int travel = Math.max(1, CACHE_SCROLL_H - ENC_KNOB_H);
        final int value = (mouseY - ENC_KNOB_H / 2 - grooveTop) * this.maxCacheScroll() / travel;
        this.cacheScroll = Math.max(0, Math.min(this.maxCacheScroll(), value));
        layoutPatternCache();
    }

    /** Draws a 1.21-style scrollbar (dark groove + short light knob) at a texture position. */
    private void drawScrollbarAt(final int x, final int y, final int height, final int scroll, final int range) {
        // Only the official small_scroller handle is drawn, riding directly on the panel, with its
        // travel spanning the control's full height.
        final int knobX = x + (ENC_SCROLL_W - ENC_KNOB_W) / 2;
        final int knobY = y + Math.max(0, height - ENC_KNOB_H) * scroll / Math.max(1, range);
        // AE2 1.21's Scrollbar.SMALL handle, copied into wcwt_widgets.png: enabled (38,0) and
        // disabled (47,0); no scrolling left means the disabled sprite, like the original.
        this.mc.getTextureManager()
            .bindTexture(WIDGETS_TEXTURE);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        drawTexturedModalRect(knobX, knobY, range > 0 ? 38 : 47, 0, ENC_KNOB_W, ENC_KNOB_H);
    }

    /** Maps a cursor position onto a scroll offset, so the knob follows the cursor. */
    private void dragEncScrollTo(final int mouseY) {
        final int grooveTop = this.guiTop + bottomStartRel() + (ENC_SCROLL_Y - REF_BOTTOM_TOP) + 1;
        final int travel = Math.max(1, ENC_SCROLL_H - ENC_KNOB_H);
        final int offset = mouseY - ENC_KNOB_H / 2 - grooveTop;
        final int value = offset * this.maxEncScroll() / travel;
        this.encScroll = Math.max(0, Math.min(this.maxEncScroll(), value));
        layoutPatternArea();
    }

    /**
     * Y of the bottom slice's top, relative to guiTop (i.e. where the scrollable rows end).
     */
    private int bottomStartRel() {
        return HEADER_H + this.rows * ROW_H;
    }

    /**
     * Screen Y of a widget from its WCWT texture Y, i.e. WCWT's {@code guiTop + ySize - bottom}
     * rule: the bottom slice starts at {@link #bottomStartRel()} and the widget sits
     * {@code textureY - REF_BOTTOM_TOP} pixels into it.
     */
    private int frameY(final int textureY) {
        return this.guiTop + bottomStartRel() + (textureY - REF_BOTTOM_TOP);
    }

    /**
     * {@link #frameY} for the management header chrome, which drops clear of the encoding panel when
     * the enlargement switch is on.
     * Kept separate from {@code frameY} rather than folded into it: every other button on the page
     * (the batch radios, the encoding options) must stay where WCWT puts it.
     */
    private int mgmtChromeY(final int textureY) {
        return this.guiTop + bottomStartRel() + (textureY - REF_BOTTOM_TOP) + this.mgmtChromeShift();
    }

    /**
     * Maps every non-player container slot from its WCWT reference Y (stored by the container) onto
     * the area below the last item row. Slot coordinates are guiTop-relative.
     */
    private void repositionFixedSlots() {
        for (final Object o : this.inventorySlots.inventorySlots) {
            if (o instanceof final AppEngSlot s && !(s instanceof SlotPlayerInv)
                && !(s instanceof SlotPlayerHotBar)
                // The encoding cells and the pattern cache are placed by layoutPatternArea /
                // layoutPatternCache; their container-side Y is a shared reference (every encoding
                // input is built with y=72), so mapping them all through getY() would collapse the
                // whole column onto one row - items piling up on top of each other and the rest of
                // the rows going blank. Leave them to the layout pass.
                && !this.monitorableContainer.isEncodingAreaSlot(s)) {
                s.yDisplayPosition = bottomStartRel() + (s.getY() - REF_BOTTOM_TOP);
            }
        }
    }

    @Override
    public void setScrollBar() {
        super.setScrollBar();
        // GuiMonitor parks the scrollbar at x=175 (a 9-column layout); with WCWT's 18 columns that
        // would overlap the item grid, so use the scrollbar's real slot from the WCWT screen JSON.
        this.getScrollBar()
            .setLeft(SCROLLBAR_X)
            .setTop(SCROLLBAR_Y);
    }

    @Override
    protected void actionPerformed(GuiButton btn) {
        for (int i = 0; i < this.multButtons.length; i++) {
            if (this.multButtons[i] == btn) {
                if (MULTIPLIERS[i] == SWAP) {
                    // WCWT's swap (⇄): rotates the processing outputs between main and secondary.
                    Wtct.proxy.netHandler.sendToServer(
                        new com.asdflj.wtct.network.CPacketTerminalBtns(
                            "PatternTerminal.SwapOutputs",
                            "1",
                            new NBTTagCompound()));
                } else {
                    Wtct.proxy.netHandler.sendToServer(
                        new com.asdflj.wtct.network.CPacketTerminalBtns(
                            "PatternTerminal.Multiply",
                            Integer.toString(MULTIPLIERS[i]),
                            new NBTTagCompound()));
                }
                return;
            }
        }
        if (this.cycleOutputBtn == btn) {
            // Same rotation the batch plate's swap performs - WCWT keeps both controls.
            Wtct.proxy.netHandler.sendToServer(
                new com.asdflj.wtct.network.CPacketTerminalBtns(
                    "PatternTerminal.SwapOutputs",
                    "1",
                    new NBTTagCompound()));
            return;
        }
        if (this.encClearBtn == btn) {
            Wtct.proxy.netHandler.sendToServer(
                new com.asdflj.wtct.network.CPacketTerminalBtns("PatternTerminal.Clear", "1", new NBTTagCompound()));
            return;
        }
        if (this.substBtn == btn) {
            Wtct.proxy.netHandler.sendToServer(
                new com.asdflj.wtct.network.CPacketTerminalBtns(
                    "PatternTerminal.Substitute",
                    this.monitorableContainer.substitute ? "0" : "1",
                    new NBTTagCompound()));
            return;
        }
        if (this.beSubstOnBtn != null && (this.beSubstOnBtn == btn || this.beSubstOffBtn == btn)) {
            Wtct.proxy.netHandler.sendToServer(
                new com.asdflj.wtct.network.CPacketTerminalBtns(
                    "PatternTerminal.beSubstitute",
                    this.monitorableContainer.beSubstitute ? "0" : "1",
                    new NBTTagCompound()));
            return;
        }
        if (this.mergeBtn == btn) {
            // GTNH AE's "merge identical materials" (the extended pattern terminal's option, ported as
            // NEIUtils.compress): while it is on, an NEI recipe transfer folds the identical single
            // items of the recipe into one cell. The encoder itself is never touched, which is what
            // keeps the items you place exactly where you put them.
            Wtct.proxy.netHandler.sendToServer(
                new com.asdflj.wtct.network.CPacketTerminalBtns(
                    "PatternTerminal.Combine",
                    this.monitorableContainer.combine ? "0" : "1",
                    new NBTTagCompound()));
            return;
        }
        if (this.manualClearBtn == btn) {
            Wtct.proxy.netHandler
                .sendToServer(new com.asdflj.wtct.network.CPacketTerminalBtns("CraftTerminal.Clear", 1));
            return;
        }
        if (this.stashBtn == btn) {
            Wtct.proxy.netHandler
                .sendToServer(new com.asdflj.wtct.network.CPacketTerminalBtns("CraftTerminal.Stash", 1));
            return;
        }
        if (this.mgmtDisplayToggle == btn) {
            // WCWT cycles ALL -> VISIBLE -> NOT_FULL and rebuilds the list. The value goes to the
            // server, which writes it onto the terminal item - that is what makes it outlive the GUI.
            this.mgmtSetting("PatternManagement.DisplayMode", (this.mgmtDisplayMode() + 1) % 3);
            return;
        }
        if (this.mgmtSlotsToggle == btn) {
            this.mgmtSetting("PatternManagement.ShowSlots", this.mgmtShowSlots() ? "0" : "1");
            return;
        }
        if (this.mgmtSearchModeToggle == btn) {
            // WCWT cycles OUT -> IN -> IN_OUT; the scope is applied to the search text.
            this.mgmtSetting("PatternManagement.SearchMode", (this.mgmtSearchMode() + 1) % 3);
            return;
        }
        if (this.mgmtAutoUploadToggle == btn) {
            // WCWT's 自动上传 switch: it decides whether encoding hands the finished pattern to the
            // management area (see sendEncode), and is remembered with the terminal.
            this.mgmtSetting("PatternManagement.Upload", this.mgmtUpload() ? "0" : "1");
            return;
        }
        if (this.mgmtExpandToggle == btn) {
            // Ours: pull the list up over the pattern cache. Written through to the terminal item the
            // same way, so the layout is remembered.
            this.mgmtSetting("PatternManagement.Expand", this.mgmtExpanded() ? "0" : "1");
            return;
        }
        if (this.batchItemRadio == btn) {
            // The action name has to start with "PatternTerminal.": the server's dispatcher only
            // enters its pattern-branch switch for those, and a "PatternCache." name fell straight
            // through - which is why the switch looked like it did nothing at all.
            this.sendBatchSwitch(
                "PatternTerminal.ItemSubstitution",
                !this.monitorableContainer.substitute,
                "wtct.gui.batch.item_substitution");
            return;
        }
        if (this.batchBeSubRadio == btn) {
            this.sendBatchSwitch(
                "PatternTerminal.BeSubstitution",
                !this.monitorableContainer.beSubstitute,
                "wtct.gui.batch.be_substitution");
            return;
        }
        if (this.encodeBtn == btn) {
            // Encode, and hand the finished pattern to the pattern management area when the automatic
            // upload toggle is on (WCWT's "样板编码后直接传输到样板管理区").
            this.sendEncode();
            return;
        }
        if (this.tabCraftButton == btn || this.tabProcessButton == btn) {
            // Both tabs are on screen, so a click selects that mode directly
            // ("1" = crafting, "0" = processing).
            final boolean crafting = this.tabCraftButton == btn;
            if (crafting != this.monitorableContainer.craftingMode) {
                Wtct.proxy.netHandler.sendToServer(
                    new com.asdflj.wtct.network.CPacketTerminalBtns(
                        "PatternTerminal.CraftMode",
                        crafting ? "1" : "0",
                        new NBTTagCompound()));
            }
            return;
        }
        if (this.cardImportBtn == btn) {
            InventoryHandler.switchGui(GuiType.CARD_IMPORT);
            return;
        }
        if (this.cardExportBtn == btn) {
            InventoryHandler.switchGui(GuiType.CARD_EXPORT);
            return;
        }
        if (this.cardMagnetBtn == btn) {
            // WCWT opens the magnet card's menu from this button; the magnet's own on/off setting
            // moved into that screen, which is where its cycle button now lives.
            InventoryHandler.switchGui(GuiType.CARD_MAGNET);
            return;
        }
        if (this.cardPickerBtn == btn) {
            // The addon opens its BlockPickerAmountScreen from this button; this port reuses the amount
            // panel it already draws for the encoder cells (middle-click), pre-filled with the picker's
            // current amount.
            this.openPickerAmountPanel();
            return;
        }
        if (this.autoFillBtn == btn) {
            // The same flip the settings screen performs: the local stack answers the redraw, the
            // packet's server half writes the real one.
            final ItemStack terminal = this.monitorableContainer.getTerminalStack();
            ContainerWcwtSettings.toggleOn(true, terminal, ContainerWcwtSettings.KEY_CRAFT_IF_MISSING);
            final NBTTagCompound tag = new NBTTagCompound();
            tag.setString("Key", ContainerWcwtSettings.KEY_CRAFT_IF_MISSING);
            Wtct.proxy.netHandler.sendToServer(new CPacketTerminalBtns("CardConfig.SettingToggle", 0, tag));
            this.updateCardButtons();
            return;
        }
        if (this.trashBtn == btn) {
            InventoryHandler.switchGui(GuiType.WCWT_TRASH);
            return;
        }
        if (this.settingsBtn == btn) {
            InventoryHandler.switchGui(GuiType.WCWT_SETTINGS);
            return;
        }
        if (this.statusBtn == btn) {
            // Sent as this terminal's own type rather than GuiType.CRAFTING_STATUS: that one is the *part*
            // factory, right for a terminal mounted on a block, but this host is an item and a part factory
            // cannot resolve an item coordinate. CRAFTING_STATUS_ITEM_COMPREHENSIVE is the same container
            // and screen, but its "back to terminal" tab returns here instead of to the wireless
            // dual-interface terminal, which shares this terminal's host class.
            //
            // Handled before super as well: GuiMonitor would otherwise claim the click through the
            // craftingStatusBtn field.
            Wtct.proxy.netHandler.sendToServer(new CPacketSwitchGuis(GuiType.CRAFTING_STATUS_ITEM_COMPREHENSIVE));
            return;
        }
        super.actionPerformed(btn);
    }

    /**
     * Places the player inventory and hotbar on WCWT's PLAYER_INVENTORY (left 8, bottom 85) and
     * PLAYER_HOTBAR (left 8, bottom 27) rects, i.e. texture Y 203/221/239 and 261.
     *
     * <p>
     * Positions are recovered from the coordinates AE2 itself used when binding the slots
     * ({@code bindPlayerInventory}: x = 8 + col*18 + offsetX, row*18 for the main inventory and 58
     * for the hotbar) rather than from {@code Slot#getSlotIndex()}, which returns the slot's index
     * in the container list instead of the inventory index.
     *
     * <p>
     * Important: AE2 replaces the slot that holds the terminal with a {@link SlotDisabled}, so the
     * held terminal is neither a SlotPlayerInv nor a SlotPlayerHotBar - it must be handled here too,
     * otherwise it stays at its container-bound position (which sits far above the hotbar row).
     * Coordinates are guiTop-relative, like every other slot.
     */
    private void repositionPlayerSlots() {
        for (final Object o : this.inventorySlots.inventorySlots) {
            if (!(o instanceof final AppEngSlot s)) {
                continue;
            }
            // AE2's own bound Y for a main-inventory row vs. the hotbar row.
            final int boundY = s.getY();
            final boolean hotbarRow = boundY >= PLAYER_BIND_HOTBAR_Y;
            final boolean mainInventory = s instanceof SlotPlayerInv
                || (s instanceof SlotDisabled && isBoundPlayerY(boundY) && !hotbarRow);
            final boolean hotbar = s instanceof SlotPlayerHotBar
                || (s instanceof SlotDisabled && isBoundPlayerY(boundY) && hotbarRow);
            if (mainInventory) {
                s.xDisplayPosition = s.getX() - PLAYER_BIND_X_OFFSET;
                s.yDisplayPosition = bottomStartRel() + (203 - REF_BOTTOM_TOP) + (boundY / ROW_H) * ROW_H;
            } else if (hotbar) {
                s.xDisplayPosition = s.getX() - PLAYER_BIND_X_OFFSET;
                s.yDisplayPosition = bottomStartRel() + (261 - REF_BOTTOM_TOP);
            }
        }
    }

    /** True for the Y values AE2 uses when binding player inventory slots. */
    private static boolean isBoundPlayerY(final int y) {
        return y == 0 || y == ROW_H || y == 2 * ROW_H || y == PLAYER_BIND_HOTBAR_Y;
    }

    @Override
    public void drawFG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        // TEMP DIAGNOSTIC (1.0.37, remove once the terminal-open delay is pinned down).
        final boolean diag = this.diagFgs < 3;
        final long diagT0 = diag ? System.currentTimeMillis() : 0;
        // WCWT's screen JSON titles the screen "comprehensive_work_area" (its terminal item name) in the
        // header. Left 8 of the raw panel would sit under the sidebar's plates, so the text starts just
        // past them - in the reference screenshot the full title is visible right of the toolbar.
        this.fontRendererObj.drawString(
            net.minecraft.util.StatCollector.translateToLocal("item.comprehensive_work_terminal.name"),
            TITLE_X,
            6,
            4210752);
        this.drawAreaTitles();
        // Fluid-marked cells: the fluid itself plus its count, the way GTNH's pattern encoder shows a
        // fluid stack (the carrier item's own sprite and tooltip are replaced by these).
        this.drawFluidCells();
        // The little blue diamond on encoding cells whose item has a pattern in the network
        // (WCWT's craftable-pattern indicator).
        this.drawCraftableMarkers();
        // ae2helpers' pending visuals: ghost material + spinner on cells the Ctrl+hammer is watching.
        this.drawAutoFillGhosts();
        // The mode can change server-side at any time, so keep the tabs (and the encoding area's
        // slot layout) in step.
        updateModeTabs();
        updateEncodingOptionButtons();
        // The enlargement switch can flip at any moment, so the management header is re-anchored here
        // too, before the list (which it must clear) is drawn.
        this.updateMgmtChromePositions();
        // The provider list is drawn in the foreground pass: it renders items, and vanilla (and AE2)
        // only render GUI items after RenderHelper.enableGUIStandardItemLighting, which the item
        // batching in Angelica also assumes. Drawing items from the background pass is what made the
        // NEI panel come out with wrongly lit (dark-topped) blocks.
        this.drawPatternManagement(mouseX, mouseY);
        // Tooltips belong to the foreground layer: drawn from the background pass they end up under
        // everything the container draws afterwards.
        //
        // Wrapped in a full attribute save/restore: GuiScreen.drawHoveringText turns GL_LIGHTING back
        // on at its end, so without this the state this GUI hands on flips between "lighting on"
        // (tooltip shown) and "lighting off" (no tooltip) every time the cursor crosses a slot - and
        // whatever renders next (NEI paints its item panel right after) then gets inconsistent
        // lighting, which shows up as block items with dark top faces.
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        this.drawMgmtTooltip(this.guiLeft + MGMT_X, this.guiTop + this.mgmtTop(), mouseX, mouseY);
        // The mapping plates' hint goes up last of all, once every plate and every row is down.
        if (this.mappingTooltip != null) {
            this.drawHoveringText(
                java.util.Collections.singletonList(this.mappingTooltip),
                mouseX - this.guiLeft,
                mouseY - this.guiTop,
                this.fontRendererObj);
        }
        GL11.glPopAttrib();
        // Same reason as in drawBG: the tooltip's gradient rects leave a dark glColor behind, and
        // whatever draws next (NEI included) would inherit it.
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        if (diag) {
            cpw.mods.fml.common.FMLLog.info(
                "[wtct-diag] client drawFG call=%d took=%dms t=%d",
                this.diagFgs,
                System.currentTimeMillis() - diagT0,
                diagT0);
            this.diagFgs++;
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Fluid marking on the encoder cells (GTNH's PatternTerminalEx / VirtualMEPhantomSlot)
    // ---------------------------------------------------------------------------------------------

    /** True when the pointer sits on an encoder cell and the mark was handled there. */
    private boolean markFluidUnderCursor(final int mouseX, final int mouseY, final int mouseButton) {
        final ContainerComprehensiveWorkTerminal cwt = this.monitorableContainer;
        if (cwt == null) {
            return false;
        }
        for (final SlotFake cell : cwt.getEncodingSlots()) {
            if (cell == null || cell.xDisplayPosition < 0 || cell.yDisplayPosition < 0) {
                continue;
            }
            final int x = this.guiLeft + cell.xDisplayPosition;
            final int y = this.guiTop + cell.yDisplayPosition;
            if (mouseX < x || mouseX >= x + 16 || mouseY < y || mouseY >= y + 16) {
                continue;
            }
            return this.handlePatternCellFluidClick(cell, cell.slotNumber, mouseButton);
        }
        return false;
    }

    /**
     * GTNH's fluid marking, taken from {@code VirtualMEPhantomSlot.handleMouseClicked} - the search
     * for it walks every stack type the cell accepts, and for a cell that also accepts items (the
     * encoder's cells do) the container's fluid needs Ctrl: without it the container itself is what
     * the click means. A fluid display item (NEI's, GregTech's, AE2FC's drop) is that fluid already
     * and needs no key; the right button takes one unit back off a cell that already holds fluid.
     *
     * <p>
     * The decision has to be made here rather than on the server: Ctrl is not part of AE2's
     * {@code PacketInventoryAction}, and GTNH decides in the GUI for the same reason. The cell is
     * given its new content locally - a slow network would otherwise show the click doing nothing -
     * and the server is told through {@code PacketClickOrDragFakeSlot}, which overwrites the slot
     * outright. For an AE2 fluid cell the fluid only exists on the server, so that one is asked.
     *
     * <p>
     * What this does not recognise - plain items, a container without Ctrl, an empty hand over an
     * item cell - falls through to AE2's own handling, which already does what GTNH does for those.
     */
    @Override
    protected boolean handlePatternCellFluidClick(final Slot slot, final int slotIdx, final int mouseButton) {
        // The middle button is the amount dialog, always. Letting the fluid gesture see it too is what
        // wiped a cell the player had just given an amount: a Ctrl+middle click on a fluid-carrying
        // cell rewrote the stack into a one-unit packet and the typed number was gone. Answering "not
        // mine" was not enough - the click then fell through to AE2's own fake-slot handling, which sets
        // or clears whichever cell the pointer happens to be over, so a middle click on anything the
        // dialog does not serve copied the cursor's stack into it, or emptied it. Consume the button
        // here instead: a middle click paints no cell, the dialog is the only thing it does. The cell
        // is left as it is, which is what the true return says.
        if (mouseButton == 2) {
            return true;
        }
        final ContainerComprehensiveWorkTerminal cwt = this.monitorableContainer;
        // GTNH's rule, whatever the encoder's mode is: the fluid gesture is the container's.
        final boolean settable = cwt != null && cwt.canMarkFluid(slot);
        if (!settable) {
            return false;
        }

        final ItemStack inCell = slot.getStack();
        final ItemStack hand = this.getStackFromHand();
        final ItemStack result;

        if (hand == null) {
            // Ctrl on whatever the cell already holds: a container/tank in the cell becomes the fluid
            // it holds (the same conversion the cursor's container gets), and an AE2 fluid cell - whose
            // fluids only the server can read - is converted there.
            if (isCtrlKeyDown() && inCell != null) {
                final FluidStack fromCell = fluidToMark(inCell, true);
                if (fromCell != null && fromCell.amount > 0) {
                    result = ItemFluidPacket.newStack(fromCell);
                } else if (isFluidStorageCell(inCell)) {
                    Wtct.proxy.netHandler.sendToServer(new CPacketFluidCellMark(slot.slotNumber, true));
                    return true;
                } else {
                    return false;
                }
            } else {
                // The right button on a fluid cell takes one mB back off it, down to nothing.
                final FluidStack present = cellFluid(inCell);
                if (mouseButton != 1 || present == null) {
                    return false;
                }
                if (present.amount <= 1) {
                    result = null;
                } else {
                    final FluidStack less = present.copy();
                    less.amount -= 1;
                    result = ItemFluidPacket.newStack(less);
                }
            }
        } else {
            final FluidStack marked = fluidToMark(hand, isCtrlKeyDown());
            if (marked == null || marked.amount <= 0) {
                // Ctrl on a fluid storage cell: its fluids live in the server's storage manager, so
                // the client cannot read them - the container marks the first one it finds instead.
                if (isCtrlKeyDown() && isFluidStorageCell(hand)) {
                    Wtct.proxy.netHandler.sendToServer(new CPacketFluidCellMark(slot.slotNumber, false));
                    return true;
                }
                return false;
            }
            final FluidStack present = cellFluid(inCell);
            if (mouseButton == 1 && present != null && present.isFluidEqual(marked)) {
                // Same fluid already there: GTNH's decStackSize(-1) - one more mB of it, not another
                // container's worth.
                final FluidStack total = present.copy();
                total.amount += 1;
                result = ItemFluidPacket.newStack(total);
            } else {
                result = ItemFluidPacket.newStack(marked);
            }
        }

        slot.putStack(result);
        NetworkHandler.instance.sendToServer(new PacketClickOrDragFakeSlot(result, slot.slotNumber, true));
        return true;
    }

    /**
     * The fluid a click on the cursor should mark, or null when it is not a fluid gesture. This is
     * GTNH's {@code FluidUtils.getFluidFromContainer} plus its {@code convertStackFromItem}: a
     * container hands over what it holds - the amount is the container's own, so a bucket is 1000 mB
     * no matter how many of them are stacked together - and anything else that is shown as a fluid
     * (NEI's phantom items, GregTech's) is that fluid.
     */
    private static FluidStack fluidToMark(final ItemStack hand, final boolean ctrl) {
        if (hand.getItem() instanceof IFluidContainerItem || FluidContainerRegistry.isContainer(hand)) {
            // Without Ctrl the container item itself goes in, which is AE2's own handling - and GTNH's.
            if (!ctrl) {
                return null;
            }
            // IFluidContainerItem first: GregTech-grade tanks keep their fluid in the item's own tag,
            // and the registry only knows filled vanilla-style containers - a partly filled tank
            // returns null there, which is why the registry-first order used to mark nothing.
            if (hand.getItem() instanceof final IFluidContainerItem container) {
                final FluidStack held = container.getFluid(hand);
                if (held != null && held.amount > 0) {
                    return held.copy();
                }
            }
            final FluidStack held = FluidContainerRegistry.isContainer(hand)
                ? FluidContainerRegistry.getFluidForFilledItem(hand)
                : null;
            if (held != null && held.amount > 0) {
                return held.copy();
            }
            return null;
        }
        final ItemStack one = hand.copy();
        one.stackSize = 1;
        return StackInfo.getFluid(one);
    }

    /**
     * The fluid an encoder cell holds, or null when it holds an item instead of a fluid. Everything
     * that is shown as a fluid is recognised: the two carriers AE2FC uses - the fluid packet (fluid
     * and amount in its own {@code FluidStack} tag) and the fluid drop (fluid name in the tag, amount
     * as the stack size) - and, through the same lookup AE2's own
     * {@code AEFluidStackType.convertStackFromItem} runs, GregTech's display item and NEI's phantom
     * items, which is what a recipe transfer leaves in a cell. A container is left out of it, because
     * a bucket in a crafting grid is a bucket and AE2 draws that line too.
     */
    private static FluidStack cellFluid(final ItemStack inCell) {
        return PatternScaling.fluidCarriedBy(inCell);
    }

    /**
     * The count from which a cell's amount is shown at all - AE2's own rule for the terminal font size
     * ({@code GuiMEMonitorable}: over 999 with the large font, over 9999 with the small one), so a
     * marked bucket of fluid reads "1k" here exactly like it does in the item list.
     */
    private long amountDisplayThreshold() {
        final boolean large = this.bigCounts() || AEConfig.instance.getTerminalFontSize() == TerminalFontSize.LARGE;
        return large ? 999L : 9999L;
    }

    /**
     * True when the stack is an AE2 fluid storage cell. Its fluid list belongs to the server's
     * storage manager, so the cell is marked by the container rather than here.
     */
    private static boolean isFluidStorageCell(final ItemStack stack) {
        if (stack == null) {
            return false;
        }
        try {
            return appeng.api.AEApi.instance()
                .registries()
                .cell()
                .getCellInventory(
                    stack,
                    null,
                    appeng.util.item.AEFluidStackType.FLUID_STACK_TYPE) instanceof com.asdflj.wtct.common.storage.ITFluidCellInventoryHandler;
        } catch (final Throwable ignored) {
            return false;
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Middle-click amount entry (GTNH's own pattern value screen)
    // ---------------------------------------------------------------------------------------------

    /**
     * Hands a cell to GTNH's amount screen. The click carries the cell no further than the server: the
     * server knows the terminal's target stack and opens {@code GuiPatternValueAmount} - AE2FC's
     * screen, the one GTNH's pattern terminals raise for this button - prefilled from the cell. The
     * number comes back as {@code CPacketPatternValueSet}, which reopens the terminal and writes it
     * into this very slot by index.
     *
     * <p>
     * Doing the exchange on the server's side of the wire is what makes a fluid cell read its count
     * out of the fluid instead of the stack (a fluid packet's stack size is always one), and it keeps
     * the edited cell in step with the menu behind, which never closes.
     */
    private void openPatternValuePanel(final Slot slot) {
        final IAEItemStack stack = AEItemStack.create(slot.getStack());
        if (stack == null) {
            return;
        }
        this.monitorableContainer.setTargetStack(stack);
        Wtct.proxy.netHandler.sendToServer(
            new CPacketInventoryActionExtend(InventoryActionExtend.SET_PATTERN_VALUE, slot.slotNumber, 0, stack));
    }

    /**
     * The block picker's amount, the addon's {@code BlockPickerAmountScreen}: the same dialog, but it
     * edits the number the terminal stores for "pick block" instead of a cell.
     */
    private void openPickerAmountPanel() {
        final ItemStack terminal = this.monitorableContainer.getTerminalStack();
        if (terminal == null) {
            return;
        }
        this.openOnTop(
            new GuiWcwtAmount(
                this,
                StatCollector.translateToLocal("item.wcwt_card_block_picker.name"),
                null,
                cardIcon("card_icon_block_picker"),
                CardTicker.pickerAmount(terminal),
                this::submitPickerAmount));
    }

    /** Sends the block picker's amount, the one the terminal stores on its own stack. */
    private void submitPickerAmount(final long amount) {
        final NBTTagCompound tag = new NBTTagCompound();
        tag.setLong("Amount", amount);
        Wtct.proxy.netHandler.sendToServer(new CPacketTerminalBtns("CardConfig.SetPickerAmount", 0, tag));
    }

    /**
     * The encoder cells' amounts, drawn the way GTNH's pattern terminal draws its pattern slots
     * ({@code VirtualMEPatternSlot.setShowAmount} + AE2's own threshold): a fluid mark is painted as the
     * fluid itself with its count, and any cell asking for a long count shows that count in the corner.
     * A fluid mark is carried by an AE2FC item whose own sprite ("fluid packet"/"fluid drop") and
     * tooltip say nothing useful inside a pattern, so both are replaced here - the cell is painted over
     * the icon, and {@link #handleItemTooltip} answers for the hover.
     */
    private void drawFluidCells() {
        final long threshold = this.amountDisplayThreshold();
        for (final SlotFake slot : this.monitorableContainer.getEncodingSlots()) {
            this.drawFluidCell(slot, threshold);
        }
        // A processing pattern's output column carries fluids as well, and the cell is painted the same
        // way there: leaving the outputs out of this pass is why setting an amount on an output fluid
        // left the cell with no number - the mark was never painted over its carrier sprite.
        for (final SlotFake slot : this.monitorableContainer.getOutputSlots()) {
            this.drawFluidCell(slot, threshold);
        }
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** One cell of the editing area or of the output column, painted as the method above describes. */
    private void drawFluidCell(final SlotFake slot, final long threshold) {
        final ItemStack stack = slot.getStack();
        if (stack == null || slot.xDisplayPosition < 0 || slot.yDisplayPosition < 0) {
            return;
        }
        final int x = slot.xDisplayPosition;
        final int y = slot.yDisplayPosition;
        final FluidStack fluid = cellFluid(stack);
        final long amount = cellAmount(stack);
        // A count is left to AE2's own slot rendering wherever AE2 will actually draw it - that is
        // every cell whose stack size is above one - because that is where the "1.7K" on a long
        // stack comes from. Two cases are left for this screen instead:
        // - the fluid sprite is painted over the whole cell, and AE2's count goes under it, so a
        // marked fluid - GregTech's display item included, whose amount is its stack size -
        // would otherwise end up with no number at all no matter what it holds;
        // - the amount lives outside the stack (a fluid packet keeps it in its tag and its stack
        // size stays one), so AE2 has nothing to draw.
        // A cell that is neither keeps AE2's own count: drawing one here as well put two copies of
        // the same number on the cell, a pixel apart.
        final boolean fluidPainted = fluid != null && fluid.getFluid()
            .getStillIcon() != null;
        // A painted fluid hides AE2's count under its sprite, so the cell's number has to be drawn
        // here even for the small amounts a fluid asks for ("144" for an ingot) - AE2 only draws a
        // count above one, and this screen's own long-amount threshold (999/9999) must not apply
        // to a fluid, or the fluid's number never shows up at all.
        final boolean ownCount = fluidPainted ? amount > 1 : amount > threshold && stack.stackSize <= 1;
        if (fluid == null) {
            if (ownCount) {
                GL11.glPushMatrix();
                GL11.glTranslatef(0.0F, 0.0F, 200.0F);
                this.drawCellCount(amount, x, y);
                GL11.glPopMatrix();
            }
            return;
        }
        // The fluid and its count share one z: the count used to be drawn at the GUI's own z, which
        // the fluid sprite - drawn above the item layer - then covered, which is the report of the
        // fluid's text being hidden.
        GL11.glPushMatrix();
        GL11.glTranslatef(0.0F, 0.0F, 200.0F);
        this.drawFluidTile(fluid, x, y);
        if (ownCount) {
            this.drawCellCount(amount, x, y);
        }
        GL11.glPopMatrix();
    }

    /**
     * The corner count AE2 writes on a stack, drawn through AE2's own {@code StackSizeRenderer} - the
     * same call {@code AEBaseGui.drawSlotWithAEFont} makes for a slot - so the number comes out the size,
     * shape and place the terminal's item list uses. Drawing it here with the plain GUI font at full
     * size is what made a fluid cell's number overflow its 18px cell and cover its neighbours. The
     * renderer reads its font size from AE2's own setting, which this screen already lends the large
     * value for the duration of the frame when the terminal's switch is on (see {@link #drawScreen}).
     */
    private void drawCellCount(final long amount, final int x, final int y) {
        StackSizeRenderer.drawStackSize(x, y, amount, this.fontRendererObj, AEConfig.instance.getTerminalFontSize());
    }

    /** One cell's fluid sprite, tinted the way AE2 tints a fluid stack in a slot. */
    private void drawFluidTile(final FluidStack fluid, final int x, final int y) {
        final IIcon icon = fluid.getFluid()
            .getStillIcon();
        if (icon == null) {
            return;
        }
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT);
        this.mc.getTextureManager()
            .bindTexture(TextureMap.locationBlocksTexture);
        final int color = fluid.getFluid()
            .getColor();
        GL11.glColor4f(((color >> 16) & 0xFF) / 255.0F, ((color >> 8) & 0xFF) / 255.0F, (color & 0xFF) / 255.0F, 1.0F);
        final Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();
        // The caller already pushed the item-overlay z, so the sprite sits at the GUI's own depth here.
        tess.addVertexWithUV(x, y + 16, 0.0D, icon.getMinU(), icon.getMaxV());
        tess.addVertexWithUV(x + 16, y + 16, 0.0D, icon.getMaxU(), icon.getMaxV());
        tess.addVertexWithUV(x + 16, y, 0.0D, icon.getMaxU(), icon.getMinV());
        tess.addVertexWithUV(x, y, 0.0D, icon.getMinU(), icon.getMinV());
        tess.draw();
        GL11.glPopAttrib();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /**
     * The tooltip of an encoder cell, built the way GTNH builds it for its own pattern slots
     * ({@code GuiPatternTerm}: the stack's name, the count once it is long, and AE2's "MMB - Change
     * Amount" line) - so a marked fluid reads as the fluid and its amount instead of the AE2FC item's
     * "use a packet decoder" text, which is about an item that is not actually in the pattern.
     */
    @Override
    public List<String> handleItemTooltip(final ItemStack stack, final int mouseX, final int mouseY,
        final List<String> currentToolTip) {
        // AE2's own answer comes first, exactly like GuiMEMonitorable does it. A grid fluid has no NEI
        // item stack to hand NEI (AEFluidStack#getItemStackForNEI is null in this pack), so NEI arrives
        // here with an empty tooltip and no name; AE2's override is what writes the fluid's name into it.
        // Dropping this call is why hovering a fluid in the ME grid showed no box at all.
        super.handleItemTooltip(stack, mouseX, mouseY, currentToolTip);
        // AE2's base names the fluid but never its amount, and a grid fluid arrives here with a null NEI
        // stack (so NEI's own count details cannot fill it in either). Answer for the shared monitorable
        // slots the way GuiMEMonitorable answers its own, with this GUI's count threshold.
        final VirtualMESlot hoveredGridSlot = this.getVirtualMESlotUnderMouse();
        if (hoveredGridSlot != null) {
            final IAEStack<?> hoveredAeStack = hoveredGridSlot.getAEStack();
            if (hoveredAeStack instanceof IAEFluidStack
                && hoveredAeStack.getStackSize() > this.amountDisplayThreshold()) {
                currentToolTip.add(
                    EnumChatFormatting.GRAY + String.format(
                        ButtonToolTips.ItemsStored.getLocal(),
                        NumberFormat.getNumberInstance(Locale.US)
                            .format(hoveredAeStack.getStackSize())));
            }
        }
        final String favourite = favoriteLine(stack);
        final ContainerComprehensiveWorkTerminal cwt = this.monitorableContainer;
        if (cwt == null || stack == null) {
            return currentToolTip;
        }
        final Slot hovered = this.getSlotAtPosition(mouseX, mouseY);
        // canMarkFluid is "this slot is one of the encoder's cells" - the same set the fluid gesture
        // and the amount dialog work on.
        if (hovered == null || !cwt.canMarkFluid(hovered)) {
            if (favourite == null) {
                return currentToolTip;
            }
            // A favourited grid item says so the way WCWT says it - the corner heart is an icon with no
            // words of its own.
            final List<String> lines = new ArrayList<>(currentToolTip.size() + 1);
            lines.addAll(currentToolTip);
            lines.add(favourite);
            return lines;
        }
        final FluidStack fluid = cellFluid(stack);
        final List<String> lines = new ArrayList<>(4);
        lines.add(fluid == null ? stack.getDisplayName() : fluid.getLocalizedName());
        final long amount = cellAmount(stack);
        if (amount > this.amountDisplayThreshold()) {
            lines.add(
                EnumChatFormatting.GRAY + String.format(
                    ButtonToolTips.ItemCount.getLocal(),
                    NumberFormat.getNumberInstance(Locale.US)
                        .format(amount)));
        }
        lines.add(ButtonToolTips.ChangeAmount.getLocal());
        if (favourite != null) {
            lines.add(favourite);
        }
        return lines;
    }

    /** The gold "favourited" line for a stack the player has favourited, or null for anything else. */
    private static String favoriteLine(final ItemStack stack) {
        if (stack == null) {
            return null;
        }
        final AEItemStack key = AEItemStack.create(stack);
        if (key == null) {
            return null;
        }
        final Pinned.PinInfo info = WtctAPI.instance()
            .getPinned()
            .getPinInfo(key);
        if (info == null || info.reason != Pinned.PinReason.FAVORITE) {
            return null;
        }
        return EnumChatFormatting.GOLD + StatCollector.translateToLocal("wtct.gui.favorite.marked");
    }

    /**
     * WCWT's craftable-pattern indicator: encoding cells whose item the network can craft carry a
     * small encoded-pattern badge in their top-right corner, so a pattern's ingredients that will
     * need autocrafting stand out before the pattern is even encoded.
     */
    private void drawCraftableMarkers() {
        for (final SlotFake slot : this.monitorableContainer.getEncodingSlots()) {
            this.drawCraftableMarker(slot);
        }
        // The output column is part of the same editing area and its cells can hold a fluid or an
        // autocraftable item just the same, so it gets the badge too (the reference stamps every
        // pattern slot, not only the ingredient half).
        for (final SlotFake slot : this.monitorableContainer.getOutputSlots()) {
            this.drawCraftableMarker(slot);
        }
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** The badge on one cell. */
    private void drawCraftableMarker(final SlotFake slot) {
        final ItemStack stack = slot.getStack();
        if (stack == null || slot.xDisplayPosition < 0 || slot.yDisplayPosition < 0) {
            return;
        }
        if (!this.slotCraftable(slot, stack)) {
            return;
        }
        // Same badge the reference mod stamps on pattern slots (see RenderPatternSlotFake): the
        // encoded-pattern icon scaled to 40% and tucked into the cell's top-right corner. A "+"
        // text was tried first and read as a stray glyph, so the icon is what we draw.
        //
        // Depth testing is switched off for the draw and the badge is lifted past the fluid pass's
        // own z: a fluid cell paints its fluid sprite at z=200 in front of everything the slot pass
        // left there, and with depth testing on the badge - drawn at the same z - lost to it, which
        // is why a craftable fluid came out with no badge at all while an item cell had one.
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_LIGHTING_BIT);
        GL11.glPushMatrix();
        GL11.glTranslatef(0, 0, 250f);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glScalef(0.4f, 0.4f, 0.4f);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting();
        this.itemRender.renderItemAndEffectIntoGUI(
            this.fontRendererObj,
            this.mc.getTextureManager(),
            PATTERN,
            (int) ((slot.xDisplayPosition + 10) * 2.5),
            (int) (slot.yDisplayPosition * 2.5));
        net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting();
        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }

    /**
     * True when the network holds a pattern for the item, so the encoding cell can be autocrafted.
     * The container's pushed set is the complete answer, but the repo is consulted as well: that is
     * exactly the lookup the recipe overlay's blue runs on, so the marker shows wherever the overlay
     * proves the item craftable - and the two can never disagree about the same cell.
     */
    /**
     * The craftable-badge answer for one cell, memoised.
     *
     * <p>
     * The badge is stamped in the per-frame pass, and answering the question means walking the network's
     * whole item list - twice, once as an item and once as the fluid it stands for - for every encoding
     * cell: thirty-odd cells against a few thousand stacks, every frame. The answer only changes when the
     * cell's contents or the network's list do, so it is remembered against the exact stack it was worked
     * out from, and the whole cache is dropped whenever either of those arrives.
     */
    private boolean slotCraftable(final SlotFake slot, final ItemStack stack) {
        final CachedCraftable cached = this.craftableMarks.get(slot);
        if (cached != null && ItemStack.areItemStacksEqual(cached.stack, stack)) {
            return cached.craftable;
        }
        final boolean craftable = this.hasCraftablePattern(stack);
        this.craftableMarks.put(slot, new CachedCraftable(stack.copy(), craftable));
        return craftable;
    }

    /** One cell's remembered badge answer; the stack it was worked out from is the key. */
    private static final class CachedCraftable {

        private final ItemStack stack;
        private final boolean craftable;

        CachedCraftable(final ItemStack stack, final boolean craftable) {
            this.stack = stack;
            this.craftable = craftable;
        }
    }

    private boolean hasCraftablePattern(final ItemStack stack) {
        if (this.craftableKeys.contains(craftableKey(stack))) {
            return true;
        }
        final AdvItemRepo repo = this.getRepo();
        if (repo == null) {
            return false;
        }
        final IItemList<IAEStack<?>> list = Ae2ReflectClient.getList(repo);
        if (list == null) {
            return false;
        }
        final IAEStack<?> precise = list.findPrecise(AEItemStack.create(stack));
        if (precise != null && precise.isCraftable()) {
            return true;
        }
        // A fluid mark is looked up the way the network files fluids. This terminal receives them as
        // IAEFluidStack entries, and the fluid monitor stamps craftable on those from the matching
        // ItemFluidDrop item (see FluidMonitor); a network that files fluids as pseudo items holds the
        // display stack instead. The mark is on whichever of the two the repo has - looking up the
        // packet item a fluid cell is stored as here matched neither, which is why a fluid cell never
        // got a craftable badge.
        final FluidStack fluid = cellFluid(stack);
        if (fluid == null) {
            return false;
        }
        final IAEFluidStack asFluid = AEFluidStack.create(fluid);
        return isCraftable(list, asFluid)
            || isCraftable(list, AEItemStack.create(ItemFluidDrop.newDisplayStack(fluid)));
    }

    /** Whether the repo holds this stack and the network can craft it. */
    private static boolean isCraftable(final IItemList<IAEStack<?>> list, final IAEStack<?> lookup) {
        if (lookup == null) {
            return false;
        }
        final IAEStack<?> precise = list.findPrecise(lookup);
        return precise != null && precise.isCraftable();
    }

    // ---------------------------------------------------------------------------------------------
    // Ctrl+hammer auto-fill watch - ae2helpers' AutoCraftingWatcher, rebuilt for 1.7.10
    // ---------------------------------------------------------------------------------------------

    /**
     * The terminal's "craft if missing" switch, which the settings screen and this terminal's own
     * toolbar button both flip. Off means Ctrl+hammer behaves like a plain hammer: nothing is asked
     * of the network and no cell is watched.
     */
    private boolean autoFillEnabled() {
        return this.switchOn(ContainerWcwtSettings.KEY_CRAFT_IF_MISSING, true);
    }

    /**
     * Set while one of this terminal's own menu-less dialogs sits on top of it - see
     * {@link #openOnTop}. {@link #onGuiClosed} needs it to tell "a dialog opened over the terminal"
     * apart from "the terminal itself was closed".
     */
    private boolean overlayScreenOpen;
    /**
     * Set while the button that opened one of {@link #openOnTop}'s dialogs is still held, so that dragging is
     * ignored until it comes up - see {@link #mouseClickMove}.
     */
    private boolean ignoreDragUntilRelease;
    /** Ticks the drag suppression above still lasts for, so a dialog's click cannot leak right after it closes. */
    private int dragSuppressTicks;
    /** Probe counter for the messages {@link #dragSuppressed} writes while this is being verified in game. */
    private int dragSuppressLogged;
    /** Set by {@link #clearGridOnClose} so that a single close asks for the clear only once. */
    private boolean clearGridAsked;

    /**
     * Shows one of the terminal's own dialogs over it (the two amount boxes and the recipe-type
     * mapping screen). They are screens without menus, so this terminal's menu stays open behind
     * them, and 1.7.10 calls this screen's {@code onGuiClosed} as it hands over - which is not the
     * terminal closing and must not trigger "clear grid on close".
     */
    private void openOnTop(final GuiScreen screen) {
        this.overlayScreenOpen = true;
        // The mouse must not touch this terminal's slots until the dialog is done with it: see dragSuppressed. The
        // tick budget covers the moment the dialog closes, the button check covers a drag that outlives it.
        this.ignoreDragUntilRelease = true;
        this.dragSuppressTicks = 10;
        this.mc.displayGuiScreen(screen);
    }

    /**
     * AE2 1.21's "clear grid on close": the manual crafting matrix goes back to the network (and
     * from there to the player) as the terminal goes away. The switch lives on the terminal item -
     * see {@code ContainerWcwtSettings.KEY_CLEAR_GRID_ON_CLOSE} - and does nothing until the player
     * turns it on.
     *
     * <p>
     * Where this is called from matters. Vanilla's {@code GuiContainer.keyTyped} answers ESC and the
     * inventory key by sending its close-window packet to the server <em>first</em> and running
     * {@code onGuiClosed} only afterwards, so a clear asked for in {@code onGuiClosed} would arrive
     * at a server that has already let this menu go. {@link #keyTyped} therefore asks before that
     * packet; {@code onGuiClosed} covers the closes that never go through the key (the management
     * area's "highlight machine", which drops the screen itself).
     */
    private void clearGridOnClose() {
        if (this.clearGridAsked || !this.switchOn(ContainerWcwtSettings.KEY_CLEAR_GRID_ON_CLOSE, false)) {
            return;
        }
        this.clearGridAsked = true;
        Wtct.proxy.netHandler.sendToServer(new com.asdflj.wtct.network.CPacketTerminalBtns("CraftTerminal.Clear", 1));
    }

    @Override
    public void onGuiClosed() {
        // The Ctrl+hammer arms the watch (SPacketAutoFillPending) and then opens AE2's craft plan on
        // top of this screen, so the watch has to outlive it - the crafted materials land while the
        // plan is up and are only moved into the cells once the terminal is back. ae2helpers keeps
        // its watcher in a static for the same reason. Anywhere else this terminal really is done
        // with: the cells it never managed to fill stop being watched instead of haunting the grid
        // the next time it is opened.
        final GuiScreen next = this.mc.currentScreen;
        if (!(next instanceof GuiCraftConfirm) && next != this) {
            AutoFillWatch.INSTANCE.clear();
        }
        // The screens that swap this menu out for another one (settings, trash, status, the card
        // dialogs, the craft plan) need no exclusion here: by the time this packet arrives the
        // player's open container is theirs, and the server half of "CraftTerminal.Clear" ignores
        // anything that is not this terminal's menu. Only the dialogs above keep that menu open, so
        // only they have to be held back.
        final boolean overlay = this.overlayScreenOpen;
        this.overlayScreenOpen = false;
        if (overlay) {
            // The dialog on top is a sub-screen of THIS menu (the reference opens its
            // SetProcessingPatternAmountScreen the same way): vanilla's onGuiClosed would send the
            // close-window packet here, the server would let the terminal's menu go, and the amount
            // the player had just set would land on the player-inventory container - which is the
            // fluid amount vanishing the moment the dialog closed. Nothing is closed behind a
            // sub-screen, so nothing may be announced as closed.
            this.clearGridAsked = false;
            return;
        }
        this.clearGridOnClose();
        this.clearGridAsked = false;
        super.onGuiClosed();
    }

    /** Every client tick - the watcher's whole loop lives here, as ae2helpers ticks it per screen tick. */
    @Override
    public void updateScreen() {
        super.updateScreen();
        if (this.dragSuppressTicks > 0) {
            this.dragSuppressTicks--;
        }
        this.tickAutoFillWatch();
    }

    /**
     * One watched cell per round: the first whose material the network now holds is handed to the
     * server, which moves a single one into it. The pause afterwards is ae2helpers' guard against
     * the same item landing in two cells before the grid has caught up.
     */
    private void tickAutoFillWatch() {
        if (AutoFillWatch.INSTANCE.isEmpty()) {
            return;
        }
        if (!this.autoFillEnabled()) {
            AutoFillWatch.INSTANCE.clear();
            return;
        }
        if (AutoFillWatch.INSTANCE.pause()) {
            return;
        }
        final AdvItemRepo repo = this.getRepo();
        final java.util.Iterator<Map.Entry<Integer, ItemStack[]>> it = AutoFillWatch.INSTANCE.pending()
            .entrySet()
            .iterator();
        while (it.hasNext()) {
            final Map.Entry<Integer, ItemStack[]> entry = it.next();
            final Slot slot = this.craftingCell(entry.getKey());
            if (slot == null) {
                // Nothing in this screen carries that matrix index - the watch would sit on a cell that
                // does not exist, so it goes.
                it.remove();
                continue;
            }
            if (slot.getStack() != null) {
                // Filled in the meantime - by the pull, the server, or the player.
                it.remove();
                continue;
            }
            final ItemStack found = repo == null ? null : storedIn(repo, entry.getValue());
            if (found == null) {
                continue;
            }
            Wtct.proxy.netHandler.sendToServer(new CPacketFillCraftingSlot(entry.getKey(), found));
            it.remove();
            AutoFillWatch.INSTANCE.holdRetry();
            break;
        }
    }

    /** The crafting grid's cell for a matrix index, or {@code null} when this screen has none. */
    private Slot craftingCell(final int slotIndex) {
        for (final Object o : this.inventorySlots.inventorySlots) {
            if (o instanceof appeng.container.slot.SlotCraftingMatrix slot && slot.getSlotIndex() == slotIndex) {
                return slot;
            }
        }
        return null;
    }

    /**
     * The network entry for the ingredient that actually holds something - ae2helpers' "stored
     * amount > 0" test. Craftable-only entries hold none and are skipped on purpose: those are what
     * the auto-craft request is for, and they turn into stored items once the CPUs are done.
     */
    private static ItemStack storedIn(final AdvItemRepo repo, final ItemStack[] variants) {
        for (final IAEStack<?> entry : repo.getAllStacks()) {
            if (!(entry instanceof IAEItemStack ais) || ais.getStackSize() <= 0) {
                continue;
            }
            for (final ItemStack variant : variants) {
                if (variant != null && ais.isSameType(variant)) {
                    final ItemStack found = ais.getItemStack();
                    found.stackSize = 1;
                    return found;
                }
            }
        }
        return null;
    }

    /**
     * ae2helpers' pending visuals: on every watched empty crafting cell, the expected material as a
     * ghost item under a gray wash with a spinning dot trail - "the network is on it".
     */
    private void drawAutoFillGhosts() {
        if (AutoFillWatch.INSTANCE.isEmpty() || !this.autoFillEnabled()) {
            return;
        }
        for (final Object o : this.inventorySlots.inventorySlots) {
            if (!(o instanceof appeng.container.slot.SlotCraftingMatrix slot)) {
                continue;
            }
            if (slot.getStack() != null || slot.xDisplayPosition < 0) {
                continue;
            }
            final ItemStack[] variants = AutoFillWatch.INSTANCE.variantsAt(slot.getSlotIndex());
            if (variants != null && variants.length > 0) {
                this.drawPendingGhost(slot.xDisplayPosition, slot.yDisplayPosition, variants);
            }
        }
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void drawPendingGhost(final int x, final int y, final ItemStack[] variants) {
        // ae2helpers cycles the accepted variants: which one the pull will take is up to the network.
        // NEI's packed lists can carry holes, so the cycle skips over them rather than rendering null.
        final int start = (int) ((System.currentTimeMillis() / 1000) % variants.length);
        ItemStack ghost = null;
        for (int i = 0; i < variants.length && ghost == null; i++) {
            ghost = variants[(start + i) % variants.length];
        }
        if (ghost == null) {
            return;
        }
        // 1. The expected material as a ghost item, floating over the empty cell (ae2helpers'
        // renderGhosts). It needs the GUI item lighting or Angelica draws it unlit and near black.
        GL11.glPushMatrix();
        GL11.glTranslatef(0.0F, 0.0F, GHOST_ITEM_Z);
        this.itemRender.zLevel = GHOST_ITEM_Z;
        // Restore the lighting instead of switching it off. This runs in the foreground pass, which is
        // already lit (see drawMgmtItem) - the old explicit disableStandardItemLighting() left the rest
        // of the frame unlit, and NEI bakes whatever lighting it sees into its item-panel render cache,
        // so the whole panel came back dark until that cache was rebuilt.
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting();
        this.itemRender.renderItemAndEffectIntoGUI(this.fontRendererObj, this.mc.getTextureManager(), ghost, x, y);
        GL11.glPopAttrib();
        this.itemRender.zLevel = 0.0F;
        GL11.glPopMatrix();
        // 2. The wash and the spinner, on a layer of their own in front of the item. Both need that
        // head start in Z and the depth test off: at the item's own depth the quads are rejected
        // against the item's, which is exactly how "the ghost item is there but no gray, no spinner"
        // looks. The wash is a Gui.drawRect quad, not a hand-rolled Tessellator one - the pack's
        // renderer is what made the marker badges' hand-rolled quads come out blank.
        GL11.glPushMatrix();
        GL11.glTranslatef(0.0F, 0.0F, GHOST_OVERLAY_Z);
        // All bits, not just the enable/colour-buffer pair: Gui.drawRect sets the current colour and
        // the item pass above turns the lighting on and off, and neither lives in those two masks.
        // NEI paints its item panel right after this screen, so a half-saved attribute bit shows up
        // there as dimmed items - the same reason drawFG saves everything around its tooltips.
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        drawRect(x, y, x + 16, y + 16, GHOST_WASH);
        // Six dots around the center, one turn a second with a trailing fade.
        final float angle = (System.currentTimeMillis() % 1000) / 1000.0F * 360.0F;
        GL11.glPushMatrix();
        GL11.glTranslatef(x + 8.0F, y + 8.0F, 0.0F);
        GL11.glRotatef(angle, 0.0F, 0.0F, 1.0F);
        for (int i = 0; i < 6; i++) {
            final double rad = Math.toRadians(360.0 / 6 * i);
            final int dx = (int) Math.round(Math.cos(rad) * 5);
            final int dy = (int) Math.round(Math.sin(rad) * 5);
            drawRect(dx - 1, dy - 1, dx + 1, dy + 1, (255 - i * (200 / 6)) << 24 | 0xFFFFFF);
        }
        GL11.glPopMatrix();
        GL11.glPopAttrib();
        GL11.glPopMatrix();
        // glPopAttrib brings the colour back, but be explicit: the wash and the spinner leave a
        // translucent tint behind if the attribute mask ever drifts, and the next renderer inherits it.
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** Z of the ghost item itself - AE2 draws GUI items at 100, so the ghost sits just in front. */
    private static final float GHOST_ITEM_Z = 110.0F;

    /** Z of the wash and spinner. Must clear the item layer, or the depth test eats them. */
    private static final float GHOST_OVERLAY_Z = 250.0F;

    /** ae2helpers' wash: gray at 37.6% over the ghost item. */
    private static final int GHOST_WASH = 0x608B8B8B;

    /** The badge sprite: the encoded pattern, the same icon the reference mod stamps on pattern slots. */
    private static final ItemStack PATTERN = AEApi.instance()
        .definitions()
        .items()
        .encodedPattern()
        .maybeStack(1)
        .orNull();

    /** The craftable item keys of the network's storage list, refreshed a few times a second. */
    private Set<Integer> craftableKeys = new java.util.HashSet<>();

    /**
     * The badge answers already worked out, keyed by the cell they belong to. Dropped whenever the
     * network's storage list or its craftable-key set arrives - the two things a remembered answer can
     * be invalidated by; see {@link #slotCraftable}.
     */
    private final java.util.IdentityHashMap<SlotFake, CachedCraftable> craftableMarks = new java.util.IdentityHashMap<>();

    // TEMP DIAGNOSTIC counters (1.0.37, remove once the terminal-open delay is pinned down).
    private int diagFrames;
    private int diagAllFrames;
    private long diagAllTotal;
    private long diagAllMax;
    private int diagBgs;
    private int diagFgs;

    /** Server push (SPacketCraftableKeys): the network's complete craftable-key set. */
    public void postCraftableKeys(final int[] keys) {
        final Set<Integer> received = new java.util.HashSet<>();
        for (final int key : keys) {
            received.add(key);
        }
        this.craftableKeys = received;
        this.craftableMarks.clear();
    }

    /** The network's craftable keys as pushed by the container - the hammer preview reads this. */
    public Set<Integer> getCraftableKeys() {
        return this.craftableKeys;
    }

    /** Item+damage key, the same shape the server pushes ({@code id << 16 | damage}). */
    public static int craftableKey(final ItemStack stack) {
        return (Item.getIdFromItem(stack.getItem()) << 16) | (stack.getItemDamage() & 0xFFFF);
    }

    /**
     * The amount a cell holds: a fluid mark counts fluid (packet tag or drop size, GregTech's
     * display item's own fluid), the rest items - the encoder's own rule, so the number drawn on the
     * cell is the number the pattern will ask for.
     */
    private static long cellAmount(final ItemStack stack) {
        return ContainerComprehensiveWorkTerminal.fluidCellAmount(stack);
    }

    /**
     * Draws the area labels WCWT's screen JSON puts over the bottom slice, in its own order: the
     * crafting area and the encoding area both anchor to bottom 232 (the first line below the item
     * rows), then the batch area (bottom 149), the pattern cache (bottom 153) and the player
     * inventory (bottom 95).
     *
     * <p>
     * WCWT's "pattern management area" label is left out on purpose: its page is the pattern
     * management panel, which this port does not have, and drawing the label would point at the
     * player inventory instead.
     */
    private void drawAreaTitles() {
        this.fontRendererObj.drawString(
            net.minecraft.util.StatCollector.translateToLocal("wtct.gui.area.crafting"),
            AREA_TITLE_LEFT_X,
            this.areaTitleY(232),
            AREA_TITLE_COLOR);
        this.fontRendererObj.drawString(
            net.minecraft.util.StatCollector.translateToLocal("wtct.gui.area.encoding"),
            AREA_TITLE_RIGHT_X,
            this.areaTitleY(232),
            AREA_TITLE_COLOR);
        this.drawBatchTitle();
        // The cache title belongs to the cache: drawn in WCWT's layout, dropped once the enlargement
        // switch shuts the cache down - the area is gone, so its label goes with it.
        if (!this.mgmtExpanded()) {
            this.fontRendererObj.drawString(
                net.minecraft.util.StatCollector.translateToLocal("wtct.gui.area.caching"),
                AREA_TITLE_RIGHT_X,
                this.areaTitleY(153),
                AREA_TITLE_COLOR);
        }
        // The management title keeps to WCWT's layout too: enlarged, the header chrome sits in the
        // strip that used to hold it (79..102, between the encoding panel above and the list below).
        if (!this.mgmtExpanded()) {
            this.fontRendererObj.drawString(
                net.minecraft.util.StatCollector.translateToLocal("wtct.gui.area.management"),
                MGMT_X,
                this.areaTitleY(106),
                AREA_TITLE_COLOR);
        }
        this.fontRendererObj.drawString(
            net.minecraft.util.StatCollector.translateToLocal("container.inventory"),
            AREA_TITLE_LEFT_X,
            this.areaTitleY(95),
            AREA_TITLE_COLOR);
    }

    /**
     * WCWT's {@code renderBatchProcessingLabels}, anchor for anchor: the multiplier label is a per-glyph
     * vertical column with its left edge on x = 13 when the text is CJK (one glyph every 8px at scale
     * 0.875) and a centred line on x = 17 at scale 0.75 when it is not, both on WCWT's own row for it; the
     * item and fluid substitution labels are plain lines at left 108.
     */
    private void drawBatchTitle() {
        final String title = net.minecraft.util.StatCollector.translateToLocal("wtct.gui.area.batch");
        if (!title.isEmpty()) {
            if (title.codePointAt(0) >= 0x2E80) {
                GL11.glPushMatrix();
                GL11.glTranslatef(BATCH_TITLE_CJK_X, areaTitleY(BATCH_TITLE_BOTTOM), 0.0F);
                GL11.glScalef(0.875F, 0.875F, 1.0F);
                for (int i = 0; i < title.length(); i++) {
                    this.fontRendererObj.drawString(title.substring(i, i + 1), 0, i * 8, AREA_TITLE_COLOR);
                }
                GL11.glPopMatrix();
            } else {
                final float scale = 0.75F;
                final int w = (int) (this.fontRendererObj.getStringWidth(title) * scale);
                GL11.glPushMatrix();
                GL11.glTranslatef(BATCH_TITLE_CENTER_X - w / 2.0F, areaTitleY(BATCH_TITLE_LATIN_BOTTOM), 0.0F);
                GL11.glScalef(scale, scale, 1.0F);
                this.fontRendererObj.drawString(title, 0, 0, AREA_TITLE_COLOR);
                GL11.glPopMatrix();
            }
        }
        this.fontRendererObj.drawString(
            net.minecraft.util.StatCollector.translateToLocal("wtct.gui.batch.item_substitution"),
            BATCH_LABEL_X,
            areaTitleY(BATCH_LABEL1_BOTTOM),
            AREA_TITLE_COLOR);
        this.fontRendererObj.drawString(
            net.minecraft.util.StatCollector.translateToLocal("wtct.gui.batch.be_substitution"),
            BATCH_LABEL_X,
            areaTitleY(BATCH_LABEL2_BOTTOM),
            AREA_TITLE_COLOR);
    }

    /** Local Y of a WCWT "bottom" anchor - the same rule the slot layout uses. */
    private int areaTitleY(final int bottom) {
        return bottomStartRel() + (REF_HEIGHT - bottom - REF_BOTTOM_TOP);
    }

    /** True for the buttons the left toolbar owns - the ones the sidebar's surface has to cover. */
    private static boolean isSidebarButton(final Object button) {
        return button instanceof GuiWcwtToolbarButton || button instanceof GuiWcwtSidebarIconButton;
    }

    /**
     * The left column's first free row. Measured from the buttons themselves rather than from a running
     * offset, so a method that adds to the column after {@code initGui} re-anchored the window still
     * lands directly under the last plate.
     */
    private int nextSidebarRowY() {
        int y = this.toolbarY();
        for (final Object b : this.buttonList) {
            if (isSidebarButton(b)) {
                y = Math.max(y, ((GuiButton) b).yPosition + GuiWcwtToolbarButton.ROW_PITCH);
            }
        }
        return y;
    }

    /**
     * Where the sidebar's surface ends: one row below the lowest plate, clamped so the closing border still
     * fits inside the window. That extra row is the buses' own foot - their wrapping plate leaves one middle
     * row under the last plate before its bottom band starts, a 5px foot counting the band's four rows - so
     * the surface runs one row past the plate and the band hangs below it.
     *
     * <p>
     * Every plate in the column has to be counted - the AE2 settings buttons <em>and</em> the card
     * buttons - because the surface is the terminal's own background carried leftwards: a plate the
     * surface does not reach hangs over the void outside the window instead of sitting in the column.
     * Hidden ones are not: a card button whose card is out of the terminal is not drawn, and counting
     * its stale row would keep the surface - and so the column - one plate too long.
     */
    private void refreshSidebarBottom() {
        int bottom = this.toolbarY() + GuiWcwtToolbarButton.ROW_PITCH;
        for (final Object b : this.buttonList) {
            if (isSidebarButton(b) && ((GuiButton) b).visible) {
                bottom = Math.max(bottom, ((GuiButton) b).yPosition + GuiWcwtToolbarButton.PLATE_H);
            }
        }
        this.sidebarBottom = Math.min(bottom, this.guiTop + this.ySize - CAP_H - 1);
    }

    /**
     * Carries one background slice over the sidebar, so the sidebar is the terminal's own background
     * continued to the left instead of a strip of its own.
     *
     * <p>
     * The slice's frame columns (texture x = 0, 1) are blitted at the sidebar's left edge, and its surface
     * columns (x = 2, 3) are tiled from there past the terminal's own left frame - both of its columns, the
     * 0x413F54 edge <em>and</em> the 0xF2F2F2 line on {@code offsetX + 1}. That last column is what puts the
     * plates at zero distance from the terminal: in the reference nothing separates a plate's right bezel
     * from the terminal's surface - the same 0xCBCCD4 flows on into the slot grid - while a leftover 0xF2F2F2
     * line read as a white seam drawn between the two.
     *
     * <p>
     * Rows are carried only down to {@link #sidebarBottom}: the surface stops a row under the last plate -
     * the foot row the closing border hangs below - and the terminal's own left frame carries on under the
     * cap, so the toolbar box ends where the toolbar ends instead of running the terminal's whole height.
     */
    private void blitSidebarSlice(final int offsetX, final int offsetY, final int v, final int height) {
        final int end = this.sidebarBottom + 1; // the sidebar paints rows [offsetY, end)
        if (offsetY >= end) {
            return;
        }
        final int h = Math.min(height, end - offsetY);
        final int sx = offsetX - SIDEBAR_W;
        blit512(sx, offsetY, 0, v, 2, h);
        for (int x = sx + 2; x <= offsetX; x += 2) {
            blit512(x, offsetY, 2, v, 2, h);
        }
        blit512(offsetX + 1, offsetY, 2, v, 1, h); // the terminal's 0xF2F2F2 line, repainted as surface
    }

    /**
     * Closes the sidebar with the window's own bottom border - texture rows {@link #CAP_V}..+{@link
     * #CAP_H}, the horizontal border (0xF2F2F2, two 0x878FA5 rows, 0x413F54) uniform across its width,
     * blitted two columns past the strip's frame ({@link #SIDEBAR_W} + 2 wide). Those extra two are the
     * terminal's own frame columns, which {@link #blitSidebarSlice} repaints as surface for as long as the
     * sidebar runs: left to the main art they would resurface beside the border's gray rows, the 0xF2F2F2
     * line reading as a white stub pinned to the cap and the 0x413F54 edge pricking through it. Carried
     * over instead, the line steps down into the border - the cap's own top row is the same 0xF2F2F2 - and
     * the frame picks up again under it, the same junction the buses' wrapping plate makes with its bottom
     * band.
     *
     * <p>
     * One column is then given back: the border art is uniform past its corner column, but the buses' band
     * keeps its rightmost column 0xF2F2F2 through all four rows - their texture's last column is the
     * panel's own white line, and the line runs down the band's right edge unbroken into the frame below.
     * Blitted as-is the cap's gray and dark rows would bite three rows out of that line, leaving a white
     * stub that only resumes under the dark edge; so the cap's last column is redrawn from the border's
     * own white rule row, putting the line back the way the buses wear it.
     */
    private void drawSidebarCap(final int offsetX) {
        final int y = this.sidebarBottom + 1;
        blit512(offsetX - SIDEBAR_W, y, 0, CAP_V, SIDEBAR_W + 2, CAP_H);
        // blit512 is 1:1, so one 4-tall blit here would replay the border's own four rows; instead the
        // white rule pixel is stamped down the column, one blit per band row.
        for (int i = 0; i < CAP_H; i++) {
            blit512(offsetX + 1, y + i, 1, CAP_V, 1, 1);
        }
    }

    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY) {
        // TEMP DIAGNOSTIC (1.0.37, remove once the terminal-open delay is pinned down).
        final boolean diag = this.diagBgs < 3;
        final long diagT0 = diag ? System.currentTimeMillis() : 0;
        // Re-place the cache slots from the current state, before anything draws. This has to happen
        // here and every frame, not just when the enlargement switch flips: the slots are positioned
        // by their xDisplayPosition/yDisplayPosition, which vanilla consumes when it renders them
        // between the background and the foreground pass - so a relayout done from drawFG (where the
        // list is drawn) only takes effect on the next frame and leaves one frame of the old layout
        // on screen. Doing it in the background pass keeps the switch a single-frame change.
        this.layoutPatternCache();
        bindTextureBack(BACKGROUND);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        // Every slice is carried over the sidebar as it is drawn, so the sidebar is the terminal's own
        // background continued to the left - one frame, one surface, one top and bottom border.
        // Header.
        blit512(offsetX, offsetY, 0, 0, GUI_WIDTH, HEADER_H);
        blitSidebarSlice(offsetX, offsetY, 0, HEADER_H);
        // First row.
        blit512(offsetX, offsetY + HEADER_H, 0, 17, GUI_WIDTH, ROW_H);
        blitSidebarSlice(offsetX, offsetY + HEADER_H, 17, ROW_H);
        // Repeating middle rows.
        for (int i = 1; i < this.rows - 1; i++) {
            final int y = offsetY + HEADER_H + ROW_H + (i - 1) * ROW_H;
            blit512(offsetX, y, 0, REF_HEIGHT, GUI_WIDTH, ROW_H);
            blitSidebarSlice(offsetX, y, REF_HEIGHT, ROW_H);
        }
        // Last row.
        if (this.rows >= 2) {
            final int y = offsetY + HEADER_H + this.rows * ROW_H - ROW_H;
            blit512(offsetX, y, 0, 35, GUI_WIDTH, ROW_H);
            blitSidebarSlice(offsetX, y, 35, ROW_H);
        }
        // Bottom slice (crafting / encoding / cache / player inventory areas).
        final int bottomY = offsetY + HEADER_H + this.rows * ROW_H;
        blit512(offsetX, bottomY, 0, REF_BOTTOM_TOP, GUI_WIDTH, BOTTOM_H);
        blitSidebarSlice(offsetX, bottomY, REF_BOTTOM_TOP, BOTTOM_H);
        // Close the sidebar under the last plate with the window's own bottom border.
        this.drawSidebarCap(offsetX);
        // Encoding cells: the WCWT texture keeps this panel flat because AE2 1.21 blits its own slot
        // sprite on top at runtime, so the cells are painted here (same place in the layer order:
        // after the background, before the items).
        // WCWT draws the mode's encoding-panel art here, before the slots: the grid guides and, in
        // crafting mode, the crafting arrow. Without it the arrow is simply absent.
        this.drawPatternModeBackground(offsetX, offsetY);
        this.drawEncodingSlotBackgrounds(offsetX, offsetY);
        this.drawCacheBandMask(offsetX, offsetY);
        this.drawEncodingScroll(offsetX, offsetY);
        this.drawCacheScroll(offsetX, offsetY);
        // The upgrade panel hangs off the terminal's right frame, outside the texture like the
        // sidebar is on the left, so it is drawn from the background layer rather than baked in.
        this.drawUpgradePanel(offsetX, offsetY);
        // After the panel art, which paints the cells the seats sit in: AE2 1.21's own sprites for
        // the empty ones - see SlotWcwtCard.
        com.asdflj.wtct.client.gui.container.slot.SlotWcwtCard
            .drawHints(this.mc, offsetX, offsetY, this.inventorySlots.inventorySlots);
        com.asdflj.wtct.client.gui.container.slot.SlotWcwtCard.drawHint(
            this.mc,
            offsetX,
            offsetY,
            this.monitorableContainer.getUpgradeSingularitySlot(),
            com.asdflj.wtct.client.gui.container.slot.SlotWcwtCard.HINT_SINGULARITY_U,
            com.asdflj.wtct.client.gui.container.slot.SlotWcwtCard.HINT_SINGULARITY_V);
        if (this.mgmtSearch != null) {
            this.mgmtSearch.drawTextBox();
        }
        if (this.mgmtMappingField != null) {
            this.mgmtMappingField.drawTextBox();
        }
        if (this.searchField != null) {
            this.searchField.drawTextBox();
        }
        // Armor column (WCWT AE2WTLIB_* slots): the texture leaves the cells flat, so paint them
        // like the encoding cells, and draw vanilla's empty-armor placeholder icons.
        for (final Object o : this.inventorySlots.inventorySlots) {
            if (!(o instanceof final ContainerComprehensiveWorkTerminal.SlotPlayerArmor s)
                || s.xDisplayPosition < 0) {
                continue;
            }
            final int x = this.guiLeft + s.xDisplayPosition;
            final int y = this.guiTop + s.yDisplayPosition;
            this.mc.getTextureManager()
                .bindTexture(WIDGETS_TEXTURE);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            drawTexturedModalRect(x - 1, y - 1, 56, 0, 18, 18);
            if (s.getStack() == null) {
                final IIcon icon = ItemArmor.func_94602_b(s.armorTypeIndex());
                if (icon != null) {
                    this.mc.getTextureManager()
                        .bindTexture(TextureMap.locationItemsTexture);
                    GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                    this.drawTexturedModelRectFromIcon(x, y, icon, 16, 16);
                }
            }
        }
        // WCWT renders the live player entity inside the texture's black panel, centred: at scale 30
        // the model is 54px tall in the 70px panel, so the feet sit 8px above the panel's bottom.
        // Drawn in this untranslated layer; nothing else (slots, buttons) overlaps the box. Wrapped
        // in PushAttrib/PopAttrib: func_147046_a leaves GL_RESCALE_NORMAL disabled and its own
        // lighting state behind.
        if (this.mc.thePlayer != null) {
            final int cx = this.guiLeft + PLAYER_BOX_X + PLAYER_BOX_W / 2;
            final int feet = frameY(PLAYER_BOX_TOP) + PLAYER_BOX_H - (PLAYER_BOX_H - PLAYER_MODEL_H) / 2;
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            // 1.7.10: GuiInventory's entity renderer is the static func_147046_a (MCP
            // drawEntityOnScreen): (x, feetY, scale, yaw, pitch, entity).
            GuiInventory.func_147046_a(cx, feet, PLAYER_SCALE, cx - mouseX, feet - 50 - mouseY, this.mc.thePlayer);
            GL11.glPopAttrib();
        }
        // GuiScreen.drawRect leaves glColor at whatever it last drew - for us that is the dark
        // translucent tint of the scrollbars/toggles - and NEI draws its item panel after this GUI
        // without resetting it, so the leftover colour dims everything it draws. Hand control on
        // with a clean state instead.
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        if (diag) {
            cpw.mods.fml.common.FMLLog.info(
                "[wtct-diag] client drawBG call=%d took=%dms t=%d",
                this.diagBgs, System.currentTimeMillis() - diagT0, diagT0);
            this.diagBgs++;
        }
    }

    /**
     * Blits AE2 1.21's per-mode encoding-panel art (WCWT's renderPatternEncodingBackground): a 124x66
     * region of pattern_modes.png at the encoding background origin, 70px apart per mode variant.
     */
    private void drawPatternModeBackground(final int guiX, final int guiY) {
        this.mc.getTextureManager()
            .bindTexture(new ResourceLocation(Wtct.MODID, "textures/" + PATTERN_MODES));
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        final int srcY = this.monitorableContainer.craftingMode ? MODES_TEX_CRAFT_Y : MODES_TEX_PROCESS_Y;
        blit256(
            guiX + ENC_PANEL_X,
            guiY + bottomStartRel() + (ENC_PANEL_Y - REF_BOTTOM_TOP),
            0,
            srcY,
            ENC_PANEL_W,
            ENC_PANEL_H);
    }

    /** drawTexturedModalRect with 256-based UV normalisation (pattern_modes.png is 256x256). */
    private void blit256(final int x, final int y, final int srcX, final int srcY, final int w, final int h) {
        final float f = 1.0F / 256.0F;
        final Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();
        tess.addVertexWithUV(x, y + h, this.zLevel, srcX * f, (srcY + h) * f);
        tess.addVertexWithUV(x + w, y + h, this.zLevel, (srcX + w) * f, (srcY + h) * f);
        tess.addVertexWithUV(x + w, y, this.zLevel, (srcX + w) * f, srcY * f);
        tess.addVertexWithUV(x, y, this.zLevel, srcX * f, srcY * f);
        tess.draw();
    }

    /**
     * Paints a cell for every encoding-area slot that is currently on screen.
     *
     * <p>
     * The cell is 16x16 starting at the slot position, matching the area vanilla treats as the slot
     * ({@code GuiContainer} hit-tests {@code xDisplayPosition} with a size of 16) and where the item
     * is rendered. Drawing 18x18 here would sit one pixel low and right of the item, and would also
     * close the 2px gap the 18px grid leaves between cells.
     */
    private void drawEncodingSlotBackgrounds(final int guiX, final int guiY) {
        for (final Object o : this.inventorySlots.inventorySlots) {
            if (!(o instanceof final AppEngSlot s) || s.xDisplayPosition < 0
                || !this.monitorableContainer.isEncodingAreaSlot(s)) {
                continue;
            }
            final int x = guiX + s.xDisplayPosition;
            final int y = guiY + s.yDisplayPosition;
            // Slots inside the mode art (the input grid, the preview and the output column) already
            // have their cells painted by pattern_modes.png, with uniform 2px separators - blitting
            // SLOT_BACKGROUND over them would add the sprite's panel-coloured top line and make the
            // vertical spacing read one pixel wider. Only slots outside the art get a cell sprite.
            if (x - guiX >= ENC_PANEL_X && x - guiX < ENC_PANEL_X + ENC_PANEL_W) {
                continue;
            }
            this.mc.getTextureManager()
                .bindTexture(WIDGETS_TEXTURE);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            drawTexturedModalRect(x - 1, y - 1, 56, 0, 18, 18);
        }
    }

    /**
     * Draws the encoding scrollbar in AE2 1.21's style: a dark recessed groove with a short light knob
     * riding in it. It replaces AE2 1.7.10's GuiScrollbar here, which only blits a vanilla-looking
     * knob and expects the screen art to provide the groove.
     */
    private void drawEncodingScroll(final int guiX, final int guiY) {
        // AE2 1.21's Scrollbar contract: the track is pre-baked in the screen's background art - the
        // processing-mode region of pattern_modes.png carries the groove line at texture x8 (GUI
        // x184) - so only the handle is drawn here. Like the original, the handle shows whenever
        // processing mode is active, using its disabled sprite while there is nothing to scroll.
        if (this.monitorableContainer.craftingMode) {
            return;
        }
        drawScrollbarAt(
            guiX + ENC_SCROLL_X,
            guiY + bottomStartRel() + (ENC_SCROLL_Y - REF_BOTTOM_TOP),
            ENC_SCROLL_H,
            this.encScroll,
            this.maxEncScroll());
    }

    /** drawTexturedModalRect with 512-based UV normalisation (the texture is 512x512). */
    private void blit512(int x, int y, int srcX, int srcY, int w, int h) {
        final float f = 1.0F / 512.0F;
        final Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();
        tess.addVertexWithUV(x, y + h, this.zLevel, srcX * f, (srcY + h) * f);
        tess.addVertexWithUV(x + w, y + h, this.zLevel, (srcX + w) * f, (srcY + h) * f);
        tess.addVertexWithUV(x + w, y, this.zLevel, (srcX + w) * f, srcY * f);
        tess.addVertexWithUV(x, y, this.zLevel, srcX * f, srcY * f);
        tess.draw();
    }

    @Override
    public void bindTextureBack(final String file) {
        this.mc.getTextureManager()
            .bindTexture(new ResourceLocation(Wtct.MODID, "textures/" + file));
    }

    @Override
    public void postStackUpdate(List<? extends IAEStack<?>> list) {
        for (IAEStack<?> stack : list) {
            this.repo.postUpdate(stack);
        }
        this.repo.updateView();
        this.setScrollBar();
        // The list just moved under the badges: every remembered answer may be stale now.
        this.craftableMarks.clear();
    }

    // =============================================================================================
    // Pattern management area (WCWT 样板管理区)
    //
    // WCWT's player inventory sits on the LEFT (x=8..170) and the management list on the RIGHT
    // (management_page: left 176, bottom 82, 160x72) - directly below the pattern cache and above
    // nothing else: both are always on screen and never overlap. That is also why this port needs
    // no page mechanism and no hidden player slots; our layout leaves that band empty.
    //
    // The data comes from AE2's own interface-terminal packet: this terminal's host is a
    // WirelessDualInterfaceTerminalInventory, i.e. an IInterfaceTerminal, so the container can hold
    // an AE2 ContainerInterfaceTerminal and its rows arrive here as PacketInterfaceTerminalUpdate
    // (same path the wireless dual-interface terminal uses).
    // =============================================================================================

    /** WCWT management_page: left 176, bottom 82, 160x72 (texture Y = 288 - bottom). */
    private static final int MGMT_X = 176;
    private static final int MGMT_TEX_Y = 206;
    private static final int MGMT_W = 160;
    private static final int MGMT_H = 72;
    /** Row metrics: header row 18px, slot row 18px, 9 columns of 18, like WCWT. */
    private static final int MGMT_ROW_H = 18;
    private static final int MGMT_COLS = 9;
    /** A row's cell that stands for the provider's remaining empty slots (WCWT's {@code -1} entry). */
    private static final int EMPTY_SLOTS_CELL = -1;
    /** The cell list of a header row: a provider header has no slots at all. */
    private static final int[] HEADER_ROW = new int[0];
    /** WCWT manage_scrollbar: left 343, bottom 82, height 72. */
    private static final int MGMT_SCROLL_X = 343;
    private static final int MGMT_SCROLL_TEX_Y = 206;
    private static final int MGMT_SCROLL_H = 72;
    /**
     * WCWT's in-row buttons: JSON left 285 / 306 / 338 minus the page's own left (176). The upload
     * and open-UI plates sit on the header row; the little highlight sprite hangs past the page's
     * right edge, in the 7px strip before the scrollbar - which is why the list scissor has to grow
     * by {@link #MGMT_HIGHLIGHT_OVERHANG} for it.
     */
    private static final int MGMT_UPLOAD_X = 285 - MGMT_X;
    private static final int MGMT_UI_X = 306 - MGMT_X;
    private static final int MGMT_HIGHLIGHT_X = 338 - MGMT_X;
    private static final int MGMT_ROWBTN_W = 14;
    private static final int MGMT_ROWBTN_H = 15;
    /** Top of the two plates inside their row (WCWT's JSON top). */
    private static final int MGMT_ROWBTN_TOP = 2;
    /** WCWT manage_highlight_provider: top 4, drawn 5x10 out of a 6x11 source sprite. */
    private static final int MGMT_HIGHLIGHT_TOP = 4;
    private static final int MGMT_HIGHLIGHT_W = 5;
    private static final int MGMT_HIGHLIGHT_H = 10;
    /** How far the highlight button reaches past the page's right edge (338 + 5 - 336). */
    private static final int MGMT_HIGHLIGHT_OVERHANG = MGMT_HIGHLIGHT_X + MGMT_HIGHLIGHT_W - MGMT_W;
    /** The row buttons' plate: WCWT states sheet, idle at (161,0), the hovered one 16px to the right. */
    private static final String MGMT_STATES_TEXTURE = "guis/wcwt/wcwt_states.png";
    private static final int MGMT_ROWBTN_PLATE_U = 161;
    private static final int MGMT_ROWBTN_PLATE_V = 0;
    /** WCWT reuses AE2's states sheet for the upload arrow; our copy of it carries the same sprite. */
    private static final String MGMT_AE2_STATES_TEXTURE = "guis/wcwt/ae2_toolbar_states.png";
    private static final int MGMT_UPLOAD_ICON_U = 0;
    private static final int MGMT_UPLOAD_ICON_V = 144;
    private static final int MGMT_UPLOAD_ICON_SIZE = 16;
    /** The open-UI glyph in WCWT's own states sheet. */
    private static final int MGMT_UI_ICON_U = 52;
    private static final int MGMT_UI_ICON_V = 5;
    private static final int MGMT_UI_ICON_W = 8;
    private static final int MGMT_UI_ICON_H = 7;
    /**
     * ExtendedAE's 64x64 icon sheet, bundled: WCWT's highlight button cuts the 6x11 sprite at
     * (48,32) and draws it 5x10, one pixel shrunk.
     */
    private static final String MGMT_HIGHLIGHT_TEXTURE = "guis/wcwt/ex_nicons.png";
    private static final int MGMT_HIGHLIGHT_ICON_U = 48;
    private static final int MGMT_HIGHLIGHT_ICON_V = 32;
    private static final int MGMT_HIGHLIGHT_ICON_SRC_W = 6;
    private static final int MGMT_HIGHLIGHT_ICON_SRC_H = 11;
    /** WCWT automatic_upload: left 217, bottom 97, 14x14 - the icon-less toggle see {@link GuiWcwtMgmtToggle}. */
    private static final int MGMT_AUTOUPLOAD_X = 217;
    private static final int MGMT_AUTOUPLOAD_TEX_Y = 191;
    /** The chrome's bottom edge in WCWT's texture space (the second mapping-plate row's last pixel). */
    private static final int MGMT_CHROME_BOTTOM_TEX_Y = 204;
    /**
     * The header chrome's top edge in WCWT's texture space: the switch row's first pixel. The chrome
     * is drawn as three stacked rows (switches, provider search, mapping field + the four mapping
     * plates), so its height is fixed and both edges are needed to place it.
     */
    private static final int MGMT_CHROME_TOP_TEX_Y = 181;
    /** Height of the header chrome, in texture pixels: the switch row down to the last mapping plate. */
    private static final int MGMT_CHROME_H = MGMT_CHROME_BOTTOM_TEX_Y - MGMT_CHROME_TOP_TEX_Y + 1;
    /** WCWT manage_search: left 230, bottom 107, 60x12 - provider search, and the upload match word. */
    private static final int MGMT_SEARCH_X = 230;
    private static final int MGMT_SEARCH_TEX_Y = 181;
    private static final int MGMT_SEARCH_W = 60;
    private static final int MGMT_SEARCH_H = 12;
    /** Item z-levels, matching the ones the interface terminal uses for its own rows. */
    /**
     * WCWT's pattern mapping widgets (样板映射): the mapping field (230, bottom 96 -> texture Y 192,
     * 60x12) and four 30x11 text buttons - 增加/重载 on texture Y 181, 删除/取消 on 193. The mapping
     * binds the key typed in the field to the provider named in the search box, so the pattern that
     * is encoded next can be uploaded without picking a provider by hand.
     */
    private static final int MGMT_MAPPING_X = 230;
    private static final int MGMT_MAPPING_TEX_Y = 192;
    private static final int MGMT_MAPPING_W = 60;
    private static final int MGMT_MAPPING_H = 12;
    private static final int MGMT_MAPBTN_ADD_X = 291;
    private static final int MGMT_MAPBTN_ALT_X = 322;
    private static final int MGMT_MAPBTN_ROW1_TEX_Y = 181;
    /** Lang suffixes of the four mapping buttons' tooltips, in WCWT's own order. */
    private static final String[] MAPPING_TIPS = { "add", "reload", "delete", "cancel" };
    /** The four text buttons' 60x22 plate sprites in wcwt_widgets.png (WCWT's own art). */
    private static final int MAPPING_BTN_NORMAL_V = 48;
    private static final int MAPPING_BTN_HOVER_V = 72;
    private static final int MGMT_MAPBTN_ROW2_TEX_Y = 193;
    private static final int MGMT_MAPBTN_W = 30;
    private static final int MGMT_MAPBTN_H = 11;
    private static final float MGMT_ITEM_Z = 100.0F;
    private static final float MGMT_ITEM_OVERLAY_Z = 200.0F;
    /** Slot sprite: WCWT slots the first 18x18 cell of wcwt_management.png. */
    private static final String MGMT_TEXTURE = "guis/wcwt/wcwt_management.png";
    /** Selection highlight over a provider's slot row. */
    private static final int MGMT_SELECTED_TINT = 0x40FFFFFF;

    /** One pattern provider: an AE2 interface-terminal row. */
    private static final class PatternProvider {

        private long id = -1;
        private String name = "";
        /**
         * The name AE2 sent, before it was localised - kept because it carries what the display name
         * drops: GTNH's interface enhancement packs the targeted recipe type into it, so a player who
         * searches for "gt.recipe.press" still finds the hatch sitting on a press.
         */
        private String rawName = "";
        private int x;
        private int y;
        private int z;
        private int dim;
        private int side;
        /** AE2's display representation of the interface, i.e. WCWT's header badge (/16 -> 8px). */
        private ItemStack icon;
        private ItemStack[] slots = new ItemStack[0];
        /**
         * AE2's "show in interface terminal" switch, carried by every add and by an overwrite whose
         * {@code terminalVisibleValid} is set (that is how the server reports a flip of the button in
         * the interface's own GUI). The list honours it: a machine hidden from the interface terminal
         * stays out of this one too.
         */
        private boolean terminalVisible = true;
    }

    /** A list row: either a provider header or one line of its pattern slots. */
    private static final class MgmtRow {

        private final PatternProvider provider;
        /** The provider slot each cell of this row shows; see {@link #displayedMgmtSlots}. */
        private final int[] slots;

        private MgmtRow(final PatternProvider provider, final int[] slots) {
            this.provider = provider;
            this.slots = slots;
        }

        private boolean isHeader() {
            return this.slots.length == 0;
        }
    }

    private final List<PatternProvider> patternProviders = new ArrayList<>();
    private final List<MgmtRow> mgmtRows = new ArrayList<>();
    private int mgmtScroll;
    private boolean mgmtRowsDirty = true;
    private boolean draggingMgmtKnob;
    /** The provider the encode key uploads to first; also the highlighted row. */
    private long selectedProviderId = -1;
    /** Plus' {@code lastProviderSearchKey}: the term the last upload used, reused on the next one. */
    private String lastProviderSearchKey = "";
    /**
     * The key the last resolve started from - the recipe type for a processing pattern. Kept so the
     * provider chooser can show what the type was next to what it resolved to.
     */
    private String lastUploadSearchKey = "";
    /** True when the last search term was only a default (remembered / crafting), not player input. */
    private boolean lastSearchWasDefault;
    /** WCWT's provider search box: filters the list, and is the match word the upload uses. */
    private GuiWcwtTextField mgmtSearch;
    /** WCWT's mapping key field (样板映射输入框). */
    private GuiWcwtTextField mgmtMappingField;
    /**
     * Hover hint of the four mapping plates, recorded while they are drawn and raised at the very end
     * of the foreground pass. Drawn where it is decided - inside the button loop - the plates that
     * come after it painted over it.
     */
    private String mappingTooltip;

    private static final boolean NEI_PRESENT = Loader.isModLoaded("NotEnoughItems");

    /**
     * The pattern the cursor is on in the management list, recorded as the tooltip is assembled. AE2's
     * NEI integration asks the GUI for this ({@code IGuiTooltipHandler#getHoveredStack}) before
     * falling back to the slot under the mouse, which is what lets NEI's keys work on the management
     * area's cells - they are painted by hand and are not real slots. The interface terminal exposes
     * its own patterns the same way.
     */
    private ItemStack hoveredMgmtPattern;

    /**
     * WCWT's craftable-pattern marker: the item keys the network lists as craftable, rebuilt at most
     * every 250ms, so the encoding cells can show the little blue diamond without walking the whole
     * repo list every frame.
     */
    // (superseded: craftable keys now arrive via SPacketCraftableKeys, see craftableKeys)

    @Override
    public void postUpdate(final List<PacketInterfaceTerminalUpdate.PacketEntry> updates, final int statusFlags) {
        if ((statusFlags & PacketInterfaceTerminalUpdate.CLEAR_ALL_BIT) != 0) {
            this.patternProviders.clear();
        }
        for (final PacketInterfaceTerminalUpdate.PacketEntry entry : updates) {
            this.applyProviderUpdate(entry);
        }
        this.mgmtRowsDirty = true;
    }

    /**
     * AE2 sends the interface's raw name, and GTNH's interface fills that with the unlocalized name of
     * the machine it targets ({@code DualityInterface#getRawTermName} → e.g. "ic2.blockCompressor"),
     * so translate it before it reaches the list, the tooltip and the search filter. A custom name set
     * through {@code PacketRename} skips this - it is already what the player typed.
     */
    private static String localizeProviderName(final String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        // GTNH's interface enhancement (sciencenotleisure) rewrites an interface's name into a marker
        // sentence when it sits on a GT machine - "gt_circuit_0_extra_start_gt.recipe.press_extra_end_
        // extra_item_start_..._extra_item_end_gt.blockmachines.basicmachine.press.tier.03" - and AE2's
        // own interface terminal cleans those up through that mod's helper. Same call here, so the list
        // shows the machine instead of the sentence.
        final String enhanced = ExtraInterfaceName.clean(raw);
        if (enhanced != null) {
            return enhanced;
        }
        for (final String key : new String[] { raw, raw + ".name" }) {
            final String translated = net.minecraft.util.StatCollector.translateToLocal(key);
            if (!translated.equals(key)) {
                // AE2 leaves a " - %s" placeholder in the name for its adjacent-machine suffix; the
                // interface terminal fills it in, we have no argument to give, so drop it.
                return translated.replace(" - %s", "")
                    .replace("%s", "")
                    .trim();
            }
        }
        return raw.replace(" - %s", "")
            .replace("%s", "")
            .trim();
    }

    /**
     * GTNH's interface enhancement, reached reflectively because it lives in a mod this one does not
     * build against and may not be installed at all.
     *
     * <p>
     * Its {@code com.science.gtnl.utils.Utils.getExtraInterfaceName} is the same call AE2's interface
     * terminal makes on the names it receives; without it the pattern list would show the marker
     * sentence instead of the machine. Only the marker-laden names are handed over - everything else
     * is AE2's own naming, which {@link #localizeProviderName} already handles - and a name the helper
     * cannot make sense of (or a helper that blows up) falls back to that same localisation.
     */
    private static final class ExtraInterfaceName {

        private static final String MARKER_START = "gt_circuit_";
        private static final String MARKER_EXTRA = "extra_start_";
        private static final String MARKER_EXTRA_ITEM = "extra_item_start_";

        private static final Method METHOD = find();

        private static Method find() {
            try {
                final Method method = Class.forName("com.science.gtnl.utils.Utils")
                    .getMethod("getExtraInterfaceName", String.class);
                return Modifier.isStatic(method.getModifiers()) ? method : null;
            } catch (final Throwable ignored) {
                return null;
            }
        }

        private ExtraInterfaceName() {}

        /** The cleaned name, or null when it is not a marked one or nothing can clean it. */
        private static String clean(final String raw) {
            if (METHOD == null || !isMarked(raw)) {
                return null;
            }
            try {
                final Object cleaned = METHOD.invoke(null, raw);
                final String name = cleaned == null ? ""
                    : cleaned.toString()
                        .trim();
                return name.isEmpty() ? null : name;
            } catch (final Throwable ignored) {
                // A name we cannot clean costs the machine's label, nothing else.
                return null;
            }
        }

        /** True for the names the enhancement rewrote; see {@link #localizeProviderName}. */
        private static boolean isMarked(final String raw) {
            return raw.startsWith(MARKER_START) || raw.contains(MARKER_EXTRA) || raw.contains(MARKER_EXTRA_ITEM);
        }
    }

    private void applyProviderUpdate(final PacketInterfaceTerminalUpdate.PacketEntry entry) {
        if (entry instanceof PacketInterfaceTerminalUpdate.PacketAdd add) {
            final PatternProvider provider = new PatternProvider();
            provider.id = add.entryId;
            provider.rawName = add.name == null ? "" : add.name;
            provider.name = localizeProviderName(add.name);
            provider.x = add.x;
            provider.y = add.y;
            provider.z = add.z;
            provider.dim = add.dim;
            provider.side = add.side;
            provider.icon = add.dispRep != null ? add.dispRep : add.selfRep;
            provider.slots = readProviderSlots(add.items, add.rowSize * add.rows);
            provider.terminalVisible = add.terminalVisible;
            this.patternProviders.add(provider);
        } else if (entry instanceof PacketInterfaceTerminalUpdate.PacketRemove) {
            this.patternProviders.removeIf(provider -> provider.id == entry.entryId);
        } else if (entry instanceof PacketInterfaceTerminalUpdate.PacketRename rename) {
            for (final PatternProvider provider : this.patternProviders) {
                if (provider.id == entry.entryId) {
                    provider.rawName = rename.newName == null ? "" : rename.newName;
                    provider.name = rename.newName == null ? "" : rename.newName;
                }
            }
        } else if (entry instanceof PacketInterfaceTerminalUpdate.PacketOverwrite overwrite) {
            for (final PatternProvider provider : this.patternProviders) {
                if (provider.id != entry.entryId) {
                    continue;
                }
                if (overwrite.itemsValid) {
                    if (overwrite.allItemUpdate) {
                        provider.slots = readProviderSlots(overwrite.items, overwrite.validIndices.length);
                    } else {
                        for (int i = 0; i < overwrite.validIndices.length; i++) {
                            final int slot = overwrite.validIndices[i];
                            if (slot >= 0 && slot < provider.slots.length) {
                                provider.slots[slot] = readProviderSlot(overwrite.items, i);
                            }
                        }
                    }
                }
                // AE2 reports a flip of the interface's own "show in interface terminal" button with
                // an overwrite that carries nothing but the visibility flag, so it has to be handled
                // here even when no items came along with it.
                if (overwrite.terminalVisibleValid) {
                    provider.terminalVisible = overwrite.terminalVisible;
                }
            }
        }
    }

    /** AE2 ships one item NBT per slot, an empty compound standing for an empty slot. */
    private static ItemStack[] readProviderSlots(final NBTTagList items, final int size) {
        final ItemStack[] slots = new ItemStack[Math.max(0, size)];
        for (int i = 0; i < slots.length; i++) {
            slots[i] = readProviderSlot(items, i);
        }
        return slots;
    }

    private static ItemStack readProviderSlot(final NBTTagList items, final int index) {
        if (items == null || index < 0 || index >= items.tagCount()) {
            return null;
        }
        final NBTTagCompound tag = items.getCompoundTagAt(index);
        return tag == null || !tag.hasKey("id") ? null : ItemStack.loadItemStackFromNBT(tag);
    }

    /**
     * Rows are rebuilt lazily: a provider takes one header row, then one row per nine displayed slots -
     * and, with the empty slots folded, "displayed" is only the occupied ones plus the summary cell.
     */
    private void rebuildMgmtRows() {
        this.mgmtRows.clear();
        // WCWT filters the list by the *resolved* search text, so a recipe-type key or alias in the
        // box still narrows the list down to the provider the mapping points at.
        final String filter = this.resolvedMgmtSearch()
            .toLowerCase(java.util.Locale.ROOT);
        for (final PatternProvider provider : this.orderedMgmtProviders(filter)) {
            this.mgmtRows.add(new MgmtRow(provider, HEADER_ROW));
            // WCWT's show-slots switch: with it off the list keeps only the provider headers.
            if (this.mgmtShowSlots()) {
                final int[] shown = displayedMgmtSlots(provider);
                for (int offset = 0; offset < shown.length; offset += MGMT_COLS) {
                    this.mgmtRows.add(
                        new MgmtRow(
                            provider,
                            java.util.Arrays.copyOfRange(shown, offset, Math.min(offset + MGMT_COLS, shown.length))));
                }
            }
        }
        this.mgmtRowsDirty = false;
        this.mgmtSeenSettings = this.mgmtSettings();
    }

    /**
     * The providers the list shows, in the order it shows them, with a search narrowing it down first.
     *
     * <p>
     * What the search is aimed at comes first, then what it also happens to find. Three buckets, in
     * order: the machine family the mapping box's key names - its voltage tiers, led by the one the search
     * was aimed at rather than by whichever interface the interface terminal happened to report first -
     * then the interfaces the search finds <em>by name</em>, then the ones it only finds <em>inside a
     * pattern</em>. The box is labelled "provider search", so typing a machine's name has to put that
     * machine's own interfaces at the top; searching "组装机" used to lead with whichever interface sat
     * on a circuit assembling machine, and before that with whichever merely held a recipe for one.
     *
     * <p>
     * The filter still decides what is listed - this only decides the order. With no search at all the
     * incoming interface-terminal order is what the list keeps, unless the mapping box names a family, in
     * which case that family leads.
     *
     * <p>
     * The machine's own hide button and the "not full" display mode are honoured here as well, so this is
     * the only place that decides what the list contains.
     */
    private List<PatternProvider> orderedMgmtProviders(final String filter) {
        final String family = this.searchFamily();
        final List<PatternProvider> ofFamily = new ArrayList<>();
        final List<PatternProvider> byName = new ArrayList<>();
        final List<PatternProvider> byPattern = new ArrayList<>();
        for (final PatternProvider provider : this.patternProviders) {
            // A machine hidden from AE2's interface terminal stays out of this list as well: the
            // packet carries the interface's own switch, and honouring it here is what makes the
            // hide button in the interface's GUI mean anything in this terminal.
            if (!provider.terminalVisible) {
                continue;
            }
            if (!this.passesMgmtDisplayMode(provider)) {
                continue;
            }
            final boolean named = filter.isEmpty() || providerNameMatches(provider, filter);
            if (!named && !this.providerSlotsMatch(provider, filter)) {
                continue;
            }
            if (named && !family.isEmpty() && family.equals(machineFamily(provider.rawName))) {
                ofFamily.add(provider);
            } else if (named) {
                byName.add(provider);
            } else {
                byPattern.add(provider);
            }
        }
        ofFamily.addAll(byName);
        ofFamily.addAll(byPattern);
        return ofFamily;
    }

    /**
     * Which of a provider's slots the list actually shows.
     *
     * <p>
     * WCWT's auto-compact: the empty slots are folded away and the row keeps a single cell that stands
     * for however many are left, so a provider holding three patterns takes one line instead of four.
     * Occupied slots keep their order, and the summary cell is only added while the provider still has
     * room - a full provider has nothing left to summarise. The summary cell is drawn with the number of
     * hidden slots on it and, like WCWT's, does not act on a click.
     */
    private static int[] displayedMgmtSlots(final PatternProvider provider) {
        int occupied = 0;
        for (final ItemStack stack : provider.slots) {
            if (stack != null) {
                occupied++;
            }
        }
        final boolean summarise = provider.slots.length > occupied;
        final int[] shown = new int[occupied + (summarise ? 1 : 0)];
        int at = 0;
        for (int i = 0; i < provider.slots.length; i++) {
            if (provider.slots[i] != null) {
                shown[at++] = i;
            }
        }
        if (summarise) {
            shown[at] = EMPTY_SLOTS_CELL;
        }
        return shown;
    }

    /** True when one of the provider's patterns has the search term in its name (WCWT's filter). */
    /**
     * WCWT's {@code isPatternProviderVisibleInManagement}: ALL and VISIBLE both keep every provider,
     * NOT_FULL keeps those that still have a free slot.
     */
    private boolean passesMgmtDisplayMode(final PatternProvider provider) {
        if (this.mgmtDisplayMode() != 2) {
            return true;
        }
        for (final ItemStack stack : provider.slots) {
            if (stack == null) {
                return true;
            }
        }
        return false;
    }

    /**
     * WCWT's search: the text is matched against the provider name and, depending on the search
     * scope, against the patterns' inputs and/or outputs.
     */
    private boolean providerSlotsMatch(final PatternProvider provider, final String filter) {
        for (final ItemStack stack : provider.slots) {
            if (stack == null) {
                continue;
            }
            final List<List<ItemStack>> cells = com.asdflj.wtct.util.PatternScaling.readPatternCells(stack);
            if (cells == null) {
                if (itemNameMatches(stack, filter)) {
                    return true;
                }
                continue;
            }
            // Outputs are matched in OUT and IN_OUT, inputs in IN and IN_OUT.
            if (this.mgmtSearchMode() != 1 && this.cellsMatch(cells.get(1), filter)) {
                return true;
            }
            if (this.mgmtSearchMode() != 0 && this.cellsMatch(cells.get(0), filter)) {
                return true;
            }
        }
        return false;
    }

    private boolean cellsMatch(final List<ItemStack> cells, final String filter) {
        if (cells == null) {
            return false;
        }
        for (final ItemStack stack : cells) {
            if (itemNameMatches(stack, filter)) {
                return true;
            }
        }
        return false;
    }

    private static boolean itemNameMatches(final ItemStack stack, final String filter) {
        return stack != null && nameMatches(stack.getDisplayName(), filter);
    }

    /**
     * Case-insensitive containment that also understands pinyin, so the management area's search box
     * finds an interface or a pattern whose name is Chinese by typing its initials or full pinyin.
     * Without a pinyin mod installed {@link NeCharUtil} falls back to a plain containment test.
     */
    private static boolean nameMatches(final String name, final String filter) {
        return name != null && NeCharUtil.INSTANCE.contains(filter, name.toLowerCase(java.util.Locale.ROOT));
    }

    /**
     * The provider's name against the filter. The display name and the raw one AE2 sent are both tried:
     * GTNH's interface enhancement folds the machine and the recipe type it is set to into the raw name
     * and the display name keeps only the machine, so a search for "gt.recipe.press" has to keep
     * finding the hatch that sits on a press.
     */
    private static boolean providerNameMatches(final PatternProvider provider, final String filter) {
        return nameMatches(provider.name, filter) || nameMatches(provider.rawName, filter);
    }

    /** Keeps the three management toggles' icons, pressed look and tooltips in step with the state. */
    /**
     * The four management switches, read from the container's synced copy of what the terminal item
     * holds. They are not cached here: the item is the authority (WCWT puts them there too), so a
     * terminal opens with the values it was left with, and a click is a round trip that comes back
     * through the field sync. The fallbacks are WCWT's own defaults, used before the first sync lands.
     */
    private boolean mgmtUpload() {
        return this.monitorableContainer == null || this.monitorableContainer.mgmtUpload;
    }

    private int mgmtDisplayMode() {
        return this.monitorableContainer == null ? 1 : this.monitorableContainer.mgmtDisplayMode;
    }

    private boolean mgmtShowSlots() {
        return this.monitorableContainer == null || this.monitorableContainer.mgmtShowSlots;
    }

    private int mgmtSearchMode() {
        return this.monitorableContainer == null ? 2 : this.monitorableContainer.mgmtSearchMode;
    }

    /** Packs the switches into one number, so a change through the sync can be noticed cheaply. */
    private int mgmtSettings() {
        return (this.mgmtUpload() ? 1 : 0) | (this.mgmtShowSlots() ? 2 : 0)
            | (this.mgmtDisplayMode() << 2)
            | (this.mgmtSearchMode() << 6)
            | (this.mgmtExpanded() ? 1 << 9 : 0);
    }

    private void updateMgmtToggles() {
        if (this.mgmtDisplayToggle == null) {
            return;
        }
        final int displayMode = this.mgmtDisplayMode();
        final String displayKey = switch (displayMode) {
            case 1 -> "visible";
            case 2 -> "not_full";
            default -> "all";
        };
        this.mgmtDisplayToggle.setIcon(
            displayMode == 1 ? MGMT_ICON_DISPLAY_VISIBLE_U
                : displayMode == 2 ? MGMT_ICON_DISPLAY_NOT_FULL_U : MGMT_ICON_DISPLAY_ALL_U,
            displayMode == 0 ? MGMT_ICON_DISPLAY_ALL_V : MGMT_ICON_DISPLAY_V,
            16);
        this.mgmtDisplayToggle.setTooltip(
            StatCollector.translateToLocalFormatted(
                "wtct.gui.management.display_mode",
                StatCollector.translateToLocal("wtct.gui.management.display." + displayKey)));
        final boolean showSlots = this.mgmtShowSlots();
        this.mgmtSlotsToggle.setIcon(showSlots ? MGMT_ICON_SLOTS_ON_U : MGMT_ICON_SLOTS_OFF_U, MGMT_ICON_SLOTS_V, 16);
        this.mgmtSlotsToggle.setActive(!showSlots);
        this.mgmtSlotsToggle.setTooltip(
            StatCollector
                .translateToLocal(showSlots ? "wtct.gui.management.slots.hide" : "wtct.gui.management.slots.show"));
        final int searchMode = this.mgmtSearchMode();
        final String scopeKey = switch (searchMode) {
            case 1 -> "in";
            case 2 -> "in_out";
            default -> "out";
        };
        this.mgmtSearchModeToggle.setIcon(
            searchMode == 1 ? MGMT_ICON_SEARCH_IN_U
                : searchMode == 2 ? MGMT_ICON_SEARCH_IN_OUT_U : MGMT_ICON_SEARCH_OUT_U,
            searchMode == 0 ? MGMT_ICON_SEARCH_OUT_V : MGMT_ICON_SEARCH_V,
            12);
        this.mgmtSearchModeToggle.setTooltip(
            StatCollector.translateToLocalFormatted(
                "wtct.gui.management.search_mode",
                StatCollector.translateToLocal("wtct.gui.management.search." + scopeKey)));
        final boolean upload = this.mgmtUpload();
        this.mgmtAutoUploadToggle.setChecked(upload);
        this.mgmtAutoUploadToggle.setTooltip(
            StatCollector
                .translateToLocal(upload ? "wtct.gui.management.autoUpload.on" : "wtct.gui.management.autoUpload.off"));
        if (this.mgmtExpandToggle != null) {
            final boolean expanded = this.mgmtExpanded();
            this.mgmtExpandToggle.setChecked(expanded);
            this.mgmtExpandToggle.setTooltip(
                StatCollector
                    .translateToLocal(expanded ? "wtct.gui.management.expand.on" : "wtct.gui.management.expand.off"));
        }
    }

    /**
     * Whether the management list grows downwards under the pattern cache. Read from the container,
     * which mirrors the terminal item, so both sides agree the moment the switch is flipped; a GUI
     * with no container yet (or a host that has no such switch) reads as WCWT's layout.
     */
    private boolean mgmtExpanded() {
        return this.monitorableContainer != null && this.monitorableContainer.mgmtExpanded;
    }

    /**
     * Local Y of the management area's top. Normally WCWT's page top (texture Y 206, offset 153);
     * with the enlargement switch on the cache is shut and the header chrome rides up under the
     * encoding panel, so the list starts on the row right below that chrome and gains everything
     * down to the page's own bottom edge.
     */
    private int mgmtTop() {
        return this.mgmtExpanded()
            ? bottomStartRel() + (MGMT_CHROME_BOTTOM_TEX_Y - REF_BOTTOM_TOP) + this.mgmtChromeShift() + 1
            : bottomStartRel() + (MGMT_TEX_Y - REF_BOTTOM_TOP);
    }

    /**
     * Height of the management list. Normally WCWT's page height (72); enlarged, it runs from under
     * the lifted header chrome down to the page's own bottom edge (offset 225), so the bottom never
     * moves and the player inventory keeps its place - the cache's strip and the chrome's old band
     * both go into the list.
     */
    private int mgmtHeight() {
        final int listBottomOff = (MGMT_TEX_Y - REF_BOTTOM_TOP) + MGMT_H;
        return this.mgmtExpanded() ? listBottomOff - (this.mgmtTop() - bottomStartRel()) : MGMT_H;
    }

    /**
     * How many rows the list shows at once. The scrollbar and the drag arithmetic follow this, so the
     * knob's travel stays right in both layouts without any special-casing.
     */
    private int mgmtScrollHeight() {
        return this.mgmtHeight();
    }

    /** Local Y of WCWT's provider search box (bottom 107). */
    /**
     * How far the management header chrome (the switch row, the provider search box, the mapping
     * field and the four mapping plates) is shifted when the enlargement switch is on.
     *
     * <p>
     * The header normally sits in the 25px gap the texture leaves between the pattern cache and the
     * management page (its own bottom edge lands at screen offset 151). Enlarged, the cache is shut
     * and the header rides up to the encoding panel: it rises until its <em>top</em> edge lands on
     * the panel's bottom plus one (offset 79), and the list then starts on the row below it. The
     * 24px-tall chrome occupies 79..102 and the list gets 103..225.
     *
     * <p>
     * Derived from the edges rather than hard-coded, so it stays correct if the chrome, the encoding
     * panel or the cache moves: the rise is "the encoding panel's bottom plus one, minus the chrome's
     * own top".
     */
    private int mgmtChromeShift() {
        if (!this.mgmtExpanded()) {
            return 0;
        }
        final int encBottomOff = (ENC_PANEL_Y - REF_BOTTOM_TOP) + ENC_PANEL_H;
        // The chrome's bottom edge is an offset, and its height is counted inclusively, so its top is
        // "bottom offset minus height plus one" - without the +1 the chrome lands one row low and the
        // list's top edge rides up into it.
        final int chromeBottomOff = MGMT_CHROME_BOTTOM_TEX_Y - REF_BOTTOM_TOP;
        final int chromeTopOff = chromeBottomOff - MGMT_CHROME_H + 1;
        return encBottomOff + 1 - chromeTopOff;
    }

    private int mgmtSearchTop() {
        return bottomStartRel() + (MGMT_SEARCH_TEX_Y - REF_BOTTOM_TOP) + this.mgmtChromeShift();
    }

    private int mgmtMappingTop() {
        return bottomStartRel() + (MGMT_MAPPING_TEX_Y - REF_BOTTOM_TOP) + this.mgmtChromeShift();
    }

    /** The mapping key typed in the field (样板映射输入框). */
    private String mgmtMappingText() {
        return this.mgmtMappingField == null ? ""
            : this.mgmtMappingField.getText()
                .trim();
    }

    private boolean isOnMgmtMapping(final int mouseX, final int mouseY) {
        final int x = this.guiLeft + MGMT_MAPPING_X;
        final int y = this.guiTop + this.mgmtMappingTop();
        return mouseX >= x && mouseX < x + MGMT_MAPPING_W && mouseY >= y && mouseY < y + MGMT_MAPPING_H;
    }

    /**
     * Shows what the key in play is already bound to, so the Chinese name a mapping stands for can be
     * read and edited here - the field used to be write-only, which left "change the name of an existing
     * mapping" with nowhere to happen (the only other option was adding a second row for the same key).
     * Never runs while the box holds the caret: that text is the player's.
     */
    private void syncMappingFieldFromKey() {
        if (this.mgmtMappingField == null || this.mgmtMappingField.isFocused()) {
            return;
        }
        final String key = this.mappingKeyForAdd();
        if (key == null || key.isEmpty()) {
            return;
        }
        final String bound = PatternMappingStore.providerFor(key);
        if (bound == null || bound.isEmpty() || bound.equals(this.mgmtMappingField.getText())) {
            return;
        }
        this.mgmtMappingField.setText(bound);
    }

    /** Screen rect of one of the four mapping buttons: (row, col) with col 0 = 增加/删除. */
    private int[] mappingButtonRect(final int row, final int col) {
        final int x = this.guiLeft + (col == 0 ? MGMT_MAPBTN_ADD_X : MGMT_MAPBTN_ALT_X);
        final int y = this.guiTop + bottomStartRel()
            + ((row == 0 ? MGMT_MAPBTN_ROW1_TEX_Y : MGMT_MAPBTN_ROW2_TEX_Y) - REF_BOTTOM_TOP)
            + this.mgmtChromeShift();
        return new int[] { x, y, MGMT_MAPBTN_W, MGMT_MAPBTN_H };
    }

    /**
     * Re-anchors the management header chrome to the layout in force this frame: the five toggle
     * buttons, the two text fields and the four mapping plates all ride up when the enlargement
     * switch is on. They are real widgets created once in {@code initGui}, and the switch can be
     * flipped at any time, so their positions are re-applied every frame rather than only when the
     * switch flips - the same reason the encoding option buttons do it.
     */
    private void updateMgmtChromePositions() {
        this.mgmtDisplayToggle.yPosition = this.mgmtChromeY(MGMT_TOGGLE_TEX_Y);
        this.mgmtSlotsToggle.yPosition = this.mgmtChromeY(MGMT_TOGGLE_TEX_Y);
        this.mgmtSearchModeToggle.yPosition = this.mgmtChromeY(MGMT_TOGGLE_TEX_Y);
        this.mgmtAutoUploadToggle.yPosition = this.mgmtChromeY(MGMT_AUTOUPLOAD_TEX_Y);
        // The enlargement switch lines up with the strip's own three plates - the magnet, the trash
        // and the terminal settings cog - and takes the next slot to their right, one pitch along. Its
        // widget blits the whole 14x14 sprite rather than the cropped 12x12 the strip plates use, so
        // its own corner sits one pixel up and left of the slot to land flush; see
        // STRIP_PLATE_INSET.
        this.mgmtExpandToggle.xPosition = this.guiLeft + MGMT_TOGGLE_EXPAND_X;
        this.mgmtExpandToggle.yPosition = frameY(MGMT_TOGGLE_EXPAND_TEX_Y);
        // The two text fields bake their origin into final fields when they are built (see
        // GuiWcwtTextField), so they cannot simply be nudged: they are rebuilt whenever the shift
        // actually changes, carrying the text and the focus across. Rebuilding only on a change keeps
        // the caret and the selection intact during normal play.
        final int shift = this.mgmtChromeShift();
        if (shift != this.mgmtSeenChromeShift) {
            this.mgmtSeenChromeShift = shift;
            this.rebuildMgmtFields();
        }
    }

    /** Recreates the provider-search and mapping fields at the current chrome position. */
    private void rebuildMgmtFields() {
        final String searchValue = this.mgmtSearch == null ? "" : this.mgmtSearch.getText();
        final boolean searchFocused = this.mgmtSearch != null && this.mgmtSearch.isFocused();
        this.mgmtSearch = new GuiWcwtTextField(
            this.fontRendererObj,
            this.guiLeft + MGMT_SEARCH_X,
            this.guiTop + this.mgmtSearchTop(),
            MGMT_SEARCH_W,
            MGMT_SEARCH_H);
        this.mgmtSearch.setPlaceholder(StatCollector.translateToLocal("wtct.gui.management.search"));
        this.mgmtSearch.setText(searchValue);
        this.mgmtSearch.setFocused(searchFocused);

        final String mappingValue = this.mgmtMappingField == null ? "" : this.mgmtMappingField.getText();
        final boolean mappingFocused = this.mgmtMappingField != null && this.mgmtMappingField.isFocused();
        this.mgmtMappingField = new GuiWcwtTextField(
            this.fontRendererObj,
            this.guiLeft + MGMT_MAPPING_X,
            this.guiTop + this.mgmtMappingTop(),
            MGMT_MAPPING_W,
            MGMT_MAPPING_H);
        this.mgmtMappingField.setPlaceholder(StatCollector.translateToLocal("wtct.gui.management.mapping"));
        this.mgmtMappingField.setText(mappingValue);
        this.mgmtMappingField.setFocused(mappingFocused);
    }

    /** The four mapping plates, drawn like the row buttons but with their label centred. */
    private void drawMappingButtons(final int mouseX, final int mouseY) {
        this.mappingTooltip = null;
        final String[] labels = { StatCollector.translateToLocal("wtct.gui.management.mapping.add"),
            StatCollector.translateToLocal("wtct.gui.management.mapping.reload"),
            StatCollector.translateToLocal("wtct.gui.management.mapping.delete"),
            StatCollector.translateToLocal("wtct.gui.management.mapping.cancel") };
        final float scale = 0.75F;
        for (int i = 0; i < 4; i++) {
            final int[] r = this.mappingButtonRect(i / 2, i % 2);
            // The management area renders inside the already-translated foreground layer, so the
            // plates are positioned relatively; mappingButtonRect stays absolute for the click test.
            r[0] -= this.guiLeft;
            r[1] -= this.guiTop;
            final boolean hover = mouseX >= r[0] && mouseX < r[0] + r[2] && mouseY >= r[1] && mouseY < r[1] + r[3];
            // WCWT's renderPatternManagementTextButton: a 60x22 sprite (hover swaps in the pressed
            // art) scaled down into the 30x11 button.
            this.mc.getTextureManager()
                .bindTexture(WIDGETS_TEXTURE);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            blitScaled(r[0], r[1], 0, hover ? MAPPING_BTN_HOVER_V : MAPPING_BTN_NORMAL_V, 60, 22, r[2], r[3]);
            // ... and the label at 0.75 scale, white and centred, sinking a pixel on hover.
            final String label = labels[i];
            GL11.glPushMatrix();
            GL11.glTranslatef(
                r[0] + (r[2] - this.fontRendererObj.getStringWidth(label) * scale) / 2.0F,
                r[1] + (r[3] - this.fontRendererObj.FONT_HEIGHT * scale) / 2.0F + (hover ? 1.0F : 0.0F),
                0.0F);
            GL11.glScalef(scale, scale, 1.0F);
            this.fontRendererObj.drawString(label, 0, 0, 0xFFFFFF);
            GL11.glPopMatrix();
            if (hover) {
                // WCWT spells out what each mapping button does on hover. Recorded here, drawn at the
                // end of drawFG: a tooltip raised inside this loop is covered by the plates that are
                // drawn after it, and by whatever the foreground pass puts down afterwards.
                this.mappingTooltip = StatCollector
                    .translateToLocal("wtct.gui.management.mapping." + MAPPING_TIPS[i] + ".tooltip");
            }
        }
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** Blits a source region of the widget sheet into an arbitrary destination rectangle. */
    private void blitScaled(final int destX, final int destY, final int srcX, final int srcY, final int srcW,
        final int srcH, final int destW, final int destH) {
        final float f = 1.0F / 256.0F;
        final Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();
        tess.addVertexWithUV(destX, destY + destH, this.zLevel, srcX * f, (srcY + srcH) * f);
        tess.addVertexWithUV(destX + destW, destY + destH, this.zLevel, (srcX + srcW) * f, (srcY + srcH) * f);
        tess.addVertexWithUV(destX + destW, destY, this.zLevel, (srcX + srcW) * f, srcY * f);
        tess.addVertexWithUV(destX, destY, this.zLevel, srcX * f, srcY * f);
        tess.draw();
    }

    /**
     * The four management header switches, hit-tested and dispatched here instead of leaving it to AE2's
     * own button routing. They are drawn on the strip that sits over the terminal's bottom slice, and a
     * click there was swallowed before it ever reached the button list - which is why the scope switch
     * did nothing at all while the list rows (handled here too) worked. Returning true keeps the click
     * from being read as a slot action as well.
     */
    private boolean handleMgmtToggleClick(final int mouseX, final int mouseY) {
        if (this.onButton(this.mgmtDisplayToggle, mouseX, mouseY)) {
            this.actionPerformed(this.mgmtDisplayToggle);
            return true;
        }
        if (this.onButton(this.mgmtSlotsToggle, mouseX, mouseY)) {
            this.actionPerformed(this.mgmtSlotsToggle);
            return true;
        }
        if (this.onButton(this.mgmtSearchModeToggle, mouseX, mouseY)) {
            this.actionPerformed(this.mgmtSearchModeToggle);
            return true;
        }
        if (this.onButton(this.mgmtAutoUploadToggle, mouseX, mouseY)) {
            this.actionPerformed(this.mgmtAutoUploadToggle);
            return true;
        }
        if (this.onButton(this.mgmtExpandToggle, mouseX, mouseY)) {
            this.actionPerformed(this.mgmtExpandToggle);
            return true;
        }
        return false;
    }

    /** Whether a button's own rectangle holds the cursor. */
    private static boolean onButton(final GuiButton button, final int mouseX, final int mouseY) {
        return button != null && mouseX >= button.xPosition
            && mouseX < button.xPosition + button.width
            && mouseY >= button.yPosition
            && mouseY < button.yPosition + button.height;
    }

    /**
     * WCWT's four mapping buttons. 增加 binds the key in the mapping field to the provider named in
     * the search box, 重载 re-reads the mapping table, 删除 drops the mappings matching the field and
     * 取消 clears both fields.
     */
    private boolean handleMappingClick(final int mouseX, final int mouseY) {
        for (int i = 0; i < 4; i++) {
            final int[] r = this.mappingButtonRect(i / 2, i % 2);
            if (mouseX < r[0] || mouseX >= r[0] + r[2] || mouseY < r[1] || mouseY >= r[1] + r[3]) {
                continue;
            }
            switch (i) {
                case 0 -> this.applyMappingAdd();
                case 1 -> {
                    // Re-read the file, then let the search box resolve again.
                    PatternMappingStore.load();
                    this.mgmtRowsDirty = true;
                    this.mappingMessage(
                        EnumChatFormatting.GREEN,
                        "wtct.gui.management.mapping.reloaded",
                        PatternMappingStore.getRecipeTypeMappings()
                            .size());
                }
                case 2 -> this.applyMappingDelete();
                default -> {
                    // WCWT opens ExtendedAE Plus' mapping manager here; this port ships its own copy
                    // of that screen, so the button now leads to it.
                    if (this.mgmtSearch != null && this.mgmtMappingField != null) {
                        this.mgmtSearch.setFocused(false);
                        this.mgmtMappingField.setFocused(false);
                    }
                    this.openOnTop(
                        new com.asdflj.wtct.client.gui.GuiRecipeTypeMappingScreen(this, this.providerEntries()));
                }
            }
            return true;
        }
        return false;
    }

    /**
     * WCWT's 增加映射 (and what Enter in the mapping field runs): binds the key the encoder is working
     * from to the word typed in the mapping field, then hands that word to the provider search box -
     * the term the upload will match a provider against.
     *
     * <p>
     * Every outcome is answered in the chat line, exactly like ExtendedAE Plus answers its own manager
     * ("已添加/更新映射: %s -> %s", "请输入搜索关键字后再添加映射"): the mapping button is the one control
     * whose success used to be indistinguishable from its failure, because an empty key makes the add
     * return false and nothing else on screen moves.
     */
    private void applyMappingAdd() {
        // WCWT's direction: the provider search box carries the key, the mapping box the value (its
        // "增加映射" calls addOrUpdateRecipeTypeMapping(searchText, mappingText)). The box only ever
        // shows the localised term its key resolved to, though, so the key recorded has to be the raw
        // one the encoder worked from - otherwise the table fills up with "无序合成 → 分子装配" instead
        // of "crafting → 分子装配室".
        final String key = this.mappingKeyForAdd();
        final String value = this.mgmtMappingText();
        if (key == null || key.isEmpty()) {
            this.mappingMessage(EnumChatFormatting.RED, "wtct.gui.management.mapping.need_key");
            return;
        }
        if (value.isEmpty()) {
            this.mappingMessage(EnumChatFormatting.RED, "wtct.gui.management.mapping.need_value");
            return;
        }
        if (!PatternMappingStore.addOrUpdateAliasMapping(key, value)) {
            this.mappingMessage(EnumChatFormatting.RED, "wtct.gui.management.mapping.save_failed");
            return;
        }
        // WCWT then hands the value to the provider search box and empties the mapping field - that is
        // the proactive fill: the term the upload will use is visible.
        this.mgmtSearch.setText(value);
        this.mgmtMappingField.setText("");
        this.mgmtRowsDirty = true;
        this.mappingMessage(EnumChatFormatting.GREEN, "wtct.gui.management.mapping.added", key, value);
    }

    /**
     * WCWT's 删除映射: drops what the mapping field names, or - with that box empty - the key the search
     * box resolves from, so either box can be used to point at the entry to remove.
     */
    private void applyMappingDelete() {
        final String key = this.mappingKeyForAdd();
        final String value = this.mgmtMappingText();
        final int removed;
        if (value.isEmpty()) {
            removed = PatternMappingStore.removeRecipeTypeMapping(key) ? 1 : 0;
        } else {
            removed = PatternMappingStore.removeMappingsByCnValue(value);
        }
        this.mgmtMappingField.setText("");
        this.mgmtRowsDirty = true;
        if (removed <= 0) {
            this.mappingMessage(
                EnumChatFormatting.RED,
                "wtct.gui.management.mapping.not_found",
                value.isEmpty() ? key : value);
            return;
        }
        this.mappingMessage(
            EnumChatFormatting.GREEN,
            "wtct.gui.management.mapping.deleted",
            removed,
            value.isEmpty() ? key : value);
    }

    /**
     * WCWT's two batch switches beside the multiplier: the flag goes to the server, which sets it on
     * the encoder - so the pattern encoded next carries it - and re-writes the cached crafting patterns
     * with it. The radios read their state back from the container, and the chat line says which way
     * the switch went, since the switch's own icon is one pixel of shading.
     */
    private void sendBatchSwitch(final String action, final boolean on, final String labelKey) {
        Wtct.proxy.netHandler.sendToServer(new com.asdflj.wtct.network.CPacketTerminalBtns(action, on));
        this.mappingMessage(
            on ? EnumChatFormatting.GREEN : EnumChatFormatting.GRAY,
            on ? "wtct.gui.batch.switched_on" : "wtct.gui.batch.switched_off",
            StatCollector.translateToLocal(labelKey));
    }

    /**
     * One line of feedback for the mapping plates, printed in the chat - the terminal has no room for
     * a status line, and WCWT answers the same actions through {@code
     * displayPatternManagementMessage}.
     */
    private void mappingMessage(final EnumChatFormatting color, final String langKey, final Object... args) {
        if (this.mc == null || this.mc.thePlayer == null) {
            return;
        }
        final String body = args == null || args.length == 0 ? StatCollector.translateToLocal(langKey)
            : StatCollector.translateToLocalFormatted(langKey, args);
        this.mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText(color + body));
    }

    /** The provider search text, i.e. both the list filter and the upload match word. */
    private String mgmtSearchText() {
        return this.mgmtSearch == null ? ""
            : this.mgmtSearch.getText()
                .trim();
    }

    /**
     * The key a new mapping is recorded under: the recipe type the encoding worked from, not the
     * localised word the search box happens to show. That raw key is remembered by
     * {@link #noteTransferredRecipe} / {@link #resolveUploadSearch}, and is still the one in play while
     * the box shows what it resolved to - so it is used whenever re-resolving it reproduces the box's
     * term, and dropped the moment the player types a key of their own into the box.
     */
    private String mappingKeyForAdd() {
        final String typed = this.mgmtSearchText();
        if (this.lastUploadSearchKey.isEmpty()) {
            return typed;
        }
        if (typed.isEmpty()) {
            return this.lastUploadSearchKey;
        }
        return this.resolveKey(this.lastUploadSearchKey)
            .equals(this.resolvedMgmtSearch()) ? this.lastUploadSearchKey : typed;
    }

    private int maxMgmtScroll() {
        return Math.max(0, this.mgmtRows.size() * MGMT_ROW_H - this.mgmtHeight());
    }

    /**
     * Draws the provider list, its scrollbar and the automatic-upload toggle. Called from the
     * foreground pass with GUI-relative coordinates and screen-space mouse coordinates, which is
     * where all GUI items in this mod (and in vanilla) are rendered - the background pass runs
     * before vanilla sets up the item lighting NEI and the item batching mods rely on.
     */
    private void drawPatternManagement(final int mouseX, final int mouseY) {
        // The four switches arrive from the terminal item through the container's field sync, so a
        // change - a click here, or the values the terminal was left with - has to rebuild the list and
        // refresh the four toggle icons.
        if (this.mgmtRowsDirty || this.mgmtSeenSettings != this.mgmtSettings()) {
            this.rebuildMgmtRows();
            this.updateMgmtToggles();
            // The enlargement switch changes the list's height, so the scroll offset has to be
            // re-clamped to what the new height allows - and the cache, whose rows the list now sits
            // under, has to be re-laid-out. Both are cheap and idempotent.
            final boolean expanded = this.mgmtExpanded();
            if (expanded != this.mgmtSeenExpanded) {
                this.mgmtSeenExpanded = expanded;
                this.mgmtScroll = Math.max(0, Math.min(this.maxMgmtScroll(), this.mgmtScroll));
                this.cacheScroll = Math.max(0, Math.min(this.maxCacheScroll(), this.cacheScroll));
                this.layoutPatternCache();
            }
        }
        this.syncMappingFieldFromKey();
        final int left = MGMT_X;
        final int top = this.mgmtTop();
        // Hover tests work on the GUI-relative position; the drawing below is relative too.
        final int relMouseX = mouseX - this.guiLeft;
        final int relMouseY = mouseY - this.guiTop;
        // Everything here is our own pixel work: save and restore the whole attribute set so nothing
        // is left behind for whoever draws next (the tooltip and NEI both come later).
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        // WCWT clips the list to the panel (it scissors every slot individually); one scissor around
        // the whole list keeps rows from spilling into the player inventory below and lets a row be
        // drawn half-visible at the edges, exactly like WCWT's cropTop/cropBottom handling. The width
        // grows by the highlight button's overhang: that button hangs past the page's right edge, in
        // the strip before the scrollbar, and WCWT draws it outside its own per-slot scissors.
        this.setMgmtScissor(left, top, MGMT_W + MGMT_HIGHLIGHT_OVERHANG, this.mgmtHeight(), true);
        for (int i = 0; i < this.mgmtRows.size(); i++) {
            final int rowY = top + i * MGMT_ROW_H - this.mgmtScroll;
            if (rowY + MGMT_ROW_H <= top || rowY >= top + this.mgmtHeight()) {
                continue;
            }
            final MgmtRow row = this.mgmtRows.get(i);
            if (row.isHeader()) {
                this.drawMgmtHeader(row, left, rowY, relMouseX, relMouseY);
            } else {
                this.drawMgmtSlots(row, left, rowY, relMouseX, relMouseY);
            }
        }
        this.setMgmtScissor(0, 0, 0, 0, false);
        if (this.maxMgmtScroll() > 0) {
            drawScrollbarAt(MGMT_SCROLL_X, top, this.mgmtScrollHeight(), this.mgmtScroll, this.maxMgmtScroll());
        }
        this.drawMappingButtons(relMouseX, relMouseY);
        GL11.glPopAttrib();
        // glPopAttrib also restores the colour, but be explicit: leftover translucent tints are what
        // dims the NEI panel / the item batching that follows.
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /**
     * Clips drawing to a rectangle given in GUI-relative coordinates. The scissor box itself is in
     * real framebuffer pixels, so it needs the GUI origin and the current GUI scale.
     */
    private void setMgmtScissor(final int x, final int y, final int w, final int h, final boolean enable) {
        if (!enable) {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            return;
        }
        final ScaledResolution scaled = new ScaledResolution(this.mc, this.mc.displayWidth, this.mc.displayHeight);
        final int scale = scaled.getScaleFactor();
        final int screenHeight = this.mc.displayHeight;
        final int absX = (this.guiLeft + x) * scale;
        final int absY = (this.guiTop + y) * scale;
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(absX, screenHeight - absY - h * scale, w * scale, h * scale);
    }

    /** Provider header row: the 8px badge, the name and the in-row buttons (WCWT's row layout). */
    private void drawMgmtHeader(final MgmtRow row, final int left, final int rowY, final int mouseX, final int mouseY) {
        final PatternProvider provider = row.provider;
        if (provider.id == this.selectedProviderId) {
            // WCWT marks the selected provider by tinting its header row.
            drawRect(left, rowY, left + MGMT_W, rowY + MGMT_ROW_H, MGMT_SELECTED_TINT);
        }
        if (provider.icon != null) {
            // Full size, not WCWT's halved 8px: the row is 18px tall, so the machine's own icon gets the
            // same 16px every other item in the list gets - at half scale it was too small to tell one
            // interface's machine from another's. No amount overlay either: it is a machine standing for
            // the interface, not a stack of something.
            GL11.glPushMatrix();
            GL11.glTranslatef(left + 1, rowY + 1, MGMT_ITEM_Z);
            this.drawMgmtItem(provider.icon, 0, 0, false);
            GL11.glPopMatrix();
        }
        this.fontRendererObj
            .drawString(this.fontRendererObj.trimStringToWidth(provider.name, 88), left + 19, rowY + 5, 0x404040);
        // The upload plate carries AE2's up-arrow sprite, the open-UI plate WCWT's own glyph - the
        // same two textures WCWT blits over its button plates.
        this.drawMgmtSpriteButton(
            left + MGMT_UPLOAD_X,
            rowY + MGMT_ROWBTN_TOP,
            MGMT_ROWBTN_W,
            MGMT_ROWBTN_H,
            MGMT_AE2_STATES_TEXTURE,
            256,
            MGMT_UPLOAD_ICON_U,
            MGMT_UPLOAD_ICON_V,
            MGMT_UPLOAD_ICON_SIZE,
            MGMT_UPLOAD_ICON_SIZE,
            mouseX,
            mouseY);
        this.drawMgmtSpriteButton(
            left + MGMT_UI_X,
            rowY + MGMT_ROWBTN_TOP,
            MGMT_ROWBTN_W,
            MGMT_ROWBTN_H,
            MGMT_STATES_TEXTURE,
            256,
            MGMT_UI_ICON_U,
            MGMT_UI_ICON_V,
            MGMT_UI_ICON_W,
            MGMT_UI_ICON_H,
            mouseX,
            mouseY);
        if (this.mgmtRowHasHighlightButton(row)) {
            this.drawMgmtHighlightButton(left, rowY);
        }
    }

    /**
     * One of the two plated row buttons: WCWT's button sprite (idle at (161,0), hovered one sprite
     * right) sinking one pixel under the cursor, with the button's own icon centred on top of it.
     */
    private void drawMgmtSpriteButton(final int x, final int y, final int w, final int h, final String iconTexture,
        final int texSize, final int iconU, final int iconV, final int iconW, final int iconH, final int mouseX,
        final int mouseY) {
        final boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        final int dy = hover ? 1 : 0;
        this.blitMgmtSprite(
            MGMT_STATES_TEXTURE,
            x,
            y + dy,
            hover ? MGMT_ROWBTN_PLATE_U + 16 : MGMT_ROWBTN_PLATE_U,
            MGMT_ROWBTN_PLATE_V,
            w,
            h,
            w,
            h,
            256,
            256);
        final int iconDrawW = Math.min(iconW, w);
        final int iconDrawH = Math.min(iconH, h);
        this.blitMgmtSprite(
            iconTexture,
            x + (w - iconDrawW) / 2,
            y + (h - iconDrawH) / 2 + dy,
            iconU,
            iconV,
            iconDrawW,
            iconDrawH,
            iconW,
            iconH,
            texSize,
            texSize);
    }

    /** The little world-highlight sprite in the strip right of the page (WCWT's exact placement). */
    private void drawMgmtHighlightButton(final int left, final int rowY) {
        this.blitMgmtSprite(
            MGMT_HIGHLIGHT_TEXTURE,
            left + MGMT_HIGHLIGHT_X,
            rowY + MGMT_HIGHLIGHT_TOP,
            MGMT_HIGHLIGHT_ICON_U,
            MGMT_HIGHLIGHT_ICON_V,
            MGMT_HIGHLIGHT_W,
            MGMT_HIGHLIGHT_H,
            MGMT_HIGHLIGHT_ICON_SRC_W,
            MGMT_HIGHLIGHT_ICON_SRC_H,
            64,
            64);
    }

    /**
     * One sprite out of a GUI sheet, at its exact source size and scale. {@code drawTexturedModalRect}
     * normalises its UVs against 256, which only works for the sheets that happen to be that size -
     * this takes the sheet size as an argument so ExtendedAE's 64x64 sheet can be used as well.
     */
    private void blitMgmtSprite(final String texture, final int x, final int y, final int u, final int v, final int w,
        final int h, final int srcW, final int srcH, final int texW, final int texH) {
        this.bindTextureBack(texture);
        // The sheets are cut up, so every sprite's surround is transparent - and the group icon drawn
        // just above goes through RenderItem, which turns blending off. Without turning it back on
        // those alpha-0 texels land on the framebuffer as solid black, which is what the deposit
        // button's AE2 arrow was showing.
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
    }

    /**
     * One line of a provider's pattern slots: WCWT's 18px cells with the item drawn one pixel low.
     * WCWT crops the last column at the panel's right edge (the 9-slot grid is 162px wide inside a
     * 160px page), which is what {@code drawW} does here.
     */
    private void drawMgmtSlots(final MgmtRow row, final int left, final int rowY, final int mouseX, final int mouseY) {
        for (int col = 0; col < row.slots.length; col++) {
            final int index = row.slots[col];
            final int x = left + col * MGMT_ROW_H;
            // The texture has to be bound per slot: drawMgmtItem goes through RenderItem, which
            // leaves the item atlas bound, and drawTexturedModalRect normalises its UVs against 256 -
            // so without this, every plate after the first rendered item is cut out of the atlas's
            // top-left corner and shows whatever unrelated sprite lives there.
            this.bindTextureBack(MGMT_TEXTURE);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            drawTexturedModalRect(x, rowY, 0, 0, Math.min(MGMT_ROW_H, left + MGMT_W - x), MGMT_ROW_H);
            if (index == EMPTY_SLOTS_CELL) {
                this.drawMgmtEmptySlotsCell(row.provider, x, rowY);
                continue;
            }
            final ItemStack stack = row.provider.slots[index];
            if (stack != null) {
                this.drawMgmtItem(stack, x, rowY + 1, true);
            }
        }
    }

    /**
     * The folded cell: the empty cell art with the number of slots it stands for in its top-right
     * corner - WCWT's own badge, a 6/8-scale white number with a shadow, right-aligned so it ends on
     * the cell's 16px item box. Nothing is drawn inside the cell otherwise, which is what makes a
     * mostly empty provider read as one short row instead of a mostly blank grid.
     */
    private void drawMgmtEmptySlotsCell(final PatternProvider provider, final int x, final int rowY) {
        int hidden = 0;
        for (final ItemStack stack : provider.slots) {
            if (stack == null) {
                hidden++;
            }
        }
        if (hidden <= 0) {
            return;
        }
        final String count = Integer.toString(hidden);
        GL11.glPushMatrix();
        GL11.glTranslatef(x + MGMT_ROW_H - 2, rowY, MGMT_ITEM_Z);
        GL11.glScalef(0.75F, 0.75F, 1.0F);
        this.fontRendererObj.drawString(count, -this.fontRendererObj.getStringWidth(count), 0, 0xFFFFFF, true);
        GL11.glPopMatrix();
    }

    /**
     * What to draw for a provider slot: WCWT's {@code getPatternDisplayStack}, i.e. a pattern shows its
     * output instead of itself.
     *
     * <p>
     * Two cases keep the pattern itself: an output that ae2fc hands over as its model-less fluid
     * packet item (drawing that is what produces a missing-texture sprite - the old interface-terminal
     * code path had the same problem until it went through {@link IAEStack#drawInGui}), and a pattern
     * whose output cannot be read at all.
     */
    private static IAEStack<?> mgmtDisplayStack(final ItemStack stack) {
        if (!(stack.getItem() instanceof ItemEncodedPattern encoded)) {
            return AEItemStack.create(stack);
        }
        final IAEStack<?> output = encoded.getOutputAE(stack);
        if (output == null) {
            return AEItemStack.create(stack);
        }
        if (output instanceof IAEItemStack itemOutput && !isRenderableProduct(itemOutput.getItemStack())) {
            return AEItemStack.create(stack);
        }
        return output;
    }

    /**
     * Whether a pattern's product can be drawn at all. Two kinds cannot, and both used to show up as
     * a missing-texture square in the list:
     *
     * <ul>
     * <li>a stack that carries <em>no item at all</em> - which is what AE2 hands back for a product
     * it cannot resolve ({@code ItemEncodedPattern} builds {@code new ItemStack(Blocks.fire)}, and
     * fire has no item block, then names it "未知物品");</li>
     * <li>ae2fc's fluid packet, which has no item model of its own - the fluid has to be drawn from
     * the fluid stack instead, which this port cannot do in a slot that is also allowed to hold
     * ordinary items.</li>
     * </ul>
     *
     * Falls back to the pattern itself in both cases, so the slot always shows something that exists.
     */
    private static boolean isRenderableProduct(final ItemStack product) {
        return product != null && product.getItem() != null && !(product.getItem() instanceof ItemFluidPacket);
    }

    /**
     * Draws a slot item with the GUI item lighting AE2 uses for its own slot-like rows. Rendering an
     * item without it leaves the lighting/colour state modified for everything drawn afterwards, which
     * is what made the list and the rows below it look layered wrongly.
     */
    /**
     * Draws one item the way the list wants it.
     *
     * <p>
     * {@code overlay} adds AE2's amount / craftable overlay. The pattern cells want it - they carry the
     * amounts the encoded pattern asks for - but the provider's own icon is a machine standing for the
     * interface, and with the amount forced on ({@code showAmountAlways}) a plain machine item drew a
     * stray "1" beside it, which reads as if the interface held one of something.
     */
    private void drawMgmtItem(final ItemStack stack, final int x, final int y, final boolean overlay) {
        final IAEStack<?> display = mgmtDisplayStack(stack);
        if (display == null) {
            return;
        }
        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, MGMT_ITEM_Z);
        // No lighting calls here on purpose. This runs in the foreground pass, which vanilla already
        // wrapped in RenderHelper.enableGUIStandardItemLighting() - exactly the situation vanilla's own
        // slot items are rendered in. Re-enabling it per item (36+ times a frame) churns the GUI
        // (matrix-rotated) light positions through the state manager, and NEI's block items - drawn
        // right after this GUI - then shade with those lights instead of the world ones, which is what
        // darkened their top faces.
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        display.drawInGui(this.mc, 0, 0);
        if (overlay) {
            GL11.glTranslatef(0.0F, 0.0F, MGMT_ITEM_OVERLAY_Z);
            display.drawOverlayInGui(this.mc, 0, 0, true, true, false, false);
        }
        GL11.glPopAttrib();
        GL11.glPopMatrix();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** Slot / header tooltips: pattern name, or the provider's position. */
    private void drawMgmtTooltip(final int left, final int top, final int mouseX, final int mouseY) {
        this.hoveredMgmtPattern = null;
        final int relX = mouseX - left;
        final int relY = mouseY - top;
        // The page plus the strip the highlight button hangs into - the scrollbar starts right after.
        // The height is the layout's own (the list's), not the page's original 72: with the
        // enlargement switch on the list runs 122 rows deep, and a pointer below the old bottom edge
        // would lose every tooltip - exactly half the list going silent.
        if (relX < 0 || relX >= MGMT_W + MGMT_HIGHLIGHT_OVERHANG || relY < 0 || relY >= this.mgmtHeight()) {
            return;
        }
        final int index = (relY + this.mgmtScroll) / MGMT_ROW_H;
        if (index < 0 || index >= this.mgmtRows.size()) {
            return;
        }
        final MgmtRow row = this.mgmtRows.get(index);
        final int rowY = top + index * MGMT_ROW_H - this.mgmtScroll;
        // The three row controls answer for themselves when hovered - they are icons without labels.
        // drawFG runs inside GuiContainer's translate(guiLeft, guiTop), so the tooltip needs
        // GUI-relative coordinates here exactly like the ones below - raw screen coordinates put the
        // box a whole GUI offset away from the cursor.
        final String button = this.mgmtRowButtonAt(mouseX, mouseY, row, left, rowY);
        if (button != null) {
            this.drawHoveringText(
                java.util.Collections.singletonList(StatCollector.translateToLocal(button)),
                mouseX - this.guiLeft,
                mouseY - this.guiTop,
                this.fontRendererObj);
            return;
        }
        if (relX >= MGMT_W) {
            // Over the highlight strip but not on the button itself - nothing to say.
            return;
        }
        final List<String> lines = new ArrayList<>(2);
        if (row.isHeader()) {
            lines.add(row.provider.name);
            lines.add(
                StatCollector.translateToLocalFormatted(
                    "wtct.gui.management.location",
                    row.provider.dim,
                    row.provider.x,
                    row.provider.y,
                    row.provider.z));
            // The machine family this interface is grouped under, read out of the raw name AE2 sent:
            // it is what decides which machine the list leads with and where an upload goes, and it is
            // invisible everywhere else. "组装机" and "电路组装机" are different families even though the
            // one word contains the other, and only this line tells the two apart on sight.
            lines.add(
                StatCollector.translateToLocalFormatted(
                    "wtct.gui.management.family",
                    machineFamily(row.provider.rawName),
                    row.provider.rawName == null ? "" : row.provider.rawName));
        } else {
            final int col = (relX - (relX % MGMT_ROW_H)) / MGMT_ROW_H;
            if (col >= row.slots.length) {
                return;
            }
            final int slot = row.slots[col];
            if (slot == EMPTY_SLOTS_CELL) {
                // The folded cell says how many slots it stands for and what a click on it does - the
                // empty grid it replaces gave that away by itself.
                int hidden = 0;
                for (final ItemStack stack : row.provider.slots) {
                    if (stack == null) {
                        hidden++;
                    }
                }
                lines.add(
                    StatCollector
                        .translateToLocalFormatted("wtct.gui.management.empty_slots", Integer.toString(hidden)));
            } else if (row.provider.slots[slot] != null) {
                // The pattern's own tooltip (Result / ingredients / substitution flags), exactly like
                // hovering it in a real slot - not the one-line display name a bare getDisplayName gives.
                this.hoveredMgmtPattern = row.provider.slots[slot];
                if (NEI_PRESENT) {
                    // NEI asks for this stack through getHoveredStack() and draws the same tooltip
                    // itself, so drawing ours here would put two boxes on top of each other. Handing it
                    // over is also what makes NEI's keys (recipe / usage / pattern view) act on the
                    // pattern - the way they do on the interface terminal's slots.
                    return;
                }
                addStackTooltip(row.provider.slots[slot], lines);
            }
        }
        // drawFG runs inside GuiContainer's translate(guiLeft, guiTop), and GuiContainer's
        // drawHoveringText draws against whatever matrix is current - so the tooltip needs
        // GUI-relative coordinates here, or it lands a whole GUI offset away from the cursor.
        // (The hit test above stays in screen coordinates: that is what drawFG is handed.)
        this.drawHoveringText(lines, mouseX - this.guiLeft, mouseY - this.guiTop, this.fontRendererObj);
    }

    /**
     * The pattern under the cursor in the management list, handed to NEI so its keys and its tooltip
     * work on the hand-painted cells. Those cells are painted by this GUI and NEI cannot find them
     * through the slot under the mouse, so they come first.
     *
     * <p>
     * Everything else falls through to AE2's own answer - the ME stack under the cursor - which is
     * the whole reason NEI's recipe/usage keys work in an AE2 terminal at all. Returning only the
     * management pattern here (as this used to) hides every other stack from NEI and makes R and U
     * do nothing anywhere in the terminal.
     */
    @Override
    public ItemStack getHoveredStack() {
        return this.hoveredMgmtPattern != null ? this.hoveredMgmtPattern : super.getHoveredStack();
    }

    /**
     * Vanilla's tooltip assembly ({@code GuiContainer#renderToolTip}): first line in the item's rarity
     * colour, the rest grey. AE2's pattern tooltip is built by {@code ItemEncodedPattern}, which needs
     * a client world/player, so this only runs on the GUI side. Falls back to the display name if the
     * pattern cannot be parsed.
     */
    private static void addStackTooltip(final ItemStack stack, final List<String> lines) {
        try {
            final List<String> raw = stack.getTooltip(net.minecraft.client.Minecraft.getMinecraft().thePlayer, false);
            for (int i = 0; i < raw.size(); i++) {
                lines.add(
                    i == 0 ? stack.getRarity().rarityColor + raw.get(i)
                        : net.minecraft.util.EnumChatFormatting.GRAY + raw.get(i));
            }
        } catch (final Exception e) {
            lines.add(stack.getDisplayName());
        }
    }

    /** True while the cursor is over WCWT's provider search box. */
    private boolean isOnMgmtSearch(final int mouseX, final int mouseY) {
        final int x = this.guiLeft + MGMT_SEARCH_X;
        final int y = this.guiTop + this.mgmtSearchTop();
        return mouseX >= x && mouseX < x + MGMT_SEARCH_W && mouseY >= y && mouseY < y + MGMT_SEARCH_H;
    }

    /** True while the cursor is over the management list. */
    private boolean isInMgmtArea(final int mouseX, final int mouseY) {
        final int left = this.guiLeft + MGMT_X;
        final int top = this.guiTop + this.mgmtTop();
        // The width includes the highlight button's overhang: that button hangs past the page's right
        // edge, in the strip before the scrollbar - clamping the area at the page edge is what made
        // the button show its tooltip but swallow every click.
        return mouseX >= left && mouseX < left + MGMT_W + MGMT_HIGHLIGHT_OVERHANG
            && mouseY >= top
            && mouseY < top + this.mgmtHeight();
    }

    /**
     * The management list's wheel target: the list, the scrollbar beside it and the rest of that band
     * across to the terminal's right frame - the page's own column and nothing else.
     *
     * <p>
     * Three shapes were tried before this one, and each showed up as a different complaint. The
     * original rectangle (the page's right edge plus the highlight button's overhang, ending at {@code
     * MGMT_X + MGMT_W + 7} = 343, exactly where the scrollbar starts) left the scrollbar itself out, so
     * the one strip a player aims at to scroll did nothing. Adding the scrollbar fixed that but still
     * stopped dead at the page's edges, so a pointer that had drifted a few pixels sideways lost the
     * wheel. Stretching it across the whole window then took the wheel away from the left column,
     * which is the terminal's own item list's territory - the manual crafting area, the multiplier
     * panel and the player inventory all scroll the ME items there, and they must keep doing so.
     *
     * <p>
     * So the band is bounded by the page on the left and the frame on the right: everything inside
     * answers the list, everything left of it stays with the terminal's items. The click and drag
     * paths are untouched throughout - {@link #isOnMgmtScrollbar}, the row buttons and the inventory
     * slots are all tested separately.
     */
    private boolean isInMgmtWheelArea(final int mouseX, final int mouseY) {
        if (this.isInMgmtArea(mouseX, mouseY)) {
            return true;
        }
        final int top = this.guiTop + this.mgmtTop();
        if (mouseY < top || mouseY >= top + this.mgmtHeight()) {
            return false;
        }
        final int scrollX = this.guiLeft + MGMT_SCROLL_X;
        if (mouseX >= scrollX && mouseX < scrollX + ENC_SCROLL_W) {
            return true;
        }
        return mouseX >= this.guiLeft + MGMT_X && mouseX < this.guiLeft + GUI_WIDTH;
    }

    private boolean isOnMgmtScrollbar(final int mouseX, final int mouseY) {
        final int x = this.guiLeft + MGMT_SCROLL_X;
        final int y = this.guiTop + this.mgmtTop();
        return mouseX >= x && mouseX < x + ENC_SCROLL_W && mouseY >= y && mouseY < y + this.mgmtScrollHeight();
    }

    private void dragMgmtScrollTo(final int mouseY) {
        final int grooveTop = this.guiTop + this.mgmtTop();
        final int travel = Math.max(1, this.mgmtScrollHeight() - ENC_KNOB_H);
        final int value = (mouseY - ENC_KNOB_H / 2 - grooveTop) * this.maxMgmtScroll() / travel;
        this.mgmtScroll = Math.max(0, Math.min(this.maxMgmtScroll(), value));
    }

    /** Row hit test: the row under the cursor, or null. */
    private MgmtRow mgmtRowAt(final int mouseX, final int mouseY) {
        if (this.mgmtRowsDirty) {
            this.rebuildMgmtRows();
        }
        if (!this.isInMgmtArea(mouseX, mouseY)) {
            return null;
        }
        final int index = (mouseY - (this.guiTop + this.mgmtTop()) + this.mgmtScroll) / MGMT_ROW_H;
        return index < 0 || index >= this.mgmtRows.size() ? null : this.mgmtRows.get(index);
    }

    /** Handles a click inside the management area; true when it was consumed. */
    private boolean handleMgmtClick(final int mouseX, final int mouseY, final boolean shift) {
        final MgmtRow row = this.mgmtRowAt(mouseX, mouseY);
        if (row == null) {
            return false;
        }
        final int left = this.guiLeft + MGMT_X;
        final int rowY = this.guiTop + this.mgmtTop() + (this.mgmtRows.indexOf(row) * MGMT_ROW_H) - this.mgmtScroll;
        if (row.isHeader()) {
            if (this.isOnRowButton(
                mouseX,
                mouseY,
                left + MGMT_UPLOAD_X,
                rowY + MGMT_ROWBTN_TOP,
                MGMT_ROWBTN_W,
                MGMT_ROWBTN_H)) {
                this.sendMgmtAction("PatternManagement.UploadCache", row.provider.id);
                return true;
            }
            if (this.isOnRowButton(
                mouseX,
                mouseY,
                left + MGMT_UI_X,
                rowY + MGMT_ROWBTN_TOP,
                MGMT_ROWBTN_W,
                MGMT_ROWBTN_H)) {
                // The server right-clicks the machine this provider feeds for the player, so whatever
                // mod owns that machine opens it - WCWT's manage_open_ui.
                this.sendMgmtAction("PatternManagement.OpenProviderUi", row.provider.id);
                this.selectedProviderId = row.provider.id;
                return true;
            }
        }
        // The highlight button hangs past the last slot column, so it has to win over the slot hit
        // test - on a slot row the click would otherwise land on the ninth cell.
        if (this.mgmtRowHasHighlightButton(row) && this.isOnRowButton(
            mouseX,
            mouseY,
            left + MGMT_HIGHLIGHT_X,
            rowY + MGMT_HIGHLIGHT_TOP,
            MGMT_HIGHLIGHT_W,
            MGMT_HIGHLIGHT_H)) {
            this.highlightProvider(row.provider);
            return true;
        }
        if (row.isHeader()) {
            this.selectedProviderId = row.provider.id;
            return true;
        }
        final int relX = mouseX - left;
        if (relX >= MGMT_W) {
            // In the strip past the page's right edge but not on the highlight button - consume the
            // click instead of letting the column math land it on the ninth slot.
            return true;
        }
        final int col = Math.max(0, Math.min(MGMT_COLS - 1, (mouseX - left) / MGMT_ROW_H));
        if (col >= row.slots.length) {
            return true;
        }
        final int slot = row.slots[col];
        if (slot == EMPTY_SLOTS_CELL) {
            // The provider's empty slots are folded into this one cell, so a click here is aimed at the
            // first slot that is free - which is how a pattern carried on the cursor still goes in with
            // the empty slots collapsed. (WCWT leaves the cell inert, but then only an occupied slot can
            // be clicked and a pattern can no longer be put in by hand at all.)
            this.sendMgmtAction("PatternManagement.InsertFirstFree", row.provider.id);
            this.selectedProviderId = row.provider.id;
            return true;
        }
        final NBTTagCompound tag = new NBTTagCompound();
        tag.setLong("provider", row.provider.id);
        tag.setBoolean("quick", shift);
        Wtct.proxy.netHandler.sendToServer(new CPacketTerminalBtns("PatternManagement.SlotAction", slot, tag));
        this.selectedProviderId = row.provider.id;
        return true;
    }

    /** The lang key of the row control under the cursor, or null - the three carry icons, not labels. */
    private String mgmtRowButtonAt(final int mouseX, final int mouseY, final MgmtRow row, final int left,
        final int rowY) {
        if (row.isHeader()) {
            if (this.isOnRowButton(
                mouseX,
                mouseY,
                left + MGMT_UPLOAD_X,
                rowY + MGMT_ROWBTN_TOP,
                MGMT_ROWBTN_W,
                MGMT_ROWBTN_H)) {
                return "wtct.gui.management.upload_btn";
            }
            if (this.isOnRowButton(
                mouseX,
                mouseY,
                left + MGMT_UI_X,
                rowY + MGMT_ROWBTN_TOP,
                MGMT_ROWBTN_W,
                MGMT_ROWBTN_H)) {
                return "wtct.gui.management.open_ui";
            }
        }
        if (this.mgmtRowHasHighlightButton(row) && this.isOnRowButton(
            mouseX,
            mouseY,
            left + MGMT_HIGHLIGHT_X,
            rowY + MGMT_HIGHLIGHT_TOP,
            MGMT_HIGHLIGHT_W,
            MGMT_HIGHLIGHT_H)) {
            return "wtct.gui.management.highlight_btn";
        }
        return null;
    }

    private static boolean isOnRowButton(final int mouseX, final int mouseY, final int x, final int y, final int width,
        final int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    /**
     * Where the highlight button lives. WCWT moves it around - on the header while the slots are
     * folded away, else on the row holding the provider's first slot - which puts a 5x10 sprite in a
     * different place depending on a toggle, and that sprite is easy enough to miss as it is. Ours
     * always rides the header row, right of the open-UI plate, so the button sits in one spot no
     * matter how the slots toggle is set.
     */
    private boolean mgmtRowHasHighlightButton(final MgmtRow row) {
        return row.isHeader();
    }

    /**
     * WCWT's manage_highlight_provider, done with the interface terminal's exact highlight call:
     * AE2's {@code BlockPosHighlighter#highlightNamedBlocks} blinks a box around the block (through
     * walls, just like the interface terminal's highlight), the interface terminal's own messages
     * name where it is, and the screen closes so the box is actually visible - the interface
     * terminal closes its screen for the same reason.
     */
    private void highlightProvider(final PatternProvider provider) {
        final String name = provider.name == null ? "" : provider.name;
        final appeng.api.util.NamedDimensionalCoord coord = new appeng.api.util.NamedDimensionalCoord(
            provider.x,
            provider.y,
            provider.z,
            provider.dim,
            name);
        // The interface terminal picks the message pair by whether the machine carries a name.
        final String[] messages = name.isEmpty()
            ? new String[] { appeng.core.localization.PlayerMessages.MachineHighlighted.getUnlocalized(),
                appeng.core.localization.PlayerMessages.MachineInOtherDim.getUnlocalized() }
            : new String[] { appeng.core.localization.PlayerMessages.MachineHighlightedNamed.getUnlocalized(),
                appeng.core.localization.PlayerMessages.MachineInOtherDimNamed.getUnlocalized() };
        appeng.client.render.highlighter.BlockPosHighlighter
            .highlightNamedBlocks(this.mc.thePlayer, java.util.Collections.singletonMap(coord, messages), name);
        this.mc.displayGuiScreen(null);
    }

    /** Chat feedback for the two controls 1.7.10 cannot do properly, mirroring WCWT's messages. */
    private void reportProvider(final String key, final PatternProvider provider) {
        this.mc.thePlayer.addChatMessage(
            new net.minecraft.util.ChatComponentText(
                StatCollector.translateToLocalFormatted(key, provider.dim, provider.x, provider.y, provider.z)));
    }

    private void sendMgmtAction(final String action, final long providerId) {
        final NBTTagCompound tag = new NBTTagCompound();
        tag.setLong("provider", providerId);
        Wtct.proxy.netHandler.sendToServer(new CPacketTerminalBtns(action, 0, tag));
    }

    /**
     * One of the four management switches: sends the new value to the server, which writes it onto the
     * terminal item (so it survives closing the GUI and restarting the game) and reflects it back through
     * the container's field sync. Nothing is flipped here - the toggle shows what the terminal holds, so
     * a click that the server refuses cannot leave the icon lying.
     */
    private void mgmtSetting(final String action, final int value) {
        this.mgmtSetting(action, Integer.toString(value));
    }

    private void mgmtSetting(final String action, final String value) {
        Wtct.proxy.netHandler.sendToServer(new CPacketTerminalBtns(action, value, null));
    }

    /**
     * Encodes, and hands the result to the management area when the toggle is on.
     *
     * <p>
     * The mapping table drives where the pattern goes, the way WCWT/ExtendedAE Plus does it: the key
     * in the mapping field (or the search box when that is empty) is resolved to a provider search
     * term ({@link PatternMappingStore#resolveProviderSearchKey}). Only a term that names exactly one
     * provider uploads outright - and providers sharing one name count as one, which is what the
     * chooser groups its list by. With none, or with several names, nothing is handed to a guess: the
     * pattern stays in the edit slot, and when the terminal's "stash ambiguous uploads" switch is on
     * it is moved into the pattern cache instead, where the management area can hand it over by hand.
     * The "only a unique provider name" switch of the settings screen turns the gate off: then any
     * provider the term matches takes the pattern - the emptiest of them.
     */
    private void sendEncode() {
        // Resolve first, unconditionally: WCWT resolves and fills the provider search box on every
        // encode even when the automatic upload is switched off - the box never silently disagrees
        // with what the terminal is about to do. resolveUploadSearch() also writes the term into the
        // box, which is the proactive fill asked for on the encode key.
        final String search = this.resolveUploadSearch();
        if (!this.mgmtUpload()) {
            this.sendEncodePacket(false, -1, "");
            return;
        }
        // The target is resolved by provider *name*, so several interfaces sharing one name are one
        // answer and the pattern goes to whichever of them has the most room. Nothing is handed to a
        // guess: with a target the pattern is uploaded into it, otherwise it is left in the edit slot
        // - or, with the terminal's "stash ambiguous uploads" switch on, put into the pattern cache,
        // where the management area can hand it over by hand. Processing patterns go the same way:
        // nothing is being uploaded, so there is no recipe type that has to be on screen first, and
        // the cache is where the player picks the provider for them afterwards.
        final long target = this.resolveUploadProvider(search);
        if (target >= 0) {
            this.sendEncodePacket(true, target, search);
            return;
        }
        this.sendEncodePacket(false, -1, "");
        if (this.stashAmbiguousUpload()) {
            Wtct.proxy.netHandler
                .sendToServer(new CPacketTerminalBtns("PatternTerminal.StashToCache", 0, new NBTTagCompound()));
        }
    }

    /** "4" = encode, and upload when the flag says so; the tag carries the target and search text. */
    private void sendEncodePacket(final boolean upload, final long providerId, final String search) {
        final NBTTagCompound tag = new NBTTagCompound();
        tag.setBoolean("upload", upload);
        tag.setLong("provider", providerId);
        tag.setString("search", search);
        Wtct.proxy.netHandler.sendToServer(new CPacketTerminalBtns("PatternTerminal.Encode", "4", tag));
    }

    /**
     * WCWT's {@code resolvedPatternManagementSearchText} as a field gesture: text typed into the
     * mapping box that is a known mapping key (or a crafting-table spelling) fills the provider search
     * box with the term it resolves to, while it is being typed - the proactive fill, so the term the
     * upload will use is visible while it works instead of happening behind the player's back. Text that
     * is <em>not</em> a known key is left alone - that is the player authoring a new mapping value, which
     * the "增加映射" button needs kept as it is.
     *
     * @return true when the typed text was understood as a key, so the field's content is a key and not a
     *         value the plates should bind
     */
    private boolean autoFillProviderSearchFromMappingField() {
        if (this.mgmtMappingField == null || this.mgmtSearch == null) {
            return false;
        }
        final String typed = this.mgmtMappingField.getText()
            .trim();
        if (typed.isEmpty()) {
            return false;
        }
        final String resolved = PatternMappingStore.resolveProviderSearchKey(typed);
        if (resolved == null) {
            return false;
        }
        final String term = resolved.trim();
        if (term.isEmpty() || term.equals(typed)) {
            return false;
        }
        if (!term.equals(this.mgmtSearch.getText())) {
            this.mgmtSearch.setText(term);
            this.mgmtRowsDirty = true;
        }
        return true;
    }

    /**
     * WCWT's {@code resolvedPatternManagementSearchText}: the provider search box after the mapping
     * table has translated it. Everything that matches a provider - the list filter and the upload -
     * goes through this, so one box holds both the key the player types and the term it resolves to.
     */
    private String resolvedMgmtSearch() {
        final String raw = this.mgmtSearchText();
        if (raw.isEmpty()) {
            return "";
        }
        final String resolved = PatternMappingStore.resolveProviderSearchKey(raw);
        return resolved == null ? raw.trim() : resolved.trim();
    }

    /**
     * The term the upload matches providers against, and the place the proactive fill happens: a key
     * typed into the mapping box is resolved and pushed into the provider search box, so the box
     * always shows the word actually being used instead of the mapping working invisibly.
     */
    private String resolveUploadSearch() {
        this.lastSearchWasDefault = false;
        String key = "";
        // 1) the provider search box: a key or alias sitting there resolves to its term.
        String term = this.resolvedMgmtSearch();
        if (!term.isEmpty()) {
            key = this.mgmtSearchText();
        }
        // 2) a key typed into the mapping box.
        if (term.isEmpty()) {
            final String typed = this.mgmtMappingText();
            term = this.resolveKey(typed);
            key = term.isEmpty() ? "" : typed;
        }
        // 3) the term the last upload used - Plus' lastProviderSearchKey cache.
        if (term.isEmpty() && !this.lastProviderSearchKey.isEmpty()) {
            term = this.resolveKey(this.lastProviderSearchKey);
            key = this.lastProviderSearchKey;
            this.lastSearchWasDefault = true;
        }
        // 4) crafting patterns fall back to "crafting" (Plus' DEFAULT_CRAFTING_SEARCH_KEY); processing
        // patterns have no default because their recipe type is what supplies the key.
        if (term.isEmpty() && this.monitorableContainer != null && this.monitorableContainer.craftingMode) {
            term = this.resolveKey(DEFAULT_CRAFTING_SEARCH_KEY);
            key = term.isEmpty() ? "" : DEFAULT_CRAFTING_SEARCH_KEY;
            this.lastSearchWasDefault = !term.isEmpty();
        }
        this.lastUploadSearchKey = key;
        // Whatever came out goes into the box: pressing encode is the moment the mapping speaks, so
        // the search box has to show the word it produced instead of the mapping working invisibly.
        // WCWT writes the key it started from, and the box resolves it again on read, so the key and
        // the term can never drift apart.
        if (!term.isEmpty()) {
            this.lastProviderSearchKey = term;
            if (this.mgmtSearch != null && !key.equals(this.mgmtSearch.getText())) {
                this.mgmtSearch.setText(key);
                this.mgmtRowsDirty = true;
            }
        }
        return term;
    }

    /**
     * WCWT/ExtendedAE Plus' recipe-type hook, fed by NEI's recipe overlay: clicking a recipe over the
     * terminal records the recipe type of what is being moved into the encoding area (Plus'
     * {@code presetCraftingProviderSearchKey} for crafting recipes, {@code setLastProcessingName}
     * otherwise).
     *
     * <p>
     * The key goes into the provider search box right here, not only when the encode key is pressed,
     * so the type is visible the moment the overlay is clicked. What gets written is the localised
     * form: the mapping table's value when it knows the key, otherwise NEI's own localised recipe
     * name ({@code getRecipeName()}), otherwise the key itself - 1.7.10 has no recipe-type registry,
     * so NEI's overlay identifier is what stands in for the type. The raw key is remembered as well,
     * so the provider chooser can show it next to the term it resolved to.
     */
    public void noteTransferredRecipe(final boolean crafting, final String recipeId, final String localizedName) {
        final String key = crafting ? DEFAULT_CRAFTING_SEARCH_KEY : recipeId == null ? "" : recipeId.trim();
        if (key.isEmpty()) {
            return;
        }
        String term = this.resolveKey(key);
        if (term.equals(key) && !key.contains(":")) {
            // The table spells vanilla ids with their namespace (minecraft:smelting) while NEI reports
            // the bare id, so try the namespaced spelling before giving up on the mapping.
            final String namespaced = this.resolveKey("minecraft:" + key);
            if (!namespaced.equals("minecraft:" + key)) {
                term = namespaced;
            }
        }
        if (term.equals(key) && localizedName != null
            && !localizedName.trim()
                .isEmpty()) {
            // Unmapped: show NEI's localised recipe name rather than a raw identifier.
            term = localizedName.trim();
            // ...and record it. The search box alone was not enough: clicking an overlay filled the box
            // and left the table empty, so the type this terminal had just learned was gone with the
            // session and the manager the player opens to check it did not list the recipe at all.
            if (PatternMappingStore.addOrUpdateAliasMapping(key, term)) {
                this.mgmtRowsDirty = true;
            }
        }
        this.lastProviderSearchKey = key;
        this.lastUploadSearchKey = key;
        // The box holds the key, not the name it resolved to: the key is what the mapping table is
        // indexed by and what a further edit has to be matched against, and the resolved name is
        // already on screen next to it - in the mapping field, which {@code syncMappingFieldFromKey}
        // keeps showing what this key is bound to. Every reader resolves the box again
        // ({@code resolvedMgmtSearch}), so the provider matching is unaffected.
        if (this.mgmtSearch != null && !key.equals(this.mgmtSearch.getText())) {
            this.mgmtSearch.setText(key);
            this.mgmtRowsDirty = true;
        }
    }

    /** One key through the mapping table, blank when there is nothing to resolve. */
    private String resolveKey(final String raw) {
        if (raw == null || raw.trim()
            .isEmpty()) {
            return "";
        }
        final String resolved = PatternMappingStore.resolveProviderSearchKey(raw.trim());
        return resolved == null ? "" : resolved.trim();
    }

    /** The terminal's provider list, in the shape the chooser wants. */
    private List<GuiProviderSelectScreen.Entry> providerEntries() {
        final List<GuiProviderSelectScreen.Entry> out = new ArrayList<>();
        for (final PatternProvider provider : this.patternProviders) {
            out.add(new GuiProviderSelectScreen.Entry(provider.id, provider.name, emptySlotCount(provider)));
        }
        return out;
    }

    /**
     * The providers an upload may hand the pattern to: those with a free slot whose name or raw name
     * carries the search term, plus - always - the interfaces sitting on the machine family the recipe
     * type names.
     *
     * <p>
     * The family test is what lets one word stand for every voltage tier of a machine, and what keeps two
     * machines with similar names apart. A GT interface is named after the machine's own item, so the raw
     * name AE2 sends is that item's unlocalised name, {@code
     * gt.blockmachines.basicmachine.assembler.tier.03}: the voltage tier is a segment of its own, which is
     * why searching "组装机" reaches every tier of the assembling machine - and the trouble is that it
     * reaches the circuit assembling machine as well, whose "电路组装机" merely ends in the same three
     * characters. The family segment is not shared: {@code assembler} is not the family of {@code
     * gt.blockmachines.basicmachine.circuitassembler.tier.01}, {@code circuitassembler} is. Comparing
     * segments therefore tells the two machines apart at any tier and however many interfaces there are.
     *
     * @param family the family segment the recipe key names, or "" when no family can be read out of the
     *               key - a localised name, say - in which case the plain name search decides alone
     */
    private List<PatternProvider> uploadProviders(final String term, final String family) {
        final String needle = term.toLowerCase(java.util.Locale.ROOT);
        final List<PatternProvider> out = new ArrayList<>();
        for (final PatternProvider provider : this.patternProviders) {
            // A nameless provider (the interface-terminal list can hold one) has no name to search
            // for and no name to be grouped under, exactly as the chooser's own grouping drops them.
            if (provider.name == null || provider.name.isEmpty() || emptySlotCount(provider) <= 0) {
                continue;
            }
            if (!providerNameMatches(provider, needle)
                && (family.isEmpty() || !family.equals(machineFamily(provider.rawName)))) {
                continue;
            }
            out.add(provider);
        }
        // When a family was asked for it decides, because the term alone cannot: "组装机" also catches
        // every tier of the machines whose names merely end in it.
        if (!family.isEmpty()) {
            final List<PatternProvider> ofFamily = new ArrayList<>();
            for (final PatternProvider provider : out) {
                if (family.equals(machineFamily(provider.rawName))) {
                    ofFamily.add(provider);
                }
            }
            if (!ofFamily.isEmpty()) {
                return ofFamily;
            }
        }
        return out;
    }

    /**
     * The machine family an interface's raw name carries. GT registers its basic machines as {@code
     * gt.blockmachines.basicmachine.<family>.tier.NN}, so the family is the segment between {@code
     * basicmachine} and the {@code tier} that follows it: the same word for every voltage tier of one
     * machine, a different one for the circuit assembling machine. "" for an interface that is not on a
     * GT machine, or whose name does not follow that shape.
     */
    private static String machineFamily(final String rawName) {
        if (rawName == null || rawName.isEmpty()) {
            return "";
        }
        final String[] parts = rawName.toLowerCase(java.util.Locale.ROOT)
            .split("[^a-z0-9]+");
        for (int i = 0; i + 2 < parts.length; i++) {
            if ("basicmachine".equals(parts[i]) && "tier".equals(parts[i + 2])) {
                return parts[i + 1];
            }
        }
        return "";
    }

    /**
     * The machine-family word a recipe key ends in: "gtceu:assembler" and "gt.recipe.assembler" both give
     * "assembler", and "gt.recipe.circuitassembler" gives "circuitassembler". Everything before the last
     * "." or ":" is namespace or category, and separators inside that last part are dropped so the
     * 1.21-style "circuit_assembler" reads the same as GT5's "circuitassembler". "" when the key is not an
     * id at all - a localised name, say, which no machine family can be read out of.
     */
    private static String familyOfKey(final String key) {
        if (key == null) {
            return "";
        }
        final String text = key.trim()
            .toLowerCase(java.util.Locale.ROOT);
        if (text.isEmpty()) {
            return "";
        }
        return text.substring(Math.max(text.lastIndexOf('.'), text.lastIndexOf(':')) + 1)
            .replaceAll("[^a-z0-9]", "");
    }

    /**
     * The machine family the boxes currently stand for, or "" when nothing in them names one.
     *
     * <p>
     * Three sources are tried, because which of them holds the raw recipe key depends on what the player
     * has done: the provider search box holds it whenever the key is what was put there (clicking a
     * recipe overlay writes the key and shows the word it resolves to beside it), the mapping box holds
     * it while an id is being bound, and {@code lastUploadSearchKey} holds it after an upload - and it has
     * to be asked even when the boxes are empty, because the list is rebuilt the moment the terminal
     * opens, long before any upload has happened. Any source that is not an id-like word answers "" and is
     * skipped, so a box holding a localised name costs nothing.
     */
    private String searchFamily() {
        final String[] sources = { this.mgmtSearchText(), this.mgmtMappingText(), this.lastUploadSearchKey };
        for (final String source : sources) {
            final String family = familyOfKey(source);
            if (!family.isEmpty()) {
                return family;
            }
        }
        return "";
    }

    /**
     * How an upload groups the providers it may send to: by machine family where the machine has one, by
     * name otherwise. Grouping by family is what keeps a machine's voltage tiers from counting as so many
     * ambiguous names - ten assembling machines are ten names but one family, and a term that names the
     * family names them all.
     */
    private String providerGroup(final PatternProvider provider) {
        final String family = machineFamily(provider.rawName);
        return family.isEmpty() ? provider.name : family;
    }

    /**
     * The provider an automatic upload hands the finished pattern to, or -1 when there is none.
     *
     * <p>
     * The match is on the provider's <em>name</em>, the way the chooser groups its list: several
     * interfaces sharing one name are one answer, and the pattern goes to whichever of them has the
     * most room - the search box above them can only ever name a provider, never one of its
     * duplicates, so counting the interfaces instead used to refuse an upload the player had aimed
     * perfectly well. A machine's voltage tiers count as one answer too, grouped by machine family, so a
     * machine with ten interfaces still takes an upload rather than reading as ten ambiguous names - see
     * {@link #providerGroup}. With the terminal's "only a unique provider name" switch on (the default,
     * and how the upload behaved before the switch existed) a term naming two different providers is
     * ambiguous and nothing is uploaded; with the switch off any name the term matches takes the
     * pattern, and the emptiest provider among them wins.
     *
     * <p>
     * The candidates are the interfaces the term names plus the ones sitting on the machine family the
     * recipe key names, and that second half is what holds a machine's many voltage tiers together while
     * leaving the circuit assembling machine out - see {@link #uploadProviders} and
     * {@link #searchFamily}. The family is read out of the key in the mapping box, so {@code
     * gtceu:assembler} and GT5's {@code gt.recipe.assembler} both stand for the same machine.
     */
    private long resolveUploadProvider(final String search) {
        final String term = search == null ? "" : search.trim();
        if (term.isEmpty()) {
            return -1;
        }
        // The key the search came from is the better source of a family, and it sits in the mapping box:
        // a localised name in the search box has no family in it, the id it was resolved from does.
        final List<PatternProvider> candidates = this.uploadProviders(term, this.searchFamily());
        if (candidates.isEmpty()) {
            return -1;
        }
        // Providers are grouped the way a search box can name one of them: by machine family when the
        // machine has one - every voltage tier of the assembling machine is then one answer, which is what
        // lets a term covering ten tiers still upload instead of reading as ten ambiguous names - and by
        // name for the interfaces that have no family of their own.
        final Map<String, GuiProviderSelectScreen.Entry> byGroup = new LinkedHashMap<>();
        for (final PatternProvider provider : candidates) {
            final String group = this.providerGroup(provider);
            final GuiProviderSelectScreen.Entry entry = new GuiProviderSelectScreen.Entry(
                provider.id,
                provider.name,
                emptySlotCount(provider));
            final GuiProviderSelectScreen.Entry best = byGroup.get(group);
            if (best == null || entry.emptySlots() > best.emptySlots()) {
                byGroup.put(group, entry);
            }
        }
        GuiProviderSelectScreen.Entry best = null;
        for (final GuiProviderSelectScreen.Entry entry : byGroup.values()) {
            if (best == null || entry.emptySlots() > best.emptySlots()) {
                best = entry;
            }
        }
        if (best == null) {
            return -1;
        }
        // The gate counts families, not interfaces and not names: the tiers of one machine are one
        // provider as far as the search box is concerned.
        if (this.uniqueMatchUploadOnly() && byGroup.size() > 1) {
            return -1;
        }
        return best.id();
    }

    private long firstProviderWithRoomClient() {
        for (final PatternProvider provider : this.patternProviders) {
            if (emptySlotCount(provider) > 0) {
                return provider.id;
            }
        }
        return -1;
    }

    private boolean providerHasRoomClient(final long id) {
        for (final PatternProvider provider : this.patternProviders) {
            if (provider.id == id) {
                return emptySlotCount(provider) > 0;
            }
        }
        return false;
    }

    /** Free pattern slots reported for this provider by the interface-terminal stream. */
    private static int emptySlotCount(final PatternProvider provider) {
        int empty = 0;
        for (final ItemStack stack : provider.slots) {
            if (stack == null) {
                empty++;
            }
        }
        return empty;
    }
}
