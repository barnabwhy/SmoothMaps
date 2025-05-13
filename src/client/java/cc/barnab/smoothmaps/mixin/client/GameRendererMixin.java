package cc.barnab.smoothmaps.mixin.client;

import cc.barnab.smoothmaps.client.GameRenderTimeGetter;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin implements GameRenderTimeGetter {
    @Unique
    private long lastFrameRenderTime = 0L;

    @Override
    public long getLastRenderTime() {
        return lastFrameRenderTime;
    }

    @Inject(method = "render", at = @At("HEAD"))
    void render(DeltaTracker deltaTracker, boolean bl, CallbackInfo ci) {
        lastFrameRenderTime = Util.getNanos();
    }
}
