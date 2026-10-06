package com.asdflj.wtct.proxy;

import static net.minecraft.client.gui.GuiScreen.isShiftKeyDown;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.p455w0rd.wirelesscraftingterminal.client.gui.GuiWirelessCraftingTerminal;

import com.asdflj.wtct.Wtct;
import com.asdflj.wtct.api.MouseWheelHandler;
import com.asdflj.wtct.api.WtctAPI;
import com.asdflj.wtct.api.adapter.terminal.item.FCBaseItemTerminal;
import com.asdflj.wtct.api.adapter.terminal.item.FCUltraTerminal;
import com.asdflj.wtct.api.adapter.terminal.item.WCTWirelessCraftingTerminal;
import com.asdflj.wtct.api.adapter.terminal.parts.AETerminal;
import com.asdflj.wtct.client.event.AEGuiCloseEvent;
import com.asdflj.wtct.client.event.CraftTracking;
import com.asdflj.wtct.client.event.EncodeEvent;
import com.asdflj.wtct.client.event.GuiOverlayButtonEvent;
import com.asdflj.wtct.client.event.NeiHammerButtonHandler;
import com.asdflj.wtct.client.event.NotificationEvent;
import com.asdflj.wtct.client.event.OpenTerminalEvent;
import com.asdflj.wtct.client.event.UpdateAmountTextEvent;
import com.asdflj.wtct.client.gui.BaseMEGui;
import com.asdflj.wtct.client.gui.GuiBaseInterfaceWireless;
import com.asdflj.wtct.client.render.BlockPosHighlighter;
import com.asdflj.wtct.client.render.Notification;
import com.asdflj.wtct.integration.Mods;
import com.asdflj.wtct.loader.KeybindLoader;
import com.asdflj.wtct.loader.ListenerLoader;
import com.asdflj.wtct.loader.RenderLoader;
import com.asdflj.wtct.nei.recipes.DefaultExtractorLoader;
import com.asdflj.wtct.network.CPacketCraftRequest;
import com.asdflj.wtct.util.FindITUtil;

import appeng.api.events.GuiScrollEvent;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.client.gui.implementations.GuiCraftingTerm;
import appeng.client.gui.implementations.GuiMEMonitorable;
import appeng.client.gui.implementations.GuiPatternTerm;
import appeng.client.gui.implementations.GuiPatternTermEx;
import codechicken.nei.recipe.GuiOverlayButton;
import codechicken.nei.recipe.GuiRecipe;
import codechicken.nei.recipe.GuiRecipeButton;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;

public class ClientProxy extends CommonProxy {

    private static GuiOverlayButton overlayButton = null;
    public static List<MouseWheelHandler> mouseHandlers = new ArrayList<>();
    private static GuiBaseInterfaceWireless.InterfaceWirelessEntryWrapper entryWrapper = null;

    public static void setInterfaceHighlightEntry(
        GuiBaseInterfaceWireless.InterfaceWirelessEntryWrapper interfaceWirelessEntryWrapper) {
        entryWrapper = interfaceWirelessEntryWrapper;
    }

    public static GuiBaseInterfaceWireless.InterfaceWirelessEntryWrapper getInterfaceHighlightEntry() {
        return entryWrapper;
    }

    @Override
    public void onLoadComplete(FMLLoadCompleteEvent event) {
        super.onLoadComplete(event);
        if (Mods.NOT_ENOUGH_ITEMS.isModLoaded()) {
            new DefaultExtractorLoader().run();
            // The phial went with the rest of the Thaumcraft bridge, so there is nothing left to hide from NEI.
            if (Mods.FIND_IT.isModLoaded()) {
                FindITUtil.instance.run();
            }
        }
    }

