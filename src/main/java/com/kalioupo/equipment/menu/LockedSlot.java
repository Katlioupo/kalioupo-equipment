package com.kalioupo.equipment.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * A slot that cannot be picked up or placed into. Used for the inventory slot that holds the
 * backpack currently being edited: the client refuses the interaction itself, so the item never
 * leaves its slot and no "pick up -> rejected -> snap back" flicker happens.
 */
public class LockedSlot extends Slot {
	public LockedSlot(Container container, int slot, int x, int y) {
		super(container, slot, x, y);
	}

	@Override
	public boolean mayPickup(Player player) {
		return false;
	}

	@Override
	public boolean mayPlace(ItemStack stack) {
		return false;
	}
}
