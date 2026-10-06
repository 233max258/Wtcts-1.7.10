package com.asdflj.wtct.client.render;

import static codechicken.nei.guihook.GuiContainerManager.drawItem;

import java.util.function.Predicate;

import net.minecraft.client.Minecraft;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;

import com.asdflj.wtct.client.gui.GuiBaseInterfaceWireless;
import com.asdflj.wtct.client.gui.IGuiDrawSlot;

import appeng.api.AEApi;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.container.slot.SlotPlayerHotBar;
import appeng.container.slot.SlotPlayerInv;
import appeng.items.misc.ItemEncodedPattern;

public class RenderEncodedPattern implements ISlotRender {

    private static final ItemStack PATTERN = AEApi.instance()
        .definitions()
        .items()
        .encodedPattern()
        .maybeStack(1)
        .orNull();

    @Override
    public Predicate<Slot> get() {
        return slot -> (slot instanceof SlotPlayerInv || slot instanceof SlotPlayerHotBar) && slot.getStack()
            .getItem() instanceof ItemEncodedPattern;
    }

    @Override
    public boolean drawSlot(Slot slot, IAEItemStack stack, IGuiDrawSlot draw, boolean display, Runnable baseDraw) {
        if (draw.getAEBaseGui() instanceof GuiBaseInterfaceWireless && stack != null
            && stack.getItem() != null
            && stack.getItem() instanceof ItemEncodedPattern pattern) {
            final Minecraft mc = Minecraft.getMinecraft();
            final IAEStack<?> outputAE = pattern.getOutputAE(stack.getItemStack());
            GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_LIGHTING_BIT);
            GL11.glPushMatrix();
            if (outputAE instanceof IAEFluidStack fluidOutput) {
                // Fluid output: the ItemStack path renders ae2fc's model-less ItemFluidPacket
                // (pink/missing icon), so draw the fluid texture directly instead.
                draw.drawWidget(slot.xDisplayPosition, slot.yDisplayPosition, fluidOutput.getFluid());
            } else if (outputAE != null) {
                RenderHelper.itemRender.renderItemAndEffectIntoGUI(
                    mc.fontRenderer,
                    mc.getTextureManager(),
                    pattern.getOutput(stack.getItemStack()),
                    slot.xDisplayPosition,
                    slot.yDisplayPosition);
            }
            GL11.glDisable(GL11.GL_LIGHTING);
            if (stack.getStackSize() > 1) {
                String s1 = String.valueOf(stack.getStackSize());
                GL11.glTranslatef(0, 0, 200f);
                GL11.glDisable(GL11.GL_BLEND);
                draw.getFontRender()
                    .drawStringWithShadow(
                        s1,
                        slot.xDisplayPosition + 19
                            - 2
                            - draw.getFontRender()
                                .getStringWidth(s1),
                        slot.yDisplayPosition + 6 + 3,
                        16777215);
                GL11.glTranslatef(0, 0, -200f);
            }
            GL11.glTranslatef(0, 0, 100f);
            GL11.glScalef(0.4f, 0.4f, 0.4f);
            drawItem((int) ((slot.xDisplayPosition + 10) * 2.5), (int) (slot.yDisplayPosition * 2.5), PATTERN);
            GL11.glTranslatef(0, 0, -100f);
            RenderHelper.disableStandardItemLighting();
            GL11.glPopMatrix();
            GL11.glPopAttrib();
            return false;
        }

        return true;
    }
}
