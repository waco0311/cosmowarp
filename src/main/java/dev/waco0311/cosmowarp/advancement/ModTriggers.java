package dev.waco0311.cosmowarp.advancement;

import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers cosmowarp's custom advancement criterion triggers.
 *
 * Don't forget to call TRIGGER_TYPES.register(modEventBus) once, in the mod's
 * constructor (wherever the other DeferredRegisters like items/blocks are registered).
 */
public class ModTriggers {

    // Replace "cosmowarp" below with your MODID constant if you have one (e.g. Cosmonauticswarpdrive.MODID).
    public static final DeferredRegister<CriterionTrigger<?>> TRIGGER_TYPES =
            DeferredRegister.create(Registries.TRIGGER_TYPE, "cosmowarp");

    public static final DeferredHolder<CriterionTrigger<?>, RegisterLocationTrigger> REGISTER_LOCATION =
            TRIGGER_TYPES.register("register_location", RegisterLocationTrigger::new);

    public static final DeferredHolder<CriterionTrigger<?>, WarpPerformedTrigger> WARP_PERFORMED =
            TRIGGER_TYPES.register("warp_performed", WarpPerformedTrigger::new);
}
