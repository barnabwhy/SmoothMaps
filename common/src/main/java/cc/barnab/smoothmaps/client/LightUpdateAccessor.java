package cc.barnab.smoothmaps.client;

public interface LightUpdateAccessor {
    default long getLastUpdated() {
        return 0L;
    };
}
