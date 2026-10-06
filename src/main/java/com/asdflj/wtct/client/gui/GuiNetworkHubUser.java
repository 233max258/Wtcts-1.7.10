package com.asdflj.wtct.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import com.asdflj.wtct.Wtct;
import com.asdflj.wtct.client.gui.container.ContainerNetworkHub;
import com.asdflj.wtct.client.gui.nethub.IListItem;
import com.asdflj.wtct.client.gui.nethub.IconButton;
import com.asdflj.wtct.client.gui.nethub.ListCtrl;
import com.asdflj.wtct.client.gui.nethub.ListItem;
import com.asdflj.wtct.common.nethub.User;
import com.asdflj.wtct.util.NameConst;

/**
 * The member screen: every player on the server, plus the members already stored on this network's group, with their
 * permission level. Clicking a row moves that player up the ladder - or off the list, once they are an admin.
 */
public class GuiNetworkHubUser extends GuiContainer {

    private static final ResourceLocation BACKGROUND = new ResourceLocation(
        Wtct.MODID,
        "textures/gui/nethub/background.png");
    private static final ResourceLocation WIDGETS = new ResourceLocation(Wtct.MODID, "textures/gui/nethub/widgets.png");
    private static final String TITLE_KEY = "tile." + Wtct.MODID + "." + NameConst.BLOCK_NETWORK_HUB + ".name";

    private final ContainerNetworkHub containerNetworkHub;

    private ListCtrl<User> listCtrl;
    private IconButton netScreen;

    public GuiNetworkHubUser(ContainerNetworkHub containerNetworkHub) {
        super(containerNetworkHub);
        this.containerNetworkHub = containerNetworkHub;
        this.xSize = 200;
        this.ySize = 159;
    }

    @Override
    public void initGui() {
        super.initGui();

        this.netScreen = new IconButton(99998, guiLeft + 201, guiTop + 5, 40, 146);

        this.buttonList.add(this.netScreen);

        List<User> users = this.containerNetworkHub.getGroup()
            .getUsers();
        this.listCtrl = new ListCtrl<User>(this.mc, guiLeft + 20, guiTop + 15, 165, 136, 20, users) {

            @Override
            protected IListItem<User> getItem(Object id, String text, User o, int x, int y, int width, int height) {
                return new ListItem<User>(id, text, o, x, y, width, height) {

                    @Override
                    public void draw(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
                        int i = 1;
                        if (get().isOwner()) {
                            i = 5;
                        }
                        if (isMouseOver(mouseX, mouseY)) {
                            i = 2;
                        }
                        mc.getTextureManager()
                            .bindTexture(WIDGETS);
                        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                        drawTexturedModalRect(this.x, this.y + 3, 0, i * 14, this.width / 2, 14);
                        drawTexturedModalRect(
                            this.x + this.width / 2,
                            this.y + 3,
                            200 - this.width / 2,
                            i * 14,
                            this.width / 2,
                            14);
                        this.drawCenteredString(
                            mc.fontRenderer,
                            this.text + "[" + I18n.format(this.getPermText()) + "]",
                            this.x + this.width / 2,
                            this.y + (this.height - 8) / 2,
                            0xFFFFFFFF);
                    }

                    @Override
                    public List<String> getTooltip() {
                        List<String> tooltip = new ArrayList<>();
                        tooltip.add(this.getPermText());
                        return tooltip;
                    }

                    public String getPermText() {
                        if (get().isGuest()) {
                            return NameConst.GUI_NETWORK_HUB_USER_NONE;
                        }
                        if (get().isMember()) {
                            return NameConst.GUI_NETWORK_HUB_USER_USER;
                        }
                        if (get().isAdmin()) {
                            return NameConst.GUI_NETWORK_HUB_USER_ADMIN;
                        }
                        if (get().isOwner()) {
                            return NameConst.GUI_NETWORK_HUB_USER_OWNER;
                        }
                        return "";
                    }

                    @Override
                    public void click() {
                        UUID uuid = get().getUuid();
                        String name = get().getName();
                        if (uuid == null || name == null) return;
                        NBTTagCompound tag = new NBTTagCompound();
                        tag.setString("user", uuid.toString());
                        tag.setString("name", name);
                        containerNetworkHub.sendAction("change-player-permission", tag);
                    }
                };
            }
        };

        this.listCtrl.setScroll(true);
        this.listCtrl.setScrollByItem(true);
        this.listCtrl.setTooltipRenderer(
            (lines, mouseX, mouseY) -> this.drawHoveringText(lines, mouseX, mouseY, this.fontRendererObj));
        this.listCtrl.refresh();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);

        RenderHelper.disableStandardItemLighting();
        GL11.glDisable(GL11.GL_LIGHTING);

        if (containerNetworkHub.selectedNetwork != null) {
            this.listCtrl.draw(mouseX, mouseY, partialTicks);
        } else {
            this.drawCenteredString(
                this.fontRendererObj,
                I18n.format(NameConst.GUI_NETWORK_HUB_USER + "no_selected"),
                guiLeft + xSize / 2,
                100,
                0xFFFFFFFF);
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

        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        this.drawCenteredString(this.fontRendererObj, I18n.format(TITLE_KEY), xSize / 2, 4, 0xFFFFFFFF);
    }

    @Override
    protected void actionPerformed(GuiButton ba) {
        if (netScreen.id == ba.id) {
            this.mc.displayGuiScreen(new GuiNetworkHubCore(this.containerNetworkHub));
        }
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
        super.mouseClicked(mouseX, mouseY, mouseButton);
        listCtrl.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseMovedOrUp(int mouseX, int mouseY, int state) {
        super.mouseMovedOrUp(mouseX, mouseY, state);
        listCtrl.mouseReleased(mouseX, mouseY, state);
    }
}
