package com.asdflj.wtct.inventory;

import net.minecraft.inventory.IInventory;

import com.asdflj.wtct.inventory.item.INetworkTerminal;

public interface IPatternTerminal extends INetworkTerminal {

    IInventory getInventoryByName(final String name);

    boolean isInverted();

    boolean canBeSubstitute();

    boolean isPrioritize();

    boolean isSubstitution();

    boolean shouldCombine();

    void setCraftingRecipe(final boolean craftingMode);

    void setSubstitution(boolean canSubstitute);

    void setBeSubstitute(boolean canBeSubstitute);

    void setCombineMode(boolean shouldCombine);

    void setPrioritization(boolean canPrioritize);

    void setInverted(boolean inverted);

    int getActivePage();

    void setActivePage(int activePage);

    boolean isCraftingRecipe();

    /**
     * WCWT's "merge materials" option: while it is on, a processing pattern's identical inputs are
     * folded into a single cell. Only the comprehensive work terminal implements it today, so the
     * other hosts inherit "off".
     */
    default boolean isMergeMaterials() {
        return false;
    }

    default void setMergeMaterials(boolean mergeMaterials) {}

    default void sortCraftingItems() {}

    /**
     * The four switches of the pattern management area, stored with the terminal itself - the same four
     * WCWT keeps on its terminal item, defaults included: upload on, display mode VISIBLE, slots shown,
     * search mode 2 (the two-cell scope). They are read when the GUI opens and written back on every
     * click through {@link #saveSettings()}, so a terminal remembers how it was left. Only the
     * comprehensive work terminal has the area, so every other host inherits the defaults and ignores
     * the setters.
     */
    default boolean isPatternManagementUpload() {
        return true;
    }

    default void setPatternManagementUpload(final boolean upload) {}

    default int getPatternManagementDisplayMode() {
        return 1;
    }

    default void setPatternManagementDisplayMode(final int mode) {}

    default boolean isPatternManagementShowSlots() {
        return true;
    }

    default void setPatternManagementShowSlots(final boolean showSlots) {}

    default int getPatternManagementSearchMode() {
        return 2;
    }

    default void setPatternManagementSearchMode(final int mode) {}

    /**
     * The "enlarge the management area" switch: while it is on the management area is pulled up to
     * the pattern cache's own place and the cache is hidden outright - its slots are moved off screen
     * and answer to nothing, and the wheel over that strip drives the management list instead. Off by
     * default: a terminal that has never been touched opens exactly the way WCWT draws it.
     * Stored on the terminal item like the other four switches, so it outlives the GUI.
     */
    default boolean isPatternManagementExpanded() {
        return false;
    }

    default void setPatternManagementExpanded(final boolean expanded) {}

    void saveSettings();

    boolean hasRefillerUpgrade();
}
