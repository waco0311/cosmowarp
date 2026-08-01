package dev.waco0311.cosmowarp.menu;

import dev.waco0311.cosmowarp.block.entity.WarpDriveBlockEntity;
import dev.waco0311.cosmowarp.registry.ModItems;
import dev.waco0311.cosmowarp.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;

public class WarpDriveMenu extends AbstractContainerMenu {

    private static final int CRYSTAL_SLOT_INDEX = 0;

    // Single source of truth for layout, shared with WarpDriveScreen so the two can never drift
    // apart again (previous bug: screen panel height changed but these stayed hardcoded).
    public static final int PANEL_H = 148;
    public static final int CRYSTAL_SLOT_X = 10;
    public static final int CRYSTAL_SLOT_Y = 8;
    public static final int PLAYER_INV_Y = PANEL_H + 14;   // 162
    public static final int HOTBAR_Y = PLAYER_INV_Y + 58;  // 220

    public final WarpDriveBlockEntity blockEntity;
    private final ContainerLevelAccess access;

    public WarpDriveMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        super(ModMenuTypes.WARP_DRIVE_MENU.get(), containerId);
        this.access = ContainerLevelAccess.create(playerInventory.player.level(), pos);

        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        if (!(be instanceof WarpDriveBlockEntity warpDrive)) {
            throw new IllegalStateException("No WarpDriveBlockEntity at " + pos);
        }
        this.blockEntity = warpDrive;

        // Crystal slot. Accepts a Warp Crystal (can trigger a warp) or a Memory Card (can
        // register/hold points but not trigger a warp -- see WarpDriveBlockEntity#beginWarp()).
        // Deliberately placed away from the warp point list's click area (see
        // WarpDriveScreen#mouseClicked) so slot clicks and list-row clicks never fight over the
        // same screen region.
        addSlot(new SlotItemHandler(blockEntity.getCrystalSlot(), 0, CRYSTAL_SLOT_X, CRYSTAL_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.WARP_CRYSTAL.get()) || stack.is(ModItems.MEMORY_CARD.get());
            }
        });

        // Player inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, PLAYER_INV_Y + row * 18));
            }
        }
        // Hotbar
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, HOTBAR_Y));
        }
    }

    public long getEnergyStored() {
        return blockEntity.getEnergyStorage().getEnergyStored();
    }

    public long getEnergyCapacity() {
        return WarpDriveBlockEntity.warpCostFE();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            result = stackInSlot.copy();

            if (index == CRYSTAL_SLOT_INDEX) {
                if (!moveItemStackTo(stackInSlot, 1, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (stackInSlot.is(ModItems.WARP_CRYSTAL.get()) || stackInSlot.is(ModItems.MEMORY_CARD.get())) {
                    if (!moveItemStackTo(stackInSlot, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    return ItemStack.EMPTY;
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, blockEntity.getBlockState().getBlock());
    }
}