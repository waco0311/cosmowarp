package dev.waco0311.cosmowarp.network;

import dev.waco0311.cosmowarp.block.entity.WarpDriveBlockEntity;
import dev.waco0311.cosmowarp.menu.CrystalDriverMenu;
import dev.waco0311.cosmowarp.menu.WarpDriveMenu;
import dev.waco0311.cosmowarp.Cosmonauticswarpdrive;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModNetworking {

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(Cosmonauticswarpdrive.MODID).versioned("4");

        registrar.playToServer(WarpDriveSimpleActionPayload.TYPE, WarpDriveSimpleActionPayload.STREAM_CODEC,
                ModNetworking::handleSimpleAction);
        registrar.playToServer(WarpDriveSelectPayload.TYPE, WarpDriveSelectPayload.STREAM_CODEC,
                ModNetworking::handleSelect);
        registrar.playToServer(WarpDriveRenamePayload.TYPE, WarpDriveRenamePayload.STREAM_CODEC,
                ModNetworking::handleRename);

        registrar.playToServer(CrystalDriverSelectPayload.TYPE, CrystalDriverSelectPayload.STREAM_CODEC,
                ModNetworking::handleCrystalDriverSelect);
        registrar.playToServer(CrystalDriverActionPayload.TYPE, CrystalDriverActionPayload.STREAM_CODEC,
                ModNetworking::handleCrystalDriverAction);

        registrar.playToClient(WarpEffectPayload.TYPE, WarpEffectPayload.STREAM_CODEC,
                dev.waco0311.cosmowarp.client.WarpEffectClient::handle);
    }

    /**
     * Tells exactly the given players (and no one else) to start/stop the hyperspace screen
     * effect. Deliberately NOT radius-based: only players actually on the ship (see
     * WarpDriveBlockEntity#findPlayersOnShip) should see the personal screen distortion. Anyone
     * else nearby still sees the particles, which sync separately via normal vanilla means.
     * sourceKey must be the SAME value on the matching start(true)/stop(false) pair -- the client
     * uses it as a set key, so a mismatched key (e.g. a moving position) would leave the effect
     * stuck on. The Warp Drive's own block position is used for this, since it doesn't move.
     */
    public static void broadcastWarpEffectToPlayers(java.util.List<ServerPlayer> players, net.minecraft.core.BlockPos sourceKey, boolean active) {
        WarpEffectPayload payload = new WarpEffectPayload(sourceKey, active);
        for (ServerPlayer player : players) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    // All handlers below resolve their target BlockEntity through the player's currently open
    // menu (player.containerMenu), NOT by re-searching player.level() with a stored BlockPos.
    // Once a Warp Drive / Crystal Driver is physicalized inside a Sable sub-level, its real
    // position/level no longer matches what player.level().getBlockEntity(pos) would find, so a
    // position-based lookup silently returns nothing. The menu already holds a direct, correct
    // reference to the block entity from when it was opened, so we reuse that instead.

    private static void handleSimpleAction(WarpDriveSimpleActionPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (!(player.containerMenu instanceof WarpDriveMenu menu)) return;
            WarpDriveBlockEntity warpDrive = menu.blockEntity;

            switch (payload.action()) {
                case REGISTER_HERE -> {
                    if (warpDrive.registerHere()) {
                        dev.waco0311.cosmowarp.advancement.ModTriggers.REGISTER_LOCATION.get().trigger(player);
                    }
                }
                case WARP -> {
                    WarpDriveBlockEntity.WarpResult result = warpDrive.beginWarp();
                    if (result == WarpDriveBlockEntity.WarpResult.SUCCESS) {
                        player.displayClientMessage(Component.translatable("message.cosmowarp.warp_charging"), true);
                    } else {
                        Component message = result == WarpDriveBlockEntity.WarpResult.NOT_ENOUGH_POWER
                                ? Component.translatable("message.cosmowarp.warp_failed.not_enough_power",
                                String.format("%,d", WarpDriveBlockEntity.warpCostFE()))
                                : Component.translatable("message.cosmowarp.warp_failed." + result.name().toLowerCase());
                        player.displayClientMessage(message, true);
                    }
                }
            }
        });
    }

    private static void handleSelect(WarpDriveSelectPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player.containerMenu instanceof WarpDriveMenu menu) {
                menu.blockEntity.setSelectedId(payload.id());
            }
        });
    }

    private static void handleRename(WarpDriveRenamePayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player.containerMenu instanceof WarpDriveMenu menu) {
                String safeName = payload.newName().isBlank() ? "Warp Point" : payload.newName().trim();
                menu.blockEntity.renamePoint(payload.id(), safeName);
            }
        });
    }

    private static void handleCrystalDriverSelect(CrystalDriverSelectPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player.containerMenu instanceof CrystalDriverMenu menu) {
                menu.blockEntity.setSelectedId(payload.id());
            }
        });
    }

    private static void handleCrystalDriverAction(CrystalDriverActionPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (!(player.containerMenu instanceof CrystalDriverMenu menu)) return;

            switch (payload.action()) {
                case COPY -> menu.blockEntity.copyToTarget();
                case DELETE -> menu.blockEntity.deleteFromSource();
            }
        });
    }
}
