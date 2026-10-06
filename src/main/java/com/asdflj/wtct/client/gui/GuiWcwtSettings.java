package com.asdflj.wtct.client.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import com.asdflj.wtct.Wtct;
import com.asdflj.wtct.api.WtctAPI;
import com.asdflj.wtct.client.gui.container.ContainerWcwtSettings;
import com.asdflj.wtct.client.gui.widget.GuiWcwtBackButton;
import com.asdflj.wtct.client.gui.widget.GuiWcwtCheckbox;
import com.asdflj.wtct.inventory.InventoryHandler;
import com.asdflj.wtct.inventory.gui.GuiType;
import com.asdflj.wtct.nei.ButtonConstants;
import com.asdflj.wtct.nei.NEI_TH_Config;
import com.asdflj.wtct.network.CPacketTerminalBtns;

import appeng.api.storage.ITerminalHost;
import appeng.client.gui.AEBaseGui;

/**
 * The wireless terminal's settings screen - WTLib's {@code WirelessTerminalSettingsScreen} laid out
 * the way its {@code wireless_terminal_settings.json} does: a modal whose switches are grouped under
 * headers (the screen title, "上传设置" and "磁铁设置"), each row a 22x12 slider switch with its
 * label right beside it, the back tab hanging off the top-right corner. The reference panel is 200x122
 * with two magnet rows; this port has one, extra switches in the first group, and the upload pair
 * WCWT keeps in its pattern management area.
 *
 * <p>
 * Rows: the reference's pick-block / craft-if-missing / restock trio first (the port's own block
 * picker is the pick-block switch, and the other two are switches without behaviour behind them
 * yet), then the port's larger-counts and crafting-pin switches, then the upload pair, then the
 * magnet group. As in the reference, "缺货自动制作" is dead while "启用选取方块" is off. The
 * reference screen has no slots at all, so the player's rows are parked off-screen instead of being
 * drawn over the panel.
 */
public class GuiWcwtSettings extends AEBaseGui {

    private static final int PANEL_W = 200;
    private static final int PANEL_H = 228;
    /** Palette of the terminal's own panel: face, frame, outer line. */
    private static final int FACE = 0xFFCBCBD4;
    private static final int FRAME = 0xFF413F54;

    /** The reference JSON's text anchors: screen title at (8,7), a group's at the rows above it. */
    private static final int TITLE_X = 8;
    private static final int TITLE_Y = 7;
    /** Rows are 16px apart, and a group's first row starts 10px under its header. */
    private static final int ROW_X = 10;
    private static final int ROW_PICK_Y = 25;
    private static final int ROW_PICK_BLOCK_CRAFT_Y = 41;
    private static final int ROW_RESTOCK_Y = 57;
    private static final int ROW_BIG_COUNTS_Y = 73;
    /**
     * AE2 1.21's two crafting-pin switches, which live in its terminal settings screen next to these. They
     * are stored in NEI's global config rather than on the terminal item, because both are consulted while no
     * terminal is open: a crafting job pins its output as it starts, and its finished notification arrives
     * after the screen is long gone.
     */
    private static final int ROW_PINNED_AUTO_CRAFT_Y = 89;
    private static final int ROW_CRAFTING_NOTIFY_Y = 105;
    /**
     * AE2 1.21's third crafting switch, "clear grid on close". Unlike the two above it this one is a
     * property of the terminal (it is read as the terminal closes, from the terminal stack itself),
     * so it lives in the terminal's NBT - see {@code ContainerWcwtSettings.KEY_CLEAR_GRID_ON_CLOSE}.
     */
    private static final int ROW_CLEAR_GRID_Y = 121;
    /**
     * The upload group: every switch here is about what an encode does with its finished pattern or with
     * the amounts it was built from. "Only a unique provider name" is the gate, "circuit always one"
     * decides what a multiply may touch, and "stash ambiguous uploads" decides where a pattern that did
     * not name one provider goes - the three read in the order the upload meets them.
     */
    private static final int UPLOAD_GROUP_TITLE_Y = 137;
    private static final int ROW_UPLOAD_UNIQUE_Y = 147;
    private static final int ROW_CIRCUIT_ONE_Y = 163;
    private static final int ROW_STASH_Y = 179;
    private static final int MAGNET_GROUP_TITLE_Y = 195;
    private static final int ROW_MAGNET_Y = 205;
    /** The label sits 4px past the switch and is centred on it, as the reference puts it. */
    private static final int LABEL_DX = 26;
    private static final int LABEL_DY = 2;
    /** A dead row's label, next to its half-faded switch. */
    private static final int MUTED_TEXT = 0x909090;

