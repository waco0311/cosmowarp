package dev.waco0311.cosmowarp.block.entity;

import dev.waco0311.cosmowarp.Config;
import dev.waco0311.cosmowarp.block.TestDriveBlock;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.api.physics.force.ForceTotal;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

/**
 * "Crouching start" inertial drive: while armed, brakes the ship toward a small fraction of
 * whatever speed it had at the moment of arming (see sable$physicsTick), producing a slow drift
 * instead of its normal speed, and counts up a charge value the whole time. A rising redstone
 * edge on the block's TOP face fires an actual warp -- distance scaled by how long it was
 * charging, direction taken from the ship's current heading -- reusing the same WarpSubLevel() +
 * hyperspace-effect path as the Warp Drive.
 *
 * Redstone layout:
 * - front face (the block's FACING direction) -> armed (level-triggered: held high = stays armed)
 * - top face, rising edge only               -> fires the jump (requires armed + speed condition
 *                                                + charge > 0)
 *
 * NOT YET WIRED: CC:Tweaked peripheral methods (isArmed/getDistanceTraveled-equivalent/
 * getVelocity/meetsSpeedCondition) mentioned in design discussion are a separate follow-up.
 */
public class TestDriveBlockEntity extends BlockEntity implements BlockEntitySubLevelActor {

    private int chargeTicksElapsed = 0;
    private boolean wasTopPowered = false;

    // Fire sequence: a rising top-face edge (while armed, past speed condition, with charge > 0)
    // locks in the jump's direction/distance and starts this countdown instead of executing the
    // warp immediately. See TEST_DRIVE_FIRE_DELAY_TICKS's config comment for why the delay itself
    // matters (letting a vanilla button's own auto-reset schedule finish before the structure --
    // and the button along with it -- gets relocated), not just as a visual buildup.
    private boolean firing = false;
    private int fireCountdownTicks = 0;
    private double pendingDirX, pendingDirY, pendingDirZ;
    private double pendingDistance;

    // Ticks (server-tick cadence) remaining before sable$physicsTick resumes applying braking
    // impulses after a jump. Warping likely involves Sable rebuilding the sub-level's physics
    // state (same reason chargingEffectPlayers needed NBT persistence on the Warp Drive side) --
    // hammering a fresh/mid-transition rigid body with a large corrective impulse right at that
    // moment is a strong suspect for the post-warp fling/disappearance reports. Give it a moment
    // to settle before this block starts pushing on it again.
    private int postWarpCooldownTicks = 0;
    private static final int POST_WARP_COOLDOWN_TICKS = 20; // 1 second

    // client-only, used purely to pace the ring particle spawn rate; not persisted.
    private int particleTickCounter = 0;

    public TestDriveBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // --- public state (also the basis for the eventual CC:Tweaked peripheral) --------------

    /** The block's front face (the direction it's FACING) holds this armed while powered. */
    public boolean isArmed() {
        if (level == null) return false;
        Direction facing = getBlockState().getValue(TestDriveBlock.FACING);
        return level.getSignal(worldPosition.relative(facing), facing.getOpposite()) > 0;
    }

    /** Mirrors the original Test Drive's guard: won't fire while the ship is basically stationary.
     *  Checked against the speed captured at the moment of arming, NOT the live current speed --
     *  since this block's own braking (see sable$physicsTick) deliberately suppresses live speed
     *  down toward the throttle target, checking live speed here would often fail purely because
     *  of our own throttling, not because the ship was actually stationary when armed. */
    public boolean meetsSpeedCondition() {
        return baselineSpeed >= Config.TEST_DRIVE_MIN_SPEED.get();
    }

    public int getChargeTicksElapsed() {
        return chargeTicksElapsed;
    }

    /** What the jump distance would be if fired right now, given the current charge. */
    public double getPendingJumpDistance() {
        int maxTicks = Math.max(1, Config.TEST_DRIVE_MAX_CHARGE_TICKS.get());
        double fraction = Math.min(1.0, (double) chargeTicksElapsed / maxTicks);
        return fraction * Config.TEST_DRIVE_MAX_JUMP_DISTANCE.get();
    }

