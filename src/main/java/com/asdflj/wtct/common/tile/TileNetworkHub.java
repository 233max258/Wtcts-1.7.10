package com.asdflj.wtct.common.tile;

import java.util.HashSet;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.ForgeDirection;

import com.asdflj.wtct.common.Config;
import com.asdflj.wtct.common.block.BlockNetworkHub;
import com.asdflj.wtct.common.nethub.BlockPosDim;
import com.asdflj.wtct.common.nethub.NetHubPowerUsage;
import com.asdflj.wtct.common.nethub.NetHubStorage;
import com.asdflj.wtct.common.nethub.NetHubUtil;
import com.asdflj.wtct.common.nethub.Network;
import com.asdflj.wtct.util.BlockPos;

import appeng.api.AEApi;
import appeng.api.exceptions.FailedConnection;
import appeng.api.implementations.IPowerChannelState;
import appeng.api.implementations.parts.IPartCable;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridConnection;
import appeng.api.networking.IGridNode;
import appeng.api.util.AECableType;
import appeng.me.GridNode;
import appeng.tile.TileEvent;
import appeng.tile.events.TileEventType;
import appeng.tile.grid.AENetworkTile;
import io.netty.buffer.ByteBuf;

/**
 * A block that hard-links two pieces of ME network into one, even across dimensions.
 *
 * <p>
 * The block a network was created on is its <em>head</em>; every other hub that is plugged into the network is a
 * <em>receiver</em> and holds a single grid connection to the head. Because a grid connection cannot outlive a world
 * reload, only the head's position and the network's name are saved - receivers find their head again through
 * {@link Network#getSendPos()} and rebuild the connection from {@link #setupConnection}.
 */
public class TileNetworkHub extends AENetworkTile implements IPowerChannelState {

    private boolean isHead;
    private UUID networkUuid;
    private boolean isConnected;
    /** Whether this hub sits on a powered ME network, synced to the client so the probe can be answered there. */
    private boolean online;
    private double power = Config.netHubPowerBase;
    private int surplusChannels;
    private IGridConnection connection;
    private int tickCounter;

    public TileNetworkHub() {
        // A hub has to be able to carry a whole dense cable's worth of channels across the link.
        this.getProxy()
            .setFlags(GridFlags.DENSE_CAPACITY);
    }

    @Override
    public AECableType getCableConnectionType(ForgeDirection dir) {
        return AECableType.DENSE;
    }

    /**
     * The grid is only consulted periodically: the work here is a handful of grid lookups and a block update, and the
     * connection cannot change faster than once a second anyway.
     */
    @TileEvent(TileEventType.TICK)
    public void onTickEvent() {
        if (this.worldObj == null || this.worldObj.isRemote) return;
        if (++this.tickCounter < 20) return;
        this.tickCounter = 0;
        this.onTick();
    }

    private void onTick() {
        boolean online = this.isOnline();
        if (online != this.online) {
            this.online = online;
            this.sync();
        }

        if (this.networkUuid == null) return;
        NetHubStorage storage = NetHubStorage.get(this.worldObj);
        Network network = storage.getNetwork(this.networkUuid);
        if (network == null) {
            this.unsetAll();
            return;
        }

        BlockPosDim pos = network.getSendPos();
        if (!this.isHead && pos != null
            && pos.getDimension() == this.worldObj.provider.dimensionId
            && pos.getX() == this.xCoord
            && pos.getY() == this.yCoord
            && pos.getZ() == this.zCoord) {
            this.setHead(true);
        }

        if (this.isHead) {
            if (this.isConnected) {
                this.setConnected(
                    !network.getReceivePos()
                        .isEmpty());
            }
            this.surplusChannels = Math.max(this.getMaxChannels() - this.getUsedChannels(), 0);
            this.sync();
        }

        if (!this.isHead && this.connection == null) {
            this.setupConnection(network);
        }
    }

    /**
     * Two hubs that have only found each other are not on a network yet - the link is up, but nothing is wired in. Any
     * other neighbour (cable, machine, part) means there is an ME network here.
     */
    private boolean isOnNetwork() {
        IGridNode node = this.getProxy()
            .getNode();
        if (node == null) return false;
        for (IGridConnection gc : node.getConnections()) {
            if (!(gc.getOtherSide(node)
                .getMachine() instanceof TileNetworkHub)) return true;
        }
        return false;
    }

