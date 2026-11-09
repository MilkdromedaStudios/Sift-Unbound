package com.milkdromedastudios.siftunbound.portal;

import com.milkdromedastudios.siftunbound.registry.ModBlocks;
import com.milkdromedastudios.siftunbound.registry.ModTags;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Finds and fills the rectangular Warden Portal frame (reinforced deepslate, like the one in the centre of every
 * ancient city: a 20x6 opening) with Sift portal blocks.
 */
public final class WardenPortal {
	public static final int MIN_WIDTH = 2;
	public static final int MIN_HEIGHT = 3;
	public static final int MAX_SIZE = 23;

	/** A found opening: bottom-left interior corner, the horizontal axis it spans, and its size. */
	public record Shape(BlockPos bottomLeft, Direction.Axis axis, int width, int height) {
		public Direction right() {
			return axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
		}

		public Iterable<BlockPos> interior() {
			BlockPos top = bottomLeft.relative(right(), width - 1).above(height - 1);
			return BlockPos.betweenClosed(bottomLeft, top);
		}
	}

	public enum Result { LIT, NO_FRAME, TOO_BIG }

	public static boolean isFrame(BlockState state) {
		return state.is(ModTags.WARDEN_PORTAL_FRAME);
	}

	/** Blocks the tuner may clear from inside a frame (ancient-city frames can be filled with leftover terrain). */
	private static boolean isClearable(BlockGetter level, BlockPos pos, BlockState state) {
		if (state.isAir() || state.canBeReplaced() || state.is(ModBlocks.SIFT_PORTAL)) {
			return true;
		}
		return !isFrame(state) && !state.hasBlockEntity() && state.getDestroySpeed(level, pos) >= 0.0F;
	}

	/**
	 * Tries to find a complete frame around {@code start} in either horizontal axis.
	 * {@code start} may be inside the opening or on the frame itself.
	 */
	public static Optional<Shape> find(BlockGetter level, BlockPos start) {
		for (Direction.Axis axis : new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}) {
			Direction right = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
			BlockPos[] candidates = {start, start.above(), start.below(), start.relative(right), start.relative(right.getOpposite())};
			for (BlockPos c : candidates) {
				if (isFrame(level.getBlockState(c))) {
					continue;
				}
				Optional<Shape> s = findFrom(level, c, axis);
				if (s.isPresent()) {
					return s;
				}
			}
		}
		return Optional.empty();
	}

	private static Optional<Shape> findFrom(BlockGetter level, BlockPos inside, Direction.Axis axis) {
		Direction right = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
		Direction left = right.getOpposite();
		// walk down to the bottom edge
		BlockPos p = inside;
		int steps = 0;
		while (!isFrame(level.getBlockState(p.below()))) {
			if (++steps > MAX_SIZE || !isClearable(level, p.below(), level.getBlockState(p.below()))) {
				return Optional.empty();
			}
			p = p.below();
		}
		// walk left to the side edge
		steps = 0;
		while (!isFrame(level.getBlockState(p.relative(left)))) {
			if (++steps > MAX_SIZE || !isClearable(level, p.relative(left), level.getBlockState(p.relative(left)))) {
				return Optional.empty();
			}
			p = p.relative(left);
		}
		BlockPos bottomLeft = p;
		// width along the bottom row
		int width = 0;
		BlockPos q = bottomLeft;
		while (!isFrame(level.getBlockState(q))) {
			if (++width > MAX_SIZE || !isFrame(level.getBlockState(q.below()))) {
				return Optional.empty();
			}
			q = q.relative(right);
		}
		// height along the left column
		int height = 0;
		q = bottomLeft;
		while (!isFrame(level.getBlockState(q))) {
			if (++height > MAX_SIZE || !isFrame(level.getBlockState(q.relative(left)))) {
				return Optional.empty();
			}
			q = q.above();
		}
		if (width < MIN_WIDTH || height < MIN_HEIGHT) {
			return Optional.empty();
		}
		// verify the whole border and that the interior can be cleared
		for (int x = 0; x < width; x++) {
			if (!isFrame(level.getBlockState(bottomLeft.relative(right, x).above(height)))) {
				return Optional.empty();
			}
		}
		for (int y = 0; y < height; y++) {
			if (!isFrame(level.getBlockState(bottomLeft.relative(right, width).above(y)))) {
				return Optional.empty();
			}
		}
		Shape shape = new Shape(bottomLeft.immutable(), axis, width, height);
		for (BlockPos ip : shape.interior()) {
			if (!isClearable(level, ip, level.getBlockState(ip))) {
				return Optional.empty();
			}
		}
		return Optional.of(shape);
	}

	public static void fill(LevelAccessor level, Shape shape) {
		BlockState portal = ModBlocks.SIFT_PORTAL.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_AXIS, shape.axis());
		for (BlockPos p : shape.interior()) {
			level.setBlock(p, portal, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
		}
	}

	public static Result tryLight(Level level, BlockPos start) {
		Optional<Shape> shape = find(level, start);
		if (shape.isEmpty()) {
			return Result.NO_FRAME;
		}
		fill(level, shape.get());
		return Result.LIT;
	}

	/** True if the in-plane neighbours of a portal block are still portal or frame. */
	public static boolean isIntact(BlockGetter level, BlockPos pos, Direction.Axis axis) {
		Direction right = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
		for (Direction d : new Direction[]{Direction.UP, Direction.DOWN, right, right.getOpposite()}) {
			BlockState n = level.getBlockState(pos.relative(d));
			if (!n.is(ModBlocks.SIFT_PORTAL) && !isFrame(n)) {
				return false;
			}
		}
		return true;
	}

	/** Builds a small (2x3 opening) frame + portal centred at {@code base}, which is the bottom-left opening block. */
	public static Shape buildSmallPortal(LevelAccessor level, BlockPos base, Direction.Axis axis, BlockState platform) {
		Direction right = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
		Direction forward = right.getClockWise();
		BlockState frame = Blocks.REINFORCED_DEEPSLATE.defaultBlockState();
		// clear a 4x5x3 space and lay a platform under it
		for (int a = -1; a <= 2; a++) {
			for (int f = -1; f <= 1; f++) {
				BlockPos column = base.relative(right, a).relative(forward, f);
				BlockPos floor = column.below();
				if (!level.getBlockState(floor).isSolidRender()) {
					level.setBlock(floor, platform, Block.UPDATE_ALL);
				}
				for (int y = 0; y < 4; y++) {
					if (f != 0 || a < 0 || a > 1 || y > 2) {
						level.setBlock(column.above(y), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
					}
				}
			}
		}
		for (int a = -1; a <= 2; a++) {
			level.setBlock(base.relative(right, a).below(), frame, Block.UPDATE_ALL);
			level.setBlock(base.relative(right, a).above(3), frame, Block.UPDATE_ALL);
		}
		for (int y = 0; y < 3; y++) {
			level.setBlock(base.relative(right, -1).above(y), frame, Block.UPDATE_ALL);
			level.setBlock(base.relative(right, 2).above(y), frame, Block.UPDATE_ALL);
		}
		Shape shape = new Shape(base.immutable(), axis, 2, 3);
		fill(level, shape);
		return shape;
	}

	private WardenPortal() {
	}
}
