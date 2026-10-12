package com.minecraftmode.craft;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * One of the mod's crops ({@link Crops}): four stages like beetroots, grown only by random ticks (bone meal does nothing, so
 * farming experience cannot be bought with it). Basic crops grow about as fast as wheat, advanced ones a third slower.
 */
public class FarmCropBlock extends CropBlock {
	public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
	private static final VoxelShape[] SHAPES = Block.boxes(3, age -> Block.column(16.0, 0.0, 3 + age * 3));

	private final Crops.CropDef def;

	public FarmCropBlock(final Crops.CropDef def, final Properties properties) {
		super(properties);
		this.def = def;
	}

	public Crops.CropDef def() {
		return this.def;
	}

	@Override
	protected IntegerProperty getAgeProperty() {
		return AGE;
	}

	@Override
	public int getMaxAge() {
		return 3;
	}

	@Override
	protected ItemLike getBaseSeedId() {
		return Crops.planting(this.def);
	}

	@Override
	protected void randomTick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
		if (random.nextInt(this.def.advanced() ? Crops.ADVANCED_SLOWNESS : Crops.BASIC_SLOWNESS) == 0) {
			super.randomTick(state, level, pos, random);
		}
	}

	@Override
	public boolean isValidBonemealTarget(final LevelReader level, final BlockPos pos, final BlockState state, final BonemealSource source) {
		return false;
	}

	@Override
	protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AGE);
	}

	@Override
	protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
		return SHAPES[this.getAge(state)];
	}
}
