package com.asdflj.wtct.coremod.mixin.ae;

import java.util.List;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.asdflj.wtct.api.TerminalMenu;
import com.asdflj.wtct.client.event.AEGuiCloseEvent;
import com.asdflj.wtct.client.gui.BaseMEGui;
import com.asdflj.wtct.client.render.RenderHelper;

import appeng.client.gui.AEBaseGui;
import appeng.client.gui.slots.VirtualMESlot;
import appeng.client.gui.widgets.GuiScrollbar;
import appeng.container.slot.SlotPlayerHotBar;
import appeng.container.slot.SlotPlayerInv;
import codechicken.nei.recipe.StackInfo;

@Mixin(value = AEBaseGui.class)
public abstract class MixinAEBaseGui extends GuiContainer {

    public MixinAEBaseGui(Container container) {
        super(container);
    }

    @Shadow(remap = false)
    protected abstract GuiScrollbar getScrollBar();

    @Inject(method = "handleMouseClick", at = @At(value = "HEAD"), cancellable = true)
    protected void handleMouseClick(Slot slot, int slotIdx, int ctrlDown, int mouseButton, CallbackInfo ci) {
        if (ctrlDown == 1 && mouseButton == 0
            && (slot instanceof SlotPlayerInv || slot instanceof SlotPlayerHotBar)
            && slot.getHasStack()) {
            ItemStack item = slot.getStack();
            TerminalMenu menu = new TerminalMenu();
            for (int i = 0; i < menu.getItems()
                .size(); i++) {
                ItemStack term = menu.getItems()
                    .get(i);
                if (StackInfo.equalItemAndNBT(term, item, true)) {
                    menu.openTerminal(i);
                    ci.cancel();
                    break;
                }
            }

        }
    }

    @Inject(method = "onGuiClosed", at = @At(value = "HEAD"))
    public void onGuiClosed(CallbackInfo ci) {
        MinecraftForge.EVENT_BUS.post(new AEGuiCloseEvent((AEBaseGui) (Object) this));
    }

    @Inject(method = "handleMouseInput", at = @At("HEAD"))
    public void handleMouseInput(CallbackInfo ci) {
        if (this.getScrollBar() != null) {
            if (!Mouse.isButtonDown(0)) {
                ((AccessorGuiScrollbar) this.getScrollBar()).setIsLatestClickOnScrollbar(false);
            }
        }
    }

    /**
     * Lays the pin/favourite marks down in the foreground pass, just before the virtual slots draw their
     * item icons - the place AE2's own terminals paint their pin backgrounds from. Everything drawn here
     * is therefore behind the items, which is what the marks have to be: drawn after the pass instead,
     * they sat on top of the icons and a marked cell came out as a black square while an item was held.
     */
    @Inject(method = "drawVirtualSlots", at = @At("HEAD"), remap = false)
    private void wtct$drawPinnedSlots(List<VirtualMESlot> slots, int mouseX, int mouseY, CallbackInfo ci) {
        if ((Object) this instanceof BaseMEGui gui) {
            RenderHelper.drawPinnedSlots(gui, gui.getMeSlots());
        }
    }
}
