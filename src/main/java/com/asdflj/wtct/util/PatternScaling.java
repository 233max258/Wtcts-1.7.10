package com.asdflj.wtct.util;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.fluids.FluidStack;

import com.glodblock.github.common.item.ItemFluidDrop;
import com.glodblock.github.common.item.ItemFluidPacket;

import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.items.misc.ItemEncodedUltimatePattern;
import appeng.util.Platform;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;

/**
 * WCWT's batch pattern multiplier - the {@code Double_button0..7} row (⇄ ×2 ×3 ×5 / =1 ÷2 ÷3 ÷5).
 *
 * <p>
 * It works on a processing pattern's <em>amounts</em>, which live in two places: the encoder's own
 * input/output cells while a pattern is being edited, and the already encoded patterns sitting in
 * the pattern cache. Both sides need the same three operations, so they live here instead of being
 * duplicated in the container. WCWT applies every one of them to both places at once; only the
 * editor half is meaningless while a crafting pattern is being edited (a crafting matrix has no
 * amounts).
 *
 * <ul>
 * <li>{@code > 0} multiplies every amount, {@code < 0} divides them;
 * <li>{@link #REDUCE} is WCWT's "=1": divide every amount by the shared GCD of all of them, i.e.
 * reduce the pattern to its smallest whole-number ratio. It does <b>not</b> set everything to one,
 * so a 2:1 recipe stays 2:1;
 * <li>{@link #ROTATE} is WCWT's "⇄": rotate the outputs, which is how a secondary output gets
 * promoted to the primary one.
 * </ul>
 *
 * <p>
 * WCWT's {@code checkCanModify} is all-or-nothing: a multiply that would exceed the cap, or a
 * divide that would leave a remainder, leaves the pattern completely untouched.
 */
public final class PatternScaling {

    /** WCWT's "=1": reduce every amount by the shared GCD. */
    public static final int REDUCE = 0;
    /** WCWT's "⇄": rotate a pattern's outputs instead of scaling. */
    public static final int ROTATE = Integer.MIN_VALUE;

    /**
     * WCWT caps a pattern amount at 999999 units. An item is one unit; for ae2fc a fluid unit is a
     * bucket (1000 mB), so fluids get the same budget in AE2's mB accounting.
     */
    private static final long MAX_ITEM_AMOUNT = 999_999L;
    private static final long MAX_FLUID_AMOUNT = MAX_ITEM_AMOUNT * 1000L;

    private PatternScaling() {}

    // ---------------------------------------------------------------------------------------------
    // The operations, on plain ItemStacks (the encoder's cells and the decoded cache entries alike)
    // ---------------------------------------------------------------------------------------------

    /**
     * Applies {@code operation} to both amount lists, in place.
     *
     * @return true when something was written; false when the operation was a no-op or WCWT's check
     *         refused it, in which case nothing was touched at all
     */
    public static boolean apply(final List<ItemStack> inputs, final List<ItemStack> outputs, final int operation) {
        return apply(inputs, outputs, operation, false);
    }

    /**
     * The same operation with the terminal's "编程器电路始终为一" switch: with {@code keepCircuitAtOne}
     * a programming circuit is left exactly as it is - it is not scaled, it does not take part in the
     * "=1" ratio, and it does not make a divide inexact. A circuit picks the machine's recipe, so a
     * ×2 that turned "1 circuit + 8 plates" into "2 circuits + 16 plates" asked for a circuit the
     * machine does not have.
     */
    public static boolean apply(final List<ItemStack> inputs, final List<ItemStack> outputs, final int operation,
        final boolean keepCircuitAtOne) {
        if (operation == ROTATE) {
            return rotate(outputs);
        }
        if (operation == REDUCE) {
            return reduceRatio(inputs, outputs, keepCircuitAtOne);
        }
        if (!canScale(inputs, operation, keepCircuitAtOne) || !canScale(outputs, operation, keepCircuitAtOne)) {
            return false;
        }
        scaleList(inputs, operation, keepCircuitAtOne);
        scaleList(outputs, operation, keepCircuitAtOne);
        return true;
    }