    /** Off-screen position for the slots this dialog does not use, as the card screens park theirs. */
    private static final int HIDDEN_SLOT = -9999;

    private final ContainerWcwtSettings container;
    private final ITerminalHost host;

    private GuiWcwtCheckbox pickBlockToggle;
    private GuiWcwtCheckbox pickBlockCraftToggle;
    private GuiWcwtCheckbox restockToggle;
    private GuiWcwtCheckbox bigCountsToggle;
    private GuiWcwtCheckbox pinnedAutoCraftToggle;
    private GuiWcwtCheckbox craftingNotifyToggle;
    private GuiWcwtCheckbox clearGridToggle;
    private GuiWcwtCheckbox uploadUniqueToggle;
    private GuiWcwtCheckbox circuitOneToggle;
    private GuiWcwtCheckbox stashToggle;
    private GuiWcwtCheckbox magnetToggle;
    private GuiWcwtBackButton backButton;

    public GuiWcwtSettings(final InventoryPlayer ip, final ITerminalHost host) {
        super(new ContainerWcwtSettings(ip, host));
        this.container = (ContainerWcwtSettings) this.inventorySlots;
        this.host = host;
        this.xSize = PANEL_W;
        this.ySize = PANEL_H;
    }

    @Override
    public void initGui() {
        super.initGui();
        this.xSize = PANEL_W;
        this.ySize = PANEL_H;
        final int left = this.guiLeft;
        final int top = this.guiTop;
        // The dialog owns the whole screen: the player's rows would otherwise land on the panel.
        for (final Object o : this.inventorySlots.inventorySlots) {
            if (o instanceof final Slot slot) {
                slot.xDisplayPosition = HIDDEN_SLOT;
                slot.yDisplayPosition = HIDDEN_SLOT;
            }
        }
        this.buttonList
            .add(this.pickBlockToggle = new GuiWcwtCheckbox(left + ROW_X, top + ROW_PICK_Y));
        this.pickBlockToggle.setTooltip(StatCollector.translateToLocal("wtct.settings.pick_block.desc"));
        this.buttonList.add(
            this.pickBlockCraftToggle = new GuiWcwtCheckbox(left + ROW_X, top + ROW_PICK_BLOCK_CRAFT_Y));
        this.pickBlockCraftToggle
            .setTooltip(StatCollector.translateToLocal("wtct.settings.pick_block_craft.desc"));
        this.buttonList.add(this.restockToggle = new GuiWcwtCheckbox(left + ROW_X, top + ROW_RESTOCK_Y));
        this.restockToggle.setTooltip(StatCollector.translateToLocal("wtct.settings.restock.desc"));
        this.buttonList.add(this.bigCountsToggle = new GuiWcwtCheckbox(left + ROW_X, top + ROW_BIG_COUNTS_Y));
        this.bigCountsToggle.setTooltip(StatCollector.translateToLocal("wtct.settings.big_counts.desc"));
        this.buttonList.add(
            this.pinnedAutoCraftToggle = new GuiWcwtCheckbox(left + ROW_X, top + ROW_PINNED_AUTO_CRAFT_Y));
        this.pinnedAutoCraftToggle
            .setTooltip(StatCollector.translateToLocal("wtct.settings.pinned_auto_craft.desc"));
        this.buttonList.add(
            this.craftingNotifyToggle = new GuiWcwtCheckbox(left + ROW_X, top + ROW_CRAFTING_NOTIFY_Y));
        this.craftingNotifyToggle.setTooltip(StatCollector.translateToLocal("wtct.settings.crafting_notify.desc"));
        this.buttonList.add(this.clearGridToggle = new GuiWcwtCheckbox(left + ROW_X, top + ROW_CLEAR_GRID_Y));
        this.clearGridToggle.setTooltip(StatCollector.translateToLocal("wtct.settings.clear_grid_on_close.desc"));
        this.buttonList
            .add(this.uploadUniqueToggle = new GuiWcwtCheckbox(left + ROW_X, top + ROW_UPLOAD_UNIQUE_Y));
        this.uploadUniqueToggle.setTooltip(StatCollector.translateToLocal("wtct.settings.upload_unique.desc"));
        this.buttonList
            .add(this.circuitOneToggle = new GuiWcwtCheckbox(left + ROW_X, top + ROW_CIRCUIT_ONE_Y));
        this.circuitOneToggle.setTooltip(StatCollector.translateToLocal("wtct.settings.circuit_one.desc"));
        this.buttonList.add(this.stashToggle = new GuiWcwtCheckbox(left + ROW_X, top + ROW_STASH_Y));
        this.stashToggle.setTooltip(StatCollector.translateToLocal("wtct.settings.stash.desc"));
        this.buttonList.add(this.magnetToggle = new GuiWcwtCheckbox(left + ROW_X, top + ROW_MAGNET_Y));
        this.magnetToggle.setTooltip(StatCollector.translateToLocal("wtct.settings.magnet_card.desc"));
        // Back tab: AE2 1.21's 20x20 tab plate with its back arrow on it - the icon every screen of this
        // mod closes with - rather than a tab carrying the terminal's own item sprite, which read as a
        // square lump in the corner and said nothing about what clicking it does.
        this.buttonList.add(this.backButton = new GuiWcwtBackButton(0, left + PANEL_W - 24, top - 5));
        this.refreshToggles();
    }

