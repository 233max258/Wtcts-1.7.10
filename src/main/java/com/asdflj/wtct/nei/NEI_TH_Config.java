package com.asdflj.wtct.nei;

import java.util.ArrayList;
import java.util.List;

import com.asdflj.wtct.Tags;
import com.asdflj.wtct.Wtct;
import com.asdflj.wtct.client.gui.GuiComprehensiveWorkTerminal;
import com.asdflj.wtct.integration.Mods;
import com.asdflj.wtct.nei.recipes.FluidRecipe;
import com.github.vfyjxf.nee.nei.NEETerminalBookmarkContainerHandler;

import codechicken.lib.config.ConfigTagParent;
import codechicken.nei.NEIClientConfig;
import codechicken.nei.api.API;
import codechicken.nei.api.IConfigureNEI;

@SuppressWarnings("unused")
public class NEI_TH_Config implements IConfigureNEI {

    private static final ConfigTagParent tag = NEIClientConfig.global.config;

    @Override
    public void loadConfig() {
        API.registerNEIGuiHandler(new AE2TH_NEIGuiHandler());
        List<String> recipes = new ArrayList<>();
        recipes.add("crafting");
        recipes.add("crafting2x2");
        for (String identifier : recipes) {
            // The comprehensive work terminal's "+" encodes the recipe into its encoding area; the
            // materials are pulled into the manual crafting grid by the hammer button or by Shift+click,
            // which this handler routes itself. AE2Things reached the same handler through the
            // null-handler fallback, so the "+" must not be handed to CraftingTransferHandler - that one
            // writes the manual grid, and it was only ever registered for the backpack terminal's
            // crafting grid, which is no longer part of this mod.
            if (!API.hasGuiOverlayHandler(GuiComprehensiveWorkTerminal.class, identifier)) {
                API.registerGuiOverlayHandler(
                    GuiComprehensiveWorkTerminal.class,
                    PatternTerminalRecipeTransferHandler.INSTANCE,
                    identifier);
            }
        }
        for (String identifier : FluidRecipe.getSupportRecipes()) {
            // The comprehensive work terminal is the only pattern terminal left, so every recipe this mod
            // supports is registered against it.
            if (!API.hasGuiOverlayHandler(GuiComprehensiveWorkTerminal.class, identifier)) {
                API.registerGuiOverlayHandler(
                    GuiComprehensiveWorkTerminal.class,
                    PatternTerminalRecipeTransferHandler.INSTANCE,
                    identifier);
            }
        }
        API.addOption(new BaseToggleButton(ButtonConstants.HISTORY, false));
        API.addOption(new BaseToggleButton(ButtonConstants.INVENTORY_STATE));
        API.addOption(new BaseToggleButton(ButtonConstants.ULTRA_TERMINAL_MODE));
        API.addOption(new BaseToggleButton(ButtonConstants.DUAL_INTERFACE_TERMINAL, false));
        API.addOption(new BaseToggleButton(ButtonConstants.DUAL_INTERFACE_TERMINAL_APPEND_CIRCUIT_DAMAGE));
        API.addOption(new BaseToggleButton(ButtonConstants.CRAFTING_NOTIFICATION));
        API.addOption(new BaseToggleButton(ButtonConstants.NEI_CRAFT_ITEM));
        if (Mods.PROGRAMMABLE_HATCHES.isModLoaded()) {
            API.addOption(new BaseToggleButton(ButtonConstants.DUAL_INTERFACE_TERMINAL_FILL_CIRCUIT, false));
        }
        if (Mods.BLOCK_RENDERER.isModLoaded()) {
            API.addOption(new BaseToggleButton(ButtonConstants.BLOCK_RENDER));
        }
        if (Mods.NOT_ENOUGH_ENERGISTICS.isModLoaded()) {
            API.registerBookmarkContainerHandler(
                GuiComprehensiveWorkTerminal.class,
                NEETerminalBookmarkContainerHandler.instance);
        }
    }

    public static boolean getConfigValue(String identifier) {
        return tag.getTag(identifier)
            .getBooleanValue(true);
    }

    /**
     * Writes a toggle back to NEI's own global config, which is also where the NEI option buttons read it
     * from - the terminal settings screen and NEI's options list therefore always agree.
     */
    public static void setConfigValue(String identifier, boolean value) {
        tag.getTag(identifier)
            .setBooleanValue(value);
    }

    @Override
    public String getName() {
        return Wtct.MODID;
    }

    @Override
    public String getVersion() {
        return Tags.VERSION;
    }
}
