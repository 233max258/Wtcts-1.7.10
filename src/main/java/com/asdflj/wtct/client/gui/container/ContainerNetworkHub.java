package com.asdflj.wtct.client.gui.container;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.Constants;

import com.asdflj.wtct.common.nethub.BlockPosDim;
import com.asdflj.wtct.common.nethub.Group;
import com.asdflj.wtct.common.nethub.NetHubStorage;
import com.asdflj.wtct.common.nethub.NetHubUtil;
import com.asdflj.wtct.common.nethub.Network;
import com.asdflj.wtct.common.nethub.Perm;
import com.asdflj.wtct.common.nethub.User;
import com.asdflj.wtct.common.tile.TileNetworkHub;
import com.asdflj.wtct.util.NameConst;

import appeng.container.AEBaseContainer;
import appeng.container.sync.ActionHandler;
import appeng.container.sync.StreamCodec;
import appeng.container.sync.StreamCodecs;
import cpw.mods.fml.common.network.ByteBufUtils;

/**
 * The network screen's container: it owns the network the player picked, and every button on the screen turns into an
 * action here.
 *
 * <p>
 * State travels through AE2's container sync instead of a packet of our own. The client sends actions up the
 * {@code action} handler and the server pushes the whole screenful of state back down the {@code sync} handler, which
 * keeps the window id validation and the "container went away" handling for free.
 */
public class ContainerNetworkHub extends AEBaseContainer {

    /** Wire format of {@code action}: the action's name, then its payload. */
    private static final StreamCodec<NBTTagCompound> ACTION_CODEC = StreamCodecs
        .of("wtct_nethub_action", (buf, tag) -> {
            ByteBufUtils.writeUTF8String(buf, tag == null ? "" : tag.getString("type"));
            ByteBufUtils.writeTag(buf, tag);
        }, buf -> {
            String type = ByteBufUtils.readUTF8String(buf);
            NBTTagCompound tag = ByteBufUtils.readTag(buf);
            if (tag == null) tag = new NBTTagCompound();
            tag.setString("type", type);
            return tag;
        });

    /** Wire format of {@code sync}: the state compound, exactly as {@link #getSyncData} built it. */
    private static final StreamCodec<NBTTagCompound> SYNC_CODEC = StreamCodecs
        .of("wtct_nethub_sync", ByteBufUtils::writeTag, buf -> {
            NBTTagCompound tag = ByteBufUtils.readTag(buf);
            return tag == null ? new NBTTagCompound() : tag;
        });

    public final TileNetworkHub hub;
    @Nullable
    public UUID selectedNetwork;
    @Nullable
    public Integer surplusChannels;
    /** On the server these are the storage's own maps; on the client they are the copy the sync rebuilt. */
    public Map<UUID, Network> networks = new LinkedHashMap<>();
    public Map<UUID, Group> groups = new LinkedHashMap<>();

    private final ActionHandler<NBTTagCompound> actionHandler;
    private final ActionHandler<NBTTagCompound> syncHandler;

    public ContainerNetworkHub(InventoryPlayer ip, TileNetworkHub hub) {
        super(ip, hub);
        this.hub = hub;
        boolean server = !ip.player.worldObj.isRemote;

        if (server) {
            NetHubStorage storage = NetHubStorage.get(hub.getWorldObj());
            this.networks = storage.getNetworks();
            this.groups = storage.getGroups();
            this.selectedNetwork = hub.getNetworkUuid();
        }

        this.actionHandler = this.syncRegistrar()
            .actionC2S("action", ACTION_CODEC);
        this.syncHandler = this.syncRegistrar()
            .actionS2C("sync", SYNC_CODEC);
        if (server) {
            this.actionHandler.onServerAction(this::onAction);
            this.sync();
        } else {
            this.syncHandler.onClientAction(this::doSyncFrom);
        }
    }

    /** Sends one of the screen's buttons up to the server. Client side only. */
    public void sendAction(String type) {
        this.sendAction(type, new NBTTagCompound());
    }

    public void sendAction(String type, NBTTagCompound payload) {
        payload.setString("type", type);
        this.actionHandler.send(payload);
    }

    /** Picks a network by uuid; 1.7.10 has no uuid tag type, so it travels as a plain string. */
    public void sendAction(String type, UUID uuid) {
        NBTTagCompound payload = new NBTTagCompound();
        payload.setString("uuid", uuid.toString());
        this.sendAction(type, payload);
    }

    /** Carries a bare string, which is all the "create network" button needs to pass along. */
    public void sendAction(String type, String string) {
        NBTTagCompound payload = new NBTTagCompound();
        payload.setString("string", string);
        this.sendAction(type, payload);
    }

