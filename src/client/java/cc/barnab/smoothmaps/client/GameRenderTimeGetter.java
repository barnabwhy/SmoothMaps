package cc.barnab.smoothmaps.client;

public interface GameRenderTimeGetter {
    default long getLastRenderTime() {
        return 0L;
    }
}