    // --- server tick: redstone polling, charge accounting, particles -----------------------

    /** Called every server tick via TestDriveBlock#getTicker. */
    public void tick() {
        if (level == null || level.isClientSide) return;

        if (postWarpCooldownTicks > 0) postWarpCooldownTicks--;

        boolean topPowered = level.getSignal(worldPosition.above(), Direction.DOWN) > 0;
        boolean armed = isArmed();

        if (firing) {
            // Once committed, keep counting down regardless of arm/charge state -- the jump's
            // direction/distance were already locked in when the sequence began. Particles keep
            // playing through the delay too, so the charge-up visual doesn't cut out right before
            // the jump.
            spawnChargeRingParticles();
            if (--fireCountdownTicks <= 0) {
                executeFire();
            }
        } else if (armed) {
            chargeTicksElapsed = Math.min(chargeTicksElapsed + 1, Config.TEST_DRIVE_MAX_CHARGE_TICKS.get());
            spawnChargeRingParticles();
        } else {
            chargeTicksElapsed = 0;
        }

        // Rising edge only, so holding the top face powered doesn't re-trigger every tick.
        if (topPowered && !wasTopPowered && armed && !firing) {
            beginFireSequence();
        }

        wasTopPowered = topPowered;
        setChanged();
    }

    /**
     * Locks in the jump's direction/distance from right now (NOT recalculated when the delay
     * ends -- otherwise releasing the arm signal or the ship's heading drifting during the delay
     * would silently change what was already "decided" the moment the button was pressed) and
     * starts the fire-delay countdown. See TEST_DRIVE_FIRE_DELAY_TICKS's config comment for why
     * this delay exists at all, not just as a future visual buildup.
     */
    private void beginFireSequence() {
        if (!meetsSpeedCondition()) return;
        if (chargeTicksElapsed <= 0) return;

        ServerSubLevel subLevel = findServerSubLevel();
        if (subLevel == null) return;

        Vec3 direction = travelDirection(subLevel);
        double distance = getPendingJumpDistance();
        if (distance <= 0) return;

        pendingDirX = direction.x;
        pendingDirY = direction.y;
        pendingDirZ = direction.z;
        pendingDistance = distance;

        firing = true;
        fireCountdownTicks = Config.TEST_DRIVE_FIRE_DELAY_TICKS.get();
    }

    private void executeFire() {
        firing = false;

        ServerSubLevel subLevel = findServerSubLevel();
        if (subLevel == null || !(level instanceof ServerLevel serverLevel)) {
            chargeTicksElapsed = 0;
            return;
        }

        Vec3 currentPos = SableCompanion.INSTANCE.projectOutOfSubLevel(level, Vec3.atCenterOf(worldPosition));
        Vector3d destPos = new Vector3d(
                currentPos.x + pendingDirX * pendingDistance,
                currentPos.y + pendingDirY * pendingDistance,
                currentPos.z + pendingDirZ * pendingDistance);

        serverLevel.playSound(null, worldPosition,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 1.0f, 1.0f);

        // Same-dimension jump, same warp mechanism the Warp Drive uses -- reuses the existing
        // hyperspace screen effect automatically since that's driven by WarpDriveBlockEntity's
        // sibling machinery, not anything specific to how the destination was chosen.
        dev.egg.SubLevelWarper.WarpSubLevel(subLevel, serverLevel, destPos);

        serverLevel.playSound(null, BlockPos.containing(destPos.x, destPos.y, destPos.z),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 1.0f, 1.0f);

        chargeTicksElapsed = 0;
        postWarpCooldownTicks = POST_WARP_COOLDOWN_TICKS;
        wasArmedForPhysics = false; // force a fresh baseline capture once the cooldown ends
        setChanged();
    }

    /** Ship's current heading: normalized velocity if moving meaningfully, else this block's FACING. */
    private Vec3 travelDirection(ServerSubLevel subLevel) {
        Vector3d vel = subLevel.latestLinearVelocity;
        if (vel.lengthSquared() > 1e-4) {
            Vector3d n = new Vector3d(vel).normalize();
            return new Vec3(n.x, n.y, n.z);
        }
        Direction facing = getBlockState().getValue(TestDriveBlock.FACING);
        return Vec3.atLowerCornerOf(facing.getNormal());
    }