    /** WCWT's "=1": divide every amount in both lists by their shared GCD. */
    private static boolean reduceRatio(final List<ItemStack> inputs, final List<ItemStack> outputs,
        final boolean keepCircuitAtOne) {
        final long gcd = sharedGcd(inputs, outputs, keepCircuitAtOne);
        if (gcd <= 1L) {
            return false; // already the smallest ratio
        }
        divideBy(inputs, gcd, keepCircuitAtOne);
        divideBy(outputs, gcd, keepCircuitAtOne);
        return true;
    }

    /** True when an operation may touch this cell at all. */
    private static boolean scalable(final ItemStack stack, final boolean keepCircuitAtOne) {
        return !keepCircuitAtOne || !PHUtil.isProgrammingCircuit(stack);
    }

    /**
     * WCWT's {@code rotateOutputs}: every non-empty entry takes the value of the <em>next</em>
     * non-empty entry, wrapping around, so empty cells stay empty - {@code [A, -, B]} becomes
     * {@code [B, -, A]} rather than sliding the entries into the gaps. Fewer than two entries means
     * there is nothing to promote and the list is left alone.
     */
    public static boolean rotate(final List<ItemStack> outputs) {
        int nonEmpty = 0;
        for (final ItemStack stack : outputs) {
            if (stack != null) {
                nonEmpty++;
            }
        }
        if (nonEmpty < 2) {
            return false;
        }
        final List<ItemStack> rotated = new ArrayList<>(outputs);
        for (int i = 0; i < outputs.size(); i++) {
            if (outputs.get(i) == null) {
                continue;
            }
            for (int j = 1; j < outputs.size(); j++) {
                final ItemStack next = outputs.get((i + j) % outputs.size());
                if (next != null) {
                    rotated.set(i, next);
                    break;
                }
            }
        }
        for (int i = 0; i < outputs.size(); i++) {
            outputs.set(i, rotated.get(i));
        }
        return true;
    }

    // ---------------------------------------------------------------------------------------------
    // The operations, on an encoded pattern's NBT
    // ---------------------------------------------------------------------------------------------

    /**
     * Rewrites the amounts of one encoded processing pattern. The pattern's own stack is returned
     * when it was rewritten (so callers can push it back through the slot), or {@code null} when it
     * has to be left alone: crafting patterns carry no amounts, patterns AE2 failed to parse are
     * marked with a sticky {@code InvalidPattern} tag, and a pattern whose entries cannot be read
     * back is skipped rather than risk writing a damaged one.
     */
    public static ItemStack applyToPattern(final ItemStack pattern, final int operation) {
        return applyToPattern(pattern, operation, false);
    }

    /**
     * The same rewrite with the terminal's "编程器电路始终为一" switch: an encoded pattern's own
     * circuit entry then keeps the amount it has, in the cache and everywhere else the multiply
     * reaches.
     */
    public static ItemStack applyToPattern(final ItemStack pattern, final int operation,
        final boolean keepCircuitAtOne) {
        if (pattern == null || !(pattern.getItem() instanceof ICraftingPatternItem) || !pattern.hasTagCompound()) {
            return null;
        }
        final NBTTagCompound tag = pattern.getTagCompound();
        if (tag.getBoolean("crafting") || tag.getBoolean("InvalidPattern")) {
            return null;
        }
        final NBTTagList inTag = tag.getTagList(IN_TAG, TAG_COMPOUND);
        final NBTTagList outTag = tag.getTagList(OUT_TAG, TAG_COMPOUND);
        final List<ItemStack> inputs = readCells(tag, IN_TAG);
        final List<ItemStack> outputs = readCells(tag, OUT_TAG);
        if (!allReadable(inTag, inputs) || !allReadable(outTag, outputs)) {
            return null; // an entry we cannot decode - leave the pattern untouched
        }
        if (!apply(inputs, outputs, operation, keepCircuitAtOne)) {
            return null;
        }
        // A rewritten entry keeps the shape its pattern's reader understands: only an Ultimate
        // pattern may carry a real fluid entry (see patternEntry).
        final boolean fluidAsStack = pattern.getItem() instanceof ItemEncodedUltimatePattern;
        tag.setTag(IN_TAG, writeEntries(inputs, fluidAsStack));
        tag.setTag(OUT_TAG, writeEntries(outputs, fluidAsStack));
        return pattern;
    }

