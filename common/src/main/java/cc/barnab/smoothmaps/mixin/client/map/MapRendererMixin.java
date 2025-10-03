package cc.barnab.smoothmaps.mixin.client.map;

import cc.barnab.smoothmaps.client.LightUpdateAccessor;
import cc.barnab.smoothmaps.client.MathUtil;
import cc.barnab.smoothmaps.client.RenderRelightCounter;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MapRenderer.class)
public abstract class MapRendererMixin implements RenderRelightCounter {
    @Unique
    boolean shouldReuseVertexLights = false;

    @Unique
    private final static int[][] vertexRotationMap = new int[][]{
            {2, 0, 3, 1},
            {3, 2, 1, 0},
            {1, 3, 0, 2}
    };

    @Unique
    private final static float[] xPositions = { 0f, 1f, 1f, 0f };
    @Unique
    private final static float[] yPositions = { 1f, 1f, 0f, 0f };

    @Unique
    int[][][] lightLevels = new int[3][3][3];

    @Unique
    boolean shouldSmoothLight = false;

    @Unique
    private int numRendered = 0;
    @Unique
    private int numRelit = 0;

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

    @Unique
    private boolean shouldSmoothLight(MapRenderState mapRenderState, boolean shouldReuseVertexLights) {
        if (mapRenderState.isGlowing())
            return false;

        if (!Minecraft.useAmbientOcclusion())
            return false;

        // If we're reusing the light values it's faster than these checks
        // So just skip them and allow smooth lighting
        if (shouldReuseVertexLights)
            return true;

        // Optimisations to disable smooth lighting in situations you wouldn't see it
        GameRenderer gameRenderer = Minecraft.getInstance().gameRenderer;
        Camera mainCamera = gameRenderer.getMainCamera();
        Vector3f camLook = mainCamera.getLookVector();

        // Is camera behind frame
        Direction.Axis axis = mapRenderState.direction().getAxis();
        Direction.AxisDirection axisDir = mapRenderState.direction().getAxisDirection();
        boolean isCamBehind = switch (axis) {
            case X -> (mapRenderState.getBlockPos().getX() - mainCamera.getPosition().x) * axisDir.getStep() > 1f;
            case Y -> (mapRenderState.getBlockPos().getY() - mainCamera.getPosition().y) * axisDir.getStep() > 1f;
            case Z -> (mapRenderState.getBlockPos().getZ() - mainCamera.getPosition().z) * axisDir.getStep() > 1f;
        };

        if (isCamBehind)
            return false;

        // Is camera >128 blocks from frame
        float distSqr = (float) mapRenderState.getBlockPos().distSqr(mainCamera.getBlockPosition());
        if (distSqr > 128f * 128f)
            return false;

        // Is camera facing away from frame
        float clampedFovModifier = Math.max(Math.max(gameRenderer.fovModifier, gameRenderer.oldFovModifier), 1f);
        float fovVert = (float) Math.toRadians((float)Minecraft.getInstance().options.fov().get() * clampedFovModifier);
        float fovHoriz = fovVert * (float)Minecraft.getInstance().getWindow().getWidth() / (float)Minecraft.getInstance().getWindow().getHeight();
        if (mapRenderState.direction().step().angle(camLook) < Math.PI - fovHoriz)
            return false;

        return true;
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void render(
            MapRenderState mapRenderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, boolean bl, int i,
            CallbackInfo ci,
            @Share("originalLight") LocalIntRef originalLight
    ) {
        numRendered++;

        // Check if we can reuse vertex lights
        assert Minecraft.getInstance().level != null;
        LevelLightEngine lightEngine = Minecraft.getInstance().level.getLightEngine();

        BlockPos blockPos = mapRenderState.getBlockPos();
        ItemFrame itemFrame = mapRenderState.getItemFrame();

        // Skip all this if the map isn't in an item frame
        // Also prevents crash if a mod doesn't respect in frame bool
        // Reported by craftish37 (https://github.com/barnabwhy/SmoothMaps/issues/1)
        if (!bl || itemFrame == null) {
            shouldReuseVertexLights = false;
            shouldSmoothLight = false;
            return;
        }

        shouldReuseVertexLights = ((LightUpdateAccessor)lightEngine).getLastUpdated() <= itemFrame.getLastUpdated()
                && blockPos.equals(itemFrame.getLastBlockPos())
                && itemFrame.getDirection().equals(itemFrame.getLastDirection())
                && itemFrame.getRotation() == itemFrame.getLastRotation();

        // Don't smooth light held maps or glow frames
        shouldSmoothLight = shouldSmoothLight(mapRenderState, shouldReuseVertexLights);
        if (!shouldSmoothLight)
            return;

        originalLight.set(i);

        if (shouldReuseVertexLights) {
            return;
        }

        numRelit++;
        itemFrame.setLastUpdated(Minecraft.getInstance().gameRenderer.getLastRenderTime());
        itemFrame.setLastBlockPos(blockPos);
        itemFrame.setLastRotation(itemFrame.getRotation());
        itemFrame.setLastDirection(itemFrame.getDirection());

        // Get light levels for surrounding blocks
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) {
                        lightLevels[x+1][y+1][z+1] = i;
                        continue;
                    }

                    BlockPos pos = blockPos.offset(x, y, z);

                    int light = 0;
                    if (lightEngine.skyEngine != null) {
                        int skyLight = lightEngine.skyEngine.getLightValue(pos);
                        light += (skyLight * 16) << 16;
                    }

                    if (lightEngine.blockEngine != null) {
                        int blockLight = lightEngine.blockEngine.getLightValue(pos);
                        light += blockLight * 16;
                    }

                    lightLevels[x+1][y+1][z+1] = light;
                }
            }
        }

        // Loop through all queried light levels
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    // Unpack light
                    int light = lightLevels[x+1][y+1][z+1];
                    int skyLight = light >> 16;
                    int blockLight = light & 0xFFFF;

                    // If were in a solid block
                    if (blockLight == 0 && skyLight == 0) {
                        int maxSky = 0;
                        int maxBlock = 0;

                        // Loop through 3x3 cube surrounding this block
                        for (int x2 = x-1; x2 <= x+1; x2++) {
                            for (int y2 = y-1; y2 <= y+1; y2++) {
                                for (int z2 = z-1; z2 <= z+1; z2++) {
                                    // If this block skip
                                    if (x2 == x && y2 == y && z2 ==z)
                                        continue;

                                    // Check if the pos is in our original 3x3 bounds
                                    if (x2 >= -1 && x2 <= 1 && y2 >= -1 && y2 <= 1 && z2 >= -1 && z2 <= 1) {
                                        // Calculate taxicab distance
                                        int dist = Math.abs(x2-x) + Math.abs(y2-y) + Math.abs(z2-z);

                                        int otherLight = lightLevels[x2+1][y2+1][z2+1];

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

                        light = (skyLight << 16) + blockLight;
                        lightLevels[x+1][y+1][z+1] = light;
                    }
                }
            }
        }
    }

    @Redirect(
            method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitCustomGeometry(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/RenderType;Lnet/minecraft/client/renderer/SubmitNodeCollector$CustomGeometryRenderer;)V", ordinal = 0)
    )
    private void submitMapGeometry(
            SubmitNodeCollector instance, PoseStack poseStack, RenderType renderType, SubmitNodeCollector.CustomGeometryRenderer customGeometryRenderer,
            @Local(ordinal = 0, argsOnly = true) int light,
            @Local(argsOnly = true) MapRenderState mapRenderState
    ) {
        int[] lights = { light, light, light, light };

        if (shouldSmoothLight) {
            int[] vertexLights = mapRenderState.getItemFrame().getVertLights();

            for (int i = 0; i < 4; i++) {
                int rotatedVertNum = switch (mapRenderState.rotation()) {
                    default -> i;
                    case 1, 5 -> vertexRotationMap[0][i];
                    case 2, 6 -> vertexRotationMap[1][i];
                    case 3, 7 -> vertexRotationMap[2][i];
                };

                if (!shouldReuseVertexLights) {
                    float f = xPositions[i];
                    float g = yPositions[i];

                    float xInBlock = switch(mapRenderState.rotation()) {
                        case 0, 4 -> f;
                        case 1, 5 -> 1.0f - g;
                        case 2, 6 -> 1.0f - f;
                        case 3, 7 -> g;
                        default -> 0.0f;
                    };

                    float yInBlock = switch(mapRenderState.rotation()) {
                        case 0, 4 -> g;
                        case 1, 5 -> f;
                        case 2, 6 -> 1.0f - g;
                        case 3, 7 -> 1.0f - f;
                        default -> 0.0f;
                    };

                    int lightVal = getLight(lightLevels, xInBlock, yInBlock, mapRenderState.direction());
                    vertexLights[rotatedVertNum] = lightVal;
                }

                lights[i] = vertexLights[rotatedVertNum];
            }

            if (!shouldReuseVertexLights)
                mapRenderState.getItemFrame().setVertLights(vertexLights);
        }

        instance.submitCustomGeometry(poseStack, renderType, (pose, vertexConsumer) -> {
            vertexConsumer.addVertex(pose, 0.0F, 128.0F, -0.01F).setColor(-1).setUv(0.0F, 1.0F).setLight(lights[0]);
            vertexConsumer.addVertex(pose, 128.0F, 128.0F, -0.01F).setColor(-1).setUv(1.0F, 1.0F).setLight(lights[1]);
            vertexConsumer.addVertex(pose, 128.0F, 0.0F, -0.01F).setColor(-1).setUv(1.0F, 0.0F).setLight(lights[2]);
            vertexConsumer.addVertex(pose, 0.0F, 0.0F, -0.01F).setColor(-1).setUv(0.0F, 0.0F).setLight(lights[3]);
        });
    }

