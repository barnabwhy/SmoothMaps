package cc.barnab.smoothmaps.mixin.client.painting;

import cc.barnab.smoothmaps.client.PaintingLightAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.decoration.painting.Painting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Painting.class)
public class PaintingMixin implements PaintingLightAccessor {
    @Unique
    public int[] vertLights = null;

    @Unique
    public BlockPos lastBlockPos = null;

    @Unique
    public Direction lastDirection = null;

    @Unique
    public long lastUpdated = -1L;

    @Override
    public int[] getVertLights() {
        return vertLights;
    }

    @Override
    public void setVertLights(int[] vertLights) {
        this.vertLights = vertLights;
    }

    @Override
    public BlockPos getLastBlockPos() {
        return lastBlockPos;
    }

    @Override
    public void setLastBlockPos(BlockPos lastBlockPos) {
        this.lastBlockPos = lastBlockPos;
    }

    @Override
    public Direction getLastDirection() {
        return lastDirection;
    }
    @Override
    public void setLastDirection(Direction lastDirection) {
        this.lastDirection = lastDirection;
    }

    @Override
    public long getLastUpdated() {
        return lastUpdated;
    }
    @Override
    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
