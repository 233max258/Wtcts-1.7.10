package com.asdflj.wtct.client.render;

import static appeng.client.gui.AEBaseGui.aeRenderItem;

import java.util.function.Predicate;

import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.asdflj.wtct.client.gui.IGuiDrawSlot;
import com.glodblock.github.common.item.ItemFluidPacket;

import appeng.api.storage.data.IAEItemStack;

public class RenderFluidPacketPatternSlot implements ISlotRender {

    @Override
    public Predicate<Slot> get() {
        return slot -> {
            ItemStack stack = slot.getStack();
            return stack != null && stack.getItem() instanceof ItemFluidPacket;
        };
    }

    @Override
    public boolean drawSlot(Slot slot, IAEItemStack stack, IGuiDrawSlot draw, boolean display, Runnable baseDraw) {
        if (stack.getItem() instanceof ItemFluidPacket) {
            FluidStack fluidStack = ItemFluidPacket.getFluidStack(stack);
            if (fluidStack == null || fluidStack.amount <= 0) {
                return true;
            }
            // ae2fc 的 ItemFluidPacket 没有 item model（其 jar 的 models/item 为空），vanilla 路径画出来
            // 是空白，所以用流体贴图代替物品底图（与 RenderFluidDrop 画流体图标一致）。绝不能调
            // draw.getAEBaseGui().func_146977_a(slot)，那会重入 GUI 覆写 → IGuiDrawSlot 分发 → 本渲染器
            // → 无限递归 StackOverflow。
            draw.drawWidget(slot.xDisplayPosition, slot.yDisplayPosition, fluidStack.getFluid());
            IAEItemStack fake = stack.copy();
            fake.setStackSize(fluidStack.amount);
            aeRenderItem.setAeStack(fake);
            draw.renderStackSize(display, stack, slot);
            return false;
        }
        return true;
    }
}
