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

    public static float[] getUVs(Object renderState) {
        if (!available || !renderStateClass.isInstance(renderState)) return null;

        try {
            Object atlas = getAtlasTexture.invoke(renderState);
            if (atlas == null) return null;

            int x = (int) getAtlasX.invoke(renderState);
            int y = (int) getAtlasY.invoke(renderState);

            float u1 = (float)x / atlasSize;
            float u2 = (float)(x + mapSize) / atlasSize;
            float v1 = (float)y / atlasSize;
            float v2 = (float)(y + mapSize) / atlasSize;

            return new float[]{u1, v1, u2, v2};
        } catch (Throwable ignored) {
            return null;
        }
    }

    private ImmediatelyFastCompat() {}
}
