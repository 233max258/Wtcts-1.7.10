package com.asdflj.wtct.client.gui;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import com.asdflj.wtct.Wtct;
import com.asdflj.wtct.client.gui.container.ContainerWcwtTrash;
import com.asdflj.wtct.client.gui.widget.GuiWcwtBackButton;
import com.asdflj.wtct.inventory.InventoryHandler;
import com.asdflj.wtct.inventory.gui.GuiType;

import appeng.api.storage.ITerminalHost;
import appeng.client.gui.AEBaseGui;

/**
 * WCWT's trash screen - its {@code WcwtTrashScreen}: three rows of nine slots that destroy whatever
 * is left in them when the screen closes, over the player's inventory. WTLib's own background and
 * slot layout ({@code wtlib/trash.json}), unchanged.
 */
public class GuiWcwtTrash extends AEBaseGui {

    private static final String BG = "guis/wcwt/wtlib_trash_gui.png";

    private final ITerminalHost host;
    private GuiWcwtBackButton backButton;

    public GuiWcwtTrash(final InventoryPlayer ip, final ITerminalHost host) {
        super(new ContainerWcwtTrash(ip, host));
        this.host = host;
        this.xSize = ContainerWcwtTrash.GUI_WIDTH;
        this.ySize = ContainerWcwtTrash.GUI_HEIGHT;
    }

    @Override
    public void initGui() {
        super.initGui();
        this.xSize = ContainerWcwtTrash.GUI_WIDTH;
        this.ySize = ContainerWcwtTrash.GUI_HEIGHT;
        this.layoutPlayerSlots();
        // AE2 1.21's back tab (plate + arrow), the icon every screen of this mod closes with.
        this.buttonList.add(
            this.backButton = new GuiWcwtBackButton(
                0,
                this.guiLeft + ContainerWcwtTrash.GUI_WIDTH - 24,
                this.guiTop - 5));
    }

    /** Places the player's rows against the screen's bottom, exactly like the card config screens. */
    private void layoutPlayerSlots() {
        final int height = ContainerWcwtTrash.GUI_HEIGHT;
        for (final Object o : this.inventorySlots.inventorySlots) {
            if (!(o instanceof final appeng.container.slot.AppEngSlot s) || !isPlayerSlot(s)) {
                continue;
            }
            final boolean hotbar = s.getY() >= ContainerWcwtTrash.PLAYER_BIND_HOTBAR_Y;
            s.yDisplayPosition = hotbar ? ContainerWcwtTrash.GUI_HEIGHT - ContainerWcwtTrash.PLAYER_HOTBAR_BOTTOM
                : ContainerWcwtTrash.GUI_HEIGHT - ContainerWcwtTrash.PLAYER_INV_BOTTOM + s.getY();
        }
    }

    /** True for the 36 slots {@code bindPlayerInventory} added, terminal slot included. */
    private static boolean isPlayerSlot(final appeng.container.slot.AppEngSlot slot) {
        return slot instanceof appeng.container.slot.SlotPlayerInv
            || slot instanceof appeng.container.slot.SlotPlayerHotBar
            || slot instanceof appeng.container.slot.SlotDisabled;
    }

    @Override
    public void drawBG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        this.mc.getTextureManager()
            .bindTexture(new ResourceLocation(Wtct.MODID, "textures/" + BG));
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.drawTexturedModalRect(offsetX, offsetY, 0, 0, this.xSize, this.ySize);
    }

    @Override
    public void drawFG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        this.fontRendererObj.drawString(StatCollector.translateToLocal("gui.ae2wtlib.trash"), 8, 6, 0x404040);
        // WTLib's trash.json: the player's title at bottom 121.
        this.fontRendererObj
            .drawString(StatCollector.translateToLocal("container.inventory"), 8, this.ySize - 121, 0x404040);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    protected void actionPerformed(final net.minecraft.client.gui.GuiButton button) {
        if (button == this.backButton) {
            InventoryHandler.switchGui(GuiType.COMPREHENSIVE_WORK_TERMINAL);
            return;
        }
        super.actionPerformed(button);
    }
}
