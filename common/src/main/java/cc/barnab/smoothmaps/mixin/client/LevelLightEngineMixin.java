package cc.barnab.smoothmaps.mixin.client;

import cc.barnab.smoothmaps.client.LightUpdateTracker;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Util;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(LevelLightEngine.class)
public class LevelLightEngineMixin {
    @Unique
    private final LongSet modifiedChunks = new LongOpenHashSet();

    @Inject(method = "queueSectionData", at = @At("RETURN"))
    private void onQueueData(LightLayer layer, SectionPos pos, DataLayer data, CallbackInfo ci) {
        if (data != null) {
            this.modifiedChunks.add(pos.chunk().toLong());
        }
    }

    @Inject(method = "checkBlock", at = @At("HEAD"))
    private void onCheckBlock(BlockPos pos, CallbackInfo ci) {
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;


        // Add center and all 8 neighbors (North, South, East, West, and Diagonals)
        this.modifiedChunks.add(ChunkPos.asLong(cx, cz));
        this.modifiedChunks.add(ChunkPos.asLong(cx + 1, cz));
        this.modifiedChunks.add(ChunkPos.asLong(cx - 1, cz));
        this.modifiedChunks.add(ChunkPos.asLong(cx, cz + 1));
        this.modifiedChunks.add(ChunkPos.asLong(cx, cz - 1));
        this.modifiedChunks.add(ChunkPos.asLong(cx + 1, cz + 1));
        this.modifiedChunks.add(ChunkPos.asLong(cx + 1, cz - 1));
        this.modifiedChunks.add(ChunkPos.asLong(cx - 1, cz + 1));
        this.modifiedChunks.add(ChunkPos.asLong(cx - 1, cz - 1));
    }

    @Inject(method = "runLightUpdates", at = @At("RETURN"))
    private void onUpdatesComplete(CallbackInfoReturnable<Integer> cir) {
        if (!this.modifiedChunks.isEmpty()) {
            for (long chunkPos : this.modifiedChunks) {
                LightUpdateTracker.setLastUpdated(chunkPos, Util.getNanos());
            }

            // Clear the set so we don't re-process them next tick
            this.modifiedChunks.clear();
        }
    }
}
