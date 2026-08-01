package dev.waco0311.cosmowarp;

import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue LOG_DIRT_BLOCK = BUILDER
            .comment("Whether to log the dirt block on common setup")
            .define("logDirtBlock", true);

    public static final ModConfigSpec.LongValue WARP_COST_FE = BUILDER
            .comment("FE consumed by a single Warp Drive activation")
            .defineInRange("warpCostFE", 8_000_000L, 0L, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue WARP_CHARGE_TICKS = BUILDER
            .comment("Ticks between pressing Warp and the actual jump (hyperspace charge-up). 20 ticks = 1 second")
            .defineInRange("warpChargeTicks", 60, 0, 24000);

    public static final ModConfigSpec.IntValue WARP_PARTICLE_COUNT = BUILDER
            .comment("Number of converging particles spawned around the ship per tick while charging")
            .defineInRange("warpParticleCount", 32, 0, 256);

    // --- Test Drive (working name; inertial/momentum-triggered warp, no crystal, FE only) ---
    public static final ModConfigSpec.LongValue TEST_DRIVE_COST_FE = BUILDER
            .comment("FE consumed by a single Test Drive activation")
            .defineInRange("testDriveCostFE", 8_000_000L, 0L, Integer.MAX_VALUE);

    // --- Warp Drive console display (liquid-crystal panel text) ---
    public enum DisplayMode { FE, COUNTDOWN, COORDINATES, POINT_NAME }

    public static final ModConfigSpec.EnumValue<DisplayMode> WARP_DRIVE_DISPLAY_MODE = BUILDER
            .comment("What the Warp Drive's console screen shows")
            .defineEnum("warpDriveDisplayMode", DisplayMode.FE);

    public static final ModConfigSpec.DoubleValue TEST_DRIVE_MIN_SPEED = BUILDER
            .comment("Minimum ship speed (blocks/tick) required, while armed, for a redstone pulse to trigger a Test Drive warp")
            .defineInRange("testDriveMinSpeed", 0.5, 0.0, 100.0);

    // --- Memory Card (cheap, non-Moon-derived recording medium; can register/store points but
    // cannot itself trigger a warp -- use a Crystal Driver to move points onto a real Warp Crystal) ---
    public static final ModConfigSpec.IntValue MEMORY_CARD_CAPACITY = BUILDER
            .comment("Max number of warp points a Memory Card can hold")
            .defineInRange("memoryCardCapacity", 5, 1, 10);

    public static final ModConfigSpec.IntValue MAGIC_NUMBER = BUILDER
            .comment("A magic number")
            .defineInRange("magicNumber", 42, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.ConfigValue<String> MAGIC_NUMBER_INTRODUCTION = BUILDER
            .comment("What you want the introduction message to be for the magic number")
            .define("magicNumberIntroduction", "The magic number is... ");

    public static final ModConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS = BUILDER
            .comment("A list of items to log on common setup.")
            .defineListAllowEmpty("items", List.of("minecraft:iron_ingot"), () -> "", Config::validateItemName);

    static final ModConfigSpec SPEC = BUILDER.build();

    private static boolean validateItemName(final Object obj) {
        return obj instanceof String itemName && BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(itemName));
    }
}