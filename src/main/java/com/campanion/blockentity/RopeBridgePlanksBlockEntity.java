package com.campanion.blockentity;

import com.campanion.ropebridge.RopeBridge;
import com.campanion.ropebridge.RopeBridgePlank;
import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.BooleanBiFunction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RopeBridgePlanksBlockEntity extends BlockEntity implements BlockEntityClientSerializable {

    private final List<RopeBridgePlank> planks = new ArrayList<>();

    private VoxelShape fullPlankShape;
    private VoxelShape cutPlankShape;

    public RopeBridgePlanksBlockEntity(BlockEntityType<?> type) {
        super(type);
    }

    public RopeBridgePlanksBlockEntity() {
        super(CampanionBlockEntities.ROPE_BRIDGE_PLANK);
    }

    public List<RopeBridgePlank> getPlanks() {
        return Collections.unmodifiableList(this.planks);
    }

    public void addPlank(RopeBridgePlank plank) {
        this.planks.add(plank);
        this.fullPlankShape = null;
        this.cutPlankShape = null;
    }

    public boolean removeBroken() {
        boolean ret = this.planks.removeIf(RopeBridgePlank::isBroken);
        this.fullPlankShape = null;
        this.cutPlankShape = null;
        this.markDirty();
        if(this.world != null && !this.world.isClient) {
            this.sync();
            this.world.updateListeners(this.pos, this.getCachedState(), this.getCachedState(), 11);
        }
        return ret;
    }

    public VoxelShape getFullPlankShape() {
        if(this.fullPlankShape != null) {
            return this.fullPlankShape;
        }

        VoxelShape shape = VoxelShapes.empty();
        if(this.planks.isEmpty() || this.forceRenderStopper()) {
            shape = VoxelShapes.union(shape, this.generateStopperShape(0.5F, 0, 0.5F,0, -RopeBridge.PLANK_LENGTH/2));
            shape = VoxelShapes.union(shape, this.generateStopperShape(0.5F, 0, 0.5F,0, RopeBridge.PLANK_LENGTH/2));
        }
        for (RopeBridgePlank plank : this.planks) {
            double sin = RopeBridge.PLANK_LENGTH/2*Math.sin(plank.getyAngle());
            double cos = RopeBridge.PLANK_LENGTH/2*Math.cos(plank.getyAngle());

            double xRange = 1.5/16F + Math.abs(sin);
            double yRange = Math.abs(Math.sin(plank.getTiltAngle())) * RopeBridge.PLANK_WIDTH/2 + 1.5/16F;
            double zRange = 1.5/16F + Math.abs(cos);

            double minY = plank.getDeltaPosition().y - yRange;
            double maxY = plank.getDeltaPosition().y + yRange;

            double minX = plank.getDeltaPosition().x - xRange;
            double maxX = plank.getDeltaPosition().x + xRange;

            double minZ = plank.getDeltaPosition().z - zRange;
            double maxZ = plank.getDeltaPosition().z + zRange;

            shape = VoxelShapes.union(shape, VoxelShapes.cuboid(minX, minY, minZ, maxX, maxY, maxZ));

            if(plank.isStopper()) {
                shape = VoxelShapes.union(shape, this.generateStopperShape((float) plank.getDeltaPosition().x, (float) plank.getDeltaPosition().y, (float) plank.getDeltaPosition().z, -sin, -cos));
                shape = VoxelShapes.union(shape, this.generateStopperShape((float) plank.getDeltaPosition().x, (float) plank.getDeltaPosition().y, (float) plank.getDeltaPosition().z, sin, cos));
            }
        }

        return this.fullPlankShape = shape;
    }

    private VoxelShape generateStopperShape(float x, float y, float z, double sin, double cos) {
        float size = (RopeBridge.STOPPER_WIDTH+1) / 32F;
        return VoxelShapes.cuboid(-size, 0, -size, size, (RopeBridge.STOPPER_HEIGHT + 0.5) / 16F, size).offset(x+sin, y, z-cos);
    }

    public VoxelShape getCutPlankShape() {
        if(this.cutPlankShape != null) {
            return this.cutPlankShape;
        }
        return this.cutPlankShape = VoxelShapes.combine(this.getFullPlankShape(), VoxelShapes.fullCube(), BooleanBiFunction.AND);
    }

    public boolean forceRenderStopper() {
        return false;
    }

    @Override
    public void fromTag(CompoundTag tag) {
        this.fromClientTag(tag);
        super.fromTag(tag);
    }

    @Override
    public CompoundTag toTag(CompoundTag tag) {
        return this.toClientTag(super.toTag(tag));
    }

    @Override
    public void fromClientTag(CompoundTag tag) {
        this.planks.clear();
        this.planks.addAll(this.getFrom(tag.getList("Planks", 10)));
        this.fullPlankShape = null;
        this.cutPlankShape = null;

        if(this.world != null && this.world.isClient) {
            this.world.updateListeners(this.pos, this.getCachedState(), this.getCachedState(), 11);
        }
    }

    @Override
    public CompoundTag toClientTag(CompoundTag tag) {
        tag.put("Planks", writeTo(this.planks));
        return tag;
    }

    protected List<RopeBridgePlank> getFrom(ListTag list) {
        List<RopeBridgePlank> out = new ArrayList<>();
        for (Tag nbt : list) {
            out.add(RopeBridgePlank.deserialize((CompoundTag) nbt));
        }
        return out;
    }

    protected ListTag writeTo(List<RopeBridgePlank> planks) {
        ListTag list = new ListTag();
        for (RopeBridgePlank plank : planks) {
            list.add(RopeBridgePlank.serialize(plank));
        }
        return list;
    }
}
