package com.asdflj.wtct.client.gui;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import com.asdflj.wtct.Wtct;
import com.asdflj.wtct.client.gui.container.ContainerCardConfig;
import com.asdflj.wtct.client.gui.widget.GuiWcwtBackButton;
import com.asdflj.wtct.client.gui.widget.GuiWcwtUpgradePanel;
import com.asdflj.wtct.common.item.card.ItemWcwtUpgradeCard;
import com.asdflj.wtct.inventory.InventoryHandler;
import com.asdflj.wtct.inventory.gui.GuiType;
import com.asdflj.wtct.network.CPacketTerminalBtns;

import appeng.api.storage.ITerminalHost;
import appeng.client.gui.AEBaseGui;

/**
 * The import / export card's screen - AE2ImportExportCard's {@code UpgradeScreen} in the 1.7.10
 * flavour.
 *
 * <p>
 * The card's ghost filter runs across the top (two rows of nine, one more row per capacity card in
 * the card's own upgrade slots), the player's inventory sits under it, and the card's upgrade column
 * is drawn with the same WTLib panel the terminal itself uses, anchored at {@code right 2, top 0}.
 *
 * <p>
 * Marking a slot is a click on that slot with an empty hand, exactly like the addon: import marks a
 * slot with a checkmark, export cycles the slot through the filter entries (the number drawn on it)
 * and a right click clears it. The two mass-select buttons fill or clear a whole row at once.
 */
public class GuiCardConfig extends AEBaseGui {

    private static final String BG = "guis/wcwt/card_bg_%d.png";
    private static final String CHECKMARK = "guis/wcwt/card_checkmark.png";
    private static final String XMARK = "guis/wcwt/card_xmark.png";
    private static final String MASS_SELECT = "guis/wcwt/card_mass_select.png";

    /** The mass-select cells, from the addon: 16x16 sprite, 4x5 of it is the button. */
    private static final int MASS_SELECT_X = ContainerCardConfig.PLAYER_X + 16 * 10;
    private static final int MASS_SELECT_HIT_W = 4;
    private static final int MASS_SELECT_HIT_H = 5;
    private static final int MASS_SELECT_GAP = 16 * 3 + 10;
    /** AE2's own binding offsets, so the player's rows can be recovered from the slot coordinates. */
    private static final int PLAYER_BIND_HOTBAR_Y = 58;

    private final ContainerCardConfig container;
    private final ITerminalHost host;
    private final ItemWcwtUpgradeCard.Kind kind;
    /** The marks as the screen shows them; the server is told about every change. */
    private final int[] selectedSlots;

    private GuiWcwtUpgradePanel upgradePanel;
    private GuiWcwtBackButton backButton;
    private com.asdflj.wtct.client.gui.widget.GuiWcwtFuzzyButton fuzzyButton;
    private com.asdflj.wtct.client.gui.widget.GuiWcwtHintArea upgradeHint;
    /** Capacity-card count the screen was last laid out for, so a change can re-layout it. */
    private int laidOutCapacity = -1;

    public GuiCardConfig(final InventoryPlayer ip, final ITerminalHost host, final ItemWcwtUpgradeCard.Kind kind) {
        super(new ContainerCardConfig(ip, host, kind));
        this.container = (ContainerCardConfig) this.inventorySlots;
        this.host = host;
        this.kind = kind;
        this.selectedSlots = this.container.getSelectedSlots();
        this.xSize = ContainerCardConfig.GUI_WIDTH;
    }

    private ItemStack card() {
        return this.container.getCard();
    }

    private int capacityCards() {
        final ItemStack card = this.card();
        return card == null ? 0 : ItemWcwtUpgradeCard.capacityCards(card);
    }

    /** Screen height: one 18px row per capacity card, like the addon's four backgrounds. */
    private int heightFor(final int capacity) {
        return ContainerCardConfig.BASE_HEIGHT + capacity * ContainerCardConfig.HEIGHT_PER_ROW;
    }

