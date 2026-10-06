package com.asdflj.wtct.coremod.mixin.ae;

import net.minecraft.client.gui.inventory.GuiContainer;

import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.asdflj.wtct.client.gui.SearchFill;

import appeng.client.gui.implementations.GuiMEMonitorable;
import appeng.client.gui.widgets.MEGuiTextField;

/**
 * Gives AE2's own terminals the F gesture this mod's terminals have: fill the search box with the name
 * of the stack under the cursor.
 *
 * <p>
 * AE2 Auto Pattern Upload (1.12.2) is a mixin on exactly this class, and a player standing at a plain
 * ME terminal should not have to remember which terminal does and which does not. This mod's own
 * terminals do it in {@code GuiMonitor#keyTyped}; this is the same gesture for the stock one.
 *
 * <p>
 * AE2's search box filters as it changes - {@code setText} fires the field's own {@code onTextChange},
 * which pushes the text into the repo and refreshes it - so writing the text is the whole update here.
 */
@Mixin(GuiMEMonitorable.class)
public abstract class MixinGuiMEMonitorable {

    @Shadow(remap = false)
    protected MEGuiTextField searchField;

    @Inject(method = "keyTyped", at = @At(value = "HEAD"), cancellable = true)
    private void wtct$fillSearchFromHovered(final char character, final int key, final CallbackInfo ci) {
        if (key != Keyboard.KEY_F || SearchFill.modifierDown()) {
            return;
        }
        final String hovered = SearchFill.hoveredName((GuiContainer) (Object) this);
        if (hovered == null) {
            return;
        }
        this.searchField.setText(hovered);
        this.searchField.setCursorPositionEnd();
        ci.cancel();
    }
}
