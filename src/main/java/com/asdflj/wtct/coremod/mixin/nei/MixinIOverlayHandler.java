package com.asdflj.wtct.coremod.mixin.nei;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import org.spongepowered.asm.mixin.Mixin;

import com.asdflj.wtct.api.WtctAPI;
import com.asdflj.wtct.client.gui.widget.IGuiMonitor;
import com.asdflj.wtct.nei.AEItemOverlayState;
import com.asdflj.wtct.util.Ae2ReflectClient;
import com.asdflj.wtct.util.Util;
import com.glodblock.github.common.item.ItemFluidDrop;

import appeng.api.config.FuzzyMode;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IDisplayRepo;
import appeng.api.storage.data.IItemList;
import appeng.client.gui.AEBaseGui;
import appeng.client.me.ItemRepo;
import appeng.util.item.AEItemStack;
import codechicken.nei.PositionedStack;
import codechicken.nei.api.IOverlayHandler;
import codechicken.nei.recipe.GuiOverlayButton;
import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.StackInfo;

@Mixin(value = IOverlayHandler.class)
public interface MixinIOverlayHandler extends IOverlayHandler {

    @Override
    default List<GuiOverlayButton.ItemOverlayState> presenceOverlay(GuiContainer firstGui, IRecipeHandler recipe,
        int recipeIndex) {
        final List<GuiOverlayButton.ItemOverlayState> itemPresenceSlots = new ArrayList<>();
        final List<PositionedStack> ingredients = recipe.getIngredientStacks(recipeIndex);
        IItemList<IAEStack<?>> list = null;
        boolean displayFluid = false;
        if (firstGui instanceof IGuiMonitor gm) {
            list = Ae2ReflectClient.getList(gm.getRepo());
            displayFluid = true;
        } else if (WtctAPI.instance()
            .terminal()
            .isTerminal(firstGui)) {
                IDisplayRepo repo = Util.getDisplayRepo((AEBaseGui) firstGui);
                if (repo instanceof ItemRepo) {
                    list = Ae2ReflectClient.getList((ItemRepo) repo);
                }
            }
        final List<ItemStack> invStacks = firstGui.inventorySlots.inventorySlots.stream()
            .filter(
                s -> s != null && s.getStack() != null
                    && s.getStack().stackSize > 0
                    && s.isItemValid(s.getStack())
                    && s.canTakeStack(firstGui.mc.thePlayer))
            .map(
                s -> s.getStack()
                    .copy())
            .collect(Collectors.toCollection(ArrayList::new));

        // What the network still has to hand out, spent cell by cell the way the inventory below is spent:
        // "however much is stored" is how many cells it fills, not a yes/no per item. A recipe that wants
        // nine and finds four paints four cells and leaves the other five missing - and the missing line
        // counts those five, because the ingredients are asked one at a time and each takes what it needs.
        final Map<IAEStack<?>, Long> stock = new HashMap<>();

        for (PositionedStack stack : ingredients) {
            final int needed = Math.max(1, stack.items[0].stackSize);
            int filled = 0;
            Optional<ItemStack> used = invStacks.stream()
                .filter(is -> is.stackSize > 0 && stack.contains(is))
                .findAny();
            if (used.isPresent()) {
                ItemStack is = used.get();
                filled = Math.min(needed, is.stackSize);
                is.stackSize -= filled;
            }
            if (filled >= needed) {
                itemPresenceSlots.add(new GuiOverlayButton.ItemOverlayState(stack, true));
                continue;
            }
            if (list == null) {
                itemPresenceSlots.add(new GuiOverlayButton.ItemOverlayState(stack, false));
                continue;
            }
            boolean isCraftable = false;
            FluidStack fs = StackInfo.getFluid(stack.item);
            IAEItemStack item;
            if (fs != null) {
                item = displayFluid ? AEItemStack.create(ItemFluidDrop.newDisplayStack(fs))
                    : ItemFluidDrop.newAeStack(fs);
            } else {
                item = AEItemStack.create(stack.item);
            }
            // The entries the network offers for this ingredient: the precise one, or - for a stack the
            // list only holds as a wildcard - every fuzzy match the cell accepts.
            final List<IAEStack<?>> candidates = new ArrayList<>();
            final IAEStack<?> precise = list.findPrecise(item);
            if (precise != null) {
                // A craftable-only entry stores nothing: it is craftable, not present. Counting it
                // present made NEI paint every craftable cell green.
                candidates.add(precise);
                isCraftable = precise.isCraftable();
            } else if (fs == null) {
                for (IAEStack<?> is : list.findFuzzy(item, FuzzyMode.IGNORE_ALL)) {
                    if (is instanceof IAEItemStack ais && ais.getStackSize() > 0
                        && stack.contains(ais.getItemStack())) {
                        candidates.add(is);
                        if (is.isCraftable()) {
                            isCraftable = true;
                        }
                    }
                }
            }
            for (final IAEStack<?> candidate : candidates) {
                if (filled >= needed) {
                    break;
                }
                final long left = stock.computeIfAbsent(candidate, entry -> Math.max(0L, entry.getStackSize()));
                final long taken = Math.min(needed - filled, left);
                stock.put(candidate, left - taken);
                filled += taken;
            }
            itemPresenceSlots.add(new AEItemOverlayState(stack, filled >= needed, isCraftable));
        }

        return itemPresenceSlots;
    }
}
