package cc.barnab.smoothmaps.client;

public interface GameRenderTimeGetter {
    default long getLastRenderTime() {
        return 0L;
    }

    /**
     * Cached per-frame threshold for map culling.
     * This is cos(PI - fovHoriz) computed once per frame in GameRendererMixin.
     */
    default float getMapCullingDotThreshold() {
        return -1.0f;
    }
}
