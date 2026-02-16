package cc.barnab.smoothmaps.mixin.client;

import cc.barnab.smoothmaps.client.GameRenderTimeGetter;
import net.minecraft.util.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.entity.PaintingRenderer;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin implements GameRenderTimeGetter {
    @Shadow
    public float fovModifier;
    @Shadow
    public float oldFovModifier;
    @Shadow
    @Final
    private Minecraft minecraft;
    @Unique
    private long lastFrameRenderTime = 0L;

    @Unique
    private float mapCullingDotThreshold = -1.0F;

    @Override
    public long getLastRenderTime() {
        return lastFrameRenderTime;
    }

    @Override
    public float getMapCullingDotThreshold() {
        return mapCullingDotThreshold;
    }

    @Inject(method = "render", at = @At("HEAD"))
    void render(DeltaTracker deltaTracker, boolean bl, CallbackInfo ci) {
        lastFrameRenderTime = Util.getNanos();

        float clampedFovModifier = Math.max(Math.max(fovModifier, oldFovModifier), 1f);
        float fovVert = (float) Math.toRadians((float)minecraft.options.fov().get() * clampedFovModifier);
        float fovHoriz = fovVert * (float)minecraft.getWindow().getWidth() / (float)minecraft.getWindow().getHeight();

        mapCullingDotThreshold = (float) Math.cos(Math.PI - fovHoriz);

        minecraft.getMapRenderer().resetCounters();

        PaintingRenderer paintingRenderer = (PaintingRenderer) minecraft.getEntityRenderDispatcher().renderers.get(EntityType.PAINTING);
        if (paintingRenderer != null)
            paintingRenderer.resetCounters();
    }
}
