package com.asdflj.wtct.coremod;

import javax.annotation.Nonnull;

import com.asdflj.wtct.integration.Mods;
import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.gtnewhorizon.gtnhmixins.builders.MixinBuilder;

public enum Mixins implements IMixins {

    AE_CLIENT(new MixinBuilder()
        .addClientMixins(
            "ae.AccessorGuiScrollbar",
            "ae.MixinAEBaseGui",
            "ae.MixinContainerCraftConfirm",
            "ae.MixinCraftingCPUCluster",
            "ae.MixinGuiCraftAmount",
            "ae.MixinGuiCraftConfirm",
            "ae.MixinGuiMEMonitorable",
            "ae.MixinItemRepo",
            "ae.MixinContainerCraftAmount",
            "ae.MixinGuiTabButton")
        .addRequiredMod(Mods.AE2)
        .setPhase(Phase.LATE)),

    AE_SERVER(new MixinBuilder()
        .addCommonMixins(
            "ae.MixinContainerCraftConfirm",
            "ae.MixinContainerCraftingStatus",
            "ae.MixinCraftingCPUCluster",
            "ae.MixinCraftingGridCache",
            "ae.MixinContainerCraftAmount",
            "ae.MixinPacketMonitorableAction")
        .addRequiredMod(Mods.AE2)
        .setPhase(Phase.LATE)),

    NEI(new MixinBuilder()
        .addClientMixins(
            "nei.MixinGuiContainerManager",
            "nei.MixinGuiOverlayButton",
            "nei.MixinIOverlayHandler",
            "nei.MixinPanelWidget",
            "nei.MixinRecipeHandlerRef",
            "nei.MixinRecipeInfo",
            "nei.MixinRecipeItemInputHandler")
        .addRequiredMod(Mods.NOT_ENOUGH_ITEMS)
        .setPhase(Phase.LATE)),

    // The multiblock structure preview's "?" button hands its recipe to the server in NEE's own packet,
    // which NEE only reads for AE2's ContainerPatternTerm - so the comprehensive work terminal never saw
    // a structure at all. Common rather than client: the packet is handled on the server.
    NEENERGISTICS(new MixinBuilder().addCommonMixins("nee.MixinPacketNEIPatternRecipeHandler")
        .addRequiredMod(Mods.NOT_ENOUGH_ENERGISTICS)
        .setPhase(Phase.LATE)),

    WIRELESS_CRAFTING_TERMINAL(new MixinBuilder().addCommonMixins("wct.MixinRandomUtils")
        .addRequiredMod(Mods.WIRELESS_CRAFTING_TERMINAL)
        .setPhase(Phase.LATE)),

    // The terminal's IC2 tier is "any" (-1), which GT's own charge path honours but the battery
    // buffer's slot gate does not: isElectricItem(ItemStack, byte) matches the tier exactly, so
    // without this the buffer refuses to take the item in and nothing ever charges.
    GREGTECH(new MixinBuilder().addCommonMixins("gt.MixinGTModHandler")
        .addRequiredMod(Mods.GREGTECH)
        .setPhase(Phase.LATE)),

    VANILLA_NET_HANDLER(new MixinBuilder().addCommonMixins("vanilla.MixinNetHandlerPlayServer")
        .setPhase(Phase.LATE));

    private final MixinBuilder builder;

    Mixins(MixinBuilder builder) {
        this.builder = builder;
    }

    @Nonnull
    @Override
    public MixinBuilder getBuilder() {
        return builder;
    }
}
