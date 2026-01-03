package cc.barnab.smoothmaps.mixin.client.painting;

import cc.barnab.smoothmaps.client.PaintingStateAccessor;
import net.minecraft.client.renderer.entity.state.PaintingRenderState;
import net.minecraft.world.entity.decoration.painting.Painting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(PaintingRenderState.class)
public class PaintingRenderStateMixin implements PaintingStateAccessor {
    @Unique
    public Painting painting = null;

    @Override
    public Painting getPainting() {
        return painting;
    }

    @Override
    public void setPainting(Painting painting) {
        this.painting = painting;
    }
}