    @Override
    public void initGui() {
        // updateScreen() rebuilds this screen whenever a capacity card goes in or out (see there),
        // and only GuiScreen.setWorldAndResolution clears this list - a direct initGui() call does
        // not. Without this the widgets added below stack up, one more return button per capacity
        // card, all drawing on top of each other.
        this.buttonList.clear();
        // Both sizes have to be in place *before* AEBaseGui.initGui(), because that is where
        // GuiContainer centres the screen from them - resizing afterwards would leave guiLeft/guiTop
        // computed for the old height.
        this.xSize = ContainerCardConfig.GUI_WIDTH;
        this.ySize = this.heightFor(this.capacityCards());
        super.initGui();
        this.laidOutCapacity = this.capacityCards();
        this.layout();

        // The card's own upgrade column rides the same WTLib panel the terminal uses. It has no
        // singularity slot - that belongs to the terminal - so the column is just the card slots.
        this.upgradePanel = new GuiWcwtUpgradePanel(
            null,
            this.container.getCardUpgradeSlots(),
            this.xSize - 2,
            0,
            null);
        this.upgradePanel.setMaxRows(Math.max(2, this.container.getCardUpgradeSlots().length));
        this.upgradePanel.layout();

        // AE2 1.21's back tab (plate + arrow), the icon every screen of this mod closes with.
        this.buttonList.add(
            this.backButton = new GuiWcwtBackButton(
                0,
                this.guiLeft + ContainerCardConfig.GUI_WIDTH - 24,
                this.guiTop - 5));

        // The addon's fuzzy toggle sits on the left toolbar (import_card.json's verticalToolbar at
        // left 18, top 1) and only appears with a fuzzy card nested in the card.
        this.buttonList.add(
            this.fuzzyButton = new com.asdflj.wtct.client.gui.widget.GuiWcwtFuzzyButton(
                this.guiLeft + 18,
                this.guiTop + 1,
                ""));

        // The upgrade panel's hover area, with the addon's "Compatible Upgrades" list. The panel is
        // anchored at right 2 / top 0 (AE2's common.json), so its origin is known here.
        this.buttonList.add(
            this.upgradeHint = new com.asdflj.wtct.client.gui.widget.GuiWcwtHintArea(
                this.guiLeft + ContainerCardConfig.GUI_WIDTH - 2,
                this.guiTop,
                this.upgradePanel.boundsWidth(),
                this.upgradePanel.boundsHeight(),
                compatibleUpgrades()));

        this.refreshCardWidgets();
    }

    /**
     * The addon's {@code UpgradesPanel} tooltip: "Compatible Upgrades" followed by what this card
     * takes, which is the same list its guide page gives.
     */
    private String compatibleUpgrades() {
        final StringBuilder text = new StringBuilder(StatCollector.translateToLocal("wtct.card.compatible"));
        appendUpgrade(text, ItemWcwtUpgradeCard.capacityCard(), ItemWcwtUpgradeCard.MAX_CAPACITY_CARDS);
        appendUpgrade(text, ItemWcwtUpgradeCard.fuzzyCard(), 1);
        if (this.kind == ItemWcwtUpgradeCard.Kind.IMPORT) {
            appendUpgrade(text, inverterCard(), 1);
        } else {
            appendUpgrade(text, craftingCard(), 1);
            appendUpgrade(text, speedCard(), 1);
        }
        return text.toString();
    }

    private void appendUpgrade(final StringBuilder text, final appeng.api.definitions.IItemDefinition card,
        final int limit) {
        final ItemStack is = card.maybeStack(1)
            .orNull();
        if (is == null) {
            return;
        }
        text.append('\n')
            .append(is.getDisplayName())
            .append(" ×")
            .append(limit);
    }

    private appeng.api.definitions.IItemDefinition inverterCard() {
        return appeng.api.AEApi.instance()
            .definitions()
            .materials()
            .cardInverter();
    }

    private appeng.api.definitions.IItemDefinition craftingCard() {
        return appeng.api.AEApi.instance()
            .definitions()
            .materials()
            .cardCrafting();
    }

    private appeng.api.definitions.IItemDefinition speedCard() {
        return appeng.api.AEApi.instance()
            .definitions()
            .materials()
            .cardSpeed();
    }

    /** Re-reads the card for the widgets that follow its state - the fuzzy toggle's icon and reach. */
    private void refreshCardWidgets() {
        final ItemStack card = this.card();
        final boolean fuzzy = card != null && ItemWcwtUpgradeCard.hasUpgrade(card, ItemWcwtUpgradeCard.fuzzyCard());
        this.fuzzyButton.visible = fuzzy;
        if (fuzzy) {
            this.fuzzyButton.setMode(ItemWcwtUpgradeCard.fuzzyMode(card));
            this.fuzzyButton.setTooltip(
                StatCollector.translateToLocal("wtct.card.fuzzy.title") + ": "
                    + StatCollector.translateToLocal(
                        "wtct.card.fuzzy." + ItemWcwtUpgradeCard.fuzzyMode(card)
                            .name()));
        }
    }

