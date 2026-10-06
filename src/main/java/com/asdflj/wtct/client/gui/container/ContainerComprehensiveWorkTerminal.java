package com.asdflj.wtct.client.gui.container;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import com.asdflj.wtct.Wtct;
import com.asdflj.wtct.api.Constants;
import com.asdflj.wtct.client.gui.container.slot.SlotTicCraftingTerm;
import com.asdflj.wtct.client.gui.container.widget.IWidgetPatternContainer;
import com.asdflj.wtct.common.item.card.CardTicker;
import com.asdflj.wtct.inventory.IPatternTerminal;
import com.asdflj.wtct.inventory.WcwtUpgradesInventory;
import com.asdflj.wtct.inventory.item.INetworkTerminal;
import com.asdflj.wtct.network.SPacketAutoFillPending;
import com.asdflj.wtct.network.SPacketCraftableKeys;
import com.asdflj.wtct.util.Ae2Reflect;
import com.asdflj.wtct.util.BaublesUtil;
import com.asdflj.wtct.util.NeCharUtil;
import com.asdflj.wtct.util.PHUtil;
import com.asdflj.wtct.util.PatternScaling;
import com.asdflj.wtct.util.ProviderSlotSync;
import com.glodblock.github.common.item.ItemFluidDrop;
import com.glodblock.github.common.item.ItemFluidPacket;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.definitions.IItemDefinition;
import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.crafting.ICraftingJob;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.parts.IInterfaceTerminal;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.ITerminalHost;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.container.ContainerNull;
import appeng.container.guisync.GuiSync;
import appeng.container.implementations.ContainerInterfaceTerminal;
import appeng.container.slot.AppEngSlot;
import appeng.container.slot.IOptionalSlotHost;
import appeng.container.slot.SlotCraftingMatrix;
import appeng.container.slot.SlotFake;
import appeng.container.slot.SlotPatternTerm;
import appeng.container.slot.SlotRestrictedInput;
import appeng.helpers.IInterfaceHost;
import appeng.helpers.InventoryAction;
import appeng.me.helpers.ChannelPowerSrc;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.tile.inventory.InvOperation;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;

/**
 * Container of the ME Comprehensive Work Terminal (WCWT-style fixed layout, 354px wide).
 * Slot coordinates mirror the WCWT screen JSON (see comprehensive-terminal-design.md).
 */
