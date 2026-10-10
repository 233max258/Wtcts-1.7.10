package com.asdflj.wtct.client.gui.container.slot;

import net.minecraft.inventory.IInventory;

import appeng.container.slot.IOptionalSlotHost;
import appeng.container.slot.OptionalSlotFake;

public class SlotPatternFake extends OptionalSlotFake {

    private static final int POSITION_SHIFT = 9000;
    private boolean hidden = false;

    public SlotPatternFake(IInventory inv, IOptionalSlotHost containerBus, int idx, int x, int y, int offX, int offY,
        int groupNum) {
        super(inv, containerBus, idx, x, y, offX, offY, groupNum);
        this.setRenderDisabled(false);
    }

    public void setHidden(boolean hide) {
        if (this.hidden != hide) {
            this.hidden = hide;
            this.xDisplayPosition += (hide ? -1 : 1) * POSITION_SHIFT;
        }
    }

    /**
     * Sets the hidden flag without moving the slot.
     *
     * <p>
     * {@link #setHidden} hides a slot by shifting {@code xDisplayPosition}, so it may only be used
     * where the slot position has no other owner. The comprehensive terminal's GUI lays the encoding
     * area out itself - it writes {@code xDisplayPosition} directly (parking hidden cells at
     * {@code HIDDEN_SLOT}) - and never calls {@link #setHidden}, which left the flag stuck at false
     * while the slot sat off screen. Everything that keys off {@link #isHidden()} (the NEI badge in
     * {@code RenderPatternSlotFake}) then decorated a parked cell. Use this from the GUI so the flag
     * tracks the position the GUI itself assigned.
     */
    public void setHiddenFlag(boolean hide) {
        this.hidden = hide;
    }

    public boolean isHidden() {
        return this.hidden;
    }
}
