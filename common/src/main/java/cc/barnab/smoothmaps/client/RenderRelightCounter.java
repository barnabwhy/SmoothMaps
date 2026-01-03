package cc.barnab.smoothmaps.client;

public interface RenderRelightCounter {
    default int getNumRendered() {
        return 0;
    }
    default int getNumSmoothLit() {
        return 0;
    }
    default int getNumRelit() {
        return 0;
    }
    default void resetCounters() {
    }
}
