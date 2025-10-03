package cc.barnab.smoothmaps.mixin.client;

import cc.barnab.smoothmaps.client.SmoothMapsDebugEntry;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(DebugScreenEntries.class)
public abstract class DebugScreenEntriesMixin {
    @Unique
    private static final ResourceLocation SMOOTHMAPS_STATS = DebugScreenEntries.register("smoothmaps_stats", new SmoothMapsDebugEntry());
}
