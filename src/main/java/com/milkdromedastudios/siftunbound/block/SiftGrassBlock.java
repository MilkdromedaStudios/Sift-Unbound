package com.milkdromedastudios.siftunbound.block;

import com.milkdromedastudios.siftunbound.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Salmon-coloured Sift grass. Spreads onto nearby Sift soil when lit, decays to soil when smothered,
 * and grows Sift plants when bone-mealed.
 */
public class SiftGrassBlock extends Block implements BonemealableBlock {

	public SiftGrassBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}


	private static boolean canStayGrass(LevelReader level, BlockPos pos) {
		BlockState above = level.getBlockState(pos.above());
		return !above.isSolidRender() && above.getFluidState().isEmpty();
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (!canStayGrass(level, pos)) {
			level.setBlockAndUpdate(pos, ModBlocks.SIFT_SOIL.defaultBlockState());
			return;
		}
		if (level.getMaxLocalRawBrightness(pos.above()) >= 9) {
			for (int i = 0; i < 4; i++) {
				BlockPos target = pos.offset(random.nextInt(3) - 1, random.nextInt(5) - 3, random.nextInt(3) - 1);
				if (level.getBlockState(target).is(ModBlocks.SIFT_SOIL) && canStayGrass(level, target)) {
					level.setBlockAndUpdate(target, defaultBlockState());
				}
			}
		}
	}

	@Override
	public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, BonemealSource source) {
		return level.getBlockState(pos.above()).isAir();
	}

	@Override
	public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
		return true;
	}

	@Override
	public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
		BlockPos start = pos.above();
		for (int i = 0; i < 64; i++) {
			BlockPos p = start;
			boolean ok = true;
			for (int j = 0; j < i / 16; j++) {
				p = p.offset(random.nextInt(3) - 1, (random.nextInt(3) - 1) * random.nextInt(3) / 2, random.nextInt(3) - 1);
				if (!level.getBlockState(p.below()).is(this) || level.getBlockState(p).isCollisionShapeFullBlock(level, p)) {
					ok = false;
					break;
				}
			}
			if (!ok || !level.getBlockState(p).isAir()) {
				continue;
			}
			int roll = random.nextInt(16);
			BlockState plant;
			if (roll == 0) {
				plant = ModBlocks.LULLABY_BLOOM.defaultBlockState();
			} else if (roll < 3) {
				plant = ModBlocks.TEAL_SPROUTS.defaultBlockState();
			} else if (roll < 5) {
				plant = ModBlocks.TALL_SIFT_GRASS.defaultBlockState();
				if (plant.canSurvive(level, p) && level.isEmptyBlock(p.above())) {
					DoublePlantBlock.placeAt(level, plant, p, Block.UPDATE_CLIENTS);
				}
				continue;
			} else {
				plant = ModBlocks.SHORT_SIFT_GRASS.defaultBlockState();
			}
			if (plant.canSurvive(level, p)) {
				level.setBlock(p, plant, Block.UPDATE_ALL);
			}
		}
	}
}
