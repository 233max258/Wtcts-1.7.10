package com.asdflj.wtct.inventory.gui;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.asdflj.wtct.api.WirelessObject;
import com.asdflj.wtct.client.gui.GuiCardConfig;
import com.asdflj.wtct.client.gui.GuiCardMagnetConfig;
import com.asdflj.wtct.client.gui.GuiCellLink;
import com.asdflj.wtct.client.gui.GuiComprehensiveCraftingStatus;
import com.asdflj.wtct.client.gui.GuiComprehensiveWorkTerminal;
import com.asdflj.wtct.client.gui.GuiCraftAmount;
import com.asdflj.wtct.client.gui.GuiCraftConfirm;
import com.asdflj.wtct.client.gui.GuiCraftingStatus;
import com.asdflj.wtct.client.gui.GuiExIOBus;
import com.asdflj.wtct.client.gui.GuiExStorageBus;
import com.asdflj.wtct.client.gui.GuiNetworkHubCore;
import com.asdflj.wtct.client.gui.GuiPatternValueAmount;
import com.asdflj.wtct.client.gui.GuiPatternValueName;
import com.asdflj.wtct.client.gui.GuiRenamer;
import com.asdflj.wtct.client.gui.GuiTerminalMenu;
import com.asdflj.wtct.client.gui.GuiWcwtSettings;
import com.asdflj.wtct.client.gui.GuiWcwtTrash;
import com.asdflj.wtct.client.gui.container.ContainerCardConfig;
import com.asdflj.wtct.client.gui.container.ContainerCardMagnetConfig;
import com.asdflj.wtct.client.gui.container.ContainerCellLink;
import com.asdflj.wtct.client.gui.container.ContainerComprehensiveWorkTerminal;
import com.asdflj.wtct.client.gui.container.ContainerCraftConfirm;
import com.asdflj.wtct.client.gui.container.ContainerExIOBus;
import com.asdflj.wtct.client.gui.container.ContainerExStorageBus;
import com.asdflj.wtct.client.gui.container.ContainerNetworkHub;
import com.asdflj.wtct.client.gui.container.ContainerPatternValueAmount;
import com.asdflj.wtct.client.gui.container.ContainerPatternValueName;
import com.asdflj.wtct.client.gui.container.ContainerRenamer;
import com.asdflj.wtct.client.gui.container.ContainerTerminalMenu;
import com.asdflj.wtct.client.gui.container.ContainerWcwtSettings;
import com.asdflj.wtct.client.gui.container.ContainerWcwtTrash;
import com.asdflj.wtct.common.item.ItemComprehensiveWorkTerminal;
import com.asdflj.wtct.common.item.card.ItemWcwtUpgradeCard;
import com.asdflj.wtct.common.parts.PartExExportBus;
import com.asdflj.wtct.common.parts.PartExImportBus;
import com.asdflj.wtct.common.parts.PartExStorageBus;
import com.asdflj.wtct.common.parts.THPart;
import com.asdflj.wtct.common.tile.TileNetworkHub;
import com.asdflj.wtct.inventory.ItemCellLinkInventory;
import com.asdflj.wtct.inventory.item.WirelessDualInterfaceTerminalInventory;
import com.google.common.collect.ImmutableList;

import appeng.api.exceptions.AppEngException;
import appeng.api.storage.ITerminalHost;
import appeng.container.AEBaseContainer;
import appeng.container.ContainerOpenContext;
import appeng.container.implementations.ContainerCraftAmount;
import appeng.container.implementations.ContainerCraftingStatus;
import appeng.core.localization.PlayerMessages;

public enum GuiType {

