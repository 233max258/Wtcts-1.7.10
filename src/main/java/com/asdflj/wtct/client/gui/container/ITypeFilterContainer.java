package com.asdflj.wtct.client.gui.container;

import net.minecraft.entity.player.EntityPlayer;

import appeng.api.storage.ITerminalTypeFilterProvider;
import appeng.api.storage.data.IAEStackType;
import it.unimi.dsi.fastutil.objects.Reference2BooleanMap;

/**
 * Implemented by containers that host an {@link ITerminalTypeFilterProvider} terminal so the client can sync per-type
 * visibility toggles to the server.
 */
public interface ITypeFilterContainer {

    ITerminalTypeFilterProvider getTypeFilterHost();

    /** One toggle: flip a single type and persist the terminal. */
    default void updateTypeFilter(IAEStackType<?> type, boolean value, EntityPlayer player) {
        final ITerminalTypeFilterProvider host = this.getTypeFilterHost();
        if (host == null || type == null) {
            return;
        }
        host.getTypeFilter(player)
            .put(type, value);
        host.saveTypeFilter();
    }

    default void updateTypeFilters(Reference2BooleanMap<IAEStackType<?>> map, EntityPlayer player) {
        final ITerminalTypeFilterProvider host = this.getTypeFilterHost();
        if (host == null || map == null) {
            return;
        }
        final Reference2BooleanMap<IAEStackType<?>> target = host.getTypeFilter(player);
        for (Reference2BooleanMap.Entry<IAEStackType<?>> entry : map.reference2BooleanEntrySet()) {
            target.put(entry.getKey(), entry.getBooleanValue());
        }
        host.saveTypeFilter();
    }
}
