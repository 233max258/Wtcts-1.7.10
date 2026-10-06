package com.asdflj.wtct.client.gui.widget;

import java.util.function.BooleanSupplier;

import net.minecraft.client.Minecraft;
import net.minecraft.inventory.Slot;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.asdflj.wtct.Wtct;
import com.asdflj.wtct.inventory.WcwtUpgradesInventory;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The comprehensive work terminal's right-hand upgrade panel - WCWT's {@code scrollingUpgrades}, i.e.
 * AE2WTLib's {@code ScrollingUpgradesPanel} with its own {@code icons.png} art.
 *
 * <p>
 * Geometry is WTLib's, constant for constant, taken from {@code ScrollingUpgradesPanel}:
 * {@code SLOT_SIZE} 18, {@code PADDING} 5, {@code SCROLLBAR_WIDTH} 5. Panel order is WCWT's - the
 * quantum singularity slot first, then the terminal's cards (the container keeps the opposite data
 * order so saved terminals keep their card where it was, see {@link WcwtUpgradesInventory}).
 *
 * <p>
 * The number of rows on screen is WTLib's {@code Math.max(2, getVisibleRows())} - two rows of the
 * column are the floor, and a taller terminal shows more of it, scrolling for the rest. With the
 * terminal's smallest layout that is exactly WCWT's default look: the singularity slot plus two
 * card slots.
 *
 * <p>
 * Art: {@code guis/wcwt/wtlib_icons.png}, byte-for-byte AE2WTLib's {@code guis/icons.png}, so the
 * sprite rectangles below are that sheet's own coordinates. Three slices make the column - a top
 * slice carrying the panel's top border and the first cell, a middle slice per further cell and a
 * bottom slice carrying the last cell plus the panel's closing border - and WTLib's blit offsets are
 * reproduced verbatim. They are what puts each slice's 18x18 cell art exactly around the slot's
 * 16x16 item cell, which is where this port's slots are placed.
 */
@SideOnly(Side.CLIENT)
public final class GuiWcwtUpgradePanel {

    /** WTLib ScrollingUpgradesPanel: SLOT_SIZE / PADDING / SCROLLBAR_WIDTH. */
    public static final int SLOT_SIZE = 18;
    public static final int PADDING = 5;
    private static final int SCROLLBAR_WIDTH = 5;

    private static final ResourceLocation TEXTURE = new ResourceLocation(
        Wtct.MODID,
        "textures/guis/wcwt/wtlib_icons.png");

    /** Fixed-width slices (no scrollbar), Icon.UPGRADE_BACKGROUND_{TOP,MIDDLE,BOTTOM}. */
    private static final int FIXED_U = 77, FIXED_TOP_V = 62, TOP_H = 23;
    private static final int FIXED_MID_V = 85, MID_H = 18;
    private static final int FIXED_BOT_V = 103, BOT_H = 25;
    private static final int FIXED_W = 23;
    /** Scrolling slices, Icon.UPGRADE_BACKGROUND_SCROLLING_{TOP,MIDDLE,BOTTOM}: same v, 6px wider. */
    private static final int SCROLL_U = 48, SCROLL_W = 29;

    /** Where the scrollbar handle rides, WCWT's {@code upgradeScrollbar} (right -17, top 6). */
    private static final int SCROLLBAR_DX = 19;
    private static final int SCROLLBAR_DY = 6;

    /** Panel origin, guiTop-relative (WCWT's {@code right 2, top 0}). */
    private final int x;
    private final int y;
    /** The quantum singularity slot, or null when the host has no upgrade inventory. */
    private final Slot singularitySlot;
    private final Slot[] cardSlots;
    /**
     * Whether the singularity slot is allowed to show while it is empty. WCWT only shows it once a
     * quantum bridge card is installed; the terminal answers that from its own card column.
     */
    private final BooleanSupplier singularityAllowed;
    /** The answer {@link #singularityAllowed} gave at the last layout, to notice a change. */
    private boolean showedSingularity;

    /** Rows of the column on screen at once - WTLib's {@code maxRows}. */
    private int maxRows = 2;
    /** First panel slot on screen - WTLib's scrollbar value. */
    private int scroll;

    public GuiWcwtUpgradePanel(final Slot singularitySlot, final Slot[] cardSlots, final int x, final int y,
        final BooleanSupplier singularityAllowed) {
        this.singularitySlot = singularitySlot;
        this.x = x;
        this.y = y;
        this.singularityAllowed = singularityAllowed == null ? () -> true : singularityAllowed;
        // Only the slots the container really created take part in the column: a host without an
        // upgrade inventory leaves the array empty, and the panel must then be empty too.
        if (cardSlots == null) {
            this.cardSlots = new Slot[0];
        } else {
            int count = 0;
            for (final Slot s : cardSlots) {
                if (s != null) {
                    count++;
                }
            }
            this.cardSlots = new Slot[count];
            int i = 0;
            for (final Slot s : cardSlots) {
                if (s != null) {
                    this.cardSlots[i++] = s;
                }
            }
        }
        this.showedSingularity = this.singularityVisible();
    }

