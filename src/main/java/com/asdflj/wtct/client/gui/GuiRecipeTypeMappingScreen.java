package com.asdflj.wtct.client.gui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.StatCollector;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import com.asdflj.wtct.client.gui.widget.GuiWcwtBackButton;
import com.asdflj.wtct.util.PatternMappingStore;

/**
 * Port of ExtendedAE Plus' {@code RecipeTypeMappingScreen} (配方类型映射管理) - the screen WCWT's
 * 映射管理 button opens when Plus is installed.
 *
 * <p>
 * Layout and behaviour are taken straight from the original: a centred panel (up to 600x390) with a
 * filter box, a key/value pair and a save button, a paged list of {@code key → value} rows, page
 * arrows, and a bottom row of 新建 / 删除选中 / 重新加载 / 返回 buttons. Only the widgets differ -
 * 1.7.10 uses GuiButton/GuiTextField instead of Minecraft's Button/EditBox.
 */
public class GuiRecipeTypeMappingScreen extends GuiScreen {

    private static final int ROW_HEIGHT = 22;
    private static final int ROW_BUTTON_H = 20;
    private static final int INPUT_H = 20;
    private static final int SAVE_WIDTH = 76;
    private static final int GAP = 5;
    private static final int PANEL_FILL = 0xE0101010;
    private static final int PANEL_EDGE = 0xFF808080;
    private static final int LABEL_COLOR = 0xFFB0B0B0;
    private static final int OK_COLOR = 0xFF55FF55;
    /**
     * Row plates. Drawn by hand rather than with a vanilla {@code GuiButton}: 1.7.10's
     * {@code GuiButton.drawButton} splits the button in two and takes the right half's U coordinate
     * as {@code 200 - width / 2}, so anything wider than 400px samples outside the widget atlas and
     * repeats unrelated sprites across the row - the white bands this screen used to show.
     */
    private static final int ROW_FILL = 0xFF9E9E9E;
    private static final int ROW_FILL_HOVER = 0xFFB4B4B4;
    private static final int ROW_EDGE = 0xFF000000;
    private static final int ROW_TEXT = 0xFFE0E0E0;
    private static final int ROW_TEXT_SELECTED = 0xFFFFFFFF;
    private static final int ERR_COLOR = 0xFFFF5555;

    private static final int ID_SAVE = 100;
    private static final int ID_PREV = 1000;
    private static final int ID_NEXT = 1001;
    private static final int ID_NEW = 2000;
    private static final int ID_DELETE = 2001;
    private static final int ID_RELOAD = 2002;
    private static final int ID_BACK = 2003;
    /** Opens the provider chooser so the value can be picked from the network's real providers. */
    private static final int ID_PICK_PROVIDER = 2004;

    private final GuiScreen parent;
    /**
     * The terminal's provider list, straight from the interface-terminal stream the pattern access
     * terminal (二合一接口终端) also uses. Handed to the chooser so a value can be picked from the
     * providers that actually exist instead of being typed blind.
     */
    private final List<GuiProviderSelectScreen.Entry> providers;

    private final List<PatternMappingStore.RecipeTypeMapping> mappings = new ArrayList<>();
    private final List<PatternMappingStore.RecipeTypeMapping> filtered = new ArrayList<>();

    private MappingField filterField;
    private MappingField keyField;
    private MappingField valueField;

    private String selectedKey;
    private String status = "";
    private int statusColor = LABEL_COLOR;
    private int page;
    private int pageSize = 6;
    private boolean needsRebuild;

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int innerX;
    private int innerW;

    public GuiRecipeTypeMappingScreen(final GuiScreen parent) {
        this(parent, List.of());
    }

    public GuiRecipeTypeMappingScreen(final GuiScreen parent, final List<GuiProviderSelectScreen.Entry> providers) {
        this.parent = parent;
        this.providers = providers == null ? List.of() : List.copyOf(providers);
    }

    // ------------------------------------------------------------- layout

