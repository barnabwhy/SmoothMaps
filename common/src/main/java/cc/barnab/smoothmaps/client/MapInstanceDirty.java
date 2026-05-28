package cc.barnab.smoothmaps.client;

public interface MapInstanceDirty {
    default void setDirty(int x, int y, int w, int h) {
    }
}
