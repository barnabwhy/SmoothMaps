package cc.barnab.smoothmaps.mixin.client.painting;

import cc.barnab.smoothmaps.client.*;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.PaintingRenderer;
import net.minecraft.client.renderer.entity.state.PaintingRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Purpose: Prevents rendering of unnecessary painting vertices & Adds smooth lighting
 */

@Mixin(PaintingRenderer.class)
public abstract class PaintingRendererMixin implements RenderRelightCounter {
    @Unique
    private static final String VERTEX_TARGET = "Lnet/minecraft/client/renderer/entity/PaintingRenderer;vertex(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lcom/mojang/blaze3d/vertex/VertexConsumer;FFFFFIIII)V";

    @Unique
    private int numRendered = 0;
    @Unique
    private int numRelit = 0;

    @Unique
    private int[] vertLightsShared;

    @Override
    public int getNumRendered() {
        return numRendered;
    }
    @Override
    public int getNumRelit() {
        return numRelit;
    }

    @Override
    public void resetCounters() {
        numRendered = 0;
        numRelit = 0;
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/decoration/Painting;Lnet/minecraft/client/renderer/entity/state/PaintingRenderState;F)V", at = @At("TAIL"))
    public void extractRenderState(Painting painting, PaintingRenderState paintingRenderState, float f, CallbackInfo ci) {
        paintingRenderState.setPainting(painting);
    }


