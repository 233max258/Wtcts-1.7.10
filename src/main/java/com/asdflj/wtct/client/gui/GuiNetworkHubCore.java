package com.asdflj.wtct.client.gui;

import java.util.Collections;
import java.util.UUID;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import com.asdflj.wtct.Wtct;
import com.asdflj.wtct.client.gui.container.ContainerNetworkHub;
import com.asdflj.wtct.client.gui.nethub.IListItem;
import com.asdflj.wtct.client.gui.nethub.IconButton;
import com.asdflj.wtct.client.gui.nethub.ListCtrl;
import com.asdflj.wtct.client.gui.nethub.ListItem;
import com.asdflj.wtct.client.gui.nethub.MyButton;
import com.asdflj.wtct.client.gui.nethub.MyLockIconButton;
import com.asdflj.wtct.client.gui.nethub.MyTextField;
import com.asdflj.wtct.client.gui.nethub.StringRenderUtil;
import com.asdflj.wtct.common.nethub.BlockPosDim;
import com.asdflj.wtct.common.nethub.NetHubUtil;
import com.asdflj.wtct.common.nethub.Network;
import com.asdflj.wtct.common.tile.TileNetworkHub;
import com.asdflj.wtct.util.NameConst;

/**
 * The network screen: the list of networks on the left, the selected network's details on the right, and the buttons
 * that act on it along the bottom.
 *
 * <p>
 * Every button is an action on {@link ContainerNetworkHub}; nothing is resolved here, so the screen never has to guess
 * what the server would have allowed.
 */
public class GuiNetworkHubCore extends GuiContainer {

    private static final ResourceLocation BACKGROUND = new ResourceLocation(
        Wtct.MODID,
        "textures/gui/nethub/background.png");
    private static final ResourceLocation FRAME = new ResourceLocation(
        Wtct.MODID,
        "textures/gui/nethub/network_hub.png");
    private static final String TITLE_KEY = "tile." + Wtct.MODID + "." + NameConst.BLOCK_NETWORK_HUB + ".name";

    /** Kept across screens so reopening the hub does not lose the filter the player typed. */
    private static String searchNet = "";

    private final ContainerNetworkHub containerNetworkHub;
    private ListCtrl<Network> listCtrl;
    private GuiTextField searchField;
    private MyLockIconButton lockButton;
    private GuiButton createButton;
    private GuiButton deleteButton;
    private GuiButton connectButton;
    private GuiButton disConnectButton;
    private MyTextField createField;
    private boolean isCreating = false;

    private IconButton userScreen;

    public GuiNetworkHubCore(InventoryPlayer ip, TileNetworkHub hub) {
        this(new ContainerNetworkHub(ip, hub));
    }

    /**
     * Reuses the container the member screen already holds. Going through the two-argument constructor instead would
     * build a second client-side container that the server knows nothing about, and every action would go nowhere.
     */
    public GuiNetworkHubCore(ContainerNetworkHub containerNetworkHub) {
        super(containerNetworkHub);
        this.containerNetworkHub = containerNetworkHub;
        this.xSize = 200;
        this.ySize = 159;
    }

