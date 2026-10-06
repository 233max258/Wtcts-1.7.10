package com.asdflj.wtct.util;

import static com.glodblock.github.util.Ae2Reflect.readField;
import static com.glodblock.github.util.Ae2Reflect.reflectField;
import static com.glodblock.github.util.Ae2Reflect.reflectMethod;
import static com.glodblock.github.util.Ae2Reflect.writeField;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import appeng.api.networking.crafting.ICraftingCallback;
import appeng.api.networking.crafting.ICraftingJob;
import appeng.api.storage.IMEInventory;
import appeng.container.implementations.ContainerCraftConfirm;
import appeng.container.implementations.ContainerInterfaceTerminal;
import appeng.container.slot.SlotCraftingTerm;
import appeng.core.sync.packets.PacketInterfaceTerminalUpdate;
import appeng.crafting.v2.CraftingJobV2;
import appeng.me.Grid;
import appeng.me.GridStorage;
import appeng.me.storage.MEInventoryHandler;
import appeng.me.storage.MEPassThrough;
import appeng.util.prioitylist.IPartitionList;

/**
 * The AE2 internals this mod reaches into, looked up by name.
 *
 * <p>
 * Every lookup is optional: the classes are named as strings and each one is resolved on its own, so
 * a build of AE2 (or of AE2FluidCraft) that renamed or dropped one member leaves that one feature
 * unavailable instead of killing the class. The earlier all-or-nothing static block threw on the
 * first miss, and a class whose initialiser has thrown can never be initialised again - every later
 * use (including the server's container tick, via {@code ProviderSlotSync}) then died with
 * {@code NoClassDefFoundError: Could not initialize class Ae2Reflect}, which is exactly the crash
 * this guards against.
 *
 * <p>
 * The accessors below therefore answer with {@code null} (or a harmless default) when their member is
 * missing, never by dereferencing a null {@link Field} - AE2FluidCraft's {@code readField} throws on
 * a null field, so the guard has to live here.
 */
public class Ae2Reflect {

    private static final Field fAEPass_internal;
    private static final Field fAEInv_partitionList;
    private static final Field fContainerInterfaceTerminal_tracked;
    private static final Field fContainerInterfaceTerminal_trackedById;
    private static final Field fContainerInterfaceTerminal_dirty;
    private static final Field fContainerInterfaceTerminal_isDirty;
    private static final Field fInvTracker_patterns;
    private static final Field fInvTracker_id;
    private static final Field fInvTracker_name;
    private static final Field fInvTracker_x;
    private static final Field fInvTracker_y;
    private static final Field fInvTracker_z;
    private static final Field fInvTracker_dim;
    private static final Field fInvTracker_side;
    private static final Method mInvTracker_updateNBT;
    private static final Field fCraftingJobV2_callback;
    private static final Field fGrid_myStorage;
    private static final Field fContainerCraftConfirm_result;
    private static final Method mSlotCraftingTerm_makeItem;

    /** One line per member that could not be resolved - logged once, never thrown. */
    private static final Set<String> MISSING = new java.util.HashSet<>();

    private static final String INTERFACE_TERMINAL = "appeng.container.implementations.ContainerInterfaceTerminal";
    private static final String INV_TRACKER = INTERFACE_TERMINAL + "$InvTracker";

    static {
        fAEPass_internal = field("appeng.me.storage.MEPassThrough", "internal");
        fAEInv_partitionList = field("appeng.me.storage.MEInventoryHandler", "myPartitionList");
        fCraftingJobV2_callback = field("appeng.crafting.v2.CraftingJobV2", "callback");
        fGrid_myStorage = field("appeng.me.Grid", "myStorage");
        fContainerInterfaceTerminal_tracked = field(INTERFACE_TERMINAL, "tracked");
        fContainerInterfaceTerminal_trackedById = field(INTERFACE_TERMINAL, "trackedById");
        // The pending update packet and its dirty flag: the only route by which a slot's *contents*
        // ever reach the client when AE2's own slot actions were not the ones that changed them.
        fContainerInterfaceTerminal_dirty = field(INTERFACE_TERMINAL, "dirty");
        fContainerInterfaceTerminal_isDirty = field(INTERFACE_TERMINAL, "isDirty");
        // InvTracker is a private nested class, so its own fields have to be looked up by name.
        fInvTracker_patterns = field(INV_TRACKER, "patterns");
        fInvTracker_id = field(INV_TRACKER, "id");
        fInvTracker_name = field(INV_TRACKER, "name");
        // The row also remembers where its interface stands, which is what lets the terminal open
        // the machine that interface feeds and point the player at it.
        fInvTracker_x = field(INV_TRACKER, "x");
        fInvTracker_y = field(INV_TRACKER, "y");
        fInvTracker_z = field(INV_TRACKER, "z");
        fInvTracker_dim = field(INV_TRACKER, "dim");
        fInvTracker_side = field(INV_TRACKER, "side");
        mInvTracker_updateNBT = method(INV_TRACKER, "updateNBT");
        mSlotCraftingTerm_makeItem = method(
            "appeng.container.slot.SlotCraftingTerm",
            "makeItem",
            EntityPlayer.class,
            ItemStack.class);
        fContainerCraftConfirm_result = field("appeng.container.implementations.ContainerCraftConfirm", "result");
    }

