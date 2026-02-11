package cz.stofiiis.echoregions.data;

import net.minecraft.world.level.ChunkPos;

public record RegionPos(int x, int z) {
    public static final int SHIFT = 3;

    public static RegionPos fromChunk(ChunkPos chunkPos) {
        return new RegionPos(chunkPos.x >> SHIFT, chunkPos.z >> SHIFT);
    }

    public static RegionPos fromChunk(int chunkX, int chunkZ) {
        return new RegionPos(chunkX >> SHIFT, chunkZ >> SHIFT);
    }

    public long toLong() {
        return ((long) x & 0xffffffffL) << 32 | ((long) z & 0xffffffffL);
    }

    public static RegionPos fromLong(long value) {
        return new RegionPos((int) (value >> 32), (int) value);
    }
}
