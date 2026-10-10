package com.asdflj.wtct.inventory.item;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

import com.asdflj.wtct.api.Constants;
import com.asdflj.wtct.api.WirelessObject;
import com.asdflj.wtct.client.gui.container.ContainerComprehensiveWorkTerminal;
import com.asdflj.wtct.common.item.ItemComprehensiveWorkTerminal;
import com.asdflj.wtct.inventory.IPatternTerminal;
import com.asdflj.wtct.inventory.ItemBiggerAppEngInventory;
import com.asdflj.wtct.inventory.ItemPatternsInventory;
import com.asdflj.wtct.inventory.WcwtUpgradesInventory;
import com.asdflj.wtct.util.TypeFilterUtil;
import com.asdflj.wtct.util.Util;
import com.glodblock.github.common.item.ItemFluidDrop;
import com.glodblock.github.common.item.ItemFluidPacket;

import appeng.api.config.Settings;
import appeng.api.config.SortDir;
import appeng.api.config.SortOrder;
import appeng.api.config.ViewItems;
import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.parts.IInterfaceTerminal;
import appeng.api.storage.ITerminalTypeFilterProvider;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStackType;
import appeng.api.util.AECableType;
import appeng.api.util.IConfigManager;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.tile.inventory.IAEAppEngInventory;
import appeng.tile.inventory.InvOperation;
import appeng.util.AEStackTypeFilter;
import appeng.util.ConfigManager;
import appeng.util.Platform;
import it.unimi.dsi.fastutil.objects.Reference2BooleanMap;