    @Override
    public void initGui() {
        super.initGui();

        this.userScreen = new IconButton(99999, guiLeft + 201, guiTop + 5, 60, 146);

        this.listCtrl = new ListCtrl<Network>(
            this.mc,
            guiLeft + 7,
            guiTop + 35,
            86,
            116,
            20,
            this.containerNetworkHub.networks.values()) {

            @Override
            protected IListItem<Network> getItem(Object id, String text, Network o, int x, int y, int width,
                int height) {
                return new ListItem<Network>(id, text, o, x, y, width, height) {

                    @Override
                    public void click() {
                        UUID uuid = get().getUuid();
                        if (uuid == null) return;
                        containerNetworkHub.sendAction("switch-network", uuid);
                    }
                };
            }
        };
        this.listCtrl.setScroll(true);
        this.listCtrl.setIsSelected(true);
        this.listCtrl.setFilter(searchNet);
        this.listCtrl.setSelected(containerNetworkHub.selectedNetwork);
        this.listCtrl.setScrollByItem(true);
        this.listCtrl.setTooltipRenderer(
            (lines, mouseX, mouseY) -> this.drawHoveringText(lines, mouseX, mouseY, this.fontRendererObj));
        this.listCtrl.refresh();

        this.searchField = new GuiTextField(this.fontRendererObj, guiLeft + 9, guiTop + 17, 85, 11);
        this.searchField.setVisible(true);
        this.searchField.setMaxStringLength(10);
        this.searchField.setEnableBackgroundDrawing(false);
        this.searchField.setTextColor(16777215);
        this.searchField.setText(searchNet);
        this.searchField.setFocused(false);

        this.createButton = new MyButton(
            995,
            guiLeft + 105,
            guiTop + 113,
            37,
            14,
            I18n.format(NameConst.GUI_NETWORK_HUB_BUTTON + "create"));
        this.deleteButton = new MyButton(
            996,
            guiLeft + 105,
            guiTop + 133,
            37,
            14,
            I18n.format(NameConst.GUI_NETWORK_HUB_BUTTON + "delete"));
        this.connectButton = new MyButton(
            997,
            guiLeft + 151,
            guiTop + 113,
            37,
            14,
            I18n.format(NameConst.GUI_NETWORK_HUB_BUTTON + "connect"));
        this.disConnectButton = new MyButton(
            998,
            guiLeft + 151,
            guiTop + 133,
            37,
            14,
            I18n.format(NameConst.GUI_NETWORK_HUB_BUTTON + "disconnect"));
        this.lockButton = new MyLockIconButton(999, guiLeft + 201, guiTop + 25);
        this.createField = new MyTextField(this.mc, this.fontRendererObj, guiLeft + 105, guiTop + 113, 82, 14);

        if (this.containerNetworkHub.hub.isConnected()) {
            this.createButton.enabled = false;
        }

        if (this.containerNetworkHub.hub.isHead()) {
            this.createButton.enabled = false;
            this.connectButton.enabled = false;
            this.disConnectButton.enabled = false;
        }

        this.lockButton.enabled = containerNetworkHub.selectedNetwork != null;
        this.lockButton.setLocked(
            !this.selected()
                .isOvert());

        this.createField.setVisible(false);
        this.createField.setMaxStringLength(10);
        this.createField.setEnableBackgroundDrawing(false);

        this.buttonList.add(this.userScreen);
        this.buttonList.add(this.createButton);
        this.buttonList.add(this.deleteButton);
        this.buttonList.add(this.connectButton);
        this.buttonList.add(this.disConnectButton);
        this.buttonList.add(this.lockButton);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);

        RenderHelper.disableStandardItemLighting();
        GL11.glDisable(GL11.GL_LIGHTING);

        boolean isSelected = containerNetworkHub.selectedNetwork != null;
        boolean isHead = this.containerNetworkHub.hub.isHead();
        boolean isConnected = this.containerNetworkHub.hub.isConnected();
        this.lockButton.setLocked(
            !this.selected()
                .isOvert());
        this.lockButton.enabled = isSelected;
        this.createButton.enabled = !isHead && !isConnected;
        this.disConnectButton.enabled = isSelected && !isHead && isConnected;
        this.connectButton.enabled = isSelected && !isHead;
        this.deleteButton.enabled = isSelected;

        this.listCtrl.draw(mouseX, mouseY, partialTicks);

        this.createField.drawTextBox();
        this.searchField.drawTextBox();

        if (this.isMouseOverButton(lockButton, mouseX, mouseY)) {
            this.drawHoveringText(
                Collections.singletonList(I18n.format(NameConst.GUI_NETWORK_HUB_BUTTON + "public.desc")),
                mouseX,
                mouseY,
                this.fontRendererObj);
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        this.mc.getTextureManager()
            .bindTexture(BACKGROUND);
        this.drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);

