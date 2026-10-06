package com.asdflj.wtct.client.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import com.asdflj.wtct.Wtct;
import com.asdflj.wtct.client.gui.container.ContainerExIOBus;
import com.asdflj.wtct.client.gui.widget.GuiWcwtToolbarButton;
import com.asdflj.wtct.common.parts.ExBusScreenHost;
import com.asdflj.wtct.common.parts.ExBusSlots;

import appeng.api.config.SchedulingMode;
import appeng.api.config.Settings;
import appeng.api.config.Upgrades;
import appeng.api.storage.data.IAEStackType;
import appeng.client.gui.implementations.GuiUpgradeable;
import appeng.client.gui.slots.VirtualMEPhantomSlot;
import appeng.client.gui.widgets.GuiImgButton;
import appeng.container.implementations.ContainerUpgradeable;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketConfigButton;
import appeng.parts.automation.PartBaseExportBus;
import appeng.tile.inventory.IAEStackInventory;
import appeng.util.item.AEFluidStackType;

/**
 * The screen shared by both ME extended buses.
 *
 * <p>
 * AE2's own bus screen cannot be extended for this: its filter grid is built by a private method and its slots are not
 * reachable from a subclass, so the screen is written out here against the same background art. The art is AE2 1.21's
 * io bus panel - the screen ExtendedAE's buses reuse - which bakes the first two config rows and the player inventory,
 * so the remaining rows are stamped from the art's own slot tile and the eight upgrade slots hang off the panel's
 * right edge just like they do on 1.21.
 */
public class GuiExIOBus extends GuiUpgradeable {

    /** AE2 1.21's io bus background, copied verbatim into this mod's assets. */
    private static final ResourceLocation BACKGROUND = new ResourceLocation(Wtct.MODID, "textures/guis/storagebus.png");
    /** Source of the 16x16 cell art; the art carries a 2px gap after each cell of the 18px pitch. */
    private static final int SLOT_TILE_X = ExBusSlots.SLOT_X0;
    private static final int SLOT_TILE_Y = ExBusSlots.SLOT_Y0;
    private static final int SLOT_TILE_SIZE = 16;
    /** The grid area inside the art's white frame: the last cell of a row/column has no trailing gap. */
    private static final int GRID_W = ExBusSlots.COLUMNS * ExBusSlots.SLOT_SIZE - 2;
    private static final int GRID_H = ExBusSlots.MAX_ROWS * ExBusSlots.SLOT_SIZE - 2;
    /** The art's blank face under its two baked rows: it starts at y=63 and stops at the white frame line. */
    private static final int BLANK_SRC_Y = ExBusSlots.SLOT_Y0 + ExBusSlots.BASE_ROWS * ExBusSlots.SLOT_SIZE - 2;
    private static final int BLANK_H = ExBusSlots.SLOT_Y0 + GRID_H - BLANK_SRC_Y;
    /** 1.21's upgrade panel, tiled from extra_panels.png into the art's spare right half. */
    private static final int PANEL_SRC_X = 224;
    private static final int PANEL_SRC_Y = 0;
    private static final int PANEL_SRC_W = 28;
    private static final int PANEL_SRC_H = 156;
    /** The panel overlaps the art's right border by 2px (1.21's {@code right: 2} anchor); slots at +1/+6. */
    private static final int PANEL_X = ExBusSlots.UPGRADE_PANEL_X;
    private static final int PANEL_Y = ExBusSlots.UPGRADE_PANEL_Y;
    /** 1.21's vertical toolbar: anchor (3, 1), MARGIN 2, 16px buttons - the button box lands at -15. */
    private static final int TOOLBAR_BUTTON_X = -15;
    private static final int TOOLBAR_FIRST_Y = 3;
    /**
     * 1.21's own pitch: the 16px button plus {@code VERTICAL_SPACING} = 6. The 2px slot between two 20px
     * plates shows the wrapping plate behind them, which is what joins the column into one piece - pressing
     * the plates to a 20px pitch instead stacks their outlines into a doubled dark seam, which the user
     * rejected against the 1.21 reference.
     */
    private static final int TOOLBAR_PITCH = GuiWcwtToolbarButton.ROW_PITCH;

