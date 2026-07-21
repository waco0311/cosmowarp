package dev.waco0311.cosmowarp.registry;

import dev.waco0311.cosmowarp.Cosmonauticswarpdrive;
import dev.waco0311.cosmowarp.data.WarpPoint;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.UUID;

public class ModDataComponents {

    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Cosmonauticswarpdrive.MODID);

    // Every warp point recorded on this Warp Crystal.
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<WarpPoint>>> WARP_POINTS =
            DATA_COMPONENTS.register("warp_points", () -> DataComponentType.<List<WarpPoint>>builder()
                    .persistent(WarpPoint.CODEC.listOf())
                    .networkSynchronized(WarpPoint.STREAM_CODEC.apply(ByteBufCodecs.list()))
                    .build());

    // Currently selected warp point id, drives the GUI's "selected point" panel.
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> SELECTED_WARP_POINT =
            DATA_COMPONENTS.register("selected_warp_point", () -> DataComponentType.<UUID>builder()
                    .persistent(UUIDUtil.CODEC)
                    .networkSynchronized(UUIDUtil.STREAM_CODEC)
                    .build());

    public static void register(net.neoforged.bus.api.IEventBus modEventBus) {
        DATA_COMPONENTS.register(modEventBus);
    }
}