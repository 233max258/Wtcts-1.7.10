package com.asdflj.wtct.crossmod.waila;

import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;

import com.asdflj.wtct.common.Config;
import com.asdflj.wtct.common.tile.TileNetworkHub;
import com.asdflj.wtct.util.NameConst;

import appeng.integration.modules.waila.BaseWailaDataProvider;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;

/**
 * Adds what AE2's own tile probe cannot know about: the link state, the channels the head has left to hand out and,
 * if configured, the network uuid. The powered/active/offline line is deliberately left to AE2's
 * {@code PowerStateWailaDataProvider}, which already covers every {@code AEBaseTile} and would otherwise be printed
 * twice.
 */
public class NetworkHubWaila extends BaseWailaDataProvider {

    @Override
    public List<String> getWailaBody(final ItemStack itemStack, final List<String> currentToolTip,
        final IWailaDataAccessor accessor, final IWailaConfigHandler config) {
        final TileEntity te = accessor.getTileEntity();
        if (!(te instanceof TileNetworkHub hub)) return currentToolTip;

        currentToolTip.add(StatCollector.translateToLocal(NameConst.TT_NETWORK_HUB_STATE + hub.isConnected()));
        currentToolTip
            .add(StatCollector.translateToLocalFormatted(NameConst.TT_NETWORK_HUB_CHANNELS, hub.getSurplusChannels()));
        if (Config.netHubShowNetworkUuid) {
            currentToolTip.add(
                StatCollector.translateToLocal(NameConst.TT_NETWORK_HUB_NETWORK) + " "
                    + (hub.getNetworkUuid() == null ? "Unknown" : hub.getNetworkUuid()));
        }

        return currentToolTip;
    }
}
