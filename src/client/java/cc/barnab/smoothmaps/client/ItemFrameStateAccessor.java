package cc.barnab.smoothmaps.client;

import net.minecraft.world.entity.decoration.ItemFrame;

public interface ItemFrameStateAccessor {
    default ItemFrame getItemFrame() {
        return null;
    }
    default void setItemFrame(ItemFrame itemFrame) {
    }
}
