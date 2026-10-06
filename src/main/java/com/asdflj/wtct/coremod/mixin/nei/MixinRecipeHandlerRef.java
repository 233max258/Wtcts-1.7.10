package com.asdflj.wtct.coremod.mixin.nei;

import net.minecraft.client.gui.inventory.GuiContainer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.asdflj.wtct.client.gui.GuiComprehensiveWorkTerminal;
import com.asdflj.wtct.nei.PatternTerminalRecipeTransferHandler;

import codechicken.nei.api.IOverlayHandler;
import codechicken.nei.recipe.RecipeHandlerRef;

/**
 * NEI's overlay lookup funnels through {@code RecipeHandlerRef#getOverlayHandler(GuiContainer)} before it decides
 * whether the "?" overlay button can be used at all. A null result makes {@code canFillCraftingGrid()} false and the
 * overlay panel shows "Mismatch Crafting Grid" for recipes that this mod can actually transfer (any recipe without a
 * pre-registered identifier, e.g. machine recipes).
 *
 * Intercept the single choke point and fall back to this mod's handler for the pattern writing GUIs.
 */
@Mixin(RecipeHandlerRef.class)
public abstract class MixinRecipeHandlerRef {

    private static final Logger WTCT_NEI_LOG = LogManager.getLogger("Wtct|NEIOverlay");

    @Inject(method = "getOverlayHandler", at = @At("RETURN"), cancellable = true, remap = false)
    private void wtct$fallbackOverlayHandler(GuiContainer gui, CallbackInfoReturnable<IOverlayHandler> cir) {
        final IOverlayHandler current = cir.getReturnValue();
        WTCT_NEI_LOG.info(
            "[Wtct] NEI overlay handler lookup: gui={} handler={}",
            gui == null ? "null"
                : gui.getClass()
                    .getName(),
            current == null ? "null"
                : current.getClass()
                    .getName());
        if (current != null) return;
        if (gui instanceof GuiComprehensiveWorkTerminal) {
            WTCT_NEI_LOG.info(
                "[Wtct] NEI overlay handler fallback applied for {}",
                gui.getClass()
                    .getName());
            cir.setReturnValue(PatternTerminalRecipeTransferHandler.INSTANCE);
        }
    }
}
