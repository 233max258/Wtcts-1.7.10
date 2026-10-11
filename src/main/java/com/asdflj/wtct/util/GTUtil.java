package com.asdflj.wtct.util;

import static com.asdflj.wtct.api.Constants.MessageType.UPDATE_PLAYER_ITEM;
import static com.asdflj.wtct.nei.NEI_TH_Config.getConfigValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;

import com.asdflj.wtct.Wtct;
import com.asdflj.wtct.integration.Mods;
import com.asdflj.wtct.nei.ButtonConstants;
import com.asdflj.wtct.nei.object.OrderStack;
import com.asdflj.wtct.network.SPacketMEItemInvUpdate;

import appeng.api.util.IInterfaceViewable;
import appeng.util.item.AEItemStack;
import codechicken.nei.recipe.IRecipeHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.objects.ItemData;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;
import gregtech.common.blocks.ItemMachines;
import gregtech.nei.GTNEIDefaultHandler;

public class GTUtil {

    public static String CoreModVersion = getCoreModVersion();

    public static IInterfaceViewable getIInterfaceViewable(TileEntity tile) {
        if (tile instanceof BaseMetaTileEntity bmte && bmte.getMetaTileEntity() instanceof IInterfaceViewable iv) {
            return iv;
        }
        return null;
    }

    @SideOnly(Side.CLIENT)
    public static boolean isDataStick() {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        ItemStack dataStick = player.inventory.getItemStack();
        return ItemList.Tool_DataStick.isStackEqual(dataStick, false, true);
    }

    /** The ore-dictionary prefix every hot ingot carries; the cooled one is the same name without it. */
    private static final String HOT_INGOT_ORE_PREFIX = "ingotHot";
    /** The prefix every cooled ingot carries. */
    private static final String INGOT_ORE_PREFIX = "ingot";

