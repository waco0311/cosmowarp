package dev.waco0311.cosmowarp.registry;

import dev.waco0311.cosmowarp.Cosmonauticswarpdrive;
import dev.waco0311.cosmowarp.block.entity.CrystalDriverBlockEntity;
import dev.waco0311.cosmowarp.block.entity.WarpDriveBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Cosmonauticswarpdrive.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WarpDriveBlockEntity>> WARP_DRIVE =
            BLOCK_ENTITIES.register("warp_drive", () -> new BlockEntityType<>(
                    (pos, state) -> new WarpDriveBlockEntity(ModBlockEntities.WARP_DRIVE.get(), pos, state),
                    java.util.Set.of(ModBlocks.WARP_DRIVE.get()), null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrystalDriverBlockEntity>> CRYSTAL_DRIVER =
            BLOCK_ENTITIES.register("crystal_driver", () -> new BlockEntityType<>(
                    (pos, state) -> new CrystalDriverBlockEntity(ModBlockEntities.CRYSTAL_DRIVER.get(), pos, state),
                    java.util.Set.of(ModBlocks.CRYSTAL_DRIVER.get()), null));

    public static void register(net.neoforged.bus.api.IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
    }
}