    @Nullable
    public Network getSelected() {
        Network network = this.networks.get(this.selectedNetwork);
        return network == null ? Network.empty() : network;
    }

    public Group getGroup() {
        Group group = this.groups.get(
            this.getSelected()
                .getOwner());
        return group == null ? Group.empty() : group;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        if (!this.isValidContainer()) return false;
        return player.getDistanceSq(this.hub.xCoord + 0.5D, this.hub.yCoord + 0.5D, this.hub.zCoord + 0.5D) <= 64.0D;
    }

    @Override
    public void detectAndSendChanges() {
        if (!this.getInventoryPlayer().player.worldObj.isRemote) {
            EntityPlayer player = this.getInventoryPlayer().player;
            boolean gone = this.hub.isInvalid() || this.hub.getWorldObj() == null
                || this.hub.getWorldObj()
                    .getTileEntity(this.hub.xCoord, this.hub.yCoord, this.hub.zCoord) != this.hub;
            if (gone || !this.getSelected()
                .hasPermission(player, Perm.USER)) {
                // The block is gone or the player lost access: let AE2 close the screen instead of yanking it here.
                this.setValidContainer(false);
            }
        }
        super.detectAndSendChanges();
    }

    /** Runs one of the screen's buttons. Server side only, reached through the {@code action} handler. */
    private void onAction(NBTTagCompound payload) {
        if (this.getInventoryPlayer().player.worldObj.isRemote) return;
        EntityPlayer player = this.getInventoryPlayer().player;
        NetHubStorage storage = NetHubStorage.get(this.hub.getWorldObj());
        switch (payload.getString("type")) {
            case "switch-network" -> {
                UUID uuid = NetHubUtil.parseUuid(payload.getString("uuid"));
                Network network = storage.getNetwork(uuid);
                if (network != null && network.hasPermission(player, Perm.USER)) {
                    this.selectedNetwork = uuid;
                }
                this.sync();
            }
            case "create-network" -> {
                Network network = storage.createNetwork(
                    player,
                    payload.getString("string"),
                    false,
                    new BlockPosDim(
                        this.hub.xCoord,
                        this.hub.yCoord,
                        this.hub.zCoord,
                        this.hub.getWorldObj().provider.dimensionId));
                this.hub.setHead(true);
                this.hub.setNetworkUuid(network.getUuid());
                this.selectedNetwork = network.getUuid();
                storage.markDirty();
                this.hub.sync();
                this.sync();
            }
            case "change-player-permission" -> {
                UUID uuid = NetHubUtil.parseUuid(payload.getString("user"));
                String name = payload.getString("name");
                Group group = this.getGroup();
                if (group.hasPermission(player, Perm.ADMIN)) {
                    User user = group.getUser(User.create(uuid, name));
                    if (user != null) {
                        if (user.getPerm() == Perm.ADMIN && group.hasPermission(player, Perm.OWNER)) {
                            group.removeUser(user);
                        }
                        if (user.getPerm() == Perm.USER) {
                            if (group.hasPermission(player, Perm.OWNER)) {
                                user.setPerm(Perm.ADMIN);
                            } else {
                                group.removeUser(user);
                            }
                        }
                        if (user.getPerm() == Perm.NONE) {
                            user.setPerm(Perm.USER);
                        }
                    } else {
                        group.addUser(User.create(uuid, name, Perm.USER));
                    }
                    storage.markDirty();
                    this.hub.sync();
                    this.sync();
                } else {
                    this.noPermission(player);
                }
            }
            case "delete-network" -> {
                Network network = storage.getNetwork(this.selectedNetwork);
                if (network != null && network.hasPermission(player, Perm.ADMIN)) {
                    storage.removeNetwork(this.selectedNetwork);
                    this.selectedNetwork = Objects.equals(this.selectedNetwork, this.hub.getNetworkUuid()) ? null
                        : this.hub.getNetworkUuid();
                    storage.markDirty();
                    this.hub.sync();
                    this.sync();
                } else {
                    this.noPermission(player);
                }
            }
            case "connect-network" -> {
                Network network = storage.getNetwork(this.selectedNetwork);
                if (network != null && network.hasPermission(player, Perm.USER)) {
                    if (!Objects.equals(this.selectedNetwork, this.hub.getNetworkUuid())) {
                        this.hub.breakConnection();
                    }
                    storage.markDirty();
                    this.hub.setNetworkUuid(this.selectedNetwork);
                    this.hub.sync();
                    this.sync();
                } else {
                    this.noPermission(player);
                }
            }
            case "disconnect-network" -> {
                Network network = storage.getNetwork(this.hub.getNetworkUuid());
                if (network != null && network.hasPermission(player, Perm.USER)) {
                    this.hub.breakConnection();
                    this.selectedNetwork = null;
                    storage.markDirty();
                    this.hub.sync();
                    this.sync();
                } else {
                    this.noPermission(player);
                }
            }
            case "switch-public" -> {
                Network network = storage.getNetwork(this.selectedNetwork);
                if (network != null && network.hasPermission(player, Perm.ADMIN)) {
                    network.setOvert(!network.isOvert());
                    storage.markDirty();
                    this.hub.sync();
                    this.sync();
                } else {
                    this.noPermission(player);
                }
            }
            default -> {}
        }
    }

