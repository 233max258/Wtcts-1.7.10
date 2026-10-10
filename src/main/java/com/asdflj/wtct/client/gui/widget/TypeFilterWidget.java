package com.asdflj.wtct.client.gui.widget;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;

import com.asdflj.wtct.Wtct;
import com.asdflj.wtct.network.CPacketTypeFilter;
import com.asdflj.wtct.network.CPacketTypeFilterRequest;

import appeng.api.storage.data.AEStackTypeRegistry;
import appeng.api.storage.data.IAEStackType;
import appeng.client.gui.widgets.TypeToggleButton;
import it.unimi.dsi.fastutil.objects.Reference2BooleanMap;

/**
 * Shared client-side helper that renders one {@link TypeToggleButton} per registered {@link IAEStackType} and keeps a
 * local copy of the per-type visibility map. GUIs delegate their {@code getTypeFilter()} to {@link #getFilters()} so
 * the
 * shared {@code ItemRepo} can apply the filter, and route toggle clicks through {@link #handleButtonClick(GuiButton)}.
 */
public class TypeFilterWidget {

    private final Map<TypeToggleButton, IAEStackType<?>> buttons = new IdentityHashMap<>();
    private Reference2BooleanMap<IAEStackType<?>> filters;
    private final int windowId;
    /** True while laying out the header row, whose buttons wear WCWT's switch art. */
    private boolean skinned;

    public TypeFilterWidget(int windowId) {
        this.windowId = windowId;
    }

    /** Stacked downwards from {@code (x, yStart)}, 20 apart - AE2's own column left of the panel. */
    public void init(List<GuiButton> buttonList, int x, int yStart) {
        this.init(buttonList, x, yStart, 0, 20);
        this.requestSavedState();
    }

    /**
     * Laid out as one row: WCWT anchors its display toggles (item, fluid, everything else) at left 79,
     * 104 and 129 of the terminal header - all at the same top, 25 apart.
     */
    public void initRow(List<GuiButton> buttonList, int x, int y, int pitch) {
        this.skinned = true;
        try {
            this.init(buttonList, x, y, pitch, 0);
        } finally {
            this.skinned = false;
        }
    }

    private void init(List<GuiButton> buttonList, int xStart, int yStart, int pitchX, int pitchY) {
        this.buttons.clear();
        if (this.filters == null) {
            return;
        }
        int x = xStart;
        int y = yStart;
        for (final IAEStackType<?> type : AEStackTypeRegistry.getSortedTypes()) {
            final ResourceLocation texture = type.getButtonTexture();
            final IIcon icon = type.getButtonIcon();
            if (texture == null || icon == null) {
                continue;
            }
            // One toggle per registered type, and each governs its own type only (the map is keyed by type
            // and the click flips that key alone). The column layouts keep AE2's own TypeToggleButton, which
            // paints type.getButtonIcon() - the item type's crate, the fluid type's water drop - so the
            // button still says which type it manages; only the header row wears WCWT's plain switch.
            final TypeToggleButton btn = this.skinned
                ? new GuiWcwtTypeButton(x, y, texture, icon, type.getDisplayName())
                : new TypeToggleButton(x, y, texture, icon, type.getDisplayName());
            btn.setEnabled(this.filters.getBoolean(type));
            this.buttons.put(btn, type);
            buttonList.add(btn);
            x += pitchX;
            y += pitchY;
        }
    }

    public void setFilters(Reference2BooleanMap<IAEStackType<?>> filters) {
        this.filters = filters;
    }

    /**
     * Asks the server for the filter the terminal was saved with. The GUI opens with "everything enabled" (its own
     * copy of the map is not persisted anywhere - the terminal item's NBT is the only store), so without this the three
     * 物品 / 流体 / 源质 toggles always came back fully enabled.
     */
    public void requestSavedState() {
        Wtct.proxy.netHandler.sendToServer(new CPacketTypeFilterRequest(this.windowId));
    }

    public Reference2BooleanMap<IAEStackType<?>> getFilters() {
        return this.filters;
    }

    /**
     * @return true when the click hit a type-toggle button and was handled.
     */
    public boolean handleButtonClick(GuiButton btn) {
        if (!(btn instanceof TypeToggleButton tbtn)) {
            return false;
        }
        final IAEStackType<?> type = this.buttons.get(tbtn);
        if (type == null || this.filters == null) {
            return false;
        }
        final boolean next = !this.filters.getBoolean(type);
        this.filters.put(type, next);
        tbtn.setEnabled(next);
        Wtct.proxy.netHandler.sendToServer(new CPacketTypeFilter(this.windowId, type.getId(), next));
        return true;
    }

    /**
     * Reflects the current filter map on the toggle buttons that are already built - the same write a
     * click does - instead of rebuilding the GUI. The saved-filter sync used to run
     * {@code reInitalize} (a whole {@code initGui}: every button, every ME slot, the whole layout)
     * just to flip the three switches the terminal opens with, which re-created the entire screen a
     * moment after it opened.
     */
    public void syncButtonStates() {
        for (final Map.Entry<TypeToggleButton, IAEStackType<?>> e : this.buttons.entrySet()) {
            e.getKey()
                .setEnabled(this.filters != null && this.filters.getBoolean(e.getValue()));
        }
    }
}
