package com.kalioupo.equipment.menu;

import com.kalioupo.equipment.item.BackpackItem;
import com.kalioupo.equipment.registry.ModMenus;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import org.jetbrains.annotations.Nullable;

/**
 * Storage grid sized by the configured capacity, plus the player inventory.
 *
 * <p>Scrolling is a <b>server-side</b> window: the server maps slot {@code i} to
 * {@code backing[i + offset]}, while the client simply stores whatever stack was synced for
 * slot {@code i}. Remapping on the client as well would race with the separately synced scroll
 * offset and produced ghost items.
 */
public class BackpackMenu extends ChestMenu {
	public static final int BACKING_SIZE = 256;

	@Nullable
	private final SimpleContainer backing;
	@Nullable
	private final ScrollingContainer view;
	private final ItemStack stack;
	@Nullable
	private final Player player;
	private final int capacity;
	private final int rows;
	private int scrollRow;
	private int usedSlots;

	/** Client-side constructor. */
	public BackpackMenu(int syncId, Inventory playerInventory, int packed) {
		this(syncId, playerInventory, unpackSlot(packed), unpackCapacity(packed), unpackRows(packed), ItemStack.EMPTY, null,
			null, new SimpleContainer(unpackRows(packed) * 9));
	}

	/** Server-side constructor. */
	public BackpackMenu(int syncId, Inventory playerInventory, SimpleContainer backing,
						ItemStack stack, Player player, int slot, int capacity, int rows) {
		this(syncId, playerInventory, slot, capacity, rows, stack, player, backing,
			new ScrollingContainer(backing, rows * 9));
	}

	private BackpackMenu(int syncId, Inventory playerInventory, int slot, int capacity, int rows,
						 ItemStack stack, @Nullable Player player,
						 @Nullable SimpleContainer backing, Container container) {
		super(ModMenus.BACKPACK, syncId, playerInventory, container, rows);
		this.backing = backing;
		this.view = container instanceof ScrollingContainer scrolling ? scrolling : null;
		this.stack = stack;
		this.player = player;
		this.capacity = capacity;
		this.rows = rows;

		int visible = rows * 9;

		// Visible storage slots: reject backpacks, and keep the container index right.
		for (int i = 0; i < visible; i++) {
			Slot old = this.slots.get(i);
			BackpackSlot replacement = new BackpackSlot(container, i, old.x, old.y);
			replacement.index = i;
			this.slots.set(i, replacement);
		}

		// The slot that holds the open backpack itself: completely frozen.
		int bagSlot = visible + 27 + slot;
		if (slot >= 0 && slot <= 8 && bagSlot < this.slots.size()) {
			Slot old = this.slots.get(bagSlot);
			LockedSlot replacement = new LockedSlot(old.container, old.getContainerSlot(), old.x, old.y);
			replacement.index = old.index;
			this.slots.set(bagSlot, replacement);
		}

		this.addDataSlot(new DataSlot() {
			@Override
			public int get() {
				return BackpackMenu.this.scrollRow;
			}

			@Override
			public void set(int value) {
				BackpackMenu.this.applyScroll(value);
			}
		});

		this.addDataSlot(new DataSlot() {
			@Override
			public int get() {
				return BackpackMenu.this.countUsed();
			}

			@Override
			public void set(int value) {
				BackpackMenu.this.usedSlots = value;
			}
		});

		this.addSlotListener(new ContainerListener() {
			@Override
			public void slotChanged(AbstractContainerMenu menu, int slotIndex, ItemStack changed) {
				save();
			}

			@Override
			public void dataChanged(AbstractContainerMenu menu, int index, int value) {
			}
		});
	}

	public int getCapacity() {
		return this.capacity;
	}

	public int getVisibleRows() {
		return this.rows;
	}

	public int getUsedSlots() {
		return this.usedSlots;
	}

	public int getTotalRows() {
		return this.capacity / 9;
	}

	public boolean canScroll() {
		return this.getTotalRows() > this.rows;
	}

	public int getScrollRow() {
		return this.scrollRow;
	}

	public int getMaxScrollRow() {
		return Math.max(0, this.getTotalRows() - this.rows);
	}

	/** Number of non-empty slots inside the configured capacity (server side). */
	private int countUsed() {
		if (this.backing == null) {
			return this.usedSlots;
		}

		int used = 0;
		for (int i = 0; i < this.capacity; i++) {
			if (!this.backing.getItem(i).isEmpty()) {
				used++;
			}
		}

		return used;
	}

	private void applyScroll(int row) {
		this.scrollRow = Mth.clamp(row, 0, this.getMaxScrollRow());

		if (this.view != null) {
			this.view.setOffset(this.scrollRow * 9);
		}
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (id == 0 || id == 1) {
			applyScroll(this.scrollRow + (id == 0 ? -1 : 1));
			return true;
		}

		return false;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = this.slots.get(index);
		if (!slot.hasItem()) {
			return ItemStack.EMPTY;
		}

		ItemStack stack = slot.getItem();

		// Shift-clicking / auto-moving a backpack must do nothing at all.
		if (stack.getItem() instanceof BackpackItem) {
			return ItemStack.EMPTY;
		}

		ItemStack original = stack.copy();
		int storageSlots = this.rows * 9;

		if (index < storageSlots) {
			if (!this.moveItemStackTo(stack, storageSlots, this.slots.size(), true)) {
				return ItemStack.EMPTY;
			}
		} else if (!this.moveItemStackTo(stack, 0, storageSlots, false) && !this.moveToOtherPages(stack, storageSlots)) {
			return ItemStack.EMPTY;
		}

		if (stack.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}

		if (stack.getCount() == original.getCount()) {
			return ItemStack.EMPTY;
		}

		slot.onTake(player, stack);
		return original;
	}

	/**
	 * When the visible page is full, walk the other pages and keep inserting. The menu stays on
	 * the page that received the items so the player can see where they went.
	 */
	private boolean moveToOtherPages(ItemStack stack, int storageSlots) {
		int maxScroll = this.getMaxScrollRow();
		if (maxScroll <= 0) {
			return false;
		}

		int start = this.scrollRow;
		boolean moved = false;

		for (int step = 1; step <= maxScroll && !stack.isEmpty(); step++) {
			int row = (start + step) % (maxScroll + 1);
			applyScroll(row);
			int before = stack.getCount();
			this.moveItemStackTo(stack, 0, storageSlots, false);

			if (stack.getCount() != before) {
				moved = true;
			}
		}

		if (!moved) {
			applyScroll(start);
		}

		return moved;
	}

	@Override
	public void removed(Player player) {
		super.removed(player);

		if (!player.level().isClientSide()) {
			save();
		}
	}

	private void save() {
		if (this.player == null || this.backing == null || this.stack.isEmpty()) {
			return;
		}

		// Never write the backpack into itself; that created a self-reference and lost data.
		for (int i = 0; i < BACKING_SIZE; i++) {
			if (this.backing.getItem(i) == this.stack) {
				return;
			}
		}

		this.stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(this.backing.getItems()));
	}

	public static int pack(int slot, int capacity, int rows) {
		return (slot & 0xF) | ((capacity & 0xFFF) << 4) | ((rows & 0xFF) << 16);
	}

	public static int unpackSlot(int packed) {
		return packed & 0xF;
	}

	public static int unpackCapacity(int packed) {
		int capacity = (packed >> 4) & 0xFFF;
		return capacity < 9 ? 54 : capacity;
	}

	public static int unpackRows(int packed) {
		int rows = (packed >> 16) & 0xFF;
		return Math.max(1, Math.min(16, rows < 1 ? 6 : rows));
	}
}
