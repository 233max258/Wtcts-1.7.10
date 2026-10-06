package com.asdflj.wtct.nei;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import com.asdflj.wtct.Wtct;
import com.asdflj.wtct.client.gui.GuiComprehensiveWorkTerminal;
import com.asdflj.wtct.network.CPacketNEIRecipe;

import appeng.container.slot.SlotCraftingMatrix;
import appeng.container.slot.SlotFakeCraftingMatrix;
import appeng.util.Platform;
import codechicken.nei.PositionedStack;
import codechicken.nei.api.IOverlayHandler;
import codechicken.nei.recipe.IRecipeHandler;

public class CraftingTransferHandler implements IOverlayHandler {

    public static final CraftingTransferHandler INSTANCE = new CraftingTransferHandler();

    @Override
    public void overlayRecipe(GuiContainer firstGui, IRecipeHandler recipe, int recipeIndex, boolean shift) {
        try {
            final List<PositionedStack> ingredients = recipe.getIngredientStacks(recipeIndex);
            if (firstGui instanceof GuiComprehensiveWorkTerminal) {
                CPacketNEIRecipe packet = new CPacketNEIRecipe(packIngredients(firstGui, ingredients));
                Wtct.proxy.netHandler.sendToServer(packet);
            }
        } catch (final Exception | Error ignored) {
            // NO-OP
        }
    }

    static NBTTagCompound packIngredients(GuiContainer gui, List<PositionedStack> ingredients) throws IOException {
        final NBTTagCompound recipe = new NBTTagCompound();
        for (final PositionedStack positionedStack : ingredients) {
            if (positionedStack.items != null && positionedStack.items.length > 0) {
                for (final Object o : gui.inventorySlots.inventorySlots) {
                    if (o instanceof SlotCraftingMatrix || o instanceof SlotFakeCraftingMatrix) {
                        Slot slot = (Slot) o;
                        if (slot.getSlotIndex() == slotIndexOf(positionedStack)) {
                            final NBTTagList tags = new NBTTagList();
                            for (final ItemStack is : orderedVariants(positionedStack)) {
                                final NBTTagCompound tag = new NBTTagCompound();
                                is.writeToNBT(tag);
                                tags.appendTag(tag);
                            }
                            recipe.setTag("#" + slot.getSlotIndex(), tags);
                            break;
                        }
                    }
                }
            }
        }
        return recipe;
    }

    /**
     * NEI draws its ingredients relative to the recipe panel; the crafting grid cell an ingredient
     * belongs to is the one the server writes when it pulls the recipe. Both the pull and the
     * auto-fill watch have to agree on that mapping, so it lives here.
     */
    public static int slotIndexOf(final PositionedStack positionedStack) {
        final int col = (positionedStack.relx - 25) / 18;
        final int row = (positionedStack.rely - 6) / 18;
        return col + row * 3;
    }

    /** The ingredient's accepted variants, pure crystals first - the order the pull tries them in. */
    public static List<ItemStack> orderedVariants(final PositionedStack positionedStack) {
        final List<ItemStack> list = new LinkedList<>();
        for (final ItemStack variant : positionedStack.items) {
            if (Platform.isRecipePrioritized(variant)) {
                list.add(0, variant);
            } else {
                list.add(variant);
            }
        }
        return list;
    }
}
