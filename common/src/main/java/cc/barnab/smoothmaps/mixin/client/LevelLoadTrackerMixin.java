package cc.barnab.smoothmaps.mixin.client;

import cc.barnab.smoothmaps.client.LightUpdateTracker;
import net.minecraft.client.multiplayer.LevelLoadTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelLoadTracker.class)
public class LevelLoadTrackerMixin {
    @Inject(method = "startClientLoad", at = @At("TAIL"))
    private void startClientLoad(CallbackInfo ci) {
        // Reset light update tracking when we change world/dimension
        LightUpdateTracker.reset();
    }
}
