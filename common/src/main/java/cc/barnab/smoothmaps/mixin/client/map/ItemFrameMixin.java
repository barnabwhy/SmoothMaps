package cc.barnab.smoothmaps.mixin.client.map;

import cc.barnab.smoothmaps.client.ItemFrameLightAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.decoration.ItemFrame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ItemFrame.class)
public class ItemFrameMixin implements ItemFrameLightAccessor {
    @Unique
    public int[] vertLights = new int[4];

    @Unique
    public BlockPos lastBlockPos = null;

    @Unique
    public int lastRotation = 0;

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

    public int getLastRotation() {
        return lastRotation;
    }
    @Override
    public void setLastRotation(int lastRotation) {
        this.lastRotation = lastRotation;
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
