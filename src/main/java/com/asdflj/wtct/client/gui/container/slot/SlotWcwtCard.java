package com.asdflj.wtct.client.gui.container.slot;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.asdflj.wtct.Wtct;

import appeng.container.slot.AppEngSlot;

/**
 * A slot whose acceptance rule is the backing inventory's own
 * {@code isItemValidForSlot} - which {@code AppEngSlot} does <em>not</em> consult: its
 * {@code isItemValid} falls through to vanilla's "everything fits", so without this override any
 * item can be hand-placed into the card column.
 *
 * <p>
 * This is the same delegation AE2's own {@code SlotRestrictedInput} makes; the card column just
 * backs it with {@code WcwtUpgradesInventory} / {@code CardUpgradeInventory} instead of a
 * placable-item-type check.
 *
 * <p>
 * An empty cell shows AE2 1.21's card sprite, which {@link #drawHints} paints. AE2's own
 * placeholder mechanism cannot be used for it: {@code AppEngSlot}'s icon is an index into
 * AE2 1.7.10's own {@code guis/states.png} - the sprite it draws there is the pre-1.21 card - and
 * that sheet is inside AE2, not this mod. So the slots carry no icon at all and the screens paint
 * the 1.21 sprite themselves, from the copy of 1.21's {@code states.png} shipped in this mod.
 */
public class SlotWcwtCard extends AppEngSlot {

    /** AE2 1.21's redrawn {@code states.png}, the sheet its card sprite lives in. */
    private static final ResourceLocation HINTS = new ResourceLocation(
        Wtct.MODID,
        "textures/guis/wcwt/ae2_121_states.png");

    /** {@code Icon.BACKGROUND_UPGRADE} / {@code Icon.BACKGROUND_SINGULARITY} in that sheet. */
    public static final int HINT_UPGRADES_U = 240, HINT_UPGRADES_V = 208;
    public static final int HINT_SINGULARITY_U = 240, HINT_SINGULARITY_V = 160;
    private static final int HINT_SIZE = 16;
    /** The alpha AE2 draws slot icons at. */
    private static final float HINT_ALPHA = 0.4F;

    /** Whether this slot paints the empty-cell card hint. */
    private final boolean hint;

    public SlotWcwtCard(final IInventory inventory, final int index, final int x, final int y) {
        this(inventory, index, x, y, true);
    }

    /** {@code hint} adds the faint-card placeholder; the card's own inner slots want it too. */
    public SlotWcwtCard(final IInventory inventory, final int index, final int x, final int y, final boolean hint) {
        super(inventory, index, x, y);
        this.hint = hint;
    }

    @Override
    public boolean isItemValid(final ItemStack stack) {
        if (!this.isEnabled()) {
            return false;
        }
        if (stack == null || stack.getItem() == null) {
            return false;
        }
        return this.inventory.isItemValidForSlot(this.getSlotIndex(), stack);
    }

    /**
     * Paints every empty card seat of {@code slots} with the 1.21 card sprite. Called from the
     * screens' background layer: that layer is drawn before the slots and their items, so the hint
     * sits under anything placed in the cell, and it takes screen coordinates rather than the slot
     * pass's translated ones.
     */
    public static void drawHints(final Minecraft mc, final int guiX, final int guiY, final List<Slot> slots) {
        for (final Slot slot : slots) {
            if (slot instanceof final SlotWcwtCard card && card.hint) {
                drawHint(mc, guiX, guiY, slot, HINT_UPGRADES_U, HINT_UPGRADES_V);
            }
        }
    }

    /**
     * Paints the sheet's 16x16 sprite at {@code (u, v)} over {@code slot} while it is empty. The
     * slot must have had its AE2 icon cleared, or AE2 draws its own placeholder underneath: see the
     * class comment. A slot parked off-screen (see the upgrade panel) draws nothing.
     */
    public static void drawHint(final Minecraft mc, final int guiX, final int guiY, final Slot slot, final int u,
        final int v) {
        if (slot == null || slot.xDisplayPosition < 0 || slot.getHasStack()) {
            return;
        }
        mc.getTextureManager()
            .bindTexture(HINTS);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        blit(guiX + slot.xDisplayPosition, guiY + slot.yDisplayPosition, u, v);
        GL11.glDisable(GL11.GL_BLEND);
    }

    private static void blit(final int x, final int y, final int u, final int v) {
        final float f = 1.0F / 256.0F;
        final Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();
        tess.setColorRGBA_F(1.0F, 1.0F, 1.0F, HINT_ALPHA);
        tess.addVertexWithUV(x, y + HINT_SIZE, 0.0F, u * f, (v + HINT_SIZE) * f);
        tess.addVertexWithUV(x + HINT_SIZE, y + HINT_SIZE, 0.0F, (u + HINT_SIZE) * f, (v + HINT_SIZE) * f);
        tess.addVertexWithUV(x + HINT_SIZE, y, 0.0F, (u + HINT_SIZE) * f, v * f);
        tess.addVertexWithUV(x, y, 0.0F, u * f, v * f);
        tess.draw();
    }
}
