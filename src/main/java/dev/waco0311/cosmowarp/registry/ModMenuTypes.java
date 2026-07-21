package dev.waco0311.cosmowarp.registry;

import dev.waco0311.cosmowarp.Cosmonauticswarpdrive;
import dev.waco0311.cosmowarp.menu.CrystalDriverMenu;
import dev.waco0311.cosmowarp.menu.WarpDriveMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, Cosmonauticswarpdrive.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<WarpDriveMenu>> WARP_DRIVE_MENU =
            MENU_TYPES.register("warp_drive_menu", () -> IMenuTypeExtension.create(
                    (containerId, playerInventory, buf) ->
                            new WarpDriveMenu(containerId, playerInventory, buf.readBlockPos())));

    public static final DeferredHolder<MenuType<?>, MenuType<CrystalDriverMenu>> CRYSTAL_DRIVER_MENU =
            MENU_TYPES.register("crystal_driver_menu", () -> IMenuTypeExtension.create(
                    (containerId, playerInventory, buf) ->
                            new CrystalDriverMenu(containerId, playerInventory, buf.readBlockPos())));

    public static void register(net.neoforged.bus.api.IEventBus modEventBus) {
        MENU_TYPES.register(modEventBus);
    }
}