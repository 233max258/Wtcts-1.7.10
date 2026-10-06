package com.asdflj.wtct.loader;

import java.util.List;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.asdflj.wtct.api.Constants;
import com.asdflj.wtct.api.WtctAPI;
import com.asdflj.wtct.api.adapter.pattern.FCPatternTerminal;
import com.asdflj.wtct.api.adapter.pattern.IPatternTerminalAdapter;
import com.asdflj.wtct.api.adapter.pattern.IRecipeHandler;
import com.asdflj.wtct.api.adapter.pattern.THComprehensiveWorkTerminal;
import com.asdflj.wtct.client.gui.container.ContainerComprehensiveWorkTerminal;
import com.asdflj.wtct.nei.NEIUtils;
import com.asdflj.wtct.nei.object.OrderStack;
import com.asdflj.wtct.util.PatternScaling;

import appeng.api.storage.StorageName;
import appeng.api.storage.data.IAEStack;
import appeng.container.implementations.ContainerPatternTerm;
import appeng.container.implementations.ContainerPatternTermEx;
import appeng.tile.inventory.IAEStackInventory;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;

public class BRLoader implements Runnable {

    /** The CRAFTING_EX / OUTPUT_EX encoding area every structure transfer lands in. */
    private static final IPatternTerminalAdapter STRUCTURE_ADAPTER = new THComprehensiveWorkTerminal();

    @Override
    public void run() {
        IRecipeHandler handler = (container, inputs, outputs, identifier, adapter, message) -> {
            if (container instanceof ContainerPatternTerm c) {
                c.getPatternTerminal()
                    .setCraftingRecipe(false);
                IInventory inputSlot = adapter.getInventoryByName(c, adapter.getCraftingInvName());
                IInventory outputSlot = adapter.getInventoryByName(c, adapter.getOutputInvName());
                if (inputSlot == null || outputSlot == null) {
                    // A pattern terminal opened from a wireless terminal item keeps its encoding area on
                    // StorageName-backed AE stack inventories instead of item inventories, so both lookups
                    // above come back null there. Writing through those virtual slots is the only way such a
                    // host accepts a recipe - and leaving the null alone used to throw on the network thread
                    // and drop the player's connection.
                    writeVirtualSlots(c, inputs, outputs);
                    return;
                }
                for (int i = 0; i < inputSlot.getSizeInventory(); i++) {
                    inputSlot.setInventorySlotContents(i, null);
                }
                for (int i = 0; i < outputSlot.getSizeInventory(); i++) {
                    outputSlot.setInventorySlotContents(i, null);
                }
                inputs = NEIUtils.clearNull(inputs);
                outputs = NEIUtils.clearNull(outputs);
                adapter.transferPack(inputs, inputSlot);
                adapter.transferPack(outputs, outputSlot);
                c.onCraftMatrixChanged(inputSlot);
                c.onCraftMatrixChanged(outputSlot);
                c.getPatternTerminal()
                    .saveChanges();
            }
        };

        WtctAPI.instance()
            .terminal()
            .registerPatternTerminal(
                new FCPatternTerminal(ContainerPatternTerm.class).registerIdentifier(Constants.NEI_BR, handler));
        WtctAPI.instance()
            .terminal()
            .registerPatternTerminal(
                new FCPatternTerminal(ContainerPatternTermEx.class).registerIdentifier(Constants.NEI_BR, handler));
        // The comprehensive work terminal keeps its encoding area on the same CRAFTING_EX / OUTPUT_EX
        // inventories, but the structure transfer was never registered for it: the "?" button in a
        // multiblock preview sent NEI_BR for a terminal that had no handler and quietly did nothing.
        WtctAPI.instance()
            .terminal()
            .registerPatternTerminal(new THComprehensiveWorkTerminal())
            .registerIdentifier(Constants.NEI_BR, (container, inputs, outputs, identifier, adapter, message) -> {
                if (container instanceof ContainerComprehensiveWorkTerminal cwt) {
                    installStructure(cwt, inputs, outputs);
                }
            });

    }

    /**
     * Writes a multiblock structure into the comprehensive work terminal's encoding area: the blocks are
     * the inputs, the named paper the output, and the pattern is a processing one.
     *
     * <p>
     * Shared with the NEE packet hook, because the "?" button does not actually come through this
     * terminal's NEI transfer at all: BlockRenderer6343 sends NEE's {@code PacketNEIPatternRecipe}
     * straight to the server, whose handler gates on AE2's own {@code ContainerPatternTerm} and casts to
     * it, so every other container - this terminal included - had its structure dropped before it was
     * read. What the handler does afterwards is what happens here.
     */
    public static void installStructure(ContainerComprehensiveWorkTerminal cwt, List<OrderStack<?>> inputs,
        List<OrderStack<?>> outputs) {
        cwt.setCraftingMode(false);
        IInventory inputSlot = STRUCTURE_ADAPTER.getInventoryByName(cwt, STRUCTURE_ADAPTER.getCraftingInvName());
        IInventory outputSlot = STRUCTURE_ADAPTER.getInventoryByName(cwt, STRUCTURE_ADAPTER.getOutputInvName());
        if (inputSlot == null || outputSlot == null) return;
        for (int i = 0; i < inputSlot.getSizeInventory(); i++) {
            inputSlot.setInventorySlotContents(i, null);
        }
        for (int i = 0; i < outputSlot.getSizeInventory(); i++) {
            outputSlot.setInventorySlotContents(i, null);
        }
        STRUCTURE_ADAPTER.transferPack(NEIUtils.clearNull(inputs), inputSlot);
        STRUCTURE_ADAPTER.transferPack(NEIUtils.clearNull(outputs), outputSlot);
        cwt.saveChanges();
    }

    /**
     * Writes a recipe into the encoding area of a pattern terminal that exposes it as virtual slots: every slot of
     * the area is written, the ones the recipe does not fill cleared, exactly like the item inventory path does.
     */
    private static void writeVirtualSlots(ContainerPatternTerm container, List<OrderStack<?>> inputs,
        List<OrderStack<?>> outputs) {
        packVirtualSlots(container, StorageName.CRAFTING_INPUT, inputs);
        packVirtualSlots(container, StorageName.CRAFTING_OUTPUT, outputs);
        container.getPatternTerminal()
            .saveChanges();
    }

    private static void packVirtualSlots(ContainerPatternTerm container, StorageName name, List<OrderStack<?>> stacks) {
        IAEStackInventory inventory = container.getPatternTerminal()
            .getAEInventoryByName(name);
        if (inventory == null) return;
        // Every slot of the area is written, the ones the recipe does not fill cleared. This used to
        // hand the whole map to ContainerPatternTerm#receiveSlotStacks, which rv3-beta-1073 removed;
        // the inventory it forwarded to is right here, so the write happens directly.
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            inventory.putAEStackInSlot(i, null);
        }
        for (OrderStack<?> stack : stacks) {
            if (stack == null) continue;
            int index = stack.getIndex();
            if (index < 0 || index >= inventory.getSizeInventory()) continue;
            inventory.putAEStackInSlot(index, toAEStack(stack.getStack()));
        }
    }

    private static IAEStack<?> toAEStack(Object stack) {
        if (stack instanceof FluidStack fluid) {
            return AEFluidStack.create(fluid);
        }
        if (stack instanceof ItemStack item) {
            FluidStack fluid = PatternScaling.fluidCarriedBy(item);
            return fluid == null ? AEItemStack.create(item) : AEFluidStack.create(fluid);
        }
        return null;
    }
}
