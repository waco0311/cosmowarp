package dev.waco0311.cosmowarp.network;

import dev.waco0311.cosmowarp.Cosmonauticswarpdrive;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

// No BlockPos: resolved via the player's open menu server-side (see ModNetworking / the note in
// WarpDriveSimpleActionPayload for why position-based lookup breaks once physicalized).
public record CrystalDriverSelectPayload(UUID id) implements CustomPacketPayload {

    public static final Type<CrystalDriverSelectPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Cosmonauticswarpdrive.MODID, "crystal_driver_select"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CrystalDriverSelectPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, CrystalDriverSelectPayload::id,
            CrystalDriverSelectPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}