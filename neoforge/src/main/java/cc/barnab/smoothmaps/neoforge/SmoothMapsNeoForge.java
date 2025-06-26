package cc.barnab.smoothmaps.neoforge;

import cc.barnab.smoothmaps.SmoothMaps;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;

@Mod(SmoothMaps.MOD_ID)
public final class SmoothMapsNeoForge {
    public SmoothMapsNeoForge() {
        ModContainer mod = ModList.get().getModContainerById(SmoothMaps.MOD_ID).orElseThrow(() -> new RuntimeException("Where is SmoothMaps???!"));

        SmoothMaps.init(String.valueOf(mod.getModInfo().getVersion()));
    }
}