public class WirelessDualInterfaceTerminalInventory extends WirelessTerminal implements IGridHost, IPatternTerminal,
    IClickableInTerminal, IAEAppEngInventory, IInterfaceTerminal, ITerminalTypeFilterProvider {

    protected AppEngInternalInventory craftingEx;
    protected AppEngInternalInventory outputEx;
    protected AppEngInternalInventory pattern;
    /**
     * The terminal's upgrade column: the card slots plus the quantum singularity slot of the
     * right-hand upgrade panel. Built by {@link WcwtUpgradesInventory}, which also owns the
     * per-slot validity rules; the cards sit first so the index the terminal has always used for
     * its pattern refill card stays index 0.
     */
    protected WcwtUpgradesInventory upgrades;
    protected AppEngInternalInventory crafting;
    protected AppEngInternalInventory craftGrid;
    protected AppEngInternalInventory patternCache;
    protected boolean craftingMode = false;
    protected boolean substitute = false;
    protected boolean combine = false;
    protected boolean prioritize = false;
    protected boolean inverted = false;
    protected boolean beSubstitute = false;
    /** WCWT's "merge materials" toggle, persisted with the terminal like the other options. */
    protected boolean mergeMaterials = false;
    /**
     * The pattern management area's four switches. WCWT keeps them on the terminal item as well, with
     * these exact defaults (upload on, display mode 1 = VISIBLE, slots shown, search mode 2), so a
     * terminal reopens the way it was left instead of resetting every time.
     */
    protected boolean patternManagementUpload = true;
    protected int patternManagementDisplayMode = 1;
    protected boolean patternManagementShowSlots = true;
    protected int patternManagementSearchMode = 2;
    /** The management area's fifth switch, our own addition: pull the list up over the cache. */
    protected boolean patternManagementExpanded = false;
    protected int activePage = 0;
    private Util.DimensionalCoordSide tile;
    private final AEStackTypeFilter typeFilters = new AEStackTypeFilter();
    /** Mutable view handed out by {@link #getTypeFilter} and written back into {@code typeFilters} on save. */
    private Reference2BooleanMap<IAEStackType<?>> typeFilterView;

    public WirelessDualInterfaceTerminalInventory(WirelessObject obj) {
        super(obj);
        pattern = new ItemPatternsInventory(obj.getItemStack(), this, obj.getPlayer(), obj.getSlot());
        crafting = new ItemBiggerAppEngInventory(
            obj.getItemStack(),
            Constants.CRAFTING,
            9,
            obj.getPlayer(),
            obj.getSlot(),
            this);
        patternCache = new ItemBiggerAppEngInventory(
            obj.getItemStack(),
            Constants.PATTERN_CACHE,
            36,
            obj.getPlayer(),
            obj.getSlot(),
            this);
        craftGrid = new ItemBiggerAppEngInventory(
            obj.getItemStack(),
            Constants.CRAFT_GRID,
            9,
            obj.getPlayer(),
            obj.getSlot(),
            this);
        craftingEx = new ItemBiggerAppEngInventory(
            obj.getItemStack(),
            Constants.CRAFTING_EX,
            ContainerComprehensiveWorkTerminal.ENC_INPUT_SLOTS,
            obj.getPlayer(),
            obj.getSlot());
        outputEx = new ItemBiggerAppEngInventory(
            obj.getItemStack(),
            Constants.OUTPUT_EX,
            32,
            obj.getPlayer(),
            obj.getSlot());
        upgrades = new WcwtUpgradesInventory(obj.getItemStack(), obj.getPlayer(), obj.getSlot());
        this.readFromNBT();
    }

    private void readFromNBT() {
        NBTTagCompound data = Platform.openNbtData(this.obj.getItemStack());
        this.setSubstitution(data.getBoolean("substitute"));
        this.setCombineMode(data.getBoolean("combine"));
        this.setBeSubstitute(data.getBoolean("beSubstitute"));
        this.setMergeMaterials(data.getBoolean("mergeMaterials"));
        // Absent keys keep the defaults above - a terminal made before the management area existed must
        // not come back with its switches all off.
        if (data.hasKey("patternManagementUpload")) {
            this.setPatternManagementUpload(data.getBoolean("patternManagementUpload"));
        }
        if (data.hasKey("patternManagementDisplayMode")) {
            this.setPatternManagementDisplayMode(data.getInteger("patternManagementDisplayMode"));
        }
        if (data.hasKey("patternManagementShowSlots")) {
            this.setPatternManagementShowSlots(data.getBoolean("patternManagementShowSlots"));
        }
        if (data.hasKey("patternManagementSearchMode")) {
            this.setPatternManagementSearchMode(data.getInteger("patternManagementSearchMode"));
        }
        if (data.hasKey("patternManagementExpanded")) {
            this.setPatternManagementExpanded(data.getBoolean("patternManagementExpanded"));
            // Diagnostic: shows what the terminal item actually carried when the screen was built.
            // Remove once the reopen behaviour is settled.
            cpw.mods.fml.common.FMLLog
                .info("[wtct] terminal item read back management-expand = %s", this.patternManagementExpanded);
        } else {
            cpw.mods.fml.common.FMLLog.info("[wtct] terminal item carried no management-expand key");
        }
        this.setPrioritization(data.getBoolean("priorization"));
        this.setInverted(data.getBoolean("inverted"));
        this.setActivePage(data.getInteger("activePage"));
        this.setCraftingRecipe(data.getBoolean("craftingMode"));
        if (data.hasKey("clickedInterface")) {
            NBTTagCompound tileMsg = (NBTTagCompound) data.getTag("clickedInterface");
            this.tile = Util.DimensionalCoordSide.readFromNBT(tileMsg);
        }
        this.typeFilters.readFromNBT(data);
    }

    @Override
    public IConfigManager getConfigManager() {
        final ConfigManager out = new ConfigManager((manager, settingName, newValue) -> {
            final NBTTagCompound data = Platform.openNbtData(this.getItemStack());
            manager.writeToNBT(data);
            saveSettings();
        });
        out.registerSetting(Settings.SORT_BY, SortOrder.NAME);
        out.registerSetting(Settings.VIEW_MODE, ViewItems.ALL);
        out.registerSetting(Settings.SORT_DIRECTION, SortDir.ASCENDING);
        out.readFromNBT(
            (NBTTagCompound) Platform.openNbtData(this.getItemStack())
                .copy());
        return out;
    }

    @Override
    public IGridNode getActionableNode() {
        return this.obj.getGridNode();
    }

    @Override
    public IGridNode getGridNode(ForgeDirection dir) {
        return this.obj.getGridNode();
    }

    @Override
    public AECableType getCableConnectionType(ForgeDirection dir) {
        return null;
    }

    @Override
    public void securityBreak() {

    }

    @Override
    public IInventory getInventoryByName(String name) {
        return switch (name) {
            case Constants.CRAFTING_EX -> this.craftingEx;
            case Constants.OUTPUT_EX -> this.outputEx;
            case Constants.CRAFTING -> this.crafting;
            case Constants.CRAFT_GRID -> this.craftGrid;
            case Constants.PATTERN_CACHE -> this.patternCache;
            case Constants.PATTERN -> this.pattern;
            case Constants.UPGRADES -> this.upgrades;
            default -> null;
        };

    }

    @Override
    public void setActivePage(int value) {
        this.activePage = value;
    }

    @Override
    public int getActivePage() {
        return this.activePage;
    }

    @Override
    public boolean shouldCombine() {
        return this.combine;
    }

    @Override
    public void setCombineMode(boolean shouldCombine) {
        this.combine = shouldCombine;
    }

    @Override
    public void setPrioritization(boolean canPrioritize) {
        this.prioritize = canPrioritize;
    }

    @Override
    public void setInverted(boolean inverted) {
        this.inverted = inverted;
    }

    @Override
    public void setCraftingRecipe(boolean craftingMode) {
        this.craftingMode = craftingMode;
        this.fixCraftingRecipes();
    }

    @Override
    public void setSubstitution(boolean canSubstitute) {
        this.substitute = canSubstitute;
    }

    @Override
    public void setBeSubstitute(boolean canBeSubstitute) {
        this.beSubstitute = canBeSubstitute;
    }

    @Override
    public boolean isMergeMaterials() {
        return this.mergeMaterials;
    }

    @Override
    public void setMergeMaterials(boolean mergeMaterials) {
        this.mergeMaterials = mergeMaterials;
    }

    @Override
    public boolean isPatternManagementUpload() {
        return this.patternManagementUpload;
    }

    @Override
    public void setPatternManagementUpload(final boolean upload) {
        this.patternManagementUpload = upload;
    }

    @Override
    public int getPatternManagementDisplayMode() {
        return this.patternManagementDisplayMode;
    }

    @Override
    public void setPatternManagementDisplayMode(final int mode) {
        this.patternManagementDisplayMode = mode;
    }

    @Override
    public boolean isPatternManagementShowSlots() {
        return this.patternManagementShowSlots;
    }

    @Override
    public void setPatternManagementShowSlots(final boolean showSlots) {
        this.patternManagementShowSlots = showSlots;
    }

    @Override
    public int getPatternManagementSearchMode() {
        return this.patternManagementSearchMode;
    }

    @Override
    public void setPatternManagementSearchMode(final int mode) {
        this.patternManagementSearchMode = mode;
    }

    @Override
    public boolean isPatternManagementExpanded() {
        return this.patternManagementExpanded;
    }

    @Override
    public void setPatternManagementExpanded(final boolean expanded) {
        this.patternManagementExpanded = expanded;
    }

    @Override
    public boolean isCraftingRecipe() {
        return this.craftingMode;
    }

    @Override
    public boolean isInverted() {
        return this.inverted;
    }

    @Override
    public boolean canBeSubstitute() {
        return this.beSubstitute;
    }

    @Override
    public boolean isPrioritize() {
        return this.prioritize;
    }

    @Override
    public boolean isSubstitution() {
        return this.substitute;
    }

    @Override
    public void sortCraftingItems() {
        List<ItemStack> items = new ArrayList<>();
        List<ItemStack> fluids = new ArrayList<>();
        for (ItemStack is : this.craftingEx) {
            if (is == null) continue;
            if (is.getItem() instanceof ItemFluidPacket) {
                fluids.add(is);
            } else {
                items.add(is);
            }
        }
        if (this.prioritize) {
            fluids.addAll(items);
            items.clear();
        } else {
            items.addAll(fluids);
            fluids.clear();
        }

        for (int i = 0; i < this.craftingEx.getSizeInventory(); i++) {
            if (this.craftingEx.getStackInSlot(i) == null) break;
            if (items.isEmpty()) {
                this.craftingEx.setInventorySlotContents(i, fluids.get(i));
            } else {
                this.craftingEx.setInventorySlotContents(i, items.get(i));
            }
        }
    }

    @Override
    public void saveSettings() {
        writeToNBT();
    }

    @Override
    public boolean hasRefillerUpgrade() {
        return this.upgrades.hasPatternRefillCard();
    }

    @Override
    public void saveChanges() {

    }

    /**
     * True when this inventory hosts the comprehensive work terminal instead of the wireless dual-interface
     * one. The two terminals share this host class, and every sub-screen they open needs to know which
     * terminal to return to, so the answer has to come from the item the inventory was built for.
     */
    public boolean isComprehensive() {
        final ItemStack is = this.getItemStack();
        return is != null && is.getItem() instanceof ItemComprehensiveWorkTerminal;
    }

    @Override
    public void onChangeInventory(IInventory inv, int slot, InvOperation mc, ItemStack removedStack,
                                  ItemStack newStack) {
        if (inv == this.pattern && slot == 1) {
            final ItemStack is = inv.getStackInSlot(1);

            if (is != null && is.getItem() instanceof final ICraftingPatternItem craftingPatternItem) {
                if (appeng.util.Platform.isClient()) {
                    // The client only needs the four GUI flags. AE2's getPatternForItem builds the whole
                    // CraftingPattern (an uncached, roughly quadratic fuzzy pre-computation over every
                    // ingredient) and this very method fires from the container's own slot sync the
                    // moment the terminal opens - on the client's main thread, which is the ~0.5s the
                    // terminal spent frozen whenever an encoded pattern sat in the encoding area. The
                    // cells themselves reach the client through that same sync, so the flags are read
                    // straight from the pattern's own NBT instead (the same values AE2 encoded there).
                    final NBTTagCompound tag = is.getTagCompound();
                    if (tag != null) {
                        this.setCraftingRecipe(tag.getBoolean("crafting"));
                        this.setSubstitution(tag.getBoolean("substitute"));
                        this.setBeSubstitute(tag.getBoolean("beSubstitute"));
                        int inputsCount = 0;
                        int outputCount = 0;
                        final List<List<ItemStack>> cells = com.asdflj.wtct.util.PatternScaling
                            .readPatternCells(is);
                        if (cells != null) {
                            for (final ItemStack cell : cells.get(0)) {
                                if (cell != null) {
                                    inputsCount++;
                                }
                            }
                            for (final ItemStack cell : cells.get(1)) {
                                if (cell != null) {
                                    outputCount++;
                                }
                            }
                        }
                        this.setInverted(inputsCount <= 8 && outputCount > 8);
                        this.setActivePage(0);
                    }
                    return;
                }
                // TEMP DIAGNOSTIC (1.0.38, remove once the terminal-open delay is pinned down).
                final long diagT0 = System.currentTimeMillis();
                final ICraftingPatternDetails details = craftingPatternItem
                    .getPatternForItem(is, this.getActionableNode().getWorld());
                cpw.mods.fml.common.FMLLog.info(
                    "[wtct-diag] server pattern decode took %dms t=%d thread=%s",
                    System.currentTimeMillis() - diagT0, diagT0, Thread.currentThread()
                        .getName());

                if (details != null) {
                    final IAEItemStack[] inItems = details.getInputs();
                    final IAEItemStack[] outItems = details.getOutputs();
                    this.setCraftingRecipe(details.isCraftable());
                    int inputsCount = 0;
                    int outputCount = 0;
                    for (IAEItemStack inItem : inItems) {
                        if (inItem != null) {
                            inputsCount++;
                        }
                    }
                    for (IAEItemStack outItem : outItems) {
                        if (outItem != null) {
                            outputCount++;
                        }
                    }

                    this.setSubstitution(details.canSubstitute());
                    if (newStack != null) {
                        this.setBeSubstitute(details.canBeSubstitute());
                    }
                    this.setInverted(inputsCount <= 8 && outputCount > 8);
                    this.setActivePage(0);

                    for (int i = 0; i < this.craftingEx.getSizeInventory(); i++) {
                        this.craftingEx.setInventorySlotContents(i, null);
                    }
                    for (int i = 0; i < this.crafting.getSizeInventory(); i++) {
                        this.crafting.setInventorySlotContents(i, null);
                    }

                    for (int i = 0; i < this.outputEx.getSizeInventory(); i++) {
                        this.outputEx.setInventorySlotContents(i, null);
                    }

                    for (int i = 0; i < getCraftingInternalInventory().getSizeInventory() && i < inItems.length; i++) {
                        final IAEItemStack item = inItems[i];
                        if (item != null) {
                            if (item.getItem() instanceof ItemFluidDrop && !this.isCraftingRecipe()) {
                                ItemStack packet = ItemFluidPacket
                                    .newStack(ItemFluidDrop.getFluidStack(item.getItemStack()));
                                getCraftingInternalInventory().setInventorySlotContents(i, packet);
                            } else getCraftingInternalInventory().setInventorySlotContents(i, item.getItemStack());
                        }
                    }

                    if (inverted) {
                        for (int i = 0; i < this.outputEx.getSizeInventory() && i < outItems.length; i++) {
                            final IAEItemStack item = outItems[i];
                            if (item != null) {
                                if (item.getItem() instanceof ItemFluidDrop) {
                                    ItemStack packet = ItemFluidPacket
                                        .newStack(ItemFluidDrop.getFluidStack(item.getItemStack()));
                                    this.outputEx.setInventorySlotContents(i, packet);
                                } else this.outputEx.setInventorySlotContents(i, item.getItemStack());
                            }
                        }
                    } else {
                        for (int i = 0; i < outItems.length && i < 8; i++) {
                            final IAEItemStack item = outItems[i];
                            if (item != null) {
                                if (item.getItem() instanceof ItemFluidDrop) {
                                    ItemStack packet = ItemFluidPacket
                                        .newStack(ItemFluidDrop.getFluidStack(item.getItemStack()));
                                    this.outputEx.setInventorySlotContents(i >= 4 ? 12 + i : i, packet);
                                } else this.outputEx.setInventorySlotContents(i >= 4 ? 12 + i : i, item.getItemStack());
                            }
                        }
                    }
                }
            }
        }
        if (inv == this.crafting) {
            this.fixCraftingRecipes();
        }
    }

    private AppEngInternalInventory getCraftingInternalInventory() {
        return this.isCraftingRecipe() ? this.crafting : this.craftingEx;
    }

    private void fixCraftingRecipes() {
        if (this.craftingMode) {
            for (int x = 0; x < this.crafting.getSizeInventory(); x++) {
                final ItemStack is = this.crafting.getStackInSlot(x);
                if (is != null) {
                    is.stackSize = 1;
                }
            }
        }
    }

    private void writeToNBT() {
        NBTTagCompound data = Platform.openNbtData(this.getItemStack());
        data.setBoolean("craftingMode", this.craftingMode);
        data.setBoolean("substitute", this.substitute);
        data.setBoolean("combine", this.combine);
        data.setBoolean("beSubstitute", this.beSubstitute);
        data.setBoolean("mergeMaterials", this.mergeMaterials);
        data.setBoolean("patternManagementUpload", this.patternManagementUpload);
        data.setInteger("patternManagementDisplayMode", this.patternManagementDisplayMode);
        data.setBoolean("patternManagementShowSlots", this.patternManagementShowSlots);
        data.setInteger("patternManagementSearchMode", this.patternManagementSearchMode);
        data.setBoolean("patternManagementExpanded", this.patternManagementExpanded);
        data.setBoolean("priorization", this.prioritize);
        data.setBoolean("inverted", this.inverted);
        data.setInteger("activePage", this.activePage);
        this.craftingEx.markDirty();
        this.outputEx.markDirty();
        this.upgrades.markDirty();
        this.pattern.markDirty();
        this.crafting.markDirty();
        this.craftGrid.markDirty();
        this.patternCache.markDirty();
        NBTTagCompound tileMsg = new NBTTagCompound();
        if (tile != null) {
            tile.writeToNBT(tileMsg);
        }
        data.setTag("clickedInterface", tileMsg);
        this.typeFilters.writeToNBT(data);
    }

    @Override
    public void setClickedInterface(Util.DimensionalCoordSide tile) {
        this.tile = tile;
        this.writeToNBT();
    }

    @Override
    public Util.DimensionalCoordSide getClickedInterface() {
        return this.tile;
    }

    @Override
    public boolean needsUpdate() {
        return true;
    }

    @Override
    public Reference2BooleanMap<IAEStackType<?>> getTypeFilter(EntityPlayer player) {
        this.typeFilterView = TypeFilterUtil.mutableView(this.typeFilters);
        return this.typeFilterView;
    }

    @Override
    public void saveTypeFilter() {
        TypeFilterUtil.flush(this.typeFilters, this.typeFilterView);
        this.typeFilterView = null;
        this.writeToNBT();
    }
}