public class ContainerComprehensiveWorkTerminal extends BasePatternContainerMonitor
    implements IWidgetPatternContainer, IOptionalSlotHost {

    /**
     * Encoding inputs, living on the terminal's CRAFTING_EX inventory. Three columns of twenty-four
     * rows: the panel shows three rows and pages through the rest with the encoding scrollbar
     * (crafting mode uses the first three rows only, as its matrix).
     */
    public static final int ENC_INPUT_SLOTS = 72;
    /**
     * Encoding outputs: one column, as many cells as the inputs have rows. WCWT drives both columns
     * from a single scroll offset and sizes its scrollbar from the inputs
     * ({@code maxScroll = inputs / 3 - 3}), so a column of {@code inputs / 3} outputs is exactly what
     * the scroll can reach - three outputs on screen, the rest paged by the same wheel/scrollbar.
     */
    public static final int ENC_OUTPUT_SLOTS = ENC_INPUT_SLOTS / 3;
    /** Visible encoding rows (WCWT shows 3 of its 27). */
    public static final int ENC_VISIBLE_ROWS = 3;
    /** The encoder's 3x3 crafting matrix: the first nine input cells, like WCWT/AE2. */
    private static final int CRAFTING_MATRIX = 9;
    /** Craftable-key push bookkeeping: recompute every 2s, ship only when the set changed. */
    private int craftableKeyTick;
    private java.util.Set<Integer> pushedCraftableKeys;
    /** Pattern cache: 36 slots (4 rows of 9) on the terminal, of which the GUI shows two rows. */
    public static final int CACHE_SLOTS = 36;
    public static final int CACHE_ROWS = 4;
    public static final int CACHE_COLS = 9;
    public static final int CACHE_VISIBLE_ROWS = 2;
    /**
     * WCWT_PATTERN_CACHE origin. The JSON anchor is bottom 144, but WCWT's screen code puts the
     * first row at {@code imageHeight - 142 - 1} (= 145 in the 288px reference layout), i.e. one
     * pixel below the anchor and level with the caching scrollbar's top.
     */
    public static final int CACHE_X = 176;
    public static final int CACHE_Y = 145;

    // ---------------------------------------------------------------------------------------------
    // Upgrade panel (WCWT's `scrollingUpgrades` / WTLib's ScrollingUpgradesPanel). WCWT anchors it
    // at `right 2, top 0`, i.e. 2px inside the terminal's right frame and hanging out past it -
    // `right: 2` resolves to `imageWidth - 2`. Our terminal texture is 355 wide (the GUI's
    // GUI_WIDTH), so the origin is 353: the panel's own white line then continues the terminal's
    // white line column and only the two frame columns sit under the panel. WTLib lays the first
    // slot at PADDING + 1 from that origin (SLOT_SIZE 18, PADDING 5).
    // ---------------------------------------------------------------------------------------------
    public static final int UPGRADE_PANEL_X = 353;
    public static final int UPGRADE_PANEL_Y = 0;
    /** First slot's item cell: the panel origin plus WTLib's {@code PADDING + 1} (5 + 1). */
    public static final int UPGRADE_SLOT_X = UPGRADE_PANEL_X + 1;
    public static final int UPGRADE_SLOT_Y = UPGRADE_PANEL_Y + 6;
    /** Slot pitch, WTLib's SLOT_SIZE. */
    public static final int UPGRADE_SLOT_PITCH = 18;
    /** The upgrade panel's slots, WCWT's panel order: singularity first, then the cards. */
    private Slot upgradeSingularitySlot;
    private final Slot[] upgradeCardSlots = new Slot[WcwtUpgradesInventory.CARD_SLOTS];

    private final SlotFake[] encodingSlots = new SlotFake[ENC_INPUT_SLOTS];
    private final SlotRestrictedInput[] cacheSlots = new SlotRestrictedInput[CACHE_SLOTS];
    /** The terminal inventory backing the cache (persisted in the terminal's NBT). */
    private IInventory patternCacheInv;
    /** Processing outputs, 3 rows @ x=277 (WCWT PROCESSING_OUTPUTS). */
    private final SlotFake[] outputSlots = new SlotFake[ENC_OUTPUT_SLOTS];
    /** Crafting recipe preview next to the matrix (WCWT pattern_crafting_result_slot). */
    private final AppEngInternalInventory craftingResultInv = new AppEngInternalInventory(this, 1);
    private SlotPatternTerm craftingResultSlot;

    /**
     * Encoding options, mirrored to the client through AE2's field sync so the toggle icons in the
     * GUI follow whatever the terminal host holds. The host stays authoritative, so these are copied
     * over on every server-side tick.
     */
    @GuiSync(90)
    public boolean substitute = false;
    @GuiSync(89)
    public boolean beSubstitute = false;
    /**
     * The pattern management area's four switches. The terminal item holds them (see
     * {@code IPatternTerminal}), so they survive the GUI closing and the game restarting; these are the
     * client's copy, refreshed from the host every server tick. The defaults are WCWT's own.
     */
    @GuiSync(87)
    public boolean mgmtUpload = true;
    @GuiSync(86)
    public int mgmtDisplayMode = 1;
    @GuiSync(85)
    public boolean mgmtShowSlots = true;
    @GuiSync(84)
    public int mgmtSearchMode = 2;
    /**
     * The management area's enlargement switch (our own, WCWT has no such thing): while it is on the
     * list is pulled up over the pattern cache, which is hidden outright. Synced like the other four
     * so both sides see the same layout the moment it is flipped.
     */
    @GuiSync(83)
    public boolean mgmtExpanded = false;
    /**
     * The pattern last seen in the edit slot, so {@link #loadEditSlotPattern()} loads a new one
     * exactly once instead of every tick.
     */
    private ItemStack lastEditSlotPattern;
    /**
     * The editing area as the last load left it. A change that finds the area anything else is the
     * encoder's own bookkeeping over the player's work and is not opened over it.
     */
    private String lastLoadedArea;
    /**
     * The pattern whose contents the area holds. A load that finds the same pattern arriving while the
     * area was edited is that bookkeeping and is held back; a <em>different</em> pattern is the player
     * handing this area a new one, and it takes over - the two are only told apart here.
     */
    private ItemStack lastAreaPattern;
    /** A pattern the edit slot offered while the area was busy - opened once the area is emptied. */
    private ItemStack pendingEditSlotLoad;
    /**
     * Pattern providers of the network, for the management area. {@code null} when the host is not an
     * interface terminal, in which case the area stays empty.
     */
    private final ContainerInterfaceTerminal providerRegistry;
    /**
     * Pushes provider slot contents the client has not been told about, so the management list follows
     * the network live when a pattern is put in or taken out - by this terminal's upload buttons, by the
     * interface's own GUI, or by automation. See {@link ProviderSlotSync}.
     */
    private final ProviderSlotSync providerSlotSync;
    /** True when the last {@link #encode()} actually produced a pattern. */
    private boolean encodeSucceeded;

    public ContainerComprehensiveWorkTerminal(InventoryPlayer ip, ITerminalHost monitorable) {
        super(ip, monitorable);
        this.lockSlot();
        this.setupPatternSlots(ip);
        this.setupUpgradeSlots(ip);
        this.setupCraftingGrid();
        this.setupArmorSlots();
        this.bindPlayerInventory(ip, 14, 0);
        // The pattern management area lists the network's pattern providers through AE2's own
        // interface terminal, exactly like the wireless dual-interface terminal does: this terminal's
        // host is a WirelessDualInterfaceTerminalInventory, which implements IInterfaceTerminal.
        this.providerRegistry = monitorable instanceof IInterfaceTerminal anchor
            ? new ContainerInterfaceTerminal(ip, anchor)
            : null;
        this.providerSlotSync = new ProviderSlotSync(this.providerRegistry);
    }

    private void setupPatternSlots(InventoryPlayer ip) {
        final IInventory patternInv = this.it.getInventoryByName(Constants.PATTERN);
        final IInventory outputEx = this.it.getInventoryByName(Constants.OUTPUT_EX);
        // getY() stores the slot's Y in WCWT's reference layout, derived from the screen JSON
        // (textureY = 288 - bottom): BLANK_PATTERN bottom 223 -> 65, ENCODED_PATTERN 199 -> 89,
        // PROCESSING_INPUTS/OUTPUTS bottom 216 -> 72. The GUI maps them below the item rows.
        this.addSlotToContainer(
            this.patternSlotIN = new SlotRestrictedInput(
                SlotRestrictedInput.PlacableItemType.BLANK_PATTERN,
                patternInv,
                0,
                308,
                65,
                ip));
        this.patternSlotOUT = new SlotRestrictedInput(
            SlotRestrictedInput.PlacableItemType.ENCODED_PATTERN,
            patternInv,
            1,
            308,
            89,
            ip);
        this.patternSlotOUT.setStackLimit(1);
        this.addSlotToContainer(this.patternSlotOUT);
        // Encoding inputs. Their real coordinates are assigned per mode by the GUI (WCWT puts the
        // crafting matrix at x=183 and the processing inputs at x=192, y=72+row*18).
        final IInventory craftingEx = this.it.getInventoryByName(Constants.CRAFTING_EX);
        for (int i = 0; i < ENC_INPUT_SLOTS; i++) {
            this.addSlotToContainer(this.encodingSlots[i] = new SlotFake(craftingEx, i, 192, 72));
        }
        // Processing outputs @ x=277, y=72/90/108.
        for (int i = 0; i < this.outputSlots.length; i++) {
            this.addSlotToContainer(this.outputSlots[i] = new SlotFake(outputEx, i, 277, 72 + i * 18));
        }
        // Pattern cache (WCWT_PATTERN_CACHE: 9 columns starting at x=176, first row at CACHE_Y).
        // Only two rows are on screen at a time; the GUI scrolls the rest. These are *real* slots,
        // exactly like WCWT's RestrictedInputSlot(ENCODED_PATTERN): AE2's SlotFake is a ghost slot
        // (isItemValid/canTakeStack both false), which would make the cache read-only.
        // ENCODED_PATTERN also gets AE2's pattern preview - getDisplayStack() draws the encoded
        // pattern's output, which is what the cache is supposed to show.
        this.patternCacheInv = this.it.getInventoryByName(Constants.PATTERN_CACHE);
        if (this.patternCacheInv != null) {
            for (int i = 0; i < CACHE_SLOTS; i++) {
                this.addSlotToContainer(
                    this.cacheSlots[i] = new SlotRestrictedInput(
                        SlotRestrictedInput.PlacableItemType.ENCODED_PATTERN,
                        this.patternCacheInv,
                        i,
                        CACHE_X,
                        CACHE_Y,
                        ip));
                // An encoded pattern is not a stackable item: one pattern per cell, always. AE2's own
                // ItemEncodedPattern reports a stack size of 64, so without this a shift-clicked pile
                // of identical patterns would fold into a single cell as a count. AE2's slot routing
                // reads getSlotStackLimit(), which is what makes the limit effective.
                this.cacheSlots[i].setStackLimit(1);
            }
        }
        // Crafting recipe preview @ x=274, y=90 (WCWT pattern_crafting_result_slot) - it turns the
        // matrix into a visible result, which is also what the encoder reads.
        this.addSlotToContainer(
            this.craftingResultSlot = new SlotPatternTerm(
                this.getPlayerInv().player,
                this.getActionSource(),
                this.getPowerSource(),
                this.host,
                this.crafting,
                patternInv,
                this.craftingResultInv,
                274,
                90,
                this,
                0,
                this));
    }

    @Override
    public boolean isSlotEnabled(int idx) {
        return true;
    }

    /**
     * The right-hand upgrade panel's slots - WCWT's {@code scrollingUpgrades}: one quantum
     * singularity slot plus the terminal's card column.
     *
     * <p>
     * The panel shows only {@code max(2, terminal rows)} of them at a time and scrolls for the rest
     * (WTLib's own {@code setMaxRows}), so the client parks whatever is not in view off-screen and
     * this constructor's coordinates are no more than the first paint's starting point. Validity is
     * the inventory's business ({@link WcwtUpgradesInventory}); the cards therefore use plain
     * {@link AppEngSlot}s, whose {@code isItemValid} defers to
     * {@code IInventory#isItemValidForSlot}. The singularity gets AE2's own
     * {@code PlacableItemType.QE_SINGULARITY}, which both checks the item and shows the empty-slot
     * placeholder WCWT relies on.
     */
    private void setupUpgradeSlots(final InventoryPlayer ip) {
        final IInventory upgrades = this.it.getInventoryByName(Constants.UPGRADES);
        if (upgrades == null) {
            return;
        }
        for (int i = 0; i < this.upgradeCardSlots.length; i++) {
            // SlotWcwtCard, not a plain AppEngSlot: AppEngSlot.isItemValid falls through to vanilla's
            // "everything fits", which let any item be hand-placed into the card column.
            this.addSlotToContainer(
                this.upgradeCardSlots[i] = new com.asdflj.wtct.client.gui.container.slot.SlotWcwtCard(
                    upgrades,
                    i,
                    UPGRADE_SLOT_X,
                    UPGRADE_SLOT_Y + i * UPGRADE_SLOT_PITCH));
        }
        final SlotRestrictedInput singularity = new SlotRestrictedInput(
            SlotRestrictedInput.PlacableItemType.QE_SINGULARITY,
            upgrades,
            WcwtUpgradesInventory.SINGULARITY_SLOT,
            UPGRADE_SLOT_X,
            UPGRADE_SLOT_Y,
            ip);
        // AE2's icon for this slot is an index into its own 1.7.10 sheet; the screen paints 1.21's
        // sprite instead (SlotWcwtCard.drawHint), and leaving AE2's on would stack the two.
        singularity.setIIcon(-1);
        this.addSlotToContainer(this.upgradeSingularitySlot = singularity);
    }

    /** The card slots of the upgrade panel, in card order (index 0 is the first card slot). */
    public Slot[] getUpgradeCardSlots() {
        return this.upgradeCardSlots;
    }

    /** The upgrade panel's singularity slot, or null when the host has no upgrade inventory. */
    public Slot getUpgradeSingularitySlot() {
        return this.upgradeSingularitySlot;
    }

    /**
     * The terminal item itself - the card column's NBT lives on it, so anything that needs to know
     * which cards are installed (the panel's singularity rule, the card buttons) reads this.
     */
    public ItemStack getTerminalStack() {
        return this.it instanceof final appeng.api.implementations.guiobjects.IGuiItemObject gui ? gui.getItemStack()
            : null;
    }

    /**
     * Steps the magnet card's mode (off / to inventory / into the network). The mode lives on the
     * terminal item under the key the backpack terminal uses, so the two share their setting; the
     * card column is marked dirty afterwards so the item is written back to the player's slot.
     */
    public void cycleMagnetMode() {
        final ItemStack terminal = this.getTerminalStack();
        if (terminal == null) {
            return;
        }
        CardTicker.MagnetMode.set(
            terminal,
            CardTicker.MagnetMode.of(terminal)
                .next());
        this.saveTerminal();
    }

    /** Steps the block picker card's amount through its presets, the same way. */
    public void cycleBlockPickerAmount() {
        final ItemStack terminal = this.getTerminalStack();
        if (terminal == null) {
            return;
        }
        CardTicker.nextPickerAmount(terminal);
        this.saveTerminal();
    }

    /** The addon's amount entry for the block picker: writes the exact number it was given. */
    public void setBlockPickerAmount(final long amount) {
        final ItemStack terminal = this.getTerminalStack();
        if (terminal == null) {
            return;
        }
        CardTicker.setPickerAmount(terminal, amount);
        this.saveTerminal();
    }

    /** Writes the terminal item back into the player's slot it came from. */
    private void saveTerminal() {
        final IInventory upgrades = this.it.getInventoryByName(Constants.UPGRADES);
        if (upgrades != null) {
            upgrades.markDirty();
        }
    }

    /** Keeps the crafting preview in step with the matrix, like AE2's own pattern terminal. */
    private void updateCraftingPreview() {
        if (!this.craftingMode) {
            return;
        }
        final ItemStack result = this.findCraftingResult();
        final ItemStack current = this.craftingResultInv.getStackInSlot(0);
        if (result == null ? current != null : !ItemStack.areItemStacksEqual(result, current)) {
            this.craftingResultInv.setInventorySlotContents(0, result);
        }
    }

    private final SlotCraftingMatrix[] manualCraftSlots = new SlotCraftingMatrix[9];
    private SlotTicCraftingTerm manualCraftOutput;

    /** Manual 3x3 crafting grid @ x=79 rows 74/92/110, output @ x=149 row 92 (reference layout). */
    private void setupCraftingGrid() {
        final IInventory craftGrid = this.it.getInventoryByName(Constants.CRAFT_GRID);
        if (craftGrid == null) {
            return;
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                this.addSlotToContainer(
                    this.manualCraftSlots[col + row * 3] = new SlotCraftingMatrix(
                        this,
                        craftGrid,
                        col + row * 3,
                        79 + col * 18,
                        74 + row * 18));
            }
        }
        final AppEngInternalInventory output = new AppEngInternalInventory(this, 1);
        this.addSlotToContainer(
            this.manualCraftOutput = new SlotTicCraftingTerm(
                this.getPlayerInv().player,
                this.getActionSource(),
                this.getPowerSource(),
                this.host,
                craftGrid,
                craftGrid,
                output,
                149,
                92,
                this));
        this.onCraftMatrixChanged(craftGrid);
    }

    /** Recomputes the manual crafting output (AE2 SlotCraftingMatrix calls this on every change). */
    @Override
    public void onCraftMatrixChanged(final IInventory par1IInventory) {
        if (this.manualCraftSlots[0] == null) {
            return;
        }
        final appeng.container.ContainerNull cn = new appeng.container.ContainerNull();
        final net.minecraft.inventory.InventoryCrafting ic = new net.minecraft.inventory.InventoryCrafting(cn, 3, 3);
        for (int x = 0; x < 9; x++) {
            ic.setInventorySlotContents(x, this.manualCraftSlots[x].getStack());
        }
        this.manualCraftOutput.putStack(
            net.minecraft.item.crafting.CraftingManager.getInstance()
                .findMatchingRecipe(ic, this.getPlayerInv().player.worldObj));
    }

    /**
     * NEI's hammer for this terminal: pull the recipe's materials out of the network into the
     * manual crafting grid, and stay in the terminal - the grid lives here, so there is no other
     * screen to return to.
     *
     * <p>
     * With {@code auto} (Ctrl held, WCWT's Ctrl+hammer / ae2helpers' auto-craft watcher) the
     * container also asks the network to make what the pull could not fill; the terminal's
     * client-side watch fills the cells as the crafted stacks land in storage.
     */
    public void fillCraftGridFromRecipe(final EntityPlayer player, final ItemStack[][] recipe, final boolean auto) {
        final IInventory craftGrid = this.it == null ? null : this.it.getInventoryByName(Constants.CRAFT_GRID);
        if (craftGrid == null) {
            return;
        }
        // On Ctrl the pull also reports the cells it could not fill. The cells themselves are the
        // client's business from here on (the terminal's auto-fill watch drops the material in the
        // moment the network holds it - ae2helpers' watcher); the server only has to ask the network
        // to make the missing-but-craftable ones. "Craft if missing" off means Ctrl is no different
        // from a plain hammer: nothing is asked for and no cell is watched.
        final boolean watch = auto
            && ContainerWcwtSettings.isOn(this.getTerminalStack(), ContainerWcwtSettings.KEY_CRAFT_IF_MISSING);
        final Map<Integer, ItemStack[]> missing = new java.util.LinkedHashMap<>();
        final com.asdflj.wtct.network.NetworkRecipeFiller.AutoFillSink sink = watch ? missing::put : null;
        com.asdflj.wtct.network.NetworkRecipeFiller.fillFromRecipe(
            player,
            craftGrid,
            this.getPowerSource(),
            this.getActionSource(),
            this.getMonitor(),
            this.getViewCells(),
            this.useRealItems(),
            recipe,
            sink);
        // Recompute the manual output - a pure inventory write does not notify the container, and
        // without this the result slot stays empty ("the crafted item cannot be taken out").
        this.onCraftMatrixChanged(craftGrid);
        if (watch && !missing.isEmpty()) {
            this.planMissingCrafts(player, missing);
        }
        this.saveChanges();
        this.detectAndSendChanges();
    }

    /**
     * ae2helpers' {@code FillCraftingSlotPacket}'s server half: move a single one of the stack the
     * client's watch found in the network into the crafting cell it waits in.
     */
    public void fillCraftingSlot(final int slotIndex, final ItemStack wanted) {
        final IInventory craftGrid = this.it == null ? null : this.it.getInventoryByName(Constants.CRAFT_GRID);
        if (craftGrid == null || slotIndex < 0 || slotIndex >= craftGrid.getSizeInventory()) {
            return;
        }
        if (craftGrid.getStackInSlot(slotIndex) != null) {
            return; // filled in the meantime
        }
        final IAEItemStack request = appeng.util.item.AEItemStack.create(wanted);
        if (request == null) {
            return;
        }
        request.setStackSize(1);
        final IAEItemStack out = Platform
            .poweredExtraction(this.getPowerSource(), this.getMonitor(), request, this.getActionSource());
        if (out == null) {
            return; // the watch will simply try again once the network really holds it
        }
        craftGrid.setInventorySlotContents(slotIndex, out.getItemStack());
        this.onCraftMatrixChanged(craftGrid);
        this.saveChanges();
        this.detectAndSendChanges();
    }

    /**
     * GTNH AE's pull-up-crafting: the cells the pull could not fill are missing from storage but have
     * patterns in the network, so the network is asked to make them - every one of them, one plan
     * each. Not on the spot, though - the first plan goes on screen as AE2's own craft confirm and
     * nothing runs until the player approves it; that one approval starts the rest too.
     *
     * <p>
     * The cells that can be made are also handed to the client's auto-fill watch first (ae2helpers'
     * {@code setPending}): the terminal's screen owns the waiting and the ghost items, while this side
     * only moves the stack once the client sees it in storage.
     */
    private void planMissingCrafts(final EntityPlayer player, final Map<Integer, ItemStack[]> missingCells) {
        final IGridNode node = this.networkNode;
        final IGrid grid = node == null ? null : node.getGrid();
        final ICraftingGrid crafting = grid == null ? null : grid.getCache(ICraftingGrid.class);
        if (crafting == null) {
            return;
        }
        final net.minecraft.world.World world = this.getPlayerInv().player == null ? null
            : this.getPlayerInv().player.worldObj;
        // The network's own output stacks, counted by how many cells want each of them, and the cells
        // that have one - the ones worth watching.
        final Map<IAEItemStack, Integer> wanted = new java.util.LinkedHashMap<>();
        final Map<Integer, ItemStack[]> craftable = new java.util.LinkedHashMap<>();
        for (final Map.Entry<Integer, ItemStack[]> cell : missingCells.entrySet()) {
            final ItemStack[] variants = cell.getValue();
            final IAEItemStack canonical = canonicalOutput(crafting, variants);
            if (canonical == null) {
                continue; // nothing makes this - the hammer tints it red and never asks
            }
            craftable.put(cell.getKey(), variants);
            boolean merged = false;
            for (final Map.Entry<IAEItemStack, Integer> known : wanted.entrySet()) {
                if (known.getKey()
                    .isSameType(canonical)) {
                    known.setValue(known.getValue() + 1);
                    merged = true;
                    break;
                }
            }
            if (!merged) {
                wanted.put(canonical, 1);
            }
        }
        // Arm the terminal's watch: it keeps the cells and polls the network for the crafted stacks,
        // so they are dropped in as they arrive instead of the player filling them by hand.
        if (!craftable.isEmpty() && player instanceof EntityPlayerMP crafter) {
            Wtct.proxy.netHandler.sendTo(new SPacketAutoFillPending(craftable), crafter);
        }
        if (wanted.isEmpty()) {
            return;
        }
        // Every missing ingredient the network can make gets its own job - a plan only ever covers one
        // item, so "craft what is missing" means one per item. The first goes on screen as AE2's own
        // craft confirm; the rest ride along with that screen, so the player's single confirmation
        // starts them all instead of only the first. Nothing runs until that click.
        final List<PendingCraft> jobs = new ArrayList<>();
        for (final Map.Entry<IAEItemStack, Integer> entry : wanted.entrySet()) {
            final IAEItemStack what = entry.getKey()
                .copy();
            what.setStackSize(entry.getValue());
            final java.util.concurrent.Future<ICraftingJob> future = this.beginCraftJob(crafting, grid, world, what);
            if (future != null) {
                jobs.add(new PendingCraft(what, future));
            }
        }
        if (jobs.isEmpty()) {
            return;
        }
        if (!this.openCraftPlan(this.getPlayerInv().player, jobs.get(0), jobs.subList(1, jobs.size()))) {
            for (final PendingCraft job : jobs) {
                job.future()
                    .cancel(true);
            }
        }
    }

    /** Starts planning one craft; the plan itself is computed off-thread, so this only hands back the future. */
    private java.util.concurrent.Future<ICraftingJob> beginCraftJob(final ICraftingGrid crafting, final IGrid grid,
        final net.minecraft.world.World world, final IAEItemStack what) {
        try {
            if (crafting instanceof appeng.me.cache.CraftingGridCache cgc) {
                // GTNH AE's "Ignore Missing" - the mode its own crafting confirm offers to let a job
                // through when the network cannot supply every piece. A STANDARD request whose raw
                // material is absent comes back isSimulation(), and the confirm screen shows nothing.
                // AE2 rv3-beta-1073 added a "lite crafting" boolean between the mode and the callback
                // (CraftingGridCache's own PacketCraftRequest passes AEConfig's setting); omitting it
                // leaves the network's fast-path preference at its default of off.
                return cgc.beginCraftingJob(
                    world,
                    grid,
                    this.getActionSource(),
                    what,
                    appeng.api.config.CraftingMode.IGNORE_MISSING,
                    appeng.core.AEConfig.instance.getUseLiteCraftingMode(),
                    null);
            }
            return crafting.beginCraftingJob(world, grid, this.getActionSource(), what, null);
        } catch (final Throwable t) {
            appeng.core.AELog.debug(t);
            return null;
        }
    }

    /**
     * Shows what is about to be made: the terminal's own craft-confirm container - AE2's screen, with
     * {@code switchToOriginalGUI} taught to come back to this terminal - seeded with the first planned
     * item and its job, plus the items that follow it. The confirm hands those over the moment the
     * player approves it, and this container submits them one by one as their plans come back.
     */
    private boolean openCraftPlan(final EntityPlayer player, final PendingCraft first,
        final List<PendingCraft> followUps) {
        if (!(this.openTerminalScreen(
            player,
            com.asdflj.wtct.inventory.gui.GuiType.CRAFTING_CONFIRM_ITEM) instanceof ContainerCraftConfirm confirm)) {
            return false;
        }
        confirm.setItemToCraft(first.what());
        confirm.setJob(first.future());
        confirm.setFollowUps(followUps);
        confirm.detectAndSendChanges();
        return true;
    }

    /** One craft the Ctrl+hammer asked for: what to make, and the plan the network is computing. */
    public static final class PendingCraft {

        private final IAEItemStack what;
        private final java.util.concurrent.Future<ICraftingJob> future;
        /** When the player approved it - an unapproved plan is never submitted, see {@link #approveFollowUps}. */
        private long approvedAt;

        PendingCraft(final IAEItemStack what, final java.util.concurrent.Future<ICraftingJob> future) {
            this.what = what;
            this.future = future;
        }

        public IAEItemStack what() {
            return this.what;
        }

        public java.util.concurrent.Future<ICraftingJob> future() {
            return this.future;
        }
    }

    /** Plans still in flight after the player's confirmation; submitted by {@link #submitApprovedJobs}. */
    private static final Map<java.util.UUID, List<PendingCraft>> APPROVED = new java.util.HashMap<>();
    /** A plan that takes longer than this after the confirmation is dropped rather than started later. */
    private static final long APPROVED_TTL_MS = 30000;

    /**
     * The confirm screen's half of the Ctrl+hammer: the plans it could not start itself - a confirm
     * only ever runs one job - are handed to the terminal the player returns to, which submits them as
     * soon as they are ready.
     */
    static void approveFollowUps(final EntityPlayer player, final List<PendingCraft> jobs) {
        if (player == null || jobs == null || jobs.isEmpty()) {
            return;
        }
        final long now = System.currentTimeMillis();
        for (final PendingCraft job : jobs) {
            job.approvedAt = now;
        }
        APPROVED.computeIfAbsent(player.getUniqueID(), ignored -> new ArrayList<>())
            .addAll(jobs);
    }

    /** Submits the approved plans that have finished computing; the rest wait for the next tick. */
    private void submitApprovedJobs(final EntityPlayer player) {
        if (player == null) {
            return;
        }
        final List<PendingCraft> jobs = APPROVED.get(player.getUniqueID());
        if (jobs == null || jobs.isEmpty()) {
            return;
        }
        final IGridNode node = this.networkNode;
        final IGrid grid = node == null ? null : node.getGrid();
        final ICraftingGrid crafting = grid == null ? null : grid.getCache(ICraftingGrid.class);
        if (crafting == null) {
            return;
        }
        final long now = System.currentTimeMillis();
        for (final java.util.Iterator<PendingCraft> it = jobs.iterator(); it.hasNext();) {
            final PendingCraft job = it.next();
            if (now - job.approvedAt > APPROVED_TTL_MS) {
                it.remove();
                job.future()
                    .cancel(true);
                continue;
            }
            if (!job.future()
                .isDone()) {
                continue;
            }
            it.remove();
            try {
                final ICraftingJob plan = job.future()
                    .get();
                if (plan != null) {
                    crafting.submitJob(plan, null, null, true, this.getActionSource(), true);
                }
            } catch (final Throwable t) {
                appeng.core.AELog.debug(t);
            }
        }
        if (jobs.isEmpty()) {
            APPROVED.remove(player.getUniqueID());
        }
    }

    /** Opens one of the terminal's own AE2 screens; its host is the same wireless item either way. */
    private Object openTerminalScreen(final EntityPlayer player, final com.asdflj.wtct.inventory.gui.GuiType type) {
        if (player == null || !(this.it instanceof com.asdflj.wtct.inventory.item.WirelessTerminal wireless)) {
            return null;
        }
        final appeng.container.ContainerOpenContext context = this.getOpenContext();
        if (context == null || context.getSide() == null) {
            return null;
        }
        com.asdflj.wtct.inventory.InventoryHandler.openGui(
            player,
            player.worldObj,
            new com.asdflj.wtct.util.BlockPos(wireless.getInventorySlot(), 0, 0),
            context.getSide(),
            type);
        return player.openContainer;
    }

    /**
     * The crafting cache's own output stack for one of the recipe's accepted variants, or null when
     * nothing in the network makes it. The cache is keyed by exact stacks - NBT included - and the
     * job's root has to be that exact key: NEI's ingredient variant routinely differs (tag contents,
     * damage), which left the job a simulation and {@code submitJob} refusing it.
     */
    private static IAEItemStack canonicalOutput(final ICraftingGrid crafting, final ItemStack[] variants) {
        if (variants == null) {
            return null;
        }
        for (final ItemStack variant : variants) {
            if (variant == null) {
                continue;
            }
            final IAEItemStack fromItems = findPatternKey(
                crafting.getCraftingPatterns()
                    .keySet(),
                variant);
            if (fromItems != null) {
                return fromItems.copy();
            }
        }
        // The legacy map is a view over the same cache, but it is keyed by a converted stack - when it
        // comes up empty, the stack-keyed map the network itself serves storage from still has the
        // entry. Same "which key does this item belong to" question, two answers.
        for (final ItemStack variant : variants) {
            if (variant == null) {
                continue;
            }
            final IAEItemStack fromStacks = findPatternKey(
                crafting.getCraftingMultiPatterns()
                    .keySet(),
                variant);
            if (fromStacks != null) {
                return fromStacks.copy();
            }
        }
        return null;
    }

    /**
     * The cache key for the item a recipe variant stands for: the exact stack first, then the same
     * item and damage, then the item alone - NEI hands out wildcard-damage and ore-dictionary
     * variants whose probe carries a damage the stored key never has.
     */
    private static IAEItemStack findPatternKey(final Iterable<? extends appeng.api.storage.data.IAEStack<?>> keys,
        final ItemStack variant) {
        final IAEItemStack probe = appeng.util.item.AEItemStack.create(variant);
        if (probe == null) {
            return null;
        }
        IAEItemStack sameItem = null;
        for (final appeng.api.storage.data.IAEStack<?> key : keys) {
            if (!(key instanceof IAEItemStack ais) || ais.getItem() != variant.getItem()) {
                continue;
            }
            if (ais.isSameType(probe)) {
                return ais;
            }
            if (!variant.getItem()
                .getHasSubtypes() || ais.getItemDamage() == variant.getItemDamage()) {
                sameItem = ais;
            }
        }
        return sameItem;
    }

    /**
     * Computes the network's complete craftable-key set off the server's storage list - the list
     * the client repo is fed from is sliced by the current search, so the client can never assemble
     * this itself - and ships it when it changes. The first call (right after the terminal opens)
     * always ships.
     */
    private void pushCraftableKeys() {
        if (++this.craftableKeyTick % 40 != 0) {
            return;
        }
        final java.util.Set<Integer> keys = new java.util.HashSet<>();
        // The authoritative craftable set: the crafting grid cache's pattern map is keyed by exactly
        // the items the network can make. (The storage list's isCraftable entries proved incomplete:
        // GTNH's serving kept reporting keys=1 even with a full pattern network.)
        if (this.networkNode != null && this.networkNode.getGrid() != null) {
            final ICraftingGrid craftingGrid = this.networkNode.getGrid()
                .getCache(ICraftingGrid.class);
            if (craftingGrid != null && craftingGrid.getCraftingPatterns() != null) {
                for (final IAEItemStack ais : craftingGrid.getCraftingPatterns()
                    .keySet()) {
                    if (ais != null) {
                        keys.add(SPacketCraftableKeys.keyOf(ais.getItem(), ais.getItemDamage()));
                    }
                }
            }
        }
        final IMEMonitor<IAEItemStack> mon = this.getMonitor();
        if (mon != null) {
            for (final IAEItemStack ais : mon.getStorageList()) {
                if (ais != null && ais.isCraftable()) {
                    keys.add(SPacketCraftableKeys.keyOf(ais.getItem(), ais.getItemDamage()));
                }
            }
        }
        if (this.pushedCraftableKeys != null && this.pushedCraftableKeys.equals(keys)) {
            return;
        }
        this.pushedCraftableKeys = keys;
        final int[] array = new int[keys.size()];
        int i = 0;
        for (final int key : keys) {
            array[i++] = key;
        }
        final SPacketCraftableKeys packet = new SPacketCraftableKeys(array);
        for (final Object crafter : this.crafters) {
            if (crafter instanceof EntityPlayerMP) {
                Wtct.proxy.netHandler.sendTo(packet, (EntityPlayerMP) crafter);
            }
        }
    }

    /** Clears the manual crafting matrix: leftovers go to the ME network, then the player. */
    public void clearManualCraftingGrid() {
        final IInventory craftGrid = this.it.getInventoryByName(Constants.CRAFT_GRID);
        if (craftGrid == null) {
            return;
        }
        final appeng.util.inv.AdaptorPlayerHand hand = new appeng.util.inv.AdaptorPlayerHand(
            this.getPlayerInv().player);
        for (int i = 0; i < craftGrid.getSizeInventory(); i++) {
            final ItemStack is = craftGrid.getStackInSlot(i);
            if (is == null) {
                continue;
            }
            craftGrid.setInventorySlotContents(i, null);
            ItemStack remaining = is;
            if (this.host.getItemInventory() != null) {
                final IAEItemStack rest = this.host.getItemInventory()
                    .injectItems(
                        appeng.util.item.AEItemStack.create(remaining),
                        Actionable.MODULATE,
                        this.getActionSource());
                remaining = rest == null ? null : rest.getItemStack();
            }
            if (remaining != null) {
                final ItemStack overflow = hand.addItems(remaining);
                if (overflow != null) {
                    this.getPlayerInv().player.entityDropItem(overflow, 0);
                }
            }
        }
        this.onCraftMatrixChanged(craftGrid);
        this.saveChanges();
    }

    /**
     * WCWT clearToPlayerInventory: merges the grid's stacks into the player inventory (existing
     * stacks first, then empty slots, hotbar first); whatever does not fit stays in the grid.
     */
    public void stashManualCraftingGrid() {
        final IInventory craftGrid = this.it.getInventoryByName(Constants.CRAFT_GRID);
        if (craftGrid == null) {
            return;
        }
        for (int i = 0; i < craftGrid.getSizeInventory(); i++) {
            final ItemStack is = craftGrid.getStackInSlot(i);
            if (is == null) {
                continue;
            }
            // addItemStackToInventory mutates the stack: merged into existing stacks first, then
            // emptied slots, and shrinks stackSize by whatever was moved.
            this.getPlayerInv()
                .addItemStackToInventory(is);
            craftGrid.setInventorySlotContents(i, is.stackSize > 0 ? is : null);
        }
        this.onCraftMatrixChanged(craftGrid);
        this.saveChanges();
    }

    /**
     * WCWT's armor column (AE2WTLIB_HELMET..BOOTS): the player's real armor at x=8, texture Y
     * 65..119 (18px pitch; JSON bottoms 223/205/187/169). No offhand on 1.7.10.
     */
    private void setupArmorSlots() {
        final InventoryPlayer inv = this.getPlayerInv();
        for (int i = 0; i < 4; i++) {
            // InventoryPlayer's IInventory view keeps the armor in its last four indices:
            // 39=helm, 38=chest, 37=legs, 36=boots - same indices WCWT's menu uses.
            this.addSlotToContainer(new SlotPlayerArmor(inv, inv.getSizeInventory() - 1 - i, 8, 65 + i * 18, i));
        }
    }

    /** One of the player's real armor slots (WCWT PlayerArmorSlot, 1.7.10 flavour). */
    public final class SlotPlayerArmor extends AppEngSlot {

        private final int armorType;

        SlotPlayerArmor(final IInventory inv, final int index, final int x, final int y, final int armorType) {
            super(inv, index, x, y);
            this.armorType = armorType;
        }

        /** 0=helm, 1=chest, 2=legs, 3=boots - for the empty-slot placeholder icon. */
        public int armorTypeIndex() {
            return this.armorType;
        }

        @Override
        public boolean isItemValid(final ItemStack stack) {
            return stack != null && stack.getItem()
                .isValidArmor(stack, this.armorType, ContainerComprehensiveWorkTerminal.this.getPlayerInv().player);
        }

        @Override
        public int getSlotStackLimit() {
            return 1;
        }
    }

    /**
     * Fluids ride their own packet, so the fluid monitor has to be ticked alongside the item monitor -
     * exactly what the pattern and dual-interface containers do. Without this call the grid's fluid list
     * was never sent to the client and the terminal showed items only, however the type filter was set.
     */
    @Override
    public void processItemList() {
        super.processItemList();
        this.fluidMonitor.processItemList();
    }

    @Override
    void setMonitor() {
        if (this.host instanceof INetworkTerminal) {
            final IGridNode node = ((appeng.api.networking.IGridHost) this.host).getGridNode(ForgeDirection.UNKNOWN);
            if (node != null) {
                this.networkNode = node;
                final IGrid g = node.getGrid();
                if (g != null) {
                    this.setPowerSource(new ChannelPowerSrc(this.networkNode, g.getCache(IEnergyGrid.class)));
                    final IStorageGrid storageGrid = g.getCache(IStorageGrid.class);
                    this.monitor.setMonitor(storageGrid.getItemInventory());
                    // The live craftable lookup: the crafting grid cache's own pattern map is rebuilt on
                    // every pattern change, which is what lets a pulled-out pattern's item leave the
                    // terminal instead of lingering there with nothing to craft it. (getCraftingPatterns()
                    // is not usable for that - that map is only ever added to in this build.)
                    final ICraftingGrid craftingGrid = g.getCache(ICraftingGrid.class);
                    if (craftingGrid != null) {
                        this.monitor.setCraftableCheck(
                            stack -> !craftingGrid.getCraftingFor(stack, null, 0, null)
                                .isEmpty());
                    }
                    this.fluidMonitor.setMonitor(storageGrid.getFluidInventory(), storageGrid.getItemInventory());
                    this.monitor.setFluidMonitorObject(this.fluidMonitor);
                    if (this.monitor.getMonitor() == null) {
                        this.setValidContainer(false);
                    } else {
                        this.monitor.addListener();
                        this.fluidMonitor.addListener();
                    }
                }
            } else {
                this.setValidContainer(false);
            }
        }
    }

    private void lockSlot() {
        if (this.it instanceof com.asdflj.wtct.inventory.item.WirelessTerminal wirelessTerminal) {
            final int slot = wirelessTerminal.getInventorySlot();
            if (!BaublesUtil.isBaublesSlot(slot)) {
                this.lockPlayerInventorySlot(slot);
            }
        }
    }

    public IInventory getInventoryByName(String name) {
        if (name.equals("player")) {
            return this.getInventoryPlayer();
        }
        return this.it.getInventoryByName(name);
    }

    @Override
    public IGridNode getNetworkNode() {
        return this.it.getGridNode();
    }

    @Override
    public boolean useRealItems() {
        return true;
    }

    @Override
    public ItemStack[] getViewCells() {
        return new ItemStack[0];
    }

    @Override
    public void saveChanges() {
        if (appeng.util.Platform.isServer()) {
            this.it.saveSettings();
        }
    }

    @Override
    public void onChangeInventory(IInventory inv, int slot, InvOperation mc, ItemStack removedStack,
        ItemStack newStack) {

    }

    /**
     * Fluids go to the network's fluid storage - the channel AE2's own terminal and ae2fc's terminals
     * use, and the only one that keeps a fluid a fluid.
     *
     * <p>
     * This used to store them as an ae2fc {@code ItemFluidDrop} in the item inventory. A drop is a
     * pseudo item, so the item storage then holds "water drops" and every grid can only draw the drop
     * sprite: a droplet tinted with the fluid's colour (ae2fc's own item renderer), never the fluid.
     * With the fluid stored in fluid storage the grid draws the fluid itself - water's still texture.
     *
     * <p>
     * The drop route stays as a fallback for a network with no fluid storage at all: its fluid
     * inventory has nowhere to put anything, and item storage is the only place it has.
     */
    @Override
    protected IAEFluidStack extractFluids(IAEFluidStack ifs, Actionable mode) {
        if (ifs.getStackSize() == 0) return ifs;
        final IAEFluidStack extracted = super.extractFluids(ifs, mode);
        if (extracted != null) return extracted;
        final IAEItemStack drop = this.host.getItemInventory()
            .extractItems(ItemFluidDrop.newAeStack(ifs), mode, this.getActionSource());
        return ItemFluidDrop.getAeFluidStack(drop);
    }

    @Override
    protected IAEFluidStack injectFluids(IAEFluidStack ifs, Actionable mode) {
        final IAEFluidStack leftover = super.injectFluids(ifs, mode);
        // Null or a short leftover means fluid storage took (some of) it; only a leftover as big as the
        // request means that channel could not take any of it, which is when the drop route is needed.
        if (leftover == null || leftover.getStackSize() < ifs.getStackSize()) return leftover;
        // ItemFluidDrop.newAeStack returns null for a zero-size request or a fluid the drop item cannot
        // represent - a null stack into the item monitor NPEs deep inside AE2's NetworkMonitor, so a null
        // here just means the item channel cannot take it either and everything stays leftover.
        final IAEItemStack drop = ItemFluidDrop.newAeStack(ifs);
        if (drop == null) return leftover;
        final IAEItemStack rest = this.host.getItemInventory()
            .injectItems(drop, mode, this.getActionSource());
        return ItemFluidDrop.getAeFluidStack(rest);
    }

    // ---------------------------------------------------------------------------------------------
    // Pattern encoding (IPatternContainer / IWidgetPatternContainer), driven by CPacketTerminalBtns
    // "PatternTerminal.*" messages exactly like the other pattern terminals of this mod.
    // ---------------------------------------------------------------------------------------------

    @Override
    public IPatternContainer getContainer() {
        return this;
    }

    @Override
    public boolean isPatternTerminal() {
        return true;
    }

    @Override
    public IPatternTerminal getPatternTerminal() {
        return this.it;
    }

    @Override
    public boolean hasRefillerUpgrade() {
        return this.it.hasRefillerUpgrade();
    }

    @Override
    public void refillBlankPatterns(Slot slot) {
        // TODO(pattern cache batch): pull blank patterns from the ME network into the given slot.
    }

    /** Mirrors the mode the host stores, which the "PatternTerminal.CraftMode" packet updates. */
    @Override
    public void detectAndSendChanges() {
        if (this.it != null) {
            if (appeng.util.Platform.isServer()) {
                // Mirrored from the host on the server only. The client's own terminal object is a
                // snapshot taken when its GUI opened, so mirroring there would undo the @GuiSync'd
                // value the server has just sent - which is what used to keep the mode (and with it
                // the tab highlight and the whole encoding layout) from ever following the terminal.
                if (this.craftingMode != this.it.isCraftingRecipe()) {
                    this.craftingMode = this.it.isCraftingRecipe();
                }
                this.substitute = this.it.isSubstitution();
                this.beSubstitute = this.it.canBeSubstitute();
                // The "merge identical materials" option lives on the terminal item; the NEI transfer
                // reads this field, so it has to follow the host (the packet writes the host directly).
                this.combine = this.it.shouldCombine();
                // The management switches live on the terminal item; mirror them so the client's four
                // toggle icons show what the terminal actually holds.
                this.mgmtUpload = this.it.isPatternManagementUpload();
                this.mgmtDisplayMode = this.it.getPatternManagementDisplayMode();
                this.mgmtShowSlots = this.it.isPatternManagementShowSlots();
                this.mgmtSearchMode = this.it.getPatternManagementSearchMode();
                this.mgmtExpanded = this.it.isPatternManagementExpanded();
                this.loadEditSlotPattern();
                this.openPendingEditSlotPatternIfAreaFree();
                // The craftable markers need the network's WHOLE craftable list - the client repo is
                // only fed the current search slice, so the container computes and pushes it.
                this.pushCraftableKeys();
                // The Ctrl+hammer's crafts beyond the one the confirm screen started are submitted here,
                // as their plans come back - the player lands on this container the moment they confirm.
                this.submitApprovedJobs(this.getPlayerInv().player);
            }
        }
        if (this.providerRegistry != null) {
            // Contents first: whatever the list has not been told about is queued in the registry's own
            // packet, which the call below then ships.
            this.providerSlotSync.tick();
            // Drives AE2's own row diffing: it notices changes in the provider pattern inventories and
            // pushes them to the client, which is what keeps the management list in step.
            this.providerRegistry.detectAndSendChanges();
        }
        this.updateCraftingPreview();
        super.detectAndSendChanges();
    }

    // ---------------------------------------------------------------------------------------------
    // Pattern management area (WCWT 样板管理区): the network's pattern providers, i.e. AE2's
    // interfaces. Everything about them - the list, the slot contents and the remote slot actions -
    // comes from AE2's own interface-terminal container, the same one the wireless dual-interface
    // terminal delegates to.
    // ---------------------------------------------------------------------------------------------

    /**
     * Pushes provider slot contents the client has not been told about, so the management list follows
     * the network live when a pattern is put in or taken out.
     *
     * <p>
     * AE2's {@code updateList} compares a tracked row's name, size, priority and online state and never
     * the patterns in its slots; contents travel only through its private {@code syncIfaceSlot}, which
     * only AE2's own slot actions call. Everything else that writes into a provider - this terminal's
     * upload buttons, the interface's own GUI, an import bus filling patterns, another player - therefore
     * stayed invisible in the list until the terminal was reopened. {@link ProviderSlotSync} closes that:
     * it holds what the client has been told and hands each difference to AE2's own overwrite entry, the
     * same one a click produces.
     */
    private void pushProviderSlot(final long id, final int slot, final ItemStack stack) {
        this.providerSlotSync.push(id, slot, stack);
    }

    /** True when the management area has a provider list to show. */
    public boolean hasProviderRegistry() {
        return this.providerRegistry != null;
    }

    /**
     * The management area's four switches, written through to the terminal item so they outlive the GUI
     * - WCWT stores exactly these four on its terminal item, so a terminal reopens the way it was left.
     * Each write is followed by {@link IPatternTerminal#saveSettings()}, which is what puts it in the
     * item's NBT; the mirror in {@link #detectAndSendChanges()} then refreshes the client's copy.
     */
    public void setMgmtUpload(final boolean upload) {
        if (this.it != null) {
            this.it.setPatternManagementUpload(upload);
            this.it.saveSettings();
        }
    }

    public void setMgmtDisplayMode(final int mode) {
        if (this.it != null) {
            this.it.setPatternManagementDisplayMode(mode);
            this.it.saveSettings();
        }
    }

    public void setMgmtShowSlots(final boolean showSlots) {
        if (this.it != null) {
            this.it.setPatternManagementShowSlots(showSlots);
            this.it.saveSettings();
        }
    }

    public void setMgmtSearchMode(final int mode) {
        if (this.it != null) {
            this.it.setPatternManagementSearchMode(mode);
            this.it.saveSettings();
        }
    }

    /**
     * WCWT has no such switch; this one is ours. It writes through to the terminal item like the
     * other four, so a terminal remembers whether its management area was left enlarged.
     */
    public void setMgmtExpanded(final boolean expanded) {
        if (this.it != null) {
            this.it.setPatternManagementExpanded(expanded);
            this.it.saveSettings();
        }
    }

    /**
     * A click on the folded cell of a provider's slot row. Every empty slot of that provider is folded
     * into that one cell, so a click there addresses the first slot that is really free; the server
     * resolves which one that is (the client's copy could be a tick old) and then runs AE2's own slot
     * action, which keeps the duplicate check, the slot's own validity rules and the update push in one
     * place - the pattern on the cursor goes in exactly as it would have into a visible empty slot.
     */
    public void insertIntoFirstFreeProviderSlot(final long id) {
        final IInventory patterns = this.providerPatterns(id);
        if (patterns == null) {
            return;
        }
        final int free = findFreeProviderSlot(patterns);
        if (free < 0) {
            return;
        }
        this.providerSlotAction(id, free, false);
    }

    /**
     * A click on a provider's pattern slot. The plain click exchanges through the player's cursor
     * (take one out / put one in / swap), Shift moves the pattern into the player's inventory - both
     * handled by AE2's own interface-terminal logic.
     */
    public void providerSlotAction(final long id, final int slot, final boolean quick) {
        if (this.providerRegistry == null) {
            return;
        }
        this.providerRegistry.doAction(
            (EntityPlayerMP) this.getPlayerInv().player,
            quick ? InventoryAction.SHIFT_CLICK : InventoryAction.PICKUP_OR_SET_DOWN,
            slot,
            id);
    }

    /**
     * Opens the UI of the machine a provider feeds - WCWT's {@code manage_open_ui} ("打开供应器对应机器 UI").
     *
     * <p>
     * WCWT walks the provider's own target sides and uses each neighbouring block as the player would
     * (a right click), then checks whether a container came up. 1.7.10 can do exactly that: the row's
     * interface is looked up in the world, its {@code getTargets()} gives the sides it feeds, and every
     * candidate neighbour is right-clicked for the player. Nothing is assumed about the machine's mod -
     * anything that opens a GUI from {@code onBlockActivated} works.
     */
    public boolean openProviderMachineUi(final long id) {
        if (this.providerRegistry == null) {
            return false;
        }
        final Object tracker = Ae2Reflect.getTrackedById(this.providerRegistry, id);
        if (tracker == null) {
            return false;
        }
        final int[] loc = Ae2Reflect.getTrackedLocation(tracker);
        final World world = Ae2Reflect.getTrackedWorld(tracker);
        if (loc == null || world == null) {
            return false;
        }
        final EntityPlayer player = this.getPlayerInv().player;
        for (final ForgeDirection dir : providerFeedSides(world, loc, Ae2Reflect.getTrackedSide(tracker))) {
            final int x = loc[0] + dir.offsetX;
            final int y = loc[1] + dir.offsetY;
            final int z = loc[2] + dir.offsetZ;
            if (openMachineUi(world, x, y, z, player, dir.getOpposite())) {
                return true;
            }
        }
        return false;
    }

    /**
     * The sides the row's interface feeds: its own targets when AE2 can tell us, otherwise every side -
     * the same order WCWT tries them in, with "every side" as the fallback rather than a failure.
     */
    private static ForgeDirection[] providerFeedSides(final World world, final int[] loc, final ForgeDirection side) {
        final TileEntity te = world.getTileEntity(loc[0], loc[1], loc[2]);
        IInterfaceHost host = null;
        if (te instanceof final IInterfaceHost iface) {
            host = iface;
        } else if (te instanceof final IPartHost partHost) {
            if (side != ForgeDirection.UNKNOWN) {
                final IPart part = partHost.getPart(side);
                if (part instanceof final IInterfaceHost iface) {
                    host = iface;
                }
            }
            for (final ForgeDirection d : ForgeDirection.VALID_DIRECTIONS) {
                if (host != null) {
                    break;
                }
                final IPart part = partHost.getPart(d);
                if (part instanceof final IInterfaceHost iface) {
                    host = iface;
                }
            }
        }
        if (host != null) {
            final EnumSet<ForgeDirection> targets = host.getTargets();
            if (targets != null && !targets.isEmpty()) {
                return targets.toArray(new ForgeDirection[0]);
            }
        }
        return ForgeDirection.VALID_DIRECTIONS;
    }

    /** Right-clicks that block for the player; true when it opened a container. */
    private static boolean openMachineUi(final World world, final int x, final int y, final int z,
        final EntityPlayer player, final ForgeDirection side) {
        if (!world.blockExists(x, y, z)) {
            return false;
        }
        final Block block = world.getBlock(x, y, z);
        if (block == null || block.isAir(world, x, y, z)) {
            return false;
        }
        final Container before = player.openContainer;
        // WCWT clicks the target "without item" (1.21's useWithoutItem), and that is what makes this
        // work on GTNH's machines: a GregTech block reads the held stack first and answers with its
        // cover/wrench behaviour, so a player carrying the terminal never reaches the machine's own
        // GUI. Lend the click an empty hand and put the stack straight back.
        final int heldSlot = player.inventory.currentItem;
        final ItemStack heldStack = player.inventory.getStackInSlot(heldSlot);
        player.inventory.setInventorySlotContents(heldSlot, null);
        try {
            block.onBlockActivated(world, x, y, z, player, side.ordinal(), 0.5F, 0.5F, 0.5F);
        } finally {
            player.inventory.setInventorySlotContents(heldSlot, heldStack);
        }
        // The same test WCWT uses: the player only has one open container, so a change means the click
        // was the one that opened the machine's GUI.
        return player.openContainer != before;
    }

    /**
     * WCWT's in-row upload button, "将样板缓存区里的样板依次保存到该供应器": every pattern in the pattern
     * cache moves into the first free slot of that provider. Patterns the provider already holds are
     * skipped, and the cache slot is only emptied once its pattern really went over.
     */
    public int uploadCacheToProvider(final long id) {
        final IInventory patterns = this.providerPatterns(id);
        if (patterns == null || this.patternCacheInv == null) {
            return 0;
        }
        int moved = 0;
        for (int i = 0; i < CACHE_SLOTS; i++) {
            final ItemStack cached = this.patternCacheInv.getStackInSlot(i);
            if (cached == null || !(cached.getItem() instanceof ICraftingPatternItem)
                || containsPattern(patterns, cached)) {
                continue;
            }
            final int free = findFreeProviderSlot(patterns);
            if (free < 0) {
                break; // provider is full
            }
            patterns.setInventorySlotContents(free, cached);
            this.patternCacheInv.setInventorySlotContents(i, null);
            // Straight to the list: the cache slot empties through the container's own slot sync, and
            // the provider slot has no such route - AE2 only reports those from its own clicks.
            this.pushProviderSlot(id, free, cached);
            moved++;
        }
        if (moved > 0) {
            this.refreshProvider(id);
            this.refillBlankPatternSlot();
            this.detectAndSendChanges();
        }
        return moved;
    }

    /**
     * Hands the pattern in the edit slot to a provider - WCWT's automatic transfer after encoding.
     * The target is the selected provider when it can take it, otherwise the first provider whose
     * name matches {@code search}, otherwise the first provider with room.
     */
    public boolean uploadEditSlotPattern(final long preferredId, final String search) {
        final ItemStack pattern = this.patternSlotOUT.getStack();
        if (pattern == null || !(pattern.getItem() instanceof ICraftingPatternItem)) {
            return false;
        }
        long target = this.providerHasRoom(preferredId) ? preferredId : -1;
        if (target < 0 && search != null && !search.isEmpty()) {
            target = this.findProviderByName(search);
        }
        if (target < 0) {
            target = this.findProviderWithRoom();
        }
        final IInventory patterns = this.providerPatterns(target);
        if (patterns == null || containsPattern(patterns, pattern)) {
            return false;
        }
        final int free = findFreeProviderSlot(patterns);
        if (free < 0) {
            return false;
        }
        patterns.setInventorySlotContents(free, pattern);
        this.patternSlotOUT.putStack(null);
        this.lastEditSlotPattern = null; // the edit slot is empty now
        this.refreshProvider(target);
        // The edit slot empties through the container's own slot sync; the provider slot is reported
        // here, immediately, so the row shows the pattern the moment it was handed over.
        this.pushProviderSlot(target, free, pattern);
        this.refillBlankPatternSlot();
        this.detectAndSendChanges();
        return true;
    }

    /**
     * Moves the pattern waiting in the edit slot into the pattern cache - the fallback for an encode
     * whose upload could not name a single provider, so the pattern is kept (and shown in the
     * management area) instead of being handed to a guess.
     */
    public boolean stashEditSlotPatternToCache() {
        final ItemStack pattern = this.patternSlotOUT.getStack();
        final boolean isPatternItem = pattern != null && pattern.getItem() instanceof ICraftingPatternItem;
        final int free = this.findEmptyPatternCacheSlot();
        if (!isPatternItem) {
            return false;
        }
        if (free < 0 || this.patternCacheInv == null) {
            return false;
        }
        this.patternCacheInv.setInventorySlotContents(free, pattern);
        this.patternSlotOUT.putStack(null);
        this.lastEditSlotPattern = null; // the edit slot is empty now
        this.refillBlankPatternSlot();
        this.detectAndSendChanges();
        return true;
    }

    /** Encodes the editing area and, when asked, uploads the result right away (WCWT's auto upload). */
    public void encodeAndUpload(final boolean upload, final long preferredId, final String search) {
        this.encode();
        if (upload && this.encodeSucceeded) {
            this.uploadEditSlotPattern(preferredId, search);
        }
    }

    /**
     * Puts a blank pattern back into the blank-pattern slot after one was consumed or a pattern was
     * handed to a provider, so encoding can continue without a manual refill (WCWT does the same
     * through {@code tryFillBlankPatternFromNetwork}).
     */
    public void refillBlankPatternSlot() {
        final ItemStack inSlot = this.patternSlotIN.getStack();
        if (inSlot != null && inSlot.stackSize >= inSlot.getMaxStackSize()) {
            return;
        }
        final IMEMonitor<IAEItemStack> monitor = this.getMonitor();
        if (monitor == null) {
            return;
        }
        final int wanted = inSlot == null ? 1 : inSlot.getMaxStackSize() - inSlot.stackSize;
        IAEItemStack got = null;
        for (final ItemStack blank : AEApi.instance()
            .definitions()
            .materials()
            .blankPattern()
            .maybeStack(wanted)
            .asSet()) {
            got = Platform
                .poweredExtraction(this.getPowerSource(), monitor, AEItemStack.create(blank), this.getActionSource());
        }
        if (got == null || got.getStackSize() <= 0) {
            return;
        }
        final ItemStack refill = got.getItemStack();
        final int amount = (int) Math.min(Integer.MAX_VALUE, got.getStackSize());
        if (inSlot == null) {
            refill.stackSize = amount;
            this.patternSlotIN.putStack(refill);
        } else {
            // Top the slot up rather than replacing it: putStack throws away whatever is already
            // there, so encoding once out of a full stack collapsed the slot to the single blank
            // the refill had just fetched.
            final ItemStack merged = inSlot.copy();
            merged.stackSize += amount;
            this.patternSlotIN.putStack(merged);
        }
        this.detectAndSendChanges();
    }

    /** The pattern inventory of a provider, or {@code null} for an unknown id. */
    private IInventory providerPatterns(final long id) {
        if (this.providerRegistry == null || id < 0) {
            return null;
        }
        final Object tracker = Ae2Reflect.getTrackedById(this.providerRegistry, id);
        return tracker == null ? null : Ae2Reflect.getTrackedPatterns(tracker);
    }

    private void refreshProvider(final long id) {
        final Object tracker = this.providerRegistry == null ? null
            : Ae2Reflect.getTrackedById(this.providerRegistry, id);
        if (tracker != null) {
            Ae2Reflect.refreshTracked(tracker);
        }
    }

    private boolean providerHasRoom(final long id) {
        final IInventory patterns = this.providerPatterns(id);
        return patterns != null && findFreeProviderSlot(patterns) >= 0;
    }

    /** Ids of every provider, in AE2's own row order. */
    private List<Long> providerIds() {
        if (this.providerRegistry == null) {
            return Collections.emptyList();
        }
        final Map<?, ?> tracked = Ae2Reflect.getTracked(this.providerRegistry);
        final List<Long> ids = new ArrayList<>(tracked.size());
        for (final Object tracker : tracked.values()) {
            final long id = Ae2Reflect.getTrackedId(tracker);
            if (id >= 0) {
                ids.add(id);
            }
        }
        return ids;
    }

    /**
     * The first provider whose row name matches {@code search}, case-insensitive. The match goes through
     * {@link NeCharUtil} so a pinyin query finds an interface named in Chinese.
     */
    private long findProviderByName(final String search) {
        if (this.providerRegistry == null) {
            return -1;
        }
        final String needle = search.toLowerCase(java.util.Locale.ROOT);
        for (final Object tracker : Ae2Reflect.getTracked(this.providerRegistry)
            .values()) {
            final String name = Ae2Reflect.getTrackedName(tracker);
            if (name != null && NeCharUtil.INSTANCE.contains(needle, name.toLowerCase(java.util.Locale.ROOT))
                && findFreeProviderSlot(Ae2Reflect.getTrackedPatterns(tracker)) >= 0) {
                return Ae2Reflect.getTrackedId(tracker);
            }
        }
        return -1;
    }

    private long findProviderWithRoom() {
        for (final long id : this.providerIds()) {
            if (this.providerHasRoom(id)) {
                return id;
            }
        }
        return -1;
    }

    /** The provider's first empty pattern slot, or {@code -1} when it has none. */
    private static int findFreeProviderSlot(final IInventory patterns) {
        if (patterns == null) {
            return -1;
        }
        for (int i = 0; i < patterns.getSizeInventory(); i++) {
            if (patterns.getStackInSlot(i) == null) {
                return i;
            }
        }
        return -1;
    }

    /** True when the provider already stores exactly this pattern (item, damage and NBT). */
    private static boolean containsPattern(final IInventory patterns, final ItemStack pattern) {
        for (int i = 0; i < patterns.getSizeInventory(); i++) {
            if (Platform.isSameItemPrecise(patterns.getStackInSlot(i), pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * AE2's own hook for "a slot's content changed" - {@code ContainerPatternTerm} notices its edit
     * slot in exactly this method. Reacting here too (rather than only from
     * {@link #detectAndSendChanges}) loads the pattern the moment it lands, whichever route it took
     * to get there; the per-tick check stays as a safety net.
     */
    @Override
    public void onSlotChange(final Slot slot) {
        super.onSlotChange(slot);
        if (slot == this.patternSlotOUT && appeng.util.Platform.isServer()) {
            this.loadEditSlotPattern();
        }
    }

    /**
     * AE2's generic shift-click routing ends with "nothing on the container side wants this stack, so
     * copy it into the first empty ghost slot"
     * ({@code AEBaseContainer#getValidDestinationFakeSlot}). Our encoding cells are plain
     * {@link SlotFake}s, so a stack shift-clicked from the player inventory used to be <em>copied</em>
     * into the pattern area while staying in the inventory. AE2's own pattern terminal never shows
     * this - it has no {@code SlotFake} at all - and the encoding area is not a configuration ghost
     * slot, so the fallback is switched off here. A shift-clicked stack that this terminal has no
     * slot for now goes to the ME network instead
     * ({@link ContainerMonitor#storeShiftClickedStack}), which is what AE2's terminals do.
     */
    @Override
    public SlotFake getValidDestinationFakeSlot(final ItemStack stack) {
        return null;
    }

    /**
     * The encoder's fluid marking is decided in the GUI, following GTNH's pattern terminal: Ctrl is
     * the key that turns a fluid container into the fluid it holds, and AE2's {@code
     * PacketInventoryAction} has nowhere to carry a key. {@code
     * GuiComprehensiveWorkTerminal#handlePatternCellFluidClick} therefore sends the finished cell
     * content with AE2's {@code PacketClickOrDragFakeSlot} instead, and only the clicks it does not
     * recognise arrive here.
     *
     * <p>
     * That is what {@code VirtualMEPhantomSlot} does too, and it is why nothing about fluids is
     * converted in this method any more: without Ctrl a bucket has to land in the cell as a bucket,
     * which is exactly what AE2's own handling below does. The earlier version of this terminal
     * followed AE2FluidCraft and turned every container into its fluid regardless of the key.
     */
    @Override
    public void doAction(final EntityPlayerMP player, final InventoryAction action, final int slotId, final long id) {
        super.doAction(player, action, slotId, id);
    }

    /** True for the encoder's own cells: the processing inputs and the output column. */
    private boolean isEncodingCell(final Slot slot) {
        if (slot == null) {
            return false;
        }
        for (final SlotFake s : this.encodingSlots) {
            if (s == slot) {
                return true;
            }
        }
        for (final SlotFake s : this.outputSlots) {
            if (s == slot) {
                return true;
            }
        }
        return false;
    }

    /**
     * True when the cell's amount may be typed in - AE2 1.21's {@code canModifyAmountForSlot}: only
     * the processing encoder's cells carry amounts, so crafting mode and everything else is out.
     */
    public boolean canSetCellAmount(final Slot slot) {
        return !this.craftingMode && this.isEncodingCell(slot);
    }

    /**
     * True when the cell takes a fluid mark. Separate from {@link #canSetCellAmount}: GTNH's pattern
     * terminal marks a container's fluid in the encoder whatever mode it is in (the amount dialog is
     * the processing-only one), so gating the fluid gesture on the amount gate is what used to make
     * "hold a bucket, Ctrl+click" do nothing in crafting mode.
     */
    public boolean canMarkFluid(final Slot slot) {
        return this.isEncodingCell(slot);
    }

    /**
     * The server half of "Ctrl+right-click a fluid storage cell onto an encoder cell": a cell's
     * fluids live in the storage manager, which the client cannot read, so the GUI asks here and the
     * first fluid the cell holds is marked into the cell as the fluid packet a processing pattern
     * carries. Partitioned-but-empty cells mark their partition instead.
     */
    public void markFluidFromHeldCell(final EntityPlayer player, final int slotNumber, final boolean fromCell) {
        if (slotNumber < 0 || slotNumber >= this.inventorySlots.size()) {
            return;
        }
        final Slot slot = this.getSlot(slotNumber);
        // The fluid comes either out of the item the player holds (a tank item) or out of the item
        // already sitting in the cell - both keep it where only the server can read it.
        final ItemStack source = fromCell ? (slot == null ? null : slot.getStack()) : player.inventory.getItemStack();
        final FluidStack marked = source == null ? null : fluidOfCell(source);
        if (marked == null || marked.amount <= 0) {
            return;
        }
        if (!this.canMarkFluid(slot)) {
            return;
        }
        slot.putStack(ItemFluidPacket.newStack(marked));
        this.detectAndSendChanges();
    }

    /** The fluid a storage cell holds, read the way the cell itself reads it (server side only). */
    private static FluidStack fluidOfCell(final ItemStack stack) {
        try {
            final appeng.api.storage.IMEInventoryHandler<?> inv = appeng.api.AEApi.instance()
                .registries()
                .cell()
                .getCellInventory(
                    stack,
                    null,
                    appeng.util.item.AEFluidStackType.FLUID_STACK_TYPE);
            if (inv instanceof final com.asdflj.wtct.common.storage.ITFluidCellInventoryHandler handler) {
                final com.asdflj.wtct.common.storage.ITFluidCellInventory cell = handler.getCellInv();
                if (cell == null) {
                    return null;
                }
                for (final IAEFluidStack stored : cell.getContents()) {
                    if (stored != null && stored.getFluidStack() != null) {
                        final FluidStack fluid = stored.getFluidStack()
                            .copy();
                        fluid.amount = (int) Math.max(1L, Math.min(Integer.MAX_VALUE, stored.getStackSize()));
                        return fluid;
                    }
                }
                for (final IAEFluidStack partitioned : handler.getPartitionInv()) {
                    if (partitioned != null && partitioned.getFluidStack() != null) {
                        final FluidStack fluid = partitioned.getFluidStack()
                            .copy();
                        fluid.amount = 1000;
                        return fluid;
                    }
                }
            }
        } catch (final Throwable ignored) {
            // NO-OP - a cell that cannot be read marks nothing.
        }
        return null;
    }

    /**
     * Sets the amount of the given encoder cell, the server half of the middle-click amount entry.
     * Fluid packets carry their amount inside the tag, items in the stack size, and fake cells are not
     * bound by a stack's maximum - the amount is what the pattern will ask for. A non-positive amount
     * is ignored rather than treated as a wipe: clearing a cell is the hand gesture's job.
     */
    public void setCellAmount(final int slotNumber, final long amount) {
        if (slotNumber < 0 || slotNumber >= this.inventorySlots.size()) {
            return;
        }
        final Slot slot = this.getSlot(slotNumber);
        if (!this.canSetCellAmount(slot)) {
            return;
        }
        if (slot.getStack() == null) {
            return;
        }
        if (amount <= 0) {
            // See the screen's half: a zero the player never asked for must not wipe the cell.
            return;
        }
        final ItemStack updated = cellWithAmount(slot.getStack(), amount);
        slot.putStack(updated);
        this.detectAndSendChanges();
    }

    /**
     * The cell a given amount turns the current one into, or null when the amount clears it. A cell
     * that stands for a fluid keeps its number inside the fluid, in a fluid packet - the shape every
     * fluid cell here is held in, and the only place this mod and GTNH's own pattern readers look for
     * it. Anything else keeps the amount in the stack size, and a fake cell is not bound by a stack's
     * maximum because the amount is what the pattern will ask for. Shared with the screen, which
     * writes its own copy of the cell before telling the server, so both halves end up with the same
     * stack.
     */
    public static ItemStack cellWithAmount(final ItemStack current, final long amount) {
        if (current == null || amount <= 0) {
            return null;
        }
        final FluidStack fluid = PatternScaling.fluidCarriedBy(current);
        if (fluid != null) {
            final FluidStack marked = fluid.copy();
            marked.amount = (int) Math.min(Integer.MAX_VALUE, amount);
            return ItemFluidPacket.newStack(marked);
        }
        final ItemStack updated = current.copy();
        updated.stackSize = (int) Math.min(Integer.MAX_VALUE, amount);
        return updated;
    }

    /**
     * The amount a cell asks for: a fluid's own count where it carries one (a packet, a recipe's
     * GregTech display item, where GTNH keeps the recipe's number in the fluid), the stack size
     * otherwise. The screen draws this same number and the encoder writes it, so what a cell shows is
     * what the pattern will ask for.
     */
    public static long fluidCellAmount(final ItemStack stack) {
        if (stack == null) {
            return 0;
        }
        final FluidStack fluid = PatternScaling.fluidCarriedBy(stack);
        return fluid != null && fluid.amount > 1 ? fluid.amount : Math.max(1, stack.stackSize);
    }

    /**
     * The same cell in this mod's canonical fluid shape: a cell that stands for a fluid becomes the
     * fluid packet carrying that fluid with the amount the cell was asking for, and a packet (or a
     * plain item, a bucket included) is handed back untouched.
     *
     * <p>
     * A recipe transfer leaves GregTech's display item behind - or NEI's phantom item - whose own
     * stack size is one while the number sits in the fluid it displays, so a cell read without this
     * step says "one unit of that fluid" to a scaling or to the encoder. Normalising first is what
     * makes the number on the cell the number in the pattern.
     */
    public static ItemStack asFluidCell(final ItemStack stack) {
        if (stack == null || stack.getItem() instanceof ItemFluidPacket) {
            return stack;
        }
        if (PatternScaling.fluidCarriedBy(stack) == null) {
            return stack;
        }
        return cellWithAmount(stack, fluidCellAmount(stack));
    }

    /** Every fluid cell of the editing area, and of the output column, back in the canonical shape. */
    private void normalizeFluidCells() {
        for (final SlotFake slot : this.encodingSlots) {
            slot.putStack(asFluidCell(slot.getStack()));
        }
        for (final SlotFake slot : this.outputSlots) {
            slot.putStack(asFluidCell(slot.getStack()));
        }
    }

    /**
     * The server half of Shift+wheel over an encoding cell: every cell matching {@code in} takes
     * {@code out}, the way GTNH's own terminals cycle a recipe's alternatives. Two things differ,
     * both because these cells carry amounts:
     *
     * <ul>
     * <li>a cell normalised to a fluid packet still answers to the display item the picker offers for
     * the same fluid, so the wheel keeps working on a fluid it has already replaced once;
     * <li>a fluid cell keeps the amount it was asking for - cycling the ingredient picks the fluid, it
     * must not throw away the number the player set on the cell.
     * </ul>
     *
     * @return true when a cell was written
     */
    public boolean cycleEncodingIngredient(final ItemStack in, final ItemStack out) {
        if (out == null) {
            return false;
        }
        final IInventory inv = this.getInventoryByName(Constants.CRAFTING_EX);
        if (inv == null) {
            return false;
        }
        boolean changed = false;
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            final ItemStack cell = inv.getStackInSlot(i);
            if (!sameIngredient(cell, in)) {
                continue;
            }
            // The replacement arrives ready to be written - the picker's screen has already put the
            // amount back on it (see cellKeepingAmount), and it is the screen that can read a fluid out
            // of the display item a recipe offers.
            inv.setInventorySlotContents(i, out);
            changed = true;
        }
        if (changed) {
            this.onCraftMatrixChanged(inv);
            this.saveChanges();
        }
        return changed;
    }

    /**
     * True when a cell holds the ingredient the picker is cycling away from: the same stack, or
     * another carrier of the same fluid - a cell is held as a fluid packet where the recipe offers
     * GregTech's display item for that fluid.
     */
    private static boolean sameIngredient(final ItemStack cell, final ItemStack picking) {
        if (Platform.isSameItemPrecise(cell, picking)) {
            return true;
        }
        final FluidStack held = PatternScaling.fluidCarriedBy(cell);
        final FluidStack wanted = PatternScaling.fluidCarriedBy(picking);
        return held != null && wanted != null && held.isFluidEqual(wanted);
    }

    /**
     * {@code offered} as the cell that replaces {@code cell}: a fluid keeps the amount the cell was
     * asking for, anything else is taken exactly as the picker offers it.
     *
     * <p>
     * Cycling an ingredient picks which fluid goes into the cell; it is not an instruction to throw
     * away the number the player set there, which is what used to happen - the picker's stack is the
     * recipe's own display item, whose amount is the recipe's, so writing it verbatim put the recipe's
     * number (or a bare one) back on the cell.
     */
    public static ItemStack cellKeepingAmount(final ItemStack cell, final ItemStack offered) {
        if (PatternScaling.fluidCarriedBy(offered) == null) {
            return offered;
        }
        return cellWithAmount(offered, fluidCellAmount(cell));
    }

    /** One line per cell: item id and amount, or {@code -}. */
    private String[] cellSnapshot() {
        final String[] out = new String[this.encodingSlots.length + this.outputSlots.length];
        for (int i = 0; i < this.encodingSlots.length; i++) {
            out[i] = describeCell(this.encodingSlots[i]);
        }
        for (int i = 0; i < this.outputSlots.length; i++) {
            out[this.encodingSlots.length + i] = describeCell(this.outputSlots[i]);
        }
        return out;
    }

    private static String describeCell(final SlotFake slot) {
        final ItemStack stack = slot == null ? null : slot.getStack();
        if (stack == null || stack.getItem() == null) {
            return "-";
        }
        return net.minecraft.item.Item.itemRegistry.getNameForObject(stack.getItem()) + "x" + stack.stackSize;
    }

    /**
     * The "edit slot": besides receiving the encoder's output, an already encoded pattern dropped in
     * there is loaded back into the editing area, so a finished pattern can be re-edited instead of
     * rebuilt from scratch (WCWT's "样板编辑槽…也可放入现有已编码样板回填到编辑区").
     *
     * <p>
     * This follows GTNH's AE, whose {@code appeng.api.parts.IPatternTerminal#loadPatternFromItem}
     * is exactly this operation for its own pattern terminals, in the same order:
     *
     * <ol>
     * <li>the options come off the pattern's own NBT ({@code crafting} / {@code substitute} /
     * {@code beSubstitute}) and are pushed onto the terminal;
     * <li>the contents are read with AE2's pattern readers - {@code PatternHelper} for a plain
     * encoded pattern, {@code UltimatePatternHelper} for the Ultimate one (see
     * {@link PatternScaling#readPatternCells});
     * <li>both cells sets are cleared and then filled by slot, so positions are kept;
     * <li>the pattern stays where it is, so pressing encode again simply overwrites it.
     * </ol>
     *
     * <p>
     * The differences: our terminal does not implement AE2's {@code IPatternTerminal} and its
     * editing area is slot based rather than an {@code IAEStackInventory}, so the recipe is followed
     * here by hand; and the trigger is {@link #detectAndSendChanges()}, because 1.7.10 has no
     * equivalent of the {@code PatternEncodingLogic} that drives this on 1.21. Doing it per tick
     * also catches patterns arriving through shift-click or from the pattern cache, not just a drag
     * onto the slot. Crafting patterns fill the 3x3 matrix (amounts forced to one, a crafting matrix
     * has none) and select crafting mode; processing patterns fill the inputs plus the three output
     * cells and select processing mode.
     */
    private void loadEditSlotPattern() {
        final ItemStack pattern = this.patternSlotOUT.getStack();
        if (ItemStack.areItemStacksEqual(pattern, this.lastEditSlotPattern)) {
            return;
        }
        // Remembered before anything is written: the writes below change slots, which re-enters
        // detectAndSendChanges().
        this.lastEditSlotPattern = pattern == null ? null : pattern.copy();

        if (pattern == null) {
            return;
        }
        // The editing area as the player left it. When it no longer holds what the last load put
        // there, the player has been working in it - and the *same* pattern arriving again at that
        // moment is the encoder's own bookkeeping, not a pattern to open. Loading it would clear the
        // cells and write the old pattern's contents back: the "materials jump out of the cells / the
        // fluid mark disappears" report. A different pattern is the player handing this area a new
        // one - pulled out of the pattern cache, or dropped into the slot by hand - and it takes the
        // area over; holding that back as well left the previous encoding's cells sitting there, so
        // the new pattern never appeared.
        final String area = java.util.Arrays.toString(this.cellSnapshot());
        if (this.lastLoadedArea != null && !this.lastLoadedArea.equals(area)
            && ItemStack.areItemStacksEqual(pattern, this.lastAreaPattern)) {
            this.pendingEditSlotLoad = pattern.copy();
            return;
        }
        this.pendingEditSlotLoad = null;
        final NBTTagCompound tag = pattern.getTagCompound();
        if (tag == null || !(pattern.getItem() instanceof ICraftingPatternItem)) {
            return;
        }
        final List<List<ItemStack>> cells = PatternScaling.readPatternCells(pattern);
        if (cells == null) {
            return;
        }
        final boolean crafting = tag.getBoolean("crafting");

        this.clearEncodingArea();
        final List<ItemStack> inputs = cells.get(0);
        if (crafting) {
            for (int i = 0; i < CRAFTING_MATRIX; i++) {
                this.encodingSlots[i].putStack(withAmount(cellAt(inputs, i), 1));
            }
        } else {
            for (int i = 0; i < this.encodingSlots.length; i++) {
                this.encodingSlots[i].putStack(cellAt(inputs, i));
            }
            final List<ItemStack> outputs = cells.get(1);
            for (int i = 0; i < this.outputSlots.length; i++) {
                this.outputSlots[i].putStack(cellAt(outputs, i));
            }
        }
        this.setCraftingMode(crafting);
        this.restoreEncodingOptions(tag);
        this.detectAndSendChanges();
        // The area is now exactly this pattern - that is the state a later edit-slot change can open
        // over again without costing anybody their work.
        this.lastLoadedArea = java.util.Arrays.toString(this.cellSnapshot());
        this.lastAreaPattern = pattern.copy();
    }

    /**
     * Opens the pattern the edit slot offered while the area still held the player's work, once the
     * area has been emptied by hand. This is the only way a held-back load ever comes through, so no
     * pattern is dropped without the player being able to reach it.
     */
    private void openPendingEditSlotPatternIfAreaFree() {
        if (this.pendingEditSlotLoad == null) {
            return;
        }
        for (final String cell : this.cellSnapshot()) {
            if (!"-".equals(cell)) {
                return; // still holding something - keep waiting
            }
        }
        this.pendingEditSlotLoad = null;
        // Forcing the loader to take the edit slot's pattern again: the guard below compares against
        // an area snapshot, and the area it will load over is this empty one.
        this.lastEditSlotPattern = null;
        this.lastLoadedArea = java.util.Arrays.toString(this.cellSnapshot());
    }

    /** The loaded cell for a slot index, or null when the pattern has nothing there. */
    private static ItemStack cellAt(final List<ItemStack> cells, final int index) {
        return index < cells.size() ? cells.get(index) : null;
    }

    /**
     * Restores the encoding options the pattern was made with, in the same order
     * {@code IPatternTerminal#loadPatternFromItem} does. A pattern carries them in its own NBT (what
     * {@link #encode()} writes), so editing and re-encoding one does not silently drop its
     * substitution settings.
     */
    private void restoreEncodingOptions(final NBTTagCompound tag) {
        if (tag.hasKey("substitute")) {
            this.it.setSubstitution(tag.getBoolean("substitute"));
        }
        if (tag.hasKey("beSubstitute")) {
            this.it.setBeSubstitute(tag.getBoolean("beSubstitute"));
        }
    }

    /** A copy of {@code stack} carrying {@code amount}, or null for an empty cell. */
    private static ItemStack withAmount(final ItemStack stack, final int amount) {
        if (stack == null) {
            return null;
        }
        final ItemStack copy = stack.copy();
        copy.stackSize = amount;
        return copy;
    }

    /**
     * Switches the encoding mode server-side (NEI recipe transfer decides this from the recipe kind),
     * keeping the terminal's own mode flag in step so the client redraws the right area.
     */
    public void setCraftingMode(final boolean crafting) {
        this.craftingMode = crafting;
        if (this.it != null) {
            this.it.setCraftingRecipe(crafting);
        }
    }

    /**
     * Encodes the editing area into a pattern. Crafting mode matches the 3x3 matrix against the
     * vanilla recipe list and stores its result as the single output; processing mode takes the
     * matrix as inputs and the output column as outputs. Both only differ in the "crafting" flag
     * and where the two lists come from.
     *
     * <p>
     * The pattern *item* is picked by mode, exactly as AE2's own
     * {@code ContainerPatternTerm#encode} does: a crafting pattern is a plain encoded pattern (its
     * 3x3 matrix is item-only), a processing one is an Ultimate pattern - the only kind whose reader
     * understands a fluid entry. The entries themselves are written by {@link PatternScaling}, in the
     * generic form GTNH's ME pattern terminal writes, so a pattern made here is the same pattern GTNH
     * makes. That is also why the slot's previous item is replaced rather than re-tagged: an item of
     * the wrong kind would carry an entry its own reader cannot read.
     */
    @Override
    public void encode() {
        this.encodeSucceeded = false;
        final ItemStack[] in;
        final ItemStack[] out;
        if (this.craftingMode) {
            final ItemStack result = this.findCraftingResult();
            in = this.collectInputs(true);
            if (in == null || result == null) {
                return;
            }
            out = new ItemStack[] { result };
        } else {
            // A fluid cell is written as a fluid entry, so the editing area is put in the canonical
            // fluid shape first: a recipe transfer leaves GregTech's display item behind, whose own
            // stack size is one, and the number it stands for lives in the fluid it displays.
            this.normalizeFluidCells();
            in = this.collectInputs(false);
            out = this.collectOutputs();
            if (in == null || out == null) {
                return;
            }
        }

        if (this.patternSlotOUT.getStack() != null && this.notPattern(this.patternSlotOUT.getStack())) {
            return; // occupied by something that is not a pattern
        }
        if (this.patternSlotOUT.getStack() == null) {
            // An empty slot tops itself up from the network first, so a terminal that has run out of
            // blanks keeps encoding instead of refusing for want of one...
            if (this.notPattern(this.patternSlotIN.getStack())) {
                this.refillBlankPatternSlot();
            }
            final ItemStack blank = this.patternSlotIN.getStack();
            if (this.notPattern(blank)) {
                return; // no blank pattern available
            }
            blank.stackSize--;
            if (blank.stackSize <= 0) {
                this.patternSlotIN.putStack(null);
            }
            // ...and one goes back in right after the blank was consumed. Encoding is the one place a
            // blank is always taken, so it is also the place to replace it: without this, an encode
            // that is not uploaded - or whose result the player takes out by hand - left the slot
            // empty and the next encode refused for want of a blank.
            this.refillBlankPatternSlot();
        }
        final ItemStack pattern = patternItem(this.craftingMode);
        if (pattern == null) {
            return;
        }

        final NBTTagCompound value = new NBTTagCompound();
        final NBTTagList tagIn = new NBTTagList();
        final NBTTagList tagOut = new NBTTagList();
        final boolean fluidAsStack = !this.craftingMode;
        for (final ItemStack stack : in) {
            tagIn.appendTag(PatternScaling.patternEntry(stack, fluidAsStack));
        }
        for (final ItemStack stack : out) {
            tagOut.appendTag(PatternScaling.patternEntry(stack, fluidAsStack));
        }
        value.setTag("in", tagIn);
        value.setTag("out", tagOut);
        if (this.craftingMode) {
            value.setBoolean("crafting", true);
        }
        value.setBoolean("substitute", this.it.isSubstitution());
        value.setBoolean("beSubstitute", this.it.canBeSubstitute());
        value.setBoolean("prioritize", this.it.isPrioritize());
        pattern.setTagCompound(value);
        pattern.stackTagCompound.setString("author", this.getPlayerInv().player.getCommandSenderName());
        this.patternSlotOUT.putStack(pattern);
        // Remember the result as "already in the edit slot" so the loader does not treat the
        // encoder's own output as a pattern that was dropped in - the editing area already holds
        // exactly this, and reloading it would normalise it (a crafting matrix's amounts are forced
        // to one on load, a processing one would be re-packed).
        this.lastEditSlotPattern = pattern.copy();
        // The area is what this pattern was made of: a state a later edit-slot change may open over
        // without destroying anything.
        this.lastLoadedArea = java.util.Arrays.toString(this.cellSnapshot());
        this.lastAreaPattern = pattern.copy();
        this.encodeSucceeded = true;
        this.detectAndSendChanges();
    }

    @Override
    public void encodeAndMoveToInventory() {
        this.encode();
        this.stashEncodedPattern();
    }

    /** Moves the finished pattern into a free cache slot, falling back to the player inventory. */
    private void stashEncodedPattern() {
        final ItemStack output = this.patternSlotOUT.getStack();
        if (output == null) {
            return;
        }
        final int free = this.findEmptyPatternCacheSlot();
        if (free >= 0) {
            this.cacheSlots[free].putStack(output);
        } else if (!this.getPlayerInv()
            .addItemStackToInventory(output)) {
                this.dropItem(output);
            }
        this.patternSlotOUT.putStack(null);
    }

    @Override
    public void encodeAllItemAndMoveToInventory() {
        this.encode();
        final ItemStack output = this.patternSlotOUT.getStack();
        if (output != null) {
            final ItemStack blanks = this.patternSlotIN.getStack();
            if (blanks != null) {
                output.stackSize += blanks.stackSize;
            }
            if (!this.getPlayerInv()
                .addItemStackToInventory(output)) {
                this.dropItem(output);
            }
            this.patternSlotOUT.putStack(null);
            this.patternSlotIN.putStack(null);
        }
    }

    @Override
    public void clear() {
        this.clearEncodingArea();
        this.detectAndSendChanges();
    }

    /** Empties the encoder's input and output cells; the caller takes care of syncing. */
    private void clearEncodingArea() {
        for (final SlotFake slot : this.encodingSlots) {
            if (slot != null) {
                slot.putStack(null);
            }
        }
        for (final SlotFake slot : this.outputSlots) {
            if (slot != null) {
                slot.putStack(null);
            }
        }
    }

    @Override
    public void doubleStacks(int value) {
        if (this.craftingMode) {
            return; // crafting patterns carry no stack sizes
        }
        final boolean isShift = (value & 1) != 0;
        final boolean backwards = (value & 2) != 0;
        int multi = isShift ? 8 : 2;
        if (backwards) {
            multi = -multi;
        }
        final SlotFake[] inputs = this.scalableCells(this.encodingSlots);
        final SlotFake[] outputs = this.scalableCells(this.outputSlots);
        if (canDouble(inputs, multi) && canDouble(outputs, multi)) {
            doubleStacksInternal(inputs, multi);
            doubleStacksInternal(outputs, multi);
        }
        this.detectAndSendChanges();
    }

    /**
     * True when the terminal's "编程器电路始终为一" switch is on: multiplying a processing pattern then
     * scales everything except the programmable circuit it carries.
     */
    private boolean circuitAlwaysOne() {
        return ContainerWcwtSettings.isOn(this.getTerminalStack(), ContainerWcwtSettings.KEY_CIRCUIT_ALWAYS_ONE);
    }

    /**
     * The cells a multiply may touch: with the circuit switch on the cells holding a programming
     * circuit are left out, so they are neither scaled nor able to refuse the operation - a circuit
     * sitting at one would otherwise make a ÷2 fail its exactness check and leave the whole row doing
     * nothing.
     */
    private SlotFake[] scalableCells(final SlotFake[] cells) {
        if (!this.circuitAlwaysOne()) {
            return cells;
        }
        final List<SlotFake> kept = new ArrayList<>(cells.length);
        for (final SlotFake cell : cells) {
            if (cell != null && PHUtil.isProgrammingCircuit(cell.getStack())) {
                continue;
            }
            kept.add(cell);
        }
        return kept.toArray(new SlotFake[0]);
    }

    /**
     * WCWT's batch multiplier row (⇄ ×2 ×3 ×5 / =1 ÷2 ÷3 ÷5). Like WCWT's
     * {@code applyPatternMultiplier} it hits two places at once:
     *
     * <ul>
     * <li>the encoder's own input/output amounts - only while a <em>processing</em> pattern is on
     * the bench, since a crafting matrix has no amounts to scale;
     * <li>every encoded processing pattern in the pattern cache, which is why the buttons still do
     * something while a crafting pattern is being edited.
     * </ul>
     *
     * <p>
     * The amount semantics ("=1" reduces to the smallest whole-number ratio rather than setting
     * everything to one, divides must be exact, multiplies must fit WCWT's cap) live in
     * {@link PatternScaling}, together with the all-or-nothing rule.
     */
    @Override
    public void multiplyStacks(final int multiplier) {
        boolean changed = false;
        if (!this.craftingMode) {
            // The amounts are scaled where they live, so a fluid cell is put in the canonical fluid
            // shape first - otherwise the number scaled is the display item's stack size (one) while
            // the cell shows the fluid's own.
            this.normalizeFluidCells();
            changed = PatternScaling.apply(
                this.slotStacks(this.encodingSlots),
                this.slotStacks(this.outputSlots),
                multiplier,
                this.circuitAlwaysOne());
        }
        changed |= this.rescaleCachedPatterns(multiplier);
        if (changed) {
            this.detectAndSendChanges();
        }
    }

    /**
     * WCWT's {@code changePatternCacheSubstitutionMode}: re-encodes every cached <em>crafting</em>
     * pattern with the given substitution flags (processing patterns have no such options). Item
     * substitution maps to the pattern's {@code substitute} bit; the second radio maps to
     * {@code beSubstitute} - AE2's own "可作为替换" option, which 1.21 spells {@code fluidSubstitute}
     * but 1.7.10 only has in that form. A {@code null} flag leaves that bit untouched, like the
     * original keeps the other mode's current value.
     */
    /**
     * WCWT's {@code changePatternCacheSubstitutionMode}, and the encoder's own option with it: the
     * switches sit with the batch multiplier, so what they say has to hold for the pattern encoded
     * next as well as for the patterns already in the cache - setting only the cache is what made them
     * look like they did nothing. Item substitution maps to the pattern's {@code substitute} bit; the
     * second switch maps to {@code beSubstitute} - AE2's own "可作为替换" option, which 1.21 spells
     * {@code fluidSubstitute} but 1.7.10 only has in that form. A {@code null} flag leaves that bit
     * untouched, like the original keeps the other mode's current value.
     *
     * @return how many cached patterns were re-written, so the caller can say so
     */
    public int setCachedSubstitutions(final Boolean item, final Boolean fluid) {
        if (item != null) {
            this.it.setSubstitution(item);
        }
        if (fluid != null) {
            this.it.setBeSubstitute(fluid);
        }
        if (this.patternCacheInv == null) {
            return 0;
        }
        int changed = 0;
        for (int i = 0; i < this.patternCacheInv.getSizeInventory(); i++) {
            final ItemStack stack = this.patternCacheInv.getStackInSlot(i);
            if (stack == null || !(stack.getItem() instanceof ICraftingPatternItem)) {
                continue;
            }
            final NBTTagCompound tag = stack.getTagCompound();
            if (tag == null || !tag.getBoolean("crafting")) {
                continue;
            }
            // Copy first: the change detector compares every slot against the snapshot it took when
            // the container opened, so the same instance with an edited tag still counts as equal and
            // the edit would never be sent - the flag has to travel on a new stack to take effect.
            final ItemStack updated = stack.copy();
            if (item != null) {
                updated.getTagCompound()
                    .setBoolean("substitute", item);
            }
            if (fluid != null) {
                updated.getTagCompound()
                    .setBoolean("beSubstitute", fluid);
            }
            this.patternCacheInv.setInventorySlotContents(i, updated);
            changed++;
        }
        this.detectAndSendChanges();
        return changed;
    }

    /**
     * WCWT's swap (⇄) button: each processing pattern's outputs rotate one step, so a secondary
     * output is promoted to the primary one - in the encoder and in the cached patterns alike.
     * Crafting patterns have a single result and are left alone.
     */
    @Override
    public void rotateOutputs() {
        boolean changed = false;
        if (!this.craftingMode) {
            changed = this.rotateEncodingOutputs();
        }
        changed |= this.rescaleCachedPatterns(PatternScaling.ROTATE);
        if (changed) {
            this.detectAndSendChanges();
        }
    }

    /** The encoder's live stacks, in slot order and with empty cells kept as nulls. */
    private static List<ItemStack> slotStacks(final SlotFake[] slots) {
        final List<ItemStack> stacks = new ArrayList<>(slots.length);
        for (final SlotFake slot : slots) {
            stacks.add(slot == null ? null : slot.getStack());
        }
        return stacks;
    }

    /** Rotates the encoder's outputs; the rotated stacks have to be written back into the cells. */
    private boolean rotateEncodingOutputs() {
        final List<ItemStack> outputs = this.slotStacks(this.outputSlots);
        if (!PatternScaling.rotate(outputs)) {
            return false;
        }
        for (int i = 0; i < this.outputSlots.length && i < outputs.size(); i++) {
            this.outputSlots[i].putStack(outputs.get(i));
        }
        return true;
    }

    /**
     * Applies an operation to every encoded processing pattern in the cache area. Crafting patterns
     * and patterns whose amounts cannot be read back are skipped one by one, the way WCWT's
     * per-pattern {@code checkCanModify} does it.
     */
    private boolean rescaleCachedPatterns(final int operation) {
        if (this.patternCacheInv == null) {
            return false;
        }
        boolean changed = false;
        final int size = Math.min(this.cacheSlots.length, this.patternCacheInv.getSizeInventory());
        for (int i = 0; i < size; i++) {
            // Read through the inventory rather than the slot: SlotRestrictedInput renders encoded
            // patterns through their *output*, so getStack() is not the pattern stack here.
            final ItemStack rewritten = PatternScaling
                .applyToPattern(this.patternCacheInv.getStackInSlot(i), operation, this.circuitAlwaysOne());
            if (rewritten != null) {
                this.cacheSlots[i].putStack(rewritten);
                changed = true;
            }
        }
        return changed;
    }

    /**
     * True when the stack is neither an encoded nor a blank pattern. The blank test is AE2's blank-pattern material
     * definition (rv3-beta-977 has no {@code IPatternTerminal#isBlankPattern}); for the encoded side
     * {@code ICraftingPatternItem} is used rather than AE2's {@code isEncodedPattern}, which only accepts
     * {@code ItemEncodedPattern} and would reject the Ultimate variant.
     */
    private static boolean notPattern(final ItemStack stack) {
        return !(stack != null && stack.getItem() instanceof ICraftingPatternItem) && !AEApi.instance()
            .definitions()
            .materials()
            .blankPattern()
            .isSameAs(stack);
    }

    /**
     * Inputs for the encoder. Crafting mode keeps AE2's positional list (a 3x3 matrix with nulls
     * preserved, so recipe layouts survive); processing mode condenses the 18 encoding inputs into a
     * sparse list, which is what AE2's own processing patterns store.
     */
    private ItemStack[] collectInputs(final boolean isCrafting) {
        if (isCrafting) {
            final ItemStack[] in = new ItemStack[9];
            boolean any = false;
            for (int i = 0; i < 9; i++) {
                final ItemStack stack = this.encodingSlots[i].getStack();
                in[i] = stack;
                any |= stack != null;
            }
            return any ? in : null;
        }
        final List<ItemStack> list = new ArrayList<>();
        for (final SlotFake slot : this.encodingSlots) {
            final ItemStack stack = slot.getStack();
            if (stack != null) {
                list.add(stack);
            }
        }
        return list.isEmpty() ? null : list.toArray(new ItemStack[0]);
    }

    /**
     * The pattern's outputs, empty cells left out - AE2's own encoder does the same. An unused cell
     * written as an entry is what makes a pattern unreadable to the grid: a processing pattern is
     * read by {@code UltimatePatternHelper}, whose output list keeps a null for every empty entry, and
     * AE2's own grid rebuild copies each of them without looking - so one pattern with an unused
     * output cell takes the whole server down on the next grid rebuild, that is, on every world load.
     */
    private ItemStack[] collectOutputs() {
        final List<ItemStack> out = new ArrayList<>();
        for (final SlotFake slot : this.outputSlots) {
            final ItemStack stack = slot.getStack();
            if (stack != null) {
                out.add(stack);
            }
        }
        return out.isEmpty() ? null : out.toArray(new ItemStack[0]);
    }

    private ItemStack findCraftingResult() {
        final InventoryCrafting ic = new InventoryCrafting(new ContainerNull(), 3, 3);
        for (int i = 0; i < 9; i++) {
            ic.setInventorySlotContents(i, this.encodingSlots[i].getStack());
        }
        return CraftingManager.getInstance()
            .findMatchingRecipe(ic, this.getPlayerInv().player.worldObj);
    }

    /**
     * The pattern item a mode is encoded into - AE2's own choice: the plain encoded pattern for a
     * crafting recipe, the Ultimate one for a processing recipe.
     */
    private static ItemStack patternItem(final boolean crafting) {
        final IItemDefinition definition = crafting ? AEApi.instance()
            .definitions()
            .items()
            .encodedPattern()
            : AEApi.instance()
                .definitions()
                .items()
                .encodedUltimatePattern();
        return definition.maybeStack(1)
            .orNull();
    }

    // ---------------------------------------------------------------------------------------------
    // Encoding area accessors. The GUI lays these out per mode, exactly like WCWT 1.21: crafting
    // mode shows the 3x3 matrix plus the recipe preview, processing mode shows the 18 scrollable
    // inputs (3 rows at a time) plus the outputs - the two sets share the same columns and are never
    // visible together, which is what keeps them from overlapping.
    // ---------------------------------------------------------------------------------------------

    public SlotFake[] getEncodingSlots() {
        return this.encodingSlots;
    }

    public SlotFake[] getOutputSlots() {
        return this.outputSlots;
    }

    public SlotPatternTerm getCraftingResultSlot() {
        return this.craftingResultSlot;
    }

    public SlotRestrictedInput[] getCacheSlots() {
        return this.cacheSlots;
    }

    /** First free cache slot, or -1 when the cache is full (WCWT's findEmptyPatternCacheSlot). */
    private int findEmptyPatternCacheSlot() {
        if (this.patternCacheInv == null) {
            return -1;
        }
        for (int i = 0; i < this.patternCacheInv.getSizeInventory(); i++) {
            if (this.patternCacheInv.getStackInSlot(i) == null) {
                return i;
            }
        }
        return -1;
    }

    /** True for the slots the GUI lays out inside the encoding panel (used when painting them). */
    public boolean isEncodingAreaSlot(final Slot slot) {
        if (this.craftingResultSlot == slot) {
            return true;
        }
        for (final SlotFake s : this.encodingSlots) {
            if (s == slot) {
                return true;
            }
        }
        for (final SlotFake s : this.outputSlots) {
            if (s == slot) {
                return true;
            }
        }
        for (final SlotRestrictedInput s : this.cacheSlots) {
            if (s == slot) {
                return true;
            }
        }
        return false;
    }
}