    /** Being wired in is not enough - the network it is wired into has to actually have power. */
    private boolean isOnline() {
        return this.isOnNetwork() && this.getProxy()
            .isPowered();
    }

    /**
     * The hub is rated for a dense cable, but only as far as whatever feeds it actually reaches: a dense-rated hub
     * behind a smart cable is an 8 channel link. The neighbours cannot be asked with {@link GridNode#getMaxChannels()}
     * however, because that only reads their own flags - an ME controller is flagged {@link GridFlags#CANNOT_CARRY} and
     * would come back as zero even though it is the one block that hands out the full 32. So the attached cables speak
     * for the hub instead: only cables limit it, machines and the controller do not.
     */
    private int getMaxChannels() {
        IGridNode node = this.getProxy()
            .getNode();
        if (!(node instanceof GridNode own)) return 0;
        int max = own.getMaxChannels();
        for (IGridConnection gc : node.getConnections()) {
            if (!(gc.getOtherSide(node)
                .getMachine() instanceof IPartCable cable)) continue;
            AECableType type = cable.getCableConnectionType();
            max = Math.min(max, type == AECableType.DENSE || type == AECableType.DENSE_COVERED ? 32 : 8);
        }
        return max;
    }

    /** The widest connection already hanging off this hub, i.e. what the rest of the cable has left to give. */
    private int getUsedChannels() {
        IGridNode node = this.getActionableNode();
        if (node == null) return 0;
        int howMany = 0;
        for (IGridConnection gc : node.getConnections()) {
            howMany = Math.max(gc.getUsedChannels(), howMany);
        }
        return howMany;
    }

    /**
     * Repaints the block from the current connection state and pushes the state to the clients tracking it, so WAILA
     * and the screen agree with the server.
     */
    public void sync() {
        if (this.worldObj == null || this.worldObj.isRemote) return;
        if (this.worldObj.getBlock(this.xCoord, this.yCoord, this.zCoord) != BlockNetworkHub.INSTANCE) return;
        int meta = this.isConnected ? 1 : 0;
        if (this.worldObj.getBlockMetadata(this.xCoord, this.yCoord, this.zCoord) != meta) {
            this.worldObj.setBlockMetadataWithNotify(this.xCoord, this.yCoord, this.zCoord, meta, 3);
        }
        this.markForUpdate();
    }

    /** Hooks this receiver up to the head the network points at, and bills both ends for the distance. */
    public void setupConnection(Network network) {
        if (this.worldObj == null || this.worldObj.isRemote) return;
        BlockPosDim pos = network.getSendPos();
        if (pos == null) return;
        World thatWorld = DimensionManager.getWorld(pos.getDimension());
        if (thatWorld == null || !thatWorld.blockExists(pos.getX(), pos.getY(), pos.getZ())) return;
        TileEntity tile = thatWorld.getTileEntity(pos.getX(), pos.getY(), pos.getZ());
        if (!(tile instanceof TileNetworkHub that)) {
            // The head is gone; the network has nobody left to connect to.
            NetHubStorage.get(thatWorld)
                .removeNetwork(this.networkUuid);
            return;
        }
        IGridNode here = this.getActionableNode();
        IGridNode there = that.getActionableNode();
        if (here == null || there == null) return;

        this.power = NetHubPowerUsage.calc(
            new BlockPos(this.xCoord, this.yCoord, this.zCoord),
            new BlockPos(that.xCoord, that.yCoord, that.zCoord),
            this.worldObj.provider.dimensionId,
            thatWorld.provider.dimensionId);
        try {
            this.connection = AEApi.instance()
                .createGridConnection(here, there);
            this.setConnected(true);
            that.setConnected(true);
            this.getProxy()
                .setIdlePowerUsage(this.power);
            network.addReceivePos(
                new BlockPosDim(this.xCoord, this.yCoord, this.zCoord, this.worldObj.provider.dimensionId));
            this.sync();
            that.sync();
        } catch (FailedConnection e) {
            this.unsetAll();
        }
    }