    /**
     * Lays the slots out for the current capacity count: filter rows on top, the player's rows
     * against the screen's bottom, the armour column down the left edge, and everything past the
     * active filter count parked off-screen.
     */
    private void layout() {
        final int capacity = this.capacityCards();
        final int height = this.heightFor(capacity);
        final int active = this.container.getActiveFilterSlotCount();
        final Slot[] filters = this.container.getFilterSlots();
        for (int i = 0; i < filters.length; i++) {
            this.place(
                filters[i],
                i < active,
                ContainerCardConfig.FILTER_X + i % ContainerCardConfig.FILTER_COLS * 18,
                ContainerCardConfig.FILTER_Y + i / ContainerCardConfig.FILTER_COLS * 18);
        }
        final Slot[] armor = this.container.getArmorSlots();
        for (int i = 0; i < armor.length; i++) {
            this.place(
                armor[i],
                true,
                ContainerCardConfig.ARMOR_X,
                ContainerCardConfig.ARMOR_TOP + capacity * ContainerCardConfig.HEIGHT_PER_ROW + i * 18);
        }
        // The player's 36 slots. Positions are recovered from the coordinates AE2 bound them with
        // rather than from the slot class alone: AE2 swaps the slot holding the terminal for a
        // SlotDisabled, which is neither a SlotPlayerInv nor a SlotPlayerHotBar.
        for (final Object o : this.inventorySlots.inventorySlots) {
            if (!(o instanceof final appeng.container.slot.AppEngSlot s) || !isPlayerSlot(s)) {
                continue;
            }
            final boolean hotbar = s.getY() >= PLAYER_BIND_HOTBAR_Y;
            final int column = (s.getX() - ContainerCardConfig.PLAYER_X) / 18;
            s.xDisplayPosition = ContainerCardConfig.PLAYER_X + column * 18;
            s.yDisplayPosition = hotbar ? height - ContainerCardConfig.PLAYER_HOTBAR_BOTTOM
                : height - ContainerCardConfig.PLAYER_INV_BOTTOM + s.getY();
        }
        final Slot[] cardUpgrades = this.container.getCardUpgradeSlots();
        for (int i = 0; i < cardUpgrades.length; i++) {
            this.place(cardUpgrades[i], true, 0, 0);
        }
    }

    /** True for the 36 slots {@code bindPlayerInventory} added, terminal slot included. */
    private static boolean isPlayerSlot(final appeng.container.slot.AppEngSlot slot) {
        return slot instanceof appeng.container.slot.SlotPlayerInv
            || slot instanceof appeng.container.slot.SlotPlayerHotBar
            || slot instanceof appeng.container.slot.SlotDisabled;
    }

    private void place(final Slot slot, final boolean visible, final int x, final int y) {
        if (slot == null) {
            return;
        }
        if (visible) {
            slot.xDisplayPosition = x;
            slot.yDisplayPosition = y;
        } else {
            slot.xDisplayPosition = -9999;
            slot.yDisplayPosition = -9999;
        }
    }

    @Override
    public void drawBG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        final int capacity = this.capacityCards();
        this.bindCardTexture(String.format(BG, capacity));
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.drawTexturedModalRect(offsetX, offsetY, 0, 0, ContainerCardConfig.GUI_WIDTH, this.ySize);
        if (this.upgradePanel != null) {
            this.upgradePanel.draw(this.mc, offsetX, offsetY);
        }
        // The card's own empty upgrade seats, in AE2 1.21's own card sprite - see SlotWcwtCard.
        com.asdflj.wtct.client.gui.container.slot.SlotWcwtCard
            .drawHints(this.mc, offsetX, offsetY, this.inventorySlots.inventorySlots);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /**
     * Binds one of this screen's own textures. {@code AEBaseGui.bindTexture(String)} resolves against
     * AE2's assets rather than this mod's, which paints every sprite here as the missing-texture
     * checkerboard - the mod's namespace has to be named explicitly.
     */
    private void bindCardTexture(final String file) {
        this.mc.getTextureManager()
            .bindTexture(new ResourceLocation(Wtct.MODID, "textures/" + file));
    }

    /**
     * A capacity card in or out of the card's own upgrade slots changes the screen height and the
     * number of filter rows, so the screen is rebuilt - which is exactly what the addon's
     * {@code updateCapacityLayout} does. Done from the screen tick rather than from the draw pass,
     * because rebuilding widgets mid-render is not safe.
     */
    @Override
    public void updateScreen() {
        super.updateScreen();
        if (this.capacityCards() != this.laidOutCapacity) {
            this.initGui();
            return;
        }
        this.refreshCardWidgets();
    }

