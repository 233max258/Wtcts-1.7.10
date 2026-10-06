package com.asdflj.wtct.util;

import com.asdflj.wtct.Wtct;

public class NameConst {

    public static final String ITEM_BACKPACK_TERMINAL = "backpack_terminal";
    public static final String ITEM_WIRE_CUTTER = "wire_cutter";
    public static final String ITEM_TOGGLEABLE_VIEW_CELL = "toggleable_view_cell";
    public static final String ITEM_INFINITY_CELL = "infinity_cell";
    public static final String ITEM_CREATIVE_FLUID_CELL = "creative_fluid_cell";
    public static final String ITEM_CREATIVE_CELL = "creative_cell";
    public static final String ITEM_CREATIVE_CELL_COBBLESTONE = ITEM_CREATIVE_CELL + ".cobblestone";
    public static final String ITEM_CREATIVE_FLUID_CELL_WATER = ITEM_CREATIVE_FLUID_CELL + ".water";
    public static final String ITEM_CREATIVE_FLUID_CELL_LAVA = ITEM_CREATIVE_FLUID_CELL + ".lava";
    public static final String ITEM_INFINITY_FLUID_CELL = "infinity_fluid_cell";
    public static final String ITEM_PART_INFUSION_PATTERN_TERMINAL = "part_infusion_pattern_terminal";
    public static final String ITEM_PART_THAUMATORIUM_INTERFACE = "part_thaumatorium_interface";
    public static final String ITEM_PATTERN_MODIFIER = "pattern_modifier";
    public static final String ITEM_WIRELESS_DUAL_INTERFACE_TERMINAL = "wireless_dual_interface_terminal";
    public static final String ITEM_COMPREHENSIVE_WORK_TERMINAL = "comprehensive_work_terminal";
    public static final String ITEM_PHIAL = "phial";
    public static final String ITEM_PART_MANA_IMPORT = "part_mana_import";
    public static final String ITEM_PART_MANA_EXPORT = "part_mana_export";
    public static final String ITEM_PART_EX_BUS = "ex_io_bus";
    public static final String ITEM_PART_EX_IMPORT_BUS = "ex_import_bus";
    public static final String ITEM_PART_EX_EXPORT_BUS = "ex_export_bus";
    public static final String ITEM_PART_EX_STORAGE_BUS = "ex_storage_bus";
    public static final String ITEM_CRAFTING_DEBUG_CARD = "crafting_debug_card";
    public static final String BLOCK_FISHBIG = "fishbig";
    public static final String BLOCK_MDDyue = "MDDyue";
    public static final String BLOCK_ESSENTIA_DISCRETIZER = "essentia_discretizer";
    public static final String BLOCK_WIRELESS_DISTRIBUTOR = "wireless_distributor";
    public static final String BLOCK_INFUSION_INTERFACE = "infusion_interface";
    public static final String BLOCK_EX_IO_PORT = "ex_io_port";
    public static final String BLOCK_NETWORK_HUB = "network_hub";
    public static final String MANA = "botania.mana";
    public static final String TT_KEY = Wtct.MODID + ".tooltip.";
    public static final String TT_SHIFT_FOR_MORE = TT_KEY + "shift_for_more";
    public static final String TT_CTRL_FOR_MORE = TT_KEY + "ctrl_for_more";

    public static final String TT_LINKED = TT_KEY + "linked";
    public static final String TT_INSTALLED_CARD = TT_KEY + "installed_card";
    public static final String TT_WIRELESS_DISTRIBUTOR = TT_KEY + "wireless_distributor.";
    public static final String TT_WIRELESS_DISTRIBUTOR_DESC = TT_WIRELESS_DISTRIBUTOR + "desc";
    public static final String TT_BACKPACK_TERMINAL = TT_KEY + "backpack_terminal.";
    public static final String TT_BACKPACK_TERMINAL_DESC = TT_BACKPACK_TERMINAL + "desc";
    public static final String TT_TOGGLEABLE_VIEW_CELL = TT_KEY + "toggleable_view_cell.desc";
    public static final String TT_CELL_LINK = TT_KEY + "cell_link.";
    public static final String TT_CELL_LINK_DISABLE = TT_CELL_LINK + "disable";
    public static final String TT_CELL_LINK_DESC = TT_CELL_LINK + "desc";
    public static final String TT_CELL_CONTENTS = TT_KEY + "cell_contents";
    public static final String TT_CELL_EMPTY = TT_KEY + "cell_empty";
    public static final String TT_CRAFTING_COMPLETE = TT_KEY + "crafting_complete";
    public static final String TT_INTERFACE_TERMINAL = TT_KEY + "wireless_dual_interface_terminal.";
    public static final String TT_INTERFACE_TERMINAL_DESC = TT_INTERFACE_TERMINAL + "desc";
    public static final String TT_CRAFTING_DEBUG_CARD = TT_KEY + "crafting_debug_card.";
    public static final String TT_CRAFTING_DEBUG_CARD_DESC = TT_CRAFTING_DEBUG_CARD + "desc";
    public static final String TT_WIRELESS = TT_KEY + "wireless.";
    public static final String TT_WIRELESS_INSTALLED = TT_WIRELESS + "installed";
    public static final String TT_COMPREHENSIVE = TT_KEY + "comprehensive.";
    public static final String TT_COMPREHENSIVE_TERMINAL_DESC = TT_COMPREHENSIVE + "desc";

