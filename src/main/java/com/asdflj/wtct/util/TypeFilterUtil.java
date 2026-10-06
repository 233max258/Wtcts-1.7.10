package com.asdflj.wtct.util;

import appeng.api.storage.data.AEStackTypeRegistry;
import appeng.api.storage.data.IAEStackType;
import appeng.util.AEStackTypeFilter;
import it.unimi.dsi.fastutil.objects.Reference2BooleanMap;
import it.unimi.dsi.fastutil.objects.Reference2BooleanOpenHashMap;

/**
 * AE2 rv3-beta-977 only exposes an unmodifiable copy of an {@link AEStackTypeFilter}'s map (via
 * {@code getImmutableFilters()}), while AE2's terminal type-filter contract hands out a map the caller mutates before
 * calling {@code saveTypeFilter()}. These helpers materialise that mutable map and write it back into the filter.
 */
public final class TypeFilterUtil {

    private TypeFilterUtil() {}

    /** A fresh mutable map with every registered type enabled - the equivalent of a default filter's map. */
    public static Reference2BooleanMap<IAEStackType<?>> defaultView() {
        return mutableView(new AEStackTypeFilter());
    }

    /** A mutable copy of {@code filter}'s current state, to be handed to callers that mutate it. */
    public static Reference2BooleanMap<IAEStackType<?>> mutableView(final AEStackTypeFilter filter) {
        final Reference2BooleanMap<IAEStackType<?>> map = new Reference2BooleanOpenHashMap<>();
        for (final IAEStackType<?> type : AEStackTypeRegistry.getAllTypes()) {
            map.put(type, filter.isEnabled(type));
        }
        return map;
    }

    /** Writes a view produced by {@link #mutableView} back into {@code filter}. */
    public static void flush(final AEStackTypeFilter filter, final Reference2BooleanMap<IAEStackType<?>> view) {
        if (view == null) {
            return;
        }
        for (final Reference2BooleanMap.Entry<IAEStackType<?>> entry : view.reference2BooleanEntrySet()) {
            filter.setEnabled(entry.getKey(), entry.getBooleanValue());
        }
    }
}
