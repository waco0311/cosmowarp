package dev.waco0311.cosmowarp.registry;

import dev.waco0311.cosmowarp.Cosmonauticswarpdrive;
import dev.waco0311.cosmowarp.block.CrystalDriverBlock;
import dev.waco0311.cosmowarp.block.WarpDriveBlock;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Cosmonauticswarpdrive.MODID);

    // Ore: mined by hand or pickaxe, drops raw_moon_crystal (loot table), small xp.
    public static final DeferredBlock<Block> MOON_CRYSTAL_ORE = BLOCKS.register("moon_crystal_ore",
            () -> new DropExperienceBlock(UniformInt.of(2, 5), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .requiresCorrectToolForDrops()
                    .strength(4.5f, 6f)));

    // Directional console block, placeholder shape/appearance (Contraption Controls stand-in).
    // Tool tier (iron pickaxe) is gated via tags/block/needs_iron_tool.json.
    public static final DeferredBlock<WarpDriveBlock> WARP_DRIVE = BLOCKS.register("warp_drive",
            () -> new WarpDriveBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops()
                    .strength(5f, 6f)));

    public static final DeferredBlock<CrystalDriverBlock> CRYSTAL_DRIVER = BLOCKS.register("crystal_driver",
            () -> new CrystalDriverBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops()
                    .strength(5f, 6f)));

    public static void register(net.neoforged.bus.api.IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