    private void refreshToggles() {
        this.pickBlockToggle.setChecked(this.container.isEnabled(ContainerWcwtSettings.KEY_PICK_BLOCK));
        this.pickBlockCraftToggle.setChecked(this.container.isEnabled(ContainerWcwtSettings.KEY_PICK_BLOCK_CRAFT));
        this.restockToggle.setChecked(this.container.isEnabled(ContainerWcwtSettings.KEY_RESTOCK));
        this.bigCountsToggle.setChecked(this.container.isEnabled(ContainerWcwtSettings.KEY_BIG_COUNTS));
        this.pinnedAutoCraftToggle.setChecked(NEI_TH_Config.getConfigValue(ButtonConstants.PINNED_AUTO_CRAFT));
        this.craftingNotifyToggle.setChecked(NEI_TH_Config.getConfigValue(ButtonConstants.CRAFTING_NOTIFICATION));
        this.clearGridToggle.setChecked(this.container.isEnabled(ContainerWcwtSettings.KEY_CLEAR_GRID_ON_CLOSE));
        this.uploadUniqueToggle.setChecked(this.container.isEnabled(ContainerWcwtSettings.KEY_UPLOAD_UNIQUE_MATCH));
        this.circuitOneToggle.setChecked(this.container.isEnabled(ContainerWcwtSettings.KEY_CIRCUIT_ALWAYS_ONE));
        this.stashToggle.setChecked(this.container.isEnabled(ContainerWcwtSettings.KEY_STASH_AMBIGUOUS));
        this.magnetToggle.setChecked(this.container.isEnabled(ContainerWcwtSettings.KEY_MAGNET_CARD));
        // The reference's one dependency: craft-if-missing has nothing to follow while the player
        // cannot pick blocks in the first place.
        this.pickBlockCraftToggle.enabled = this.pickBlockToggle.isChecked();
    }

    @Override
    public void drawFG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        this.fontRendererObj
            .drawString(StatCollector.translateToLocal("wtct.settings.title"), TITLE_X, TITLE_Y, 0x404040);
        this.fontRendererObj.drawString(
            StatCollector.translateToLocal("wtct.settings.upload_group"),
            TITLE_X,
            UPLOAD_GROUP_TITLE_Y,
            0x404040);
        this.fontRendererObj.drawString(
            StatCollector.translateToLocal("gui.ae2wtlib.magnet_settings_title"),
            TITLE_X,
            MAGNET_GROUP_TITLE_Y,
            0x404040);
        this.drawRowLabel("wtct.settings.pick_block", ROW_PICK_Y, true);
        this.drawRowLabel("wtct.settings.pick_block_craft", ROW_PICK_BLOCK_CRAFT_Y, this.pickBlockCraftToggle.enabled);
        this.drawRowLabel("wtct.settings.restock", ROW_RESTOCK_Y, true);
        this.drawRowLabel("wtct.settings.big_counts", ROW_BIG_COUNTS_Y, true);
        this.drawRowLabel("wtct.settings.pinned_auto_craft", ROW_PINNED_AUTO_CRAFT_Y, true);
        this.drawRowLabel("wtct.settings.crafting_notify", ROW_CRAFTING_NOTIFY_Y, true);
        this.drawRowLabel("wtct.settings.clear_grid_on_close", ROW_CLEAR_GRID_Y, true);
        this.drawRowLabel("wtct.settings.upload_unique", ROW_UPLOAD_UNIQUE_Y, true);
        this.drawRowLabel("wtct.settings.circuit_one", ROW_CIRCUIT_ONE_Y, true);
        this.drawRowLabel("wtct.settings.stash", ROW_STASH_Y, true);
        this.drawRowLabel("wtct.settings.magnet_card", ROW_MAGNET_Y, true);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** A switch's label, in the reference's own spot: right of the switch, centred on it. */
    private void drawRowLabel(final String key, final int rowY, final boolean active) {
        this.fontRendererObj.drawString(
            StatCollector.translateToLocal(key),
            ROW_X + LABEL_DX,
            rowY + LABEL_DY,
            active ? 0x404040 : MUTED_TEXT);
    }