    /** Resolves one field, or null when this AE2 build does not have it. Never throws. */
    private static Field field(final String className, final String name) {
        try {
            return reflectField(Class.forName(className), name);
        } catch (final Throwable t) {
            note(className, name, t);
            return null;
        }
    }

    /** Resolves one method, or null when this AE2 build does not have it. Never throws. */
    private static Method method(final String className, final String name, final Class<?>... params) {
        try {
            return reflectMethod(Class.forName(className), name, params);
        } catch (final Throwable t) {
            note(className, name, t);
            return null;
        }
    }

    private static void note(final String owner, final String member, final Throwable t) {
        if (MISSING.add(owner + "#" + member)) {
            System.out.println(
                "[Wtct] AE2 internals not found, that feature is off: " + owner + "#" + member + " (" + t + ")");
        }
    }

    public static IPartitionList<?> getPartitionList(MEInventoryHandler<?> me) {
        return fAEInv_partitionList == null ? null : readField(me, fAEInv_partitionList);
    }

    public static IMEInventory<?> getInternal(MEPassThrough<?> me) {
        return fAEPass_internal == null ? null : readField(me, fAEPass_internal);
    }

    /**
     * AE2's interface-terminal row map. An empty, MUTABLE map when the member is missing: callers
     * iterate it every tick (ProviderSlotSync runs off the server's container tick), and an empty
     * list of rows is exactly what a terminal with nothing to show has - never a null they would
     * dereference, and never an immutable one a caller might write to.
     */
    public static Map getTracked(ContainerInterfaceTerminal obj) {
        if (obj == null || fContainerInterfaceTerminal_tracked == null) {
            return new java.util.HashMap();
        }
        final Map tracked = readField(obj, fContainerInterfaceTerminal_tracked);
        return tracked == null ? new java.util.HashMap() : tracked;
    }

    /**
     * The AE2 interface-terminal row (its private {@code InvTracker}) for an entry id, so a caller
     * can read/write that machine's pattern inventory. {@code null} when the id is unknown.
     */
    public static Object getTrackedById(ContainerInterfaceTerminal obj, long id) {
        if (fContainerInterfaceTerminal_trackedById == null) {
            return null;
        }
        final Map map = readField(obj, fContainerInterfaceTerminal_trackedById);
        return map == null ? null : map.get(id);
    }

    /** {@code InvTracker#patterns} - the pattern inventory behind an interface-terminal row. */
    public static IInventory getTrackedPatterns(Object tracker) {
        return tracker == null || fInvTracker_patterns == null ? null : readField(tracker, fInvTracker_patterns);
    }

    /** {@code InvTracker#id} - the row id the client uses when it asks for an action. */
    public static long getTrackedId(Object tracker) {
        if (tracker == null || fInvTracker_id == null) {
            return -1;
        }
        final Long id = readField(tracker, fInvTracker_id);
        return id == null ? -1 : id;
    }

    /** {@code InvTracker#name} - the row's display name. */
    public static String getTrackedName(Object tracker) {
        return tracker == null || fInvTracker_name == null ? null : readField(tracker, fInvTracker_name);
    }

    /** {@code InvTracker#x/y/z/dim} - where the row's interface sits, as {@code {x, y, z, dim}}. */
    public static int[] getTrackedLocation(final Object tracker) {
        if (tracker == null || fInvTracker_x == null || fInvTracker_y == null || fInvTracker_z == null) {
            return null;
        }
        final Integer x = readField(tracker, fInvTracker_x);
        final Integer y = readField(tracker, fInvTracker_y);
        final Integer z = readField(tracker, fInvTracker_z);
        final Integer dim = fInvTracker_dim == null ? null : readField(tracker, fInvTracker_dim);
        if (x == null || y == null || z == null) {
            return null;
        }
        return new int[] { x, y, z, dim == null ? 0 : dim };
    }

