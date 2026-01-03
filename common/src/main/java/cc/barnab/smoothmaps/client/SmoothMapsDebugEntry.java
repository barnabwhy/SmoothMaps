package cc.barnab.smoothmaps.client;

import cc.barnab.smoothmaps.SmoothMaps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.client.renderer.MapRenderer;
import net.minecraft.client.renderer.entity.PaintingRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Unique;

public class SmoothMapsDebugEntry implements DebugScreenEntry {
    @Unique
    public static final Identifier GROUP = Identifier.withDefaultNamespace("smoothmaps_stats");

    @Unique
    private static final String VERSION_STR = "SmoothMaps v%s".formatted(SmoothMaps.MOD_VERSION);

    @Override
    public void display(DebugScreenDisplayer debugScreenDisplayer, @Nullable Level level, @Nullable LevelChunk levelChunk, @Nullable LevelChunk levelChunk2) {
        debugScreenDisplayer.addToGroup(GROUP, VERSION_STR);

        if (Minecraft.useAmbientOcclusion()) {
            MapRenderer mapRenderer = Minecraft.getInstance().getMapRenderer();
            debugScreenDisplayer.addToGroup(GROUP, "Framed maps: " + mapRenderer.getNumSmoothLit() + "/" + mapRenderer.getNumRendered() + " (" + mapRenderer.getNumRelit() + " relit)");

            PaintingRenderer paintingRenderer = (PaintingRenderer) Minecraft.getInstance().getEntityRenderDispatcher().renderers.get(EntityType.PAINTING);
            if (paintingRenderer != null) {
                debugScreenDisplayer.addToGroup(GROUP, "Paintings: " + paintingRenderer.getNumSmoothLit() + "/" + paintingRenderer.getNumRendered() + " (" + paintingRenderer.getNumRelit() + " relit)");
            }
        } else {
            debugScreenDisplayer.addToGroup(GROUP, "Smooth lighting disabled.");
        }
    }
}