    @Override
    public void initGui() {
        this.buttonList.clear();
        computeLayout();

        this.pageSize = Math.max(1, (this.panelH - 166) / ROW_HEIGHT);
        if (this.page > 0 && this.page * this.pageSize >= this.filtered.size()) {
            this.page = Math.max(0, (this.filtered.size() - 1) / this.pageSize);
        }
        if (this.mappings.isEmpty() && this.filtered.isEmpty()) {
            reloadMappings(false);
        }

        final int inputW = Math.max(60, (this.innerW - SAVE_WIDTH * 2 - GAP * 3) / 2);
        final int pairY = this.panelY + 64;

        if (this.filterField == null) {
            this.filterField = new MappingField(
                this.fontRendererObj,
                this.innerX,
                this.panelY + 28,
                this.innerW,
                INPUT_H,
                "wtct.gui.mapping.filter");
        }
        this.filterField.place(this.innerX, this.panelY + 28, this.innerW, INPUT_H);

        if (this.keyField == null) {
            this.keyField = new MappingField(
                this.fontRendererObj,
                this.innerX,
                pairY,
                inputW,
                INPUT_H,
                "wtct.gui.mapping.key");
        }
        this.keyField.place(this.innerX, pairY, inputW, INPUT_H);

        if (this.valueField == null) {
            this.valueField = new MappingField(
                this.fontRendererObj,
                this.innerX + inputW + GAP,
                pairY,
                inputW,
                INPUT_H,
                "wtct.gui.mapping.value");
        }
        this.valueField.place(this.innerX + inputW + GAP, pairY, inputW, INPUT_H);

        this.buttonList.add(
            new GuiButton(
                ID_SAVE,
                this.innerX + (inputW + GAP) * 2,
                pairY,
                SAVE_WIDTH,
                INPUT_H,
                StatCollector.translateToLocal("wtct.gui.mapping.save")));
        // Picking from the network's providers beats typing a name by hand - and it is the only way
        // to know which names the mapping can actually resolve to.
        final GuiButton pick = new GuiButton(
            ID_PICK_PROVIDER,
            this.innerX + (inputW + GAP) * 2 + SAVE_WIDTH + GAP,
            pairY,
            SAVE_WIDTH,
            INPUT_H,
            StatCollector.translateToLocal("wtct.gui.mapping.pick"));
        pick.enabled = !this.providers.isEmpty();
        this.buttonList.add(pick);

        // The page rows are painted in drawScreen and hit-tested in mouseClicked - see ROW_FILL.

        final int navY = this.panelY + this.panelH - 54;
        final GuiButton prev = new GuiButton(ID_PREV, this.panelX + this.panelW / 2 - 102, navY, 24, ROW_BUTTON_H, "<");
        final GuiButton next = new GuiButton(ID_NEXT, this.panelX + this.panelW / 2 + 78, navY, 24, ROW_BUTTON_H, ">");
        prev.enabled = this.page > 0;
        next.enabled = (this.page + 1) * this.pageSize < this.filtered.size();
        this.buttonList.add(prev);
        this.buttonList.add(next);

        final int actionY = this.panelY + this.panelH - 28;
        final int actionW = (this.innerW - GAP * 2) / 3;
        this.buttonList.add(
            new GuiButton(
                ID_NEW,
                this.innerX,
                actionY,
                actionW,
                ROW_BUTTON_H,
                StatCollector.translateToLocal("wtct.gui.mapping.new")));
        final GuiButton delete = new GuiButton(
            ID_DELETE,
            this.innerX + actionW + GAP,
            actionY,
            actionW,
            ROW_BUTTON_H,
            StatCollector.translateToLocal("wtct.gui.mapping.delete"));
        delete.enabled = this.selectedKey != null;
        this.buttonList.add(delete);
        this.buttonList.add(
            new GuiButton(
                ID_RELOAD,
                this.innerX + (actionW + GAP) * 2,
                actionY,
                actionW,
                ROW_BUTTON_H,
                StatCollector.translateToLocal("wtct.gui.mapping.reload")));
        // Going back is AE2 1.21's own back tab, hanging over the panel's top-right corner like the
        // terminal's dialogs do - the bottom row keeps its three list actions.
        this.buttonList.add(new GuiWcwtBackButton(ID_BACK, this.panelX + this.panelW - 24, this.panelY - 5));
    }

