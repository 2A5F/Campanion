package com.campanion.item;

import net.minecraft.block.DispenserBlock;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;

public class BackpackItem extends Item {
	public final Type type;

	public BackpackItem(Type type, Item.Settings settings) {
		super(settings);
		this.type = type;
		DispenserBlock.registerBehavior(this, ArmorItem.DISPENSER_BEHAVIOR);
	}

	public enum Type {
		DAY_PACK(9),
		CAMPING_PACK(18),
		HIKING_PACK(27);

		private int slots;

		Type(int slots) {
			this.slots = slots;
		}

		public int getSlots() {
			return slots;
		}
	}
}
