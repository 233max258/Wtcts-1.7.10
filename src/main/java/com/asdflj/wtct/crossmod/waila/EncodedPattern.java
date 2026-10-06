package com.asdflj.wtct.crossmod.waila;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StringUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.asdflj.wtct.util.NameConst;
import com.asdflj.wtct.util.Util;
import com.glodblock.github.common.item.ItemFluidDrop;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEItemStack;
import appeng.core.localization.GuiText;
import appeng.helpers.PatternHelper;
import appeng.helpers.UltimatePatternHelper;
import appeng.items.misc.ItemEncodedPattern;
import appeng.items.misc.ItemEncodedUltimatePattern;

public class EncodedPattern extends mcp.mobius.waila.handlers.nei.TooltipHandlerWaila {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Logger WTCT_PATTERN_LOG = LogManager.getLogger("Wtct|PatternDiag");

    @Override
    public List<String> handleItemTooltip(GuiContainer gui, ItemStack stack, int x, int y, List<String> lines) {
        if (stack != null && stack.getItem() != null && stack.getItem() instanceof ItemEncodedPattern pattern) {
            String lastLine = lines.get(lines.size() - 1);
            String firstLine = lines.get(0);
            lines.clear();
            lines.add(firstLine);
            final EntityPlayer player = mc.thePlayer;
            final NBTTagCompound encodedValue = stack.getTagCompound();

            if (encodedValue == null) {
                lines.add(EnumChatFormatting.RED + GuiText.InvalidPattern.getLocal());
                lines.add(lastLine);
                return lines;
            }
            final ICraftingPatternDetails details = pattern.getPatternForItem(stack, player.worldObj);
            final boolean substitute = encodedValue.getBoolean("substitute");
            final boolean beSubstitute = encodedValue.getBoolean("beSubstitute");
            final String author = encodedValue.getString("author");
            final boolean isCrafting = encodedValue.getBoolean("crafting");
            IAEItemStack[] inItems;
            IAEItemStack[] outItems;

            if (details == null) {
                // 【诊断】AE2 判定样板无效, 重新解析一次拿到确切异常和 NBT, 定位「无效样板」问题后可移除
                if (pattern instanceof ItemEncodedUltimatePattern) {
                    try {
                        new UltimatePatternHelper(stack);
                        WTCT_PATTERN_LOG.warn("[Wtct] UltimatePattern re-parse unexpectedly succeeded: {}", stack);
                    } catch (Throwable t) {
                        WTCT_PATTERN_LOG.warn("[Wtct] UltimatePattern parse failed. NBT={}", encodedValue, t);
                    }
                } else {
                    try {
                        new PatternHelper(stack, player.worldObj);
                        WTCT_PATTERN_LOG.warn("[Wtct] PatternHelper re-parse unexpectedly succeeded: {}", stack);
                    } catch (Throwable t) {
                        WTCT_PATTERN_LOG.warn("[Wtct] PatternHelper parse failed. NBT={}", encodedValue, t);
                    }
                }
                final ItemStack unknownItem = new ItemStack(Blocks.fire);
                unknownItem.setStackDisplayName(GuiText.UnknownItem.getLocal());
                inItems = PatternHelper.convertToCondensedList(
                    PatternHelper.loadIAEItemStackFromNBT(encodedValue.getTagList("in", 10), false, unknownItem));
                outItems = PatternHelper.convertToCondensedList(
                    PatternHelper.loadIAEItemStackFromNBT(encodedValue.getTagList("out", 10), false, unknownItem));
            } else {
                inItems = details.getCondensedInputs();
                outItems = details.getCondensedOutputs();
            }

            boolean recipeIsBroken = details == null;
            final List<String> in = new ArrayList<>();
            final List<String> out = new ArrayList<>();

            final String substitutionLabel = EnumChatFormatting.YELLOW + GuiText.Substitute.getLocal()
                + " "
                + EnumChatFormatting.RESET;
            final String beSubstitutionLabel = EnumChatFormatting.YELLOW + GuiText.BeSubstitute.getLocal()
                + " "
                + EnumChatFormatting.RESET;
            final String canSubstitute = substitute ? GuiText.Yes.getLocal() : GuiText.No.getLocal();
            final String canBeSubstitute = beSubstitute ? GuiText.Yes.getLocal() : GuiText.No.getLocal();
            final String label = (isCrafting ? GuiText.Crafts.getLocal() : GuiText.Creates.getLocal());
            final String with = GuiText.With.getLocal();
            final String result = (EnumChatFormatting.DARK_AQUA + label) + ": " + EnumChatFormatting.RESET;
            final String ingredients = (EnumChatFormatting.DARK_GREEN + with) + ": " + EnumChatFormatting.RESET;
            final String holdShift = I18n.format(NameConst.TT_SHIFT_FOR_MORE) + EnumChatFormatting.RESET;

            recipeIsBroken = addInformation(inItems, in, ingredients, EnumChatFormatting.GREEN) || recipeIsBroken;
            recipeIsBroken = addInformation(outItems, out, result, EnumChatFormatting.AQUA) || recipeIsBroken;

            if (recipeIsBroken) {
                lines.add(EnumChatFormatting.RED + GuiText.InvalidPattern.getLocal());
            } else {
                lines.addAll(out);
                if (GuiScreen.isShiftKeyDown()) {
                    lines.addAll(in);
                } else {
                    lines.add(holdShift);
                }

                lines.add(substitutionLabel + canSubstitute);
                lines.add(beSubstitutionLabel + canBeSubstitute);

                if (!StringUtils.isNullOrEmpty(author)) {
                    lines.add(
                        EnumChatFormatting.LIGHT_PURPLE + GuiText.EncodedBy.getLocal(author)
                            + EnumChatFormatting.RESET);
                }
            }
            lines.add(lastLine);
        }
        return lines;
    }

    private boolean addInformation(final IAEItemStack[] items, final List<String> lines, String label,
        EnumChatFormatting color) {
        final ItemStack unknownItem = new ItemStack(Blocks.fire);
        boolean recipeIsBroken = false;
        boolean first = true;
        List<IAEItemStack> sortedItems = new ArrayList<>(items.length);
        for (IAEItemStack item : items) {
            sortedItems.add(item);
        }
        sortedItems.sort(
            Comparator.comparingLong(IAEItemStack::getStackSize)
                .reversed());
        boolean isFluid = false;

        for (final IAEItemStack item : sortedItems) {

            if (!recipeIsBroken && item.equals(unknownItem)) {
                recipeIsBroken = true;
            }

            if (item.getItemStack()
                .getItem() instanceof ItemFluidDrop) {
                label = EnumChatFormatting.GOLD + label;
                color = EnumChatFormatting.GOLD;
                isFluid = true;
            }

            if (first) {
                lines.add(label);
                lines.add(
                    "   " + EnumChatFormatting.WHITE
                        + NumberFormat.getNumberInstance(Locale.US)
                            .format(item.getStackSize())
                        + EnumChatFormatting.RESET
                        + (isFluid ? EnumChatFormatting.WHITE + "L " : " ")
                        + EnumChatFormatting.RESET
                        + color
                        + Util.getDisplayName(item));
            }
            if (!first) {
                lines.add(
                    "   " + EnumChatFormatting.WHITE
                        + NumberFormat.getNumberInstance(Locale.US)
                            .format(item.getStackSize())
                        + EnumChatFormatting.RESET
                        + (isFluid ? EnumChatFormatting.WHITE + "L " : " ")
                        + EnumChatFormatting.RESET
                        + color
                        + Util.getDisplayName(item));
            }

            first = false;
        }

        return recipeIsBroken;
    }

}
