package com.asdflj.wtct.common.nethub;

import java.util.Objects;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

import com.mojang.authlib.GameProfile;

/**
 * One player's entry in a network's permission group: who they are, the name they had when they were added, and what
 * they may do. Doubles as a row in the member list, hence {@link IListObject}.
 */
public class User implements IListObject {

    private UUID uuid;
    private String name;
    private Perm perm;

    private User() {}

    public static User create(NBTTagCompound tag) {
        return new User().of(tag);
    }

    public static User create(@Nullable UUID uuid, @Nullable String name, Perm perm) {
        User user = new User();
        user.uuid = uuid;
        user.name = name;
        user.perm = perm;
        return user;
    }

    public static User create(@Nullable UUID uuid, @Nullable String name) {
        return create(uuid, name, Perm.NONE);
    }

    public static User create(GameProfile profile, Perm perm) {
        return create(profile.getId(), profile.getName(), perm);
    }

    public static User create(GameProfile profile) {
        return create(profile.getId(), profile.getName(), Perm.NONE);
    }

    public static User create(EntityPlayer player, Perm perm) {
        return create(player.getGameProfile(), perm);
    }

    public static User create(EntityPlayer player) {
        return create(player.getGameProfile(), Perm.NONE);
    }

    public static User empty() {
        return new User();
    }

    public boolean isOwner() {
        return this.perm == Perm.OWNER;
    }

    public boolean isAdmin() {
        return this.perm == Perm.ADMIN;
    }

    public boolean isMember() {
        return this.perm == Perm.USER;
    }

    public boolean isGuest() {
        return this.perm == Perm.NONE;
    }

    public void update(NBTTagCompound tag) {
        this.of(tag);
    }

    public User of(NBTTagCompound tag) {
        if (tag.hasKey("uuid")) this.uuid = NetHubUtil.parseUuid(tag.getString("uuid"));
        if (tag.hasKey("name")) this.name = tag.getString("name");
        if (tag.hasKey("perm")) {
            try {
                this.perm = Perm.valueOf(tag.getString("perm"));
            } catch (IllegalArgumentException e) {
                this.perm = Perm.NONE;
            }
        }
        return this;
    }

    public NBTTagCompound to(NBTTagCompound tag) {
        if (this.uuid != null) tag.setString("uuid", this.uuid.toString());
        tag.setString("name", this.name == null ? "" : this.name);
        tag.setString("perm", this.perm == null ? Perm.NONE.name() : this.perm.name());
        return tag;
    }

    public User deepCopy() {
        return create(this.to(new NBTTagCompound()));
    }

    @Override
    @Nullable
    public UUID getUuid() {
        return uuid;
    }

    @Override
    @Nullable
    public String getName() {
        return name;
    }

    @Nullable
    public Perm getPerm() {
        return perm;
    }

    public void setPerm(Perm perm) {
        this.perm = perm;
    }

    public boolean isUser(User user) {
        return this.equals(user);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof User user)) return false;
        return Objects.equals(this.uuid, user.uuid) && Objects.equals(this.name, user.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, name);
    }
}
