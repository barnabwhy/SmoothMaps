package cc.barnab.smoothmaps.mixin.client;

import cc.barnab.smoothmaps.SmoothMaps;
import cc.barnab.smoothmaps.mixin.client.map.MapRendererMixin;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.client.renderer.MapRenderer;
import net.minecraft.client.renderer.entity.PaintingRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.Painting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(DebugScreenOverlay.class)
public class DebugScreenOverlayMixin {
    @Unique
    private static final String VERSION_STR = "SmoothMaps v%s".formatted(SmoothMaps.MOD_VERSION);

    @ModifyReturnValue(method = "getGameInformation", at = @At("RETURN"))
    List<String> getGameInformation(List<String> lines) {
        lines.add("");

        lines.add(VERSION_STR);
        if (Minecraft.useAmbientOcclusion()) {
            MapRenderer mapRenderer = Minecraft.getInstance().getMapRenderer();
            lines.add("Framed maps: " + mapRenderer.getNumRendered() + " (" + mapRenderer.getNumRelit() + " relit)");

            PaintingRenderer paintingRenderer = (PaintingRenderer) Minecraft.getInstance().getEntityRenderDispatcher().renderers.get(EntityType.PAINTING);
            if (paintingRenderer != null) {
                lines.add("Paintings: " + paintingRenderer.getNumRendered() + " (" + paintingRenderer.getNumRelit() + " relit)");
            }
        } else {
            lines.add("Smooth lighting disabled.");
        }

        return lines;
    }
}
