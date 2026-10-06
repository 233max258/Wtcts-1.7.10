package com.asdflj.wtct.coremod.mixin.ae;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEStack;
import appeng.me.cache.CraftingGridCache;

/**
 * A pattern's unused output cells are null entries of {@code getAEOutputs()} - AE2 reads them that way
 * on purpose, and its own encoder never writes an entry for an unused cell. AE2 rv3-beta-977's grid
 * rebuild, however, copies every output without looking at it:
 *
 * <pre>
 * for (IAEStack&lt;?&gt; out : details.getAEOutputs()) {
 *     out = out.copy();   // NullPointerException
 * </pre>
 *
 * which takes the whole server down, and it does so on every grid rebuild - so a single such pattern
 * makes its world impossible to open. It is dropped here instead, because the rebuild only ever wants
 * the items a pattern produces; a newer AE2 does the same thing at this very place by asking for the
 * condensed outputs.
 */
@Mixin(CraftingGridCache.class)
public abstract class MixinCraftingGridCache {

    @Redirect(
        method = "setPatternsFromCraftingMethods",
        at = @At(
            value = "INVOKE",
            target = "Lappeng/api/networking/crafting/ICraftingPatternDetails;getAEOutputs()[Lappeng/api/storage/data/IAEStack;"),
        remap = false)
    private IAEStack<?>[] wtct$withoutEmptyOutputs(final ICraftingPatternDetails details) {
        final IAEStack<?>[] outputs = details.getAEOutputs();
        final List<IAEStack<?>> produced = new ArrayList<>(outputs.length);
        for (final IAEStack<?> output : outputs) {
            if (output != null) {
                produced.add(output);
            }
        }
        return produced.toArray(new IAEStack<?>[0]);
    }
}
