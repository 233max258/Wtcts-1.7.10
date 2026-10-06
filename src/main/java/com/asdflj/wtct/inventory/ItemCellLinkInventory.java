package com.asdflj.wtct.inventory;

import static com.asdflj.wtct.api.Constants.MessageType.UPDATE_PLAYER_CURRENT_ITEM;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.asdflj.wtct.Wtct;
import com.asdflj.wtct.api.Constants;
import com.asdflj.wtct.api.WtctAPI;
import com.asdflj.wtct.common.storage.StorageManager;
import com.asdflj.wtct.common.storage.infinityCell.BaseInventory;
import com.asdflj.wtct.network.SPacketMEItemInvUpdate;

import appeng.container.interfaces.IInventorySlotAware;
import appeng.tile.inventory.BiggerAppEngInventory;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;

public class ItemCellLinkInventory extends BiggerAppEngInventory implements BaseInventory, IInventorySlotAware {

    private final ItemStack is;
    private final EntityPlayer player;
    private final int slot;

    public ItemCellLinkInventory(ItemStack is, EntityPlayer player, int slot) {
        super(null, 1);
        this.is = is;
        this.player = player;
        this.slot = slot;
        this.genUUID();
    }

    private void genUUID() {
        if (Platform.isServer() && Platform.openNbtData(is)
            .hasNoTags()) {
            StorageManager m = WtctAPI.instance()
                .getStorageManager();
            m.getStorage(this.is, this.player);
            SPacketMEItemInvUpdate piu = new SPacketMEItemInvUpdate(UPDATE_PLAYER_CURRENT_ITEM);
            piu.appendItem(AEItemStack.create(this.is));
            Wtct.proxy.netHandler.sendTo(piu, (EntityPlayerMP) player);
        }
    }

    @Override
    public void markDirty() {}

    @Override
    public void setInventorySlotContents(int slot, ItemStack disk) {
        if (Platform.isServer() && disk != null
            && disk.getItem() != null
            && is.getItem() != null
            && disk.getItem()
                .equals(is.getItem())) {
            StorageManager m = WtctAPI.instance()
                .getStorageManager();
            String uid = m.getStorage(this.is, this.player)
                .getUUID();
            m.setStorage(uid, disk);
        }
        super.setInventorySlotContents(slot, disk);
    }

    @Override
    public ItemStack getItemStack() {
        return this.is;
    }

    @Override
    public int getInventorySlot() {
        return this.slot;
    }

    @Override
    public String getUUID() {
        NBTTagCompound data = Platform.openNbtData(is);
        return data.getString(Constants.DISKUUID);
    }
}
