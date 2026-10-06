package com.asdflj.wtct.common.nethub;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.util.Constants;

import cpw.mods.fml.common.FMLCommonHandler;

/**
 * The owner and the members of one network, i.e. the permission list every network points at by uuid. Several networks
 * share a group when they belong to the same player.
 *
 * <p>
 * Members are only stored when they were granted something; the member screen shows the stored members plus whoever is
 * online, merged in on the fly by {@link #injectOnlinePlayers()}.
 */
public class Group {

    private final List<User> users = new ArrayList<>();
    private UUID uuid;
    private User owner;

    private Group() {}

    public static Group create(NBTTagCompound tag) {
        return new Group().of(tag);
    }

    public static Group create(User owner) {
        Group group = new Group();
        group.uuid = UUID.randomUUID();
        group.owner = owner;
        return group;
    }

    public static Group empty() {
        return new Group();
    }

    /**
     * Adds every player currently on the server to this copy of the group, so the member screen can hand out
     * permissions to someone who is not in the list yet. Only ever called on the copy that is about to be sent to the
     * client.
     */
    public Group injectOnlinePlayers() {
        MinecraftServer server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        if (server != null) {
            for (EntityPlayerMP player : server.getConfigurationManager().playerEntityList) {
                User user = User.create(player.getGameProfile());
                if (!this.users.contains(user) && !user.equals(this.owner)) {
                    this.users.add(user);
                }
            }
        }
        return this;
    }

    /** Puts the owner at the top of the list so the member screen always shows them first. */
    public Group injectOwner() {
        if (this.owner != null && !this.users.contains(this.owner)) {
            this.users.add(0, this.owner);
        }
        return this;
    }

    public void removeUser(User user) {
        users.remove(user);
    }

    public void addUser(User user) {
        if (!users.contains(user)) this.users.add(user);
    }

    /** The owner may do anything on the network; an operator may do anything on any network. */
    public boolean hasPermission(@Nonnull EntityPlayer player, Perm perm) {
        if (NetHubUtil.isPlayerOp(player)) return true;
        User user = this.getUser(User.create(player.getGameProfile()));
        if (user == null) return false;
        Perm userPerm = user.getPerm();
        if (userPerm == null) return false;
        return userPerm.ordinal() >= perm.ordinal();
    }

    @Nullable
    public User getUser(User user) {
        if (user.equals(this.owner)) return this.owner;
        return users.stream()
            .filter(u -> u.isUser(user))
            .findFirst()
            .orElse(null);
    }

    public boolean isOwner(User user) {
        return owner != null && owner.isOwner() && owner.isUser(user);
    }

    /**
     * Merges what the server just sent into the list, without dropping a member the packet happens not to mention.
     * Used by the client's copy of the group.
     */
    public void update(NBTTagCompound tag) {
        if (tag.hasKey("uuid")) this.uuid = NetHubUtil.parseUuid(tag.getString("uuid"));
        if (tag.hasKey("owner")) this.owner = User.create(tag.getCompoundTag("owner"));
        if (tag.hasKey("users")) {
            NBTTagList list = tag.getTagList("users", Constants.NBT.TAG_COMPOUND);
            Set<UUID> uuidsToKeep = new HashSet<>();
            for (int i = 0; i < list.tagCount(); i++) {
                NBTTagCompound nbt = list.getCompoundTagAt(i);
                UUID id = NetHubUtil.parseUuid(nbt.getString("uuid"));
                if (id == null) continue;
                uuidsToKeep.add(id);
                User user = this.users.stream()
                    .filter(u -> id.equals(u.getUuid()))
                    .findFirst()
                    .orElse(null);
                if (user != null) {
                    user.of(nbt);
                } else {
                    this.users.add(User.create(nbt));
                }
            }
            this.users.removeIf(user -> !uuidsToKeep.contains(user.getUuid()));
        }
    }

    public Group of(NBTTagCompound tag) {
        if (tag.hasKey("uuid")) this.uuid = NetHubUtil.parseUuid(tag.getString("uuid"));
        if (tag.hasKey("owner")) this.owner = User.create(tag.getCompoundTag("owner"));
        if (tag.hasKey("users")) {
            this.users.clear();
            NBTTagList list = tag.getTagList("users", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < list.tagCount(); i++) {
                this.addUser(User.create(list.getCompoundTagAt(i)));
            }
        }
        return this;
    }

    public NBTTagCompound to(NBTTagCompound tag) {
        if (this.uuid != null) tag.setString("uuid", this.uuid.toString());
        if (this.owner != null) tag.setTag("owner", this.owner.to(new NBTTagCompound()));
        NBTTagList list = new NBTTagList();
        for (User user : this.users) {
            list.appendTag(user.to(new NBTTagCompound()));
        }
        tag.setTag("users", list);
        return tag;
    }

    public Group deepCopy() {
        return create(this.to(new NBTTagCompound()));
    }

    @Nullable
    public UUID getUuid() {
        return uuid;
    }

    @Nullable
    public User getOwner() {
        return owner;
    }

    @Nonnull
    public List<User> getUsers() {
        return users;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Group group)) return false;
        return Objects.equals(this.uuid, group.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(uuid);
    }
}