    private void computeLayout() {
        this.panelW = Math.min(600, this.width - 20);
        this.panelH = Math.min(390, this.height - 20);
        this.panelX = (this.width - this.panelW) / 2;
        this.panelY = (this.height - this.panelH) / 2;
        this.innerX = this.panelX + 12;
        this.innerW = this.panelW - 24;
    }

    // ----------------------------------------------------------- behaviour

    private void reloadMappings(final boolean showStatus) {
        PatternMappingStore.load();
        this.mappings.clear();
        this.mappings.addAll(PatternMappingStore.getRecipeTypeMappings());
        applyFilter();
        if (showStatus) {
            setStatus("wtct.gui.mapping.reloaded", OK_COLOR);
        }
        this.needsRebuild = true;
    }

    private void applyFilter() {
        final String query = this.filterField == null ? ""
            : this.filterField.getText()
                .trim()
                .toLowerCase(java.util.Locale.ROOT);
        this.filtered.clear();
        for (final PatternMappingStore.RecipeTypeMapping mapping : this.mappings) {
            if (query.isEmpty() || mapping.key()
                .toLowerCase(java.util.Locale.ROOT)
                .contains(query)
                || mapping.value()
                    .toLowerCase(java.util.Locale.ROOT)
                    .contains(query)) {
                this.filtered.add(mapping);
            }
        }
    }

    private void selectMapping(final PatternMappingStore.RecipeTypeMapping mapping) {
        this.selectedKey = mapping.key();
        this.keyField.setText(mapping.key());
        this.valueField.setText(mapping.value());
        this.needsRebuild = true;
    }

    private void clearSelection() {
        this.selectedKey = null;
        this.keyField.setText("");
        this.valueField.setText("");
        this.keyField.setFocused(true);
        this.needsRebuild = true;
    }

    private void saveMapping() {
        final String key = this.keyField.getText()
            .trim();
        final String value = this.valueField.getText()
            .trim();
        if (key.isEmpty() || value.isEmpty()) {
            setStatus("wtct.gui.mapping.required", ERR_COLOR);
            return;
        }
        final String previousKey = this.selectedKey;
        if (!PatternMappingStore.addOrUpdateAliasMapping(key, value)) {
            setStatus("wtct.gui.mapping.save_failed", ERR_COLOR);
            return;
        }
        // Renaming a selected mapping drops the old key, exactly like the original.
        if (previousKey != null && !previousKey.equalsIgnoreCase(key)) {
            PatternMappingStore.removeRecipeTypeMapping(previousKey);
        }
        reloadMappings(false);
        this.selectedKey = this.mappings.stream()
            .map(PatternMappingStore.RecipeTypeMapping::key)
            .filter(saved -> saved.equalsIgnoreCase(key))
            .findFirst()
            .orElse(key);
        setStatus("wtct.gui.mapping.saved", OK_COLOR);
    }

    private void deleteSelectedMapping() {
        if (this.selectedKey == null) {
            return;
        }
        if (!PatternMappingStore.removeRecipeTypeMapping(this.selectedKey)) {
            setStatus("wtct.gui.mapping.delete_failed", ERR_COLOR);
            return;
        }
        this.selectedKey = null;
        this.keyField.setText("");
        this.valueField.setText("");
        reloadMappings(false);
        setStatus("wtct.gui.mapping.deleted", OK_COLOR);
    }

    private void changePage(final int delta) {
        final int nextPage = this.page + delta;
        if (nextPage < 0 || nextPage * this.pageSize >= this.filtered.size()) {
            return;
        }
        this.page = nextPage;
        this.needsRebuild = true;
    }

    private void setStatus(final String key, final int color) {
        this.status = StatCollector.translateToLocal(key);
        this.statusColor = color;
    }

    @Override
    protected void actionPerformed(final GuiButton button) {
        switch (button.id) {
            case ID_SAVE -> saveMapping();
            case ID_PREV -> changePage(-1);
            case ID_NEXT -> changePage(1);
            case ID_NEW -> clearSelection();
            case ID_DELETE -> deleteSelectedMapping();
            case ID_RELOAD -> reloadMappings(true);
            case ID_PICK_PROVIDER -> this.mc.displayGuiScreen(
                new GuiProviderSelectScreen(
                    this,
                    this.providers,
                    this.valueField == null ? "" : this.valueField.getText(),
                    name -> {
                        if (this.valueField != null) {
                            this.valueField.setText(name);
                        }
                    }));
            case ID_BACK -> this.mc.displayGuiScreen(this.parent);
            default -> {}
        }
    }

