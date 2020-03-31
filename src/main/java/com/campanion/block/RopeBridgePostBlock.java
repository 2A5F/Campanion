package com.campanion.block;

import com.campanion.blockentity.RopeBridgePlanksBlockEntity;
import com.campanion.blockentity.RopeBridgePostBlockEntity;
import com.campanion.item.CampanionItems;
import com.campanion.ropebridge.RopeBridge;
import com.campanion.ropebridge.RopeBridgePlank;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tag.BlockTags;
import net.minecraft.tag.ItemTags;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class RopeBridgePostBlock extends RopeBridgePlanksBlock {

    private static final String CLICKED_POSITION_KEY = "ClickedPosition";

    public RopeBridgePostBlock(Settings settings) {
        super(settings);
    }

    @Override
    public BlockEntity createBlockEntity(BlockView view) {
        return new RopeBridgePostBlockEntity();
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        BlockEntity entity = world.getBlockEntity(pos);
        if(entity instanceof RopeBridgePostBlockEntity) {
            RopeBridgePostBlockEntity be = (RopeBridgePostBlockEntity) entity;
            ItemStack stack = player.getStackInHand(hand);
            if(!world.isClient) {
                if(stack.getItem() == CampanionItems.ROPE) {
                    CompoundTag tag = stack.getOrCreateTag();
                    if(tag.contains(CLICKED_POSITION_KEY, 4)) {
                        BlockPos clickedPos = BlockPos.fromLong(tag.getLong(CLICKED_POSITION_KEY));
                        tag.remove(CLICKED_POSITION_KEY);
                        RopeBridge bridge = new RopeBridge(clickedPos, pos);
                        Optional<Text> reason = bridge.getFailureReason();
                        if(reason.isPresent()) {
                            player.addChatMessage(reason.get(), false);
                        } else {
                            be.getGhostPlanks().put(clickedPos, bridge.generateBlocks(world));
                            BlockEntity blockEntityTwo = world.getBlockEntity(clickedPos);
                            if(blockEntityTwo instanceof RopeBridgePostBlockEntity) {
                                ((RopeBridgePostBlockEntity) blockEntityTwo).getLinkedPositions().add(pos);
                            }
                            entity.markDirty();
                            be.sync();
                            if(!player.isCreative()) {
                                stack.decrement(1);
                            }
                        }
                    } else {
                        tag.putLong(CLICKED_POSITION_KEY, pos.asLong());
                    }
                }
                if(stack.getItem().isIn(ItemTags.PLANKS) && this.incrementBridge(world, player, be, true) && !player.isCreative()) {
                    stack.decrement(1);
                }
            }
        }
        return ActionResult.CONSUME;
    }

    private boolean incrementBridge(World world, PlayerEntity player, RopeBridgePostBlockEntity blockEntity, boolean generateLinked) {
        List<Pair<BlockPos, List<RopeBridgePlank>>> list = null;
        for (Map.Entry<BlockPos, List<Pair<BlockPos, List<RopeBridgePlank>>>> entry : blockEntity.getGhostPlanks().entrySet()) {
            list = entry.getValue();
            break;
        }
        if(list == null || list.isEmpty()) {
            if(generateLinked) {
                for (BlockPos position : blockEntity.getLinkedPositions()) {
                    BlockEntity linkedEntity = world.getBlockEntity(position);
                    if(linkedEntity instanceof RopeBridgePostBlockEntity) {
                        return this.incrementBridge(world, player, (RopeBridgePostBlockEntity) linkedEntity, false);
                    }
                }
            }
            return false;
        }
        for (int i = 0; i < RopeBridge.PLANKS_PER_ITEM; i++) {
            if(list.isEmpty()) {
                break;
            }
            for (Pair<BlockPos, List<RopeBridgePlank>> plankPair : list) {
                BlockPos planksPos = plankPair.getLeft();
                List<RopeBridgePlank> planks = plankPair.getRight();

                BlockEntity be = world.getBlockEntity(planksPos);
                for (int j = 0; j < Math.min(RopeBridge.PLANKS_PER_ITEM - i, planks.size()); j++) {
                    if(!(be instanceof RopeBridgePlanksBlockEntity)) {
                        if (!world.isAir(planksPos)) {
                            player.addChatMessage(new TranslatableText("message.campanion.rope_bridge.obstructed", planksPos.getX(), planksPos.getY(), planksPos.getZ()), false);
                            return false;
                        }
                        world.setBlockState(planksPos, CampanionBlocks.ROPE_BRIDGE_PLANKS.getDefaultState());
                        be = world.getBlockEntity(planksPos);
                    }

                    if(be instanceof RopeBridgePlanksBlockEntity) {
                        RopeBridgePlanksBlockEntity planksBlockEntity = (RopeBridgePlanksBlockEntity) be;
                        RopeBridgePlank plank = planks.get(0);
                        planksBlockEntity.addPlank(plank);
                        planks.remove(plank);
                        planksBlockEntity.markDirty();
                        planksBlockEntity.sync();
                        if(plank.isMaster()) {
                            i++;
                        }
                    }
                }
            }
            list.removeIf(p -> p.getRight().isEmpty());
        }
        if(list.isEmpty()) {
            player.addChatMessage(new TranslatableText("message.campanion.rope_bridge.finished"), false);
        }
        return true;
    }

    @Override
    protected boolean canBeCompletelyRemoved() {
        return false;
    }
}
