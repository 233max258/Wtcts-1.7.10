package com.asdflj.wtct.util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;

import javax.annotation.Nonnull;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;

import org.lwjgl.input.Mouse;

import com.asdflj.wtct.api.Constants;
import com.asdflj.wtct.api.WtctAPI;
import com.asdflj.wtct.client.gui.IGuiMonitorTerminal;
import com.asdflj.wtct.common.item.ItemComprehensiveWorkTerminal;
import com.asdflj.wtct.integration.Mods;
import com.asdflj.wtct.inventory.gui.GuiBridgeInvType;
import com.glodblock.github.client.gui.FCGuiTextField;
import com.glodblock.github.common.item.ItemFluidDrop;
import com.glodblock.github.common.item.ItemFluidPacket;
import com.mojang.authlib.GameProfile;

import appeng.api.AEApi;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.crafting.ICraftingJob;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IDisplayRepo;
import appeng.api.util.DimensionalCoord;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.widgets.MEGuiTextField;
import appeng.client.me.ItemRepo;
import appeng.container.implementations.ContainerCraftConfirm;
import appeng.core.AELog;
import appeng.core.worlddata.WorldData;
import appeng.crafting.v2.CraftingJobV2;
import appeng.integration.modules.NEI;
import appeng.items.tools.powered.ToolWirelessTerminal;
import appeng.me.cache.CraftingGridCache;
import appeng.util.Platform;
import codechicken.nei.recipe.StackInfo;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class Util {

    private static int AE_VERSION = -1;

    public static int getAEVersion() {
        if (AE_VERSION == -1) {
            Optional<ModContainer> mod = Loader.instance()
                .getActiveModList()
                .stream()
                .filter(
                    x -> x.getModId()
                        .equals("appliedenergistics2"))
                .findFirst();
            if (mod.isPresent()) {
                try {
                    AE_VERSION = Integer.parseInt(
                        mod.get()
                            .getVersion()
                            .split("-")[2]);
                } catch (Exception ignored) {
                    AE_VERSION = 0;
                }
            } else {
                AE_VERSION = 0;
            }
        }
        return AE_VERSION;
    }

    /**
     * Replacement for the removed {@code com.glodblock.github.util.Util.getPart} in AE2FC 1.5.106. Returns the part on
     * the given side of a cable bus tile, or null.
     */
    public static appeng.api.parts.IPart getPart(Object tile, ForgeDirection dir) {
        if (tile instanceof appeng.tile.networking.TileCableBus cableBus) {
            return cableBus.getPart(dir);
        }
        return null;
    }

    public static boolean replan(EntityPlayer player, appeng.container.implementations.ContainerCraftConfirm c){
        ICraftingJob job = Ae2Reflect.getJob(c);
        if(job instanceof CraftingJobV2 jobV2 && jobV2.isDone()){
            c.simulation = true;
            c.bytesUsed = 0;
        }else{
            return false;
        }
        Object target;
        target = c.getTarget();
        if (target instanceof final IGridHost gh) {
            final IGridNode gn = gh.getGridNode(ForgeDirection.UNKNOWN);

            if (gn == null) {
                return false;
            }

            final IGrid g = gn.getGrid();
            if (g == null || c.getItemToCraft() == null) {
                return false;
            }

            Future<ICraftingJob> futureJob = null;
            try {
                final ICraftingGrid cg = g.getCache(ICraftingGrid.class);
                if (cg instanceof CraftingGridCache cgc) {
                    futureJob = cgc.beginCraftingJob(
                        c.getWorld(),
                        g,
                        c.getActionSource(),
                        c.getItemToCraft(),
                        null);
                }

                if (player.openContainer instanceof final ContainerCraftConfirm ccc) {
                    ccc.setJob(futureJob);
                    ccc.detectAndSendChanges();
                }
                return true;
            } catch (final Throwable e) {
                if (futureJob != null) {
                    futureJob.cancel(true);
                }
                AELog.debug(e);
            }
        }
        return false;
    }

    public static boolean isSameDimensionalCoord(DimensionalCoord a, DimensionalCoord b) {
        return a != null && b != null && a.x == b.x && a.y == b.y && a.z == b.z && a.getDimension() == b.getDimension();
    }

    public static int getPlayerID(EntityPlayer player) {
        final GameProfile profile = player.getGameProfile();
        return WorldData.instance()
            .playerData()
            .getPlayerID(profile);
    }

    private static int randTickSeed = 0;

    /**
     * Looks up the comprehensive work terminal: the main inventory first, then the Baubles slots, always answering with
     * a bridge-encoded coordinate so the caller can open a terminal that sits in either place.
     */
    public static int findComprehensiveWorkTerminal(EntityPlayer player) {
        for (int x = 0; x < player.inventory.mainInventory.length; x++) {
            ItemStack item = player.inventory.mainInventory[x];
            if (item == null || item.getItem() == null) continue;
            if (item.getItem() instanceof ItemComprehensiveWorkTerminal) {
                return GuiBridgeInvType.encode(x, GuiBridgeInvType.PLAYER_INV);
            }
        }
        if (Mods.BAUBLES.isModLoaded()) {
            IInventory baubles = BaublesUtil.getBaublesInv(player);
            if (baubles != null) {
                for (int i = 0; i < baubles.getSizeInventory(); i++) {
                    ItemStack item = baubles.getStackInSlot(i);
                    if (item == null || item.getItem() == null) continue;
                    if (item.getItem() instanceof ItemComprehensiveWorkTerminal) {
                        return GuiBridgeInvType.encode(i, GuiBridgeInvType.PLAYER_BAUBLES);
                    }
                }
            }
        }
        return -1;
    }

    public static IGrid getWirelessGrid(EntityPlayer player) {
        for (int x = 0; x < player.inventory.mainInventory.length; x++) {
            ItemStack item = player.inventory.mainInventory[x];
            if (item == null || item.getItem() == null) continue;
            IGridNode node = getWirelessGridNode(item);
            if (node == null) continue;
            return node.getGrid();
        }
        return null;
    }

    public static String getModId(IAEItemStack item) {
        if (item.getItem() instanceof ItemFluidDrop) {
            FluidStack fs = ItemFluidDrop.getFluidStack(item.getItemStack());
            if (fs == null) return GameRegistry.findUniqueIdentifierFor(item.getItem()).modId;
            return getFluidModID(fs.getFluid());
        }
        return Platform.getModId(item);
    }

    public static String getFluidModID(Fluid fluid) {
        String name = FluidRegistry.getDefaultFluidName(fluid);
        try {
            return name.split(":")[0];
        } catch (Exception e) {
            return "";
        }
    }

    public static boolean isFluidPacket(ItemStack stack) {
        return stack != null && stack.getItem() instanceof ItemFluidPacket;
    }

    @Nonnull
    public static String getDisplayName(IAEItemStack item) {
        FluidStack fs = StackInfo.getFluid(item.getItemStack());
        if (fs != null) {
            return fs.getLocalizedName();
        }
        return Platform.getItemDisplayName(item);
    }

    public static IGridHost getWirelessGridHost(ItemStack is) {
        if (is.getItem() instanceof ToolWirelessTerminal) {
            String key = ((ToolWirelessTerminal) is.getItem()).getEncryptionKey(is);
            return (IGridHost) AEApi.instance()
                .registries()
                .locatable()
                .getLocatableBy(Long.parseLong(key));
        }
        return null;
    }

    public static IGridNode getWirelessGridNode(ItemStack is) {
        IGridHost host = getWirelessGridHost(is);
        if (host == null) return null;
        return host.getGridNode(ForgeDirection.UNKNOWN);
    }

    public static int findItemStack(EntityPlayer player, ItemStack itemStack) {
        for (int x = 0; x < player.inventory.mainInventory.length; x++) {
            ItemStack item = player.inventory.mainInventory[x];
            if (item == null) continue;
            if (Platform.isSameItemPrecise(item, itemStack)) {
                return x;
            }
        }
        return -1;
    }

    /**
     * Where the middle-click gesture's stack should land: the held hotbar slot when the hand is free,
     * otherwise the first empty main-inventory slot, or -1 when there is nowhere to put it.
     *
     * <p>
     * The held slot is preferred because that is where the vanilla gesture would have put the block, and
     * where the empty-hand-only version of this feature always put it - a player with a free hand sees
     * no change. The first-empty fallback is what lets the gesture work while carrying something: the
     * request is delivered by filling one slot, and the server only fills an empty one, so the hand can
     * no longer be the only answer.
     *
     * <p>
     * Vanilla's 36 slots only. With Backhand loaded the array is one longer, and that extra slot is the
     * player's offhand: an order must not quietly drop a stack into the hand the player is not looking
     * at.
     */
    public static int findReceivingSlot(EntityPlayer player) {
        final ItemStack[] main = player.inventory.mainInventory;
        final int usable = Math.min(main.length, 36);
        final int held = player.inventory.currentItem;
        if (held >= 0 && held < usable && main[held] == null) {
            return held;
        }
        for (int x = 0; x < usable; x++) {
            if (main[x] == null) {
                return x;
            }
        }
        return -1;
    }

    public static long genSingularityFreq() {
        long freq = (new Date()).getTime() * 100 + (randTickSeed) % 100;
        randTickSeed++;
        return freq;
    }

    public static FluidStack getFluidFromItem(ItemStack stack) {
        if (stack != null) {
            if (stack.getItem() instanceof IFluidContainerItem) {
                FluidStack fluid = ((IFluidContainerItem) stack.getItem()).getFluid(stack);
                if (fluid != null) {
                    FluidStack fluid0 = fluid.copy();
                    fluid0.amount *= stack.stackSize;
                    return fluid0;
                }
            }
            if (FluidContainerRegistry.isContainer(stack)) {
                FluidStack fluid = FluidContainerRegistry.getFluidForFilledItem(stack);
                if (fluid != null) {
                    FluidStack fluid0 = fluid.copy();
                    fluid0.amount *= stack.stackSize;
                    return fluid0;
                }
            }
        }
        return null;
    }

    public static List<Integer> getBackpackSlot(EntityPlayer player) {
        List<Integer> result = new ArrayList<>();
        for (int x = 0; x < player.inventory.mainInventory.length; x++) {
            ItemStack item = player.inventory.mainInventory[x];
            if (item == null || item.getItem() == null) continue;
            if (WtctAPI.instance()
                .isBackpackItem(item)) {
                result.add(x);
            }
        }
        return result;
    }

    @SideOnly(Side.CLIENT)
    public static int getLimitFPS() {
        Minecraft mc = Minecraft.getMinecraft();
        return mc.gameSettings.limitFramerate;
    }

    @SideOnly(Side.CLIENT)
    public static int getCurrentFPS() {
        try {
            Field field = Minecraft.class.getDeclaredField("debugFPS");
            field.setAccessible(true);
            return field.getInt(Minecraft.getMinecraft());
        } catch (Exception ignored) {}
        return 0;
    }

    public static IDisplayRepo getDisplayRepo(AEBaseGui gui) {
        if (gui instanceof IGuiMonitorTerminal gmt) {
            return gmt.getRepo();
        }
        return getDisplayRepo(gui, gui.getClass());
    }

    public static void setSearchFieldText(AEBaseGui gui, String text) {
        String displayName = NEI.searchField.getEscapedSearchText(text);
        if (gui instanceof IGuiMonitorTerminal gmt) {
            gmt.getSearchField()
                .setText(displayName);
            gmt.getRepo()
                .setSearchString(displayName);
            gmt.getRepo()
                .updateView();
        } else {
            IDisplayRepo repo = getDisplayRepo(gui);
            if (repo != null) {
                setSearchFieldText(gui, gui.getClass(), displayName);
                repo.setSearchString(displayName);
                repo.updateView();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void setSearchFieldText(AEBaseGui gui, Class<? extends AEBaseGui> clazz, String text) {
        try {
            if (clazz == AEBaseGui.class) {
                return;
            }
            for (Field f : clazz.getDeclaredFields()) {
                if (f.getType() == MEGuiTextField.class) {
                    f.setAccessible(true);
                    ((MEGuiTextField) f.get(gui)).setText(text);
                    return;
                } else if (f.getType() == FCGuiTextField.class) {
                    f.setAccessible(true);
                    ((FCGuiTextField) f.get(gui)).setText(text);
                    return;
                }
            }
            setSearchFieldText(gui, (Class<? extends AEBaseGui>) clazz.getSuperclass(), text);
        } catch (Exception ignored) {}
    }

    @SuppressWarnings("unchecked")
    private static IDisplayRepo getDisplayRepo(AEBaseGui gui, Class<? extends AEBaseGui> clazz) {
        try {
            if (clazz == AEBaseGui.class) {
                return null;
            }
            for (Field f : clazz.getDeclaredFields()) {
                if (f.getType() == IDisplayRepo.class || f.getType() == ItemRepo.class) {
                    f.setAccessible(true);
                    return (IDisplayRepo) f.get(gui);
                }
            }
            return getDisplayRepo(gui, (Class<? extends AEBaseGui>) clazz.getSuperclass());
        } catch (Exception ignored) {}
        return null;
    }

    public static class DimensionalCoordSide extends DimensionalCoord {

        private ForgeDirection side = ForgeDirection.UNKNOWN;
        private final String name;

        public DimensionalCoordSide(final int _x, final int _y, final int _z, final int _dim, ForgeDirection side,
            String name) {
            super(_x, _y, _z, _dim);
            this.side = side;
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public ForgeDirection getSide() {
            return this.side;
        }

        @Override
        public void writeToNBT(NBTTagCompound data) {
            data.setInteger(Constants.SIDE, this.side.ordinal());
            data.setString(Constants.NAME, this.name);
            super.writeToNBT(data);
        }

        public static DimensionalCoordSide readFromNBT(final NBTTagCompound data) {
            return new DimensionalCoordSide(
                data.getInteger("x"),
                data.getInteger("y"),
                data.getInteger("z"),
                data.getInteger("dim"),
                ForgeDirection.getOrientation(data.getInteger(Constants.SIDE)),
                data.getString(Constants.NAME));
        }

    }

    public static class MousePos {

        public final int x;
        public final int y;

        public MousePos() {
            Minecraft mc = Minecraft.getMinecraft();
            final ScaledResolution scaledresolution = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
            int i = scaledresolution.getScaledWidth();
            int j = scaledresolution.getScaledHeight();
            x = Mouse.getX() * i / mc.displayWidth;
            y = j - Mouse.getY() * j / mc.displayHeight - 1;
        }
    }
}
