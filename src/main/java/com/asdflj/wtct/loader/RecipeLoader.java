package com.asdflj.wtct.loader;

import static com.asdflj.wtct.loader.ItemAndBlockHolder.BLOCK_NETWORK_HUB;
import static com.asdflj.wtct.loader.ItemAndBlockHolder.CARD_BLOCK_PICKER;
import static com.asdflj.wtct.loader.ItemAndBlockHolder.CARD_ENERGY;
import static com.asdflj.wtct.loader.ItemAndBlockHolder.CARD_EXPORT;
import static com.asdflj.wtct.loader.ItemAndBlockHolder.CARD_IMPORT;
import static com.asdflj.wtct.loader.ItemAndBlockHolder.CARD_MAGNET;
import static com.asdflj.wtct.loader.ItemAndBlockHolder.CARD_QUANTUM_BRIDGE;
import static com.asdflj.wtct.loader.ItemAndBlockHolder.ITEM_COMPREHENSIVE_WORK_TERMINAL;
import static com.asdflj.wtct.loader.ItemAndBlockHolder.ITEM_EX_BUS;
import static com.glodblock.github.loader.ItemAndBlockHolder.WIRELESS_INTERFACE_TERM;
import static com.glodblock.github.loader.ItemAndBlockHolder.WIRELESS_PATTERN_TERM;

import java.util.Arrays;
import java.util.Objects;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.ShapedOreRecipe;

import com.asdflj.wtct.common.item.ItemExBusPart;
import com.asdflj.wtct.integration.Mods;

import appeng.api.AEApi;
import appeng.api.util.AEColor;
import cpw.mods.fml.common.registry.GameRegistry;

public class RecipeLoader implements Runnable {

    public static final RecipeLoader INSTANCE = new RecipeLoader();
    public static final ItemStack CHEST = new ItemStack(Blocks.chest, 1);
    public static final ItemStack CRAFTING_TABLE = new ItemStack(Blocks.crafting_table, 1);
    public static final ItemStack DIAMOND = new ItemStack(Items.diamond, 1);
    public static final ItemStack FISH = new ItemStack(Items.fish);
    public static final ItemStack EGG = new ItemStack(Items.egg);
    public static final ItemStack AE2_PATTERN_TERM = new ItemStack(
        GameRegistry.findItem("appliedenergistics2", "item.ItemMultiPart"),
        1,
        340);
    public static final ItemStack AE2_DIGITAL_SINGULARITY_CELL = new ItemStack(
        GameRegistry.findItem("appliedenergistics2", "item.ItemExtremeStorageCell.Singularity"),
        1);
    public static final ItemStack AE2FC_DIGITAL_SINGULARITY_CELL = com.glodblock.github.loader.ItemAndBlockHolder.SINGULARITY_CELL
        .stack();
    public static final ItemStack AE2_WIRELESS_TERMINAL = GameRegistry
        .findItemStack("appliedenergistics2", "item.ToolWirelessTerminal", 1);
    /** AE2's 能源元件 - the block the energy card is built around. */
    public static final ItemStack AE2_ENERGY_CELL = GameRegistry
        .findItemStack("appliedenergistics2", "tile.BlockEnergyCell", 1);
    public static final ItemStack AE2_TERMINAL = new ItemStack(
        GameRegistry.findItem("appliedenergistics2", "item.ItemMultiPart"),
        1,
        380);
    public static final ItemStack AE2_ME_IO_PORT = AEApi.instance()
        .definitions()
        .blocks()
        .iOPort()
        .maybeStack(1)
        .get();
    public static final ItemStack AE2_SINGULARITY = AEApi.instance()
        .definitions()
        .materials()
        .singularity()
        .maybeStack(1)
        .get();
    public static final ItemStack AE2_SUPER_SPEED_CARD = new ItemStack(
        GameRegistry.findItem("appliedenergistics2", "item.ItemMultiMaterial"),
        1,
        56);
    public static final ItemStack AE2_MEMORY_CARD = AEApi.instance()
        .definitions()
        .items()
        .memoryCard()
        .maybeStack(1)
        .get();

    public static final ItemStack AE2_CRAFTING_CARD = AEApi.instance()
        .definitions()
        .materials()
        .cardCrafting()
        .maybeStack(1)
        .get();

    // Materials and parts the terminal's upgrade cards are made of.
    public static final ItemStack AE2_ADV_CARD = AEApi.instance()
        .definitions()
        .materials()
        .advCard()
        .maybeStack(1)
        .get();
    public static final ItemStack AE2_CALC_PROCESSOR = AEApi.instance()
        .definitions()
        .materials()
        .calcProcessor()
        .maybeStack(1)
        .get();
    public static final ItemStack AE2_ENG_PROCESSOR = AEApi.instance()
        .definitions()
        .materials()
        .engProcessor()
        .maybeStack(1)
        .get();
    public static final ItemStack AE2_IMPORT_BUS = AEApi.instance()
        .definitions()
        .parts()
        .importBus()
        .maybeStack(1)
        .get();
    public static final ItemStack AE2_EXPORT_BUS = AEApi.instance()
        .definitions()
        .parts()
        .exportBus()
        .maybeStack(1)
        .get();
    public static final ItemStack AE2_STORAGE_BUS = AEApi.instance()
        .definitions()
        .parts()
        .storageBus()
        .maybeStack(1)
        .get();
    public static final ItemStack REDSTONE_BLOCK = new ItemStack(Blocks.redstone_block);
    public static final ItemStack DIAMOND_PICKAXE = new ItemStack(Items.diamond_pickaxe);