    /**
     * The world the row's interface is in. rv3-beta-977's {@code InvTracker} carries only the dimension - its
     * {@code world} field arrived in a later AE2 build - so the world is resolved from that dimension instead.
     */
    public static net.minecraft.world.World getTrackedWorld(final Object tracker) {
        if (tracker == null || fInvTracker_dim == null) {
            return null;
        }
        final Integer dim = readField(tracker, fInvTracker_dim);
        final net.minecraft.server.MinecraftServer server = net.minecraft.server.MinecraftServer.getServer();
        if (dim == null || server == null) {
            return null;
        }
        return server.worldServerForDimension(dim);
    }

    /** {@code InvTracker#side} - the part's side, {@code UNKNOWN} for a full-block interface. */
    public static net.minecraftforge.common.util.ForgeDirection getTrackedSide(final Object tracker) {
        if (tracker == null || fInvTracker_side == null) {
            return net.minecraftforge.common.util.ForgeDirection.UNKNOWN;
        }
        final net.minecraftforge.common.util.ForgeDirection side = readField(tracker, fInvTracker_side);
        return side == null ? net.minecraftforge.common.util.ForgeDirection.UNKNOWN : side;
    }

    /** Marks an interface-terminal row dirty, so its contents are pushed to the client. */
    public static void refreshTracked(Object tracker) {
        if (tracker == null || mInvTracker_updateNBT == null) {
            return;
        }
        try {
            mInvTracker_updateNBT.invoke(tracker);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to invoke method: " + mInvTracker_updateNBT, e);
        }
    }

    /**
     * Queues one provider slot's current contents in AE2's own interface-terminal update packet - the
     * exact entry its private {@code syncIfaceSlot} builds - and raises that container's dirty flag, so
     * the next {@code detectAndSendChanges} ships it and the client applies it through the same handler
     * it uses for AE2's own updates.
     *
     * <p>
     * {@code syncIfaceSlot} is private and only AE2's own slot actions call it, while {@code updateList}
     * compares a row's name, size, priority and online state but never the patterns in its slots. This
     * method is therefore the only way a content change that came from anywhere else - this terminal's
     * upload buttons, the interface's own GUI, an import bus - can reach a terminal that is already open.
     * A {@code null} stack sends the empty compound that stands for an emptied slot, exactly like AE2.
     */
    public static void pushTrackedSlot(ContainerInterfaceTerminal container, long id, int slot, ItemStack stack) {
        if (container == null || fContainerInterfaceTerminal_dirty == null) {
            return;
        }
        final PacketInterfaceTerminalUpdate dirty = readField(container, fContainerInterfaceTerminal_dirty);
        if (dirty == null) {
            return;
        }
        final NBTTagList items = new NBTTagList();
        final NBTTagCompound item = new NBTTagCompound();
        if (stack != null) {
            stack.writeToNBT(item);
        }
        items.appendTag(item);
        dirty.addOverwriteEntry(id)
            .setItems(new int[] { slot }, items);
        if (fContainerInterfaceTerminal_isDirty != null) {
            writeField(container, fContainerInterfaceTerminal_isDirty, Boolean.TRUE);
        }
    }

    public static void makeItem(SlotCraftingTerm obj, EntityPlayer player, ItemStack stack) {
        if (obj == null || mSlotCraftingTerm_makeItem == null) {
            return;
        }
        try {
            mSlotCraftingTerm_makeItem.invoke(obj, player, stack);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to invoke method: " + mSlotCraftingTerm_makeItem, e);
        }
    }

    public static void setCallback(CraftingJobV2 jobV2, ICraftingCallback callback) {
        if (jobV2 == null || fCraftingJobV2_callback == null) {
            return;
        }
        writeField(jobV2, fCraftingJobV2_callback, callback);
    }

    public static ICraftingJob getJob(ContainerCraftConfirm obj) {
        if (obj == null || fContainerCraftConfirm_result == null) {
            return null;
        }
        return readField(obj, fContainerCraftConfirm_result);
    }

    public static GridStorage getMyStorage(Grid grid) {
        return grid == null || fGrid_myStorage == null ? null : readField(grid, fGrid_myStorage);
    }

}
