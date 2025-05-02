package cc.barnab.smoothmaps.mixin.client.map;

import cc.barnab.smoothmaps.client.MapRenderStateAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.client.renderer.entity.state.ItemFrameRenderState;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemFrameRenderer.class)
public class ItemFrameRendererMixin {
    @Inject(
            method = "render(Lnet/minecraft/client/renderer/entity/state/ItemFrameRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(target = "Lnet/minecraft/client/renderer/MapRenderer;render(Lnet/minecraft/client/renderer/state/MapRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ZI)V", value = "INVOKE")
    )
    private void render(ItemFrameRenderState itemFrameRenderState, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, CallbackInfo ci) {
        ((MapRenderStateAccessor)itemFrameRenderState.mapRenderState).setBlockPos(BlockPos.containing(itemFrameRenderState.x, itemFrameRenderState.y, itemFrameRenderState.z));
        ((MapRenderStateAccessor)itemFrameRenderState.mapRenderState).setIsGlowing(itemFrameRenderState.isGlowFrame);
        ((MapRenderStateAccessor)itemFrameRenderState.mapRenderState).setDirection(itemFrameRenderState.direction);
        ((MapRenderStateAccessor)itemFrameRenderState.mapRenderState).setRotation(itemFrameRenderState.rotation);
    }
}