    public static final ItemStack AE2_ADV_HOUSING = AEApi.instance()
        .definitions()
        .materials()
        .emptyAdvancedStorageCell()
        .maybeStack(1)
        .get();
    public static final ItemStack AE2_64K_PART = AEApi.instance()
        .definitions()
        .materials()
        .cell64kPart()
        .maybeStack(1)
        .get();
    public static final ItemStack AE2_BLANK_PATTERN = AEApi.instance()
        .definitions()
        .materials()
        .blankPattern()
        .maybeStack(1)
        .get();
    public static final ItemStack AE2_PROCESS_LOG = AEApi.instance()
        .definitions()
        .materials()
        .logicProcessor()
        .maybeStack(1)
        .get();
    public static final ItemStack AE2_VIEW_CELL = AEApi.instance()
        .definitions()
        .items()
        .viewCell()
        .maybeStack(1)
        .get();

    public static final ItemStack AE2_WIRELESS = AEApi.instance()
        .definitions()
        .blocks()
        .wireless()
        .maybeStack(1)
        .get();
    public static final ItemStack AE2_CERTUS = AEApi.instance()
        .definitions()
        .items()
        .certusQuartzWrench()
        .maybeStack(1)
        .get();

    // What the network hub is built from: a wireless access point, a quantum link chamber, wireless boosters,
    // quantum rings and a dense cable.
    public static final ItemStack AE2_QUANTUM_LINK = AEApi.instance()
        .definitions()
        .blocks()
        .quantumLink()
        .maybeStack(1)
        .get();
    public static final ItemStack AE2_QUANTUM_RING_BLOCK = AEApi.instance()
        .definitions()
        .blocks()
        .quantumRing()
        .maybeStack(1)
        .get();
    public static final ItemStack AE2_WIRELESS_BOOSTER = AEApi.instance()
        .definitions()
        .materials()
        .wirelessBooster()
        .maybeStack(1)
        .get();
    /** 1.7.10 AE2 has no dense smart cable, so the hub is built on a plain dense one. */
    public static final ItemStack AE2_DENSE_CABLE = AEApi.instance()
        .definitions()
        .parts()
        .cableDense()
        .stack(AEColor.Transparent, 1);