    public static final String TT_NETWORK_HUB = TT_KEY + "network_hub.";
    public static final String TT_NETWORK_HUB_STATE = TT_NETWORK_HUB + "state.";
    public static final String TT_NETWORK_HUB_NETWORK = TT_NETWORK_HUB + "network";
    public static final String TT_NETWORK_HUB_CHANNELS = TT_NETWORK_HUB + "channels";

    public static final String MAGNET = Wtct.MODID + ".magnet.";
    public static final String MAGNET_OFF = MAGNET + "off";
    public static final String MAGNET_INV = MAGNET + "inv";
    public static final String MAGNET_BACKPACK = MAGNET + "backpack";
    public static final String MAGNET_CURRENT_MODE = MAGNET + "current_mode";

    public static final String RES_KEY = Wtct.MODID + ":";

    public static final String GUI_KEY = Wtct.MODID + ".gui.";
    public static final String GUI_CELL_LINK = GUI_KEY + "cell_link";
    public static final String GUI_PATTERN_MODIFIER = GUI_KEY + "pattern_modifier";
    public static final String GUI_PATTERN_MODIFIER_REPLACE = GUI_PATTERN_MODIFIER + ".replace";
    public static final String GUI_PATTERN_MODIFIER_CLEAR = GUI_PATTERN_MODIFIER + ".clear";
    public static final String GUI_TERMINAL = GUI_KEY + "terminal.";
    public static final String GUI_INFUSION_INTERFACE = GUI_KEY + "infusion_interface";
    public static final String GUI_PART_THAUMATORIUM_INTERFACE = GUI_KEY + "part_thaumatorium_interface";
    public static final String GUI_INFUSION_TERMINAL = GUI_KEY + "infusion_terminal.";
    public static final String GUI_INFUSION_TERMINAL_CRAFTING = GUI_INFUSION_TERMINAL + "crafting_pattern";
    public static final String GUI_INFUSION_TERMINAL_PROCESSING = GUI_INFUSION_TERMINAL + "processing_pattern";
    public static final String GUI_MANA_IMPORT = GUI_KEY + ITEM_PART_MANA_IMPORT;
    public static final String GUI_MANA_EXPORT = GUI_KEY + ITEM_PART_MANA_EXPORT;
    public static final String GUI_PATTERN_RENAME = GUI_KEY + "pattern_rename";
    public static final String GUI_TERMINAL_MENU = GUI_KEY + "terminal_menu.";
    public static final String GUI_TERMINAL_MENU_PAGE = GUI_TERMINAL_MENU + "page";
    public static final String GUI_TERMINAL_MENU_MAX_PAGE = GUI_TERMINAL_MENU + "max_page";
    public static final String NEI_KEY = Wtct.MODID + ".nei.";
    public static final String NEI_FIND_CELL_ITEM = NEI_KEY + "find_cell_item.";
    public static final String NEI_FIND_CELL_ITEM_HIGHLIGHT = NEI_FIND_CELL_ITEM + "highlight";
    public static final String NEI_FIND_CELL_ITEM_IN_OTHER_DIM = NEI_FIND_CELL_ITEM + "in_other_dim";

