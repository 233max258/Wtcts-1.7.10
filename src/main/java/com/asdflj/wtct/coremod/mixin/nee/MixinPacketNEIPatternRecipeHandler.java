package com.asdflj.wtct.coremod.mixin.nee;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.asdflj.wtct.client.gui.container.ContainerComprehensiveWorkTerminal;
import com.asdflj.wtct.loader.BRLoader;
import com.asdflj.wtct.nei.object.OrderStack;
import com.github.vfyjxf.nee.network.packet.PacketNEIPatternRecipe;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

/**
 * The "?" button on a multiblock structure page fills the pattern terminal with that structure's blocks.
 * BlockRenderer6343 does not route it through NEI's overlay transfer: it builds NEE's
 * {@code PacketNEIPatternRecipe} and sends it straight to the server.
 *
 * <p>
 * NEE's handler then gates on {@code GuiUtils.isPatternContainer}, which is
 * {@code container instanceof ContainerPatternTerm} - AE2's own container, class for class - and casts to
 * it before reading a single field. Any other container that writes patterns, this mod's comprehensive work
 * terminal included, therefore had its structure silently dropped: the button looked dead, with nothing in
 * the log to show for it.
 *
 * <p>
 * Take the packet here instead, before that gate, and hand it to the same code the terminal's own NEI
 * transfer uses. Cancelling the original method is also what keeps NEE's cast from running, so the
 * structure is written exactly once.
 */
@Mixin(PacketNEIPatternRecipe.Handler.class)
public abstract class MixinPacketNEIPatternRecipeHandler {

    /** Keys BlockRenderer and NEE's own handler both pack their matrices under. */
    private static final String wtct$inputKey = "#";
    private static final String wtct$outputKey = "Outputs";
    /** A structure's ingredient list is far shorter; this only guards against a malformed packet. */
    private static final int wtct$maxSlots = 256;

    @Inject(
        method = "onMessage(Lcom/github/vfyjxf/nee/network/packet/PacketNEIPatternRecipe;Lcpw/mods/fml/common/network/simpleimpl/MessageContext;)Lcpw/mods/fml/common/network/simpleimpl/IMessage;",
        at = @At("HEAD"),
        cancellable = true,
        remap = false)
    private void wtct$installStructure(PacketNEIPatternRecipe message, MessageContext ctx,
        CallbackInfoReturnable<IMessage> cir) {
        EntityPlayerMP player = ctx.getServerHandler().playerEntity;
        if (player == null) return;
        Container container = player.openContainer;
        if (!(container instanceof ContainerComprehensiveWorkTerminal cwt)) return;
        BRLoader.installStructure(
            cwt,
            wtct$readMatrix(wtct$payload(message, "input"), wtct$inputKey),
            wtct$readMatrix(wtct$payload(message, "output"), wtct$outputKey));
        cir.setReturnValue(null);
    }

    /**
     * The packet keeps its two payload compounds package-private, so they are read reflectively rather
     * than shadowed: a mixin on the handler cannot shadow fields of the class it is nested in.
     */
    private static NBTTagCompound wtct$payload(PacketNEIPatternRecipe message, String name) {
        try {
            Field field = PacketNEIPatternRecipe.class.getDeclaredField(name);
            field.setAccessible(true);
            return (NBTTagCompound) field.get(message);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static List<OrderStack<?>> wtct$readMatrix(NBTTagCompound payload, String key) {
        List<OrderStack<?>> stacks = new ArrayList<>();
        if (payload == null) return stacks;
        for (int i = 0; i < wtct$maxSlots && payload.hasKey(key + i); i++) {
            ItemStack stack = wtct$readStack(payload.getCompoundTag(key + i));
            if (stack != null) stacks.add(new OrderStack<>(stack, i));
        }
        return stacks;
    }

    /**
     * A structure holds stacks of dozens of the same casing, which does not fit in the byte the vanilla
     * "Count" tag uses - BlockRenderer writes the real size into an int tag of the same name, and reading
     * it back the vanilla way would land every one of those stacks in the pattern as a single block.
     */
    private static ItemStack wtct$readStack(NBTTagCompound entry) {
        ItemStack stack = ItemStack.loadItemStackFromNBT(entry);
        if (stack == null) return null;
        int count = entry.getInteger("Count");
        stack.stackSize = count > 0 ? count : 1;
        return stack;
    }
}
