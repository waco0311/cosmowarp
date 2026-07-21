package dev.waco0311.cosmowarp.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * A single recorded warp destination.
 * id is stable across rename operations (GUI edits only touch "name").
 */
public record WarpPoint(UUID id, String name, ResourceKey<Level> dimension, BlockPos pos) {

    public static final Codec<WarpPoint> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.xmap(UUID::fromString, UUID::toString).fieldOf("id").forGetter(WarpPoint::id),
            Codec.STRING.fieldOf("name").forGetter(WarpPoint::name),
            ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(WarpPoint::dimension),
            BlockPos.CODEC.fieldOf("pos").forGetter(WarpPoint::pos)
    ).apply(instance, WarpPoint::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, WarpPoint> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.map(UUID::fromString, UUID::toString), WarpPoint::id,
            ByteBufCodecs.STRING_UTF8, WarpPoint::name,
            ResourceKey.streamCodec(Registries.DIMENSION), WarpPoint::dimension,
            BlockPos.STREAM_CODEC, WarpPoint::pos,
            WarpPoint::new
    );

    public WarpPoint withName(String newName) {
        return new WarpPoint(id, newName, dimension, pos);
    }

    public static WarpPoint newlyRegistered(String name, ResourceKey<Level> dimension, BlockPos pos) {
        return new WarpPoint(UUID.randomUUID(), name, dimension, pos);
    }
}