    private void noPermission(EntityPlayer player) {
        player.addChatMessage(new ChatComponentTranslation(NameConst.MESSAGE_NETWORK_HUB_NO_PERMISSION));
    }

    /**
     * Pushes the screen's state to the client. The channel count has to come from whichever hub is the head, which on a
     * receiver means reaching into another world.
     */
    public void sync() {
        if (this.getInventoryPlayer().player.worldObj.isRemote) return;
        if (this.hub.isHead()) {
            this.surplusChannels = this.hub.getSurplusChannels();
        } else if (this.selectedNetwork != null) {
            BlockPosDim pos = this.getSelected()
                .getSendPos();
            if (pos != null) {
                World thatWorld = DimensionManager.getWorld(pos.getDimension());
                if (thatWorld != null && thatWorld.blockExists(pos.getX(), pos.getY(), pos.getZ())) {
                    TileEntity tile = thatWorld.getTileEntity(pos.getX(), pos.getY(), pos.getZ());
                    if (tile instanceof TileNetworkHub that) {
                        this.surplusChannels = that.getSurplusChannels();
                    }
                }
            }
        }
        this.syncHandler.send(this.getSyncData(new NBTTagCompound()));
    }

    /** Everything the screen draws: the selected network, the channel count, and the networks the player may use. */
    public NBTTagCompound getSyncData(NBTTagCompound tag) {
        EntityPlayer player = this.getInventoryPlayer().player;
        if (this.selectedNetwork != null) tag.setString("networkUuid", this.selectedNetwork.toString());
        if (this.surplusChannels != null) tag.setInteger("surplusChannels", this.surplusChannels);
        NBTTagList networkList = new NBTTagList();
        NBTTagList groupList = new NBTTagList();
        for (Network network : this.networks.values()) {
            if (network.hasPermission(player, Perm.USER)) {
                networkList.appendTag(network.to(new NBTTagCompound()));
            }
        }
        for (Group group : this.groups.values()) {
            if (group.hasPermission(player, Perm.USER)) {
                groupList.appendTag(
                    group.deepCopy()
                        .injectOnlinePlayers()
                        .injectOwner()
                        .to(new NBTTagCompound()));
            }
        }
        tag.setTag("networks", networkList);
        tag.setTag("groups", groupList);
        return tag;
    }

    /** Client side: rebuilds the screen's state from what the server sent. */
    public void doSyncFrom(NBTTagCompound tag) {
        this.selectedNetwork = tag.hasKey("networkUuid") ? NetHubUtil.parseUuid(tag.getString("networkUuid")) : null;
        this.surplusChannels = tag.hasKey("surplusChannels") ? tag.getInteger("surplusChannels") : null;
        this.updateNetworks(tag);
        this.updateGroups(tag);
    }

    private void updateNetworks(NBTTagCompound tag) {
        NBTTagList list = tag.getTagList("networks", Constants.NBT.TAG_COMPOUND);
        Set<UUID> keep = new HashSet<>();
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound nbt = list.getCompoundTagAt(i);
            UUID uuid = NetHubUtil.parseUuid(nbt.getString("uuid"));
            if (uuid == null) continue;
            keep.add(uuid);
            Network network = this.networks.get(uuid);
            if (network != null) {
                network.update(nbt);
            } else {
                this.networks.put(uuid, Network.create(nbt));
            }
        }
        this.networks.keySet()
            .removeIf(uuid -> !keep.contains(uuid));
    }

    private void updateGroups(NBTTagCompound tag) {
        NBTTagList list = tag.getTagList("groups", Constants.NBT.TAG_COMPOUND);
        Set<UUID> keep = new HashSet<>();
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound nbt = list.getCompoundTagAt(i);
            UUID uuid = NetHubUtil.parseUuid(nbt.getString("uuid"));
            if (uuid == null) continue;
            keep.add(uuid);
            Group group = this.groups.get(uuid);
            if (group != null) {
                group.update(nbt);
            } else {
                this.groups.put(uuid, Group.create(nbt));
            }
        }
        this.groups.keySet()
            .removeIf(uuid -> !keep.contains(uuid));
    }
}
