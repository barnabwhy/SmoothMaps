package cc.barnab.smoothmaps.mixin.client;

import cc.barnab.smoothmaps.client.SmoothMapsDebugEntry;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(DebugScreenEntries.class)
public abstract class DebugScreenEntriesMixin {

    @Shadow
    private static ResourceLocation register(ResourceLocation arg, DebugScreenEntry arg2) {
        return null;
    }

    @Unique
    private static final ResourceLocation SMOOTHMAPS_STATS =
            register(ResourceLocation.fromNamespaceAndPath("smoothmaps", "stats"), new SmoothMapsDebugEntry());
}
