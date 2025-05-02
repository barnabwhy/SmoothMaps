package cc.barnab.smoothmaps.client;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public interface MapRenderStateAccessor {
    default void setBlockPos(BlockPos bPos) {};
    default BlockPos getBlockPos() {
        return null;
    };

    default void setIsGlowing(boolean glowing) {};
    default boolean isGlowing() {
        return false;
    };

    default void setDirection(Direction dir) {};
    default Direction direction() {
        return Direction.NORTH;
    };


    default void setRotation(int rot) {};
    default int rotation() {
        return 0;
    };
}
