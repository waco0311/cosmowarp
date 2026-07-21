package dev.waco0311.cosmowarp;

import com.mojang.logging.LogUtils;
import dev.waco0311.cosmowarp.block.entity.WarpDriveBlockEntity;
import dev.waco0311.cosmowarp.registry.ModBlockEntities;
import dev.waco0311.cosmowarp.registry.ModBlocks;
import dev.waco0311.cosmowarp.registry.ModCreativeTab;
import dev.waco0311.cosmowarp.registry.ModDataComponents;
import dev.waco0311.cosmowarp.registry.ModItems;
import dev.waco0311.cosmowarp.registry.ModMenuTypes;
import dev.waco0311.cosmowarp.network.ModNetworking;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;

@Mod(Cosmonauticswarpdrive.MODID)
public class Cosmonauticswarpdrive {

    public static final String MODID = "cosmowarp";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Cosmonauticswarpdrive(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModDataComponents.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModCreativeTab.register(modEventBus);
        ModMenuTypes.register(modEventBus);

        modEventBus.addListener(ModNetworking::register);
        modEventBus.addListener(this::registerCapabilities);

        NeoForge.EVENT_BUS.register(this);

        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("cosmowarp: common setup complete");
    }

    // Lets Create/other FE generators push power in, and hoppers/automation insert the Warp Crystal.
    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.WARP_DRIVE.get(),
                (be, side) -> be.getEnergyStorage());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.WARP_DRIVE.get(),
                (be, side) -> be.getCrystalSlot());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("cosmowarp: server starting");
    }
}