package dev.waco0311.cosmowarp.network;

import dev.waco0311.cosmowarp.Cosmonauticswarpdrive;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record WarpEffectPayload(BlockPos sourcePos, boolean active) implements CustomPacketPayload {

    public static final Type<WarpEffectPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Cosmonauticswarpdrive.MODID, "warp_effect"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WarpEffectPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, WarpEffectPayload::sourcePos,
            ByteBufCodecs.BOOL, WarpEffectPayload::active,
            WarpEffectPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