    @Override
    public void drawFG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        final ItemStack card = this.card();
        if (card != null) {
            this.fontRendererObj.drawString(card.getDisplayName(), ContainerCardConfig.FILTER_X, 6, 0x404040);
        }
        this.fontRendererObj.drawString(
            StatCollector.translateToLocal("container.inventory"),
            ContainerCardConfig.FILTER_X,
            this.ySize - 95,
            0x404040);

        // The foreground layer is drawn inside the (guiLeft, guiTop) translate, so slot coordinates
        // are used as they are - the offsets are only for hit testing.
        this.drawMarks();
        this.drawFilterIndices();
        this.drawMassSelect();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /**
     * The export card numbers its filter slots - every active one, marked or not - which is what
     * makes the marks on the player's slots legible: a slot showing 3 takes the third filter slot's
     * item. The addon draws this in {@code extractContents} for every fake slot when the card is an
     * export card.
     */
    private void drawFilterIndices() {
        if (this.kind != ItemWcwtUpgradeCard.Kind.EXPORT) {
            return;
        }
        final Slot[] filters = this.container.getFilterSlots();
        for (int i = 0; i < this.container.getActiveFilterSlotCount() && i < filters.length; i++) {
            final Slot slot = filters[i];
            if (slot == null || slot.xDisplayPosition < 0) {
                continue;
            }
            drawGreenIndex(slot.xDisplayPosition, slot.yDisplayPosition, i + 1);
        }
    }

    /** The green filter index, half scale in the slot's top-right corner - the addon's mark style. */
    private void drawGreenIndex(final int x, final int y, final int index) {
        final String text = Integer.toString(index);
        GL11.glPushMatrix();
        GL11.glTranslatef(x + 16 - this.fontRendererObj.getStringWidth(text) * 0.5F, y, 300.0F);
        GL11.glScalef(0.5F, 0.5F, 1.0F);
        this.fontRendererObj.drawString(text, 0, 0, 0x00FF00);
        GL11.glPopMatrix();
    }

    /** The checkmark / filter number on every player slot, and the cross on the unused ones. */
    private void drawMarks() {
        for (final Object o : this.inventorySlots.inventorySlots) {
            if (!(o instanceof final Slot slot)) {
                continue;
            }
            final int index = this.slotIndexOf(slot);
            if (index < 0 || index >= this.selectedSlots.length || slot.xDisplayPosition < 0) {
                continue;
            }
            final int mark = this.selectedSlots[index];
            final int x = slot.xDisplayPosition;
            final int y = slot.yDisplayPosition;
            if (mark >= 1) {
                if (this.kind == ItemWcwtUpgradeCard.Kind.IMPORT) {
                    this.blitTexture(CHECKMARK, x, y);
                } else {
                    // Export writes the filter index in green, at half scale in the slot's top-right
                    // corner - the same corner the checkmark occupies.
                    this.drawGreenIndex(x, y, mark);
                }
            } else if (mark == 0) {
                this.blitTexture(XMARK, x, y);
            }
        }
    }

    private void drawMassSelect() {
        final int top = this.massSelectY();
        this.blitTexture(MASS_SELECT, MASS_SELECT_X, top);
        this.blitTexture(MASS_SELECT, MASS_SELECT_X, top + MASS_SELECT_GAP);
    }

    private void blitTexture(final String texture, final int x, final int y) {
        this.bindCardTexture(texture);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.drawTexturedModalRect(x, y, 0, 0, 16, 16);
    }

    /** Where the mass-select buttons sit, under the filter rows. */
    private int massSelectY() {
        return 76 + this.capacityCards() * ContainerCardConfig.HEIGHT_PER_ROW;
    }

    /**
     * The player-inventory index a slot addresses, or -1 for slots the card does not mark.
     *
     * <p>
     * All three slot flavours were built with the player-inventory index as their slot index, so the
     * index is read straight off the slot: the main inventory and hotbar come from AE2's
     * {@code bindPlayerInventory} (9..35 and 0..8), and the armour slots were built as
     * {@code 39 - i} (39 = helmet, 36 = boots), which is the same numbering
     * {@code ItemWcwtUpgradeCard.SELECTED_SLOT_COUNT} uses. Deriving the armour index from the armour
     * <em>type</em> instead would be off by three - it would mark the boots when the helmet is
     * clicked.
     */
    private int slotIndexOf(final Slot slot) {
        if (slot instanceof appeng.container.slot.SlotPlayerInv
            || slot instanceof appeng.container.slot.SlotPlayerHotBar
            || slot instanceof ContainerCardConfig.SlotPlayerArmor) {
            return slot.getSlotIndex();
        }
        return -1;
    }

    // ---------------------------------------------------------------------------------------------
    // Marking slots
    // ---------------------------------------------------------------------------------------------

