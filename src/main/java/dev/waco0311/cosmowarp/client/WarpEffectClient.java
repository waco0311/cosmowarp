package dev.waco0311.cosmowarp.client;

import dev.waco0311.cosmowarp.Config;
import dev.waco0311.cosmowarp.network.WarpEffectPayload;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks every Warp Drive currently charging within range of this client. The actual drawing
 * happens in HyperspaceGLRenderer (raw OpenGL, not Mojang's ShaderInstance/PostChain -- see that
 * class for why), which just checks isActive()/getChargeProgress() each frame.
 *
 * activeSources is keyed by each Warp Drive's own block position and works as a simple reference
 * count: a source is "on" until its own matching STOP arrives. This depends on every START having
 * a guaranteed matching STOP eventually -- see clear() and the registerSafetyNet() listeners below
 * for what happens if that guarantee is ever broken by something outside this class (e.g. a lost
 * packet, or a server-side bug that forgets who it owes a STOP to).
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

    /**
     * Hard-resets all tracked charge sources, regardless of how many are currently active.
     * Used by the automatic safety net below, and exposed for the /cosmowarp clearhyperspace
     * escape-hatch command.
     */
    public static void clear() {
        activeSources.clear();
        effectStartNanos = -1L;
    }

    /**
     * Call once during client setup. Forces the effect off whenever the client's player instance
     * is torn down -- respawn, dimension change (these share the same underlying event), or
     * disconnect -- as a last-resort safety net in case some source's STOP never arrives. This
     * does NOT fix a missing STOP by itself; it only guarantees the visual can't survive past the
     * moment the player leaves the situation entirely, instead of persisting into an unrelated
     * world or session.
     */
    public static void registerSafetyNet() {
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.Clone event) -> clear());
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> clear());
    }
}
