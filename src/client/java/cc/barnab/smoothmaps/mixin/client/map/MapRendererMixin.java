package cc.barnab.smoothmaps.mixin.client.map;

import cc.barnab.smoothmaps.client.MapRenderStateAccessor;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MapRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;

@Mixin(MapRenderer.class)
public class MapRendererMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void render(
            MapRenderState mapRenderState, PoseStack poseStack, MultiBufferSource multiBufferSource, boolean bl, int i,
            CallbackInfo ci,
            @Share("originalLight") LocalIntRef originalLight,
            @Share("lightLevels") LocalRef<HashMap<BlockPos, Integer>> blockLights
    ) {
        // Don't smooth light held maps or glow frames
        if (!bl || ((MapRenderStateAccessor) mapRenderState).isGlowing() || !Minecraft.useAmbientOcclusion())
            return;

        originalLight.set(i);

        // Get light levels for surrounding blocks
        assert Minecraft.getInstance().level != null;
        LevelLightEngine lightEngine = Minecraft.getInstance().level.getLightEngine();

        HashMap<BlockPos, Integer> lightLevels = new HashMap<>();

        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos pos = ((MapRenderStateAccessor) mapRenderState).getBlockPos().offset(x, y, z);

                    int light = 0;
                    if (lightEngine.skyEngine != null) {
                        int skyLight = lightEngine.skyEngine.getLightValue(pos);
                        light += ((skyLight * 16) & 0xFFFF) << 16;
                    }

                    if (lightEngine.blockEngine != null) {
                        int blockLight = lightEngine.blockEngine.getLightValue(pos);
                        light += (blockLight * 16) & 0xFFFF;
                    }

                    lightLevels.put(pos, light);
                }
            }
        }

        for (BlockPos pos : lightLevels.keySet()) {
            int light = lightLevels.get(pos);
            int skyLight = light >> 16;
            int blockLight = light & 0xFFFF;

            if (blockLight == 0 && skyLight == 0) {
                int maxSky = 0;
                int maxBlock = 0;

                for (int x = -1; x <= 1; x++) {
                    for (int y = -1; y <= 1; y++) {
                        for (int z = -1; z <= 1; z++) {
                            BlockPos otherPos = pos.offset(x, y, z);
                            if (lightLevels.containsKey(otherPos)) {
                                int dist = Math.abs(x) + Math.abs(y) + Math.abs(z);
                                int otherLight = lightLevels.get(otherPos);

                                int otherSkyLight = otherLight >> 16;
                                if (otherSkyLight - dist * 16 > maxSky)
                                    maxSky = otherSkyLight - dist * 16;

                                int otherBlockLight = otherLight & 0xFFFF;
                                if (otherBlockLight - dist * 16 > maxBlock)
                                    maxBlock = otherBlockLight - dist * 16;
                            }
                        }
                    }
                }

                skyLight = maxSky;
                blockLight = maxBlock;

                light = ((skyLight & 0xFFFF) << 16) + blockLight & 0xFFFF;
                lightLevels.put(pos, light);
            }
        }

        blockLights.set(lightLevels);
    }

    @Redirect(
            method = "render",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;addVertex(Lorg/joml/Matrix4f;FFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;"),
            slice = @Slice(
                    from = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;addVertex(Lorg/joml/Matrix4f;FFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;", ordinal = 0),
                    to = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;addVertex(Lorg/joml/Matrix4f;FFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;", ordinal = 3)
            )
    )
    private VertexConsumer mapVertexSmoothLight(
            VertexConsumer instance, Matrix4f matrix4f, float f, float g, float h,
            @Local(ordinal = 0, argsOnly = true) boolean bl,
            @Local(ordinal = 0, argsOnly = true) MapRenderState mapRenderState,
            @Local(ordinal = 0, argsOnly = true) LocalIntRef light,
            @Share("lightLevels") LocalRef<HashMap<BlockPos, Integer>> blockLights
    ) {
        // Don't smooth light held maps or glow frames
        if (bl && !((MapRenderStateAccessor) mapRenderState).isGlowing() && Minecraft.useAmbientOcclusion()) {
            BlockPos pos = ((MapRenderStateAccessor) mapRenderState).getBlockPos();

            float xInBlock = switch(((MapRenderStateAccessor) mapRenderState).rotation()) {
                case 0, 4 -> f / 128.0f;
                case 1, 5 -> 1.0f - g / 128.0f;
                case 2, 6 -> 1.0f - f / 128.0f;
                case 3, 7 -> g / 128.0f;
                default -> 0.0f;
            };

            float yInBlock = switch(((MapRenderStateAccessor) mapRenderState).rotation()) {
                case 0, 4 -> g / 128.0f;
                case 1, 5 -> f / 128.0f;
                case 2, 6 -> 1.0f - g / 128.0f;
                case 3, 7 -> 1.0f - f / 128.0f;
                default -> 0.0f;
            };

            light.set(getLight(pos, blockLights.get(), xInBlock, yInBlock, ((MapRenderStateAccessor) mapRenderState).direction()));
        }

        return instance.addVertex(matrix4f, f, g, h);
    }

    @Inject(
            method = "render",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;addVertex(Lorg/joml/Matrix4f;FFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;", ordinal = 4)
    )
    private void decorationsNormalLighting(
            MapRenderState mapRenderState, PoseStack poseStack, MultiBufferSource multiBufferSource, boolean bl, int i,
            CallbackInfo ci,
            @Local(ordinal = 0, argsOnly = true) LocalIntRef light,
            @Share("originalLight") LocalIntRef originalLight
    ) {
        // Don't smooth light held maps or glow frames
        if (!bl || ((MapRenderStateAccessor) mapRenderState).isGlowing() || !Minecraft.useAmbientOcclusion())
            return;

        light.set(originalLight.get());
    }

    @Unique
    private int getLight(BlockPos pos, HashMap<BlockPos, Integer> blockLights, float x, float y, Direction direction) {
        int light = blockLights.get(pos);

        int tl = light, tr = light, bl = light, br = light;

        // This block is tl
        if (x > 0.5f && y > 0.5f) {
            bl = getLightRelative(0, -1, pos, direction, blockLights);
            tr = getLightRelative(1, 0, pos, direction, blockLights);
            br = getLightRelative(1, -1, pos, direction, blockLights);
        }

        // This block is tr
        if (x < 0.5f && y > 0.5f) {
            tl = getLightRelative(-1, 0, pos, direction, blockLights);
            bl = getLightRelative(-1, -1, pos, direction, blockLights);
            br = getLightRelative(0, -1, pos, direction, blockLights);
        }

        // This block is bl
        if (x > 0.5f && y < 0.5f) {
            tl = getLightRelative(0, 1, pos, direction, blockLights);
            tr = getLightRelative(1, 1, pos, direction, blockLights);
            br = getLightRelative(1, 0, pos, direction, blockLights);
        }

        // This block is br
        if (x < 0.5f && y < 0.5f) {
            tl = getLightRelative(-1, 1, pos, direction, blockLights);
            bl = getLightRelative(-1, 0, pos, direction, blockLights);
            tr = getLightRelative(0, 1, pos, direction, blockLights);
        }

        float xFrac = x < 0.5f ? (x + 0.5f) : (x - 0.5f);
        float yFrac = y < 0.5f ? (y + 0.5f) : (y - 0.5f);

        int lightBlock = bilinearInterp(xFrac, yFrac, tl & 0xFFFF, tr & 0xFFFF, bl & 0xFFFF, br & 0xFFFF);
        int lightSky = bilinearInterp(xFrac, yFrac, tl >> 16, tr >> 16, bl >> 16, br >> 16);

        return lightBlock + (lightSky << 16);
    }


    @Unique
    private int bilinearInterp(float x, float y, int tl, int tr, int bl, int br) {
        float t = (float)tl + (float)(tr - tl) * x;
        float b = (float)bl + (float)(br - bl) * x;
        return (int)(t + (b - t) * y);
    }

    @Unique
    private int getLightRelative(int xStep, int yStep, BlockPos blockPos, Direction dir, HashMap<BlockPos, Integer> blockLights) {
        Direction leftDir = switch(dir) {
            case DOWN -> Direction.WEST;
            case UP -> Direction.WEST;
            case NORTH -> Direction.EAST;
            case SOUTH -> Direction.WEST;
            case WEST -> Direction.NORTH;
            case EAST -> Direction.SOUTH;
        };
        Direction rightDir = switch(dir) {
            case DOWN -> Direction.EAST;
            case UP -> Direction.EAST;
            case NORTH -> Direction.WEST;
            case SOUTH -> Direction.EAST;
            case WEST -> Direction.SOUTH;
            case EAST -> Direction.NORTH;
        };
        Direction upDir = switch(dir) {
            case DOWN -> Direction.SOUTH;
            case UP -> Direction.NORTH;
            case NORTH, SOUTH, WEST, EAST -> Direction.UP;
        };
        Direction downDir = switch(dir) {
            case DOWN -> Direction.NORTH;
            case UP -> Direction.SOUTH;
            case NORTH, SOUTH, WEST, EAST -> Direction.DOWN;
        };

        BlockPos.MutableBlockPos pos = blockPos.mutable();

        if (xStep > 0)
            pos.move(rightDir, 1);

        if (xStep < 0)
            pos.move(leftDir, 1);

        if (yStep > 0)
            pos.move(upDir, 1);

        if (yStep < 0)
            pos.move(downDir, 1);

        return blockLights.get(pos);
    }
}
