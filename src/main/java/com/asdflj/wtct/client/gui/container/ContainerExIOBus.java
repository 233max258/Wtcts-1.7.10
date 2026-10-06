package com.asdflj.wtct.client.gui.container;

import static appeng.util.Platform.isServer;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;

import com.asdflj.wtct.common.parts.ExBusScreenHost;
import com.asdflj.wtct.common.parts.ExBusSlots;
import com.asdflj.wtct.common.parts.PartExImportBus;
import com.asdflj.wtct.inventory.gui.BusPrimaryGui;
import com.asdflj.wtct.inventory.gui.GuiType;

import appeng.api.storage.StorageName;
import appeng.api.storage.data.IAEStack;
import appeng.container.PrimaryGui;
import appeng.container.implementations.ContainerUpgradeable;
import appeng.container.interfaces.IVirtualSlotHolder;
import appeng.container.slot.SlotRestrictedInput;
import appeng.tile.inventory.IAEStackInventory;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

/**
 * The container behind both ME extended bus screens.
 *
 * <p>
 * AE2 builds its own bus container the same way: the filter grid is not a real inventory of slots here but a set of
 * virtual ones the screen draws, so all this has to do is carry 63 of them instead of nine, reserve room for eight
 * upgrade cards and tell the parent how tall the panel is - which is also what positions the player inventory.
 */
public class ContainerExIOBus extends ContainerUpgradeable implements IVirtualSlotHolder {

    public final IAEStack<?>[] virtualSlotsClient = new IAEStack<?>[ExBusSlots.MAX_SLOTS];
    private final ExBusScreenHost host;

    public ContainerExIOBus(final InventoryPlayer ip, final ExBusScreenHost host) {
        super(ip, host);
        this.host = host;

        // The 1.21 upgrade panel resolves to (imageWidth - 2, 0) = (174, 0) - overlapping the art's right
        // border by two pixels - and puts its slots at the panel's +1/+6 origin, not AE2 1.7.10's (187, 8)
        // strip position, so the slots the parent just created are moved over before anyone can look at them.
        // The panel art already carries 1.21's own BACKGROUND_UPGRADE slot holes, so the slot's 1.7.10
        // card-with-arrow icon (drawn on top by AEBaseGui.drawTextureOnSlot) is switched off with IIcon -1,
        // which that renderer skips.
        int upgradeIndex = 0;
        for (final Object o : this.inventorySlots) {
            if (o instanceof SlotRestrictedInput
                && ((SlotRestrictedInput) o).getItemType() == SlotRestrictedInput.PlacableItemType.UPGRADES) {
                ((Slot) o).xDisplayPosition = ExBusSlots.UPGRADE_SLOT_X;
                ((Slot) o).yDisplayPosition = ExBusSlots.UPGRADE_SLOT_Y + ExBusSlots.SLOT_SIZE * upgradeIndex++;
                ((SlotRestrictedInput) o).setIIcon(-1);
            }
        }
    }

    @Override
    protected int getHeight() {
        return ExBusSlots.HEIGHT;
    }

    @Override
    public int availableUpgrades() {
        return ExBusSlots.UPGRADE_SLOTS;
    }

    /**
     * The upgrade strip is already eight slots tall, so there is no room left for the network tool panel AE2 adds
     * beside it.
     */
    @Override
    public boolean hasToolbox() {
        return false;
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        this.updateVirtualSlots(StorageName.CONFIG, this.host.getExConfig(), this.virtualSlotsClient);
    }

    @Override
    public void receiveSlotStacks(final StorageName invName, final Int2ObjectMap<IAEStack<?>> slotStacks) {
        final IAEStackInventory storage = this.host.getExConfig();
        for (final var entry : slotStacks.int2ObjectEntrySet()) {
            storage.putAEStackInSlot(entry.getIntKey(), entry.getValue());
        }
        if (isServer()) {
            this.updateVirtualSlots(StorageName.CRAFTING_OUTPUT, storage, this.virtualSlotsClient);
        }
    }

    /**
     * AE2's ore filter screen returns through the primary gui it is handed when it is opened, and its own factory
     * builds that from a {@code GuiBridge} lookup this container cannot appear in - so the screen could be entered
     * but never left. One of this mod's own primary guis, which reopens this screen, is handed over instead.
     */
    @Override
    public PrimaryGui createPrimaryGui() {
        return BusPrimaryGui
            .create(this, this.host instanceof PartExImportBus ? GuiType.EX_IMPORT_BUS : GuiType.EX_EXPORT_BUS);
    }
}
