package cc.barnab.smoothmaps.mixin.client;

import cc.barnab.smoothmaps.client.SmoothMapsDebugEntry;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(DebugScreenEntries.class)
public abstract class DebugScreenEntriesMixin {

    @Shadow
    private static Identifier register(Identifier arg, DebugScreenEntry arg2) {
        return null;
    }

    @Unique
    private static final Identifier SMOOTHMAPS_STATS =
            register(Identifier.fromNamespaceAndPath("smoothmaps", "stats"), new SmoothMapsDebugEntry());
}
