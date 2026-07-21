package dev.waco0311.cosmowarp.registry;

import dev.waco0311.cosmowarp.Cosmonauticswarpdrive;
import dev.waco0311.cosmowarp.item.WarpCrystalItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Cosmonauticswarpdrive.MODID);

    // Raw ore drop, sanded down with Create's sand paper into MOON_CRYSTAL.
    public static final DeferredItem<Item> RAW_MOON_CRYSTAL = ITEMS.registerSimpleItem("raw_moon_crystal",
            new Item.Properties());

    // Finished sanded crystal, used as the core in the Warp Crystal recipe.
    public static final DeferredItem<Item> MOON_CRYSTAL = ITEMS.registerSimpleItem("moon_crystal",
            new Item.Properties());

    // Holds the recorded warp point list (see ModDataComponents.WARP_POINTS).
    // No default component is set here on purpose: reading ModDataComponents.WARP_POINTS.get()
    // during static init (before the data component registry event has fired) throws.
    // Consumers should read via stack.getOrDefault(ModDataComponents.WARP_POINTS.get(), List.of()).
    // Custom item class (not registerSimpleItem) so it can show a tooltip listing how many warp
    // points are saved -- otherwise identical crystals in an inventory look indistinguishable.
    public static final DeferredItem<WarpCrystalItem> WARP_CRYSTAL = ITEMS.register("warp_crystal",
            () -> new WarpCrystalItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<BlockItem> MOON_CRYSTAL_ORE = ITEMS.registerSimpleBlockItem(ModBlocks.MOON_CRYSTAL_ORE);
    public static final DeferredItem<BlockItem> WARP_DRIVE = ITEMS.registerSimpleBlockItem(ModBlocks.WARP_DRIVE);
    public static final DeferredItem<BlockItem> CRYSTAL_DRIVER = ITEMS.registerSimpleBlockItem(ModBlocks.CRYSTAL_DRIVER);

    public static void register(net.neoforged.bus.api.IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}