    @Override
    public void run() {
        // GameRegistry.addShapelessRecipe(TOGGLE_VIEW_CELL.stack(), AE2_VIEW_CELL, AE2_PROCESS_LOG);
        // The energy card is this mod's own card: an advanced card between two processors, with a piece
        // of AE2's own energy storage above it and a redstone block below. Each one lifts the
        // terminal's power cap tenfold, and a terminal takes up to five of them.
        if (AE2_ENERGY_CELL != null) {
            GameRegistry.addRecipe(
                new ShapedOreRecipe(
                    CARD_ENERGY.stack(),
                    " E ",
                    "PAP",
                    " R ",
                    'A',
                    AE2_ADV_CARD,
                    'P',
                    AE2_ENG_PROCESSOR,
                    'E',
                    AE2_ENERGY_CELL,
                    'R',
                    REDSTONE_BLOCK));
        }
        // The pattern modifier, the crafting debug card and the creative cells are no longer part of this mod.
        // The terminal's upgrade cards. Import / export / block picker are AE2ImportExportCard's own
        // recipes, kept shape for shape: an advanced card, both processors and a redstone block
        // around the bus the card is modelled on (a diamond pickaxe for the block picker).
        GameRegistry.addRecipe(
            new ShapedOreRecipe(
                CARD_IMPORT.stack(),
                "ERE",
                "XRA",
                "CRC",
                'A',
                AE2_ADV_CARD,
                'C',
                AE2_CALC_PROCESSOR,
                'E',
                AE2_ENG_PROCESSOR,
                'R',
                REDSTONE_BLOCK,
                'X',
                AE2_IMPORT_BUS));
        GameRegistry.addRecipe(
            new ShapedOreRecipe(
                CARD_EXPORT.stack(),
                "ERE",
                "XRA",
                "CRC",
                'A',
                AE2_ADV_CARD,
                'C',
                AE2_CALC_PROCESSOR,
                'E',
                AE2_ENG_PROCESSOR,
                'R',
                REDSTONE_BLOCK,
                'X',
                AE2_EXPORT_BUS));
        GameRegistry.addRecipe(
            new ShapedOreRecipe(
                CARD_BLOCK_PICKER.stack(),
                "ERE",
                "XRA",
                "CRC",
                'A',
                AE2_ADV_CARD,
                'C',
                AE2_CALC_PROCESSOR,
                'E',
                AE2_ENG_PROCESSOR,
                'R',
                REDSTONE_BLOCK,
                'X',
                DIAMOND_PICKAXE));
        // The magnet and quantum bridge cards are AE2WTLib's, and keep AE2WTLib's / AE2FC's own
        // recipes: the magnet card is ae2wct's (an advanced card between iron, redstone and lapis),
        // the quantum bridge card is AE2FC's (an advanced card and a nether star between four
        // quantum rings and two wireless receivers).
        GameRegistry.addRecipe(
            new ShapedOreRecipe(
                CARD_MAGNET.stack(),
                "a b",
                "cdc",
                "ccc",
                'a',
                "dustRedstone",
                'b',
                "gemLapis",
                'c',
                "ingotIron",
                'd',
                AE2_ADV_CARD));
        final ItemStack AE2_QUANTUM_RING = GameRegistry
            .findItemStack("appliedenergistics2", "tile.BlockQuantumRing", 1);
        final ItemStack AE2_WIRELESS_RECEIVER = GameRegistry
            .findItemStack("appliedenergistics2", "item.ItemMultiMaterial", 1);
        if (AE2_QUANTUM_RING != null && AE2_WIRELESS_RECEIVER != null) {
            AE2_WIRELESS_RECEIVER.setItemDamage(41);
            GameRegistry.addRecipe(
                new ShapedOreRecipe(
                    CARD_QUANTUM_BRIDGE.stack(),
                    "QSQ",
                    "WAW",
                    "QSQ",
                    'Q',
                    AE2_QUANTUM_RING,
                    'S',
                    new ItemStack(Items.nether_star),
                    'A',
                    AE2_ADV_CARD,
                    'W',
                    AE2_WIRELESS_RECEIVER));
        }
        // The backpack terminal, the infinity cells, the wireless connector and dual interface terminals and the
        // extended IO port are no longer part of this mod.
        // The comprehensive work terminal folds the four wireless terminals into one, as AE2-WCWT
        // does. ae2wct supplies the wireless crafting terminal; a pack without it still gets the
        // other three.
        final ItemStack AE2WCT_WIRELESS_CRAFTING_TERMINAL = Mods.WIRELESS_CRAFTING_TERMINAL.isModLoaded()
            ? GameRegistry.findItemStack("ae2wct", "wirelessCraftingTerminal", 1)
            : null;
        GameRegistry.addShapelessRecipe(
            ITEM_COMPREHENSIVE_WORK_TERMINAL.stack(),
            Arrays
                .stream(
                    new ItemStack[] { AE2_WIRELESS_TERMINAL, AE2WCT_WIRELESS_CRAFTING_TERMINAL,
                        WIRELESS_PATTERN_TERM.stack(), WIRELESS_INTERFACE_TERM.stack() })
                .filter(Objects::nonNull)
                .toArray());
        // The network hub carries a whole ME network across to another hub. YM-Additions builds it from a
        // wireless access point, a quantum link chamber, wireless boosters and quantum rings around a dense
        // cable (YM used a dense smart cable, which 1.7.10 AE2 does not have).
        GameRegistry.addRecipe(
            new ShapedOreRecipe(
                BLOCK_NETWORK_HUB.stack(),
                "xax",
                "dbd",
                "xcx",
                'a',
                AE2_WIRELESS,
                'b',
                AE2_QUANTUM_LINK,
                'd',
                AE2_WIRELESS_BOOSTER,
                'x',
                AE2_QUANTUM_RING_BLOCK,
                'c',
                AE2_DENSE_CABLE));
        // The ME extended buses are the plain AE2 buses with the extra room an advanced card opens up:
        // an advanced card and two calculation processors between four tungstensteel ingots, around
        // the bus they extend.
        GameRegistry.addRecipe(
            new ShapedOreRecipe(
                ITEM_EX_BUS.stack(1, ItemExBusPart.META_IMPORT_BUS),
                "TCT",
                "X A",
                "TCT",
                'T',
                "ingotTungstenSteel",
                'C',
                AE2_CALC_PROCESSOR,
                'A',
                AE2_ADV_CARD,
                'X',
                AE2_IMPORT_BUS));
        GameRegistry.addRecipe(
            new ShapedOreRecipe(
                ITEM_EX_BUS.stack(1, ItemExBusPart.META_EXPORT_BUS),
                "TCT",
                "X A",
                "TCT",
                'T',
                "ingotTungstenSteel",
                'C',
                AE2_CALC_PROCESSOR,
                'A',
                AE2_ADV_CARD,
                'X',
                AE2_EXPORT_BUS));
        GameRegistry.addRecipe(
            new ShapedOreRecipe(
                ITEM_EX_BUS.stack(1, ItemExBusPart.META_STORAGE_BUS),
                "TCT",
                "X A",
                "TCT",
                'T',
                "ingotTungstenSteel",
                'C',
                AE2_CALC_PROCESSOR,
                'A',
                AE2_ADV_CARD,
                'X',
                AE2_STORAGE_BUS));
        // The Thaumcraft and Botania bridges, the GT wireless distributor and wire cutter,
        // the doll blocks and the creative fluid cells are no longer part of this mod.
    }
}
