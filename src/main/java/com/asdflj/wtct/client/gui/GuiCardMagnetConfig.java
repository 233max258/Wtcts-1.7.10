package com.asdflj.wtct.client.gui;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import com.asdflj.wtct.Wtct;
import com.asdflj.wtct.client.gui.container.ContainerCardMagnetConfig;
import com.asdflj.wtct.client.gui.widget.GuiWcwtBackButton;
import com.asdflj.wtct.client.gui.widget.GuiWcwtIconButton;
import com.asdflj.wtct.common.item.card.CardTicker;
import com.asdflj.wtct.inventory.InventoryHandler;
import com.asdflj.wtct.inventory.gui.GuiType;
import com.asdflj.wtct.network.CPacketTerminalBtns;

import appeng.api.storage.ITerminalHost;
import appeng.client.gui.AEBaseGui;

/**
 * The magnet card's screen - AE2WTLib's {@code MagnetScreen}.
 *
 * <p>
 * WTLib's own layout: the pickup filter (three rows of nine), the button row under it, the insert
 * filter below that, then the player's inventory. Five of the six buttons are WTLib's
 * ({@code pickup_mode}, {@code copy_down}, {@code switch}, {@code copy_up}, {@code insert_mode}).
 * The sixth, on the left, is this port's addition: WTLib toggles the magnet itself with a hotkey,
 * and this terminal has no hotkey to spare, so the button cycles the same three states (off / to
 * inventory / into the network) the header button used to cycle.
 */
public class GuiCardMagnetConfig extends AEBaseGui {

    private static final String BG = "guis/wcwt/wtlib_magnet_gui.png";

    /** Set {@code -Dwtct.debug.magnet} to trace the magnet chain from the click to the tick. */
    private static final boolean DEBUG = Boolean.getBoolean("wtct.debug.magnet");

    // WTLib's magnet.json widget positions, unchanged.
    private static final int BUTTON_Y = 80;
    private static final int MAGNET_X = 8;
    private static final int PICKUP_MODE_X = 45;
    private static final int COPY_DOWN_X = 63;
    private static final int SWITCH_X = 81;
    private static final int COPY_UP_X = 99;
    private static final int INSERT_MODE_X = 117;

    // The icon sheet's sprites (WTLib's Icon enum).
    private static final int ICON_UP = 32;
    private static final int ICON_UP_V = 32;
    private static final int ICON_DOWN = 32;
    private static final int ICON_DOWN_V = 48;
    private static final int ICON_SWITCH = 32;
    private static final int ICON_SWITCH_V = 64;
    private static final int ICON_MAGNET = 0;
    private static final int ICON_MAGNET_V = 0;

    /** AE2's own hotbar binding offset, so the player's rows can be told apart. */
    private static final int PLAYER_BIND_HOTBAR_Y = 58;

    private final ContainerCardMagnetConfig container;

    private GuiWcwtIconButton pickupModeButton;
    private GuiWcwtIconButton insertModeButton;
    private GuiWcwtIconButton magnetButton;
    private GuiWcwtIconButton copyUpButton;
    private GuiWcwtIconButton copyDownButton;
    private GuiWcwtIconButton switchButton;
    private GuiWcwtBackButton backButton;

    public GuiCardMagnetConfig(final InventoryPlayer ip, final ITerminalHost host) {
        super(new ContainerCardMagnetConfig(ip, host));
        this.container = (ContainerCardMagnetConfig) this.inventorySlots;
        this.xSize = ContainerCardMagnetConfig.GUI_WIDTH;
        this.ySize = ContainerCardMagnetConfig.GUI_HEIGHT;
    }

