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
            .defineInRange("warpCostFE", 2_000_000L, 0L, Integer.MAX_VALUE);

    // --- Warp Drive console display (liquid-crystal panel text) ---
    public enum DisplayMode { FE, COUNTDOWN, COORDINATES, POINT_NAME }

    public static final ModConfigSpec.EnumValue<DisplayMode> WARP_DRIVE_DISPLAY_MODE = BUILDER
            .comment("What the Warp Drive's console screen shows")
            .defineEnum("warpDriveDisplayMode", DisplayMode.FE);

    public static final ModConfigSpec.DoubleValue TEST_DRIVE_MIN_SPEED = BUILDER
            .comment("Minimum ship speed (blocks/tick) required, while armed, for a redstone pulse to trigger a Test Drive warp")
            .defineInRange("testDriveMinSpeed", 0.5, 0.0, 100.0);

    public static final ModConfigSpec.DoubleValue TEST_DRIVE_THROTTLE_FRACTION = BUILDER
            .comment("Fraction of the ship's own propulsion force allowed through while a Test Drive is armed; the rest is cancelled every physics substep, producing the slow \"crouching start\" drift while charging")
            .defineInRange("testDriveThrottleFraction", 1.0 / 30.0, 0.0, 1.0);

    public static final ModConfigSpec.IntValue TEST_DRIVE_MAX_CHARGE_TICKS = BUILDER
            .comment("Charge cap in ticks; jump distance scales linearly from 0 up to testDriveMaxJumpDistance as charge approaches this")
            .defineInRange("testDriveMaxChargeTicks", 200, 1, 24000);

    public static final ModConfigSpec.IntValue TEST_DRIVE_FIRE_DELAY_TICKS = BUILDER
            .comment("Delay (ticks) between the top-face signal being detected and the jump actually executing. Kept longer than a vanilla button's own auto-reset time (stone: 20 ticks, wood: 30 ticks) on purpose, so the button has already popped back up on its own before the structure (and the button along with it) gets relocated -- otherwise the button's own \"pop back up\" schedule gets lost in the move and it stays stuck pressed. Also doubles as a natural place to eventually hang a charge-up visual effect.")
            .defineInRange("testDriveFireDelayTicks", 40, 0, 24000);

    public static final ModConfigSpec.DoubleValue TEST_DRIVE_MAX_JUMP_DISTANCE = BUILDER
            .comment("Distance in blocks covered by a fully-charged Test Drive jump")
            .defineInRange("testDriveMaxJumpDistance", 500.0, 1.0, 1_000_000.0);

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
