package com.kalioupo.equipment.menu;

import com.kalioupo.equipment.item.BackpackItem;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * A normal slot that refuses to hold our backpack, so a backpack can never contain
 * another backpack (which used to allow cycles and wipe the contents).
 */
public class BackpackSlot extends Slot {
	public BackpackSlot(Container container, int slot, int x, int y) {
		super(container, slot, x, y);
	}

	@Override
	public boolean mayPlace(ItemStack stack) {
		return !(stack.getItem() instanceof BackpackItem);
	}
}