    @Override
    public void initGui() {
        super.initGui();
        this.xSize = ContainerCardMagnetConfig.GUI_WIDTH;
        this.ySize = ContainerCardMagnetConfig.GUI_HEIGHT;
        this.layoutPlayerSlots();

        // The two mode buttons are built empty: their icon and tooltip are re-read from the card by
        // refreshButtons() before the first frame is drawn.
        this.buttonList.add(
            this.pickupModeButton = new GuiWcwtIconButton(
                this.guiLeft + PICKUP_MODE_X,
                this.guiTop + BUTTON_Y,
                GuiWcwtIconButton.ICON_NO_U,
                GuiWcwtIconButton.ICON_NO_V,
                ""));
        this.buttonList.add(
            this.copyDownButton = new GuiWcwtIconButton(
                this.guiLeft + COPY_DOWN_X,
                this.guiTop + BUTTON_Y,
                ICON_DOWN,
                ICON_DOWN_V,
                StatCollector.translateToLocal("wtct.card.magnet.copy_insert")));
        this.buttonList.add(
            this.switchButton = new GuiWcwtIconButton(
                this.guiLeft + SWITCH_X,
                this.guiTop + BUTTON_Y,
                ICON_SWITCH,
                ICON_SWITCH_V,
                StatCollector.translateToLocal("wtct.card.magnet.switch")));
        this.buttonList.add(
            this.copyUpButton = new GuiWcwtIconButton(
                this.guiLeft + COPY_UP_X,
                this.guiTop + BUTTON_Y,
                ICON_UP,
                ICON_UP_V,
                StatCollector.translateToLocal("wtct.card.magnet.copy_pickup")));
        this.buttonList.add(
            this.insertModeButton = new GuiWcwtIconButton(
                this.guiLeft + INSERT_MODE_X,
                this.guiTop + BUTTON_Y,
                GuiWcwtIconButton.ICON_NO_U,
                GuiWcwtIconButton.ICON_NO_V,
                ""));
        this.buttonList.add(
            this.magnetButton = new GuiWcwtIconButton(
                this.guiLeft + MAGNET_X,
                this.guiTop + BUTTON_Y,
                ICON_MAGNET,
                ICON_MAGNET_V,
                ""));
        // AE2 1.21's back tab (plate + arrow), the icon every screen of this mod closes with.
        this.buttonList.add(
            this.backButton = new GuiWcwtBackButton(
                0,
                this.guiLeft + ContainerCardMagnetConfig.GUI_WIDTH - 24,
                this.guiTop - 5));

        this.refreshButtons();
    }

    /** Places the player's rows against the screen's bottom, exactly like the card config screen. */
    private void layoutPlayerSlots() {
        final int height = ContainerCardMagnetConfig.GUI_HEIGHT;
        for (final Object o : this.inventorySlots.inventorySlots) {
            if (!(o instanceof final appeng.container.slot.AppEngSlot s) || !isPlayerSlot(s)) {
                continue;
            }
            final boolean hotbar = s.getY() >= PLAYER_BIND_HOTBAR_Y;
            s.yDisplayPosition = hotbar ? height - ContainerCardMagnetConfig.PLAYER_HOTBAR_BOTTOM
                : height - ContainerCardMagnetConfig.PLAYER_INV_BOTTOM + s.getY();
        }
    }

    /** True for the 36 slots {@code bindPlayerInventory} added, terminal slot included. */
    private static boolean isPlayerSlot(final appeng.container.slot.AppEngSlot slot) {
        return slot instanceof appeng.container.slot.SlotPlayerInv
            || slot instanceof appeng.container.slot.SlotPlayerHotBar
            || slot instanceof appeng.container.slot.SlotDisabled;
    }

    /**
     * Re-reads the card, so the two mode icons and the magnet tooltip follow its state. Called from
     * the screen tick rather than from the draw pass, because the server may have changed the card
     * between frames.
     */
    private void refreshButtons() {
        final boolean pickupWhitelist = this.container.isPickupWhitelist();
        final boolean insertWhitelist = this.container.isInsertWhitelist();
        if (this.pickupModeButton != null) {
            this.pickupModeButton.setIcon(
                pickupWhitelist ? GuiWcwtIconButton.ICON_YES_U : GuiWcwtIconButton.ICON_NO_U,
                pickupWhitelist ? GuiWcwtIconButton.ICON_YES_V : GuiWcwtIconButton.ICON_NO_V);
            this.pickupModeButton.setTooltip(filterTooltip("wtct.card.magnet.pickup_filter", pickupWhitelist));
        }
        if (this.insertModeButton != null) {
            this.insertModeButton.setIcon(
                insertWhitelist ? GuiWcwtIconButton.ICON_YES_U : GuiWcwtIconButton.ICON_NO_U,
                insertWhitelist ? GuiWcwtIconButton.ICON_YES_V : GuiWcwtIconButton.ICON_NO_V);
            this.insertModeButton.setTooltip(filterTooltip("wtct.card.magnet.insert_filter", insertWhitelist));
        }
        if (this.magnetButton != null) {
            final ItemStack terminal = this.container.getTerminalStack();
            final CardTicker.MagnetMode mode = terminal == null ? CardTicker.MagnetMode.OFF
                : CardTicker.MagnetMode.of(terminal);
            // A mode the master switch has overruled is worth saying out loud: the ticker stops at the
            // switch before it reads the mode, so a mode of "to inventory" on a switched-off terminal
            // does nothing at all, and the reason is nowhere else on this screen.
            final String key = !mode.equals(CardTicker.MagnetMode.OFF) && !this.container.isMagnetSwitchOn()
                ? "wtct.card.magnet.mode.off_switch"
                : "wtct.card.magnet.mode." + mode.name()
                    .toLowerCase();
            this.magnetButton.setTooltip(StatCollector.translateToLocal(key));
        }
    }

