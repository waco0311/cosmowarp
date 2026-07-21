package dev.waco0311.cosmowarp.client;

import dev.waco0311.cosmowarp.Config;
import dev.waco0311.cosmowarp.network.WarpEffectPayload;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks every Warp Drive currently charging within range of this client. The actual drawing
 * happens in HyperspaceGLRenderer (raw OpenGL, not Mojang's ShaderInstance/PostChain -- see that
 * class for why), which just checks isActive()/getChargeProgress() each frame.
 */
public class WarpEffectClient {

    private static final Set<BlockPos> activeSources = ConcurrentHashMap.newKeySet();
    private static long effectStartNanos = -1L;

    public static void handle(WarpEffectPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            boolean wasEmpty = activeSources.isEmpty();
            if (payload.active()) {
                activeSources.add(payload.sourcePos());
                if (wasEmpty) {
                    effectStartNanos = System.nanoTime();
                }
            } else {
                activeSources.remove(payload.sourcePos());
                if (activeSources.isEmpty()) {
                    effectStartNanos = -1L;
                }
            }
        });
    }

    public static boolean isActive() {
        return !activeSources.isEmpty();
    }

    /** 0.0 right when the charge starts, ramping to 1.0 as it approaches Config.WARP_CHARGE_TICKS. */
    public static float getChargeProgress() {
        if (effectStartNanos < 0) return 0f;
        float elapsedSeconds = (System.nanoTime() - effectStartNanos) / 1_000_000_000f;
        float totalSeconds = Math.max(0.05f, Config.WARP_CHARGE_TICKS.get() / 20f);
        return Math.min(1f, elapsedSeconds / totalSeconds);
    }
}