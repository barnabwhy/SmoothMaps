package cc.barnab.smoothmaps;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

public class SmoothMaps implements ModInitializer {
    public static String MOD_VERSION;

    @Override
    public void onInitialize() {
        ModContainer mod = FabricLoader.getInstance()
                .getModContainer("smoothmaps")
                .orElseThrow(NullPointerException::new);

        MOD_VERSION = mod.getMetadata().getVersion().getFriendlyString();
    }
}