    /** True when every entry the list actually holds came back as a stack. */
    private static boolean allReadable(final NBTTagList list, final List<ItemStack> cells) {
        for (int i = 0; i < list.tagCount() && i < cells.size(); i++) {
            if (!list.getCompoundTagAt(i)
                .hasNoTags() && cells.get(i) == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * A pattern's input and output cells (index 0 = inputs, index 1 = outputs), read the way AE2's
     * own pattern readers read them and aligned back to the list's layout.
     *
     * <p>
     * Each entry goes through {@code Platform.readStackNBT}, which is what both
     * {@code UltimatePatternHelper} and GTNH's own pattern terminal use: an entry names its stack type
     * ({@code item} / {@code fluid}) and may therefore hold a fluid, and older patterns that predate
     * that form are still read as items. Two things are fixed up on top, because those readers are
     * written for crafting jobs rather than for editing:
     *
     * <ul>
     * <li><b>positions</b>: the list-layout walk leaves an unused cell out of the result, so the raw
     * list is walked to put each entry back into its own cell - without that, a crafting pattern's
     * 3x3 layout would not survive the round trip;
     * <li><b>amounts</b>: the generic form keeps the amount in the long tag {@code Cnt} with
     * {@code Count} left at zero, the older one in {@code Count} alone, and only
     * {@code PatternHelper}'s constructor applies that fallback, so it is applied here as well.
     * </ul>
     *
     * @return null when the stack is not an encoded pattern or carries no NBT
     */
    public static List<List<ItemStack>> readPatternCells(final ItemStack pattern) {
        if (pattern == null || !(pattern.getItem() instanceof ICraftingPatternItem)) {
            return null;
        }
        final NBTTagCompound tag = pattern.getTagCompound();
        if (tag == null) {
            return null;
        }
        return List.of(readCells(tag, IN_TAG), readCells(tag, OUT_TAG));
    }

    /** One entry list as editing cells, positions preserved (an unused cell comes back as null). */
    private static List<ItemStack> readCells(final NBTTagCompound tag, final String key) {
        final NBTTagList list = tag.getTagList(key, TAG_COMPOUND);
        final List<ItemStack> cells = new ArrayList<>(list.tagCount());
        for (int i = 0; i < list.tagCount(); i++) {
            cells.add(readCell(list.getCompoundTagAt(i)));
        }
        return cells;
    }

    /** One encoded entry as the cell our editing area holds; null for an empty or unreadable one. */
    private static ItemStack readCell(final NBTTagCompound entry) {
        if (entry.hasNoTags()) {
            return null; // an unused cell
        }
        final ItemStack cell = cellOf(Platform.readStackNBT(entry, true));
        if (cell == null || amountOf(cell) > 0) {
            return cell;
        }
        final long amount = entry.hasKey(AMOUNT_LONG) ? entry.getLong(AMOUNT_LONG) : entry.getInteger(AMOUNT_INT);
        if (amount <= 0) {
            return null; // unreadable entry
        }
        setAmount(cell, amount);
        return cell;
    }

    /**
     * One encoded entry as the ItemStack our editing cells hold; null for an empty/unreadable one.
     *
     * <p>
     * A fluid entry is always handed back as the fluid packet, whatever shape AE2 read it in: the
     * packet is what this mod's encoder writes and what its cells draw as the fluid itself, while the
     * display item AE2 offers for a fluid ({@code getItemStackForNEI}) is a bare carrier - in a cell it
     * reads as an anonymous "fluid unit" with nothing but a stack size, which is exactly the report of
     * a marked fluid turning into an item. A pattern written by GTNH's own fluid-aware writer holds a
     * real fluid entry, and a pattern written by AE2FC's encoder holds drops; both are normalised here.
     */
    private static ItemStack cellOf(final IAEStack<?> stack) {
        if (stack == null) {
            return null;
        }
        if (stack instanceof IAEFluidStack fluid) {
            final FluidStack content = fluid.getFluidStack();
            return content == null ? null : ItemFluidPacket.newStack(content.copy());
        }
        // Pattern entries are items (this mod and ae2fc both represent a fluid as an ItemFluidPacket),
        // so this is the normal path; anything else falls back to AE2's display stack.
        final ItemStack cell = stack instanceof IAEItemStack item ? item.getItemStack() : stack.getItemStackForNEI();
        return asPacket(cell);
    }

    /**
     * A fluid drop (AE2FC's other carrier) turned into the packet shape, so a cell taken out of a
     * pattern the same way it would be put in. A packet is already in that shape and anything that is
     * not a fluid carrier passes through untouched.
     */
    private static ItemStack asPacket(final ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof ItemFluidDrop)) {
            return stack;
        }
        final FluidStack fluid = ItemFluidDrop.getFluidStack(stack);
        return fluid == null ? stack : ItemFluidPacket.newStack(fluid);
    }

    // ---------------------------------------------------------------------------------------------
    // Amounts (fluid packets count fluid, like everywhere else in this mod)
    // ---------------------------------------------------------------------------------------------

    public static long amountOf(final ItemStack stack) {
        if (stack == null) {
            return 0;
        }
        return stack.getItem() instanceof ItemFluidPacket ? ItemFluidPacket.getFluidAmount(stack) : stack.stackSize;
    }

    public static void setAmount(final ItemStack stack, final long amount) {
        if (stack == null) {
            return;
        }
        if (stack.getItem() instanceof ItemFluidPacket) {
            ItemFluidPacket.setFluidAmount(stack, amount);
        } else {
            stack.stackSize = (int) amount;
        }
    }

    private static long maxAmount(final ItemStack stack) {
        return stack.getItem() instanceof ItemFluidPacket ? MAX_FLUID_AMOUNT : MAX_ITEM_AMOUNT;
    }

    /** WCWT's {@code checkCanModify} for one list: multiply must fit the cap, divide must be exact. */
    private static boolean canScale(final List<ItemStack> stacks, final int operation, final boolean keepCircuitAtOne) {
        for (final ItemStack stack : stacks) {
            if (stack == null || !scalable(stack, keepCircuitAtOne)) {
                continue;
            }
            final long amount = amountOf(stack);
            if (amount <= 0) {
                return false;
            }
            final long divisor = -(long) operation;
            if (operation < 0 && amount % divisor != 0) {
                return false;
            }
            final long scaled = operation < 0 ? amount / divisor : amount * operation;
            if (scaled <= 0 || scaled > maxAmount(stack)) {
                return false;
            }
        }
        return true;
    }

    private static void scaleList(final List<ItemStack> stacks, final int operation, final boolean keepCircuitAtOne) {
        for (final ItemStack stack : stacks) {
            if (stack == null || !scalable(stack, keepCircuitAtOne)) {
                continue;
            }
            final long amount = amountOf(stack);
            setAmount(stack, operation < 0 ? amount / -(long) operation : amount * operation);
        }
    }

    private static void divideBy(final List<ItemStack> stacks, final long divisor, final boolean keepCircuitAtOne) {
        for (final ItemStack stack : stacks) {
            if (stack != null && scalable(stack, keepCircuitAtOne)) {
                setAmount(stack, amountOf(stack) / divisor);
            }
        }
    }

    /** GCD of every amount in both lists - WCWT's {@code computeSharedGcd}. */
    private static long sharedGcd(final List<ItemStack> inputs, final List<ItemStack> outputs,
        final boolean keepCircuitAtOne) {
        long gcd = 0L;
        for (final List<ItemStack> list : List.of(inputs, outputs)) {
            for (final ItemStack stack : list) {
                if (stack == null || !scalable(stack, keepCircuitAtOne)) {
                    continue;
                }
                final long amount = amountOf(stack);
                if (amount <= 0) {
                    continue;
                }
                gcd = gcd == 0L ? amount : gcd(gcd, amount);
                if (gcd == 1L) {
                    return 1L;
                }
            }
        }
        return gcd;
    }

    private static long gcd(final long a, final long b) {
        long left = Math.abs(a);
        long right = Math.abs(b);
        while (right != 0L) {
            final long remainder = left % right;
            left = right;
            right = remainder;
        }
        return left;
    }

    // ---------------------------------------------------------------------------------------------
    // Pattern NBT entries
    //
    // Reading goes through AE2's own reader (see readPatternCells); this section holds the writing
    // half and the tag names. An unused cell is an *empty compound*, and an entry is written the way
    // GTNH's own ME pattern terminal writes one - as a generic stack ({@code StackType}, the amount
    // in {@code Cnt}, {@code Count} at zero) - so a pattern made here is the pattern GTNH's terminals
    // and crafting jobs read.
    // ---------------------------------------------------------------------------------------------

    private static final int TAG_COMPOUND = 10;
    private static final String IN_TAG = "in";
    private static final String OUT_TAG = "out";
    private static final String AMOUNT_LONG = "Cnt";
    private static final String AMOUNT_INT = "Count";

    /**
     * One editing cell as the pattern entry AE2's readers expect.
     *
     * <p>
     * A cell holding fluid is written as a fluid stack ({@code StackType:"fluid"}), which is the
     * shape GTNH's ME pattern terminal writes one in - but only when {@code fluidAsStack} is set,
     * because only an Ultimate pattern is read by a reader that understands that shape. In a plain
     * encoded pattern such an entry has no item for AE2 to load and the pattern is marked invalid, so
     * the carrier item is written there instead.
     */
    public static NBTBase patternEntry(final ItemStack stack, final boolean fluidAsStack) {
        if (stack == null) {
            return new NBTTagCompound();
        }
        final FluidStack fluid = fluidAsStack ? fluidCarriedBy(stack) : null;
        final AEFluidStack entry = fluid == null ? null : AEFluidStack.create(fluid);
        if (entry != null) {
            entry.setStackSize(amountOf(stack));
            return entry.toNBTGeneric();
        }
        final AEItemStack item = AEItemStack.create(stack);
        return item == null ? new NBTTagCompound() : item.toNBTGeneric();
    }

    /**
     * The fluid a cell stands for, whichever carrier it uses; null for a plain item (a bucket
     * included - in a crafting grid a bucket is a bucket, which is the line AE2 draws as well).
     *
     * <p>
     * Besides the two carriers this mod writes itself, anything GTNH shows a fluid as answers here:
     * GregTech's display item and NEI's phantom items go through AE2's own stack-from-item lookup
     * ({@code AEFluidStackType.convertStackFromItem}), which is the same call the storage layer makes.
     * A recipe transfer leaves exactly those in an encoder's cell, with the fluid's own amount - the
     * stack size stays one - so this is what tells a cell "you are a fluid" and what reads the number
     * back out of it.
     */
    public static FluidStack fluidCarriedBy(final ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return null;
        }
        if (stack.getItem() instanceof ItemFluidPacket) {
            return ItemFluidPacket.getFluidStack(stack);
        }
        if (stack.getItem() instanceof ItemFluidDrop) {
            return ItemFluidDrop.getFluidStack(stack);
        }
        final IAEFluidStack fluid = appeng.util.item.AEFluidStackType.FLUID_STACK_TYPE.convertStackFromItem(stack);
        return fluid == null ? null : fluid.getFluidStack();
    }

    private static NBTTagList writeEntries(final List<ItemStack> entries, final boolean fluidAsStack) {
        final NBTTagList rebuilt = new NBTTagList();
        for (final ItemStack stack : entries) {
            rebuilt.appendTag(patternEntry(stack, fluidAsStack));
        }
        return rebuilt;
    }
}
