package com.asdflj.wtct.common.nethub;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraftforge.common.util.Constants;

import com.asdflj.wtct.Wtct;

/**
 * Every hub network and permission group of a world, stored once per save. Networks live in whichever dimension their
 * head does, so the entry point here takes any world and relies on the map storage being shared by all of them.
 */
public class NetHubStorage extends WorldSavedData {

    public static final String NAME = Wtct.MODID + "_nethub";

    private final Map<UUID, Network> networks = new LinkedHashMap<>();
    private final Map<UUID, Group> groups = new LinkedHashMap<>();

    public NetHubStorage(String name) {
        super(name);
    }

    public static NetHubStorage get(World world) {
        NetHubStorage data = null;
        if (world != null && world.mapStorage != null) {
            data = (NetHubStorage) world.mapStorage.loadData(NetHubStorage.class, NAME);
        }
        if (data == null) {
            data = new NetHubStorage(NAME);
            if (world != null && world.mapStorage != null) {
                world.mapStorage.setData(NAME, data);
            }
        }
        return data;
    }

    @Nullable
    public Network getNetwork(@Nullable UUID network) {
        return network == null ? null : networks.get(network);
    }

    public Map<UUID, Network> getNetworks() {
        return networks;
    }

    public Map<UUID, Group> getGroups() {
        return groups;
    }

    public void removeNetwork(@Nullable UUID network) {
        if (network == null) return;
        networks.remove(network);
        this.markDirty();
    }

    @Nullable
    public Group getGroup(@Nullable UUID group) {
        return group == null ? null : groups.get(group);
    }

    @Nullable
    public Group getGroupByOwner(User owner) {
        return this.groups.values()
            .stream()
            .filter(g -> g.isOwner(owner))
            .findFirst()
            .orElse(null);
    }

    /** A player gets one group for all of their networks, created the first time they build one. */
    private Group getOrCreateGroup(User owner) {
        Group group = this.getGroupByOwner(owner);
        if (group == null) {
            group = Group.create(owner);
            this.groups.put(group.getUuid(), group);
        }
        this.markDirty();
        return group;
    }

    public Network createNetwork(EntityPlayer player, String name, boolean overt, BlockPosDim sendPos) {
        User owner = User.create(player, Perm.OWNER);
        Group group = this.getOrCreateGroup(owner);
        Network network = Network.create(group.getUuid(), name, overt, sendPos);
        this.networks.put(network.getUuid(), network);
        this.markDirty();
        return network;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        networks.clear();
        NBTTagList networkList = nbt.getTagList("networks", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < networkList.tagCount(); i++) {
            Network network = Network.create(networkList.getCompoundTagAt(i));
            if (network.getUuid() != null) networks.put(network.getUuid(), network);
        }
        groups.clear();
        NBTTagList groupList = nbt.getTagList("groups", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < groupList.tagCount(); i++) {
            Group group = Group.create(groupList.getCompoundTagAt(i));
            if (group.getUuid() != null) groups.put(group.getUuid(), group);
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        NBTTagList networkList = new NBTTagList();
        NBTTagList groupList = new NBTTagList();
        for (Network network : networks.values()) {
            networkList.appendTag(network.to(new NBTTagCompound()));
        }
        for (Group group : groups.values()) {
            groupList.appendTag(group.to(new NBTTagCompound()));
        }
        tag.setTag("networks", networkList);
        tag.setTag("groups", groupList);
    }
}
