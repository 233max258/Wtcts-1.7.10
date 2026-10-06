package com.asdflj.wtct.client.render;

import java.util.function.Predicate;

import net.minecraft.inventory.Slot;

import com.asdflj.wtct.client.gui.IGuiDrawSlot;

import appeng.api.storage.data.IAEItemStack;

public interface ISlotRender {

    Predicate<Slot> get();

    /**
     * @param baseDraw vanilla 槽位绘制（GuiContainer.drawSlot 的 super 调用）。渲染器需要先画底图物品时
     *                 必须调用它，而不是回查 GUI 的 func_146977_a —— 那会重新进入 IGuiDrawSlot 分发导致无限递归
     *                 （StackOverflowError，见 RenderFluidPacketPatternSlot 修复记录）。
     */
    boolean drawSlot(Slot slot, IAEItemStack stack, IGuiDrawSlot draw, boolean display, Runnable baseDraw);

    default void drawCallback(Slot slot, IAEItemStack stack, IGuiDrawSlot draw, boolean display) {}
}