    @Override
    public void drawBG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        // The modal panel, in the terminal's own palette: outer frame, face. The dimmed backdrop
        // behind it is GuiContainer's own drawDefaultBackground.
        drawRect(offsetX - 1, offsetY - 1, offsetX + PANEL_W + 1, offsetY + PANEL_H + 1, FRAME);
        drawRect(offsetX, offsetY, offsetX + PANEL_W, offsetY + PANEL_H, FACE);
    }

    @Override
    protected void actionPerformed(final GuiButton button) {
        if (button == this.backButton) {
            InventoryHandler.switchGui(GuiType.COMPREHENSIVE_WORK_TERMINAL);
            return;
        }
        final String key;
        // The two crafting switches live in NEI's global config rather than on the terminal item: both are
        // read while no terminal is open (a job pins its output at the moment it starts and its notification
        // arrives long after), so they cannot be carried by the terminal stack the way the others are.
        if (button == this.pinnedAutoCraftToggle) {
            final boolean now = !this.pinnedAutoCraftToggle.isChecked();
            NEI_TH_Config.setConfigValue(ButtonConstants.PINNED_AUTO_CRAFT, now);
            // Turning it off also drops what autocrafting already put up there - the player's own
            // favourites stay, they are a different pin.
            if (!now) {
                WtctAPI.instance()
                    .getPinned()
                    .clearCraftingPins();
            }
            this.refreshToggles();
            return;
        }
        if (button == this.craftingNotifyToggle) {
            NEI_TH_Config.setConfigValue(ButtonConstants.CRAFTING_NOTIFICATION, !this.craftingNotifyToggle.isChecked());
            this.refreshToggles();
            return;
        }
        if (button == this.pickBlockToggle) {
            key = ContainerWcwtSettings.KEY_PICK_BLOCK;
        } else if (button == this.pickBlockCraftToggle) {
            key = ContainerWcwtSettings.KEY_PICK_BLOCK_CRAFT;
        } else if (button == this.restockToggle) {
            key = ContainerWcwtSettings.KEY_RESTOCK;
        } else if (button == this.bigCountsToggle) {
            key = ContainerWcwtSettings.KEY_BIG_COUNTS;
        } else if (button == this.stashToggle) {
            key = ContainerWcwtSettings.KEY_STASH_AMBIGUOUS;
        } else if (button == this.clearGridToggle) {
            key = ContainerWcwtSettings.KEY_CLEAR_GRID_ON_CLOSE;
        } else if (button == this.uploadUniqueToggle) {
            key = ContainerWcwtSettings.KEY_UPLOAD_UNIQUE_MATCH;
        } else if (button == this.circuitOneToggle) {
            key = ContainerWcwtSettings.KEY_CIRCUIT_ALWAYS_ONE;
        } else if (button == this.magnetToggle) {
            key = ContainerWcwtSettings.KEY_MAGNET_CARD;
        } else {
            super.actionPerformed(button);
            return;
        }
        // Flip the local copy for the immediate redraw, then let the server flip the real stack.
        this.container.toggle(key);
        this.refreshToggles();
        final NBTTagCompound tag = new NBTTagCompound();
        tag.setString("Key", key);
        Wtct.proxy.netHandler.sendToServer(new CPacketTerminalBtns("CardConfig.SettingToggle", 0, tag));
    }
}
