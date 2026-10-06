package com.asdflj.wtct.client.gui.container;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;

import appeng.api.storage.ITerminalHost;
import appeng.container.AEBaseContainer;
import appeng.container.slot.AppEngSlot;

/**
 * WCWT's trash screen - its {@code WcwtTrashMenu}: twenty-seven slots that destroy whatever is put
 * in them. The slots are real while the screen is open, so a stack can be taken back out before
 * closing; {@link #onContainerClosed} clears the inventory, which is what deletes it - WTLib's own
 * trash is an in-memory {@code AppEngInternalInventory(27)} that is never persisted either.
 */
public class ContainerWcwtTrash extends AEBaseContainer {

    // WTLib's trash.json: a 176x195 screen, trash grid at (8,18), player rows bottom 110/52.
    public static final int GUI_WIDTH = 176;
    public static final int GUI_HEIGHT = 195;

    public static final int TRASH_X = 8;
    public static final int TRASH_Y = 18;
    public static final int TRASH_COLS = 9;
    public static final int TRASH_ROWS = 3;
    public static final int TRASH_SLOTS = TRASH_COLS * TRASH_ROWS;

    /** AE2's own hotbar binding offset, so the player's rows can be told apart client-side. */
    public static final int PLAYER_BIND_HOTBAR_Y = 58;
    public static final int PLAYER_INV_BOTTOM = 110;
    public static final int PLAYER_HOTBAR_BOTTOM = 52;

    private final IInventory trash = new InventoryBasic("wcwt.trash", false, TRASH_SLOTS);

    public ContainerWcwtTrash(final InventoryPlayer ip, final ITerminalHost host) {
        super(ip, host);
        for (int row = 0; row < TRASH_ROWS; row++) {
            for (int col = 0; col < TRASH_COLS; col++) {
                this.addSlotToContainer(
                    new AppEngSlot(this.trash, row * TRASH_COLS + col, TRASH_X + col * 18, TRASH_Y + row * 18));
            }
        }
        // The player's rows sit at x=8, which is AE2's own binding - no offset needed.
        this.bindPlayerInventory(ip, 0, 0);
    }

    @Override
    public void onContainerClosed(final net.minecraft.entity.player.EntityPlayer player) {
        super.onContainerClosed(player);
        // Anything still in the trash at this point is destroyed - that is the trash.
        for (int i = 0; i < this.trash.getSizeInventory(); i++) {
            this.trash.setInventorySlotContents(i, null);
        }
    }
}
