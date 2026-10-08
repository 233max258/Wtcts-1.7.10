package com.asdflj.wtct.client.render;

import static appeng.client.gui.AEBaseGui.aeRenderItem;

import java.util.function.Predicate;

import net.minecraft.inventory.Slot;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import com.asdflj.wtct.client.gui.IGuiDrawSlot;
import com.glodblock.github.common.item.ItemFluidDrop;

import appeng.api.storage.data.IAEItemStack;

/**
 * Draws an ae2fc fluid drop as the fluid it stands for.
 *
 * <p>
 * Only the terminal's own cells are claimed. A fluid drop sitting in the player's inventory is a stack
 * the player is carrying, and {@code ItemFluidDrop} has a sprite of its own ({@code ae2fc:fluid_drop}),
 * so nothing is gained by painting a fluid over it - while the cell lost its contents whenever that
 * fluid had no still icon to draw: this renderer answered "handled", the vanilla item draw was skipped,
 * and the player was left with an empty-looking cell. Taking a stack out of the terminal into a hotbar
 * cell used to do exactly that whenever the stack was a fluid drop.
 *
 * <p>
 * The sprite is also checked before the slot is claimed at all: a fluid the atlas has no icon for is
 * not something this renderer can show, and the item's own sprite is the better answer in that case.
 */
public class RenderFluidDrop implements ISlotRender {

    @Override
    public Predicate<Slot> get() {
        return slot -> !ISlotRender.isPlayerSlot(slot) && slot.getStack() != null
            && slot.getStack()
                .getItem() instanceof ItemFluidDrop;
    }

    @Override
    public boolean drawSlot(Slot slot, IAEItemStack stack, IGuiDrawSlot draw, boolean display, Runnable baseDraw) {
        FluidStack fluidStack = ItemFluidDrop.getFluidStack(slot.getStack());
        if (fluidStack == null || !canDraw(fluidStack.getFluid())) return true;
        draw.drawWidget(slot.xDisplayPosition, slot.yDisplayPosition, fluidStack.getFluid());
        aeRenderItem.setAeStack(stack);
        draw.renderStackSize(display, stack, slot);
        return false;
    }

    /**
     * True when the fluid has a sprite to blit. {@code drawWidget} silently draws nothing without one,
     * so claiming the slot in that state would take the cell's contents with it.
     */
    static boolean canDraw(final Fluid fluid) {
        return fluid != null && fluid.getIcon() != null;
    }
}
