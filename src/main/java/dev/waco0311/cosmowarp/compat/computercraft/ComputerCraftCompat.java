package dev.waco0311.cosmowarp.compat.computercraft;

import dan200.computercraft.api.peripheral.PeripheralCapability;
import dev.waco0311.cosmowarp.registry.ModBlockEntities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * All CC: Tweaked wiring lives behind this one class on purpose.
 *
 * CC: Tweaked is an OPTIONAL dependency (compileOnly), so its classes may simply not exist at
 * runtime. The JVM resolves a class's references lazily, at first use -- so as long as nothing
 * outside this package mentions a dan200.* type directly, and the caller checks
 * ModList.get().isLoaded("computercraft") before calling in, this class is never loaded on
 * installs without CC:T and nothing blows up. Referencing PeripheralCapability straight from the
 * main mod class (even inside an if) would risk that guarantee, hence the separate class.
 */
public final class ComputerCraftCompat {

    private ComputerCraftCompat() {}

    public static void registerPeripherals(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(PeripheralCapability.get(), ModBlockEntities.WARP_DRIVE.get(),
                (blockEntity, side) -> new WarpDrivePeripheral(blockEntity));
    }
}
