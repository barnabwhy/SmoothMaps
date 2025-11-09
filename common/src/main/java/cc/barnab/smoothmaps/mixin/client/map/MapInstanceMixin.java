package cc.barnab.smoothmaps.mixin.client.map;

import cc.barnab.smoothmaps.client.MapInstanceDirty;
import cc.barnab.smoothmaps.compat.ImmediatelyFastCompat;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.MapTextureManager;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MapTextureManager.MapInstance.class)
public abstract class MapInstanceMixin implements MapInstanceDirty {
    @Unique
    private static final Logger LOGGER = LogUtils.getLogger();

    @Unique
    private boolean isDirty = true;
    @Unique
    private int dirtyX1 = 0;
    @Unique
    private int dirtyY1 = 0;
    @Unique
    private int dirtyX2 = 128;
    @Unique
    private int dirtyY2 = 128;

    @Shadow
    private MapItemSavedData data;

    @Final
    @Shadow
    private DynamicTexture texture;

    @Shadow
    private boolean requiresUpload;

    @Inject(method = "updateTextureIfNeeded", at = @At("HEAD"), cancellable = true)
    private void writeTexture(CallbackInfo ci) {
        // Skip this if ImmediatelyFast is loaded
        if (ImmediatelyFastCompat.isAvailable())
            return;

        if (requiresUpload) {
            if (isDirty) {
                NativeImage nativeimage = texture.getPixels();
                if (nativeimage != null) {
                    // Only update the dirty region
                    for (int i = dirtyY1; i < dirtyY2; ++i) {
                        for (int j = dirtyX1; j < dirtyX2; ++j) {
                            int k = j + i * 128;
                            nativeimage.setPixel(j, i, MapColor.getColorFromPackedId(data.colors[k]));
                        }
                    }
                }

                if (this.texture.pixels != null && texture.texture != null) {
                    int w = dirtyX2 - dirtyX1;
                    int h = dirtyY2 - dirtyY1;
                    int x = dirtyX1;
                    int y = dirtyY1;
                    // Original call: Writes entire texture
                    // RenderSystem.getDevice().createCommandEncoder().writeToTexture(instance.texture, instance.pixels);
                    // New call: Copies only what changed
                    RenderSystem.getDevice().createCommandEncoder().writeToTexture(texture.texture, texture.pixels, 0, 0, x, y, w, h, x, y);

                    isDirty = false;
                } else {
                    LOGGER.warn("Trying to upload disposed texture {}", texture.getTexture().getLabel());
                }
            }
            this.requiresUpload = false;
        }

        ci.cancel();
    }

    @Override
    public void setDirty(int x, int y, int w, int h) {
        int x2 = x + w;
        int y2 = y + h;
        if (isDirty) {
            dirtyX1 = Math.clamp(Math.min(x, dirtyX1), 0, 128);
            dirtyY1 = Math.clamp(Math.min(y, dirtyY1), 0, 128);
            dirtyX2 = Math.clamp(Math.max(x2, dirtyX2), 0, 128);
            dirtyY2 = Math.clamp(Math.max(y2, dirtyY2), 0, 128);
        } else {
            isDirty = true;
            dirtyX1 = Math.clamp(x, 0, 128);
            dirtyY1 = Math.clamp(y, 0, 128);
            dirtyX2 = Math.clamp(x2, 0, 128);
            dirtyY2 = Math.clamp(y2, 0, 128);
        }
    }
}