    /** WTLib's {@code setMaxRows}: WCWT calls it with {@code Math.max(2, getVisibleRows())}. */
    public void setMaxRows(final int rows) {
        final int clamped = Math.max(2, rows);
        if (clamped == this.maxRows) {
            return;
        }
        this.maxRows = clamped;
        this.layout();
    }

    // ---------------------------------------------------------------------------------------------
    // The panel's slot list, in WCWT's display order: the singularity first, then the cards.
    // ---------------------------------------------------------------------------------------------

    /**
     * WCWT/WTLib hide the singularity slot while it is empty and no quantum bridge card is
     * installed ({@code ScrollingUpgradesPanel#singularitySlotHidden}). A singularity that is
     * already in the slot keeps the slot on screen, so fitting one and then pulling the bridge card
     * cannot swallow it.
     */
    private boolean singularityVisible() {
        if (this.singularitySlot == null) {
            return false;
        }
        if (this.singularitySlot.getStack() != null) {
            return true;
        }
        return this.singularityAllowed.getAsBoolean();
    }

    /**
     * Re-checks whether the singularity slot is allowed; the terminal's card column can change while
     * the screen is open. Called once per frame by the GUI before the panel is drawn.
     */
    public void refresh() {
        final boolean visible = this.singularityVisible();
        if (visible != this.showedSingularity) {
            this.showedSingularity = visible;
            this.layout();
        }
    }

    /** Number of slots the panel lists - WTLib's {@code getUpgradeSlotCount()}. */
    public int panelSlotCount() {
        return (this.singularityVisible() ? 1 : 0) + this.cardSlots.length;
    }

    /** WTLib's {@code getVisibleSlotCount()}: {@code min(maxRows, slotCount)}. */
    public int visibleSlotCount() {
        return Math.min(this.maxRows, this.panelSlotCount());
    }

    /** WTLib's {@code scrolling()}: the panel pages when it holds more slots than fit. */
    public boolean scrolling() {
        return this.panelSlotCount() > this.maxRows;
    }

    /** WTLib's scrollbar range: {@code 0 .. slotCount - visibleSlotCount}. */
    public int maxScroll() {
        return Math.max(0, this.panelSlotCount() - this.visibleSlotCount());
    }

    public int getScroll() {
        return this.scroll;
    }

    /** Slot at panel index {@code i}, or null for an index past the end. */
    private Slot panelSlot(final int i) {
        final int cardIndex = i - (this.singularityVisible() ? 1 : 0);
        if (cardIndex < 0) {
            return this.singularitySlot;
        }
        return cardIndex < this.cardSlots.length ? this.cardSlots[cardIndex] : null;
    }