    @Override
    protected void mouseClicked(final int mouseX, final int mouseY, final int button) {
        // The card's own upgrade column hangs outside the window's rectangle, and vanilla treats a
        // click outside the window as "throw what the cursor holds onto the ground" - which is how a
        // card picked out of that column used to end up in the world. Widening the rectangle over the
        // column for the duration of the click keeps the two consistent. Same fix as the terminal's.
        final int xSize = this.xSize;
        final int ySize = this.ySize;
        if (this.upgradePanel != null) {
            // The column is anchored at {@code xSize - 2} (see initGui), so its right edge is what the
            // window rectangle has to reach.
            this.xSize = Math.max(this.xSize, xSize - 2 + this.upgradePanel.boundsWidth() + 1);
            this.ySize = Math.max(this.ySize, this.upgradePanel.boundsHeight());
        }
        try {
            this.mouseClickedInside(mouseX, mouseY, button);
        } finally {
            this.xSize = xSize;
            this.ySize = ySize;
        }
    }

    /** The click handling itself; {@link #mouseClicked} only widens the window around it. */
    private void mouseClickedInside(final int mouseX, final int mouseY, final int button) {
        if (this.mc.thePlayer.inventory.getItemStack() == null && !isShiftKeyDown()) {
            if (this.handleMassSelect(mouseX, mouseY, button)) {
                return;
            }
            final Slot slot = this.getSlotAtPosition(mouseX, mouseY);
            final int index = slot == null ? -1 : this.slotIndexOf(slot);
            if (index >= 0) {
                this.selectedSlots[index] = this.nextMark(this.selectedSlots[index], button);
                this.sendUpdate();
                return;
            }
        }
        super.mouseClicked(mouseX, mouseY, button);
    }

    /** Import toggles a slot; export walks it through "off" and every filter entry. */
    private int nextMark(final int current, final int button) {
        if (button == 1) {
            return 0;
        }
        if (this.kind == ItemWcwtUpgradeCard.Kind.IMPORT) {
            return current == 0 ? 1 : 0;
        }
        final int active = this.container.getActiveFilterSlotCount();
        return current >= active ? 0 : current + 1;
    }

    /** The row buttons: fill or clear the main inventory (top) or the hotbar (bottom) at once. */
    private boolean handleMassSelect(final int mouseX, final int mouseY, final int button) {
        final int top = this.guiTop + this.massSelectY();
        // The sprite starts one pixel left of the glyph, so the clickable cell is the addon's 4x5
        // window at x + 1, y + 1.
        final int x = this.guiLeft + MASS_SELECT_X + 1;
        final boolean hotbar = this.isOn(x, top + MASS_SELECT_GAP, mouseX, mouseY);
        final boolean inventory = this.isOn(x, top, mouseX, mouseY);
        if (!hotbar && !inventory) {
            return false;
        }
        // The hotbar marks live at 0..8, the main inventory at 9..35.
        final int from = hotbar ? 0 : 9;
        final int to = hotbar ? 9 : 36;
        for (int i = from; i < to; i++) {
            if (button == 1) {
                this.selectedSlots[i] = 0;
            } else if (this.kind == ItemWcwtUpgradeCard.Kind.IMPORT) {
                this.selectedSlots[i] = 1;
            } else {
                this.selectedSlots[i] = Math
                    .min(this.container.getActiveFilterSlotCount(), Math.max(1, this.selectedSlots[i] + 1));
            }
        }
        this.sendUpdate();
        return true;
    }

    private boolean isOn(final int x, final int y, final int mouseX, final int mouseY) {
        return mouseX >= x && mouseX < x + MASS_SELECT_HIT_W && mouseY >= y + 1 && mouseY < y + 1 + MASS_SELECT_HIT_H;
    }

    private void sendUpdate() {
        final NBTTagCompound tag = new NBTTagCompound();
        tag.setIntArray("Slots", this.selectedSlots);
        Wtct.proxy.netHandler.sendToServer(new CPacketTerminalBtns("CardConfig.SetSlots", this.kind.ordinal(), tag));
    }

    @Override
    protected void actionPerformed(final net.minecraft.client.gui.GuiButton button) {
        if (button == this.backButton) {
            InventoryHandler.switchGui(GuiType.COMPREHENSIVE_WORK_TERMINAL);
            return;
        }
        if (button == this.fuzzyButton) {
            Wtct.proxy.netHandler
                .sendToServer(new CPacketTerminalBtns("CardConfig.CycleFuzzy", 0, new NBTTagCompound()));
            return;
        }
        super.actionPerformed(button);
    }
}