        this.mc.getTextureManager()
            .bindTexture(FRAME);
        this.drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);

        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    private Network selected() {
        return containerNetworkHub.getSelected();
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        this.drawCenteredString(this.fontRendererObj, I18n.format(TITLE_KEY), xSize / 2, 4, 0xFFFFFFFF);

        int rightPanelX = 103;
        int rightPanelY = 19;
        String name = this.selected()
            .getName();
        Integer surplusChannels = this.containerNetworkHub.surplusChannels;
        BlockPosDim posDim = this.selected()
            .getSendPos();
        boolean overt = this.selected()
            .isOvert();
        boolean connected = containerNetworkHub.hub.isConnected();
        long power = (long) containerNetworkHub.hub.getPower();

        float scale = 1.0F;
        if (!"zh_cn".equals(mc.gameSettings.language)) {
            scale = 0.7f;
        }
        StringRenderUtil.drawString(
            this.fontRendererObj,
            I18n.format(NameConst.GUI_NETWORK_HUB_INFO + "network_name") + " " + name,
            rightPanelX,
            rightPanelY,
            0xFFFFFF,
            scale);
        StringRenderUtil.drawString(
            this.fontRendererObj,
            I18n.format(NameConst.GUI_NETWORK_HUB_INFO + "surplus_channels") + " "
                + (surplusChannels == null ? "Unknown" : surplusChannels),
            rightPanelX,
            rightPanelY += 12,
            0xFFFFFF,
            scale);
        StringRenderUtil.drawString(
            this.fontRendererObj,
            I18n.format(NameConst.GUI_NETWORK_HUB_INFO + "dimension_id") + " "
                + (posDim == null ? "Unknown" : posDim.getDimension()),
            rightPanelX,
            rightPanelY += 12,
            0xFFFFFF,
            scale);
        StringRenderUtil.drawString(
            this.fontRendererObj,
            I18n.format(NameConst.GUI_NETWORK_HUB_INFO + "public." + overt),
            rightPanelX,
            rightPanelY += 12,
            0xFFFFFF,
            scale);
        StringRenderUtil.drawString(
            this.fontRendererObj,
            I18n.format(NameConst.GUI_NETWORK_HUB_INFO + "state." + connected),
            rightPanelX,
            rightPanelY += 12,
            0xFFFFFF,
            scale);
        StringRenderUtil.drawString(
            this.fontRendererObj,
            I18n.format(NameConst.GUI_NETWORK_HUB_INFO + "power") + " " + NetHubUtil.formatCompact(power) + "RF/t",
            rightPanelX,
            rightPanelY + 12,
            0xFFFFFF,
            scale);
    }

    @Override
    protected void actionPerformed(GuiButton ba) {
        if (createButton.id == ba.id) {
            this.createButton.enabled = false;
            this.createButton.visible = false;
            this.connectButton.visible = false;
            this.isCreating = true;
            this.createField.setVisible(true);
            this.createField.setFocused(true);
            return;
        }

        if (userScreen.id == ba.id) {
            this.mc.displayGuiScreen(new GuiNetworkHubUser(this.containerNetworkHub));
        }

        if (containerNetworkHub.selectedNetwork == null) {
            return;
        }

        if (ba.id == deleteButton.id) {
            containerNetworkHub.sendAction("delete-network");
        }

        if (ba.id == connectButton.id) {
            containerNetworkHub.sendAction("connect-network");
        }

        if (ba.id == disConnectButton.id) {
            containerNetworkHub.sendAction("disconnect-network");
        }

        if (ba.id == lockButton.id) {
            containerNetworkHub.sendAction("switch-public");
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (isCreating) {
            this.createField.textboxKeyTyped(typedChar, keyCode);
            if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
                this.connectButton.enabled = false;
                this.disConnectButton.enabled = false;
                this.createButton.visible = true;
                this.connectButton.visible = true;
                this.isCreating = false;
                this.createField.setVisible(false);
                containerNetworkHub.sendAction("create-network", createField.getText());
                this.createField.setText("");
            }
            return;
        }
        if (searchField.isFocused()) {
            if (this.searchField.textboxKeyTyped(typedChar, keyCode)) {
                searchNet = this.searchField.getText();
                this.listCtrl.setFilter(searchNet);
                this.listCtrl.setScrollOffset(0);
                this.listCtrl.refresh();
            }
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int i = Mouse.getEventX() * this.width / this.mc.displayWidth;
        int j = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
        listCtrl.handleMouseInput(i, j);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        listCtrl.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (isCreating && !isMouseOverTextField(this.createField, mouseX, mouseY)) {
            this.createButton.enabled = true;
            this.createButton.visible = true;
            this.connectButton.visible = true;
            this.isCreating = false;
            this.createField.setVisible(false);
            return;
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
        listCtrl.mouseClicked(mouseX, mouseY, mouseButton);
        this.searchField.setFocused(isMouseOverTextField(this.searchField, mouseX, mouseY));
    }

    @Override
    protected void mouseMovedOrUp(int mouseX, int mouseY, int state) {
        super.mouseMovedOrUp(mouseX, mouseY, state);
        listCtrl.mouseReleased(mouseX, mouseY, state);
    }

    private boolean isMouseOverTextField(GuiTextField textField, int mouseX, int mouseY) {
        return mouseX >= textField.xPosition && mouseX < textField.xPosition + textField.width
            && mouseY >= textField.yPosition
            && mouseY < textField.yPosition + textField.height;
    }

    private boolean isMouseOverButton(GuiButton button, int mouseX, int mouseY) {
        return mouseX >= button.xPosition && mouseY >= button.yPosition
            && mouseX < button.xPosition + button.width
            && mouseY < button.yPosition + button.height;
    }
}