    @Override
    public void updateScreen() {
        if (this.needsRebuild) {
            this.needsRebuild = false;
            initGui();
        }
        if (this.filterField != null) {
            this.filterField.updateCursorCounter();
        }
        if (this.keyField != null) {
            this.keyField.updateCursorCounter();
        }
        if (this.valueField != null) {
            this.valueField.updateCursorCounter();
        }
    }

    // --------------------------------------------------------------- input

    @Override
    protected void keyTyped(final char character, final int key) {
        if (key == Keyboard.KEY_ESCAPE) {
            this.mc.displayGuiScreen(this.parent);
            return;
        }
        if (key == Keyboard.KEY_RETURN) {
            saveMapping();
            return;
        }
        for (final MappingField field : new MappingField[] { this.filterField, this.keyField, this.valueField }) {
            if (field != null && field.isFocused() && field.textboxKeyTyped(character, key)) {
                if (field == this.filterField) {
                    this.page = 0;
                    applyFilter();
                }
                return;
            }
        }
        super.keyTyped(character, key);
    }

    @Override
    protected void mouseClicked(final int mouseX, final int mouseY, final int mouseButton) {
        for (final MappingField field : new MappingField[] { this.filterField, this.keyField, this.valueField }) {
            if (field != null) {
                field.setFocused(false);
            }
        }
        for (final MappingField field : new MappingField[] { this.filterField, this.keyField, this.valueField }) {
            if (field != null && field.isInside(mouseX, mouseY)) {
                field.setFocused(true);
                field.mouseClicked(mouseX, mouseY, mouseButton);
                return;
            }
        }
        if (mouseButton == 0) {
            final int index = this.rowAt(mouseX, mouseY);
            if (index >= 0) {
                selectMapping(this.filtered.get(index));
                return;
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    // ------------------------------------------------------------- drawing

    /** Top edge of a visible row, in screen coordinates. */
    private int rowY(final int rowIndex) {
        return this.panelY + 94 + rowIndex * ROW_HEIGHT;
    }

    /** Index into {@link #filtered} under the cursor, or -1. */
    private int rowAt(final int mouseX, final int mouseY) {
        if (mouseX < this.innerX || mouseX >= this.innerX + this.innerW) {
            return -1;
        }
        for (int i = 0; i < this.pageSize; i++) {
            final int y = this.rowY(i);
            if (mouseY >= y && mouseY < y + ROW_BUTTON_H) {
                final int index = this.page * this.pageSize + i;
                return index < this.filtered.size() ? index : -1;
            }
        }
        return -1;
    }

    /**
     * One plate per visible mapping, the label centred on it and a ▶ in front of the selected one -
     * Plus' own row look, painted here because the rows are too wide for a 1.7.10 GuiButton.
     */
    private void drawMappingRows(final int mouseX, final int mouseY) {
        final int start = this.page * this.pageSize;
        final int end = Math.min(start + this.pageSize, this.filtered.size());
        for (int index = start; index < end; index++) {
            final PatternMappingStore.RecipeTypeMapping mapping = this.filtered.get(index);
            final int y = this.rowY(index - start);
            final boolean hover = this.rowAt(mouseX, mouseY) == index;
            final boolean selected = mapping.key()
                .equals(this.selectedKey);
            drawRect(this.innerX, y, this.innerX + this.innerW, y + ROW_BUTTON_H, hover ? ROW_FILL_HOVER : ROW_FILL);
            // One-pixel frame, the way the vanilla button is bevelled.
            drawRect(this.innerX, y, this.innerX + this.innerW, y + 1, ROW_EDGE);
            drawRect(this.innerX, y + ROW_BUTTON_H - 1, this.innerX + this.innerW, y + ROW_BUTTON_H, ROW_EDGE);
            drawRect(this.innerX, y, this.innerX + 1, y + ROW_BUTTON_H, ROW_EDGE);
            drawRect(this.innerX + this.innerW - 1, y, this.innerX + this.innerW, y + ROW_BUTTON_H, ROW_EDGE);
            final String label = (selected ? "\u25B6 " : "") + mapping.key() + "  \u2192  " + mapping.value();
            this.drawCenteredString(
                this.fontRendererObj,
                label,
                this.innerX + this.innerW / 2,
                y + (ROW_BUTTON_H - 8) / 2,
                selected ? ROW_TEXT_SELECTED : ROW_TEXT);
        }
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    public void drawScreen(final int mouseX, final int mouseY, final float partialTicks) {
        drawDefaultBackground();
        GL11.glEnable(GL11.GL_BLEND);
        drawRect(this.panelX, this.panelY, this.panelX + this.panelW, this.panelY + this.panelH, PANEL_FILL);
        // Four one-pixel edges, like the original's fill() calls.
        drawRect(this.panelX, this.panelY, this.panelX + this.panelW, this.panelY + 1, PANEL_EDGE);
        drawRect(
            this.panelX,
            this.panelY + this.panelH - 1,
            this.panelX + this.panelW,
            this.panelY + this.panelH,
            PANEL_EDGE);
        drawRect(this.panelX, this.panelY, this.panelX + 1, this.panelY + this.panelH, PANEL_EDGE);
        drawRect(
            this.panelX + this.panelW - 1,
            this.panelY,
            this.panelX + this.panelW,
            this.panelY + this.panelH,
            PANEL_EDGE);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        if (this.filterField != null) {
            this.filterField.draw();
        }
        if (this.keyField != null) {
            this.keyField.draw();
        }
        if (this.valueField != null) {
            this.valueField.draw();
        }

        drawMappingRows(mouseX, mouseY);

        super.drawScreen(mouseX, mouseY, partialTicks);

        drawCenteredString(
            this.fontRendererObj,
            StatCollector.translateToLocal("wtct.gui.mapping.title"),
            this.width / 2,
            this.panelY + 9,
            0xFFFFFF);
        this.fontRendererObj.drawString(
            StatCollector.translateToLocal("wtct.gui.mapping.key"),
            this.panelX + 12,
            this.panelY + 53,
            LABEL_COLOR);
        this.fontRendererObj.drawString(
            StatCollector.translateToLocal("wtct.gui.mapping.value"),
            this.panelX + 17 + this.keyField.width,
            this.panelY + 53,
            LABEL_COLOR);

        final int pages = Math.max(1, (this.filtered.size() + this.pageSize - 1) / this.pageSize);
        drawCenteredString(
            this.fontRendererObj,
            StatCollector.translateToLocalFormatted(
                "wtct.gui.mapping.page",
                this.filtered.isEmpty() ? 0 : this.page + 1,
                pages,
                this.filtered.size()),
            this.width / 2,
            this.panelY + this.panelH - 48,
            LABEL_COLOR);
        if (!this.status.isEmpty()) {
            this.fontRendererObj
                .drawString(this.status, this.panelX + 12, this.panelY + this.panelH - 66, this.statusColor);
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    /** A bordered field that shows its hint while it is empty, like the original's EditBox. */
    private static final class MappingField extends GuiTextField {

        private final net.minecraft.client.gui.FontRenderer font;
        private final String hint;

        private MappingField(final net.minecraft.client.gui.FontRenderer font, final int x, final int y,
            final int width, final int height, final String hintKey) {
            super(font, x, y, width, height);
            this.font = font;
            this.hint = StatCollector.translateToLocal(hintKey);
            this.setMaxStringLength(256);
        }

        private void place(final int x, final int y, final int width, final int height) {
            this.xPosition = x;
            this.yPosition = y;
            this.width = width;
            this.height = height;
        }

        private boolean isInside(final int mouseX, final int mouseY) {
            return mouseX >= this.xPosition && mouseX < this.xPosition + this.width
                && mouseY >= this.yPosition
                && mouseY < this.yPosition + this.height;
        }

        private void draw() {
            this.drawTextBox();
            if (this.getText()
                .isEmpty()) {
                this.font.drawStringWithShadow(
                    this.hint,
                    this.xPosition + 4,
                    this.yPosition + (this.height - 8) / 2,
                    0x707070);
            }
        }

    }
}
