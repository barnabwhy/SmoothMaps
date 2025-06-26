package cc.barnab.smoothmaps.client;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public interface PaintingLightAccessor {
    default int[] getVertLights() {
        return null;
    }
    default void setVertLights(int[] vertLights) {}

    default BlockPos getLastBlockPos() {
        return null;
    }
    default void setLastBlockPos(BlockPos lastBlockPos) {}

    default Direction getLastDirection() {
        return null;
    }
    default void setLastDirection(Direction lastDirection) {}

    default long getLastUpdated() {
        return 0L;
    }
    default void setLastUpdated(long lastUpdated) {}
}
