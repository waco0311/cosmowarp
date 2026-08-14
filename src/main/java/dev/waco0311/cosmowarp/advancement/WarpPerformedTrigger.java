package dev.waco0311.cosmowarp.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * Fires when a Warp Drive actually completes a jump (i.e. right where
 * dev.egg.SubLevelWarper.WarpSubLevel() is called, not when the button is pressed
 * or the charge-up starts). If multiple players are aboard the physicalized
 * structure, call {@link #trigger(ServerPlayer, boolean)} once per player.
 */
public class WarpPerformedTrigger extends SimpleCriterionTrigger<WarpPerformedTrigger.TriggerInstance> {

    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    /**
     * @param crossDimension whether this specific jump moved between two different dimensions
     */
    public void trigger(ServerPlayer player, boolean crossDimension) {
        this.trigger(player, triggerInstance -> triggerInstance.matches(crossDimension));
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player, boolean crossDimension)
            implements SimpleCriterionTrigger.SimpleInstance {

        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                Codec.BOOL.optionalFieldOf("cross_dimension", false).forGetter(TriggerInstance::crossDimension)
        ).apply(instance, TriggerInstance::new));

        /**
         * @param actualCrossDimension whether the warp that just happened crossed dimensions
         */
        public boolean matches(boolean actualCrossDimension) {
            // A criterion that doesn't require cross-dimension is satisfied by any warp.
            // A criterion that does require it only counts cross-dimension warps.
            return !this.crossDimension || actualCrossDimension;
        }
    }
}