    // --- physics tick: brake toward a target speed while armed --------------------------------
    // IMPORTANT: this does NOT try to read-and-cancel "this substep's propulsion force" -- that
    // approach (an earlier version of this file) turned out to be order-dependent: if this
    // actor's own sable$physicsTick happens to run before the ship's engines within a given
    // substep, it reads a total that's still effectively empty and cancels nothing, while the
    // engines' contribution added afterward sails through untouched (matches the observed "speed
    // doesn't drop at all" result). Braking against latestLinearVelocity instead sidesteps this
    // entirely: that value is the settled result of the FULLY completed previous physics step, not
    // something still being concurrently written to by multiple actors mid-substep.
    private double baselineSpeed = 0.0;
    private boolean wasArmedForPhysics = false;

    @Override
    public void sable$physicsTick(final ServerSubLevel subLevel, final RigidBodyHandle handle, final double timeStep) {
        if (postWarpCooldownTicks > 0) return;

        boolean armed = isArmed();
        if (!armed) {
            wasArmedForPhysics = false;
            return;
        }

        Vector3d velocity = new Vector3d(subLevel.latestLinearVelocity);
        double speed = velocity.length();

        // Capture the ship's speed at the instant of arming as the 100% baseline, so the throttle
        // fraction scales relative to "however fast it was already going" rather than some fixed
        // absolute number.
        if (!wasArmedForPhysics) {
            baselineSpeed = speed;
        }
        wasArmedForPhysics = true;

        double keepFraction = Config.TEST_DRIVE_THROTTLE_FRACTION.get();
        double targetSpeed = baselineSpeed * keepFraction;

        if (speed > targetSpeed && speed > 1e-6) {
            double excessSpeed = speed - targetSpeed;
            Vector3d brakingImpulse = new Vector3d(velocity).normalize().mul(-excessSpeed * subLevel.getMassTracker().getMass());

            ForceTotal total = subLevel.getOrCreateQueuedForceGroup(ForceGroups.PROPULSION.get()).getForceTotal();
            total.applyLinearAndAngularImpulse(brakingImpulse, new Vector3d());
        }

        // NOTE: this only brakes linear speed, not rotation -- a ship that's actively turning
        // while armed will keep turning at whatever rate its engines/rudders are producing. Adding
        // angular damping here (mirroring the linear case via getInverseInertiaTensor()) is a
        // reasonable follow-up if that turns out to matter in practice.
    }

    // --- charge-up ring particles ------------------------------------------------------------

