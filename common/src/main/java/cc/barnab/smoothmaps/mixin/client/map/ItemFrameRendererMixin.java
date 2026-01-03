package cc.barnab.smoothmaps.mixin.client.map;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MapRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.client.renderer.entity.state.ItemFrameRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemFrameRenderer.class)
public abstract class ItemFrameRendererMixin<T extends ItemFrame> {

    @Shadow
    @Final
    private MapRenderer mapRenderer;

    @Inject(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemFrameRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
            at = @At("HEAD")
    )
    private void render(ItemFrameRenderState itemFrameRenderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState, CallbackInfo ci) {
        if (itemFrameRenderState.mapId == null)
            return;

        itemFrameRenderState.mapRenderState.setBlockPos(BlockPos.containing(itemFrameRenderState.x, itemFrameRenderState.y, itemFrameRenderState.z));
        itemFrameRenderState.mapRenderState.setIsGlowing(itemFrameRenderState.isGlowFrame);
        itemFrameRenderState.mapRenderState.setDirection(itemFrameRenderState.direction);
        itemFrameRenderState.mapRenderState.setRotation(itemFrameRenderState.rotation);

        itemFrameRenderState.mapRenderState.setItemFrame(itemFrameRenderState.getItemFrame());
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/decoration/ItemFrame;Lnet/minecraft/client/renderer/entity/state/ItemFrameRenderState;F)V", at = @At("TAIL"))
    private void extractRenderState(T itemFrame, ItemFrameRenderState itemFrameRenderState, float f, CallbackInfo ci) {
        itemFrameRenderState.setItemFrame(itemFrame);
    }

    @WrapWithCondition(method = "extractRenderState(Lnet/minecraft/world/entity/decoration/ItemFrame;Lnet/minecraft/client/renderer/entity/state/ItemFrameRenderState;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/ItemModelResolver;updateForNonLiving(Lnet/minecraft/client/renderer/item/ItemStackRenderState;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/world/entity/Entity;)V"))
    private boolean updateItemModelOnlyWhenNeeded(ItemModelResolver instance, ItemStackRenderState arg, ItemStack itemStack, ItemDisplayContext arg3, Entity entity) {
        // Skip itemModelResolver.updateForNonLiving if we have a map we can render
        // Should improve performance for framed maps.
        // Extra checks don't have much impact, don't worry about deduplicating.

        ItemFrame itemFrame = (ItemFrame)entity;
        if (!itemStack.isEmpty()) {
            MapId mapId = itemFrame.getFramedMapId(itemStack);
            if (mapId != null) {
                MapItemSavedData mapItemSavedData = itemFrame.level().getMapData(mapId);
                return mapItemSavedData == null;
            }
        }

        return true;
    }
}
