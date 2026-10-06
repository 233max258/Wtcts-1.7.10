package com.asdflj.wtct.common.nethub;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

import com.asdflj.wtct.common.Config;

/**
 * One hub network: a name, who owns it, the block that acts as its head, and the blocks that are plugged into that
 * head. All the members of a network share the owner's {@link Group}.
 *
 * <p>
 * Only the head's position and the group are written to disk. {@link #receivePos} is rebuilt every time the head comes
 * back up, since a connection cannot survive a world reload.
 */
public class Network implements IListObject {

    private final Set<BlockPosDim> receivePos = new HashSet<>();
    private UUID uuid;
    /** The uuid of the {@link Group} that holds the permissions, not a player's uuid. */
    private UUID owner;
    private String name = "Unknown";
    private boolean overt;
    private BlockPosDim sendPos;

    private Network() {}

    public static Network create(NBTTagCompound tag) {
        return new Network().of(tag);
    }

    public static Network create(@Nullable UUID owner, @Nullable String name, boolean overt,
        @Nullable BlockPosDim sendPos) {
        Network network = new Network();
        network.uuid = UUID.randomUUID();
        network.owner = owner;
        network.name = name;
        network.overt = overt;
        network.sendPos = sendPos;
        return network;
    }

    public static Network empty() {
        return new Network();
    }

    /** Whether a hub in {@code dimension} may even reach this network - crossing dimensions is a config option. */
    private boolean checkDimension(int dimension) {
        if (Config.netHubCanCrossDimension) return true;
        return this.sendPos != null && dimension == this.sendPos.getDimension();
    }

    public boolean hasPermission(@Nonnull EntityPlayer player, Perm perm) {
        if (this.uuid == null) return true;
        if (!this.checkDimension(player.worldObj.provider.dimensionId)) return false;
        Group group = NetHubStorage.get(player.worldObj)
            .getGroup(this.owner);
        if (group == null) return false;
        // A public network lets anyone use it, but still reserves managing it for the group.
        if (this.overt && perm == Perm.USER) return true;
        return group.hasPermission(player, perm);
    }

    public void update(NBTTagCompound tag) {
        this.of(tag);
    }

    public Network of(NBTTagCompound tag) {
        if (tag.hasKey("uuid")) this.uuid = NetHubUtil.parseUuid(tag.getString("uuid"));
        if (tag.hasKey("owner")) this.owner = NetHubUtil.parseUuid(tag.getString("owner"));
        if (tag.hasKey("name")) this.name = tag.getString("name");
        if (tag.hasKey("overt")) this.overt = tag.getBoolean("overt");
        if (tag.hasKey("sendPos")) this.sendPos = BlockPosDim.readFromNBT(tag.getCompoundTag("sendPos"));
        return this;
    }

    public NBTTagCompound to(NBTTagCompound tag) {
        if (this.uuid != null) tag.setString("uuid", this.uuid.toString());
        if (this.owner != null) tag.setString("owner", this.owner.toString());
        tag.setString("name", this.name == null ? "Unknown" : this.name);
        tag.setBoolean("overt", this.overt);
        if (this.sendPos != null) tag.setTag("sendPos", this.sendPos.writeToNBT(new NBTTagCompound()));
        return tag;
    }

    @Override
    @Nullable
    public UUID getUuid() {
        return uuid;
    }

    @Nullable
    public UUID getOwner() {
        return owner;
    }

    @Override
    @Nonnull
    public String getName() {
        return name == null ? "Unknown" : name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isOvert() {
        return overt;
    }

    public void setOvert(boolean overt) {
        this.overt = overt;
    }

    @Nullable
    public BlockPosDim getSendPos() {
        return sendPos;
    }

    @Nonnull
    public Set<BlockPosDim> getReceivePos() {
        return receivePos;
    }

    public void addReceivePos(BlockPosDim pos) {
        this.receivePos.add(pos);
    }

    public void removeReceivePos(BlockPosDim pos) {
        this.receivePos.remove(pos);
    }
}