    /**
     * Rings of particles sized to the ship's cross-section, sliding from behind the ship toward
     * the front along the travel direction -- spawn rate/speed increases as charge builds up.
     * Mirrors WarpDriveBlockEntity#spawnChargeParticles's approach (local plot point -> real
     * world point via SableCompanion), just arranged as a ring instead of a sphere of converging
     * points.
     */
    private void spawnChargeRingParticles() {
        if (!(level instanceof ServerLevel serverLevel)) return;

        LevelPlot plot = findLevelPlot();
        ServerSubLevel subLevel = findServerSubLevel();
        if (plot == null || subLevel == null) return;

        // Spawn less often at low charge, more often as it builds up (roughly every 12 ticks at
        // 0 charge, down to every 2 ticks at max charge).
        int maxTicks = Math.max(1, Config.TEST_DRIVE_MAX_CHARGE_TICKS.get());
        float chargeFraction = Math.min(1f, (float) chargeTicksElapsed / maxTicks);
        int interval = Math.max(2, Math.round(12 - 10 * chargeFraction));
        particleTickCounter++;
        if (particleTickCounter < interval) return;
        particleTickCounter = 0;

        var bounds = plot.getBoundingBox();
        double localCx = (bounds.minX() + bounds.maxX()) / 2.0 + 0.5;
        double localCy = (bounds.minY() + bounds.maxY()) / 2.0 + 0.5;
        double localCz = (bounds.minZ() + bounds.maxZ()) / 2.0 + 0.5;

        double crossRadius = Math.max(bounds.width(), bounds.height()) / 2.0 + 1.0;
        double halfLength = Math.max(bounds.width(), bounds.length()) / 2.0 + 4.0;

        Vec3 dir = travelDirection(subLevel);
        // any vector not parallel to dir, used to build a basis perpendicular to it
        Vec3 arbitrary = Math.abs(dir.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 right = dir.cross(arbitrary).normalize();
        Vec3 up = right.cross(dir).normalize();

        // ring starts behind the ship and slides toward/through the front as it "advances" --
        // driven by particleTickCounter's reset cadence rather than a stored ring position, so
        // successive rings appear staggered along the travel axis over multiple spawns.
        double ringOffset = -halfLength + (serverLevel.getGameTime() % 40) / 40.0 * (halfLength * 2);

        int points = 20;
        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI / points) * i;
            double rx = Math.cos(angle) * crossRadius;
            double ry = Math.sin(angle) * crossRadius;

            Vec3 localPoint = new Vec3(
                    localCx + dir.x * ringOffset + right.x * rx + up.x * ry,
                    localCy + dir.y * ringOffset + right.y * rx + up.y * ry,
                    localCz + dir.z * ringOffset + right.z * rx + up.z * ry);
            Vec3 worldPoint = SableCompanion.INSTANCE.projectOutOfSubLevel(level, localPoint);

            double speed = 0.05 + 0.15 * chargeFraction;
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    worldPoint.x, worldPoint.y, worldPoint.z, 1,
                    dir.x * speed, dir.y * speed, dir.z * speed, 0.0);
        }
    }

    // --- helpers, mirroring WarpDriveBlockEntity's plot/sub-level lookup pattern -----------

    @Nullable
    private LevelPlot findLevelPlot() {
        if (!(level instanceof ServerLevel serverLevel)) return null;
        ServerSubLevelContainer container = ServerSubLevelContainer.getContainer(serverLevel);
        if (container == null) return null;
        return container.getPlot(new ChunkPos(worldPosition));
    }

    @Nullable
    private ServerSubLevel findServerSubLevel() {
        LevelPlot plot = findLevelPlot();
        if (plot == null) return null;
        var subLevel = plot.getSubLevel();
        return subLevel instanceof ServerSubLevel serverSubLevel ? serverSubLevel : null;
    }

    // --- persistence -------------------------------------------------------------------------
    // Both fields matter to persist: chargeTicksElapsed so a chunk unload/reload mid-charge
    // doesn't silently lose progress (same lesson as the earlier chargingEffectPlayers bug), and
    // wasTopPowered so a reload while the top face happens to already be held high doesn't read
    // as a false rising edge on the next tick.

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("ChargeTicksElapsed", chargeTicksElapsed);
        tag.putBoolean("WasTopPowered", wasTopPowered);
        tag.putInt("PostWarpCooldownTicks", postWarpCooldownTicks);
        tag.putBoolean("Firing", firing);
        tag.putInt("FireCountdownTicks", fireCountdownTicks);
        tag.putDouble("PendingDirX", pendingDirX);
        tag.putDouble("PendingDirY", pendingDirY);
        tag.putDouble("PendingDirZ", pendingDirZ);
        tag.putDouble("PendingDistance", pendingDistance);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        chargeTicksElapsed = tag.getInt("ChargeTicksElapsed");
        wasTopPowered = tag.getBoolean("WasTopPowered");
        postWarpCooldownTicks = tag.getInt("PostWarpCooldownTicks");
        firing = tag.getBoolean("Firing");
        fireCountdownTicks = tag.getInt("FireCountdownTicks");
        pendingDirX = tag.getDouble("PendingDirX");
        pendingDirY = tag.getDouble("PendingDirY");
        pendingDirZ = tag.getDouble("PendingDirZ");
        pendingDistance = tag.getDouble("PendingDistance");
    }
}
