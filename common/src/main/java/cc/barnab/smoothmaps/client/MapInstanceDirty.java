package cc.barnab.smoothmaps.client;

import net.minecraft.world.entity.decoration.Painting;

public interface MapInstanceDirty {
    default void setDirty(int x, int y, int w, int h) {
    }
}
