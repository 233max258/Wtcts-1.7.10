package com.asdflj.wtct.coremod.mixin.ae;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import appeng.client.gui.widgets.GuiTabButton;

/**
 * Keeps the corner tab's transparent margin from landing on the framebuffer as opaque black.
 *
 * <p>
 * The tab plate is blitted from {@code guis/states.png} and, for the "hide edge" variant the
 * crafting-status screen puts in the top-right corner ({@code u = 11*16 = 176}), only its top seven
 * rows carry any colour - everything below is {@code (0,0,0,0)}. Those texels have to be alpha
 * blended away; with {@code GL_BLEND} off they are written as opaque black and the tab comes out
 * sitting inside a black box, which on a bright dialog like the crafting status is impossible to
 * miss.
 *
 * <p>
 * AE2's own {@code GuiTabButton} never touches the GL state, so it depends on whatever the previous
 * button left behind. That is the trap: vanilla {@code RenderItem.renderItemIntoGUI} - which every
 * icon-bearing button runs to draw its item - ends with {@code GL11.glDisable(GL_BLEND)} on its
 * normal-item path (and the opaque-block path too). Buttons are drawn in list order, so once any
 * icon button before the tab has run, blending is off by the time the plate is blitted. Enabling
 * blending around the whole screen draw is therefore not enough: it has to be re-armed for the tab
 * itself, after whoever came before had their turn.
 *
 * <p>
 * The injection therefore turns blending on at the head of the draw and puts it back the way it
 * found it at the end. Alpha test is deliberately left alone: the plate is straight alpha over
 * whatever is already in the framebuffer, which is exactly what {@code GL_BLEND} does, and turning
 * alpha test on would clip the plate's own soft edges. Also mirrored into
 * {@code CraftingStatusTabBlend} for the screens that bracket the whole draw.
 */
@Mixin(value = GuiTabButton.class, remap = false)
public abstract class MixinGuiTabButton {

    /**
     * {@code -Dwtct.debug.tab} (off in release, on for runClient/runServer).
     */
    @Unique
    private static final boolean WTCT$DEBUG = Boolean.getBoolean("wtct.debug.tab");

    /**
     * Whether blending was already on when the draw started, so it can be restored exactly.
     */
    @Unique
    private boolean wtct$blendWasEnabled;

    /**
     * Targets {@code func_146112_a} - the shipped name of {@code GuiButton.drawButton}. The method is
     * deobfuscated to {@code drawButton} in the dev workspace but ships as the SRG name, and this mixin
     * is compiled against the AE2 dev jar with {@code remap = false}, so the injection has to name the
     * shipped method rather than the readable one. Naming {@code drawButton} here silently matches
     * nothing and the injection never applies.
     */
    @Inject(method = "func_146112_a", at = @At("HEAD"), remap = false)
    private void wtct$beforeDraw(CallbackInfo ci) {
        wtct$blendWasEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        if (WTCT$DEBUG) {
            final GuiTabButton self = (GuiTabButton) (Object) this;
            System.out.println(
                "[wtct-tab] drawButton x=" + self.xPos()
                    + " y="
                    + self.yPos()
                    + " w="
                    + self.getWidth()
                    + " h="
                    + self.getHeight()
                    + " hideEdge="
                    + self.getHideEdge()
                    + " blendWasOn="
                    + wtct$blendWasEnabled);
        }
    }

    @Inject(method = "func_146112_a", at = @At("RETURN"), remap = false)
    private void wtct$afterDraw(CallbackInfo ci) {
        // Drawing the tab's item icon runs RenderItem.renderItemIntoGUI, which leaves GL_BLEND off.
        // Put the state back the way this draw found it so the buttons after the tab are not dragged
        // down with it - previously the leak was handed straight on to the next button in the list.
        if (wtct$blendWasEnabled) {
            GL11.glEnable(GL11.GL_BLEND);
        } else {
            GL11.glDisable(GL11.GL_BLEND);
        }
    }
}
