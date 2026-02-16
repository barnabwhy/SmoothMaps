package cc.barnab.smoothmaps.client;

import net.minecraft.world.level.ChunkPos;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;

public class LightUpdateTracker {
    final private static Long2LongMap chunkUpdateTimes = new Long2LongOpenHashMap();

    static {
        // Explicitly set what 'get()' returns if the key is missing.
        chunkUpdateTimes.defaultReturnValue(0L);
    }

    public static synchronized long getLastUpdated(ChunkPos chunkPos) {
        return chunkUpdateTimes.get(chunkPos.toLong());
    }

    public static synchronized void setLastUpdated(long chunkPos, long lastUpdated) {
        chunkUpdateTimes.put(chunkPos, lastUpdated);
    }

    public static synchronized void reset() {
        chunkUpdateTimes.clear();
    }
}
