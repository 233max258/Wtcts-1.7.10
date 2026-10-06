package com.asdflj.wtct.common.nethub;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;

import cpw.mods.fml.common.FMLCommonHandler;

public final class NetHubUtil {

    private static final String[] UNITS = { "", "K", "M", "G", "T", "P", "E", "Z", "Y", "R", "Q" };
    private static final String REGEX_SPECIALS = "\\$()*+.[]?^{}|";

    private NetHubUtil() {}

    /** Operators skip every permission check on a network, the same way they skip the permission level of a group. */
    public static boolean isPlayerOp(EntityPlayer player) {
        MinecraftServer server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        return server != null && server.getConfigurationManager()
            .func_152596_g(player.getGameProfile());
    }

    public static String formatCompact(long amount) {
        if (amount == 0) return "0";
        int unitIndex = 0;
        double value = amount;
        while (value >= 1000 && unitIndex < UNITS.length - 1) {
            value /= 1000;
            unitIndex++;
        }
        if (unitIndex == 0) return String.valueOf(amount);
        return String.format("%.2f%s", value, UNITS[unitIndex]);
    }

    /** Escapes everything the search box's regular expressions would otherwise treat as syntax. */
    public static String escapeRegExp(String keyword) {
        if (keyword == null || keyword.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(keyword.length());
        for (int i = 0; i < keyword.length(); i++) {
            char c = keyword.charAt(i);
            if (REGEX_SPECIALS.indexOf(c) >= 0) sb.append('\\');
            sb.append(c);
        }
        return sb.toString();
    }

    /** 1.7.10 writes uuids as plain strings; a malformed one must not blow up while a world is loading. */
    @Nullable
    public static UUID parseUuid(@Nullable String value) {
        if (value == null || value.isEmpty()) return null;
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
