package com.asdflj.wtct.common.nethub;

import com.asdflj.wtct.common.Config;
import com.asdflj.wtct.util.BlockPos;

/**
 * What a receiver pays to hold its connection to the head: a base cost plus a term that grows with the distance
 * between the two blocks, multiplied when the two sit in different dimensions.
 */
public final class NetHubPowerUsage {

    private NetHubPowerUsage() {}

    public static double calc(BlockPos from, BlockPos to, int fromDimension, int toDimension) {
        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();
        double dz = to.getZ() - from.getZ();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double power = Config.netHubPowerBase
            + Config.netHubPowerDistanceMultiplier * distance * Math.log(distance * distance + 3);
        return power * (fromDimension == toDimension ? 1.0D : Config.netHubOtherDimMultiplier);
    }
}
