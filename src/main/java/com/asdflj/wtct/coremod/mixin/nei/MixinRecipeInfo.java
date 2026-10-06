package com.asdflj.wtct.coremod.mixin.nei;

import net.minecraft.client.gui.inventory.GuiContainer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.asdflj.wtct.client.gui.GuiComprehensiveWorkTerminal;
import com.asdflj.wtct.nei.PatternTerminalRecipeTransferHandler;

import codechicken.nei.api.IOverlayHandler;
import codechicken.nei.recipe.RecipeInfo;

/**
 * NEI resolves overlay handlers by (GUI class, recipe identifier), and {@code NEI_TH_Config} can only register the
 * identifiers that are known at load time. Any other recipe type - GT machine recipes such as the block cutter, for
 * example - ends up without a handler, so NEI falls back to filling a crafting grid and shows
 * "Mismatch Crafting Grid" instead of writing a processing pattern into the terminal.
 *
 * Fall back to this mod's pattern transfer handler for the GUIs that write patterns; it already supports arbitrary
 * recipes through {@code FluidRecipe}'s generic fallback path.
 */
@Mixin(RecipeInfo.class)
public abstract class MixinRecipeInfo {

    @Inject(method = "getOverlayHandler", at = @At("RETURN"), cancellable = true, remap = false)
    private static void wtct$fallbackOverlayHandler(GuiContainer gui, String identifier,
        CallbackInfoReturnable<IOverlayHandler> cir) {
        if (cir.getReturnValue() != null) return;
        if (gui instanceof GuiComprehensiveWorkTerminal) {
            cir.setReturnValue(PatternTerminalRecipeTransferHandler.INSTANCE);
        }
    }
}
