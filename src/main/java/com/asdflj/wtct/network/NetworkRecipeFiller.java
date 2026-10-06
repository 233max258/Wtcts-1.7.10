package com.asdflj.wtct.network;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.oredict.OreDictionary;

import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.container.ContainerNull;
import appeng.items.storage.ItemViewCell;
import appeng.util.InventoryAdaptor;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;
import appeng.util.prioitylist.IPartitionList;

/**
 * The server half of NEI's "hammer": pulls a recipe's materials out of the network (then the
 * player's inventory) into a crafting grid, one per cell. Shared by the crafting terminal and the
 * comprehensive work terminal's manual grid.
 *
 * <p>
 * {@code auto} (the Ctrl+hammer of WCWT/ae2helpers) additionally reports the slots that stayed
 * empty to the {@link AutoFillSink}, so the container can ask the network to craft what is missing.
 * The cells themselves are then filled by the terminal's client-side watch - ae2helpers'
 * {@code AutoCraftingWatcher}, the same behaviour on modern versions.
 */
public final class NetworkRecipeFiller {

    private NetworkRecipeFiller() {}

    /** Receives the slots a Ctrl+hammer could not fill; {@code null} disables the watching. */
    public interface AutoFillSink {

        void registerPending(int slot, ItemStack[] variants);
    }

    public static void fillFromRecipe(final EntityPlayer player, final IInventory craftMatrix,
        final IEnergySource energy, final BaseActionSource actionSrc, final IMEMonitor<IAEItemStack> storage,
        final ItemStack[] viewCells, final boolean real, final ItemStack[][] recipe, final AutoFillSink sink) {
        if (recipe == null || craftMatrix == null || storage == null) {
            return;
        }
        final EntityPlayerMP pmp = (EntityPlayerMP) player;
        final InventoryAdaptor playerInv = InventoryAdaptor.getAdaptor(player, ForgeDirection.UNKNOWN);
        final Actionable realForFake = real ? Actionable.MODULATE : Actionable.SIMULATE;

        final InventoryCrafting testInv = new InventoryCrafting(new ContainerNull(), 3, 3);
        for (int x = 0; x < 9; x++) {
            if (recipe[x] != null && recipe[x].length > 0) {
                testInv.setInventorySlotContents(x, recipe[x][0]);
            }
        }

        final IRecipe r = Platform.findMatchingRecipe(testInv, pmp.worldObj);
        if (r == null) {
            return;
        }
        final ItemStack is = r.getCraftingResult(testInv);
        if (is == null) {
            return;
        }

        final IItemList all = storage.getStorageList();
        final IPartitionList<IAEItemStack> filter = ItemViewCell.createFilter(viewCells);

        for (int x = 0; x < craftMatrix.getSizeInventory(); x++) {
            final ItemStack patternItem = testInv.getStackInSlot(x);

            ItemStack currentItem = craftMatrix.getStackInSlot(x);
            if (currentItem != null) {
                testInv.setInventorySlotContents(x, currentItem);
                final ItemStack newItemStack = r.matches(testInv, pmp.worldObj) ? r.getCraftingResult(testInv) : null;
                testInv.setInventorySlotContents(x, patternItem);

                if (newItemStack == null || !Platform.isSameItemPrecise(newItemStack, is)) {
                    // The cell holds something the recipe does not want any more: back to the network.
                    final IAEItemStack in = AEItemStack.create(currentItem);
                    if (in != null) {
                        final IAEItemStack out = realForFake == Actionable.SIMULATE ? null
                            : Platform.poweredInsert(energy, storage, in, actionSrc);
                        if (out != null) {
                            craftMatrix.setInventorySlotContents(x, out.getItemStack());
                        } else {
                            craftMatrix.setInventorySlotContents(x, null);
                        }
                        currentItem = craftMatrix.getStackInSlot(x);
                    }
                }
            }

            // True if we need to fetch an item for the recipe
            if (patternItem != null && currentItem == null) {
                // Grab from network by recipe
                ItemStack whichItem = Platform.extractItemsByRecipe(
                    energy,
                    actionSrc,
                    storage,
                    player.worldObj,
                    r,
                    is,
                    testInv,
                    patternItem,
                    x,
                    all,
                    realForFake,
                    filter);

                // If that doesn't get it, grab exact items from network
                if (whichItem == null) {
                    for (int y = 0; y < recipe[x].length; y++) {
                        final IAEItemStack request = AEItemStack.create(recipe[x][y]);
                        if (request != null) {
                            if (filter == null || filter.isListed(request)) {
                                request.setStackSize(1);
                                final IAEItemStack out = Platform
                                    .poweredExtraction(energy, storage, request, actionSrc);
                                if (out != null) {
                                    whichItem = out.getItemStack();
                                    break;
                                }
                            }
                        }
                    }
                }

                // If that doesn't work, grab from the player's inventory
                if (whichItem == null && playerInv != null) {
                    whichItem = extractItemFromPlayerInventory(playerInv, realForFake, patternItem);
                }

                craftMatrix.setInventorySlotContents(x, whichItem);

                // Ctrl+hammer: the cells that stayed empty are reported so the container can ask
                // the network to make their ingredients. No craftability gate here - whether a
                // pattern exists is settled against the crafting cache, which is the only place
                // that knows, and which NBT the pattern expects.
                if (whichItem == null && sink != null && real) {
                    sink.registerPending(x, recipe[x]);
                }
            }
        }
    }

    private static ItemStack extractItemFromPlayerInventory(final InventoryAdaptor ia, final Actionable mode,
        final ItemStack patternItem) {
        final AEItemStack request = AEItemStack.create(patternItem);
        if (request == null) {
            return null;
        }
        final boolean isSimulated = mode == Actionable.SIMULATE;
        final boolean checkFuzzy = request.isOre() || patternItem.getItemDamage() == OreDictionary.WILDCARD_VALUE
            || patternItem.hasTagCompound()
            || patternItem.isItemStackDamageable();

        if (!checkFuzzy) {
            if (isSimulated) {
                return ia.simulateRemove(1, patternItem, null);
            } else {
                return ia.removeItems(1, patternItem, null);
            }
        } else {
            if (isSimulated) {
                return ia.simulateSimilarRemove(1, patternItem, FuzzyMode.IGNORE_ALL, null);
            } else {
                return ia.removeSimilarItems(1, patternItem, FuzzyMode.IGNORE_ALL, null);
            }
        }
    }
}
