package com.asdflj.wtct.common;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

import com.asdflj.wtct.Wtct;

import cpw.mods.fml.relauncher.FMLInjectionData;

public class Config {

    private static final Configuration Config = new Configuration(
        new File(new File((File) FMLInjectionData.data()[6], "config"), Wtct.MODID + ".cfg"));
    public static boolean cellLink;
    public static int magnetRange;
    public static boolean updateViewThread = true;
    public static boolean wirelessConnectorTerminalInfinityConnectionRange = true;
    public static int exIOPortTransferContentsRate;
    /** How much faster than the matching AE2 bus (at the same upgrade tier) an extended bus moves items. */
    public static int exBusSpeedMultiplier;
    public static boolean backpackTerminalAddTicSupport = false;
    public static int craftingHistorySize = 200;
    // The network hub's power bill: a flat part plus a part that grows with the distance between the two hubs, times a
    // penalty multiplier when they do not share a dimension.
    public static double netHubPowerBase = 1000.0D;
    public static double netHubPowerDistanceMultiplier = 0.1D;
    public static double netHubOtherDimMultiplier = 10.0D;
    /** Whether a hub may link to a hub in another dimension at all. */
    public static boolean netHubCanCrossDimension = true;
    public static boolean netHubShowNetworkUuid = false;

    public static void run() {
        loadCategory();
        loadProperty();
    }

    private static void loadProperty() {
        cellLink = Config
            .getBoolean("Enable link cell", Wtct.MODID, true, "Enable link Cell,It will link every same uuid cell");
        magnetRange = Config
            .getInt("Backpack terminal magnet range", Wtct.MODID, 32, 8, 64, "Set backpack terminal magnet range");
        updateViewThread = Config
            .getBoolean("Terminal updateView thread", Wtct.MODID, true, "Terminal create update view thread");
        wirelessConnectorTerminalInfinityConnectionRange = Config.getBoolean(
            "Wireless connector terminal infinity connection range",
            Wtct.MODID,
            true,
            "Wireless connector terminal infinity connection range");
        exIOPortTransferContentsRate = Config.getInt(
            "Ex IO Port Transfer Rate",
            Wtct.MODID,
            256,
            256,
            Integer.MAX_VALUE,
            "Set Ex IO Port Transfer Rate. Base transfer quantity = 256. Make sure you have enough power to transfer stack.");
        exBusSpeedMultiplier = Config.getInt(
            "Ex Bus Speed Multiplier",
            Wtct.MODID,
            8,
            2,
            128,
            "Speed multiplier of the ME extended import/export bus, applied on top of the installed speed cards. Make sure you have enough power to transfer stack.");
        backpackTerminalAddTicSupport = Config
            .getBoolean("Tic", Wtct.MODID, false, "Let Backpack Terminal can forge tic tool");
        craftingHistorySize = Config
            .getInt("crafting history size", Wtct.MODID, 200, 100, 300, "crafting history size");
        netHubPowerBase = Config.getFloat(
            "Network hub power base",
            Wtct.MODID,
            1000.0F,
            0.0F,
            Float.MAX_VALUE,
            "Flat part of the idle power a network hub connection draws");
        netHubPowerDistanceMultiplier = Config.getFloat(
            "Network hub power distance multiplier",
            Wtct.MODID,
            0.1F,
            0.0F,
            Float.MAX_VALUE,
            "Weight of the distance between the two hubs in the network hub power bill");
        netHubOtherDimMultiplier = Config.getFloat(
            "Network hub other dimension multiplier",
            Wtct.MODID,
            10.0F,
            0.0F,
            Float.MAX_VALUE,
            "Extra multiplier on the network hub power bill when the two hubs are in different dimensions");
        netHubCanCrossDimension = Config.getBoolean(
            "Network hub can cross dimension",
            Wtct.MODID,
            true,
            "Let a network hub link to a network hub in another dimension");
        netHubShowNetworkUuid = Config
            .getBoolean("Network hub show network uuid", Wtct.MODID, false, "Show the network uuid in WAILA");
        if (Config.hasChanged()) Config.save();
    }

    private static void loadCategory() {
        Config.addCustomCategoryComment(Wtct.MODID, "Settings for Wtct.");
    }
}
