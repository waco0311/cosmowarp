package dev.waco0311.cosmowarp.block.entity;

import dev.waco0311.cosmowarp.Config;
import dev.waco0311.cosmowarp.data.WarpPoint;
import dev.waco0311.cosmowarp.menu.WarpDriveMenu;
import dev.waco0311.cosmowarp.registry.ModDataComponents;
import dev.waco0311.cosmowarp.registry.ModItems;
import dev.ryanhcode.sable.companion.SableCompanion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class WarpDriveBlockEntity extends BlockEntity implements MenuProvider {

    /** Reads the current config value each time, so a config reload takes effect on next use. */
    public static long warpCostFE() {
        return Config.WARP_COST_FE.get();
    }

    // Capacity/maxReceive/maxExtract are fixed at construction time (EnergyStorage doesn't support
    // resizing), so a config change takes effect the next time this block entity is (re)created
    // (chunk reload / server restart), not instantly for already-loaded blocks.
    // Same cap as capacity so it charges freely regardless of the supplying mod's transfer rate.
    // A named inner class (not anonymous) so loadAdditional() can call setStoredEnergy() directly
    // -- receiveEnergy() ADDS to the current value, so using it to restore a synced/saved amount
    // would snowball the stored energy upward every time a sync packet is applied.
    private final SyncingEnergyStorage energyStorage;
    {
        int cost = (int) warpCostFE();
        energyStorage = new SyncingEnergyStorage(cost, cost, cost);
    }

    private class SyncingEnergyStorage extends EnergyStorage {
        SyncingEnergyStorage(int capacity, int maxReceive, int maxExtract) {
            super(capacity, maxReceive, maxExtract);
        }

        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            int received = super.receiveEnergy(toReceive, simulate);
            if (!simulate && received > 0) syncToClient();
            return received;
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            int extracted = super.extractEnergy(toExtract, simulate);
            if (!simulate && extracted > 0) syncToClient();
            return extracted;
        }

        /** Directly overwrites the stored amount (used when applying a saved/synced value). */
        void setStoredEnergy(int value) {
            this.energy = Math.max(0, Math.min(value, this.getMaxEnergyStored()));
        }
    }

    // Single crystal/card slot. Accepts either a Warp Crystal or a Memory Card. All warp point
    // data lives on the ItemStack itself (ModDataComponents.WARP_POINTS / SELECTED_WARP_POINT),
    // not on this block entity, so it keeps its list when moved to another Warp Drive or into a
    // Crystal Driver.
    private final ItemStackHandler crystalSlot = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            syncToClient();
            updateHasCrystalState();
        }
    };

    public WarpDriveBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public EnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    public ItemStackHandler getCrystalSlot() {
        return crystalSlot;
    }

    private ItemStack crystal() {
        return crystalSlot.getStackInSlot(0);
    }

    public List<WarpPoint> getWarpPoints() {
        return crystal().getOrDefault(ModDataComponents.WARP_POINTS.get(), List.of());
    }

    public Optional<UUID> getSelectedId() {
        return Optional.ofNullable(crystal().get(ModDataComponents.SELECTED_WARP_POINT.get()));
    }

    public void setSelectedId(UUID id) {
        ItemStack stack = crystal();
        if (stack.isEmpty()) return;
        stack.set(ModDataComponents.SELECTED_WARP_POINT.get(), id);
        syncToClient();
    }

    /**
     * Registers this Warp Drive's current position/dimension as a new entry on the inserted
     * crystal/card. Refuses if a Memory Card is already at its configured capacity.
     *
     * @return true if a point was actually registered (false on any of the early-return cases
     *         below) -- the caller (ModNetworking) uses this to decide whether to fire the
     *         cosmowarp:register_location advancement trigger, so it must only be true on an
     *         actual successful registration.
     */
    public boolean registerHere() {
        if (level == null) return false;
        ItemStack stack = crystal();
        if (stack.isEmpty()) return false;
        if (stack.is(ModItems.MEMORY_CARD.get()) && getWarpPoints().size() >= Config.MEMORY_CARD_CAPACITY.get()) {
            return false; // Memory Card is full
        }

        // If this Warp Drive is physicalized (part of a Sable sub-level), worldPosition is a raw
        // plot-storage position with extreme values, not the real rendered position. Companion
        // safely no-ops and returns the position unchanged when it isn't inside a sub-level, so
        // this is correct in both cases.
        Vec3 globalPos = SableCompanion.INSTANCE.projectOutOfSubLevel(level, Vec3.atCenterOf(worldPosition));
        BlockPos realPos = BlockPos.containing(globalPos);

        WarpPoint point = WarpPoint.newlyRegistered(
                "Warp Point " + (getWarpPoints().size() + 1),
                level.dimension(),
                realPos);

        List<WarpPoint> updated = new ArrayList<>(getWarpPoints());
        updated.add(point);
        stack.set(ModDataComponents.WARP_POINTS.get(), List.copyOf(updated));
        stack.set(ModDataComponents.SELECTED_WARP_POINT.get(), point.id());
        syncToClient();
        return true;
    }

    /** Renames the point with the given id in place (id stays stable). */
    public void renamePoint(UUID id, String newName) {
        ItemStack stack = crystal();
        if (stack.isEmpty()) return;

        List<WarpPoint> updated = getWarpPoints().stream()
                .map(p -> p.id().equals(id) ? p.withName(newName) : p)
                .toList();
        stack.set(ModDataComponents.WARP_POINTS.get(), updated);
        syncToClient();
    }

    /** Deletes the point with the given id. */
    public void deletePoint(UUID id) {
        ItemStack stack = crystal();
        if (stack.isEmpty()) return;

        List<WarpPoint> updated = getWarpPoints().stream()
                .filter(p -> !p.id().equals(id))
                .toList();
        stack.set(ModDataComponents.WARP_POINTS.get(), updated);
        syncToClient();
    }

    public boolean hasEnoughPower() {
        return energyStorage.getEnergyStored() >= warpCostFE();
    }

    // --- charge-up state (Warp button starts a countdown; the actual jump happens when it hits 0) ---
    private int chargeTicksRemaining = 0;
    private UUID chargingTargetId = null;
    // IMPORTANT: this must be persisted (see saveAdditional/loadAdditional) -- if this block entity
    // gets saved/reloaded while still charging (chunk unload/reload, or a Sable warp relocating the
    // whole physicalized structure this drive is attached to), losing this list means the eventual
    // executeWarp() has no one left to send the STOP-effect packet to, and whichever player(s) were
    // watching the hyperspace effect get stuck with it on forever (previously-reported bug).
    private java.util.List<UUID> chargingEffectPlayers = java.util.List.of();

    public boolean isCharging() {
        return chargeTicksRemaining > 0;
    }

    public int getChargeTicksRemaining() {
        return chargeTicksRemaining;
    }

    /**
     * Validates the warp and, if everything checks out, consumes FE and starts the charge-up
     * countdown (see tick()). The actual sub-level move happens in executeWarp() once the
     * countdown reaches 0, not here. Only an actual Warp Crystal can trigger a warp -- a Memory
     * Card can register/hold points but not warp directly (copy its points to a Warp Crystal via
     * a Crystal Driver first).
     */
    public WarpResult beginWarp() {
        if (level == null || level.isClientSide) return WarpResult.NOT_SERVER;
        if (isCharging()) return WarpResult.ALREADY_CHARGING;
        if (!crystal().is(ModItems.WARP_CRYSTAL.get())) return WarpResult.NOT_A_WARP_CRYSTAL;

        Optional<UUID> selected = getSelectedId();
        if (selected.isEmpty()) return WarpResult.NO_POINT_SELECTED;

        WarpPoint target = getWarpPoints().stream()
                .filter(p -> p.id().equals(selected.get()))
                .findFirst()
                .orElse(null);
        if (target == null) return WarpResult.NO_POINT_SELECTED;

        if (!hasEnoughPower()) return WarpResult.NOT_ENOUGH_POWER;

        if (findServerSubLevel() == null) return WarpResult.NOT_PHYSICALIZED;

        if (((net.minecraft.server.level.ServerLevel) level).getServer().getLevel(target.dimension()) == null) {
            return WarpResult.INVALID_DESTINATION;
        }

        energyStorage.extractEnergy((int) warpCostFE(), false);
        chargingTargetId = selected.get();
        chargeTicksRemaining = Math.max(1, Config.WARP_CHARGE_TICKS.get());

        // Only players actually on the ship (inside its plot bounding box, with a small margin
        // for standing right at the edge/on the deck) should see the screen-distortion shader.
        // Particles stay visible to everyone nearby (spawned via normal vanilla ServerLevel
        // particle sync), which is intentional -- only the personal screen effect is restricted.
        java.util.List<net.minecraft.server.level.ServerPlayer> shipPlayers = findPlayersOnShip();
        chargingEffectPlayers = shipPlayers.stream().map(net.minecraft.world.entity.Entity::getUUID).toList();
        dev.waco0311.cosmowarp.network.ModNetworking.broadcastWarpEffectToPlayers(shipPlayers, worldPosition, true);

        syncToClient();
        return WarpResult.SUCCESS;
    }

    /**
     * Players Sable itself considers to be riding/tracking this ship's sub-level right now.
     * Uses SableCompanion's own getTrackingOrVehicleSubLevel(Entity) -- the same method Sable's
     * own camera-sync tooling uses -- instead of approximating with a bounding-box check, since
     * ServerSubLevel implements the companion's SubLevelAccess interface and can be compared
     * directly.
     */
    private java.util.List<net.minecraft.server.level.ServerPlayer> findPlayersOnShip() {
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) return java.util.List.of();

        dev.ryanhcode.sable.sublevel.ServerSubLevel mySubLevel = findServerSubLevel();
        if (mySubLevel == null) return java.util.List.of();

        java.util.List<net.minecraft.server.level.ServerPlayer> result = new java.util.ArrayList<>();
        for (net.minecraft.server.level.ServerPlayer player : serverLevel.players()) {
            dev.ryanhcode.sable.companion.SubLevelAccess playerSubLevel =
                    SableCompanion.INSTANCE.getTrackingOrVehicleSubLevel(player);
            if (playerSubLevel == mySubLevel) {
                result.add(player);
            }
        }
        return result;
    }

    /** Called every server tick while charging (see WarpDriveBlock#getTicker). */
    public void tick() {
        if (!isCharging() || level == null || level.isClientSide) return;

        chargeTicksRemaining--;
        spawnChargeParticles();
        if (chargeTicksRemaining <= 0) {
            executeWarp();
        } else {
            setChanged();
        }
    }

    /**
     * Converging-particle placeholder for the "hyperspace charge" effect, scaled and centered on
     * the actual physicalized structure rather than the Warp Drive block itself:
     * - center: the ship's bounding-box center (so the effect isn't lopsided toward wherever the
     *   Warp Drive happens to be mounted, e.g. near the floor)
     * - radius: half the ship's largest horizontal extent, plus a 3-block margin outside the hull
     * Each spawn point is computed in the sub-level's LOCAL space, then projected out through
     * SableCompanion so it comes out correctly positioned/rotated in true world space.
     */
    private void spawnChargeParticles() {
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;

        dev.ryanhcode.sable.sublevel.plot.LevelPlot plot = findLevelPlot();
        if (plot == null) return;

        dev.ryanhcode.sable.companion.math.BoundingBox3ic bounds = plot.getBoundingBox();

        double localCx = (bounds.minX() + bounds.maxX()) / 2.0 + 0.5;
        double localCy = (bounds.minY() + bounds.maxY()) / 2.0 + 0.5;
        double localCz = (bounds.minZ() + bounds.maxZ()) / 2.0 + 0.5;

        double shipRadius = Math.max(bounds.width(), bounds.length()) / 2.0;
        double radius = shipRadius + 3.0;

        int count = Config.WARP_PARTICLE_COUNT.get();
        for (int i = 0; i < count; i++) {
            double angle = (2 * Math.PI / count) * i + (chargeTicksRemaining * 0.3);
            double ox = Math.cos(angle) * radius;
            double oz = Math.sin(angle) * radius;
            double oy = Math.sin(chargeTicksRemaining * 0.5 + i) * (shipRadius * 0.3 + 0.5);

            Vec3 localPoint = new Vec3(localCx + ox, localCy + oy, localCz + oz);
            Vec3 worldPoint = SableCompanion.INSTANCE.projectOutOfSubLevel(level, localPoint);

            // velocity points back toward the ship's center, converging inward
            double vx = -ox * 0.06;
            double vy = -oy * 0.06;
            double vz = -oz * 0.06;

            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                    worldPoint.x, worldPoint.y, worldPoint.z, 1, vx, vy, vz, 0.0);
        }
    }

    /**
     * Finds the LevelPlot (Sable's physical storage region) this Warp Drive currently sits in,
     * if it's part of a physicalized structure right now.
     */
    @Nullable
    private dev.ryanhcode.sable.sublevel.plot.LevelPlot findLevelPlot() {
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) return null;

        dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer container =
                dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer.getContainer(serverLevel);
        if (container == null) return null;

        return container.getPlot(new net.minecraft.world.level.ChunkPos(worldPosition));
    }

    /**
     * Finds the Sable sub-level this Warp Drive is currently physicalized into, if any.
     */
    @Nullable
    private dev.ryanhcode.sable.sublevel.ServerSubLevel findServerSubLevel() {
        dev.ryanhcode.sable.sublevel.plot.LevelPlot plot = findLevelPlot();
        if (plot == null) return null;

        dev.ryanhcode.sable.sublevel.SubLevel subLevel = plot.getSubLevel();
        return subLevel instanceof dev.ryanhcode.sable.sublevel.ServerSubLevel serverSubLevel ? serverSubLevel : null;
    }

    /**
     * Executes the actual jump once the charge-up finishes: finds the Sable sub-level this Warp
     * Drive is currently part of, then hands off to Dimensional Sable's WarpSubLevel() to move
     * the whole connected structure (ropes/springs/swivel bearings included).
     * FE was already spent when the charge started (beginWarp()); a failure here (e.g. the
     * structure landed/disassembled mid-charge) does not refund it.
     */
    private void executeWarp() {
        UUID targetId = chargingTargetId;
        chargingTargetId = null;
        java.util.List<UUID> effectPlayerIds = chargingEffectPlayers;
        chargingEffectPlayers = java.util.List.of();

        net.minecraft.server.level.ServerLevel serverLevel = (net.minecraft.server.level.ServerLevel) level;

        java.util.List<net.minecraft.server.level.ServerPlayer> effectPlayers = effectPlayerIds.stream()
                .map(id -> serverLevel.getServer().getPlayerList().getPlayer(id))
                .filter(java.util.Objects::nonNull)
                .toList();
        dev.waco0311.cosmowarp.network.ModNetworking.broadcastWarpEffectToPlayers(effectPlayers, worldPosition, false);

        WarpPoint target = getWarpPoints().stream()
                .filter(p -> p.id().equals(targetId))
                .findFirst()
                .orElse(null);
        if (target == null) {
            setChanged();
            return;
        }

        net.minecraft.server.level.ServerLevel destLevel = serverLevel.getServer().getLevel(target.dimension());
        if (destLevel == null) {
            setChanged();
            return;
        }

        dev.ryanhcode.sable.sublevel.ServerSubLevel serverSubLevel = findServerSubLevel();
        if (serverSubLevel == null) {
            setChanged();
            return;
        }

        org.joml.Vector3d destPos = new org.joml.Vector3d(
                target.pos().getX() + 0.5, target.pos().getY() + 0.5, target.pos().getZ() + 0.5);

        // Sound at the departure point, using the pre-warp position/level.
        serverLevel.playSound(null, worldPosition,
                net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT, net.minecraft.sounds.SoundSource.BLOCKS,
                1.0f, 1.0f);

        dev.egg.SubLevelWarper.WarpSubLevel(serverSubLevel, destLevel, destPos);

        // Sound at the arrival point.
        destLevel.playSound(null, target.pos(),
                net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT, net.minecraft.sounds.SoundSource.BLOCKS,
                1.0f, 1.0f);

        // Fire the "First Jump"/"Across the Void" advancement triggers for everyone who actually
        // made the trip -- only reached once the warp itself has genuinely succeeded (every
        // early-return failure case above skips this).
        boolean crossDimension = !serverLevel.dimension().equals(destLevel.dimension());
        for (net.minecraft.server.level.ServerPlayer effectPlayer : effectPlayers) {
            dev.waco0311.cosmowarp.advancement.ModTriggers.WARP_PERFORMED.get().trigger(effectPlayer, crossDimension);
        }

        setChanged();
    }

    public enum WarpResult {
        SUCCESS,
        NO_POINT_SELECTED,
        NOT_ENOUGH_POWER,
        NOT_SERVER,
        NOT_PHYSICALIZED,
        INVALID_DESTINATION,
        ALREADY_CHARGING,
        NOT_A_WARP_CRYSTAL
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.cosmowarp.warp_drive");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new WarpDriveMenu(containerId, playerInventory, worldPosition);
    }

    private void syncToClient() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        }
    }

    /** Keeps the WarpDriveBlock#HAS_CRYSTAL blockstate in sync with whether the slot is occupied. */
    private void updateHasCrystalState() {
        if (level == null || level.isClientSide) return;
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof dev.waco0311.cosmowarp.block.WarpDriveBlock)) return;

        boolean hasCrystal = !crystal().isEmpty();
        if (state.getValue(dev.waco0311.cosmowarp.block.WarpDriveBlock.HAS_CRYSTAL) != hasCrystal) {
            level.setBlock(worldPosition, state.setValue(dev.waco0311.cosmowarp.block.WarpDriveBlock.HAS_CRYSTAL, hasCrystal), 3);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
    }

    @Nullable
    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("CrystalSlot", crystalSlot.serializeNBT(registries));
        tag.putInt("Energy", energyStorage.getEnergyStored());
        tag.putInt("ChargeTicksRemaining", chargeTicksRemaining);
        if (chargingTargetId != null) {
            tag.putUUID("ChargingTargetId", chargingTargetId);
        }
        // See the field comment on chargingEffectPlayers for why this must survive save/reload.
        if (!chargingEffectPlayers.isEmpty()) {
            ListTag effectPlayersTag = new ListTag();
            for (UUID id : chargingEffectPlayers) {
                effectPlayersTag.add(NbtUtils.createUUID(id));
            }
            tag.put("ChargingEffectPlayers", effectPlayersTag);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        crystalSlot.deserializeNBT(registries, tag.getCompound("CrystalSlot"));
        if (tag.contains("Energy")) {
            energyStorage.setStoredEnergy(tag.getInt("Energy"));
        }
        chargeTicksRemaining = tag.getInt("ChargeTicksRemaining");
        chargingTargetId = tag.hasUUID("ChargingTargetId") ? tag.getUUID("ChargingTargetId") : null;

        if (tag.contains("ChargingEffectPlayers")) {
            ListTag effectPlayersTag = tag.getList("ChargingEffectPlayers", Tag.TAG_INT_ARRAY);
            List<UUID> ids = new ArrayList<>(effectPlayersTag.size());
            for (Tag idTag : effectPlayersTag) {
                ids.add(NbtUtils.loadUUID(idTag));
            }
            chargingEffectPlayers = List.copyOf(ids);
        } else {
            chargingEffectPlayers = List.of();
        }
    }
}
