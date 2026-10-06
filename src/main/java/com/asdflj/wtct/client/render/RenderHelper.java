package com.asdflj.wtct.client.render;

import static net.minecraft.client.gui.Gui.drawRect;
import static net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting;

import java.awt.Color;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.asdflj.wtct.api.Pinned;
import com.asdflj.wtct.api.WtctAPI;
import com.asdflj.wtct.client.gui.BaseMEGui;
import com.asdflj.wtct.util.Util;

import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.client.gui.slots.VirtualMEMonitorableSlot;
import appeng.client.gui.slots.VirtualMESlot;
import appeng.core.AppEng;

public class RenderHelper {

    /**
     * The pin mark is AE2's own icon sheet - the very cell its native {@code VirtualMEPinSlot} draws -
     * so a row of the terminal's autocrafting pins is marked exactly like a stock ME terminal's.
     */
    private static final ResourceLocation PIN_ICON_TEXTURE = new ResourceLocation(
        AppEng.MOD_ID,
        "textures/guis/states.png");
    private static final int PIN_ICON_INDEX = 5 * 16 + 14;
    private static final int PIN_ICON_SIZE = 16;
    private static final float PIN_ICON_OPACITY = 0.4F;
    /** The translucent yellow AE2 Favorites fills a favourite cell with. */
    private static final int FAVORITE_BACKGROUND = 0x50FFFF00;

    private static Color color;
    private static long lastRunTime;
    public static long interval = 30;
    public static boolean canDrawPlus = false;
    public static RenderItem itemRender = new RenderItem();

    public static void renderAEStack(IAEStack<?> stack, int x, int y, float z) {
        renderAEStack(stack, x, y, z, true);
    }

    public static void renderItemStack(ItemStack stack, int x, int y, float z) {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        enableGUIStandardItemLighting();
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glTranslatef(0f, 0f, z);
        itemRender.renderItemAndEffectIntoGUI(
            Minecraft.getMinecraft().fontRenderer,
            Minecraft.getMinecraft()
                .getTextureManager(),
            stack,
            x,
            y);
        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }

    public static void renderAEStack(IAEStack<?> stack, int x, int y, float z, boolean renderStackSize) {
        if (stack == null) return;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        enableGUIStandardItemLighting();
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glTranslatef(0f, 0f, z);
        Minecraft mc = Minecraft.getMinecraft();
        stack.drawInGui(mc, x, y);
        if (renderStackSize) {
            stack.drawOverlayInGui(mc, x, y, true, true, true, true);
        }
        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }

    public static void updateColor() {
        color = getDynamicColor();
    }

    private static Color getDynamicColor() {
        long time = System.currentTimeMillis();
        if (time - lastRunTime >= interval || color == null) {
            lastRunTime = time;
            float hue = (time % 2000) / 2000.0F;
            Color c = Color.getHSBColor(hue, 1.0F, 1.0F);
            return new Color(c.getRed(), c.getGreen(), c.getBlue(), 128);
        } else {
            return color;
        }
    }

    public static void drawItemBorder(int x, int y) {
        if (color == null) return;
        int width = 16;
        int height = 16;
        GL11.glPushMatrix();
        GL11.glTranslatef(0.0f, 0.0f, 250.0f);
        drawRect(x - 1, y - 1, x + width + 1, y, color.getRGB());
        drawRect(x - 1, y + height + 1, x + width + 1, y + height, color.getRGB());
        drawRect(x - 1, y, x, y + height, color.getRGB());
        drawRect(x + width, y, x + width + 1, y + height, color.getRGB());
        GL11.glTranslatef(0.0f, 0.0f, -250.0f);
        GL11.glPopMatrix();
    }

    public static void updateColorAndDrawItemBorder(int x, int y) {
        updateColor();
        drawItemBorder(x, y);
    }

