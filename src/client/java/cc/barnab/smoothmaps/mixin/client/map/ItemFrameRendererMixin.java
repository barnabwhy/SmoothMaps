package cc.barnab.smoothmaps.mixin.client.map;

import cc.barnab.smoothmaps.client.MapRenderStateAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.client.renderer.entity.state.ItemFrameRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.decoration.ItemFrame;
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
        itemFrameRenderState.mapRenderState.setBlockPos(BlockPos.containing(itemFrameRenderState.x, itemFrameRenderState.y, itemFrameRenderState.z));
        itemFrameRenderState.mapRenderState.setIsGlowing(itemFrameRenderState.isGlowFrame);
        itemFrameRenderState.mapRenderState.setDirection(itemFrameRenderState.direction);
        itemFrameRenderState.mapRenderState.setRotation(itemFrameRenderState.rotation);

        itemFrameRenderState.mapRenderState.setItemFrame(itemFrameRenderState.getItemFrame());
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/decoration/ItemFrame;Lnet/minecraft/client/renderer/entity/state/ItemFrameRenderState;F)V", at = @At("TAIL"))
    private void extractRenderState(ItemFrame itemFrame, ItemFrameRenderState itemFrameRenderState, float f, CallbackInfo ci) {
        itemFrameRenderState.setItemFrame(itemFrame);
    }
}
