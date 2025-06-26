package cc.barnab.smoothmaps.fabric;

import cc.barnab.smoothmaps.SmoothMaps;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

public final class SmoothMapsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ModContainer mod = FabricLoader.getInstance()
                .getModContainer("smoothmaps")
                .orElseThrow(() -> new RuntimeException("Where is SmoothMaps???!"));

        SmoothMaps.init(mod.getMetadata().getVersion().getFriendlyString());
    }
}
