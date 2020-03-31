package com.campanion.block;

import com.campanion.blockentity.RopeBridgePlanksBlockEntity;
import com.campanion.ropebridge.RopeBridge;
import com.campanion.ropebridge.RopeBridgePlank;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.EntityContext;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.BooleanBiFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class RopeBridgePlanksBlock extends Block implements BlockEntityProvider {

	public RopeBridgePlanksBlock(Settings settings) {
		super(settings);
	}

	@Override
	public BlockEntity createBlockEntity(BlockView view) {
		return new RopeBridgePlanksBlockEntity();
	}

	@Override
	public BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.MODEL;
	}

	@Override
	public VoxelShape getCullingShape(BlockState STATE, BlockView view, BlockPos pos) {
		return VoxelShapes.empty();
	}

	@Override
	public boolean isTranslucent(BlockState state, BlockView view, BlockPos pos) {
		return true;
	}

	@Override
	public boolean canSuffocate(BlockState state, BlockView view, BlockPos pos) {
		return false;
	}

	@Override
	public boolean isSimpleFullBlock(BlockState state, BlockView view, BlockPos pos) {
		return false;
	}

	@Override
	public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		BlockEntity entity = world.getBlockEntity(pos);
		if(entity instanceof RopeBridgePlanksBlockEntity) {
			RopeBridgePlanksBlockEntity be = (RopeBridgePlanksBlockEntity) entity;

			this.scheduleRemoved(world, pos);
			boolean hasMaster = be.getPlanks().stream().anyMatch(RopeBridgePlank::isMaster);
			boolean removed = be.removeBroken();
			boolean deleted = be.getPlanks().isEmpty() && this.canBeCompletelyRemoved();
			if(removed && hasMaster) {
				world.playLevelEvent(2001, pos, getRawIdFromState(state));
			}
			if(deleted) {
				world.setBlockState(pos, Blocks.AIR.getDefaultState());
			}
		}
	}

	protected boolean canBeCompletelyRemoved() {
		return true;
	}

	private void scheduleRemoved(World world, BlockPos pos) {
		Set<Pair<BlockPos, BlockPos>> brokenLines = new HashSet<>();
		BlockEntity entity = world.getBlockEntity(pos);
		if(entity instanceof RopeBridgePlanksBlockEntity) {
			for (RopeBridgePlank plank : ((RopeBridgePlanksBlockEntity) entity).getPlanks()) {
				if(plank.isBroken()) {
					brokenLines.add(Pair.of(plank.getFrom(), plank.getTo()));
				}
			}
		}

		Set<BlockPos> neighbours = new HashSet<>();
		for (int x = -1; x <= 1; x++) {
			for (int y = -1; y <= 1; y++) {
				for (int z = -1; z <= 1; z++) {
					if(x != 0 || y != 0 || z != 0) {
						BlockPos off = pos.add(x, y, z);
						BlockEntity be = world.getBlockEntity(off);
						if(be instanceof RopeBridgePlanksBlockEntity) {
							((RopeBridgePlanksBlockEntity) be).getPlanks()
								.stream()
								.filter(plank -> brokenLines.contains(Pair.of(plank.getFrom(), plank.getTo())))
								.forEach(plank -> {
									plank.setBroken();
									neighbours.add(off);
								});
						}
					}
				}
			}
		}

		for (BlockPos neighbour : neighbours) {
			world.getBlockTickScheduler().schedule(neighbour, world.getBlockState(neighbour).getBlock(), 1);
		}
	}

	@Override
	public void onBlockRemoved(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
		BlockEntity entity = world.getBlockEntity(pos);
		if(entity instanceof RopeBridgePlanksBlockEntity) {
			for (RopeBridgePlank plank : ((RopeBridgePlanksBlockEntity) entity).getPlanks()) {
				plank.setBroken();
			}
		}
		this.scheduleRemoved(world, pos);
		super.onBlockRemoved(state, world, pos, newState, moved);
	}

	@Override
	public VoxelShape getRayTraceShape(BlockState state, BlockView view, BlockPos pos) {
		BlockEntity entity = view.getBlockEntity(pos);
		if (entity instanceof RopeBridgePlanksBlockEntity) {
			return ((RopeBridgePlanksBlockEntity) entity).getCutPlankShape();
		}
		return super.getRayTraceShape(state, view, pos);
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockView view, BlockPos pos, EntityContext context) {
		return this.getRayTraceShape(state, view, pos);
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView view, BlockPos pos, EntityContext context) {
		BlockEntity entity = view.getBlockEntity(pos);
		if (entity instanceof RopeBridgePlanksBlockEntity) {
			return ((RopeBridgePlanksBlockEntity) entity).getFullPlankShape();
		}
		return super.getOutlineShape(state, view, pos, context);
	}
}