    /** WTLib's "Pickup Filter: Whitelist" line, assembled from the two parts. */
    private static String filterTooltip(final String filterKey, final boolean whitelist) {
        final String mode = StatCollector
            .translateToLocal(whitelist ? "wtct.card.magnet.whitelist" : "wtct.card.magnet.blacklist");
        return StatCollector.translateToLocalFormatted(filterKey, mode);
    }

    @Override
    public void drawBG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        this.mc.getTextureManager()
            .bindTexture(new ResourceLocation(Wtct.MODID, "textures/" + BG));
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        // The sheet is 256x256 and the screen is its top-left 176x256, so 1.7.10's own helper - which
        // always divides by 256 - is right for once.
        this.drawTexturedModalRect(offsetX, offsetY, 0, 0, this.xSize, this.ySize);
    }

    @Override
    public void drawFG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        this.fontRendererObj.drawString(
            StatCollector.translateToLocal("item.wcwt_card_magnet.name"),
            ContainerCardMagnetConfig.PICKUP_X,
            6,
            0x404040);
        this.fontRendererObj.drawString(
            StatCollector.translateToLocal("container.inventory"),
            ContainerCardMagnetConfig.PICKUP_X,
            this.ySize - 95,
            0x404040);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        this.refreshButtons();
    }

    @Override
    protected void actionPerformed(final net.minecraft.client.gui.GuiButton button) {
        if (DEBUG) {
            System.out.println(
                "[wtct-magnet] client actionPerformed: " + button.getClass()
                    .getSimpleName()
                    + " id="
                    + button.id
                    + " magnet="
                    + (button == this.magnetButton)
                    + " enabled="
                    + button.enabled
                    + " visible="
                    + button.visible);
        }
        if (button == this.backButton) {
            InventoryHandler.switchGui(GuiType.COMPREHENSIVE_WORK_TERMINAL);
            return;
        }
        // The magnet's own mode lives on the terminal item, which is the same key the backpack
        // terminal's magnet uses - so it rides the packet the header button has always sent.
        //
        // Two things happen on a click. The screen flips its own copy first, because the server's
        // flip cannot get back to it: the terminal is normally worn in a Baubles slot, no slot of
        // this container maps to that column, and so the tooltip would otherwise still name the mode
        // the screen was opened on - the button looked dead. And the master switch is turned on,
        // because a mode of "to inventory" with the switch off (something the settings screen can
        // leave behind) makes the ticker return before it ever reads the mode, which looks exactly
        // the same from here. The packet carries no payload, so the server steps its own stack from
        // the same value and the two stay in step.
        if (button == this.magnetButton) {
            this.container.enableMagnetSwitch();
            this.container.flipMagnetModeLocally();
            this.refreshButtons();
            if (DEBUG) {
                System.out.println(
                    "[wtct-magnet] client sending CardConfig.CycleMagnet, local mode now="
                        + CardTicker.MagnetMode.of(this.container.getTerminalStack()));
            }
            Wtct.proxy.netHandler
                .sendToServer(new CPacketTerminalBtns("CardConfig.CycleMagnet", 0, new NBTTagCompound()));
            return;
        }
        final int action;
        if (button == this.pickupModeButton) {
            action = ACTION_PICKUP_MODE;
        } else if (button == this.insertModeButton) {
            action = ACTION_INSERT_MODE;
        } else if (button == this.copyUpButton) {
            action = ACTION_COPY_UP;
        } else if (button == this.copyDownButton) {
            action = ACTION_COPY_DOWN;
        } else if (button == this.switchButton) {
            action = ACTION_SWITCH;
        } else {
            super.actionPerformed(button);
            return;
        }
        // The screen's own copy of the card, not the server's: the container was built when the
        // screen opened, so its "live" stack never sees the server's reply while it stays open. Flip
        // it here for the immediate redraw, then let the server flip the real stack.
        if (action == ACTION_PICKUP_MODE) {
            this.container.togglePickupMode();
        } else if (action == ACTION_INSERT_MODE) {
            this.container.toggleInsertMode();
        }
        this.refreshButtons();
        Wtct.proxy.netHandler
            .sendToServer(new CPacketTerminalBtns("CardConfig.MagnetAction", action, new NBTTagCompound()));
    }

    // WTLib's five magnet-screen actions, as the packet's payload.
    public static final int ACTION_PICKUP_MODE = 0;
    public static final int ACTION_INSERT_MODE = 1;
    public static final int ACTION_COPY_UP = 2;
    public static final int ACTION_COPY_DOWN = 3;
    public static final int ACTION_SWITCH = 4;
}
