package dev.waco0311.cosmowarp.block;

import dev.waco0311.cosmowarp.block.entity.WarpDriveBlockEntity;
import dev.waco0311.cosmowarp.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Directional console block with a "has_crystal" state, toggled by
 * WarpDriveBlockEntity whenever the crystal/card slot's contents change, so the
 * model can show the crystal cube only when something is actually inserted.
 */
public class WarpDriveBlock extends HorizontalDirectionalBlock implements EntityBlock {

    public static final com.mojang.serialization.MapCodec<WarpDriveBlock> CODEC = simpleCodec(WarpDriveBlock::new);

    public static final BooleanProperty HAS_CRYSTAL = BooleanProperty.create("has_crystal");

    private static final VoxelShape SHAPE = net.minecraft.world.phys.shapes.Shapes.box(
            0.0, 0.0, 0.0625, 1.0, 1.0, 1.0);

    public WarpDriveBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(HAS_CRYSTAL, false));
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HAS_CRYSTAL);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(HAS_CRYSTAL, false);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WarpDriveBlockEntity(ModBlockEntities.WARP_DRIVE.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (lvl, pos, st, be) -> {
            if (be instanceof WarpDriveBlockEntity warpDrive) warpDrive.tick();
        };
    }

    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                                   net.minecraft.world.entity.player.Player player,
                                                                   BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof net.minecraft.world.MenuProvider provider) {
            player.openMenu(provider, buf -> buf.writeBlockPos(pos));
        }
        return net.minecraft.world.InteractionResult.SUCCESS;
    }
}