    public static final String MESSAGE_KEY = Wtct.MODID + ".message.";
    public static final String MESSAGE_NETWORK_HUB = MESSAGE_KEY + "network_hub.";
    public static final String MESSAGE_NETWORK_HUB_NO_PERMISSION = MESSAGE_NETWORK_HUB + "no_permission";
    public static final String MESSAGE_NETWORK_HUB_HEAD_PROTECTION = MESSAGE_NETWORK_HUB + "head_protection";
    public static final String MESSAGE_SCANNING = MESSAGE_KEY + "scanning";
    public static final String MESSAGE_CRAFTING_COMPLETE = MESSAGE_KEY + "crafting_complete";
    public static final String MESSAGE_CRAFTING_DEBUG_CARD = MESSAGE_KEY + "crafting_debug_card.";
    public static final String MESSAGE_CRAFTING_DEBUG_CARD_REQUEST_TYPE = MESSAGE_CRAFTING_DEBUG_CARD + "request_type";
    public static final String MESSAGE_CRAFTING_DEBUG_CARD_REQUEST_TYPE_MACHINE = MESSAGE_CRAFTING_DEBUG_CARD_REQUEST_TYPE
        + ".machine";
    public static final String MESSAGE_CRAFTING_DEBUG_CARD_REQUEST_TYPE_PLAYER = MESSAGE_CRAFTING_DEBUG_CARD_REQUEST_TYPE
        + ".player";
    public static final String MESSAGE_CRAFTING_DEBUG_CARD_REQUEST_TYPE_EVERYTHING = MESSAGE_CRAFTING_DEBUG_CARD_REQUEST_TYPE
        + ".everything";
    public static final String MESSAGE_CRAFTING_DEBUG_CARD_NAME = MESSAGE_CRAFTING_DEBUG_CARD + "name";
    public static final String MESSAGE_CRAFTING_DEBUG_CARD_STATE = MESSAGE_CRAFTING_DEBUG_CARD + "state";
    public static final String MESSAGE_CRAFTING_DEBUG_CARD_FINISH = MESSAGE_CRAFTING_DEBUG_CARD + "finish";
    public static final String MESSAGE_CRAFTING_DEBUG_CARD_CANCEL = MESSAGE_CRAFTING_DEBUG_CARD + "cancel";
    public static final String MESSAGE_CRAFTING_DEBUG_CARD_RUNNING = MESSAGE_CRAFTING_DEBUG_CARD + "running";
    public static final String MESSAGE_CRAFTING_DEBUG_CARD_ERROR_MESSAGE = MESSAGE_CRAFTING_DEBUG_CARD
        + "error_message";
    public static final String MESSAGE_CRAFTING_DEBUG_CARD_SIMULATION = MESSAGE_CRAFTING_DEBUG_CARD + "simulation";
    public static final String MESSAGE_CRAFTING_DEBUG_CARD_START_TIME = MESSAGE_CRAFTING_DEBUG_CARD + "start_time";
    public static final String MESSAGE_CRAFTING_DEBUG_CARD_USAGE_TIME = MESSAGE_CRAFTING_DEBUG_CARD + "usage_time";
    public static final String MESSAGE_CRAFTING_DEBUG_CARD_NETWORK_ID = MESSAGE_CRAFTING_DEBUG_CARD + "network_id";
    public static final String MESSAGE_CRAFTING_DEBUG_CARD_POS = MESSAGE_CRAFTING_DEBUG_CARD + "pos";
    public static final String MESSAGE_CRAFTING_DEBUG_CARD_STACK = MESSAGE_CRAFTING_DEBUG_CARD + "stack";
    public static final String MESSAGE_CRAFTING_DEBUG_CARD_NO_HISTORY = MESSAGE_CRAFTING_DEBUG_CARD + "no_history";
    public static final String CRAFTING_DEBUG_CARD = Wtct.MODID + ".crafting_debug_card.";
    public static final String CRAFTING_DEBUG_CARD_CURRENT_MODE = CRAFTING_DEBUG_CARD + "current_mode";
    public static final String CRAFTING_DEBUG_CARD_EXPORT_FILE = CRAFTING_DEBUG_CARD + "export_file";
    public static final String GUI_BUTTON = GUI_KEY + "button.";
    public static final String GUI_BUTTON_REPLAN = GUI_BUTTON + "replan";

    public static final String GUI_NETWORK_HUB = GUI_KEY + "network_hub.";
    public static final String GUI_NETWORK_HUB_BUTTON = GUI_NETWORK_HUB + "button.";
    public static final String GUI_NETWORK_HUB_INFO = GUI_NETWORK_HUB + "info.";
    public static final String GUI_NETWORK_HUB_USER = GUI_NETWORK_HUB + "user.";
    public static final String GUI_NETWORK_HUB_USER_NONE = GUI_NETWORK_HUB_USER + "none";
    public static final String GUI_NETWORK_HUB_USER_USER = GUI_NETWORK_HUB_USER + "user";
    public static final String GUI_NETWORK_HUB_USER_ADMIN = GUI_NETWORK_HUB_USER + "admin";
    public static final String GUI_NETWORK_HUB_USER_OWNER = GUI_NETWORK_HUB_USER + "owner";

}
