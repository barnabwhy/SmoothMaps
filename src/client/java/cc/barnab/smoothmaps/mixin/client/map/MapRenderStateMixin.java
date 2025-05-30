package cc.barnab.smoothmaps.mixin.client.map;

import cc.barnab.smoothmaps.client.ItemFrameStateAccessor;
import cc.barnab.smoothmaps.client.MapRenderStateAccessor;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.decoration.ItemFrame;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(MapRenderState.class)
public class MapRenderStateMixin implements MapRenderStateAccessor, ItemFrameStateAccessor {
    @Unique
    @Nullable
    BlockPos blockPos;

    @Unique
    boolean isGlowing;

    @Unique
    Direction direction;

    @Unique
    int rotation;

    @Unique
    public ItemFrame itemFrame;

    @Override
    public void setBlockPos(@Nullable BlockPos bPos) {
        blockPos = bPos;
    }

    @Override
    public @Nullable BlockPos getBlockPos() {
        return blockPos;
    }

    @Override
    public void setIsGlowing(boolean glowing) {
        isGlowing = glowing;
    }

    @Override
    public boolean isGlowing() {
        return isGlowing;
    }

    @Override
    public void setDirection(Direction dir) {
        direction = dir;
    }

    @Override
    public Direction direction() {
        return direction;
    }

    @Override
    public void setRotation(int rot) {
        rotation = rot;
    }

    @Override
    public int rotation() {
        return rotation;
    }

    @Override
    public ItemFrame getItemFrame() {
        return itemFrame;
    }
    @Override
    public void setItemFrame(ItemFrame itemFrame) {
        this.itemFrame = itemFrame;
    }
}
