package cc.barnab.smoothmaps.mixin.client.map;

import cc.barnab.smoothmaps.client.ItemFrameLightAccessor;
import net.minecraft.core.BlockPos;
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
    public long getLastUpdated() {
        return lastUpdated;
    }
    @Override
    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
