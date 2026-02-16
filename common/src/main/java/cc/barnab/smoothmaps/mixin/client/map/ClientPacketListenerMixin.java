package cc.barnab.smoothmaps.mixin.client.map;

import cc.barnab.smoothmaps.client.LightUpdateTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.resources.MapTextureManager;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacketData;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.util.Util;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
    @Redirect(method = "handleMapItemData", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/MapTextureManager;update(Lnet/minecraft/world/level/saveddata/maps/MapId;Lnet/minecraft/world/level/saveddata/maps/MapItemSavedData;)V"))
    private void updateMapTexture(MapTextureManager instance, MapId arg, MapItemSavedData arg2, ClientboundMapItemDataPacket packet) {
        MapTextureManager.MapInstance mapInstance = instance.getOrCreateMapInstance(arg, arg2);

        // Set dirty state of MapInstance for partial texture updates
        Optional<MapItemSavedData.MapPatch> patch = packet.colorPatch();
        patch.ifPresent(mapPatch -> mapInstance.setDirty(mapPatch.startX(), mapPatch.startY(), mapPatch.width(), mapPatch.height()));

        mapInstance.forceUpload();
    }
}
