package com.asdflj.wtct.common.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.asdflj.wtct.Wtct;
import com.asdflj.wtct.client.textures.BlockTexture;
import com.asdflj.wtct.common.item.BaseItemBlock;
import com.asdflj.wtct.common.nethub.NetHubStorage;
import com.asdflj.wtct.common.nethub.Network;
import com.asdflj.wtct.common.nethub.Perm;
import com.asdflj.wtct.common.tabs.WtctTabs;
import com.asdflj.wtct.common.tile.TileNetworkHub;
import com.asdflj.wtct.inventory.InventoryHandler;
import com.asdflj.wtct.inventory.gui.GuiType;
import com.asdflj.wtct.loader.IRegister;
import com.asdflj.wtct.util.BlockPos;
import com.asdflj.wtct.util.NameConst;

import appeng.util.Platform;
import cpw.mods.fml.common.registry.GameRegistry;

/**
 * The ME network hub: the block that carries a whole ME network across the gap to another hub. Right-clicking opens the
 * network screen; sneak-wrenching takes the block back.
 */
public class BlockNetworkHub extends BaseTileBlock implements IRegister<BlockNetworkHub> {

    /** The one instance, so a tile can tell whether the block it sits in is still itself. */
    public static BlockNetworkHub INSTANCE;

    public BlockNetworkHub() {
        super(Material.iron);
        INSTANCE = this;
        this.setBlockName(Wtct.MODID + "." + NameConst.BLOCK_NETWORK_HUB);
        this.setBlockTextureName(NameConst.RES_KEY + NameConst.BLOCK_NETWORK_HUB + "_side_off");
        setTileEntity(TileNetworkHub.class);
    }

    /**
     * Deliberately replaces AE2's own right-click handling: taking the block back with a wrench and opening the screen
     * both have to happen without the block-break event AE2 would post, otherwise a head could never be removed.
     */
    @Override
    public boolean onBlockActivated(World w, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY,
        float hitZ) {
        if (!(w.getTileEntity(x, y, z) instanceof TileNetworkHub hub)) return false;
        if (w.isRemote) return true;

        ItemStack held = player.inventory.getCurrentItem();
        if (held != null && player.isSneaking() && Platform.isWrench(player, held, x, y, z)) {
            w.setBlockToAir(x, y, z);
            w.spawnEntityInWorld(new EntityItem(w, x + 0.5D, y + 0.5D, z + 0.5D, new ItemStack(this)));
            return true;
        }

        Network network = NetHubStorage.get(w)
            .getNetwork(hub.getNetworkUuid());
        if (network == null || network.hasPermission(player, Perm.USER)) {
            InventoryHandler.openGui(player, w, new BlockPos(x, y, z), ForgeDirection.UNKNOWN, GuiType.NETWORK_HUB);
        } else {
            player.addChatMessage(new ChatComponentTranslation(NameConst.MESSAGE_NETWORK_HUB_NO_PERMISSION));
        }
        return true;
    }

    @Override
    public void onBlockPlacedBy(World w, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(w, x, y, z, placer, stack);
        if (!w.isRemote && placer instanceof EntityPlayer player
            && w.getTileEntity(x, y, z) instanceof TileNetworkHub hub) {
            hub.setOwner(player);
        }
    }

    @Override
    public void breakBlock(World w, int x, int y, int z, Block a, int b) {
        if (Platform.isServer() && w.getTileEntity(x, y, z) instanceof TileNetworkHub hub) {
            hub.breakConnection();
        }
        super.breakBlock(w, x, y, z, a, b);
    }

    @Override
    public IIcon getIcon(IBlockAccess w, int x, int y, int z, int s) {
        BlockTexture.IconWrapper wrapper = BlockTexture.textureMap.get(NameConst.BLOCK_NETWORK_HUB);
        if (wrapper != null && !wrapper.get(w.getBlockMetadata(x, y, z) == 1)
            .isEmpty()) {
            return wrapper.get(w.getBlockMetadata(x, y, z) == 1)
                .get(0);
        }
        return super.getIcon(w, x, y, z, s);
    }

    @Override
    public void setRenderStateByMeta(int itemDamage) {
        BlockTexture.IconWrapper wrapper = BlockTexture.textureMap.get(NameConst.BLOCK_NETWORK_HUB);
        if (wrapper != null && !wrapper.get(itemDamage == 1)
            .isEmpty()) {
            IIcon icon = wrapper.get(itemDamage == 1)
                .get(0);
            this.getRendererInstance()
                .setTemporaryRenderIcons(icon, icon, icon, icon, icon, icon);
        } else {
            super.setRenderStateByMeta(itemDamage);
        }
    }

    @Override
    public BlockNetworkHub register() {
        GameRegistry.registerBlock(this, BaseItemBlock.class, NameConst.BLOCK_NETWORK_HUB);
        GameRegistry.registerTileEntity(TileNetworkHub.class, NameConst.BLOCK_NETWORK_HUB);
        this.setCreativeTab(WtctTabs.INSTANCE);
        return this;
    }
}
