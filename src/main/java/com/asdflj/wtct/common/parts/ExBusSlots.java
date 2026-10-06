package com.asdflj.wtct.common.parts;

import com.asdflj.wtct.common.Config;

/**
 * Layout constants shared by the ME extended import and export buses.
 *
 * <p>
 * The two buses differ only in what they do with a marked slot, so their filter grid, upgrade strip and screen
 * geometry are defined once here. The grid always has nine columns; each capacity card adds a row, starting at two
 * rows and capping at seven - 18 slots without cards, 63 with all five.
 */
public final class ExBusSlots {

    public static final int COLUMNS = 9;
    /** Rows available with no capacity card installed. */
    public static final int BASE_ROWS = 2;
    public static final int MAX_ROWS = 7;
    public static final int MAX_SLOTS = COLUMNS * MAX_ROWS;
    public static final int UPGRADE_SLOTS = 8;

    /**
     * Fluid slots move in bucket units: the per-tick budget is tuned for items, where a single unit is a
     * meaningful transfer, while a single fluid unit is one millibucket - nothing. So a fluid slot's budget is
     * the shared item budget scaled to buckets, one item unit per bucket: that floors at {@link #MIN_FLUID_TRANSFER}
     * with no speed cards and lets every speed card lift it linearly. {@link net.minecraftforge.fluids.FluidStack}
     * carries an int amount, so the result is clamped there.
     */
    public static final int MIN_FLUID_TRANSFER = 1000;

    /** The budget a fluid slot moves this tick: the shared item budget, one bucket per item unit. */
    public static long fluidTransferBudget(final long itemBudget) {
        return Math.min(itemBudget * MIN_FLUID_TRANSFER, Integer.MAX_VALUE);
    }

    /**
     * Screen geometry, taken from AE2 1.21's io bus screen (the screen ExtendedAE's buses reuse): the config grid
     * starts at (8, 29) with an eighteen pixel pitch and its first two rows are baked into the background art.
     */
    public static final int SLOT_X0 = 8;
    public static final int SLOT_Y0 = 29;
    public static final int SLOT_SIZE = 18;
    /**
     * Panel height used for slot math. AE2 1.21 places the player inventory at {@code height - 84} and the "Inventory"
     * label at {@code height - 95}, both of which this reproduces, while AE2's 1.7.10 formulas subtract 82 and 93.
     */
    public static final int HEIGHT = 251;
    /** Size of the background art, whose last two rows are the panel's bottom border. */
    public static final int BG_HEIGHT = 253;
    public static final int BG_WIDTH = 176;

    /**
     * 1.21 upgrade panel geometry, from AE2's layout resolution chain: the shared "upgrades" widget style is
     * {@code right: 2, top: 0}, which {@code Position.resolve} turns into {@code (imageWidth - 2, 0)} against the
     * 176-wide io bus art - so the panel's top-left overlaps the art's right border by two pixels and protrudes
     * 26px beyond it - and {@code UpgradesPanel} lays its slots out at one pixel inside that corner horizontally
     * and PADDING (5) + 1 vertically.
     */
    public static final int UPGRADE_PANEL_X = BG_WIDTH - 2;
    public static final int UPGRADE_PANEL_Y = 0;
    public static final int UPGRADE_SLOT_X = UPGRADE_PANEL_X + 1;
    public static final int UPGRADE_SLOT_Y = UPGRADE_PANEL_Y + 6;

    private ExBusSlots() {}

    public static int rows(final int capacityCards) {
        return Math.min(BASE_ROWS + Math.max(capacityCards, 0), MAX_ROWS);
    }

    public static int slots(final int capacityCards) {
        return rows(capacityCards) * COLUMNS;
    }

    /**
     * Scales a bus' per-tick budget by the configured multiplier.
     *
     * <p>
     * The widest AE2 bus tops out at 67 million items per tick, so the multiplication has to happen in a long: times
     * the largest allowed multiplier it overflows an int several times over.
     */
    public static int scaleSpeed(final int baseAmount) {
        return (int) Math.min((long) baseAmount * Config.exBusSpeedMultiplier, Integer.MAX_VALUE);
    }
}
