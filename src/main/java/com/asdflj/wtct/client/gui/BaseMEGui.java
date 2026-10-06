package com.asdflj.wtct.client.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.asdflj.wtct.Wtct;
import com.asdflj.wtct.api.WtctAPI;
import com.asdflj.wtct.client.gui.widget.IGuiSelection;
import com.asdflj.wtct.integration.Mods;
import com.asdflj.wtct.nei.ButtonConstants;
import com.asdflj.wtct.nei.NEI_TH_Config;
import com.asdflj.wtct.network.CPacketFluidUpdate;
import com.asdflj.wtct.util.Ae2ReflectClient;
import com.asdflj.wtct.util.AspectUtil;
import com.asdflj.wtct.util.HBMAeAddonUtil;
import com.glodblock.github.common.item.ItemFluidDrop;
import com.glodblock.github.util.Util;

import appeng.api.config.SearchBoxMode;
import appeng.api.config.Settings;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.util.IConfigManager;
import appeng.api.util.IConfigurableObject;
import appeng.client.ActionKey;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.slots.VirtualMEMonitorableSlot;
import appeng.client.gui.slots.VirtualMESlot;
import appeng.core.AEConfig;
import appeng.core.CommonHelper;
import codechicken.nei.LayoutManager;
import codechicken.nei.util.TextHistory;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public abstract class BaseMEGui extends AEBaseGui implements IGuiSelection {

    protected IConfigManager configSrc;
    protected TextHistory history;
    protected final List<VirtualMEMonitorableSlot> meSlots = new ArrayList<>();

    public BaseMEGui(Container container) {
        super(container);
        this.configSrc = ((IConfigurableObject) this.inventorySlots).getConfigManager();
        this.history = Ae2ReflectClient.getHistory(LayoutManager.searchField);
    }

    public List<VirtualMEMonitorableSlot> getMeSlots() {
        return this.meSlots;
    }

    public void registerMESlot(VirtualMEMonitorableSlot slot) {
        this.meSlots.add(slot);
        this.registerVirtualSlots(slot);
    }

    protected boolean isNEISearch() {
        final Enum<?> s = AEConfig.instance.settings.getSetting(Settings.SEARCH_MODE);
        return s == SearchBoxMode.NEI_MANUAL_SEARCH || s == SearchBoxMode.NEI_AUTOSEARCH;
    }

    public boolean hasShiftDown() {
        return Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
    }

    protected boolean isFilledContainer(ItemStack is) {
        if (is == null) return false;
        return (Util.FluidUtil.isFluidContainer(is) && Util.FluidUtil.isFilled(is))
            || (Mods.THAUMIC_ENERGISTICS.isModLoaded() && AspectUtil.isEssentiaContainer(is)
                && !AspectUtil.isEmptyEssentiaContainer(is))
            || (Mods.HBM_AE_ADDON.isModLoaded() && HBMAeAddonUtil.getItemHasFluidType(is));
    }

    private boolean isEmptyContainer(ItemStack is, IAEFluidStack fs) {
        if (is == null) return false;
        return Util.FluidUtil.isEmpty(is)
            || (Mods.THAUMIC_ENERGISTICS.isModLoaded() && AspectUtil.isEssentiaContainer(is)
                && AspectUtil.isEmptyEssentiaContainer(is))
            || (Mods.HBM_AE_ADDON.isModLoaded() && HBMAeAddonUtil.getItemIsEmptyContainer(is, fs));
    }

    @SideOnly(Side.CLIENT)
    public boolean updateFluidContainer(VirtualMESlot slot, int ctrlDown, int mouseButton) {
        final EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (slot != null) {
            try {
                // Fluid insert/extract requires the interaction key (default LCONTROL — the same
                // bind AE2's own tooltips advertise), so plain clicks keep their normal
                // item-handling semantics: Ctrl+Left-click extracts, Ctrl+Right-click inserts.
                if (!appeng.server.ServerHelper.CONTAINER_INTERACTION_KEY.isKeyDown(player)) {
                    return false;
                }
                ItemStack cs = player.inventory.getItemStack();
                IAEItemStack item = slot.getAEStack() instanceof IAEItemStack ais ? ais : null;
                IAEFluidStack fluid = slot.getAEStack() instanceof IAEFluidStack afs ? afs : null;
                if (ctrlDown == 0) {
                    // Ctrl+Left-click with an empty container (or an empty hand): extract fluid
                    // from the network. The ME item grid shows network fluids either as raw
                    // IAEFluidStack entries (fed by SPacketMEFluidInvUpdate) or as ae2fc
                    // ItemFluidDrop pseudo items; both are extractable.
                    if (fluid == null && item != null
                        && item.getItem() != null
                        && item.getItem() instanceof ItemFluidDrop
                        && item.getStackSize() != 0) {
                        fluid = ItemFluidDrop.getAeFluidStack(item);
                    }
                    if (fluid != null && fluid.getStackSize() != 0 && (cs == null || isEmptyContainer(cs, fluid))) {
                        Wtct.proxy.netHandler.sendToServer(new CPacketFluidUpdate(fluid, isShiftKeyDown()));
                        return true;
                    }
                } else if (ctrlDown == 1 && isFilledContainer(cs)) {
                    // Ctrl+Right-click with a filled container: insert its fluid into the network.
                    Wtct.proxy.netHandler.sendToServer(new CPacketFluidUpdate(null, isShiftKeyDown()));
                    return true;
                }
                if (mouseButton == 3 && player.capabilities.isCreativeMode
                    && item != null
                    && !item.isCraftable()
                    && item.getItem() instanceof ItemFluidDrop) {
                    return false;
                }
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    // The pin/favourite marks are not drawn here: they belong behind the item icons, so they go out in
    // the foreground pass - see MixinAEBaseGui#wtct$drawPinnedSlots. Drawing them after
    // super.drawScreen() put them on top of the items instead, which is what turned a marked cell into
    // a black square while an item was held. Fluid container insert/extract hints are likewise not
    // drawn here anymore: AE2's own VirtualMEMonitorableSlot.addTooltip already renders the standard
    // "... to insert/extract ... from/to network" + "Hold Shift to process the entire stack" tooltip,
    // which is the single desired format.

    @Override
    public void drawHistorySelection(final int x, final int y, String text, int width,
        final List<String> searchHistory) {
        if (!NEI_TH_Config.getConfigValue(ButtonConstants.HISTORY)) return;
        final int maxRows = WtctAPI.maxSelectionRows;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        RenderHelper.disableStandardItemLighting();
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        String[] var4 = null;
        final List<String> history = new ArrayList<>(searchHistory);
        Collections.reverse(history);

        if (history.size() > maxRows) {
            for (int i = 1; i < history.size(); i++) {
                if (text.equals(history.get(i))) {
                    int max = Math.min(history.size(), i + maxRows - 1);
                    int min = Math.max(0, max - maxRows);
                    var4 = history.subList(min, max)
                        .toArray(new String[0]);
                    break;
                }
            }
        }
        if (var4 == null) {
            var4 = history.subList(0, Math.min(history.size(), 5))
                .toArray(new String[0]);
        }
        if (var4.length > 0) {
            int var5 = width;
            int var6;
            int var7;

            for (var6 = 0; var6 < var4.length; ++var6) {
                var7 = this.fontRendererObj.getStringWidth(var4[var6]) + 8;

                if (var7 > var5) {
                    var5 = var7;
                }
            }

            var6 = x + 3;
            var7 = y + 15;
            int var9 = 8;

            if (var4.length > 1) {
                var9 += 2 + (var4.length - 1) * 10;
            }

            if (this.guiTop + var7 + var9 + 6 > this.height) {
                var7 = this.height - var9 - this.guiTop - 6;
            }

            this.zLevel = 300.0F;
            itemRender.zLevel = 300.0F;
            final int var10 = -267386864;
            this.drawGradientRect(var6 - 3, var7 - 4, var6 + var5 + 3, var7 - 3, var10, var10);
            this.drawGradientRect(var6 - 3, var7 + var9 + 3, var6 + var5 + 3, var7 + var9 + 4, var10, var10);
            this.drawGradientRect(var6 - 3, var7 - 3, var6 + var5 + 3, var7 + var9 + 3, var10, var10);
            this.drawGradientRect(var6 - 4, var7 - 3, var6 - 3, var7 + var9 + 3, var10, var10);
            this.drawGradientRect(var6 + var5 + 3, var7 - 3, var6 + var5 + 4, var7 + var9 + 3, var10, var10);
            final int var11 = 1347420415;
            final int var12 = (var11 & 16711422) >> 1 | var11 & -16777216;
            this.drawGradientRect(var6 - 3, var7 - 3 + 1, var6 - 3 + 1, var7 + var9 + 3 - 1, var11, var12);
            this.drawGradientRect(var6 + var5 + 2, var7 - 3 + 1, var6 + var5 + 3, var7 + var9 + 3 - 1, var11, var12);
            this.drawGradientRect(var6 - 3, var7 - 3, var6 + var5 + 3, var7 - 3 + 1, var11, var11);
            this.drawGradientRect(var6 - 3, var7 + var9 + 2, var6 + var5 + 3, var7 + var9 + 3, var12, var12);

            for (int var13 = 0; var13 < var4.length; ++var13) {
                String var14 = var4[var13];
                if (var14.equals(text)) {
                    var14 = "> " + var14;
                    var14 = '\u00a7' + Integer.toHexString(15) + var14;
                } else {
                    var14 = "\u00a77" + var14;
                }

                this.fontRendererObj.drawStringWithShadow(var14, var6, var7, -1);

                if (var13 == 0) {
                    var7 += 2;
                }

                var7 += 10;
            }

            this.zLevel = 0.0F;
            itemRender.zLevel = 0.0F;
        }
        GL11.glPopAttrib();
    }

    public abstract int getOffsetY();

    public void initDone() {

    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (this instanceof IGuiMonitorTerminal gmt
            && CommonHelper.proxy.isActionKey(ActionKey.TOGGLE_FOCUS, keyCode)) {
            gmt.getSearchField()
                .setFocused(
                    !gmt.getSearchField()
                        .isFocused());
        } else {
            super.keyTyped(typedChar, keyCode);
        }
    }
}
