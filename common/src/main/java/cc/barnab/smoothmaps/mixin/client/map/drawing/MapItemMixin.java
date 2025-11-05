package cc.barnab.smoothmaps.mixin.client.map.drawing;

import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Purpose: Optimisations to map rendering (Server-side).
 * Gives performance gains of up to 60%.
 */
@Mixin(MapItem.class)
public class MapItemMixin {
    /**
     * Uses heightmap ocean floor data where possible to speed up water depth calculation.
     * Should improve from O(n) to O(1) in best case and remain at O(n) in worst.
     */
    @Inject(
            method = "update",
            at = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/core/BlockPos$MutableBlockPos;set(Lnet/minecraft/core/Vec3i;)Lnet/minecraft/core/BlockPos$MutableBlockPos;"
            ),
            slice = @Slice(
                from = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/FluidState;isEmpty()Z")
            )
    )
    private void shortCircuitWater(
            Level level, Entity entity, MapItemSavedData mapItemSavedData, CallbackInfo ci,
            @Local(name = "x") LocalIntRef x, @Local(name = "t") LocalIntRef t,
            @Local(name = "levelChunk") LevelChunk levelChunk, @Local(name = "mutableBlockPos") BlockPos.MutableBlockPos mutableBlockPos
    ) {
        // This should behave identically to the existing behaviour but
        // will be considerably faster on maps of oceans and rivers
        final int oceanFloor = levelChunk.getHeight(Heightmap.Types.OCEAN_FLOOR, mutableBlockPos.getX(), mutableBlockPos.getZ());
        if (oceanFloor <= x.get()) {
            // Since after this the do while will always run 1 iteration we
            // won't add 1 here, and leave that to the existing code
            t.set(t.get() + (x.get() - oceanFloor));
            x.set(Integer.MIN_VALUE);
        }
    }

    /**
     * Prevent copying multiset to array.
     * @see #getMapColor
     */
    @Redirect(
        method = "update",
        at = @At(
            value = "INVOKE",
            target = "Lcom/google/common/collect/Multisets;copyHighestCountFirst(Lcom/google/common/collect/Multiset;)Lcom/google/common/collect/ImmutableMultiset;",
            remap = false
        )
    )
    private <E> ImmutableMultiset<E> preventMultisetCopy(Multiset<E> multiset) {
        return null;
    }

    /**
     * Faster max count method.
     * The method Mojang use copiess the multiset to an array
     * and then sorts it, which wastes a bunch of processing
     * time since we only want the highest count value.
     */
    @SuppressWarnings("unchecked")
    @Redirect(
        method = "update",
        at = @At(
            value = "INVOKE",
            target = "Lcom/google/common/collect/Iterables;getFirst(Ljava/lang/Iterable;Ljava/lang/Object;)Ljava/lang/Object;",
            remap = false
        )
    )
    private <T> T getMapColor(Iterable<? extends T> iterable, T defaultValue, @Local Multiset<MapColor> multiset, @Local(name = "i") int i) {
        int maxCount = -1;
        int threshold = i*i/2;
        MapColor mapColor = MapColor.NONE;
        for (Multiset.Entry<MapColor> blockCount : multiset.entrySet()) {
            int val = blockCount.getCount();
            if (val > maxCount) {
                mapColor = blockCount.getElement();
                maxCount = val;

                if (val >= threshold)
                    return (T) mapColor;
            }
        }
        return (T) mapColor;
    }
}