    public static GuiOverlayButton getOverlayButton() {
        return overlayButton;
    }

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
    }

    @SubscribeEvent
    public void trackingMissingItems(CraftTracking c) {
        GuiScreen screen = Minecraft.getMinecraft().currentScreen;
        IItemList<IAEItemStack> list = c.getItems();
        if (!list.isEmpty() && WtctAPI.instance()
            .terminal()
            .isCraftingTerminal(screen)) {
            for (IAEItemStack is : list) {
                Wtct.proxy.netHandler.sendToServer(new CPacketCraftRequest(is, isShiftKeyDown()));
                is.reset();
                break;
            }
        }
    }

    @SubscribeEvent
    public void updateCraftAmount(UpdateAmountTextEvent amount) {
        amount.updateAmount();
    }

    @SubscribeEvent
    public boolean handleMouseWheelInput(GuiScrollEvent event) {
        if (mouseHandlers.isEmpty()) return false;
        for (MouseWheelHandler handler : mouseHandlers) {
            if (handler.handleMouseWheel(event, overlayButton)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        (new ListenerLoader()).run();
        (new RenderLoader()).run();
        (new KeybindLoader()).run();
        MinecraftForge.EVENT_BUS.register(new BlockPosHighlighter());
        // WCWT's hammer button in NEI's recipe panel (next to the "+").
        MinecraftForge.EVENT_BUS.register(new NeiHammerButtonHandler());
        WtctAPI.instance()
            .terminal()
            .registerTerminal(GuiMEMonitorable.class);
        WtctAPI.instance()
            .terminal()
            .registerTerminal(GuiCraftingTerm.class);
        WtctAPI.instance()
            .terminal()
            .registerTerminal(GuiPatternTerm.class);
        WtctAPI.instance()
            .terminal()
            .registerTerminal(GuiPatternTermEx.class);
        if (Mods.WIRELESS_CRAFTING_TERMINAL.isModLoaded()) {
            WtctAPI.instance()
                .terminal()
                .registerTerminal(GuiWirelessCraftingTerminal.class);
        }
        WtctAPI.instance()
            .terminal()
            .registerTerminalSet(FCBaseItemTerminal.instance);
        WtctAPI.instance()
            .terminal()
            .registerTerminalSet(FCUltraTerminal.instance);
        WtctAPI.instance()
            .terminal()
            .registerTerminalSet(WCTWirelessCraftingTerminal.instance);
        WtctAPI.instance()
            .terminal()
            .registerTerminalSet(new AETerminal());
    }

    private void placePattern() {
        // This used to drop a freshly encoded pattern into the interface terminal the player came from. That
        // terminal is gone, so there is nothing left to place it into.
    }

    @SubscribeEvent
    public void tickEvent(TickEvent.PlayerTickEvent event) {
        WtctAPI.instance()
            .getPinned()
            .updateCraftingItems();
        placePattern();
    }

    @SubscribeEvent
    public void encodeEvent(EncodeEvent event) {
        EncodeEvent.encode = true;
    }

    @SubscribeEvent
    public void onActionPerformedEventPost(GuiRecipeButton.UpdateRecipeButtonsEvent.Post event) {
        if (!(event.gui instanceof GuiRecipe<?>)) return;
        overlayButton = null;
        for (GuiRecipeButton btn : event.buttonList) {
            if (btn instanceof GuiOverlayButton gob) {
                gob.setRequireShiftForOverlayRecipe(false);
            }
        }
    }

    @SubscribeEvent
    public void onActionOverlayButton(GuiOverlayButtonEvent event) {
        overlayButton = event.getButton();
    }

    @SubscribeEvent
    public void initGuiEvent(GuiScreenEvent.InitGuiEvent.Post event) {
        if (event.gui instanceof BaseMEGui bg) {
            bg.initDone();
        }
        if (WtctAPI.instance()
            .terminal()
            .isCraftingTerminal(event.gui)) {
            MinecraftForge.EVENT_BUS.post(new CraftTracking());
        }
        if (UpdateAmountTextEvent.needUpdateAmountText()) {
            MinecraftForge.EVENT_BUS.post(new UpdateAmountTextEvent());
        }
    }

    @SubscribeEvent
    public void initGuiEvent(GuiScreenEvent.InitGuiEvent.Pre event) {
        if (WtctAPI.instance()
            .terminal()
            .isPinTerminal(event.gui)) {
            WtctAPI.instance()
                .getPinned()
                .prune();
        }
    }

    @SubscribeEvent
    public void aeBaseGuiClose(AEGuiCloseEvent event) {

    }

    @SubscribeEvent
    public void notificationEvent(NotificationEvent event) {
        Notification.INSTANCE.add(event);
    }

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type == RenderGameOverlayEvent.ElementType.ALL) {
            Notification.INSTANCE.draw();
        }
    }

    @SubscribeEvent
    public void openTerminalEvent(OpenTerminalEvent event) {
        event.openTerminal();
    }

    @SubscribeEvent
    public void onClientPostTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        // `WorldClient` is only available on the client-side, thus effectively checking if the game is running on
        // the client. We are only interested in highlighting slots when the player is in a GUI; the operation is
        // bound client-side.
        if (Minecraft.getMinecraft().theWorld == null) {
            return;
        }

        // We are only interested in GUIs that contain some kind of inventory.
        final GuiScreen screen = Minecraft.getMinecraft().currentScreen;
        if (!(screen instanceof GuiContainer)) {
            return;
        }
        if (Mods.FIND_IT.isModLoaded()) {
            FindITUtil.instance.highlighter();
        }
    }

    @SubscribeEvent
    public void ClientDisconnectionFromServerEvent(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        // Only the crafting pins die here: they are per-network state the server restates on the next
        // login. The favourites are the player's own, persisted across sessions next to the config
        // folder - clearing the whole table on logout used to wipe them (and, now that the favourites
        // are stored, to write that wipe over the store) which is exactly what made them "not save".
        WtctAPI.instance()
            .getPinned()
            .clearCraftingPins();
        Notification.INSTANCE.clear();
    }
}
