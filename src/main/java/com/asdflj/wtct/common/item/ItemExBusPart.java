package com.asdflj.wtct.common.item;

import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import com.asdflj.wtct.Wtct;
import com.asdflj.wtct.common.parts.PartExExportBus;
import com.asdflj.wtct.common.parts.PartExImportBus;
import com.asdflj.wtct.common.parts.PartExStorageBus;
import com.asdflj.wtct.common.tabs.WtctTabs;
import com.asdflj.wtct.loader.IRegister;
import com.asdflj.wtct.loader.ItemAndBlockHolder;
import com.asdflj.wtct.util.NameConst;

import appeng.api.AEApi;
import appeng.api.config.Upgrades;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartItem;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The item both ME extended buses are placed from - one item with two sub-types, the way AE2 ships its own buses.
 *
 * <p>
 * It follows the recipe AE2's {@code ItemMultiPart} demonstrates in {@link IPartItem}: tell the part helper to render
 * the item with the bus renderer, take the item's icon from the block atlas, hand placement to the part helper and
 * build
 * the part from the stack's damage value.
 */
public class ItemExBusPart extends BaseItem implements IPartItem, IRegister<ItemExBusPart> {

    public static final int META_IMPORT_BUS = 0;
    public static final int META_EXPORT_BUS = 1;
    public static final int META_STORAGE_BUS = 2;

    @SideOnly(Side.CLIENT)
    private IIcon importIcon;
    @SideOnly(Side.CLIENT)
    private IIcon exportIcon;
    @SideOnly(Side.CLIENT)
    private IIcon storageIcon;

    public ItemExBusPart() {
        AEApi.instance()
            .partHelper()
            .setItemBusRenderer(this);
        this.setHasSubtypes(true);
        this.setUnlocalizedName(NameConst.ITEM_PART_EX_BUS);
    }

    /** The filter grid tops out at five capacity cards - one more row of nine per card. */
    public static void registerUpgrades() {
        final ItemStack importBus = new ItemStack(ItemAndBlockHolder.ITEM_EX_BUS, 1, META_IMPORT_BUS);

        Upgrades.FUZZY.registerItem(importBus, 1);
        Upgrades.REDSTONE.registerItem(importBus, 1);
        Upgrades.CAPACITY.registerItem(importBus, 5);
        Upgrades.SPEED.registerItem(importBus, 4);
        Upgrades.SUPERSPEED.registerItem(importBus, 4);
        Upgrades.SUPERLUMINALSPEED.registerItem(importBus, 4);
        Upgrades.ORE_FILTER.registerItem(importBus, 1);

        final ItemStack exportBus = new ItemStack(ItemAndBlockHolder.ITEM_EX_BUS, 1, META_EXPORT_BUS);

        Upgrades.FUZZY.registerItem(exportBus, 1);
        Upgrades.REDSTONE.registerItem(exportBus, 1);
        Upgrades.CRAFTING.registerItem(exportBus, 1);
        Upgrades.CAPACITY.registerItem(exportBus, 5);
        Upgrades.SPEED.registerItem(exportBus, 4);
        Upgrades.SUPERSPEED.registerItem(exportBus, 4);
        Upgrades.SUPERLUMINALSPEED.registerItem(exportBus, 4);
        Upgrades.ORE_FILTER.registerItem(exportBus, 1);

        // rv3's own storage bus card set, speed cards included - the faced inventory is not throttled
        // here, so there is nothing for them to multiply.
        final ItemStack storageBus = new ItemStack(ItemAndBlockHolder.ITEM_EX_BUS, 1, META_STORAGE_BUS);

        Upgrades.FUZZY.registerItem(storageBus, 1);
        Upgrades.REDSTONE.registerItem(storageBus, 1);
        Upgrades.CAPACITY.registerItem(storageBus, 5);
        Upgrades.INVERTER.registerItem(storageBus, 1);
        Upgrades.STICKY.registerItem(storageBus, 1);
        Upgrades.ORE_FILTER.registerItem(storageBus, 1);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getSpriteNumber() {
        return 0;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(final IIconRegister iconRegister) {
        this.importIcon = iconRegister.registerIcon(Wtct.MODID + ":" + NameConst.ITEM_PART_EX_IMPORT_BUS);
        this.exportIcon = iconRegister.registerIcon(Wtct.MODID + ":" + NameConst.ITEM_PART_EX_EXPORT_BUS);
        this.storageIcon = iconRegister.registerIcon(Wtct.MODID + ":" + NameConst.ITEM_PART_EX_STORAGE_BUS);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamage(final int meta) {
        return meta == META_EXPORT_BUS ? this.exportIcon
            : meta == META_STORAGE_BUS ? this.storageIcon : this.importIcon;
    }

    @Override
    public String getUnlocalizedName(final ItemStack stack) {
        return switch (stack.getItemDamage()) {
            case META_EXPORT_BUS -> super.getUnlocalizedName() + ".export";
            case META_STORAGE_BUS -> super.getUnlocalizedName() + ".storage";
            default -> super.getUnlocalizedName();
        };
    }

    @Override
    public boolean onItemUse(final ItemStack is, final EntityPlayer player, final World world, final int x, final int y,
        final int z, final int side, final float hitX, final float hitY, final float hitZ) {
        return AEApi.instance()
            .partHelper()
            .placeBus(is, x, y, z, side, player, world);
    }

    @Override
    public IPart createPartFromItemStack(final ItemStack is) {
        return switch (is.getItemDamage()) {
            case META_EXPORT_BUS -> new PartExExportBus(is);
            case META_STORAGE_BUS -> new PartExStorageBus(is);
            default -> new PartExImportBus(is);
        };
    }

    @Override
    protected void getCheckedSubItems(final Item sameItem, final CreativeTabs creativeTab,
        final List<ItemStack> itemStacks) {
        itemStacks.add(this.stack(1, META_IMPORT_BUS));
        itemStacks.add(this.stack(1, META_EXPORT_BUS));
        itemStacks.add(this.stack(1, META_STORAGE_BUS));
    }

    @Override
    public ItemExBusPart register() {
        GameRegistry.registerItem(this, NameConst.ITEM_PART_EX_BUS, Wtct.MODID);
        setCreativeTab(WtctTabs.INSTANCE);
        return this;
    }
}
