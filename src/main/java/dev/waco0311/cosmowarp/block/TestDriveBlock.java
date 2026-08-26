package dev.waco0311.cosmowarp.block;

import com.mojang.serialization.MapCodec;
import dev.waco0311.cosmowarp.block.entity.TestDriveBlockEntity;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Test Drive: the actual mechanic (counters ship propulsion while armed, fires a distance-scaled
 * jump on a redstone edge) lives in TestDriveBlockEntity -- this class is just the block shell.
 *
 * Follows the same shape as WarpDriveBlock.java (HorizontalDirectionalBlock + EntityBlock, not
 * BaseEntityBlock -- corrected after checking the real file). FACING is placement-only right now
 * (used as the travel-direction fallback when the ship is near-stationary at charge start; see
 * TestDriveBlockEntity#travelDirection).
 *
 * No model/texture or ModCreativeTab entry yet -- intentionally held back until those are ready,
 * same as noted in the project status doc. SHAPE below is a placeholder box, same approach
 * WarpDriveBlock used before its real model existed.
 */
public class TestDriveBlock extends HorizontalDirectionalBlock implements EntityBlock {

    public static final MapCodec<TestDriveBlock> CODEC = simpleCodec(TestDriveBlock::new);

    // Placeholder shape until a real model exists -- same approach as WarpDriveBlock pre-model.
    private static final VoxelShape SHAPE = Shapes.box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);

    public TestDriveBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TestDriveBlockEntity(ModBlockEntities.TEST_DRIVE.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (lvl, pos, st, be) -> {
            if (be instanceof TestDriveBlockEntity testDrive) testDrive.tick();
        };
    }
}