    /**
     * Marks the pinned cells of the ME grid. Called from the foreground pass, before the item icons are
     * drawn, which is what makes every mark here a real background: nothing is painted over an item,
     * and no lighting or depth state left behind by the item pass can turn a marked cell black.
     *
     * <p>
     * The two marks follow the two blocks {@code MixinItemRepo} lays out. The autocrafting pins get
     * AE2's own white pin on every cell of their row; a favourite gets the translucent yellow fill AE2
     * Favorites uses. Neither one adds a border - a border on a cell is drawn by nothing here.
     */
    public static void drawPinnedSlots(BaseMEGui gui, List<VirtualMEMonitorableSlot> slots) {
        if (!WtctAPI.instance()
            .terminal()
            .isPinTerminal(gui)) {
            return;
        }
        final Pinned pinned = WtctAPI.instance()
            .getPinned();
        if (slots.isEmpty() || pinned.isEmpty()) {
            return;
        }
        // The pin row is wherever the autocrafting pins actually are: they are ordinary view entries,
        // so scrolling carries them away and a row baked into the top of the grid would then lie.
        int pinRowY = Integer.MAX_VALUE;
        for (VirtualMESlot slot : slots) {
            if (slot.isHidden()) continue;
            if (isReason(pinned, slot, Pinned.PinReason.CRAFTING) && slot.getY() < pinRowY) {
                pinRowY = slot.getY();
            }
        }

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        // A background takes no part in depth testing: the item icons follow in the same pass and would
        // otherwise be rejected against the depth written here.
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        for (VirtualMESlot slot : slots) {
            if (slot.isHidden()) continue;
            if (isReason(pinned, slot, Pinned.PinReason.FAVORITE)) {
                drawFavoriteBackground(slot.getX(), slot.getY());
            }
        }
        if (pinRowY != Integer.MAX_VALUE) {
            drawPinRow(slots, pinRowY);
        }

        GL11.glPopAttrib();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static boolean isReason(final Pinned pinned, final VirtualMESlot slot, final Pinned.PinReason reason) {
        final IAEStack<?> stack = slot.getAEStack();
        if (!(stack instanceof IAEItemStack item)) {
            return false;
        }
        final Pinned.PinInfo info = pinned.getPinInfo(item);
        return info != null && info.reason == reason;
    }

    /**
     * AE2's pinned-row mark: the white pin of {@code guis/states.png}, drawn on every cell of the
     * autocrafting row - including the padding cells that keep the row to itself - at 0.4 opacity and
     * behind the items, so an empty cell says what it is while a filled one still shows its item.
     */
    private static void drawPinRow(final List<VirtualMEMonitorableSlot> slots, final int rowY) {
        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(PIN_ICON_TEXTURE);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        final float u1 = (float) (PIN_ICON_INDEX % 16 * PIN_ICON_SIZE) / 256.0F;
        final float v1 = (float) (PIN_ICON_INDEX / 16 * PIN_ICON_SIZE) / 256.0F;
        final float u2 = u1 + (float) PIN_ICON_SIZE / 256.0F;
        final float v2 = v1 + (float) PIN_ICON_SIZE / 256.0F;
        final Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();
        tess.setColorRGBA_F(1.0F, 1.0F, 1.0F, PIN_ICON_OPACITY);
        for (VirtualMESlot slot : slots) {
            if (slot.isHidden() || slot.getY() != rowY) {
                continue;
            }
            final float x = slot.getX();
            final float y = slot.getY();
            tess.addVertexWithUV(x, y + PIN_ICON_SIZE, 0.0D, u1, v2);
            tess.addVertexWithUV(x + PIN_ICON_SIZE, y + PIN_ICON_SIZE, 0.0D, u2, v2);
            tess.addVertexWithUV(x + PIN_ICON_SIZE, y, 0.0D, u2, v1);
            tess.addVertexWithUV(x, y, 0.0D, u1, v1);
        }
        tess.setColorRGBA_F(1.0F, 1.0F, 1.0F, 1.0F);
        tess.draw();
    }

    private static void drawFavoriteBackground(final int x, final int y) {
        drawRect(x - 1, y - 1, x + 17, y + 17, FAVORITE_BACKGROUND);
    }

    public static void drawPlus(int x, int y) {
        float startX = x + 0.5f;
        float startY = y + 0.25f;
        float endX = startX + 3f;
        float endY = startY + 3f;
        GL11.glPushMatrix();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glTranslatef(0f, 0f, 250);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glLineWidth(3.0F);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2f(startX, startY + 1.5f);
        GL11.glVertex2f(endX, startY + 1.5f);
        GL11.glEnd();
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2f(startX + 1.5f, startY);
        GL11.glVertex2f(startX + 1.5f, endY);
        GL11.glEnd();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glTranslatef(0f, 0f, -250);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glPopMatrix();
    }

    public static void disableStandardItemLighting() {
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_LIGHT0);
        GL11.glDisable(GL11.GL_LIGHT1);
        GL11.glDisable(GL11.GL_COLOR_MATERIAL);
    }

    private static void setCanDrawPlus() {
        canDrawPlus = Util.getAEVersion() < 536;
    }

    static {
        setCanDrawPlus();
    }
}
