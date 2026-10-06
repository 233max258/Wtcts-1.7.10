package com.asdflj.wtct.coremod.mixin.ae;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.asdflj.wtct.api.Pinned;
import com.asdflj.wtct.api.WtctAPI;
import com.asdflj.wtct.client.me.AdvItemRepo;
import com.asdflj.wtct.client.me.IDisplayRepoExtend;

import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IDisplayRepo;
import appeng.api.storage.data.IItemList;
import appeng.client.me.ItemRepo;

@Mixin(ItemRepo.class)
public abstract class MixinItemRepo implements IDisplayRepo, IDisplayRepoExtend {

    @Shadow(remap = false)
    @Final
    private ArrayList<IAEStack<?>> view;

    @Shadow(remap = false)
    @Final
    private IItemList<IAEStack<?>> list;

    @Shadow(remap = false)
    private boolean paused;

    private void setAsEmpty(int i) {
        this.view.add(i, null);
    }

    private final Minecraft mc = Minecraft.getMinecraft();

    @Redirect(
        method = "updateView",
        at = @At(value = "INVOKE", target = "Ljava/util/ArrayList;add(Ljava/lang/Object;)Z"),
        remap = false)
    public boolean add(ArrayList<Object> view, Object o) {
        return addView(view, o);
    }

    private boolean addView(ArrayList<Object> view, Object o) {
        GuiScreen gui = mc.currentScreen;
        if (gui == null) return view.add(o);
        if (!WtctAPI.instance()
            .terminal()
            .isPinTerminal(gui)) {
            return view.add(o);
        } else if ((o instanceof IAEItemStack is && WtctAPI.instance()
            .getPinned()
            .isPinnedItem(is))) {
                return false;
            } else {
                return view.add(o);
            }
    }

    @Redirect(
        method = "addEntriesToView",
        at = @At(value = "INVOKE", target = "Ljava/util/ArrayList;add(Ljava/lang/Object;)Z"),
        remap = false,
        require = 0)
    public boolean addEntriesToView(ArrayList<Object> view, Object o) {
        return addView(view, o);
    }

    @Inject(method = "updateView", at = @At(value = "HEAD"), remap = false)
    public void updateViewHead(CallbackInfo ci) {
        if (((Object) this) instanceof AdvItemRepo repo) {
            if (!repo.hasCache()) {
                repo.getLock()
                    .lock();
                viewFilter();
                repo.getLock()
                    .unlock();
            }
        } else {
            viewFilter();
        }
    }

    private void viewFilter() {
        List<IAEStack<?>> list = this.view.stream()
            .filter(Objects::nonNull)
            .toList();
        this.view.clear();
        this.view.addAll(list);
    }

    @Inject(method = "updateView", at = @At(value = "TAIL"), remap = false)
    public void updateViewTail(CallbackInfo ci) {
        GuiScreen gui = mc.currentScreen;
        if (gui == null) return;
        if (!WtctAPI.instance()
            .terminal()
            .isPinTerminal(gui)) {
            return;
        }
        final Pinned pinned = WtctAPI.instance()
            .getPinned();
        // Take out whatever the previous pass left at the head of the view - the pinned cells themselves
        // and the empty cells that pad the autocrafting row out - before laying the block down again. A
        // paused repo keeps its view instead of rebuilding it, so without this every refresh would file
        // another copy of the block in front of the last one.
        this.view.removeIf(stack -> stack == null || (stack instanceof IAEItemStack is && pinned.isPinnedItem(is)));
        final List<IAEItemStack> craftingPins = pinned.getPinnedItems(Pinned.PinReason.CRAFTING);
        final List<IAEItemStack> favorites = pinned.getPinnedItems(Pinned.PinReason.FAVORITE);
        if (craftingPins.isEmpty() && favorites.isEmpty()) {
            return;
        }
        // Two blocks, laid out the way the player asked for them: the autocrafting pins take a whole
        // row of their own - padded with empty cells so the ordinary items start on the next row
        // instead of continuing on the same line - and the favourites follow them cell by cell, with
        // no padding (a favourite block of three must not push the list down by a whole row).
        final int rowSize = ((ItemRepo) (Object) this).getRowSize();
        final int craftingCells = craftingPins.isEmpty() ? 0
            : Math.max(craftingPins.size(), rowSize > 0 ? rowSize : pinned.getMaxPinSize());
        int index = 0;
        for (int i = 0; i < craftingCells; i++) {
            index = this.insertPin(craftingPins, i, i < craftingPins.size(), false, index);
        }
        for (int i = 0; i < favorites.size(); i++) {
            index = this.insertPin(favorites, i, true, true, index);
        }
    }

    /**
     * Puts one pinned cell at {@code index} - the pin itself, or an empty cell for the padding that
     * fills the autocrafting row out - and answers with the index of the next cell.
     *
     * <p>
     * When the network no longer holds the pinned item, {@code zeroIfAbsent} decides what the cell
     * says: a favourite still shows its item with a zero amount - the player asked for the item to be
     * there, and "none left" is information, not absence - while an autocrafting pin falls back to an
     * empty cell, because a job whose output is gone means the pin itself is stale.
     */
    private int insertPin(List<IAEItemStack> pins, int pinIndex, boolean hasPin, boolean zeroIfAbsent, int index) {
        if (hasPin) {
            IAEStack<?> item = this.list.findPrecise(pins.get(pinIndex));
            if (item != null) {
                this.view.add(index, item);
                return index + 1;
            }
            if (zeroIfAbsent) {
                final IAEItemStack absent = pins.get(pinIndex)
                    .copy();
                absent.setStackSize(0);
                // A pin stored earlier may carry a stale craftable flag from a pattern that has since
                // been removed. Clear it: this cell is a plain "none left" display, not a craft
                // request - AE2 hands every zero-size cell to the crafting flow otherwise.
                absent.setCraftable(false);
                this.view.add(index, absent);
                return index + 1;
            }
        }
        this.setAsEmpty(index);
        return index + 1;
    }

    @Override
    public void setAdvRepoPause(boolean pause) {
        this.paused = pause;
    }
}
