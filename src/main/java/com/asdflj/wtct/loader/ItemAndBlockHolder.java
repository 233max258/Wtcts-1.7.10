package com.asdflj.wtct.loader;

import com.asdflj.wtct.common.block.BlockNetworkHub;
import com.asdflj.wtct.common.item.ItemComprehensiveWorkTerminal;
import com.asdflj.wtct.common.item.ItemExBusPart;
import com.asdflj.wtct.common.item.card.ItemWcwtUpgradeCard;

/**
 * The items and blocks this mod puts into the game.
 *
 * <p>
 * The ME Comprehensive Work Terminal, its six upgrade cards and the ME extended import/export buses.
 */
public class ItemAndBlockHolder implements Runnable {

    public static final ItemAndBlockHolder INSTANCE = new ItemAndBlockHolder();

    public static ItemComprehensiveWorkTerminal ITEM_COMPREHENSIVE_WORK_TERMINAL = new ItemComprehensiveWorkTerminal()
        .register();

    // The terminal's upgrade cards. Import / export / block picker are AE2ImportExportCard's cards, the magnet and
    // quantum bridge cards are AE2WTLib's; see ItemWcwtUpgradeCard.
    public static ItemWcwtUpgradeCard CARD_IMPORT = new ItemWcwtUpgradeCard(ItemWcwtUpgradeCard.Kind.IMPORT).register();
    public static ItemWcwtUpgradeCard CARD_EXPORT = new ItemWcwtUpgradeCard(ItemWcwtUpgradeCard.Kind.EXPORT).register();
    public static ItemWcwtUpgradeCard CARD_BLOCK_PICKER = new ItemWcwtUpgradeCard(ItemWcwtUpgradeCard.Kind.BLOCK_PICKER)
        .register();
    public static ItemWcwtUpgradeCard CARD_MAGNET = new ItemWcwtUpgradeCard(ItemWcwtUpgradeCard.Kind.MAGNET).register();
    public static ItemWcwtUpgradeCard CARD_QUANTUM_BRIDGE = new ItemWcwtUpgradeCard(
        ItemWcwtUpgradeCard.Kind.QUANTUM_BRIDGE).register();
    // The energy card is the one card a terminal takes several of: up to five, each ×10 on the power cap.
    public static ItemWcwtUpgradeCard CARD_ENERGY = new ItemWcwtUpgradeCard(ItemWcwtUpgradeCard.Kind.ENERGY).register();

    // The block that carries a ME network across to another hub, even across dimensions.
    public static BlockNetworkHub BLOCK_NETWORK_HUB = new BlockNetworkHub().register();

    // The ME extended buses. One item, two sub-types: damage 0 is the import bus, 1 the export bus.
    public static ItemExBusPart ITEM_EX_BUS = new ItemExBusPart().register();

    /**
     * Hook for conditionally loaded content, and for the parts that have to tell AE2 which upgrade cards they accept.
     */
    @Override
    public void run() {
        ItemExBusPart.registerUpgrades();
    }
}
