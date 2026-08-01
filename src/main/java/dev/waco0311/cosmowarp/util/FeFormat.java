package dev.waco0311.cosmowarp.util;

/** Shared FE number formatting: short-form (12.3M / 1.2B) for tight display spaces like the
 * Warp Drive's console screen; the GUI's own FE bar still uses full comma-separated numbers. */
public final class FeFormat {

    private FeFormat() {}

    public static String shortForm(long value) {
        if (value >= 1_000_000_000L) {
            return String.format("%.1fB FE", value / 1_000_000_000.0);
        }
        if (value >= 1_000_000L) {
            return String.format("%.1fM FE", value / 1_000_000.0);
        }
        if (value >= 1_000L) {
            return String.format("%.1fK FE", value / 1_000.0);
        }
        return value + " FE";
    }
}