    TERMINAL_MENU(new NullGuiFactory() {

        @Override
        public Object createServerGui(EntityPlayer player, World world, int x, int y, int z, ForgeDirection face) {
            return new ContainerTerminalMenu();
        }

        @Override
        public Object createClientGui(EntityPlayer player, World world, int x, int y, int z, ForgeDirection face) {
            return new GuiTerminalMenu();
        }
    }),
    COMPREHENSIVE_WORK_TERMINAL(new ItemGuiFactory<>(ITerminalHost.class) {

        @Override
        protected Object createServerGui(EntityPlayer player, ITerminalHost inv) {
            return new ContainerComprehensiveWorkTerminal(player.inventory, inv);
        }

        @Override
        protected Object createClientGui(EntityPlayer player, ITerminalHost inv) {
            return new GuiComprehensiveWorkTerminal(player.inventory, inv);
        }
    }),
    CRAFTING_CONFIRM(new PartGuiFactory<>(THPart.class) {

        @Override
        protected Object createServerGui(EntityPlayer player, THPart inv) {
            return new ContainerCraftConfirm(player.inventory, inv);
        }

        @Override
        protected Object createClientGui(EntityPlayer player, THPart inv) {
            return new GuiCraftConfirm(player.inventory, inv);
        }
    }),
    CRAFTING_CONFIRM_ITEM(new ItemGuiFactory<>(ITerminalHost.class) {

        @Override
        protected Object createServerGui(EntityPlayer player, ITerminalHost inv) {
            return new ContainerCraftConfirm(player.inventory, inv);
        }

        @Override
        protected Object createClientGui(EntityPlayer player, ITerminalHost inv) {
            return new GuiCraftConfirm(player.inventory, inv);
        }
    }),
    RENAMER(new ItemGuiFactory<>(ITerminalHost.class) {

        @Override
        protected Object createServerGui(EntityPlayer player, ITerminalHost inv) {
            return new ContainerRenamer(player.inventory, inv);
        }

        @Override
        protected Object createClientGui(EntityPlayer player, ITerminalHost inv) {
            return new GuiRenamer(player.inventory, inv);
        }
    }),
    CRAFTING_STATUS(new PartGuiFactory<>(ITerminalHost.class) {

        @Override
        protected Object createServerGui(EntityPlayer player, ITerminalHost inv) {
            return new ContainerCraftingStatus(player.inventory, inv);
        }

        @Override
        protected Object createClientGui(EntityPlayer player, ITerminalHost inv) {
            return new GuiCraftingStatus(player.inventory, inv);
        }
    }),
    CRAFTING_STATUS_ITEM(new ItemGuiFactory<>(ITerminalHost.class) {

        @Override
        protected Object createServerGui(EntityPlayer player, ITerminalHost inv) {
            return new ContainerCraftingStatus(player.inventory, inv);
        }

        @Override
        protected Object createClientGui(EntityPlayer player, ITerminalHost inv) {
            return new GuiCraftingStatus(player.inventory, inv);
        }
    }),
    PATTERN_VALUE_SET(new PartGuiFactory<>(ITerminalHost.class) {

        @Override
        protected Object createServerGui(EntityPlayer player, ITerminalHost inv) {
            return new ContainerPatternValueAmount(player.inventory, inv);
        }

        @Override
        protected Object createClientGui(EntityPlayer player, ITerminalHost inv) {
            return new GuiPatternValueAmount(player.inventory, inv);
        }
    }),
    PATTERN_VALUE_SET_ITEM(new ItemGuiFactory<>(ITerminalHost.class) {

        @Override
        protected Object createServerGui(EntityPlayer player, ITerminalHost inv) {
            return new ContainerPatternValueAmount(player.inventory, inv);
        }

        @Override
        protected Object createClientGui(EntityPlayer player, ITerminalHost inv) {
            return new GuiPatternValueAmount(player.inventory, inv);
        }
    }),
    PATTERN_NAME_SET(new PartGuiFactory<>(ITerminalHost.class) {

        @Override
        protected Object createServerGui(EntityPlayer player, ITerminalHost inv) {
            return new ContainerPatternValueName(player.inventory, inv);
        }

        @Override
        protected Object createClientGui(EntityPlayer player, ITerminalHost inv) {
            return new GuiPatternValueName(player.inventory, inv);
        }
    }),
    PATTERN_NAME_SET_ITEM(new ItemGuiFactory<>(ITerminalHost.class) {

        @Override
        protected Object createServerGui(EntityPlayer player, ITerminalHost inv) {
            return new ContainerPatternValueName(player.inventory, inv);
        }

        @Override
        protected Object createClientGui(EntityPlayer player, ITerminalHost inv) {
            return new GuiPatternValueName(player.inventory, inv);
        }
    }),

