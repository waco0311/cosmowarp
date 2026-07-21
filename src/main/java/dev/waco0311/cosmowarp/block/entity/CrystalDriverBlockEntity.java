package dev.waco0311.cosmowarp.block.entity;

import dev.waco0311.cosmowarp.data.WarpPoint;
import dev.waco0311.cosmowarp.menu.CrystalDriverMenu;
import dev.waco0311.cosmowarp.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class CrystalDriverBlockEntity extends BlockEntity implements MenuProvider {

    // Slot 1 = source (the crystal being read from/edited). Slot 2 = target (receives copies).
    // Both slot data lives entirely on the crystal ItemStacks themselves, same as Warp Drive.
    private final ItemStackHandler slots = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    public CrystalDriverBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public ItemStackHandler getSlots() {
        return slots;
    }

    private ItemStack source() {
        return slots.getStackInSlot(0);
    }

    private ItemStack target() {
        return slots.getStackInSlot(1);
    }

    public List<WarpPoint> getSourcePoints() {
        return source().getOrDefault(ModDataComponents.WARP_POINTS.get(), List.of());
    }

    public Optional<UUID> getSelectedId() {
        return Optional.ofNullable(source().get(ModDataComponents.SELECTED_WARP_POINT.get()));
    }

    public void setSelectedId(UUID id) {
        ItemStack stack = source();
        if (stack.isEmpty()) return;
        stack.set(ModDataComponents.SELECTED_WARP_POINT.get(), id);
        setChanged();
    }

    public boolean hasTarget() {
        return !target().isEmpty();
    }

    /** Copies the selected point from the source crystal onto the target crystal (new id, same name/dim/pos). */
    public void copyToTarget() {
        ItemStack src = source();
        ItemStack tgt = target();
        if (src.isEmpty() || tgt.isEmpty()) return;

        Optional<UUID> selected = getSelectedId();
        if (selected.isEmpty()) return;

        WarpPoint point = getSourcePoints().stream()
                .filter(p -> p.id().equals(selected.get()))
                .findFirst()
                .orElse(null);
        if (point == null) return;

        WarpPoint copy = WarpPoint.newlyRegistered(point.name(), point.dimension(), point.pos());

        List<WarpPoint> targetPoints = new ArrayList<>(tgt.getOrDefault(ModDataComponents.WARP_POINTS.get(), List.of()));
        targetPoints.add(copy);
        tgt.set(ModDataComponents.WARP_POINTS.get(), List.copyOf(targetPoints));
        setChanged();
    }

    /** Deletes the selected point from the source crystal. Only allowed while slot 2 is empty. */
    public void deleteFromSource() {
        if (hasTarget()) return;
        ItemStack src = source();
        if (src.isEmpty()) return;

        Optional<UUID> selected = getSelectedId();
        if (selected.isEmpty()) return;

        List<WarpPoint> updated = getSourcePoints().stream()
                .filter(p -> !p.id().equals(selected.get()))
                .toList();
        src.set(ModDataComponents.WARP_POINTS.get(), updated);
        src.remove(ModDataComponents.SELECTED_WARP_POINT.get());
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.cosmowarp.crystal_driver");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CrystalDriverMenu(containerId, playerInventory, worldPosition);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Slots", slots.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        slots.deserializeNBT(registries, tag.getCompound("Slots"));
    }
}
