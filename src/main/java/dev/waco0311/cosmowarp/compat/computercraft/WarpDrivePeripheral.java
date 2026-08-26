package dev.waco0311.cosmowarp.compat.computercraft;

import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import dev.waco0311.cosmowarp.Config;
import dev.waco0311.cosmowarp.block.entity.WarpDriveBlockEntity;
import dev.waco0311.cosmowarp.data.WarpPoint;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * CC: Tweaked peripheral for the Warp Drive. Deliberately data/controls only -- no display or UI
 * helpers, since the design intent is that users write their own monitor UIs in Lua rather than
 * this mod dictating one.
 *
 * Every method is mainThread = true. Peripheral methods are otherwise invoked from the Lua thread,
 * and everything here reads or mutates block entity / ItemStack state that the server thread is
 * concurrently touching, so hopping to the main thread is the correct default rather than an
 * optimisation to shave off later.
 *
 * Note on advancements: cosmowarp's "Point of No Return" / warp advancement triggers fire from the
 * GUI packet handlers (which have a ServerPlayer to award), so actions taken through this
 * peripheral intentionally do NOT grant advancements -- there's no player attached to a computer
 * call. That's a deliberate choice, not an oversight; automating the drive shouldn't be a way to
 * silently farm advancements either.
 */
public class WarpDrivePeripheral implements IPeripheral {

    private final WarpDriveBlockEntity blockEntity;

    public WarpDrivePeripheral(WarpDriveBlockEntity blockEntity) {
        this.blockEntity = blockEntity;
    }

    @Override
    public String getType() {
        return "warp_drive";
    }

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        return other instanceof WarpDrivePeripheral o && blockEntity == o.blockEntity;
    }

    // --- energy -----------------------------------------------------------------------------

    /** Current FE stored in this drive. */
    @LuaFunction(mainThread = true)
    public final int getEnergy() {
        return blockEntity.getEnergyStorage().getEnergyStored();
    }

    /** Maximum FE this drive can hold. */
    @LuaFunction(mainThread = true)
    public final int getEnergyCapacity() {
        return blockEntity.getEnergyStorage().getMaxEnergyStored();
    }

    /** FE consumed by a single warp, per the server's current config. */
    @LuaFunction(mainThread = true)
    public final long getWarpCost() {
        return Config.WARP_COST_FE.get();
    }

    // --- crystal / points -------------------------------------------------------------------

    /** Whether a Warp Crystal or Memory Card is currently inserted. */
    @LuaFunction(mainThread = true)
    public final boolean hasCrystal() {
        return !blockEntity.getCrystalSlot().getStackInSlot(0).isEmpty();
    }

    /**
     * All saved warp points on the inserted crystal/card, as a 1-indexed Lua array. Each entry has
     * id, name, x, y, z, dimension, and selected.
     */
    @LuaFunction(mainThread = true)
    public final Map<Integer, Object> getWarpPoints() {
        List<WarpPoint> points = blockEntity.getWarpPoints();
        Optional<UUID> selected = blockEntity.getSelectedId();

        Map<Integer, Object> result = new HashMap<>();
        int index = 1;
        for (WarpPoint point : points) {
            result.put(index++, toLuaTable(point, selected));
        }
        return result;
    }

    /** The currently selected warp point, or nil if nothing is selected. */
    @LuaFunction(mainThread = true)
    @Nullable
    public final Map<String, Object> getSelectedPoint() {
        Optional<UUID> selected = blockEntity.getSelectedId();
        if (selected.isEmpty()) return null;
        return blockEntity.getWarpPoints().stream()
                .filter(p -> p.id().equals(selected.get()))
                .findFirst()
                .map(p -> toLuaTable(p, selected))
                .orElse(null);
    }

    /**
     * Selects a warp point. Accepts either its 1-based index (as returned by getWarpPoints), its
     * name, or its id string -- so a script can use whichever it happens to have on hand.
     *
     * @return true if a matching point was found and selected.
     */
    @LuaFunction(mainThread = true)
    public final boolean selectPoint(IArguments args) throws LuaException {
        WarpPoint point = findPoint(args, 0);
        if (point == null) return false;
        blockEntity.setSelectedId(point.id());
        return true;
    }

    /**
     * Saves this drive's current location as a new warp point on the inserted crystal/card.
     *
     * @return false if there's no crystal inserted, or a Memory Card is already at capacity.
     */
    @LuaFunction(mainThread = true)
    public final boolean registerHere() {
        return blockEntity.registerHere();
    }

    /**
     * Renames a saved point. First argument identifies the point (index, name, or id) exactly as
     * selectPoint does; second is the new name.
     */
    @LuaFunction(mainThread = true)
    public final boolean renamePoint(IArguments args) throws LuaException {
        WarpPoint point = findPoint(args, 0);
        if (point == null) return false;

        String newName = args.getString(1);
        if (newName.isBlank()) return false;

        blockEntity.renamePoint(point.id(), newName.trim());
        return true;
    }

    // --- warping ----------------------------------------------------------------------------

    /**
     * Starts a warp to the currently selected point. Returns true on success, or false plus a
     * reason string ("not_enough_power", "no_point_selected", "not_physicalized",
     * "not_a_warp_crystal", "already_charging", "invalid_destination", ...) so scripts can react
     * to specific failures rather than just knowing something went wrong.
     *
     * The jump itself isn't instant -- this begins the charge-up, same as pressing Warp in the
     * GUI. Poll isCharging()/getChargeSeconds() to follow it.
     */
    @LuaFunction(mainThread = true)
    public final Object[] warp() {
        WarpDriveBlockEntity.WarpResult result = blockEntity.beginWarp();
        if (result == WarpDriveBlockEntity.WarpResult.SUCCESS) {
            return new Object[]{ true };
        }
        return new Object[]{ false, result.name().toLowerCase(Locale.ROOT) };
    }

    /** Whether a warp charge-up is currently running. */
    @LuaFunction(mainThread = true)
    public final boolean isCharging() {
        return blockEntity.isCharging();
    }

    /** Seconds left in the current charge-up, or 0 when not charging. */
    @LuaFunction(mainThread = true)
    public final double getChargeSeconds() {
        return blockEntity.isCharging() ? blockEntity.getChargeTicksRemaining() / 20.0 : 0.0;
    }

    // --- helpers ----------------------------------------------------------------------------

    private static Map<String, Object> toLuaTable(WarpPoint point, Optional<UUID> selectedId) {
        Map<String, Object> entry = new HashMap<>();
        entry.put("id", point.id().toString());
        entry.put("name", point.name());
        entry.put("x", point.pos().getX());
        entry.put("y", point.pos().getY());
        entry.put("z", point.pos().getZ());
        entry.put("dimension", point.dimension().location().toString());
        entry.put("selected", selectedId.isPresent() && selectedId.get().equals(point.id()));
        return entry;
    }

    /** Resolves argument {@code argIndex} to a saved point by 1-based index, name, or id string. */
    @Nullable
    private WarpPoint findPoint(IArguments args, int argIndex) throws LuaException {
        List<WarpPoint> points = blockEntity.getWarpPoints();
        Object raw = args.get(argIndex);

        if (raw instanceof Number number) {
            int index = number.intValue();
            if (index < 1 || index > points.size()) return null;
            return points.get(index - 1);
        }

        String key = args.getString(argIndex);
        for (WarpPoint point : points) {
            if (point.id().toString().equals(key) || point.name().equals(key)) return point;
        }
        return null;
    }
}
