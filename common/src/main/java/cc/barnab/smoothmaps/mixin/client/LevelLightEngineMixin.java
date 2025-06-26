package cc.barnab.smoothmaps.mixin.client;

import cc.barnab.smoothmaps.client.GameRenderTimeGetter;
import cc.barnab.smoothmaps.client.LightUpdateAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelLightEngine.class)
public class LevelLightEngineMixin implements LightUpdateAccessor {
    @Unique
    private long lastUpdated = 0L;

    @Override
    public long getLastUpdated() {
        return lastUpdated;
    };

    @Inject(method = "runLightUpdates", at = @At("TAIL"))
    void runLightUpdates(CallbackInfoReturnable<Integer> cir) {
        // If any light updates happened
        if (cir.getReturnValue() > 0)
            lastUpdated = Minecraft.getInstance().gameRenderer.getLastRenderTime();
    }
}
