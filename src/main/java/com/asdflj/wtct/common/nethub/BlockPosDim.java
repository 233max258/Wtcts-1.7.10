package com.asdflj.wtct.common.nethub;

import java.util.Objects;

import net.minecraft.nbt.NBTTagCompound;

import com.asdflj.wtct.util.BlockPos;

/**
 * A {@link BlockPos} plus the dimension it sits in. A network remembers both ends by these, so equality has to take the
 * dimension into account.
 *
 * <p>
 * The three coordinates are packed into one long the way 1.8+'s BlockPos does it: x in the top 26 bits, z in the middle
 * 26, y in the low 12.
 */
public class BlockPosDim extends BlockPos {

    private final int dimension;

    public BlockPosDim(int x, int y, int z, int dimension) {
        super(x, y, z);
        this.dimension = dimension;
    }

    public BlockPosDim(BlockPos pos, int dimension) {
        this(pos.getX(), pos.getY(), pos.getZ(), dimension);
    }

    public static BlockPosDim readFromNBT(NBTTagCompound tag) {
        long packed = tag.getLong("bPos");
        return new BlockPosDim(
            (int) (packed >> 38),
            (int) ((packed << 52) >> 52),
            (int) ((packed << 26) >> 38),
            tag.getInteger("dim"));
    }

    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        tag.setLong(
            "bPos",
            ((long) this.getX() & 0x3FFFFFFL) << 38 | ((long) this.getZ() & 0x3FFFFFFL) << 12
                | ((long) this.getY() & 0xFFFL));
        tag.setInteger("dim", this.dimension);
        return tag;
    }

    public BlockPos toBlockPos() {
        return new BlockPos(this.getX(), this.getY(), this.getZ());
    }

    public int getDimension() {
        return dimension;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof BlockPosDim other)) return false;
        return this.dimension == other.dimension && super.equals(obj);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getX(), this.getY(), this.getZ(), this.dimension);
    }
}