    /** AE2 1.21's {@code vertical_buttons_bg}, verbatim: 21x26, nine-slice border top 2 / bottom 4 / sides 0. */
    private static final ResourceLocation TOOLBAR_BG = new ResourceLocation(
        Wtct.MODID,
        "textures/guis/wcwt/vertical_buttons_bg.png");
    private static final int TOOLBAR_BG_W = 21;
    private static final int TOOLBAR_BG_H = 26;
    /**
     * {@code VerticalButtonBar.drawBackgroundLayer} blits the plate at (bar bounds x - 2, y - 1); with the bar
     * bounds resolving to (-17, 1) that puts the plate at guiLeft - 19, guiTop + 0.
     */
    private static final int TOOLBAR_BG_DX = -19;

    private final ExBusScreenHost host;
    private final VirtualMEPhantomSlot[] virtualSlots = new VirtualMEPhantomSlot[ExBusSlots.MAX_SLOTS];
    private GuiImgButton schedulingMode;
    /** How many toolbar buttons the last reflow left visible; zero hides the wrapping plate. */
    private int toolbarButtons;

    public GuiExIOBus(final InventoryPlayer inventoryPlayer, final ExBusScreenHost host) {
        this(inventoryPlayer, host, new ContainerExIOBus(inventoryPlayer, host));
    }

    /**
     * The extended storage bus hangs its own container between the player and this screen; the shared
     * machinery - panel art, phantom grid, toolbar, upgrade strip - is set up here exactly as the buses'
     * two-argument path does.
     */
    protected GuiExIOBus(final InventoryPlayer inventoryPlayer, final ExBusScreenHost host,
        final ContainerUpgradeable container) {
        super(container);
        this.host = host;
        this.ySize = ExBusSlots.HEIGHT;
    }

    @Override
    public void initGui() {
        // GuiUpgradeable hardcodes the vanilla bus height (184) in its constructor, but this screen draws
        // the 253-high 1.21 art and ContainerExIOBus binds the player inventory at height - 82 = 169 - with
        // ySize still at 184 the whole lower half lands near the window edge, below where the panel visibly
        // ends. GuiContainer centres the screen from xSize/ySize inside the super call, so the real art
        // size has to be in place before it runs (same pattern GuiCardConfig uses).
        this.xSize = ExBusSlots.BG_WIDTH;
        this.ySize = ExBusSlots.HEIGHT;
        super.initGui();
        this.initVirtualSlots();
    }

    /**
     * The upgrade panel hangs off the art's right edge - xSize covers only the 176-wide body - and vanilla
     * {@code GuiContainer#mouseClicked} reads any click outside {@code [guiLeft, guiLeft + xSize)} as a click on
     * the window frame, forcing click type 4, the throw click the drop key uses. Clicking the right part of an
     * upgrade slot therefore threw the card on the ground instead of picking it up. The click rectangle is
     * widened over the panel for the duration of the click; the drawing size is restored right after.
     */
    @Override
    protected void mouseClicked(final int mouseX, final int mouseY, final int mouseButton) {
        final int width = this.xSize;
        this.xSize = PANEL_X + PANEL_SRC_W;
        try {
            super.mouseClicked(mouseX, mouseY, mouseButton);
        } finally {
            this.xSize = width;
        }
    }

    @Override
    protected void addButtons() {
        super.addButtons();

        // 1.21 toolbar plates for the setting buttons; positions and the hide-until-carded
        // behaviour stay exactly as the AE2 base class set them up.
        this.redstoneMode = this.asToolbarButton(this.redstoneMode);
        this.fuzzyMode = this.asToolbarButton(this.fuzzyMode);
        this.craftMode = this.asToolbarButton(this.craftMode);
        this.oreFilter = this.asToolbarButton(this.oreFilter);

        if (this.host instanceof PartBaseExportBus<?>) {
            this.schedulingMode = new GuiWcwtToolbarButton(
                this.guiLeft + TOOLBAR_BUTTON_X,
                this.guiTop + TOOLBAR_FIRST_Y,
                Settings.SCHEDULING_MODE,
                SchedulingMode.DEFAULT);
            this.buttonList.add(this.schedulingMode);
        }

        initCustomButtons(this.guiLeft - 18, 88);
    }

