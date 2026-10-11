package com.asdflj.wtct.coremod.mixin.nei;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import codechicken.nei.recipe.StackInfo;

/**
 * NEI's fluid-aware stack comparison dereferences {@code getFluid}'s result without checking it:
 *
 * <pre>
 * if (isFluidDisplayItem(a) &amp;&amp; isFluidDisplayItem(b) &amp;&amp; getFluid(a).isFluidEqual(getFluid(b))) ...
 * </pre>
 *
 * {@code getFluid} returns {@code null} for a display item whose fluid it cannot decode (a fluid packet
 * from another mod, an item whose handler only answers for some of its NBT, ...) - and the comparison then
 * dies with an NPE inside {@code ItemHistoryPanel#addItem}, which is what crashed the client while a
 * player asked NEI about a fluid from this terminal's list.
 *
 * <p>
 * The guard answers the question NEI was about to answer itself whenever the null case comes up: two
 * undecodable displays count as equal only if the stacks are identical, and an undecodable display is
 * never equal to a decodable one. Every other case is left to NEI's own code.
 */
@Mixin(StackInfo.class)
public abstract class MixinStackInfo {

    @Inject(method = "equalItemAndNBT", at = @At("HEAD"), cancellable = true, remap = false)
    private static void wtct$nullSafeFluidCompare(final ItemStack stackA, final ItemStack stackB, final boolean useNBT,
        final CallbackInfoReturnable<Boolean> cir) {
        if (stackA == null || stackB == null) {
            return; // NEI's own head already answers this.
        }
        if (!StackInfo.isFluidDisplayItem(stackA) || !StackInfo.isFluidDisplayItem(stackB)) {
            return;
        }
        final FluidStack fluidA = StackInfo.getFluid(stackA);
        final FluidStack fluidB = StackInfo.getFluid(stackB);
        if (fluidA != null && fluidB != null) {
            return;
        }
        cir.setReturnValue(fluidA == null && fluidB == null && ItemStack.areItemStacksEqual(stackA, stackB));
    }
}
