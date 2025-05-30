package cc.barnab.smoothmaps.mixin.client;

import cc.barnab.smoothmaps.client.LightUpdateAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightTexture.class)
public class LightTextureMixin implements LightUpdateAccessor {
    @Shadow private boolean updateLightTexture;
    @Shadow @Final private Minecraft minecraft;
    @Unique
    long lastUpdated = 0;

    @Override
    public long getLastUpdated() {
        return lastUpdated;
    }

    @Inject(method = "updateLightTexture(F)V", at = @At("HEAD"))
    void updateLightTexture(float f, CallbackInfo ci) {
        if (updateLightTexture)
            lastUpdated = minecraft.gameRenderer.getLastRenderTime();
    }
}
