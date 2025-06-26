package cc.barnab.smoothmaps.mixin.client.map;

import cc.barnab.smoothmaps.client.ItemFrameStateAccessor;
import net.minecraft.client.renderer.entity.state.ItemFrameRenderState;
import net.minecraft.world.entity.decoration.ItemFrame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ItemFrameRenderState.class)
public class ItemFrameRenderStateMixin implements ItemFrameStateAccessor {
    @Unique
    public ItemFrame itemFrame;

    @Override
    public ItemFrame getItemFrame() {
        return itemFrame;
    }
    @Override
    public void setItemFrame(ItemFrame itemFrame) {
        this.itemFrame = itemFrame;
    }

}