    CRAFTING_AMOUNT(new PartGuiFactory<>(ITerminalHost.class) {

        @Override
        protected Object createServerGui(EntityPlayer player, ITerminalHost inv) {
            return new ContainerCraftAmount(player.inventory, inv);
        }

        @Override
        protected Object createClientGui(EntityPlayer player, ITerminalHost inv) {
            return new GuiCraftAmount(player.inventory, inv);
        }
    }),
    CRAFTING_AMOUNT_ITEM(new ItemGuiFactory<>(ITerminalHost.class) {

        @Override
        protected Object createServerGui(EntityPlayer player, ITerminalHost inv) {
            return new ContainerCraftAmount(player.inventory, inv);
        }

        @Override
        protected Object createClientGui(EntityPlayer player, ITerminalHost inv) {
            return new GuiCraftAmount(player.inventory, inv);
        }
    }),
    CELL_LINK(new ItemGuiFactory<>(ItemCellLinkInventory.class) {

        @Override
        protected Object createServerGui(EntityPlayer player, ItemCellLinkInventory inv) {
            return new ContainerCellLink(player.inventory, inv);
        }

        @Override
        protected Object createClientGui(EntityPlayer player, ItemCellLinkInventory inv) {
            return new GuiCellLink(player.inventory, inv);
        }
    }),
    /**
     * Crafting status as opened from the comprehensive work terminal. Same container and same screen as
     * {@link #CRAFTING_STATUS_ITEM}, but a type of its own so the "back to terminal" tab knows where to
     * return: the comprehensive and the wireless dual-interface terminal host their inventory through the
     * same class, so the host alone cannot tell them apart.
     *
     * <p>
     * New value appended last on purpose - the ordinal is the wire format of
     * {@code CPacketSwitchGuis}, so inserting in the middle would shift every later type.
     */
    CRAFTING_STATUS_ITEM_COMPREHENSIVE(new ItemGuiFactory<>(ITerminalHost.class) {

        @Override
        protected Object createServerGui(EntityPlayer player, ITerminalHost inv) {
            return new ContainerCraftingStatus(player.inventory, inv);
        }

        @Override
        protected Object createClientGui(EntityPlayer player, ITerminalHost inv) {
            return new GuiComprehensiveCraftingStatus(player.inventory, inv);
        }
    }),
    /**
     * The import and export cards' configuration screens. One container and one screen serve both,
     * told apart by the card kind - the two differ only in what a mark means and in which background
     * art is blitted.
     */
    CARD_IMPORT(new ItemGuiFactory<>(ITerminalHost.class) {

        @Override
        protected Object createServerGui(EntityPlayer player, ITerminalHost inv) {
            return new ContainerCardConfig(player.inventory, inv, ItemWcwtUpgradeCard.Kind.IMPORT);
        }

        @Override
        protected Object createClientGui(EntityPlayer player, ITerminalHost inv) {
            return new GuiCardConfig(player.inventory, inv, ItemWcwtUpgradeCard.Kind.IMPORT);
        }
    }),
    CARD_EXPORT(new ItemGuiFactory<>(ITerminalHost.class) {

        @Override
        protected Object createServerGui(EntityPlayer player, ITerminalHost inv) {
            return new ContainerCardConfig(player.inventory, inv, ItemWcwtUpgradeCard.Kind.EXPORT);
        }

        @Override
        protected Object createClientGui(EntityPlayer player, ITerminalHost inv) {
            return new GuiCardConfig(player.inventory, inv, ItemWcwtUpgradeCard.Kind.EXPORT);
        }
    }),
    CARD_MAGNET(new ItemGuiFactory<>(ITerminalHost.class) {

        @Override
        protected Object createServerGui(EntityPlayer player, ITerminalHost inv) {
            return new ContainerCardMagnetConfig(player.inventory, inv);
        }

        @Override
        protected Object createClientGui(EntityPlayer player, ITerminalHost inv) {
            return new GuiCardMagnetConfig(player.inventory, inv);
        }
    }),
    WCWT_TRASH(new ItemGuiFactory<>(ITerminalHost.class) {

        @Override
        protected Object createServerGui(EntityPlayer player, ITerminalHost inv) {
            return new ContainerWcwtTrash(player.inventory, inv);
        }

        @Override
        protected Object createClientGui(EntityPlayer player, ITerminalHost inv) {
            return new GuiWcwtTrash(player.inventory, inv);
        }
    }),
    WCWT_SETTINGS(new ItemGuiFactory<>(ITerminalHost.class) {

        @Override
        protected Object createServerGui(EntityPlayer player, ITerminalHost inv) {
            return new ContainerWcwtSettings(player.inventory, inv);
        }

        @Override
        protected Object createClientGui(EntityPlayer player, ITerminalHost inv) {
            return new GuiWcwtSettings(player.inventory, inv);
        }
    }),
    /**
     * Bridge counterpart of {@link #COMPREHENSIVE_WORK_TERMINAL}. Same container and screen, but the terminal
     * is located through a bridge-encoded coordinate so the keybinding can open one that lives in the player
     * inventory or in a Baubles slot - {@code ItemGuiFactory} can only read {@code player.inventory}.
     *
     * <p>
     * New value appended last on purpose - the ordinal is the wire format of {@code CPacketSwitchGuis}, so
     * inserting in the middle would shift every later type.
     */
    COMPREHENSIVE_WORK_TERMINAL_BRIDGE(new ItemGuiBridge<>(ItemComprehensiveWorkTerminal.class) {

        @Override
        protected Object createServerGui(EntityPlayer player, ItemComprehensiveWorkTerminal inv, ItemStack item) {
            final int x = this.bridgeSlot;
            final WirelessDualInterfaceTerminalInventory term;
            try {
                term = create(player, item, x);
            } catch (AppEngException e) {
                // The terminal's own verdict - already localized, and not always a range problem.
                player.addChatMessage(new ChatComponentText(e.getMessage()));
                return null;
            } catch (Exception e) {
                // Anything else is a defect, not a range problem; do not mislabel it, and leave a trace.
                e.printStackTrace();
                player.addChatMessage(PlayerMessages.OutOfRange.get());
                return null;
            }
            if (term == null) {
                return null;
            }
            AEBaseContainer bc = new ContainerComprehensiveWorkTerminal(player.inventory, term);
            bc.setOpenContext(new ContainerOpenContext(term));
            bc.getOpenContext()
                .setWorld(player.worldObj);
            bc.getOpenContext()
                .setX(x);
            bc.getOpenContext()
                .setY(0);
            bc.getOpenContext()
                .setZ(0);
            bc.getOpenContext()
                .setSide(ForgeDirection.UNKNOWN);
            return bc;
        }

        @Override
        protected Object createClientGui(EntityPlayer player, ItemComprehensiveWorkTerminal inv, ItemStack item) {
            if (item == null) return null;
            try {
                final WirelessDualInterfaceTerminalInventory term = create(player, item, this.bridgeSlot);
                return term == null ? null : new GuiComprehensiveWorkTerminal(player.inventory, term);
            } catch (AppEngException e) {
                // The range/link verdict is the server's to report; the client just gets no screen.
                return null;
            } catch (ReflectiveOperationException e) {
                // The host inventory could not be constructed - a defect; the client just gets no screen.
                e.printStackTrace();
                return null;
            }
        }

        @Nullable
        private WirelessDualInterfaceTerminalInventory create(EntityPlayer player, ItemStack item, int slot)
            throws AppEngException, ReflectiveOperationException {
            if (item == null) return null;
            return (WirelessDualInterfaceTerminalInventory) new WirelessObject(
                item,
                player.worldObj,
                slot,
                0,
                0,
                player).getInventory(WirelessDualInterfaceTerminalInventory.class);
        }
    }),
    /**
     * The ME network hub's screens. New value appended last on purpose - the ordinal is the wire format of
     * {@code CPacketSwitchGuis}, so inserting in the middle would shift every later type.
     */
    NETWORK_HUB(new TileGuiFactory<>(TileNetworkHub.class) {

        @Override
        protected Object createServerGui(EntityPlayer player, TileNetworkHub inv) {
            return new ContainerNetworkHub(player.inventory, inv);
        }

        @Override
        protected Object createClientGui(EntityPlayer player, TileNetworkHub inv) {
            return new GuiNetworkHubCore(player.inventory, inv);
        }
    }),
    /**
     * The ME extended import and export buses. One container and one screen serve both, told apart by the part's own
     * behaviour - the two differ only in which way the marked stacks travel and in whether a scheduling mode exists.
     *
     * <p>
     * New value appended last on purpose - the ordinal is the wire format of {@code CPacketSwitchGuis}, so inserting in
     * the middle would shift every later type.
     */
    EX_IMPORT_BUS(new PartGuiFactory<>(PartExImportBus.class) {

        @Override
        protected Object createServerGui(EntityPlayer player, PartExImportBus inv) {
            return new ContainerExIOBus(player.inventory, inv);
        }

        @Override
        protected Object createClientGui(EntityPlayer player, PartExImportBus inv) {
            return new GuiExIOBus(player.inventory, inv);
        }
    }),
    EX_EXPORT_BUS(new PartGuiFactory<>(PartExExportBus.class) {

        @Override
        protected Object createServerGui(EntityPlayer player, PartExExportBus inv) {
            return new ContainerExIOBus(player.inventory, inv);
        }

        @Override
        protected Object createClientGui(EntityPlayer player, PartExExportBus inv) {
            return new GuiExIOBus(player.inventory, inv);
        }
    }),
    EX_STORAGE_BUS(new PartGuiFactory<>(PartExStorageBus.class) {

        @Override
        protected Object createServerGui(EntityPlayer player, PartExStorageBus inv) {
            return new ContainerExStorageBus(player.inventory, inv);
        }

        @Override
        protected Object createClientGui(EntityPlayer player, PartExStorageBus inv) {
            return new GuiExStorageBus(player.inventory, inv);
        }
    });

