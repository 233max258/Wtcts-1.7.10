package com.asdflj.wtct.util;

import static com.glodblock.github.util.Ae2Reflect.readField;
import static com.glodblock.github.util.Ae2Reflect.reflectField;
import static com.glodblock.github.util.Ae2Reflect.reflectMethod;
import static com.glodblock.github.util.Ae2Reflect.writeField;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import com.glodblock.github.client.gui.GuiFluidInterface;
import com.glodblock.github.client.gui.container.ContainerFluidInterface;
import com.glodblock.github.inventory.IDualHost;

import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.implementations.GuiCraftingStatus;
import appeng.client.gui.widgets.GuiTabButton;
import appeng.client.me.ItemRepo;
import codechicken.nei.SearchField;
import codechicken.nei.util.TextHistory;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The client-side AE2 (and NEI) internals this mod reads, looked up by name.
 *
 * <p>
 * Same contract as {@link Ae2Reflect}: every lookup is optional and the class always initialises. A
 * field that a given AE2/NEI build does not have simply leaves its read-only feature off instead of
 * throwing out of the static block - an initialiser that has thrown poisons the class for the whole
 * session (every later use dies with {@code NoClassDefFoundError}), which is the failure mode this
 * avoids. Accessors answer {@code null} rather than dereferencing a missing field.
 */
@SideOnly(Side.CLIENT)
public class Ae2ReflectClient {

    private static final Field fGuiCraftingStatus_icon;
    private static final Field fGuiCraftingStatus_originalGuiBtn;
    private static final Field fGui_drag;
    private static final Field fSearchField_history;
    private static final Field fTextHistory_history;
    private static final Field fItemRepo_view;
    private static final Field fItemRepo_list;
    private static final Field fGuiFluidInterface_cont;
    private static final Method mGui_inventorySlots;
    private static final Field fAEBaseGui_virtualSlots;

    /** One line per member that could not be resolved - logged once, never thrown. */
    private static final Set<String> MISSING = new java.util.HashSet<>();

    static {
        // Class names as strings: a class that is absent here (a NEI without TextHistory, an AE2
        // without virtualSlots) then costs its own lookup only, instead of failing the whole block.
        fGuiCraftingStatus_icon = field("appeng.client.gui.implementations.GuiCraftingStatus", "myIcon");
        fGuiCraftingStatus_originalGuiBtn = field(
            "appeng.client.gui.implementations.GuiCraftingStatus",
            "originalGuiBtn");
        fGui_drag = firstField("appeng.client.gui.AEBaseGui", "draggedSlots", "drag_click");
        mGui_inventorySlots = method("appeng.client.gui.AEBaseGui", "getInventorySlots");
        fItemRepo_view = field("appeng.client.me.ItemRepo", "view");
        fItemRepo_list = field("appeng.client.me.ItemRepo", "list");
        fGuiFluidInterface_cont = field("com.glodblock.github.client.gui.GuiFluidInterface", "cont");
        fSearchField_history = field("codechicken.nei.SearchField", "history");
        fTextHistory_history = field("codechicken.nei.util.TextHistory", "history");
        fAEBaseGui_virtualSlots = field("appeng.client.gui.AEBaseGui", "virtualSlots");
    }

    private static Field field(final String className, final String name) {
        try {
            return reflectField(Class.forName(className), name);
        } catch (final Throwable t) {
            note(className, name, t);
            return null;
        }
    }

    private static Field firstField(final String className, final String... names) {
        for (final String name : names) {
            final Field found = field(className, name);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

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

    /** Empty list when unavailable: callers walk this every frame and never expect null. */
    @SuppressWarnings("unchecked")
    public static List<Slot> getInventorySlots(AEBaseGui gui) {
        if (gui == null || mGui_inventorySlots == null) {
            return new ArrayList<>();
        }
        try {
            final List<Slot> slots = (List<Slot>) mGui_inventorySlots.invoke(gui);
            return slots == null ? new ArrayList<Slot>() : slots;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to invoke method: " + mGui_inventorySlots, e);
        }
    }

    /**
     * Clears AE2's own virtual-slot list. AE2 renders ME stacks from that list, while
     * {@code BaseMEGui.getMeSlots()} merely mirrors it, so a GUI that rebuilds its ME grid with a
     * different column count must drop the previous one - otherwise both grids are drawn on top of
     * each other (identical items look doubled with a 1px offset).
     */
    public static void clearVirtualSlots(AEBaseGui gui) {
        if (gui == null || fAEBaseGui_virtualSlots == null) {
            return;
        }
        try {
            ((List<?>) fAEBaseGui_virtualSlots.get(gui)).clear();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to clear AE2 virtual slots", e);
        }
    }

    public static void rewriteIcon(GuiCraftingStatus gui, ItemStack icon) {
        if (gui != null && fGuiCraftingStatus_icon != null) {
            writeField(gui, fGuiCraftingStatus_icon, icon);
        }
    }

    public static GuiTabButton getOriginalGuiButton(GuiCraftingStatus gui) {
        return gui == null || fGuiCraftingStatus_originalGuiBtn == null ? null
            : readField(gui, fGuiCraftingStatus_originalGuiBtn);
    }

    /**
     * The drag-click set. Empty and MUTABLE when unavailable: {@code PatternPanel} puts slots into
     * the set it is handed ({@code drag_click.add(slot)}), so handing back an immutable one would
     * turn a missing reflection entry into a crash on the next drag.
     */
    public static Set<Slot> getDragClick(AEBaseGui gui) {
        if (gui == null || fGui_drag == null) {
            return new java.util.HashSet<>();
        }
        final Set<Slot> drag = readField(gui, fGui_drag);
        return drag == null ? new java.util.HashSet<>() : drag;
    }

    public static TextHistory getHistory(SearchField searchField) {
        return searchField == null || fSearchField_history == null ? null
            : readField(searchField, fSearchField_history);
    }

    /**
     * The search box's history. Empty and MUTABLE when unavailable: the terminal's key handling
     * calls {@code removeIf} on the list it gets back.
     */
    public static List<String> getHistoryList(TextHistory textHistory) {
        if (textHistory == null || fTextHistory_history == null) {
            return new ArrayList<>();
        }
        final List<String> history = readField(textHistory, fTextHistory_history);
        return history == null ? new ArrayList<String>() : history;
    }

    /**
     * AE2's own view list. Never null: {@code AdvItemRepo} assigns this straight into a final field
     * while it is being constructed, so a null here would take the whole ME item grid down.
     */
    public static ArrayList<IAEStack<?>> getView(ItemRepo repo) {
        if (repo == null || fItemRepo_view == null) {
            return new ArrayList<>();
        }
        final ArrayList<IAEStack<?>> view = readField(repo, fItemRepo_view);
        return view == null ? new ArrayList<>() : view;
    }

    public static IItemList<IAEStack<?>> getList(ItemRepo repo) {
        return repo == null || fItemRepo_list == null ? null : readField(repo, fItemRepo_list);
    }

    public static IDualHost getHost(GuiFluidInterface gui) {
        if (gui == null || fGuiFluidInterface_cont == null) {
            return null;
        }
        ContainerFluidInterface container = readField(gui, fGuiFluidInterface_cont);
        return container == null ? null : container.getTile();
    }

}
