package com.kalioupo.equipment.menu;

import com.kalioupo.equipment.block.SmelterBlockEntity;
import com.kalioupo.equipment.block.SmelterRecipes;
import com.kalioupo.equipment.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class SmelterMenu extends AbstractContainerMenu {
	private final Container container;
	private final ContainerData data;

	public SmelterMenu(int syncId, Inventory playerInventory, Container container, ContainerData data) {
		super(ModMenus.SMELTER, syncId);
		this.container = container;
		this.data = data;

		for (int i = 0; i < SmelterBlockEntity.INPUT_SLOTS; i++) {
			int x = i < 3 ? 8 + i * 18 : 8 + (i - 3) * 18;
			int y = i < 3 ? 17 : 35;
			this.addSlot(new Slot(container, i, x, y));
		}

		this.addSlot(new Slot(container, SmelterBlockEntity.OUTPUT_SLOT, 110, 35) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return false;
			}

			@Override
			public void onTake(Player player, ItemStack stack) {
				super.onTake(player, stack);

				if (container instanceof SmelterBlockEntity smelter) {
					smelter.awardExperience(player);
				}
			}
		});

		this.addSlot(new Slot(container, SmelterBlockEntity.FUEL_SLOT, 44, 53));

		this.addStandardInventorySlots(playerInventory, 8, 84);
		this.addDataSlots(data);
	}

	/** Client-side constructor. */
	public SmelterMenu(int syncId, Inventory playerInventory, BlockPos pos) {
		this(syncId, playerInventory, new SimpleContainer(SmelterBlockEntity.SIZE), new SimpleContainerData(4));
	}

	public int getProgress() {
		return this.data.get(0);
	}

	public int getTotalTime() {
		return this.data.get(1);
	}

	public int getBurnTime() {
		return this.data.get(2);
	}

	public int getBurnDuration() {
		return this.data.get(3);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = this.slots.get(index);

		if (!slot.hasItem()) {
			return ItemStack.EMPTY;
		}

		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();

		if (index < SmelterBlockEntity.SIZE) {
			if (!this.moveItemStackTo(stack, SmelterBlockEntity.SIZE, this.slots.size(), true)) {
				return ItemStack.EMPTY;
			}
		} else if (SmelterRecipes.burnTime(stack) > 0) {
			if (!this.moveItemStackTo(stack, SmelterBlockEntity.FUEL_SLOT, SmelterBlockEntity.FUEL_SLOT + 1, false)) {
				return ItemStack.EMPTY;
			}
		} else if (!this.moveItemStackTo(stack, 0, SmelterBlockEntity.INPUT_SLOTS, false)) {
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

	@Override
	public boolean stillValid(Player player) {
		return this.container.stillValid(player);
	}
}
