package com.asdflj.wtct.nei;

import static codechicken.nei.guihook.GuiContainerManager.drawItem;

import net.minecraft.client.gui.Gui;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;

import appeng.api.AEApi;
import codechicken.nei.NEIClientUtils;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.GuiOverlayButton;

public class AEItemOverlayState extends GuiOverlayButton.ItemOverlayState {

    /**
     * "Missing, but the network holds a pattern for it" - NEI itself only knows present/absent. The value is the
     * reference WCWT's own craftable-slot blue ({@code BLUE_SLOT_HIGHLIGHT_COLOR}), so the recipe grid matches the
     * terminal the port follows instead of carrying a colour of its own.
     */
    private static final int CRAFTABLE_OVERLAY_COLOR = 0x400000FF;

    private final boolean isCraftable;

    public AEItemOverlayState(PositionedStack slot, boolean isPresent, boolean isCraftable) {
        super(slot, isPresent);
        this.isCraftable = isCraftable;
    }

    private static final ItemStack PATTERN = AEApi.instance()
        .definitions()
        .items()
        .encodedPattern()
        .maybeStack(1)
        .orNull();

    @Override
    public void draw(GuiOverlayButton.ItemOverlayFormat format) {
        if (this.isCraftable && !this.isPresent) {
            // Missing but craftable: blue instead of NEI's red. NEI only paints present/absent, so
            // this one is ours - drawn in NEI's own render context, at the ingredient's own rect.
            NEIClientUtils.gl2DRenderContext(
                () -> Gui.drawRect(
                    this.slot.relx,
                    this.slot.rely,
                    this.slot.relx + 16,
                    this.slot.rely + 16,
                    CRAFTABLE_OVERLAY_COLOR));
        } else {
            super.draw(format);
        }
        if (this.isCraftable) {
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glPushMatrix();
            GL11.glTranslatef(0, 0, 200f);
            GL11.glScalef(0.4f, 0.4f, 0.4f);
            drawItem((int) ((this.slot.relx + 10) * 2.5), (int) (this.slot.rely * 2.5), PATTERN);
            GL11.glTranslatef(0, 0, -200f);
            GL11.glPopMatrix();
            GL11.glEnable(GL11.GL_LIGHTING);
        }
    }
}
