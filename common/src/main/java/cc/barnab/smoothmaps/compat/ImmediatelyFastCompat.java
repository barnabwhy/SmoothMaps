package cc.barnab.smoothmaps.compat;

import java.lang.reflect.Method;

public final class ImmediatelyFastCompat {

    private static boolean available = false;

    private static Class<?> renderStateClass;
    private static Method getAtlasTexture;
    private static Method getAtlasX;
    private static Method getAtlasY;

    private static int atlasSize = 0;
    private static int mapSize = 0;

    static {
        try {
            renderStateClass = Class.forName("net.raphimc.immediatelyfast.injection.interfaces.IMapRenderState");

            getAtlasTexture = renderStateClass.getMethod("immediatelyFast$getAtlasTexture");
            getAtlasX = renderStateClass.getMethod("immediatelyFast$getAtlasX");
            getAtlasY = renderStateClass.getMethod("immediatelyFast$getAtlasY");

            Class<?> atlasClass = Class.forName(
                    "net.raphimc.immediatelyfast.feature.map_atlas_generation.MapAtlasTexture"
            );

            atlasSize = atlasClass.getField("ATLAS_SIZE").getInt(null);
            mapSize   = atlasClass.getField("MAP_SIZE").getInt(null);

            available = true;
        } catch (Throwable ignored) {
            // Mod not present; nothing to load
        }
    }

    public static boolean isAvailable() {
        return available;
    }

    public static boolean getUVs(Object renderState, float[] outUv4) {
        if (!available || !renderStateClass.isInstance(renderState)) return false;
        if (outUv4 == null || outUv4.length < 4) return false;

        try {
            Object atlas = getAtlasTexture.invoke(renderState);
            if (atlas == null) return false;

            int x = (int) getAtlasX.invoke(renderState);
            int y = (int) getAtlasY.invoke(renderState);

            float invAtlasSize = 1.0f / (float) atlasSize;
            float scaledMapSize = mapSize * invAtlasSize;

            float u1 = (float)x * invAtlasSize;
            float v1 = (float)y * invAtlasSize;

            outUv4[0] = u1;
            outUv4[1] = v1;
            outUv4[2] = u1 + scaledMapSize;
            outUv4[3] = v1 + scaledMapSize;
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private ImmediatelyFastCompat() {}
}
