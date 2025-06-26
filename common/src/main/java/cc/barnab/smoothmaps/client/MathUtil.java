package cc.barnab.smoothmaps.client;

import org.spongepowered.asm.mixin.Unique;

public class MathUtil {
    public static int midOf4(int tl, int tr, int bl, int br) {
        return (tl + tr +bl + br) / 4;
    }

    public static int bilinearInterp(float x, float y, int tl, int tr, int bl, int br) {
        float t = (float)tl + (float)(tr - tl) * x;
        float b = (float)bl + (float)(br - bl) * x;
        return (int)(t + (b - t) * y);
    }
}