    /**
     * Swaps one of the base class's buttons for a 1.21-styled twin at the same spot; the base class
     * keeps working off its own field references since the twin inherits everything it uses.
     */
    private GuiWcwtToolbarButton asToolbarButton(final GuiImgButton legacy) {
        this.buttonList.remove(legacy);
        final GuiWcwtToolbarButton replaced = new GuiWcwtToolbarButton(
            legacy.xPos(),
            legacy.yPos(),
            legacy.getSetting(),
            legacy.getCurrentValue());
        this.buttonList.add(replaced);
        return replaced;
    }

    @Override
    protected void actionPerformed(final GuiButton btn) {
        super.actionPerformed(btn);

        if (btn == this.schedulingMode) {
            NetworkHandler.instance
                .sendToServer(new PacketConfigButton(this.schedulingMode.getSetting(), Mouse.isButtonDown(1)));
            return;
        }

        actionPerformedCustomButtons(btn);
    }

    @Override
    public void drawFG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        super.drawFG(offsetX, offsetY, mouseX, mouseY);

        if (this.schedulingMode != null) {
            this.schedulingMode.set(this.cvb.getSchedulingMode());
        }
    }

    @Override
    public void drawBG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        this.handleButtonVisibility();
        this.mc.getTextureManager()
            .bindTexture(BACKGROUND);

        // The panel in one piece; the first two config rows and the player inventory are baked in.
        this.drawTexturedModalRect(offsetX, offsetY, 0, 0, ExBusSlots.BG_WIDTH, ExBusSlots.BG_HEIGHT);

        this.drawToolbarBackground(offsetX, offsetY);
        this.drawConfigGrid(offsetX, offsetY);
        this.drawUpgradeStrip(offsetX, offsetY);
    }

    @Override
    protected void handleButtonVisibility() {
        super.handleButtonVisibility();

        final int capacity = this.bc.getInstalledUpgrades(Upgrades.CAPACITY);
        final boolean hasOreFilter = this.bc.getInstalledUpgrades(Upgrades.ORE_FILTER) > 0;
        final int activeSlots = hasOreFilter ? 0 : ExBusSlots.slots(capacity);

        for (int i = 0; i < ExBusSlots.MAX_SLOTS; i++) {
            this.virtualSlots[i].setHidden(i >= activeSlots);
        }

        this.reflowToolbar();
    }

    /**
     * AE2 1.21's left toolbar, minus the guide button the base screen puts on top of it. The bar anchors at
     * {@code verticalToolbar} = (left 3, top 1) and lays its 16x16 buttons out with a 2px margin and 6px of
     * spacing, so a button's box sits at {@code guiLeft - 15} - the 18x20 plate around it pokes out over the
     * panel's left edge - and the stack compacts every frame as cards come and go, exactly what
     * {@code VerticalButtonBar.updateBeforeRender} does. {@link #drawToolbarBackground} wraps the column in
     * 1.21's {@code vertical_buttons_bg}.
     */
    private void reflowToolbar() {
        int nextY = TOOLBAR_FIRST_Y;
        int visible = 0;
        for (final GuiImgButton button : this.toolbarButtons()) {
            if (button == null || !button.visible) {
                continue;
            }
            button.xPosition = this.guiLeft + TOOLBAR_BUTTON_X;
            button.yPosition = this.guiTop + nextY;
            nextY += TOOLBAR_PITCH;
            visible++;
        }
        this.toolbarButtons = visible;
    }

    /**
     * The buttons the sidebar compacts, in the order they stack. The buses leave the base class's four in
     * place; the storage bus sweeps its own set instead.
     */
    protected GuiImgButton[] toolbarButtons() {
        return new GuiImgButton[] { this.redstoneMode, this.fuzzyMode, this.craftMode, this.oreFilter,
            this.schedulingMode };
    }

    /**
     * 1.21's {@code VerticalButtonBar.drawBackgroundLayer}: a {@code vertical_buttons_bg} plate behind the whole
     * column, blitted at (bounds.x - 2, bounds.y - 1) and sized (bounds.w + 1, bounds.h + 4). With the anchor at
     * (3, 1), MARGIN 2 and 16px buttons the bar bounds resolve to (-17, 1, 20, 2 + pitch * buttons), so the
     * plate lands at (guiLeft - 19, guiTop) and is 21 wide and 6 + pitch * buttons tall - which leaves the
     * sprite's 4px bottom band (white rule, shadow, dark edge) fully below the last plate plus one middle row,
     * the 5px foot the 1.21 reference shows. The sprite is nine-sliced with borders top 2 / bottom 4 /
     * left+right 0: two 1:1 bands around a middle band stretched to whatever the column height is.
     */
    private void drawToolbarBackground(final int offsetX, final int offsetY) {
        if (this.toolbarButtons == 0) {
            return;
        }
        final int height = 6 + TOOLBAR_PITCH * this.toolbarButtons;
        final int x = offsetX + TOOLBAR_BG_DX;
        final int y = offsetY;
        this.mc.getTextureManager()
            .bindTexture(TOOLBAR_BG);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.drawNativeBlit(x, y, 0, 0, TOOLBAR_BG_W, 2);
        this.drawStretchedBlit(x, y + 2, 0, 2, TOOLBAR_BG_W, TOOLBAR_BG_H - 6, height - 6);
        this.drawNativeBlit(x, y + height - 4, 0, 22, TOOLBAR_BG_W, 4);
        this.mc.getTextureManager()
            .bindTexture(BACKGROUND);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** A 1:1 blit from a texture whose native size is not 256x256. */
    private void drawNativeBlit(final int x, final int y, final int u, final int v, final int w, final int h) {
        this.drawStretchedBlit(x, y, u, v, w, h, h);
    }

    /** Maps {@code srcH} texture rows onto {@code destH} screen rows - the nine-slice middle band. */
    private void drawStretchedBlit(final int x, final int y, final int u, final int v, final int w, final int srcH,
        final int destH) {
        final float au = 1f / TOOLBAR_BG_W;
        final float av = 1f / TOOLBAR_BG_H;
        final Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(x, y + destH, this.zLevel, u * au, (v + srcH) * av);
        tessellator.addVertexWithUV(x + w, y + destH, this.zLevel, (u + w) * au, (v + srcH) * av);
        tessellator.addVertexWithUV(x + w, y, this.zLevel, (u + w) * au, v * av);
        tessellator.addVertexWithUV(x, y, this.zLevel, u * au, v * av);
        tessellator.draw();
    }

    @Override
    protected String getBackground() {
        return "guis/bus.png";
    }

    @Override
    protected String getName() {
        return this.host.getBusDisplayName();
    }

    /**
     * The grid is repainted cell by cell instead of being left to the art. The art bakes only its two base rows,
     * and a bus with no live row - an ore filter switches AE2's whole grid off - has to read as one uniform block
     * of cells, which two baked rows at full strength above faded ones cannot. So the art's blank face is stamped
     * over the whole grid first to take the baked rows out, and every row is then drawn from the art's own cell:
     * live rows at full strength, the rest at 40% - which is how AEBaseGui renders a disabled
     * {@code OptionalSlotFake} seat (its cell tile under {@code glColor(1, 1, 1, 0.4)} with blending on), and what
     * 1.21 shows too. Only the 16x16 cell is copied: the two pixels the art keeps after each cell are its own gap,
     * and copying those as well painted over the white frame lines at the grid's right and bottom edge.
     *
     * <p>
     * The face is stamped in two goes because the art only carries 90 of the grid's 124 pixels of it - it stops at
     * the white frame line, and the band below that belongs to the player inventory area. Blitting the whole grid
     * from the face in one go pulled that band, the frame line and the inventory's top cell row into the grid,
     * which is what moved the frame up and left a stray row of cells hanging under it.
     */
    private void drawConfigGrid(final int offsetX, final int offsetY) {
        this.drawTexturedModalRect(
            offsetX + ExBusSlots.SLOT_X0,
            offsetY + ExBusSlots.SLOT_Y0,
            ExBusSlots.SLOT_X0,
            BLANK_SRC_Y,
            GRID_W,
            BLANK_H);
        this.drawTexturedModalRect(
            offsetX + ExBusSlots.SLOT_X0,
            offsetY + ExBusSlots.SLOT_Y0 + BLANK_H,
            ExBusSlots.SLOT_X0,
            BLANK_SRC_Y,
            GRID_W,
            GRID_H - BLANK_H);

        final boolean oreFilter = this.cvb.getUpgradeable()
            .getInstalledUpgrades(Upgrades.ORE_FILTER) > 0;
        final int activeRows = oreFilter ? 0
            : ExBusSlots.rows(
                this.cvb.getUpgradeable()
                    .getInstalledUpgrades(Upgrades.CAPACITY));

        for (int r = 0; r < ExBusSlots.MAX_ROWS; r++) {
            final float alpha = r < activeRows ? 1.0F : 0.4F;
            if (alpha < 1.0F) {
                GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
                GL11.glColor4f(1.0F, 1.0F, 1.0F, alpha);
                GL11.glEnable(GL11.GL_BLEND);
            }
            for (int c = 0; c < ExBusSlots.COLUMNS; c++) {
                this.drawTexturedModalRect(
                    offsetX + ExBusSlots.SLOT_X0 + ExBusSlots.SLOT_SIZE * c,
                    offsetY + ExBusSlots.SLOT_Y0 + ExBusSlots.SLOT_SIZE * r,
                    SLOT_TILE_X,
                    SLOT_TILE_Y,
                    SLOT_TILE_SIZE,
                    SLOT_TILE_SIZE);
            }
            if (alpha < 1.0F) {
                GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                GL11.glPopAttrib();
            }
        }
    }

    /**
     * The eight upgrade slots hang off the art's right edge inside 1.21's own upgrade panel, so the container's
     * slot column sits where {@code UpgradesPanel} puts its slots: panel corner (174, 0) + 1/+6. One baked blit.
     */
    private void drawUpgradeStrip(final int offsetX, final int offsetY) {
        this.drawTexturedModalRect(
            offsetX + PANEL_X,
            offsetY + PANEL_Y,
            PANEL_SRC_X,
            PANEL_SRC_Y,
            PANEL_SRC_W,
            PANEL_SRC_H);
    }

    private void initVirtualSlots() {
        final IAEStackInventory config = this.host.getExConfig();

        for (int r = 0; r < ExBusSlots.MAX_ROWS; r++) {
            for (int c = 0; c < ExBusSlots.COLUMNS; c++) {
                final int index = r * ExBusSlots.COLUMNS + c;
                final VirtualMEPhantomSlot slot = new VirtualMEPhantomSlot(
                    ExBusSlots.SLOT_X0 + ExBusSlots.SLOT_SIZE * c,
                    ExBusSlots.SLOT_Y0 + ExBusSlots.SLOT_SIZE * r,
                    config,
                    index,
                    this::acceptType);
                this.virtualSlots[index] = slot;
                this.registerVirtualSlots(slot);
            }
        }
    }

    /**
     * The grid carries items and fluids side by side: the bus's own type stays first so item behaviour is unchanged,
     * and the fluid type is let through so the phantom slot's own conversion rules handle buckets, fluid display
     * items and decrements. No middle-click amount dialog - right-click with an empty hand counts down one unit,
     * exactly like AE2's own phantom slots.
     */
    private boolean acceptType(final VirtualMEPhantomSlot slot, final IAEStackType<?> type, final int mouseButton) {
        return type == this.host.getGuiStackType() || type == AEFluidStackType.FLUID_STACK_TYPE;
    }
}
