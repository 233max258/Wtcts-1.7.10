package com.asdflj.wtct.client.gui.container;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.asdflj.wtct.common.item.card.CardTicker;

import appeng.api.implementations.guiobjects.IGuiItemObject;
import appeng.api.storage.ITerminalHost;
import appeng.container.AEBaseContainer;
import appeng.util.Platform;

/**
 * The wireless terminal's settings screen - WCWT's
 * {@code WcwtWirelessTerminalSettingsSubScreen}, reduced to the switches this port has a feature
 * behind: the block picker and the magnet card. WCWT stores both on the terminal item's NBT (its
 * {@code PICK_BLOCK} / magnet components), so this screen reads and writes the same booleans the
 * {@link CardTicker} consults every tick.
 */
public class ContainerWcwtSettings extends AEBaseContainer {

    /** Terminal NBT keys, shared by the screen (display), the packet (write) and the ticker (read). */
    public static final String KEY_PICK_BLOCK = "PickBlock";
    public static final String KEY_MAGNET_CARD = "MagnetCard";
    /**
     * The "larger item counts" switch: the terminal then draws every stack size in AE2's large font
     * instead of the small one AE2 defaults to. Purely a display choice, so its default is off - the
     * other switches default to on because they enable a feature.
     */
    public static final String KEY_BIG_COUNTS = "BigCounts";
    /**
     * The "stash ambiguous uploads" switch: with it on, an encode whose provider search names nobody or
     * several providers puts the finished pattern into the pattern cache instead of leaving it in the
     * edit slot. Also a display-free choice, so it defaults to off as well.
     */
    public static final String KEY_STASH_AMBIGUOUS = "StashAmbiguous";
    /**
     * The "only a unique provider name" switch: with it on, an automatic upload only goes out when the
     * provider search names exactly one provider; with it off any provider the search matches takes the
     * pattern. A feature switch, so it defaults to on - which is also how the upload behaved before the
     * switch existed.
     */
    public static final String KEY_UPLOAD_UNIQUE_MATCH = "UploadUniqueMatch";

    /**
     * The "编程器电路始终为一" switch: with it on, multiplying a processing pattern scales everything
     * else and leaves the programmable circuit where it is - a circuit picks the recipe, it is not an
     * amount, so a ×2 that turned "1 circuit + 8 plates" into "2 circuits + 16 plates" made the
     * pattern ask for a circuit the machine does not have. A new feature switch, so it defaults to off.
     */
    public static final String KEY_CIRCUIT_ALWAYS_ONE = "CircuitAlwaysOne";

    /**
     * The middle-click "craft if missing" switch. With it on, an empty-handed middle-click on a block
     * the network cannot hand over draws up a crafting plan for exactly that block and opens the
     * confirmation screen - the plan still waits for the player to press start.
     */
    public static final String KEY_PICK_BLOCK_CRAFT = "PickBlockCraft";
    /**
     * The port's auto-fill switch: Ctrl+hammer asks the network for what it could not pull, and the
     * terminal's watch drops the crafted stacks into the grid (see {@code planMissingCrafts}). It is
     * toggled from the terminal's own toolbar button rather than from this screen.
     */
    public static final String KEY_CRAFT_IF_MISSING = "CraftIfMissing";
    /** WTLib's "Restock": stored and shown, still with no behaviour behind it. */
    public static final String KEY_RESTOCK = "Restock";
    /**
     * AE2 1.21's "Clear grid on close": the manual crafting matrix is emptied back into the network
     * (and from there to the player, see {@code clearManualCraftingGrid}) the moment the terminal
     * really closes. It reads the terminal stack rather than NEI's global config like the two
     * crafting-pin switches do, because closing the terminal is the one moment the switch is
     * consulted and the terminal is right there - the same reason the pick-block switches are stored
     * here. Off by default: this port has always kept the grid across sessions, so the switch has to
     * be the thing that opts into clearing, not the thing that opts out.
     */
    public static final String KEY_CLEAR_GRID_ON_CLOSE = "ClearGridOnClose";

    private final ItemStack terminal;

    public ContainerWcwtSettings(final InventoryPlayer ip, final ITerminalHost host) {
        super(ip, host);
        this.terminal = host instanceof final IGuiItemObject gui ? gui.getItemStack() : null;
    }

    public ItemStack getTerminalStack() {
        return this.terminal;
    }

    /** A switch that is on unless the terminal says otherwise - the feature switches. */
    private static boolean defaultsToOn(final String key) {
        return !KEY_BIG_COUNTS.equals(key) && !KEY_STASH_AMBIGUOUS.equals(key)
            && !KEY_CLEAR_GRID_ON_CLOSE.equals(key)
            && !KEY_CIRCUIT_ALWAYS_ONE.equals(key);
    }

    /** The switch's current state: features default to on, the display switches to off. */
    public boolean isEnabled(final String key) {
        return isOn(this.terminal, key);
    }

    /**
     * A switch's state on any terminal stack. The settings screen reads it from its own container,
     * the comprehensive terminal and its toolbar button from theirs - the switch is a property of the
     * terminal item, not of the screen that happens to be open.
     */
    public static boolean isOn(final ItemStack terminal, final String key) {
        if (terminal == null) {
            return defaultsToOn(key);
        }
        final NBTTagCompound tag = Platform.openNbtData(terminal);
        return tag.hasKey(key) ? tag.getBoolean(key) : defaultsToOn(key);
    }

    /**
     * Flips a switch and writes it back to the terminal item. Runs on both sides: the screen flips
     * its own copy for the immediate redraw and sends the packet, whose server half flips the real
     * stack - the same arrangement the card screens use, since 1.7.10 has no NBT resync here.
     */
    public void toggle(final String key) {
        toggleOn(this.getPlayerInv().player.worldObj.isRemote, this.terminal, key);
    }

    /** The flip itself, shared with the screens that write to a terminal stack of their own. */
    public static void toggleOn(final boolean clientSide, final ItemStack terminal, final String key) {
        if (terminal == null) {
            return;
        }
        final NBTTagCompound tag = Platform.openNbtData(terminal);
        final boolean now = !isOn(terminal, key);
        tag.setBoolean(key, now);
    }
}