    @Inject(method = "render(Lnet/minecraft/client/renderer/entity/state/PaintingRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"))
    private void render(
            PaintingRenderState paintingRenderState, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, CallbackInfo ci
    ) {
        numRendered++;

        if (Minecraft.useAmbientOcclusion()) {
            BlockPos blockPos = BlockPos.containing(paintingRenderState.x, paintingRenderState.y, paintingRenderState.z);

            // Clear vertex lights from previous
            assert Minecraft.getInstance().level != null;
            LevelLightEngine lightEngine = Minecraft.getInstance().level.getLightEngine();

            Painting painting = paintingRenderState.getPainting();
            int[] vertLights = painting.getVertLights();

            boolean shouldRelight = ((LightUpdateAccessor)lightEngine).getLastUpdated() > painting.getLastUpdated()
                    || !blockPos.equals(painting.getLastBlockPos())
                    || !painting.getDirection().equals(painting.getLastDirection())
                    || vertLights == null;
            if (shouldRelight) {
                assert paintingRenderState.variant != null;

                int frontFaceVertCount = (paintingRenderState.variant.width() + 1) * (paintingRenderState.variant.height() + 1);
                if (vertLights == null || vertLights.length < frontFaceVertCount)
                    vertLights = new int[frontFaceVertCount];

                for (int v = 0; v < frontFaceVertCount; v++)
                    vertLights[v] = -1;

                numRelit++;
                painting.setVertLights(vertLights);

                painting.setLastUpdated(Minecraft.getInstance().gameRenderer.getLastRenderTime());
                painting.setLastBlockPos(blockPos);
                painting.setLastDirection(painting.getDirection());
            }

            vertLightsShared = vertLights;
        }
    }


    @Inject(method = "renderPainting", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;getV(F)F", ordinal = 2))
    private void performEdgeChecks(
            CallbackInfo ci,
            @Local(argsOnly = true, ordinal = 0) int pWidth,
            @Local(argsOnly = true, ordinal = 1) int pHeight,
            @Local(ordinal = 2) int blockX,
            @Local(ordinal = 3) int blockY,
            @Share("isNorthEdge")LocalBooleanRef isNorthEdge,
            @Share("isEastEdge")LocalBooleanRef isEastEdge,
            @Share("isSouthEdge")LocalBooleanRef isSouthEdge,
            @Share("isWestEdge")LocalBooleanRef isWestEdge,
            @Share("blockPos") LocalRef<BlockPos> blockPos
    ) {
        isNorthEdge.set(blockY == pHeight - 1);
        isWestEdge.set(blockX == pWidth - 1);
        isSouthEdge.set(blockY == 0);
        isEastEdge.set(blockX == 0);
    }

    @WrapWithCondition(
            method = "renderPainting",
            at = {
                    @At(ordinal = 8, value = "INVOKE", target = VERTEX_TARGET),
                    @At(ordinal = 9, value = "INVOKE", target = VERTEX_TARGET),
                    @At(ordinal = 10, value = "INVOKE", target = VERTEX_TARGET),
                    @At(ordinal = 11, value = "INVOKE", target = VERTEX_TARGET)
            }
    )
    private boolean preventNorthFace(
            PaintingRenderer instance, PoseStack.Pose pose, VertexConsumer vertexConsumer, float f, float g, float h, float i, float j, int k, int l, int m, int n,
            @Share("isNorthEdge") LocalBooleanRef isEdge
            ) {
        return isEdge.get();
    }

    @WrapWithCondition(
            method = "renderPainting",
            at = {
                    @At(ordinal = 12, value = "INVOKE", target = VERTEX_TARGET),
                    @At(ordinal = 13, value = "INVOKE", target = VERTEX_TARGET),
                    @At(ordinal = 14, value = "INVOKE", target = VERTEX_TARGET),
                    @At(ordinal = 15, value = "INVOKE", target = VERTEX_TARGET)
            }
    )
    private boolean preventSouthFace(
            PaintingRenderer instance, PoseStack.Pose pose, VertexConsumer vertexConsumer, float f, float g, float h, float i, float j, int k, int l, int m, int n,
            @Share("isSouthEdge") LocalBooleanRef isEdge
    ) {
        return isEdge.get();
    }

    @WrapWithCondition(
            method = "renderPainting",
            at = {
                    @At(ordinal = 16, value = "INVOKE", target = VERTEX_TARGET),
                    @At(ordinal = 17, value = "INVOKE", target = VERTEX_TARGET),
                    @At(ordinal = 18, value = "INVOKE", target = VERTEX_TARGET),
                    @At(ordinal = 19, value = "INVOKE", target = VERTEX_TARGET)
            }
    )
    private boolean preventWestFace(
            PaintingRenderer instance, PoseStack.Pose pose, VertexConsumer vertexConsumer, float f, float g, float h, float i, float j, int k, int l, int m, int n,
            @Share("isWestEdge") LocalBooleanRef isEdge
    ) {
        return isEdge.get();
    }

    @WrapWithCondition(
            method = "renderPainting",
            at = {
                    @At(ordinal = 20, value = "INVOKE", target = VERTEX_TARGET),
                    @At(ordinal = 21, value = "INVOKE", target = VERTEX_TARGET),
                    @At(ordinal = 22, value = "INVOKE", target = VERTEX_TARGET),
                    @At(ordinal = 23, value = "INVOKE", target = VERTEX_TARGET)
            }
    )
    private boolean preventEastFace(
            PaintingRenderer instance, PoseStack.Pose pose, VertexConsumer vertexConsumer, float f, float g, float h, float i, float j, int k, int l, int m, int n,
            @Share("isEastEdge") LocalBooleanRef isEdge
    ) {
        return isEdge.get();
    }

    @Redirect(
            method = "renderPainting",
            at = @At(value = "INVOKE", target = VERTEX_TARGET)
    )
    private void vertexWithSmoothLight(
            PaintingRenderer instance, PoseStack.Pose pose, VertexConsumer vertexConsumer, float f, float g, float h, float i, float j, int k, int l, int m, int n,
            @Local(ordinal = 0, argsOnly = true) int[] lightCoords,
            @Local(ordinal = 0, argsOnly = true) int pWidth,
            @Local(ordinal = 1, argsOnly = true) int pHeight,
            @Local(ordinal = 2) int blockX,
            @Local(ordinal = 3) int blockY
    ) {
        if (Minecraft.useAmbientOcclusion()) {
            int[] vertLights = vertLightsShared;

            int packedVertPos = (int)((g + (float) pHeight / 2.0f) * (pWidth+1) + (f + (float) pWidth / 2.0f));
            if (vertLights[packedVertPos] != -1) {
                n = vertLights[packedVertPos];
            } else {
                float xInBlock = f + (float) pWidth / 2.0f - blockX;
                float yInBlock = g + (float) pHeight / 2.0f - blockY;
                n = getLight(blockX, blockY, pWidth, pHeight, lightCoords, xInBlock, yInBlock);

                vertLights[packedVertPos] = n;
            }
        }

        vertexConsumer.addVertex(pose, f, g, j).setColor(-1).setUv(h, i).setOverlay(OverlayTexture.NO_OVERLAY).setLight(n).setNormal(pose, (float)k, (float)l, (float)m);
    }

    @Unique
    private int getLight(int blockX, int blockY, int pWidth, int pHeight, int[] lightCoords, float x, float y) {
        int light = lightCoords[blockX + blockY * pWidth];

        int tl = light, tr = light, bl = light, br = light;

        // This block is tl
        if (x > 0.5f && y < 0.5f) {
            bl = getLightRelative(0, -1, blockX, blockY, pWidth, pHeight, lightCoords);
            tr = getLightRelative(1, 0, blockX, blockY, pWidth, pHeight, lightCoords);
            br = getLightRelative(1, -1, blockX, blockY, pWidth, pHeight, lightCoords);
        }

        // This block is tr
        if (x < 0.5f && y < 0.5f) {
            tl = getLightRelative(-1, 0, blockX, blockY, pWidth, pHeight, lightCoords);
            bl = getLightRelative(-1, -1, blockX, blockY, pWidth, pHeight, lightCoords);
            br = getLightRelative(0, -1, blockX, blockY, pWidth, pHeight, lightCoords);
        }

        // This block is bl
        if (x > 0.5f && y > 0.5f) {
            tl = getLightRelative(0, 1, blockX, blockY, pWidth, pHeight, lightCoords);
            tr = getLightRelative(1, 1, blockX, blockY, pWidth, pHeight, lightCoords);
            br = getLightRelative(1, 0, blockX, blockY, pWidth, pHeight, lightCoords);
        }

        // This block is br
        if (x < 0.5f && y > 0.5f) {
            tl = getLightRelative(-1, 1, blockX, blockY, pWidth, pHeight, lightCoords);
            bl = getLightRelative(-1, 0, blockX, blockY, pWidth, pHeight, lightCoords);
            tr = getLightRelative(0, 1, blockX, blockY, pWidth, pHeight, lightCoords);
        }

        //float xFrac = x < 0.5f ? (x + 0.5f) : (x - 0.5f);
        //float yFrac = y < 0.5f ? (y + 0.5f) : (y - 0.5f);

        // The verts are always exactly at the midpoint of the 4 blocks, so we can just average
        //int lightBlock = MathUtil.bilinearInterp(xFrac, yFrac, tl & 0xFFFF, tr & 0xFFFF, bl & 0xFFFF, br & 0xFFFF);
        //int lightSky = MathUtil.bilinearInterp(xFrac, yFrac, tl >> 16, tr >> 16, bl >> 16, br >> 16);
        int lightBlock = MathUtil.midOf4(tl & 0xFFFF, tr & 0xFFFF, bl & 0xFFFF, br & 0xFFFF);
        int lightSky = MathUtil.midOf4(tl >> 16, tr >> 16, bl >> 16, br >> 16);

        return lightBlock + (lightSky << 16);
    }

    @Unique
    private int getLightRelative(int xStep, int yStep, int blockX, int blockY, int pWidth, int pHeight, int[] lightCoords) {
        if (xStep > 0 && blockX != (pWidth - 1))
            blockX++;

        if (xStep < 0 && blockX != 0)
            blockX--;

        if (yStep > 0 && blockY != (pHeight - 1))
            blockY++;

        if (yStep < 0 && blockY != 0)
            blockY--;

        return lightCoords[blockX + blockY * pWidth];
    }
}
