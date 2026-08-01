package dev.waco0311.cosmowarp;

import dev.waco0311.cosmowarp.client.render.HyperspaceGLRenderer;
import dev.waco0311.cosmowarp.client.render.WarpDriveBlockEntityRenderer;
import dev.waco0311.cosmowarp.client.screen.CrystalDriverScreen;
import dev.waco0311.cosmowarp.client.screen.WarpDriveScreen;
import dev.waco0311.cosmowarp.registry.ModBlockEntities;
import dev.waco0311.cosmowarp.registry.ModMenuTypes;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = Cosmonauticswarpdrive.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = Cosmonauticswarpdrive.MODID, value = Dist.CLIENT)
public class CosmonauticswarpdriveClient {
    public CosmonauticswarpdriveClient(ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

        // RenderLevelStageEvent fires on the game bus (NeoForge.EVENT_BUS), not the mod bus this
        // class's @EventBusSubscriber defaults to, so it's registered explicitly here.
        NeoForge.EVENT_BUS.addListener(this::onRenderLevelStage);
    }

    private void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            HyperspaceGLRenderer.renderIfActive();
        }
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        // Some client setup code
        Cosmonauticswarpdrive.LOGGER.info("HELLO FROM CLIENT SETUP");
        Cosmonauticswarpdrive.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }

    @SubscribeEvent
    static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.WARP_DRIVE_MENU.get(), WarpDriveScreen::new);
        event.register(ModMenuTypes.CRYSTAL_DRIVER_MENU.get(), CrystalDriverScreen::new);
    }

    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.WARP_DRIVE.get(), WarpDriveBlockEntityRenderer::new);
    }
}