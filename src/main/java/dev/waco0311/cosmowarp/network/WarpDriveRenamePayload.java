package dev.waco0311.cosmowarp.network;

import dev.waco0311.cosmowarp.Cosmonauticswarpdrive;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

// No BlockPos: resolved via the player's open menu server-side. See WarpDriveSimpleActionPayload.
public record WarpDriveRenamePayload(UUID id, String newName) implements CustomPacketPayload {

    public static final Type<WarpDriveRenamePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Cosmonauticswarpdrive.MODID, "warp_drive_rename"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WarpDriveRenamePayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, WarpDriveRenamePayload::id,
            ByteBufCodecs.stringUtf8(64), WarpDriveRenamePayload::newName,
            WarpDriveRenamePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}