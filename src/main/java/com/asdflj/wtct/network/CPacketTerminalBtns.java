package com.asdflj.wtct.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;

import com.asdflj.wtct.client.gui.container.ContainerCardConfig;
import com.asdflj.wtct.client.gui.container.ContainerCardMagnetConfig;
import com.asdflj.wtct.client.gui.container.ContainerComprehensiveWorkTerminal;
import com.asdflj.wtct.client.gui.container.ContainerWcwtSettings;
import com.asdflj.wtct.client.gui.container.ContainerWirelessDualInterfaceTerminal;
import com.asdflj.wtct.client.gui.container.IPatternContainer;
import com.asdflj.wtct.client.gui.container.widget.IWidgetPatternContainer;
import com.asdflj.wtct.common.item.card.CardTicker;
import com.asdflj.wtct.util.Util;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class CPacketTerminalBtns implements IMessage {

    private String name = "";
    private String value;
    private NBTTagCompound tag;

    public CPacketTerminalBtns(final String name, final boolean value) {
        this(name, value ? 1 : 0);
    }

    public CPacketTerminalBtns(final String name, final int value) {
        this(name, Integer.toString(value), null);
    }

    public CPacketTerminalBtns(final String name, final int value, final NBTTagCompound tag) {
        this(name, Integer.toString(value), tag);
    }

    public CPacketTerminalBtns(final String name, final String value, final NBTTagCompound tag) {
        this.name = name;
        this.value = value;
        this.tag = tag;
    }

    public CPacketTerminalBtns() {
        // NO-OP
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int leName = buf.readInt();
        int leVal = buf.readInt();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < leName; i++) {
            sb.append(buf.readChar());
        }
        name = sb.toString();
        sb = new StringBuilder();
        for (int i = 0; i < leVal; i++) {
            sb.append(buf.readChar());
        }
        value = sb.toString();
        if (buf.readBoolean()) {
            try {
                ByteArrayInputStream bytes = new ByteArrayInputStream(
                    buf.readBytes(buf.readableBytes())
                        .array());
                tag = CompressedStreamTools.readCompressed(bytes);
            } catch (IOException ignored) {

            }
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(name.length());
        buf.writeInt(value.length());
        for (int i = 0; i < name.length(); i++) {
            buf.writeChar(name.charAt(i));
        }
        for (int i = 0; i < value.length(); i++) {
            buf.writeChar(value.charAt(i));
        }
        buf.writeBoolean(tag != null);
        if (tag != null) {
            try {
                final ByteBuf data = Unpooled.buffer();
                final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                final DataOutputStream outputStream = new DataOutputStream(bytes);

                CompressedStreamTools.writeCompressed(this.tag, outputStream);
                data.writeBytes(bytes.toByteArray());
                data.capacity(data.readableBytes());
                buf.writeBytes(data);

            } catch (IOException ignored) {

            }
        }

    }

    public static class Handler implements IMessageHandler<CPacketTerminalBtns, IMessage> {

        @Override
        public IMessage onMessage(CPacketTerminalBtns message, MessageContext ctx) {
            String name = message.name;
            String value = message.value;
            NBTTagCompound tag = message.tag;
            final Container c = ctx.getServerHandler().playerEntity.openContainer;
            if (name.equals("CardConfig.SetSlots") && c instanceof ContainerCardConfig card) {
                // The marks the card's own screen sends: import 0/1, export a filter index. The card
                // item is the one the screen is editing, so this only has to write it back.
                if (tag != null && tag.hasKey("Slots")) {
                    card.setSelectedSlots(tag.getIntArray("Slots"));
                }
                return null;
            }
            if (name.equals("CardConfig.CycleFuzzy") && c instanceof ContainerCardConfig card) {
                card.cycleFuzzyMode();
                return null;
            }
            if (name.equals("CardConfig.CycleMagnet")) {
                if (Boolean.getBoolean("wtct.debug.magnet")) {
                    System.out.println("[wtct-magnet] server received CycleMagnet, openContainer=" + c);
                }
                // Sent by the header button of the comprehensive terminal and by the magnet card's own
                // screen, so both containers have to be accepted.
                if (c instanceof ContainerComprehensiveWorkTerminal cwt) {
                    cwt.cycleMagnetMode();
                } else if (c instanceof ContainerCardMagnetConfig magnet) {
                    magnet.cycleMagnetMode();
                } else if (Boolean.getBoolean("wtct.debug.magnet")) {
                    System.out.println("[wtct-magnet] CycleMagnet arrived but openContainer is " + c);
                }
                return null;
            }
            if (name.equals("CardConfig.MagnetAction") && c instanceof ContainerCardMagnetConfig magnet) {
                // The magnet screen's five buttons: the two include/exclude toggles, copying either
                // filter onto the other, and swapping them - WTLib's own set.
                switch (Integer.parseInt(value)) {
                    case 0 -> magnet.togglePickupMode();
                    case 1 -> magnet.toggleInsertMode();
                    case 2 -> magnet.copyUp();
                    case 3 -> magnet.copyDown();
                    case 4 -> magnet.switchFilters();
                    default -> {}
                }
                return null;
            }
            if (name.equals("CardConfig.CyclePickerAmount") && c instanceof ContainerComprehensiveWorkTerminal cwt) {
                cwt.cycleBlockPickerAmount();
                return null;
            }
            if (name.equals("CardConfig.SetPickerAmount") && c instanceof ContainerComprehensiveWorkTerminal cwt) {
                // The picker button's amount entry (the addon's BlockPickerAmountScreen).
                if (tag != null) {
                    cwt.setBlockPickerAmount(tag.getLong("Amount"));
                }
                return null;
            }
            if (name.equals("CardConfig.PickBlock") && tag != null) {
                // Sent by the pick-block key: the server re-checks reach and re-derives the block, so
                // the client cannot ask for anything it could not have picked up itself.
                CardTicker.pickBlock(
                    ctx.getServerHandler().playerEntity,
                    tag.getInteger("x"),
                    tag.getInteger("y"),
                    tag.getInteger("z"));
                return null;
            }
            if (name.equals("CardConfig.SettingToggle")) {
                // The settings screen's switches: the screen flipped its own copy for the redraw,
                // this flips the real terminal stack.
                if (tag != null && tag.hasKey("Key")) {
                    // The settings screen has its own container for this; the comprehensive terminal
                    // carries the same switch now that its toolbar has the button too.
                    final ItemStack terminal = c instanceof ContainerWcwtSettings settings ? settings.getTerminalStack()
                        : c instanceof ContainerComprehensiveWorkTerminal wcwt ? wcwt.getTerminalStack() : null;
                    ContainerWcwtSettings.toggleOn(false, terminal, tag.getString("Key"));
                }
                return null;
            }
            if (name.equals("PatternAmount.Set") && c instanceof ContainerComprehensiveWorkTerminal cwt) {
                // The middle-click amount entry: the cell is named by the value, the amount rides in
                // the tag (0 clears the cell).
                if (tag != null) {
                    cwt.setCellAmount(Integer.parseInt(value), tag.getLong("Amount"));
                }
                return null;
            }
            if (name.startsWith("PatternTerminal.") && c instanceof IWidgetPatternContainer wpc) {
                IPatternContainer cpt = wpc.getContainer();
                switch (name) {
                    case "PatternTerminal.Encode" -> {
                        switch (value) {
                            case "0" -> cpt.encode();
                            case "1" -> cpt.encodeAndMoveToInventory();
                            case "3" -> cpt.encodeAllItemAndMoveToInventory();
                            // Encode and hand the result to the pattern management area in one go; the
                            // payload tag carries the selected provider and the search text.
                            case "4" -> {
                                if (c instanceof ContainerComprehensiveWorkTerminal cwt) {
                                    cwt.encodeAndUpload(
                                        tag != null && tag.getBoolean("upload"),
                                        tag == null ? -1 : tag.getLong("provider"),
                                        tag == null ? "" : tag.getString("search"));
                                } else {
                                    cpt.encode();
                                }
                            }
                        }
                    }
                    case "PatternTerminal.StashToCache" -> {
                        // The finished pattern in the edit slot goes into the pattern cache instead of
                        // being uploaded - the terminal's fallback for an ambiguous provider match.
                        if (c instanceof ContainerComprehensiveWorkTerminal cwt) {
                            cwt.stashEditSlotPatternToCache();
                        }
                    }
                    case "PatternTerminal.CraftMode" -> cpt.getPatternTerminal()
                        .setCraftingRecipe(value.equals("1"));
                    case "PatternTerminal.Combine" -> cpt.getPatternTerminal()
                        .setCombineMode(value.equals("1"));
                    case "PatternTerminal.Clear" -> cpt.clear();
                    case "PatternTerminal.ActivePage" -> cpt.getPatternTerminal()
                        .setActivePage(Integer.parseInt(value));
                    case "PatternTerminal.Double" -> cpt.doubleStacks(Integer.parseInt(value));
                    case "PatternTerminal.Multiply" -> cpt.multiplyStacks(Integer.parseInt(value));
                    case "PatternTerminal.SwapOutputs" -> cpt.rotateOutputs();
                    // These two sit under "PatternTerminal." on purpose: the enclosing branch only opens
                    // for that prefix, so the names they used to carry ("PatternCache.*") never matched
                    // and the batch switches did nothing at all.
                    case "PatternTerminal.ItemSubstitution" -> {
                        if (c instanceof ContainerComprehensiveWorkTerminal cwt) {
                            cwt.setCachedSubstitutions(Integer.parseInt(value) == 1, null);
                        }
                    }
                    case "PatternTerminal.BeSubstitution" -> {
                        if (c instanceof ContainerComprehensiveWorkTerminal cwt) {
                            cwt.setCachedSubstitutions(null, Integer.parseInt(value) == 1);
                        }
                    }
                    case "PatternTerminal.Substitute" -> cpt.getPatternTerminal()
                        .setSubstitution(value.equals("1"));
                    case "PatternTerminal.Prioritize" -> {
                        switch (value) {
                            case "0", "1" -> cpt.getPatternTerminal()
                                .setPrioritization(value.equals("1"));
                            case "2" -> cpt.getPatternTerminal()
                                .sortCraftingItems();
                        }
                    }
                    case "PatternTerminal.Invert" -> cpt.getPatternTerminal()
                        .setInverted(value.equals("1"));
                    case "PatternTerminal.beSubstitute" -> cpt.getPatternTerminal()
                        .setBeSubstitute(value.equals("1"));
                    case "PatternTerminal.MergeMaterials" -> cpt.getPatternTerminal()
                        .setMergeMaterials(value.equals("1"));
                }
                cpt.getPatternTerminal()
                    .saveSettings();
            }
            if (name.startsWith("InterfaceTerminal.") && c instanceof ContainerWirelessDualInterfaceTerminal ciw) {
                switch (name) {
                    case "InterfaceTerminal.Double" -> ciw.doubleStacks(Integer.parseInt(value), tag);
                    case "InterfaceTerminal.SetStick" -> ciw.setStick(tag);
                    case "InterfaceTerminal.PlacePattern" -> ciw.PlacePattern(Integer.parseInt(value), tag);
                }

            }
            if (name.startsWith("PatternManagement.") && c instanceof ContainerComprehensiveWorkTerminal cwt) {
                final long id = tag == null ? -1 : tag.getLong("provider");
                switch (name) {
                    case "PatternManagement.SlotAction" -> cwt
                        .providerSlotAction(id, Integer.parseInt(value), tag != null && tag.getBoolean("quick"));
                    case "PatternManagement.UploadCache" -> cwt.uploadCacheToProvider(id);
                    // The folded cell of a slot row: the provider's empty slots collapsed into one, so
                    // this is "put the pattern I am carrying into the first slot that is free".
                    case "PatternManagement.InsertFirstFree" -> cwt.insertIntoFirstFreeProviderSlot(id);
                    case "PatternManagement.OpenProviderUi" -> cwt.openProviderMachineUi(id);
                    case "PatternManagement.UploadPattern" -> cwt
                        .uploadEditSlotPattern(id, tag == null ? "" : tag.getString("search"));
                    case "PatternManagement.RefillBlank" -> cwt.refillBlankPatternSlot();
                    // The management area's four switches: written through to the terminal item, so the
                    // setting survives the GUI closing and the game restarting.
                    case "PatternManagement.Upload" -> cwt.setMgmtUpload(value.equals("1"));
                    case "PatternManagement.DisplayMode" -> cwt.setMgmtDisplayMode(Integer.parseInt(value));
                    case "PatternManagement.ShowSlots" -> cwt.setMgmtShowSlots(value.equals("1"));
                    case "PatternManagement.SearchMode" -> cwt.setMgmtSearchMode(Integer.parseInt(value));
                    case "PatternManagement.Expand" -> cwt.setMgmtExpanded(value.equals("1"));
                }
            }
            if (name.startsWith("CraftTerminal.") && c instanceof ContainerComprehensiveWorkTerminal cwt2) {
                switch (name) {
                    case "CraftTerminal.Clear" -> cwt2.clearManualCraftingGrid();
                    case "CraftTerminal.Stash" -> cwt2.stashManualCraftingGrid();
                }
            }
            if (name.startsWith("GuiCraftConfirm.replan")
                && c instanceof appeng.container.implementations.ContainerCraftConfirm ccc) {
                Util.replan(ctx.getServerHandler().playerEntity, ccc);
            }
            return null;
        }
    }
}
