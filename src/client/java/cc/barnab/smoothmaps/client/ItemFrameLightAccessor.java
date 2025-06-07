package cc.barnab.smoothmaps.client;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public interface ItemFrameLightAccessor {
    default int[] getVertLights() {
        return null;
    }
    default void setVertLights(int[] vertLights) {}

    default BlockPos getLastBlockPos() {
        return null;
    }
    default void setLastBlockPos(BlockPos lastBlockPos) {}

    default int getLastRotation() {
        return 0;
    }
    default void setLastRotation(int lastRotation) {}

    default Direction getLastDirection() {
        return null;
    }
    default void setLastDirection(Direction lastDirection) {}

    default long getLastUpdated() {
        return 0L;
    }
    default void setLastUpdated(long lastUpdated) {}
}