    /**
     * The ingot a hot one cools into, or {@code stack} itself when it is not a hot ingot this can read.
     *
     * <p>
     * Two routes, because two kinds of item carry hot ingots. GregTech's own answer through the
     * association it keeps for every metaitem it registers ({@code ingotHot} + material, which is the very
     * pair its vacuum-freezer recipe turns into {@code ingot} + material). Everything registered the other
     * way - Bartworks' werkstoffe, for one - is matched through the ore dictionary, where GregTech's
     * unifiable prefixes land under {@code <prefix><material>}: a hot ingot is <em>ingotHotTitanium</em>
     * and its cooled counterpart the same name with the "Hot" dropped. The lookup goes through
     * {@link GTOreDictUnificator#getFirstOre(Object, long)} rather than the ore dictionary directly, so
     * the answer is the metal's unified ingot and not whichever mod registered one first.
     *
     * <p>
     * Anything else is handed back untouched: no material can be invented for a stack that carries no
     * such name, and asking for a bare "ingot" would answer with the wrong metal.
     */
    public static ItemStack cooledIngot(final ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return stack;
        }
        final ItemData data = GTOreDictUnificator.getAssociation(stack);
        final boolean hotPrefix = data != null && data.mPrefix == OrePrefixes.ingotHot;
        if (hotPrefix && data.mMaterial != null && data.mMaterial.mMaterial != null) {
            final ItemStack cooled = GTOreDictUnificator
                .get(OrePrefixes.ingot, data.mMaterial.mMaterial, stack.stackSize);
            if (cooled != null) {
                return cooled;
            }
        }
        for (final int oreId : OreDictionary.getOreIDs(stack)) {
            final String ore = OreDictionary.getOreName(oreId);
            if (ore == null || !ore.startsWith(HOT_INGOT_ORE_PREFIX) || ore.length() == HOT_INGOT_ORE_PREFIX.length()) {
                continue;
            }
            final ItemStack cooled = GTOreDictUnificator
                .getFirstOre(INGOT_ORE_PREFIX + ore.substring(HOT_INGOT_ORE_PREFIX.length()), stack.stackSize);
            if (cooled != null) {
                return cooled;
            }
        }
        final ItemStack fromRecipe = cooledByVacuumFreezer(stack);
        if (fromRecipe != null) {
            return fromRecipe;
        }
        return stack;
    }

    /** GT's own hot ingot to cooled ingot pairs, read out of its vacuum freezer recipes once. */
    private static List<ItemStack[]> vacuumFreezerPairs;

    /**
     * GT's own cooling step for a hot ingot: the vacuum freezer recipe that turns it into an ingot.
     *
     * <p>
     * The third and last reading of the same question - the one that cannot be wrong about what GT
     * intends, because it is GT's own recipe list. It is also the slowest (a few hundred recipes are
     * walked once, then a list is scanned), which is why it only runs when the two name-based routes came
     * up empty: a hot ingot of a material whose cooling recipe GT generates is certain to be in it.
     */
    private static ItemStack cooledByVacuumFreezer(final ItemStack stack) {
        if (vacuumFreezerPairs == null) {
            vacuumFreezerPairs = new ArrayList<>();
            for (final GTRecipe recipe : RecipeMaps.vacuumFreezerRecipes.getAllRecipes()) {
                if (recipe.mInputs == null || recipe.mInputs.length != 1
                    || recipe.mOutputs == null
                    || recipe.mOutputs.length != 1
                    || recipe.mInputs[0] == null
                    || recipe.mOutputs[0] == null) {
                    continue;
                }
                vacuumFreezerPairs.add(
                    new ItemStack[] { GTUtility.copyAmount(1, recipe.mInputs[0]),
                        GTUtility.copyAmount(1, recipe.mOutputs[0]) });
            }
        }
        for (final ItemStack[] pair : vacuumFreezerPairs) {
            if (GTUtility.areStacksEqual(pair[0], stack, true)) {
                return GTUtility.copyAmount(stack.stackSize, pair[1]);
            }
        }
        return null;
    }

    /**
     * saw and what it answered. Only runs on an encode with the switch on, so it is silent otherwise.
     */

    public static String getRecipeName(IRecipeHandler recipe, List<OrderStack<?>> in) {
        if (recipe instanceof GTNEIDefaultHandler) {
            if (Mods.PROGRAMMABLE_HATCHES.isModLoaded()
                && getConfigValue(ButtonConstants.DUAL_INTERFACE_TERMINAL_FILL_CIRCUIT)) {
                return recipe.getRecipeName();
            }
            for (OrderStack<?> stack : in) {
                if (stack.getStack() instanceof ItemStack is && is.stackSize == 0) {
                    return getConfigValue(ButtonConstants.DUAL_INTERFACE_TERMINAL_APPEND_CIRCUIT_DAMAGE)
                        ? String.format("%s %s", recipe.getRecipeName(), is.getItemDamage())
                        : recipe.getRecipeName();
                }
            }
        }
        return recipe.getRecipeName();
    }

    public static void setDataStick(int x, int y, int z, EntityPlayer player, World w) {
        TileEntity tile = w.getTileEntity(x, y, z);
        if (tile instanceof BaseMetaTileEntity bmte && bmte.getMetaTileEntity() instanceof IInterfaceViewable) {
            ItemStack dataStick = player.inventory.getItemStack();
            if (ItemList.Tool_DataStick.isStackEqual(dataStick, false, true)) {
                NBTTagCompound tag = new NBTTagCompound();
                tag.setString("type", "CraftingInputBuffer");
                tag.setInteger("x", x);
                tag.setInteger("y", y);
                tag.setInteger("z", z);
                dataStick.setTagCompound(tag);
                dataStick
                    .setStackDisplayName("Crafting Input Buffer Link Data Stick (" + x + ", " + y + ", " + z + ")");
                player.addChatMessage(new ChatComponentText("Saved Link Data to Data Stick"));
                SPacketMEItemInvUpdate packet = new SPacketMEItemInvUpdate(UPDATE_PLAYER_ITEM);
                packet.appendItem(AEItemStack.create(dataStick));
                Wtct.proxy.netHandler.sendTo(packet, (EntityPlayerMP) player);
            }
        }
    }

    private static String getCoreModVersion() {
        Optional<ModContainer> mod = Loader.instance()
            .getActiveModList()
            .stream()
            .filter(
                x -> x.getModId()
                    .equals("dreamcraft"))
            .findFirst();
        if (mod.isPresent()) {
            return mod.get()
                .getVersion();
        }
        return "";
    }

    public static boolean isHatchItem(ItemStack item) {
        if (item != null && item.getItem() instanceof ItemMachines) {
            return ItemMachines.getMetaTileEntity(item) instanceof MTEHatch;
        }
        return false;
    }

    /*
     * @param v1
     * @param v2
     * @return 0 equals，1 left，-1 right
     */
    public static int compareVersion(String v1) {
        String v2 = "2.3.54"; // 2.6.1
        if (v1.equals(v2)) {
            return 0;
        }
        String[] version1Array = v1.split("[._]");
        String[] version2Array = v2.split("[._]");
        int index = 0;
        int minLen = Math.min(version1Array.length, version2Array.length);
        long diff = 0;

        while (index < minLen
            && (diff = Long.parseLong(version1Array[index]) - Long.parseLong(version2Array[index])) == 0) {
            index++;
        }
        if (diff == 0) {
            for (int i = index; i < version1Array.length; i++) {
                if (Long.parseLong(version1Array[i]) > 0) {
                    return 1;
                }
            }

            for (int i = index; i < version2Array.length; i++) {
                if (Long.parseLong(version2Array[i]) > 0) {
                    return -1;
                }
            }
            return 0;
        } else {
            return diff > 0 ? 1 : -1;
        }
    }
}
