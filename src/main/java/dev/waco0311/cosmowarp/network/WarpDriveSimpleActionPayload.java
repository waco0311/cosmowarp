package dev.waco0311.cosmowarp.network;

import dev.waco0311.cosmowarp.Cosmonauticswarpdrive;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * No BlockPos here on purpose: the server resolves the target Warp Drive from the player's
 * currently open menu (see ModNetworking), not by re-searching the world by position. Once a
 * Warp Drive is physicalized inside a Sable sub-level, its BlockPos/Level no longer match what
 * player.level().getBlockEntity(pos) would find, so a position-based lookup silently fails.
 */
public record WarpDriveSimpleActionPayload(Action action) implements CustomPacketPayload {

    public enum Action { REGISTER_HERE, WARP }

    public static final Type<WarpDriveSimpleActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Cosmonauticswarpdrive.MODID, "warp_drive_simple_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WarpDriveSimpleActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.idMapper(id -> Action.values()[id], Action::ordinal), WarpDriveSimpleActionPayload::action,
            WarpDriveSimpleActionPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}