package dev.waco0311.cosmowarp.network;

import dev.waco0311.cosmowarp.Cosmonauticswarpdrive;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// No BlockPos: resolved via the player's open menu server-side.
public record CrystalDriverActionPayload(Action action) implements CustomPacketPayload {

    public enum Action { COPY, DELETE }

    public static final Type<CrystalDriverActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Cosmonauticswarpdrive.MODID, "crystal_driver_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CrystalDriverActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.idMapper(id -> Action.values()[id], Action::ordinal), CrystalDriverActionPayload::action,
            CrystalDriverActionPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}