    /**
     * Builds the wireless dual-interface terminal inventory that owns {@code item}, or {@code null} when the
     * terminal is out of range or not linked to a network. The bridge-encoded {@code slot} must be handed to
     * {@link WirelessObject}: the returned inventory reports it back as its own slot, and the terminal NBT is
     * written to that slot whenever the terminal changes.
     */
    @Nullable
    private static WirelessDualInterfaceTerminalInventory resolve(EntityPlayer player, ItemStack item, int slot) {
        if (item == null) return null;
        try {
            return (WirelessDualInterfaceTerminalInventory) new WirelessObject(
                item,
                player.worldObj,
                slot,
                0,
                0,
                player).getInventory(WirelessDualInterfaceTerminalInventory.class);
        } catch (Exception e) {
            return null;
        }
    }

    public static final List<GuiType> VALUES = ImmutableList.copyOf(values());

    @Nullable
    public static GuiType getByOrdinal(int ordinal) {
        return ordinal < 0 || ordinal >= VALUES.size() ? null : VALUES.get(ordinal);
    }

    public final IGuiFactory guiFactory;

    GuiType(IGuiFactory guiFactory) {
        this.guiFactory = guiFactory;
    }

    /**
     * Returns the bridge-factory counterpart of this gui type, or {@code null} when this type has none.
     *
     * <p>
     * Item-hosted guis come in two flavours: the plain {@code ItemGuiFactory} one, which can only look the
     * item up by a raw player-inventory slot, and the {@code ItemGuiBridge} one, which understands a
     * bridge-encoded coordinate that may address a Baubles slot. Whenever a coordinate is bridge-encoded
     * the plain factory has to be replaced by its bridge counterpart, otherwise the raw lookup throws.
     */
    @Nullable
    public GuiType bridgeVariant() {
        switch (this) {
            case COMPREHENSIVE_WORK_TERMINAL -> {
                return COMPREHENSIVE_WORK_TERMINAL_BRIDGE;
            }
            default -> {
                return null;
            }
        }
    }
}
