package com.asdflj.wtct.common.item;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.ForgeEventFactory;

import com.asdflj.wtct.api.IAnyTierElectricItem;
import com.asdflj.wtct.api.WirelessObject;
import com.asdflj.wtct.common.item.card.CardTicker;
import com.asdflj.wtct.inventory.InventoryHandler;
import com.asdflj.wtct.inventory.gui.GuiType;
import com.asdflj.wtct.inventory.item.IItemInventory;
import com.asdflj.wtct.util.BlockPos;

import appeng.api.AEApi;
import appeng.api.features.ILocatable;
import appeng.api.features.IWirelessTermHandler;
import appeng.api.features.IWirelessTermRegistry;
import appeng.core.AppEng;
import appeng.core.features.AEFeature;
import appeng.core.localization.PlayerMessages;
import appeng.items.tools.powered.ToolWirelessTerminal;
import appeng.util.Platform;
import baubles.api.IBauble;
import baubles.api.expanded.BaubleExpandedSlots;
import ic2.api.item.IElectricItemManager;
import ic2.api.item.ISpecialElectricItem;

public abstract class ItemBaseWirelessTerminal extends ToolWirelessTerminal
    implements IAnyTierElectricItem, ISpecialElectricItem, IElectricItemManager, IItemInventory, IBauble {

    public ItemBaseWirelessTerminal() {
        super();
        this.setFeature(EnumSet.of(AEFeature.WirelessAccessTerminal, AEFeature.PoweredTools));
    }

    /**
     * AE2's coremod strips the IC2 item interfaces from its own powered base in this environment -
     * {@code Removing Interface ic2.api.item.ISpecialElectricItem / IElectricItemManager from
     * appeng/items/tools/powered/powersink/IC2 because IC2 integration is disabled} - leaving the
     * terminal answerable only through the RF interface, which no EU machine speaks. Every method
     * behind those interfaces survives on the superclass except {@code getManager}, which is the one
     * method exclusive to {@code ISpecialElectricItem} and so the only one AE2 removes; re-declaring
     * the interfaces here and answering the manager with {@code this} re-arms the whole chain: GT's
     * {@code isElectricItem} recognises the terminal, GT machines and IC2's charger both delegate
     * through {@code getManager} into AE2's own charge methods, and the EU they push lands in the
     * item's AE battery as before.
     */
    @Override
    public IElectricItemManager getManager(final ItemStack itemStack) {
        return this;
    }

    /**
     * AE2's powered-item base answers this with a hard {@code 1} (LV), and GregTech gates its charging
     * on that tier <em>exactly</em>: {@code GTModHandler.chargeElectricItem} only charges when the
     * item's tier equals the machine's tier (or is negative), so a tier-1 terminal could only ever
     * charge in an LV battery buffer while every higher-tier machine turned it away - which read as
     * "cannot be charged in GT machines at all". Answering {@code -1} walks through the gate GT
     * itself leaves for tier-less items, so any voltage of machine charges the terminal, at full
     * speed since the negative tier also skips GT's per-call voltage clamp. IC2's own charger never
     * looked at this value (it delegates to the item's manager, which ignores the caller's tier), so
     * that path is unchanged.
     */
    @Override
    public int getTier(final ItemStack itemStack) {
        return -1;
    }

    public ItemStack stack() {
        return new ItemStack(this, 1);
    }

    @Override
    public boolean canHandle(final ItemStack is) {
        return is != null && is.getItem() instanceof ItemBaseWirelessTerminal;
    }

    @Override
    public boolean canEquip(ItemStack itemstack, EntityLivingBase player) {
        return true;
    }

    /**
     * Baubles-Expanded decides which slots an item may go into by matching the types returned here against
     * the slot's assigned type (see {@code BaublesConfig.canTypeFitSlot}). The AE2 parent class hardcodes
     * {@code {"Terminal"}}, which would restrict this item to the single "Terminal" slot that AE2 registers.
     * To stay usable in any bauble slot we advertise our own type plus "universal" plus every currently
     * assigned slot type, so the item also fits slots added by other mods.
     */
    @Override
    public String[] getBaubleTypes(ItemStack itemstack) {
        List<String> types = new ArrayList<>();
        types.add(AppEng.BAUBLESLOT);
        types.add(BaubleExpandedSlots.universalType);
        for (String slotType : BaubleExpandedSlots.getCurrentSlotAssignments()) {
            if (slotType != null && !slotType.isEmpty()
                && !BaubleExpandedSlots.invalidType.equals(slotType)
                && !types.contains(slotType)) {
                types.add(slotType);
            }
        }
        return types.toArray(new String[0]);
    }

    @Override
    public ItemStack onItemRightClick(final ItemStack item, final World w, final EntityPlayer player) {
        if (ForgeEventFactory.onItemUseStart(player, item, 1) > 0) {
            if (Platform.isClient()) return item;
            IWirelessTermRegistry term = AEApi.instance()
                .registries()
                .wireless();
            if (!term.isWirelessTerminal(item)) {
                player.addChatMessage(PlayerMessages.DeviceNotWirelessTerminal.get());
                return item;
            }
            final IWirelessTermHandler handler = term.getWirelessTerminalHandler(item);
            final String unparsedKey = handler.getEncryptionKey(item);
            if (unparsedKey.isEmpty()) {
                player.addChatMessage(PlayerMessages.DeviceNotLinked.get());
                return item;
            }
            final long parsedKey = Long.parseLong(unparsedKey);
            final ILocatable securityStation = AEApi.instance()
                .registries()
                .locatable()
                .getLocatableBy(parsedKey);
            if (securityStation == null) {
                player.addChatMessage(PlayerMessages.StationCanNotBeLocated.get());
                return item;
            }
            if (handler.hasPower(player, 0.5, item)) {
                InventoryHandler.openGui(
                    player,
                    w,
                    new BlockPos(player.inventory.currentItem, 0, 0),
                    ForgeDirection.UNKNOWN,
                    this.guiGuiType(item));
            } else {
                player.addChatMessage(PlayerMessages.DeviceNotPowered.get());
            }
        }

        return item;
    }

    @Override
    public boolean hasInfinityRange(final ItemStack is) {
        return WirelessObject.hasInfinityBoosterCard(is);
    }

    @Override
    public boolean hasInfinityPower(final ItemStack is) {
        return WirelessObject.hasEnergyCard(is);
    }

    /**
     * The terminal's upgrade cards run from here, the 1.7.10 analogue of AE2ImportExportCard's
     * {@code WirelessTerminalItem#inventoryTick} / AE2WTLib's terminal ticker: the item is ticked by
     * whichever inventory holds it - hotbar, main inventory or a bauble - so the cards work whether
     * or not the terminal's screen is open.
     */
    @Override
    public void onUpdate(final ItemStack stack, final World world, final Entity entity, final int slot,
        final boolean isCurrentItem) {
        super.onUpdate(stack, world, entity, slot, isCurrentItem);
        CardTicker.onUpdate(stack, world, entity, slot);
    }

    /**
     * The Baubles half of {@link #onUpdate}.
     *
     * <p>
     * An item worn in a bauble slot is not in the player's {@code mainInventory}, so vanilla never
     * calls {@link #onUpdate} on it - AE2's own {@code ToolWirelessTerminal#onWornTick} only looks at
     * the battery. Without this override a terminal carried in a bauble ran no cards at all, which
     * made the magnet (and the import/export cards) look broken for anyone wearing the terminal
     * instead of holding it. The slot number is the one the card's network lookup needs, and Baubles
     * reports it through the item's own NBT on this version, so -1 is passed: the lookup only uses it
     * to re-read the terminal, which it already has.
     */
    @Override
    public void onWornTick(final ItemStack itemstack, final net.minecraft.entity.EntityLivingBase player) {
        super.onWornTick(itemstack, player);
        if (player instanceof final EntityPlayer entityPlayer) {
            CardTicker.onUpdate(itemstack, entityPlayer.worldObj, entityPlayer, -1);
        }
    }

    protected abstract GuiType guiGuiType(ItemStack item);
}
