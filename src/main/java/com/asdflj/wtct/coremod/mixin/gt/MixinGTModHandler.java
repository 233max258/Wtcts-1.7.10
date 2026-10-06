package com.asdflj.wtct.coremod.mixin.gt;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.asdflj.wtct.api.IAnyTierElectricItem;

import gregtech.api.util.GTModHandler;

/**
 * Lets the terminal into GregTech machines at every voltage. {@code isElectricItem(ItemStack, byte)}
 * is the gate on the battery buffer's slots, and it compares the item's IC2 tier against the
 * machine's tier <em>exactly</em> - the buffer itself only charges an item whose tier equals its own
 * ({@code chargeElectricItem} has the same gate, which the terminal walks through by answering
 * {@code -1} there, a value GT already treats as tier-less). Without this, a tier-less item is
 * recognised as electric but matched against no buffer, so the slots would refuse it and "cannot be
 * charged in GT machines" would stand.
 *
 * <p>
 * The exemption is scoped to the marker: every other item - AE2's own powered tools included - keeps
 * the exact-match behaviour GT's voltage design rests on.
 */
@Mixin(value = GTModHandler.class, remap = false)
public abstract class MixinGTModHandler {

    @Inject(method = "isElectricItem(Lnet/minecraft/item/ItemStack;B)Z", at = @At("HEAD"), cancellable = true)
    private static void wtct$acceptAnyTierItems(final ItemStack stack, final byte tier,
        final CallbackInfoReturnable<Boolean> cir) {
        if (stack != null && stack.getItem() instanceof IAnyTierElectricItem) {
            cir.setReturnValue(true);
        }
    }
}
