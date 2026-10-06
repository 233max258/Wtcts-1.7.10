package com.asdflj.wtct.client.event;

import java.util.List;

import net.minecraft.client.gui.inventory.GuiContainer;

import com.asdflj.wtct.nei.HammerRecipeButton;

import codechicken.nei.recipe.GuiOverlayButton;
import codechicken.nei.recipe.GuiRecipeButton;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * Adds WCWT's hammer next to NEI's "+" whenever the recipe screen is opened over the comprehensive
 * work terminal with a crafting recipe on display: "+" encodes, the hammer pulls the materials into
 * the terminal's own crafting grid. NEI rebuilds its recipe-panel buttons through
 * {@link GuiRecipeButton.UpdateRecipeButtonsEvent}, and the post list is what it ends up using - so
 * appending here is the whole integration.
 */
public final class NeiHammerButtonHandler {

    @SubscribeEvent
    public void onRecipeButtons(final GuiRecipeButton.UpdateRecipeButtonsEvent.Post event) {
        final List<GuiRecipeButton> buttons = event.buttonList;
        if (buttons == null) {
            return;
        }
        for (final GuiRecipeButton button : buttons) {
            if (button instanceof GuiOverlayButton overlay) {
                if (HammerRecipeButton.appliesTo(overlay)) {
                    final GuiContainer firstGui = overlay.firstGui;
                    // Index 0: in widget mode NEI only drives the first overlay button's item overlay,
                    // and ours draws the inherited presence marks plus the red/blue tints - so being
                    // first keeps both the "+" marks and the hammer tints alive.
                    buttons.add(
                        0,
                        new HammerRecipeButton(
                            firstGui,
                            overlay.handlerRef,
                            overlay.xPosition - 13,
                            overlay.yPosition));
                }
                return;
            }
        }
    }
}
