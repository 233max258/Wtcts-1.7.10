package com.asdflj.wtct.common.item;

import static net.minecraft.client.gui.GuiScreen.isShiftKeyDown;

import java.util.List;

import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;

import com.asdflj.wtct.Wtct;
import com.asdflj.wtct.api.WirelessObject;
import com.asdflj.wtct.common.item.card.ItemWcwtUpgradeCard;
import com.asdflj.wtct.common.tabs.WtctTabs;
import com.asdflj.wtct.inventory.gui.GuiType;
import com.asdflj.wtct.inventory.item.IItemInventory;
import com.asdflj.wtct.inventory.item.WirelessDualInterfaceTerminalInventory;
import com.asdflj.wtct.loader.IRegister;
import com.asdflj.wtct.util.NameConst;

import appeng.api.AEApi;
import appeng.api.exceptions.AppEngException;
import appeng.core.localization.PlayerMessages;
import cpw.mods.fml.common.registry.GameRegistry;

/**
 * ME Comprehensive Work Terminal: a standalone wireless terminal modeled after AE2-WCWT (1.21.1,
 * MIT). Reuses the dual-interface terminal inventory host (pattern encoding + caches) rendered in
 * the WCWT fixed layout.
 */
public class ItemComprehensiveWorkTerminal extends ItemBaseWirelessTerminal
    implements IItemInventory, IRegister<ItemComprehensiveWorkTerminal> {

    public ItemComprehensiveWorkTerminal() {
        AEApi.instance()
            .registries()
            .wireless()
            .registerWirelessHandler(this);
        setUnlocalizedName(NameConst.ITEM_COMPREHENSIVE_WORK_TERMINAL);
        // The art is the 1.21 terminal's screen in its resting frame - that strip is an animation, but
        // the icon is a plain still here: 1.7.10 has no per-frame timing for item icons and blending
        // frames by hand only ever looked busier than the still does.
        setTextureName(
            Wtct.resource(NameConst.ITEM_COMPREHENSIVE_WORK_TERMINAL)
                .toString());
    }

    @Override
    protected GuiType guiGuiType(ItemStack item) {
        return GuiType.COMPREHENSIVE_WORK_TERMINAL;
    }

    /**
     * 能源卡：每装一张，终端的电力上限 ×10（最多 5 张 → ×100000）。
     *
     * <p>
     * The terminal runs off its own battery (see {@code WirelessTerminal#extractAEPower}, which pulls from the item's
     * {@code IAEItemPowerStorage}), so raising {@code getAEMaxPower} is exactly what lets the terminal keep working
     * longer between charges - and what lets a full battery last through heavier work.
     */
    @Override
    public double getAEMaxPower(final ItemStack is) {
        return super.getAEMaxPower(is) * ItemWcwtUpgradeCard.energyPowerMultiplier(is);
    }

    @Override
    public Object getInventory(ItemStack stack, World world, int x, int y, int z, EntityPlayer player) {
        try {
            return new WirelessObject(stack, world, x, y, z, player)
                .getInventory(WirelessDualInterfaceTerminalInventory.class);
        } catch (AppEngException e) {
            // The terminal's own verdict - already localized, and not always a range problem.
            player.addChatMessage(new ChatComponentText(e.getMessage()));
            return null;
        } catch (Exception e) {
            // Anything else is a defect, not a range problem; do not mislabel it, and leave a trace.
            e.printStackTrace();
            player.addChatMessage(PlayerMessages.OutOfRange.get());
            return null;
        }
    }

    @Override
    public void addCheckedInformation(ItemStack stack, EntityPlayer player, List<String> toolTip,
        boolean displayMoreInfo) {
        super.addCheckedInformation(stack, player, toolTip, displayMoreInfo);
        if (isShiftKeyDown()) {
            toolTip.add(I18n.format(NameConst.TT_COMPREHENSIVE_TERMINAL_DESC));
        } else {
            toolTip.add(I18n.format(NameConst.TT_SHIFT_FOR_MORE));
        }
    }

    @Override
    public ItemComprehensiveWorkTerminal register() {
        GameRegistry.registerItem(this, NameConst.ITEM_COMPREHENSIVE_WORK_TERMINAL, Wtct.MODID);
        setCreativeTab(WtctTabs.INSTANCE);
        return this;
    }
}