//    @Inject(
//            method = "method_72918",
//            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;addVertex(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;FFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;", ordinal = 0)
//    )
//    private static void decorationsNormalLighting(
//            float f, TextureAtlasSprite textureAtlasSprite, int i, PoseStack.Pose pose, VertexConsumer vertexConsumer,
//            CallbackInfo ci,
//            @Local(ordinal = 0, argsOnly = true) LocalIntRef light,
//            @Share("shouldSmoothLight") LocalBooleanRef shouldSmoothLight,
//            @Share("originalLight") LocalIntRef originalLight
//    ) {
//        // Don't smooth light held maps or glow frames
//        if (!shouldSmoothLight.get())
//            return;
//
//        light.set(originalLight.get());
//    }

    @Unique
    private static int getLight(int[][][] blockLights, float x, float y, Direction direction) {
        int light = blockLights[1][1][1];

        int tl = light, tr = light, bl = light, br = light;

        // This block is tl
        if (x > 0.5f && y > 0.5f) {
            bl = getLightRelative(0, -1, direction, blockLights);
            tr = getLightRelative(1, 0, direction, blockLights);
            br = getLightRelative(1, -1, direction, blockLights);
        }

        // This block is tr
        if (x < 0.5f && y > 0.5f) {
            tl = getLightRelative(-1, 0, direction, blockLights);
            bl = getLightRelative(-1, -1, direction, blockLights);
            br = getLightRelative(0, -1, direction, blockLights);
        }

        // This block is bl
        if (x > 0.5f && y < 0.5f) {
            tl = getLightRelative(0, 1, direction, blockLights);
            tr = getLightRelative(1, 1, direction, blockLights);
            br = getLightRelative(1, 0, direction, blockLights);
        }

        // This block is br
        if (x < 0.5f && y < 0.5f) {
            tl = getLightRelative(-1, 1, direction, blockLights);
            bl = getLightRelative(-1, 0, direction, blockLights);
            tr = getLightRelative(0, 1, direction, blockLights);
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
    private static int getLightRelative(int xStep, int yStep, Direction dir, int[][][] blockLights) {
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

        BlockPos.MutableBlockPos pos = BlockPos.ZERO.mutable();

        if (xStep > 0)
            pos.move(rightDir, 1);

        if (xStep < 0)
            pos.move(leftDir, 1);

        if (yStep > 0)
            pos.move(upDir, 1);

        if (yStep < 0)
            pos.move(downDir, 1);

        return blockLights[pos.getX()+1][pos.getY()+1][pos.getZ()+1];
    }
}
