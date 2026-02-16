package cc.barnab.smoothmaps.mixin.client.map;

import cc.barnab.smoothmaps.client.LightUpdateTracker;
import cc.barnab.smoothmaps.client.MathUtil;
import cc.barnab.smoothmaps.client.RenderRelightCounter;
import cc.barnab.smoothmaps.compat.ImmediatelyFastCompat;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.lighting.LayerLightEventListener;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MapRenderer.class, priority = 1001)
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
    private int numSmoothLit = 0;
    @Unique
    private int numRelit = 0;

    @Override
    public int getNumRendered() {
        return numRendered;
    }
    @Override
    public int getNumSmoothLit() {
        return numSmoothLit;
    }
    @Override
    public int getNumRelit() {
        return numRelit;
    }

    @Override
    public void resetCounters() {
        numRendered = 0;
        numSmoothLit = 0;
        numRelit = 0;
    }

    @Unique
    private boolean shouldRender(MapRenderState mapRenderState) {
        // Optimisations to disable rendering maps in situations you wouldn't see the map
        GameRenderer gameRenderer = Minecraft.getInstance().gameRenderer;
        Camera mainCamera = gameRenderer.getMainCamera();
        Vector3fc camLook = mainCamera.forwardVector();

        // Is the camera behind the frame?
        Direction.Axis axis = mapRenderState.direction().getAxis();
        Direction.AxisDirection axisDir = mapRenderState.direction().getAxisDirection();
        boolean isCamBehind = switch (axis) {
            case X -> (mapRenderState.getBlockPos().getX() + 0.5f - mainCamera.position().x) * axisDir.getStep() > 0.5f;
            case Y -> (mapRenderState.getBlockPos().getY() + 0.5f - mainCamera.position().y) * axisDir.getStep() > 0.5f;
            case Z -> (mapRenderState.getBlockPos().getZ() + 0.5f - mainCamera.position().z) * axisDir.getStep() > 0.5f;
        };

        if (isCamBehind)
            return false;

        // Is the camera facing away from the frame?
        float clampedFovModifier = Math.max(Math.max(gameRenderer.fovModifier, gameRenderer.oldFovModifier), 1f);
        float fovVert = (float) Math.toRadians((float)Minecraft.getInstance().options.fov().get() * clampedFovModifier);
        float fovHoriz = fovVert * (float)Minecraft.getInstance().getWindow().getWidth() / (float)Minecraft.getInstance().getWindow().getHeight();

        float threshold = (float) Math.cos(Math.PI - fovHoriz);

        Vector3f step = mapRenderState.direction().step(); // axis-aligned unit vector
        float dot = step.x * camLook.x()
                  + step.y * camLook.y()
                  + step.z * camLook.z();

        if (dot > threshold)
            return false;

        return true;
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void render(
            MapRenderState mapRenderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, boolean bl, int i,
            CallbackInfo ci,
            @Share("originalLight") LocalIntRef originalLight
    ) {
        // Skip all this if the map isn't in an item frame
        // Also prevents crash if a mod doesn't respect in frame bool
        // Reported by craftish37 (https://github.com/barnabwhy/SmoothMaps/issues/1)
        ItemFrame itemFrame = mapRenderState.getItemFrame();
        if (!bl || itemFrame == null) {
            shouldReuseVertexLights = false;
            shouldSmoothLight = false;
            return;
        }

        // Call this after because we only want to count those in item frames
        numRendered++;

        // Skip smooth lighting glow item frames or if smooth lighting is disabled
        if (mapRenderState.isGlowing() || !Minecraft.useAmbientOcclusion()) {
            shouldSmoothLight = false;
            return;
        }

        shouldSmoothLight = true;

        // Check if we can reuse vertex lights
        assert Minecraft.getInstance().level != null;
        LevelLightEngine lightEngine = Minecraft.getInstance().level.getLightEngine();

        BlockPos blockPos = mapRenderState.getBlockPos();

        shouldReuseVertexLights = LightUpdateTracker.getLastUpdated(blockPos) <= itemFrame.getLastUpdated()
                && blockPos.equals(itemFrame.getLastBlockPos())
                && itemFrame.getDirection().equals(itemFrame.getLastDirection())
                && itemFrame.getRotation() == itemFrame.getLastRotation();

        // If we can skip rendering, we should
        // Skip checks if reusing lighting to save CPU
        if (!shouldReuseVertexLights && !shouldRender(mapRenderState)) {
            numRendered--;
            ci.cancel();
            return;
        }

        numSmoothLit++;

        originalLight.set(i);

        if (shouldReuseVertexLights) {
            return;
        }

        numRelit++;
        itemFrame.setLastUpdated(Minecraft.getInstance().gameRenderer.getLastRenderTime());
        itemFrame.setLastBlockPos(blockPos);
        itemFrame.setLastRotation(itemFrame.getRotation());
        itemFrame.setLastDirection(itemFrame.getDirection());

        LayerLightEventListener skyListener = lightEngine.getLayerListener(LightLayer.SKY);
        LayerLightEventListener blockListener = lightEngine.getLayerListener(LightLayer.BLOCK);

        // Get light levels for surrounding blocks
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) {
                        lightLevels[x+1][y+1][z+1] = i;
                        continue;
                    }

                    pos.set(blockPos.getX() + x, blockPos.getY() + y, blockPos.getZ() + z);

                    int skyLight = skyListener.getLightValue(pos);
                    int light = (skyLight * 16) << 16;

                    int blockLight = blockListener.getLightValue(pos);
                    light += blockLight * 16;

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

    @Unique
    final float[] imFastUvs = { 0f, 0f, 0f, 0f };

    @WrapWithCondition(
            method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitCustomGeometry(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/SubmitNodeCollector$CustomGeometryRenderer;)V", ordinal = 0)
    )
    private boolean submitMapGeometry(
            SubmitNodeCollector instance, PoseStack poseStack, RenderType renderType, SubmitNodeCollector.CustomGeometryRenderer customGeometryRenderer, @Local(ordinal = 0, argsOnly = true) int light, @Local(argsOnly = true) MapRenderState mapRenderState
    ) {
        if (!shouldSmoothLight)
            return true;

        int l0 = light, l1 = light, l2 = light, l3 = light;

        int[] vertexLights = mapRenderState.getItemFrame().getVertLights();

        for (int i = 0; i < 4; i++) {
            int rotatedVertNum = switch (mapRenderState.rotation()) {
                case 1, 5 -> vertexRotationMap[0][i];
                case 2, 6 -> vertexRotationMap[1][i];
                case 3, 7 -> vertexRotationMap[2][i];
                default -> i;
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

            switch (i) {
                case 0 -> l0 = vertexLights[rotatedVertNum];
                case 1 -> l1 = vertexLights[rotatedVertNum];
                case 2 -> l2 = vertexLights[rotatedVertNum];
                case 3 -> l3 = vertexLights[rotatedVertNum];
            }
        }

        if (!shouldReuseVertexLights)
            mapRenderState.getItemFrame().setVertLights(vertexLights);

        final int fl0 = l0, fl1 = l1, fl2 = l2, fl3 = l3;

        // ImmediatelyFast messes with the UVs of maps by putting them into an atlas, lovely
        // We need to mimic its behaviour otherwise our map will render the entire map atlas :(
        if (ImmediatelyFastCompat.isAvailable()) {
            ImmediatelyFastCompat.getUVs(mapRenderState, imFastUvs);
            if (imFastUvs != null) {
                float u1 = imFastUvs[0];
                float v1 = imFastUvs[1];
                float u2 = imFastUvs[2];
                float v2 = imFastUvs[3];

                instance.submitCustomGeometry(poseStack, renderType, (pose, vertexConsumer) -> {
                    vertexConsumer.addVertex(pose, 0.0F, 128.0F, -0.01F).setColor(-1).setUv(u1, v2).setLight(fl0);
                    vertexConsumer.addVertex(pose, 128.0F, 128.0F, -0.01F).setColor(-1).setUv(u2, v2).setLight(fl1);
                    vertexConsumer.addVertex(pose, 128.0F, 0.0F, -0.01F).setColor(-1).setUv(u2, v1).setLight(fl2);
                    vertexConsumer.addVertex(pose, 0.0F, 0.0F, -0.01F).setColor(-1).setUv(u1, v1).setLight(fl3);
                });

                return false;
            }
        }

        instance.submitCustomGeometry(poseStack, renderType, (pose, vertexConsumer) -> {
            vertexConsumer.addVertex(pose, 0.0F, 128.0F, -0.01F).setColor(-1).setUv(0.0F, 1.0F).setLight(fl0);
            vertexConsumer.addVertex(pose, 128.0F, 128.0F, -0.01F).setColor(-1).setUv(1.0F, 1.0F).setLight(fl1);
            vertexConsumer.addVertex(pose, 128.0F, 0.0F, -0.01F).setColor(-1).setUv(1.0F, 0.0F).setLight(fl2);
            vertexConsumer.addVertex(pose, 0.0F, 0.0F, -0.01F).setColor(-1).setUv(0.0F, 0.0F).setLight(fl3);
        });
        return false;
    }

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

        int dx = 0, dy = 0, dz = 0;

        if (xStep > 0) {
            dx += rightDir.getStepX();
            dy += rightDir.getStepY();
            dz += rightDir.getStepZ();
        } else if (xStep < 0) {
            dx += leftDir.getStepX();
            dy += leftDir.getStepY();
            dz += leftDir.getStepZ();
        }

        if (yStep > 0) {
            dx += upDir.getStepX();
            dy += upDir.getStepY();
            dz += upDir.getStepZ();
        } else if (yStep < 0) {
            dx += downDir.getStepX();
            dy += downDir.getStepY();
            dz += downDir.getStepZ();
        }

        return blockLights[dx+1][dy+1][dz+1];
    }
}
