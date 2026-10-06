package com.asdflj.wtct.nei;

public interface ButtonConstants {

    String HISTORY = "wtct.history";
    String INVENTORY_STATE = "wtct.state";
    String PINNED_BAR = "wtct.pinned_bar";
    String PINNED_BAR_REMOVE = "wtct.pinned_bar_remove";
    String PINNED_BAR_CRAFTING_STATE = "wtct.pinned_bar_crafting_state";
    /**
     * AE2 1.21's "Pin items obtained from autocrafting": with it on, the output of a started crafting job is
     * pinned to the top of the item list. Distinct from {@link #PINNED_BAR}, which only controls whether the
     * pinned row is drawn at all - turning that one off would hide the player's own favourites too.
     */
    String PINNED_AUTO_CRAFT = "wtct.pinned_auto_crafted";
    String BLOCK_RENDER = "wtct.block_render";
    String ULTRA_TERMINAL_MODE = "wtct.ultra_terminal_mode";
    String DUAL_INTERFACE_TERMINAL = "wtct.dual_interface_terminal_fill_search_names";
    String DUAL_INTERFACE_TERMINAL_FILL_CIRCUIT = "wtct.dual_interface_terminal_fill_circuit";
    String DUAL_INTERFACE_TERMINAL_APPEND_CIRCUIT_DAMAGE = "wtct.dual_interface_terminal_append_circuit_damage";
    String CRAFTING_NOTIFICATION = "wtct.crafting_notification";
    String NEI_CRAFT_ITEM = "wtct.nei_craft_item";
}
