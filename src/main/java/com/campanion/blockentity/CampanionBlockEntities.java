package com.campanion.blockentity;

import com.campanion.Campanion;
import com.campanion.block.CampanionBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import static com.campanion.block.CampanionBlocks.*;

public class CampanionBlockEntities {

	private static final Map<Identifier, BlockEntityType<? extends BlockEntity>> BLOCK_ENTITY_TYPES = new LinkedHashMap<>();

	public static final BlockEntityType<RopeBridgePlanksBlockEntity> ROPE_BRIDGE_PLANK = add("rope_bridge_planks", RopeBridgePlanksBlockEntity::new, ROPE_BRIDGE_ANCHOR, CampanionBlocks.ROPE_BRIDGE_PLANKS);
	public static final BlockEntityType<TentPartBlockEntity> TENT_PART = add("tent_part", TentPartBlockEntity::new, CampanionBlocks.TENT_POLE, TENT_SIDE, TENT_TOP, TENT_TOP_POLE);

	private static <T extends BlockEntity> BlockEntityType<T> add(String name, Supplier<? extends T> supplier, Block... blocks) {
		return add(name, BlockEntityType.Builder.create(supplier, blocks));
	}

	private static <T extends BlockEntity> BlockEntityType<T> add(String name, BlockEntityType.Builder<T> builder) {
		return add(name, builder.build(null));
	}

	private static <T extends BlockEntity> BlockEntityType<T> add(String name, BlockEntityType<T> blockEntityType) {
		BLOCK_ENTITY_TYPES.put(new Identifier(Campanion.MOD_ID, name), blockEntityType);
		return blockEntityType;
	}

	public static void register() {
		for (Identifier id : BLOCK_ENTITY_TYPES.keySet()) {
			Registry.register(Registry.BLOCK_ENTITY_TYPE, id, BLOCK_ENTITY_TYPES.get(id));
		}
	}

	public static Map<Identifier, BlockEntityType<? extends BlockEntity>> getBlockEntityTypes() {
		return BLOCK_ENTITY_TYPES;
	}

}
