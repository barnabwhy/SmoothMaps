package cc.barnab.smoothmaps.client;

import net.minecraft.world.entity.decoration.Painting;

public interface PaintingStateAccessor {
    default Painting getPainting() {
        return null;
    }
    default void setPainting(Painting painting) {
    }
}
