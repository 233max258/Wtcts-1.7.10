package com.asdflj.wtct.client.gui;

import java.io.IOException;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.input.Mouse;

import com.asdflj.wtct.Wtct;
import com.asdflj.wtct.client.gui.container.ContainerExStorageBus;
import com.asdflj.wtct.client.gui.widget.GuiWcwtTabButton;
import com.asdflj.wtct.client.gui.widget.GuiWcwtToolbarButton;
import com.asdflj.wtct.common.parts.PartExStorageBus;

import appeng.api.config.AccessRestriction;
import appeng.api.config.ActionItems;
import appeng.api.config.ExtractionMode;
import appeng.api.config.FuzzyMode;
import appeng.api.config.Settings;
import appeng.api.config.StorageFilter;
import appeng.client.gui.widgets.GuiImgButton;
import appeng.core.AELog;
import appeng.core.localization.GuiText;
import appeng.core.sync.GuiBridge;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketConfigButton;
import appeng.core.sync.packets.PacketSwitchGuis;
import appeng.core.sync.packets.PacketValueConfig;

/**
 * The ME extended storage bus screen: rv3's storage bus buttons on the extended bus panel.
 *
 * <p>
 * The panel, the sixty-three slot phantom grid and the upgrade strip are the extended buses' shared work;
 * what this screen adds is rv3's own storage bus row of buttons, transcribed to 1.21's toolbar: clear and
 * partition as actions, then storage filter, extract mode, fuzzy mode and the access mode that decides
 * whether extract mode means anything, with the priority tab hanging off the panel's top right corner. The
 * buttons come from the pristine 1.21 icon sheet - the pictures rv3's enum members map onto in AE2 1.21,
 * borrowed where 1.21 itself has no such setting.
 */
public class GuiExStorageBus extends GuiExIOBus {

    /** AE2 1.21's untouched states sheet: {@code Icon.PRIORITY}. */
    private static final ResourceLocation AE2_121_STATES = new ResourceLocation(
        Wtct.MODID,
        "textures/guis/wcwt/ae2_121_states.png");
    private static final int PRIORITY_ICON_U = 144, PRIORITY_ICON_V = 64;

    private final ContainerExStorageBus csb;

    private GuiImgButton clearBtn;
    private GuiImgButton partitionBtn;
    private GuiImgButton rwMode;
    private GuiImgButton extractionMode;
    private GuiImgButton storageFilter;
    private GuiWcwtTabButton priorityBtn;

    public GuiExStorageBus(final InventoryPlayer inventoryPlayer, final PartExStorageBus te) {
        super(inventoryPlayer, te, new ContainerExStorageBus(inventoryPlayer, te));
        this.csb = (ContainerExStorageBus) this.inventorySlots;
    }

    @Override
    protected void addButtons() {
        // rv3's own rows, in rv3's own order - the sidebar compacts them by visibility every frame anyway.
        this.clearBtn = new GuiWcwtToolbarButton(
            this.guiLeft - 18,
            this.guiTop + 8,
            Settings.ACTIONS,
            ActionItems.CLOSE).withIconAtlas(AE2_121_STATES);
        this.partitionBtn = new GuiWcwtToolbarButton(
            this.guiLeft - 18,
            this.guiTop + 28,
            Settings.ACTIONS,
            ActionItems.WRENCH).withIconAtlas(AE2_121_STATES);
        this.rwMode = new GuiWcwtToolbarButton(
            this.guiLeft - 18,
            this.guiTop + 48,
            Settings.ACCESS,
            AccessRestriction.READ_WRITE).withIconAtlas(AE2_121_STATES);
        this.extractionMode = new GuiWcwtToolbarButton(
            this.guiLeft - 18,
            this.guiTop + 68,
            Settings.EXTRACTION_MODE,
            ExtractionMode.LOOSE).withIconAtlas(AE2_121_STATES);
        this.storageFilter = new GuiWcwtToolbarButton(
            this.guiLeft - 18,
            this.guiTop + 88,
            Settings.STORAGE_FILTER,
            StorageFilter.EXTRACTABLE_ONLY).withIconAtlas(AE2_121_STATES);
        this.fuzzyMode = new GuiWcwtToolbarButton(
            this.guiLeft - 18,
            this.guiTop + 108,
            Settings.FUZZY_MODE,
            FuzzyMode.IGNORE_ALL);
        this.oreFilter = new GuiWcwtToolbarButton(
            this.guiLeft - 18,
            this.guiTop + 108,
            Settings.ACTIONS,
            ActionItems.ORE_FILTER);

        this.buttonList.add(this.clearBtn);
        this.buttonList.add(this.partitionBtn);
        this.buttonList.add(this.rwMode);
        this.buttonList.add(this.extractionMode);
        this.buttonList.add(this.storageFilter);
        this.buttonList.add(this.fuzzyMode);
        this.buttonList.add(this.oreFilter);

        this.priorityBtn = new GuiWcwtTabButton(
            this.guiLeft + 152,
            this.guiTop - 5,
            PRIORITY_ICON_U,
            PRIORITY_ICON_V,
            GuiText.Priority.getLocal());
        this.buttonList.add(this.priorityBtn);
    }

    @Override
    public void drawFG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        super.drawFG(offsetX, offsetY, mouseX, mouseY);

        this.storageFilter.set(this.csb.getStorageFilter());
        this.rwMode.set(this.csb.getReadWriteMode());
        this.extractionMode.set(this.csb.getExtractionMode());
        // rv3's own rule: what may be extracted only matters while the bus is writable.
        this.extractionMode.setEnabled(this.csb.getReadWriteMode() == AccessRestriction.READ_WRITE);
        this.partitionBtn.set(this.csb.getPartitionMode());
    }

    @Override
    protected void actionPerformed(final GuiButton btn) {
        super.actionPerformed(btn);

        final boolean backwards = Mouse.isButtonDown(1);

        try {
            if (btn == this.partitionBtn) {
                NetworkHandler.instance.sendToServer(
                    new PacketValueConfig("StorageBus.Action", backwards ? "Partition-Clear" : "Partition"));
            } else if (btn == this.clearBtn) {
                NetworkHandler.instance.sendToServer(new PacketValueConfig("StorageBus.Action", "Clear"));
            } else if (btn == this.priorityBtn) {
                NetworkHandler.instance.sendToServer(new PacketSwitchGuis(GuiBridge.GUI_PRIORITY));
            } else if (btn == this.rwMode) {
                NetworkHandler.instance.sendToServer(new PacketConfigButton(this.rwMode.getSetting(), backwards));
            } else if (btn == this.extractionMode) {
                NetworkHandler.instance
                    .sendToServer(new PacketConfigButton(this.extractionMode.getSetting(), backwards));
            } else if (btn == this.storageFilter) {
                NetworkHandler.instance
                    .sendToServer(new PacketConfigButton(this.storageFilter.getSetting(), backwards));
            }
        } catch (final IOException e) {
            AELog.debug(e);
        }
    }

    /**
     * The sidebar stacks the bus's own button set, 1.21's toolbar order: clear, partition, storage filter,
     * extract mode, fuzzy mode - or the ore filter, which takes its seat - and access last. The base class's
     * four and the scheduling button stay out of it.
     */
    @Override
    protected GuiImgButton[] toolbarButtons() {
        return new GuiImgButton[] { this.clearBtn, this.partitionBtn, this.storageFilter, this.extractionMode,
            this.fuzzyMode, this.oreFilter, this.rwMode };
    }
}
