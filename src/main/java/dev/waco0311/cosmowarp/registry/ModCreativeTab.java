package dev.waco0311.cosmowarp.registry;

import dev.waco0311.cosmowarp.Cosmonauticswarpdrive;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTab {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Cosmonauticswarpdrive.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> COSMOWARP_TAB =
            CREATIVE_MODE_TABS.register("cosmowarp_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.cosmowarp"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> ModItems.WARP_CRYSTAL.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.RAW_MOON_CRYSTAL.get());
                        output.accept(ModItems.MOON_CRYSTAL.get());
                        output.accept(ModItems.WARP_CRYSTAL.get());
                        output.accept(ModItems.MOON_CRYSTAL_ORE.get());
                        output.accept(ModItems.WARP_DRIVE.get());
                        output.accept(ModItems.CRYSTAL_DRIVER.get());
                        output.accept(ModItems.MEMORY_CARD.get());
                    }).build());

    public static void register(net.neoforged.bus.api.IEventBus modEventBus) {
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
