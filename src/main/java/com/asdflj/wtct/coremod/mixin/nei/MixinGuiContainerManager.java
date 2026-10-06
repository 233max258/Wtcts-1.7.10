package com.asdflj.wtct.coremod.mixin.nei;

import static appeng.client.gui.AEBaseGui.aeRenderItem;
import static codechicken.nei.guihook.GuiContainerManager.getStackMouseOver;
import static com.asdflj.wtct.client.render.RenderHelper.canDrawPlus;
import static com.asdflj.wtct.client.render.RenderHelper.drawPlus;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.asdflj.wtct.api.WtctAPI;
import com.asdflj.wtct.client.gui.widget.IGuiMonitor;
import com.asdflj.wtct.client.render.RenderHelper;
import com.asdflj.wtct.nei.ButtonConstants;
import com.asdflj.wtct.nei.NEI_TH_Config;
import com.asdflj.wtct.util.Ae2ReflectClient;
import com.asdflj.wtct.util.Util;
import com.glodblock.github.common.item.ItemFluidDrop;

import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IDisplayRepo;
import appeng.api.storage.data.IItemList;
import appeng.client.gui.AEBaseGui;
import appeng.client.me.ItemRepo;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;
import codechicken.nei.guihook.GuiContainerManager;
import codechicken.nei.recipe.GuiRecipe;
import codechicken.nei.recipe.StackInfo;

@Mixin(GuiContainerManager.class)
public abstract class MixinGuiContainerManager {

    @Shadow(remap = false)
    public GuiContainer window;

    private static RenderItem wtct$r = RenderHelper.itemRender;

    private static ItemStack wtct$lastStack = null;
    private static IAEItemStack wtct$lastAEStack = null;

    @Inject(
        method = "renderToolTips",
        at = @At(
            value = "INVOKE",
            target = "Lcodechicken/nei/guihook/GuiContainerManager;applyItemCountDetails(Ljava/util/List;Lnet/minecraft/item/ItemStack;)V"),
        remap = false)
    private void wtct$renderToolTips(int mousex, int mousey, CallbackInfo ci) {
        if (!NEI_TH_Config.getConfigValue(ButtonConstants.INVENTORY_STATE)) return;
        ItemStack stack;
        stack = getStackMouseOver(this.window);
        if (stack == null) return;
        boolean displayFluid = false;
        if (window instanceof GuiRecipe<?>gui) {
            IDisplayRepo repo = null;
            if (gui.getFirstScreenGeneral() instanceof IGuiMonitor g) {
                repo = g.getRepo();
                displayFluid = true;
            } else if (WtctAPI.instance()
                .terminal()
                .isTerminal(gui.getFirstScreenGeneral())) {
                    repo = Util.getDisplayRepo((AEBaseGui) gui.getFirstScreenGeneral());
                }
            if (!(repo instanceof ItemRepo)) return;
            IItemList<IAEStack<?>> list = Ae2ReflectClient.getList((ItemRepo) repo);
            FluidStack fs = StackInfo.getFluid(stack);
            if (fs != null) {
                stack = displayFluid ? ItemFluidDrop.newDisplayStack(fs) : ItemFluidDrop.newStack(fs);
            }
            IAEStack<?> found = list.findPrecise(
                wtct$lastStack != null && Platform.isSameItemPrecise(wtct$lastStack, stack) && wtct$lastAEStack != null
                    ? wtct$lastAEStack
                    : AEItemStack.create(stack));
            if (found instanceof IAEItemStack item) {
                wtct$render(item, mousex - 8, mousey - 40 < 0 ? mousey + 40 : mousey - 40);
                wtct$lastAEStack = item;
                wtct$lastStack = stack;
            }
        }

    }

    private void wtct$render(IAEItemStack item, int x, int y) {
        ItemStack stack = item.getItemStack();
        GL11.glPushMatrix();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glTranslatef(0.0f, 0.0f, 350);
        wtct$r.renderItemAndEffectIntoGUI(
            Minecraft.getMinecraft().fontRenderer,
            Minecraft.getMinecraft()
                .getTextureManager(),
            stack,
            x,
            y);
        GL11.glTranslatef(0.0f, 0.0f, 200.0f);
        aeRenderItem.setAeStack(item);
        aeRenderItem.renderItemOverlayIntoGUI(
            Minecraft.getMinecraft().fontRenderer,
            Minecraft.getMinecraft()
                .getTextureManager(),
            stack,
            x,
            y);
        GL11.glTranslatef(0.0f, 0.0f, -350.0f);
        if (item.isCraftable() && canDrawPlus) {
            GL11.glTranslatef(0.0f, 0.0f, 450.0f);
            drawPlus(x, y);
            GL11.glTranslatef(0.0f, 0.0f, -450.0f);
        }
        GL11.glPopMatrix();
    }
}
