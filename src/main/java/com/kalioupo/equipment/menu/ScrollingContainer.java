package com.kalioupo.equipment.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * A fixed-size window onto a larger backing container. Slot objects never move; scrolling only
 * changes {@link #offset}, which keeps the menu sync logic untouched.
 */
public class ScrollingContainer implements Container {
	private final SimpleContainer backing;
	private final int visible;
	private int offset;

	public ScrollingContainer(SimpleContainer backing, int visible) {
		this.backing = backing;
		this.visible = visible;
	}

	public void setOffset(int offset) {
		this.offset = Math.max(0, offset);
	}

	public int getOffset() {
		return this.offset;
	}

	@Override
	public int getContainerSize() {
		return this.visible;
	}

	@Override
	public boolean isEmpty() {
		for (int i = 0; i < this.visible; i++) {
			if (!this.getItem(i).isEmpty()) {
				return false;
			}
		}

		return true;
	}

	@Override
	public ItemStack getItem(int slot) {
		return this.backing.getItem(this.offset + slot);
	}

	@Override
	public ItemStack removeItem(int slot, int amount) {
		return this.backing.removeItem(this.offset + slot, amount);
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		return this.backing.removeItemNoUpdate(this.offset + slot);
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		this.backing.setItem(this.offset + slot, stack);
	}

	@Override
	public void setChanged() {
		this.backing.setChanged();
	}

	@Override
	public boolean stillValid(Player player) {
		return true;
	}

	@Override
	public void clearContent() {
		for (int i = 0; i < this.visible; i++) {
			this.setItem(i, ItemStack.EMPTY);
		}
	}
}
