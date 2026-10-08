package com.asdflj.wtct.client.render;

import java.util.function.Predicate;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;

import com.asdflj.wtct.client.gui.IGuiDrawSlot;

import appeng.api.storage.data.IAEItemStack;

public interface ISlotRender {

    Predicate<Slot> get();

    /**
     * True for a slot that belongs to the player's own inventory rather than to a terminal.
     *
     * <p>
     * Everything in there is a stack the player is actually carrying, so the cell has to show the item
     * itself - a renderer that swaps the item's sprite for the fluid it stands for (or for nothing at
     * all) would leave the player holding something they cannot see. The inventory backs it, not the
     * slot's class: the player's own cells reach parity through several slot types (main rows, hotbar,
     * armour, and the offhand cell this mod adds), and AE2's own player-side flag is not set on the
     * last two. Matching the inventory catches all of them, and nothing else shares it.
     */
    static boolean isPlayerSlot(final Slot slot) {
        return slot != null && slot.inventory instanceof InventoryPlayer;
    }

    /**
     * @param baseDraw vanilla 槽位绘制（GuiContainer.drawSlot 的 super 调用）。渲染器需要先画底图物品时
     *                 必须调用它，而不是回查 GUI 的 func_146977_a —— 那会重新进入 IGuiDrawSlot 分发导致无限递归
     *                 （StackOverflowError，见 RenderFluidPacketPatternSlot 修复记录）。
     */
    boolean drawSlot(Slot slot, IAEItemStack stack, IGuiDrawSlot draw, boolean display, Runnable baseDraw);

    default void drawCallback(Slot slot, IAEItemStack stack, IGuiDrawSlot draw, boolean display) {}
}
