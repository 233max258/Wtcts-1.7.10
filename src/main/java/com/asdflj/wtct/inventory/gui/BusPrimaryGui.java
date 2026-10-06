package com.asdflj.wtct.inventory.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import com.asdflj.wtct.inventory.InventoryHandler;
import com.asdflj.wtct.util.BlockPos;

import appeng.api.AEApi;
import appeng.container.AEBaseContainer;
import appeng.container.ContainerOpenContext;
import appeng.container.PrimaryGui;
import appeng.helpers.IPrimaryGuiIconProvider;

/**
 * The destination of the "back to the bus" tab AE2's sub screens - the ore filter and the priority editor - put on
 * their own title bar when they were opened from an extended bus.
 *
 * <p>
 * AE2 derives that destination in {@code AEBaseContainer.createPrimaryGui()} by looking the container class up in
 * its {@code GuiBridge} enum. The lookup is an identity match over the enum's own constants, so this mod's bus
 * containers get null and the tab's button does nothing at all - the sub screen could be entered but not left. The
 * extended buses hand the sub container one of these instead: a primary gui that reopens the extended bus screen
 * through this mod's own gui handler, which is what {@code GuiBridge} cannot express.
 */
public class BusPrimaryGui extends PrimaryGui {

    private final GuiType guiType;

    private BusPrimaryGui(final GuiType guiType, final ItemStack guiIcon, final TileEntity te,
        final ForgeDirection side) {
        // The gui field is only read by PrimaryGui#open, which is overridden here.
        super(null, guiIcon, te, side);
        this.guiType = guiType;
    }

    /**
     * Builds the primary gui for a bus container, choosing the tab's icon exactly as AE2 does - the host's own, or
     * the ME terminal's when the host offers none.
     */
    public static PrimaryGui create(final AEBaseContainer container, final GuiType guiType) {
        final ContainerOpenContext context = container.getOpenContext();
        final ItemStack icon = container.getTarget() instanceof IPrimaryGuiIconProvider provider
            ? provider.getPrimaryGuiIcon()
            : AEApi.instance()
                .definitions()
                .parts()
                .terminal()
                .maybeStack(1)
                .orNull();

        final BusPrimaryGui primaryGui = new BusPrimaryGui(
            guiType,
            icon,
            context == null ? null : context.getTile(),
            context == null ? ForgeDirection.UNKNOWN : context.getSide());
        primaryGui.setSlotIndex(container.getTargetSlotIndex());
        return primaryGui;
    }

    @Override
    public void open(final EntityPlayer p) {
        if (this.te == null) {
            return;
        }
        InventoryHandler.openGui(p, this.te.getWorldObj(), new BlockPos(this.te), this.side, this.guiType);
    }
}