    /**
     * WTLib's {@code updateBeforeRender}: the slots in view are put on the panel's column, the rest
     * are parked far off-screen - 1.7.10 has no optional-slot mechanism that would hide them, and a
     * parked slot is neither drawn nor hit-tested.
     *
     * <p>
     * Everything is parked <em>first</em>, then the visible window is placed. Parking first matters:
     * a slot that drops out of the list (the singularity slot going hidden, say) must not keep the
     * position an earlier layout - or {@code repositionFixedSlots}, which sweeps every AppEngSlot in
     * the container on init - left it at, or it would keep rendering its placeholder icon over
     * whichever cell it landed on.
     */
    public void layout() {
        this.scroll = Math.max(0, Math.min(this.maxScroll(), this.scroll));
        final int visible = this.visibleSlotCount();

        if (this.singularitySlot != null) {
            this.singularitySlot.xDisplayPosition = HIDDEN_SLOT;
            this.singularitySlot.yDisplayPosition = HIDDEN_SLOT;
        }
        for (final Slot slot : this.cardSlots) {
            slot.xDisplayPosition = HIDDEN_SLOT;
            slot.yDisplayPosition = HIDDEN_SLOT;
        }

        for (int row = 0; row < visible; row++) {
            final Slot slot = this.panelSlot(this.scroll + row);
            if (slot == null) {
                continue;
            }
            // WTLib's item cell position, from the panel's own origin: PADDING + 1 across and one
            // SLOT_SIZE per row down. The terminal's constants are the same numbers, but this panel
            // is also used by the card screen, which anchors it somewhere else entirely - absolute
            // coordinates would put the seats outside that window (vanilla then reads a click on one
            // as a click outside the window and drops what the cursor holds onto the ground).
            slot.xDisplayPosition = this.x + 1;
            slot.yDisplayPosition = this.y + PADDING + 1 + row * SLOT_SIZE;
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Drawing
    // ---------------------------------------------------------------------------------------------

    /**
     * Draws the panel's background art. Called from the GUI's background layer, so it lands under
     * the slots and their items.
     */
    public void draw(final Minecraft mc, final int guiX, final int guiY) {
        final int count = this.visibleSlotCount();
        if (count <= 0) {
            return;
        }
        final boolean scroll = this.scrolling();
        final int u = scroll ? SCROLL_U : FIXED_U;
        final int w = scroll ? SCROLL_W : FIXED_W;
        final int left = guiX + this.x;
        final int top = guiY + this.y;

        mc.getTextureManager()
            .bindTexture(TEXTURE);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        // Top slice: its cell art sits PADDING rows below its own top, which is what puts the first
        // cell around the first slot (the slot's item cell is at PADDING + 1).
        blit(left, top, u, FIXED_TOP_V, w, TOP_H);
        // One middle slice per further cell; its cell art starts on its first row.
        for (int i = 1; i < count - 1; i++) {
            blit(left, top + PADDING + SLOT_SIZE * i, u, FIXED_MID_V, w, MID_H);
        }
        // Bottom slice: the last cell plus the panel's closing border.
        if (count >= 2) {
            blit(left, top + PADDING + SLOT_SIZE * (count - 1), u, FIXED_BOT_V, w, BOT_H);
        }
    }

    /** The rect the scrollbar handle rides in, guiTop-relative panel coordinates. */
    public int scrollbarX() {
        return this.x + SCROLLBAR_DX;
    }

    public int scrollbarY() {
        return this.y + SCROLLBAR_DY;
    }

    /** WTLib: {@code scrollbar.setHeight(getVisibleSlotCount() * SLOT_SIZE - 2)}. */
    public int scrollbarHeight() {
        return this.visibleSlotCount() * SLOT_SIZE - 2;
    }

    // ---------------------------------------------------------------------------------------------
    // Hit testing and scrolling
    // ---------------------------------------------------------------------------------------------

    /** Panel extent, in guiLeft/guiTop-relative coordinates (what is drawn, not WTLib's bounds). */
    public int boundsWidth() {
        return this.scrolling() ? SCROLL_W : FIXED_W;
    }

    public int boundsHeight() {
        final int count = this.visibleSlotCount();
        return count <= 0 ? 0 : PADDING + SLOT_SIZE * (count - 1) + BOT_H;
    }

    /**
     * True while the cursor is over the panel. Takes absolute screen coordinates plus the GUI
     * origin, the way every other hit test in this screen does it.
     */
    public boolean contains(final int mouseX, final int mouseY, final int guiLeft, final int guiTop) {
        final int left = guiLeft + this.x;
        final int top = guiTop + this.y;
        return mouseX >= left && mouseX < left + this.boundsWidth()
            && mouseY >= top
            && mouseY < top + this.boundsHeight();
    }

    /** True while the cursor is over the handle's groove. */
    public boolean isOnScrollbar(final int mouseX, final int mouseY, final int guiLeft, final int guiTop) {
        if (this.maxScroll() <= 0) {
            return false;
        }
        final int left = guiLeft + this.scrollbarX();
        final int top = guiTop + this.scrollbarY();
        return mouseX >= left && mouseX < left + SCROLLBAR_WIDTH
            && mouseY >= top
            && mouseY < top + this.scrollbarHeight();
    }

    /** Scrolls by whole slots: wheel up (positive) moves towards the first slot. */
    public void scrollBy(final int slots) {
        final int next = Math.max(0, Math.min(this.maxScroll(), this.scroll + slots));
        if (next != this.scroll) {
            this.scroll = next;
            this.layout();
        }
    }

    /** Maps the cursor onto a scroll offset, so the handle follows it while it is dragged. */
    public void dragTo(final int mouseY, final int guiTop, final int knobHeight) {
        final int grooveTop = guiTop + this.scrollbarY();
        final int travel = Math.max(1, this.scrollbarHeight() - knobHeight);
        final int value = (mouseY - knobHeight / 2 - grooveTop) * this.maxScroll() / travel;
        final int next = Math.max(0, Math.min(this.maxScroll(), value));
        if (next != this.scroll) {
            this.scroll = next;
            this.layout();
        }
    }

    /**
     * drawTexturedModalRect with 128-based UV normalisation: 1.7.10's own helper always divides by
     * 256, which would sample the wrong quarter of this sheet.
     */
    private static void blit(final int x, final int y, final int srcX, final int srcY, final int w, final int h) {
        final float f = 1.0F / 128.0F;
        final net.minecraft.client.renderer.Tessellator tess = net.minecraft.client.renderer.Tessellator.instance;
        tess.startDrawingQuads();
        tess.addVertexWithUV(x, y + h, 0.0F, srcX * f, (srcY + h) * f);
        tess.addVertexWithUV(x + w, y + h, 0.0F, (srcX + w) * f, (srcY + h) * f);
        tess.addVertexWithUV(x + w, y, 0.0F, (srcX + w) * f, srcY * f);
        tess.addVertexWithUV(x, y, 0.0F, srcX * f, srcY * f);
        tess.draw();
    }

    /** Where the not-in-view slots are parked; the same off-screen value the GUI's slots use. */
    public static final int HIDDEN_SLOT = -9999;
}