    /**
     * Drops this hub out of its network. On the head that tears down every receiver and the network itself; on a
     * receiver it only removes the receiver.
     */
    public void breakConnection() {
        if (this.worldObj == null || this.worldObj.isRemote) return;
        NetHubStorage storage = NetHubStorage.get(this.worldObj);
        Network network = storage.getNetwork(this.networkUuid);
        if (network == null) {
            this.unsetAll();
            return;
        }
        if (this.isHead) {
            for (BlockPosDim pos : new HashSet<>(network.getReceivePos())) {
                World thatWorld = DimensionManager.getWorld(pos.getDimension());
                if (thatWorld == null) continue;
                TileEntity tile = thatWorld.getTileEntity(pos.getX(), pos.getY(), pos.getZ());
                if (tile instanceof TileNetworkHub hub) {
                    hub.breakConnection();
                }
            }
            storage.removeNetwork(this.networkUuid);
        } else {
            network.removeReceivePos(
                new BlockPosDim(this.xCoord, this.yCoord, this.zCoord, this.worldObj.provider.dimensionId));
        }
        storage.markDirty();
        this.unsetAll();
    }

    /** Forgets the network locally and drops the grid connection, without touching the stored network. */
    public void unsetAll() {
        this.setHead(false);
        this.setConnected(false);
        this.setNetworkUuid(null);
        if (this.connection != null) {
            this.connection.destroy();
            this.connection = null;
        }
        this.getProxy()
            .setIdlePowerUsage(0);
        this.sync();
    }

    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
        if (this.worldObj == null) return;
        NetHubStorage storage = NetHubStorage.get(this.worldObj);
        Network network = storage.getNetwork(this.networkUuid);
        if (this.connection != null) {
            this.connection.destroy();
            this.connection = null;
        }
        this.setConnected(false);
        if (network == null) {
            this.setHead(false);
            this.setNetworkUuid(null);
            return;
        }
        network.removeReceivePos(
            new BlockPosDim(this.xCoord, this.yCoord, this.zCoord, this.worldObj.provider.dimensionId));
        storage.markDirty();
    }

    @TileEvent(TileEventType.WORLD_NBT_READ)
    public void readFromNBT_NetHub(NBTTagCompound tag) {
        this.networkUuid = NetHubUtil.parseUuid(tag.getString("networkUuid"));
    }

    @TileEvent(TileEventType.WORLD_NBT_WRITE)
    public void writeToNBT_NetHub(NBTTagCompound tag) {
        if (this.networkUuid != null) tag.setString("networkUuid", this.networkUuid.toString());
    }

    @TileEvent(TileEventType.NETWORK_WRITE)
    public void writeToStream_NetHub(ByteBuf data) {
        data.writeBoolean(this.isConnected);
        data.writeBoolean(this.isHead);
        data.writeBoolean(this.online);
        data.writeDouble(this.power);
        data.writeInt(this.surplusChannels);
    }

    @TileEvent(TileEventType.NETWORK_READ)
    public boolean readFromStream_NetHub(ByteBuf data) {
        this.isConnected = data.readBoolean();
        this.isHead = data.readBoolean();
        this.online = data.readBoolean();
        this.power = data.readDouble();
        this.surplusChannels = data.readInt();
        return true;
    }

    public void setOwner(EntityPlayer player) {
        this.getProxy()
            .setOwner(player);
    }

    /**
     * AE2 asks every {@link IPowerChannelState} tile how it is doing, and the line it prints answers "is this device on
     * a live ME network?". For the hub that means two things: it has to be wired into a network (not merely talking to
     * another hub, which gets its own line) and that network has to have power. The value is computed on the server and
     * streamed over, because a client-side grid does not exist to ask.
     */
    @Override
    public boolean isPowered() {
        return this.online;
    }

    @Override
    public boolean isActive() {
        return this.online;
    }

    public boolean isConnected() {
        return this.isConnected;
    }

    public void setConnected(boolean connected) {
        this.isConnected = connected;
    }

    public boolean isHead() {
        return this.isHead;
    }

    public void setHead(boolean head) {
        this.isHead = head;
    }

    public double getPower() {
        return power;
    }

    public int getSurplusChannels() {
        return surplusChannels;
    }

    @Nullable
    public UUID getNetworkUuid() {
        return this.networkUuid;
    }

    public void setNetworkUuid(@Nullable UUID networkUuid) {
        this.networkUuid = networkUuid;
    }
}
