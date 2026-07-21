package dev.waco0311.cosmowarp.menu;

import dev.waco0311.cosmowarp.block.entity.CrystalDriverBlockEntity;
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

public class CrystalDriverMenu extends AbstractContainerMenu {

    // Single source of truth, shared with CrystalDriverScreen (same pattern as WarpDriveMenu).
    public static final int PANEL_H = 140;
    public static final int SOURCE_SLOT_X = 10;
    public static final int SOURCE_SLOT_Y = 8;
    public static final int TARGET_SLOT_X = 40;
    public static final int TARGET_SLOT_Y = 8;
    public static final int PLAYER_INV_Y = PANEL_H + 14;
    public static final int HOTBAR_Y = PLAYER_INV_Y + 58;

    public final CrystalDriverBlockEntity blockEntity;
    private final ContainerLevelAccess access;

    public CrystalDriverMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        super(ModMenuTypes.CRYSTAL_DRIVER_MENU.get(), containerId);
        this.access = ContainerLevelAccess.create(playerInventory.player.level(), pos);

        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        if (!(be instanceof CrystalDriverBlockEntity driver)) {
            throw new IllegalStateException("No CrystalDriverBlockEntity at " + pos);
        }
        this.blockEntity = driver;

        addSlot(new SlotItemHandler(blockEntity.getSlots(), 0, SOURCE_SLOT_X, SOURCE_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.WARP_CRYSTAL.get());
            }
        });
        addSlot(new SlotItemHandler(blockEntity.getSlots(), 1, TARGET_SLOT_X, TARGET_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.WARP_CRYSTAL.get());
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, PLAYER_INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, HOTBAR_Y));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            result = stackInSlot.copy();

            if (index == 0 || index == 1) {
                if (!moveItemStackTo(stackInSlot, 2, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (stackInSlot.is(ModItems.WARP_CRYSTAL.get())) {
                    if (!moveItemStackTo(stackInSlot, 0, 2, false)) {
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
