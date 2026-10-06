package com.asdflj.wtct.network;

import java.util.Objects;
import java.util.concurrent.Future;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import com.asdflj.wtct.api.WtctAPI;
import com.asdflj.wtct.api.adapter.crafting.ICraftingTerminalAdapter;
import com.asdflj.wtct.inventory.InventoryHandler;
import com.asdflj.wtct.inventory.gui.GuiType;
import com.asdflj.wtct.inventory.item.WirelessTerminal;
import com.asdflj.wtct.util.BlockPos;

import appeng.api.config.CraftingMode;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.crafting.ICraftingJob;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.PlayerSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.data.IAEItemStack;
import appeng.container.AEBaseContainer;
import appeng.container.ContainerOpenContext;
import appeng.container.implementations.ContainerCraftAmount;
import appeng.container.implementations.ContainerCraftConfirm;
import appeng.core.AELog;
import appeng.me.cache.CraftingGridCache;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class CPacketCraftRequest implements IMessage {

    private long amount;
    private boolean heldShift;
    private boolean heldCtrl;
    private IAEItemStack item = null;
    private Mode mode;
    private CraftingMode craftingMode;

    private enum Mode {
        ITEM,
        STACK_SIZE
    }

    public CPacketCraftRequest() {}

    public CPacketCraftRequest(final IAEItemStack item, final boolean shift) {
        this(item, shift, false, CraftingMode.STANDARD);
    }

    public CPacketCraftRequest(final IAEItemStack item, final boolean shift, CraftingMode currentValue) {
        this(item, shift, false, currentValue);
    }

    public CPacketCraftRequest(final IAEItemStack item, final boolean shift, final boolean ctrl,
        final CraftingMode currentValue) {
        this.item = item;
        this.heldShift = shift;
        this.heldCtrl = ctrl;
        this.mode = Mode.ITEM;
        this.craftingMode = currentValue;
    }

    public CPacketCraftRequest(final long craftAmt, final boolean shift, CraftingMode currentValue) {
        this(craftAmt, shift, false, currentValue);
    }

    public CPacketCraftRequest(final long craftAmt, final boolean shift, final boolean ctrl,
        final CraftingMode currentValue) {
        this.amount = craftAmt;
        this.heldShift = shift;
        this.heldCtrl = ctrl;
        this.mode = Mode.STACK_SIZE;
        this.craftingMode = currentValue;
    }

    public CPacketCraftRequest(final long craftAmt, final boolean shift) {
        this(craftAmt, shift, CraftingMode.STANDARD);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(mode.ordinal());
        buf.writeByte(craftingMode.ordinal());
        if (mode == Mode.ITEM) {
            try {
                item.writeToPacket(buf);
                buf.writeBoolean(heldShift);
                buf.writeBoolean(heldCtrl);
            } catch (Exception ignored) {}
        } else {
            buf.writeLong(amount);
            buf.writeBoolean(heldShift);
            buf.writeBoolean(heldCtrl);
        }

    }

    @Override
    public void fromBytes(ByteBuf buf) {
        mode = Mode.values()[buf.readByte()];
        craftingMode = CraftingMode.values()[buf.readByte()];
        if (mode == Mode.ITEM) {
            try {
                item = AEItemStack.loadItemStackFromPacket(buf);
                heldShift = buf.readBoolean();
                heldCtrl = buf.readBoolean();
            } catch (Exception ignored) {}
        } else {
            amount = buf.readLong();
            heldShift = buf.readBoolean();
            heldCtrl = buf.readBoolean();
        }
    }

    public static class Handler implements IMessageHandler<CPacketCraftRequest, IMessage> {

        @Nullable
        @Override
        public IMessage onMessage(CPacketCraftRequest message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            Object target;
            if (player.openContainer instanceof final ContainerCraftAmount cca) {
                target = cca.getTarget();
                if (target instanceof final IGridHost gh) {
                    final IGridNode gn = gh.getGridNode(ForgeDirection.UNKNOWN);

                    if (gn == null) {
                        return null;
                    }

                    final IGrid g = gn.getGrid();
                    if (g == null || cca.getItemToCraft() == null) {
                        return null;
                    }

                    cca.getItemToCraft().setStackSize(message.amount);

                    Future<ICraftingJob> futureJob = null;
                    try {
                        final ICraftingGrid cg = g.getCache(ICraftingGrid.class);
                        if (cg instanceof CraftingGridCache cgc) {
                            // rv3-beta-1073 put a "lite crafting" boolean between the mode and the
                            // callback; AE2's own PacketCraftRequest feeds it AEConfig's setting.
                            futureJob = cgc.beginCraftingJob(
                                cca.getWorld(),
                                cca.getGrid(),
                                cca.getActionSrc(),
                                cca.getItemToCraft(),
                                message.craftingMode,
                                appeng.core.AEConfig.instance.getUseLiteCraftingMode(),
                                null);
                        } else {
                            futureJob = cg.beginCraftingJob(
                                cca.getWorld(),
                                cca.getGrid(),
                                cca.getActionSrc(),
                                cca.getItemToCraft(),
                                null);
                        }

                        final ContainerOpenContext context = cca.getOpenContext();
                        if (context != null) {
                            final TileEntity te = context.getTile();
                            if (te != null) {
                                InventoryHandler.openGui(
                                        player,
                                        player.worldObj,
                                        new BlockPos(te),
                                        Objects.requireNonNull(context.getSide()),
                                        GuiType.CRAFTING_CONFIRM);
                            }else{
                                InventoryHandler.openGui(
                                    player,
                                    player.worldObj,
                                    new BlockPos(((WirelessTerminal) target).getInventorySlot(),0,0),
                                    Objects.requireNonNull(cca.getOpenContext().getSide()),
                                    GuiType.CRAFTING_CONFIRM_ITEM);
                            }

                            if (player.openContainer instanceof final ContainerCraftConfirm ccc) {
                                ccc.setItemToCraft(cca.getItemToCraft());
                                // The same modifier split AE2's own craft-amount screen uses: Shift only
                                // auto-starts the job, Ctrl auto-starts it and follows it to the end.
                                ccc.setAutoStart(message.heldShift && !message.heldCtrl);
                                ccc.setAutoStartAndFollow(!message.heldShift && message.heldCtrl);
                                ccc.setJob(futureJob);
                                cca.detectAndSendChanges();
                            }
                        }
                    } catch (final Throwable e) {
                        if (futureJob != null) {
                            futureJob.cancel(true);
                        }
                        AELog.debug(e);
                    }
                }
            } else if (player.openContainer instanceof AEBaseContainer c) {
                target = c.getTarget();
                if (target instanceof final IGridHost gh) {
                    final IGridNode gn = gh.getGridNode(ForgeDirection.UNKNOWN);

                    if (gn == null) {
                        return null;
                    }

                    final IGrid g = gn.getGrid();
                    if (g == null) {
                        return null;
                    }
                    Future<ICraftingJob> futureJob = null;
                    try {
                        IStorageGrid storageGrid = g.getCache(IStorageGrid.class);
                        if(storageGrid == null) return null;
                        IAEItemStack storedItem = storageGrid.getItemInventory().getStorageList().findPrecise(message.item);
                        if(storedItem == null || !storedItem.isCraftable()) return null;
                        final ICraftingGrid cg = g.getCache(ICraftingGrid.class);
                        if (cg instanceof CraftingGridCache cgc) {

                            futureJob = cgc.beginCraftingJob(
                                player.getEntityWorld(),
                                ((IActionHost)target).getActionableNode().getGrid(),
                                new PlayerSource(player, (IActionHost)target),
                                message.item,
                                message.craftingMode,
                                appeng.core.AEConfig.instance.getUseLiteCraftingMode(),
                                null);
                        } else {
                            futureJob = cg.beginCraftingJob(
                                player.getEntityWorld(),
                                ((IActionHost)target).getActionableNode().getGrid(),
                                new PlayerSource(player, (IActionHost)target),
                                message.item,
                                null);
                        }
                        final ContainerOpenContext context = c.getOpenContext();
                        if (context != null) {
                            for (ICraftingTerminalAdapter adapter:WtctAPI.instance().terminal().getCraftingTerminal().values()){
                                if(player.openContainer.getClass() == adapter.getContainer()){
                                    final TileEntity te = context.getTile();
                                    adapter.openGui(player,te,context.getSide(),target);
                                }
                            }
                            if (player.openContainer instanceof final ContainerCraftConfirm ccc) {
                                ccc.setItemToCraft(message.item);
                                // Shift: auto-start. Ctrl: auto-start and follow (AE2's own split).
                                ccc.setAutoStart(message.heldShift && !message.heldCtrl);
                                ccc.setAutoStartAndFollow(!message.heldShift && message.heldCtrl);
                                ccc.setJob(futureJob);
                                ccc.detectAndSendChanges();
                            }
                        }
                    } catch (Exception e) {
                        if (futureJob != null) {
                            futureJob.cancel(true);
                        }
                        AELog.debug(e);
                    }
                }
            }
            return null;
        }
    }
}
