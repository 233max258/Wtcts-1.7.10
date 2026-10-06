package com.asdflj.wtct.client.gui;

import org.lwjgl.opengl.GL11;

/**
 * Keeps the corner tab on the crafting-status screens from coming out as a black box.
 *
 * <p>
 * AE2's {@code GuiTabButton} blits its plate out of {@code guis/states.png} and never touches the GL
 * state, so it inherits whatever the GUI's draw loop last set. The plate's corners and a slim margin
 * around it are transparent, and a resource pack's replacement sheet makes that margin wider than the
 * stock one; if alpha blending is off when the plate is blitted, those texels are written to the
 * framebuffer as their stored colour - opaque black - and the tab ends up ringed by a black frame.
 * On the crafting status screen the tab also hangs off the bright dialog panel, which is what makes
 * it so conspicuous.
 *
 * <p>
 * The whole screen is wrapped rather than the single widget because the plate is one of several draws
 * in the button pass, and the state has to hold for all of them; the previous state is restored at the
 * end so the rest of the HUD is unaffected.
 */
final class CraftingStatusTabBlend {

    private CraftingStatusTabBlend() {}

    /** Enables straight-alpha blending; returns whether it was already on, for {@link #restore}. */
    static boolean enable() {
        final boolean wasEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        return wasEnabled;
    }

    static void restore(final boolean wasEnabled) {
        if (!wasEnabled) {
            GL11.glDisable(GL11.GL_BLEND);
        }
    }
}
