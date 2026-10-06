package com.asdflj.wtct.network;

import static appeng.api.networking.crafting.CraftingItemList.ACTIVE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import com.asdflj.wtct.Wtct;
import com.asdflj.wtct.api.Constants;
import com.asdflj.wtct.api.InventoryActionExtend;
import com.asdflj.wtct.api.WirelessObject;
import com.asdflj.wtct.client.gui.container.ContainerComprehensiveWorkTerminal;
import com.asdflj.wtct.client.gui.container.ContainerPatternValueAmount;
import com.asdflj.wtct.client.gui.container.ContainerPatternValueName;
import com.asdflj.wtct.client.gui.container.ContainerWcwtSettings;
import com.asdflj.wtct.inventory.InventoryHandler;
import com.asdflj.wtct.inventory.gui.GuiType;
import com.asdflj.wtct.inventory.item.WirelessTerminal;
import com.asdflj.wtct.util.BaublesUtil;
import com.asdflj.wtct.util.BlockPos;
import com.asdflj.wtct.util.CPUCraftingPreview;
import com.asdflj.wtct.util.InvUtil;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.features.IWirelessTermHandler;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.security.IActionHost;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.container.AEBaseContainer;
import appeng.container.ContainerOpenContext;
import appeng.container.implementations.ContainerCraftAmount;
import appeng.container.interfaces.IInventorySlotAware;
import appeng.core.AELog;
import appeng.core.localization.GuiText;
import appeng.me.cache.CraftingGridCache;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class CPacketInventoryActionExtend implements IMessage {

    private InventoryActionExtend action;
    private int slot;
    private long id;
    private IAEItemStack stack;
    private boolean isEmpty;

    public CPacketInventoryActionExtend() {}

    public CPacketInventoryActionExtend(final InventoryActionExtend action, final int slot, final int id) {
        this(action, slot, id, null);
    }

    public CPacketInventoryActionExtend(final InventoryActionExtend action) {
        this(action, 0, 0, null);
    }

    public CPacketInventoryActionExtend(final InventoryActionExtend action, final int slot, final int id,
        IAEItemStack stack) {
        this.action = action;
        this.slot = slot;
        this.id = id;
        this.stack = stack;
        this.isEmpty = stack == null;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(action.ordinal());
        buf.writeInt(slot);
        buf.writeLong(id);
        buf.writeBoolean(isEmpty);
        if (!isEmpty) {
            try {
                stack.writeToPacket(buf);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        action = InventoryActionExtend.values()[buf.readInt()];
        slot = buf.readInt();
        id = buf.readLong();
        isEmpty = buf.readBoolean();
        if (!isEmpty) {
            try {
                stack = AEItemStack.loadItemStackFromPacket(buf);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static class Handler implements IMessageHandler<CPacketInventoryActionExtend, IMessage> {

        private void extractItemFromME(EntityPlayerMP player, IAEItemStack requestItem, int slot) {
            if (requestItem.getStackSize() <= 0) {
                return;
            }
            final long wanted = requestItem.getStackSize();
            ItemStack reachable = null;
            int reachableSlot = -1;
            List<ItemStack> items = InvUtil
                .matcher(player, stack -> stack != null && stack.getItem() instanceof IWirelessTermHandler);
            for (ItemStack item : items) {
                // The settings screen's "pick block" switch is the master switch for pulling the
                // looked-at block out of the network. This path is the other half of that feature -
                // the plain middle-click one, which needs no picker card - so it has to honour the
                // switch too, or turning it off would still leave middle-click pulling blocks.
                if (!ContainerWcwtSettings.isOn(item, ContainerWcwtSettings.KEY_PICK_BLOCK)) {
                    continue;
                }
                try {
                    WirelessObject object = new WirelessObject(item, player.worldObj, slot, 0, 0, player);
                    if (object.rangeCheck() && requestItem.getStackSize() > 0) {
                        reachable = item;
                        reachableSlot = inventorySlotOf(player, item);
                        IAEItemStack result = object.getItemInventory()
                            .extractItems(requestItem, Actionable.MODULATE, object.getSource());
                        if (result != null) {
                            requestItem.decStackSize(result.getStackSize());
                        }
                        if (requestItem.getStackSize() <= 0) {
                            break;
                        }
                    }
                } catch (Exception ignored) {}
            }
            // Not a single item was handed over, so the network does not hold the block at all. That
            // is the "missing" half of the gesture: offer to have it made instead of leaving the click
            // a silent no-op.
            if (reachable != null && requestItem.getStackSize() == wanted) {
                requestCraftForMissingPick(player, requestItem, reachable, reachableSlot);
            }
        }

        /**
         * Where the terminal sits, as a coordinate the GUI factory can open. The packet carries the <em>hotbar</em>
         * index (the gesture needs an empty hand, so that slot is empty and useless), while opening a GUI is
         * addressed by the host's own coordinate - see {@code ItemGuiFactory}, which reads it back through the
         * bridge-aware helper, so a terminal worn in a Baubles slot is addressable too (it has no main-inventory
         * index, and the plain scan this used to do answered -1 for it, which is why the craft half of the
         * gesture did nothing with the terminal in a Baubles slot). -1 means the stack is in neither inventory.
         */
        private static int inventorySlotOf(final EntityPlayerMP player, final ItemStack terminal) {
            return BaublesUtil.findBridgeSlot(player, terminal);
        }

        /**
         * Whether any pattern on the grid turns out this exact stack. The network's own pattern map is
         * asked rather than the storage list, because the interesting case here is precisely a block
         * the network holds none of - it has no entry in that list, so asking it would answer "no" for
         * everything this feature is meant to handle.
         */
        private static boolean isCraftable(final CraftingGridCache cache, final IAEItemStack what) {
            for (final IAEItemStack craftable : cache.getCraftingPatterns()
                .keySet()) {
                if (craftable.isSameType(what)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Opens AE2's amount screen with the block the network could not hand over already filled
         * in, so the same middle-click can say "make me some of these". How many is the player's to
         * pick there - nothing is planned or started before they confirm, and the plan is drawn up by
         * the usual craft request once they do.
         * A block the network has no pattern for is dropped quietly rather than half-handled.
         */
        private void requestCraftForMissingPick(final EntityPlayerMP player, final IAEItemStack what,
            final ItemStack terminal, final int slot) {
            if (!ContainerWcwtSettings.isOn(terminal, ContainerWcwtSettings.KEY_PICK_BLOCK_CRAFT)) {
                return;
            }
            if (slot < 0) {
                return;
            }
            try {
                final WirelessObject object = new WirelessObject(terminal, player.worldObj, slot, 0, 0, player);
                final IGridNode node = object.getActionableNode();
                if (node == null) {
                    return;
                }
                final IGrid grid = node.getGrid();
                if (grid == null) {
                    return;
                }
                final ICraftingGrid crafting = grid.getCache(ICraftingGrid.class);
                if (!(crafting instanceof final CraftingGridCache cache)) {
                    return;
                }
                // Only for a block some pattern actually turns out. A block without one would open a
                // screen whose plan can never be made, and a stray middle-click should not do that.
                if (!isCraftable(cache, what)) {
                    return;
                }
                InventoryHandler.openGui(
                    player,
                    player.worldObj,
                    new BlockPos(slot, 0, 0),
                    ForgeDirection.UNKNOWN,
                    GuiType.CRAFTING_AMOUNT_ITEM);
                if (player.openContainer instanceof final ContainerCraftAmount amount) {
                    // Sends PacketVirtualSlot, which is what makes the client's amount screen show the
                    // item at all; the count the player types is read back by the craft request. The
                    // icon carries a single unit, since the gesture does not decide the amount.
                    final IAEItemStack one = what.copy();
                    one.setStackSize(1);
                    amount.setItemToCraft(one);
                    amount.detectAndSendChanges();
                }
            } catch (final Throwable e) {
                AELog.debug(e);
            }
        }

        @Nullable
        @Override
        public IMessage onMessage(CPacketInventoryActionExtend message, MessageContext ctx) {
            final EntityPlayerMP sender = ctx.getServerHandler().playerEntity;
            if(message.action == InventoryActionExtend.REQUEST_ITEM && sender.inventory.mainInventory[message.slot] == null){
                message.stack.setStackSize(message.stack.getItemStack().getMaxStackSize());
                IAEItemStack requestItem = message.stack.copy();
                extractItemFromME(sender,requestItem,message.slot);
                message.stack.decStackSize(requestItem.getStackSize());
                if(message.stack.getStackSize() > 0){
                    sender.inventory.setInventorySlotContents(message.slot,message.stack.getItemStack());
                }
                return null;
            }
            if (sender.openContainer instanceof final AEBaseContainer baseContainer) {
                Object target = baseContainer.getTarget();
                if (message.action == InventoryActionExtend.SET_PATTERN_NAME) {
                    final ContainerOpenContext context = baseContainer.getOpenContext();
                    if (context != null && message.stack != null) {
                        final TileEntity te = context.getTile();
                        if (te != null) {
                            InventoryHandler.openGui(
                                    sender,
                                    te.getWorldObj(),
                                    new BlockPos(te),
                                    Objects.requireNonNull(baseContainer.getOpenContext().getSide()),
                                    GuiType.PATTERN_NAME_SET);
                        }else{
                            InventoryHandler.openGui(
                                sender,
                                sender.getEntityWorld(),
                                new BlockPos(((WirelessTerminal) target).getInventorySlot(),0,0),
                                Objects.requireNonNull(baseContainer.getOpenContext().getSide()),
                                GuiType.PATTERN_NAME_SET_ITEM);
                        }

                        ItemStack itemStack = message.stack.getItemStack();
                        if(itemStack.hasDisplayName()){
                            String name = itemStack.getDisplayName();
                            Wtct.proxy.netHandler.sendTo(new SPacketSetItemName(name), sender);
                        }
                        if (sender.openContainer instanceof final ContainerPatternValueName cpv) {
                            if (baseContainer.getTargetStack() instanceof IAEItemStack ais) {
                                cpv.setValueIndex(message.slot);
                                cpv.getPatternValue().putStack(ais.getItemStack());
                            }
                            cpv.detectAndSendChanges();
                        }
                    }
                } else if (message.action == InventoryActionExtend.GET_CRAFTING_STATE) {
                    if(target instanceof IActionHost gh){
                        ICraftingGrid craftingGrid = gh.getActionableNode()
                            .getGrid()
                            .getCache(ICraftingGrid.class);
                        NBTTagCompound cpuData = new NBTTagCompound();
                        NBTTagList tagList = new NBTTagList();
                        cpuData.setTag(Constants.CPU_LIST,tagList);
                        int i = 0;
                        for (ICraftingCPU cpu: craftingGrid.getCpus()) {
                            i++;
                            if(cpu instanceof CraftingCPUCluster ccc && ccc.getFinalOutput() != null){
                                if(message.stack.hashCode() == ccc.getFinalOutput().hashCode()){
                                    IItemList<IAEItemStack> list =  AEApi.instance().storage().createPrimitiveItemList();
                                    ccc.getListOfItem(list,ACTIVE);
                                    List<IAEItemStack> activeItems = getActiveCraftingItems(list);
                                    if(activeItems.isEmpty()){
                                        continue;
                                    }
                                    NBTTagCompound data = new NBTTagCompound();
                                    final String name;
                                    if(ccc.getName().isEmpty()){
                                        name = GuiText.CPUs.getLocal() + ": #" + i;
                                    }else{
                                        name =GuiText.CPUs.getLocal() + ": "  + ccc.getName().substring(0, Math.min(20, ccc.getName().length()));
                                    }
                                    new CPUCraftingPreview(name, ccc.getRemainingItemCount(),ccc.getElapsedTime(),  activeItems).writeToNBT(data);
                                    tagList.appendTag(data);
                                }
                            }
                        }
                        Wtct.proxy.netHandler.sendTo(new SPacketCraftingStateUpdate(cpuData),ctx.getServerHandler().playerEntity);
                    }
                } else if (message.action == InventoryActionExtend.SET_PATTERN_VALUE) {
                    final ContainerOpenContext context = baseContainer.getOpenContext();
                    if (context != null && message.stack != null) {
                        final TileEntity te = context.getTile();
                        if (te != null) {
                            InventoryHandler.openGui(
                                    sender,
                                    te.getWorldObj(),
                                    new BlockPos(te),
                                    Objects.requireNonNull(baseContainer.getOpenContext().getSide()),
                                    GuiType.PATTERN_VALUE_SET);
                        }else{
                            InventoryHandler.openGui(
                                sender,
                                sender.getEntityWorld(),
                                new BlockPos(((IInventorySlotAware)target).getInventorySlot(),0,0),
                                Objects.requireNonNull(baseContainer.getOpenContext().getSide()),
                                GuiType.PATTERN_VALUE_SET_ITEM);
                        }
                        final int amt = getPatternValueAmount(message.stack);
                        Wtct.proxy.netHandler.sendTo(new SPacketSetItemAmount(amt), sender);
                        if (sender.openContainer instanceof final ContainerPatternValueAmount cpv) {
                            if (baseContainer.getTargetStack() instanceof IAEItemStack ais) {
                                cpv.setValueIndex(message.slot);
                                cpv.getPatternValue().putStack(ais.getItemStack());
                            }
                            cpv.detectAndSendChanges();
                        }
                    }
                }
            }
            return null;
        }

        /**
         * 处理样板格的值：流体包的数量存在 NBT（mB）里，ItemStack 的 stackSize 恒为 1，所以不能直接取
         * getStackSize()，否则中键流体时数量框会被预填成 1。
         *
         * <p>
         * The same read serves every fluid carrier, not just the packet: a display item a recipe
         * transfer left in the cell (or a cell an older build left divergent, its number sitting in
         * the stack size while the fluid keeps its own) must open on the number the cell *shows* -
         * the fluid's - or the dialog starts on a value the cell does not display at all.
         */
        private static int getPatternValueAmount(final IAEItemStack stack) {
            final ItemStack is = stack == null ? null : stack.getItemStack();
            if (is != null) {
                return (int) Math.min(Integer.MAX_VALUE, ContainerComprehensiveWorkTerminal.fluidCellAmount(is));
            }
            return (int) stack.getStackSize();
        }

        private List<IAEItemStack> getActiveCraftingItems(IItemList<IAEItemStack> list) {
            List<IAEItemStack> activeItems = new ArrayList<>();
            for (IAEItemStack item : list) {
                activeItems.add(item);
            }
            activeItems.sort(
                Comparator.comparingLong(IAEItemStack::getStackSize)
                    .reversed());
            if (activeItems.size() <= CPUCraftingPreview.maxSize) {
                return activeItems;
            }
            return new ArrayList<>(activeItems.subList(0, CPUCraftingPreview.maxSize));
        }
    }

}
