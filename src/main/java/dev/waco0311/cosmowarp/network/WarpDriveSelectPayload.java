package dev.waco0311.cosmowarp.network;

import dev.waco0311.cosmowarp.Cosmonauticswarpdrive;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

// No BlockPos: resolved via the player's open menu server-side. See WarpDriveSimpleActionPayload.
public record WarpDriveSelectPayload(UUID id) implements CustomPacketPayload {

    public static final Type<WarpDriveSelectPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Cosmonauticswarpdrive.MODID, "warp_drive_select"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WarpDriveSelectPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, WarpDriveSelectPayload::id,
            WarpDriveSelectPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}