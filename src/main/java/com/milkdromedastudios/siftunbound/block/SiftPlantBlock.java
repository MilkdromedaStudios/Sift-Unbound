package com.milkdromedastudios.siftunbound.block;

import com.milkdromedastudios.siftunbound.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Short Sift grass and teal sprouts. Short Sift grass grows into tall Sift grass with bone meal. */
public class SiftPlantBlock extends VegetationBlock implements BonemealableBlock {
	private static final VoxelShape SHAPE = Block.column(12.0, 0.0, 13.0);
	private final boolean growsTall;

	public SiftPlantBlock(boolean growsTall, BlockBehaviour.Properties properties) {
		super(properties);
		this.growsTall = growsTall;
	}


	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, BonemealSource source) {
		return growsTall && ModBlocks.TALL_SIFT_GRASS.defaultBlockState().canSurvive(level, pos) && level.isEmptyBlock(pos.above());
	}

	@Override
	public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
		return true;
	}

	@Override
	public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
		DoublePlantBlock.placeAt(level, ModBlocks.TALL_SIFT_GRASS.defaultBlockState(), pos, Block.UPDATE_CLIENTS);
	}
}
