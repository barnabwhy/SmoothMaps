package cc.barnab.smoothmaps.client;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;

public class LightUpdateTracker {
    final private static Long2LongMap chunkUpdateTimes = new Long2LongOpenHashMap();

    static {
        // Explicitly set what 'get()' returns if the key is missing.
        chunkUpdateTimes.defaultReturnValue(0L);
    }

    public static long getLastUpdated(BlockPos blockPos) {
        return chunkUpdateTimes.get(ChunkPos.asLong(blockPos.getX() >> 4, blockPos.getZ() >> 4));
    }

    public static void setLastUpdated(long chunkPos, long lastUpdated) {
        chunkUpdateTimes.put(chunkPos, lastUpdated);
    }

    public static void reset() {
        chunkUpdateTimes.clear();
    }
}
