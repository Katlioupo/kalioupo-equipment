package com.kalioupo.equipment.block;

import com.kalioupo.equipment.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.List;

public class SmelterBlockEntity extends BlockEntity implements Container, WorldlyContainer, ContainerData {
	public static final int INPUT_SLOTS = 5;
	public static final int OUTPUT_SLOT = 5;
	public static final int FUEL_SLOT = 6;
	public static final int SIZE = 7;

	private static final int DATA_PROGRESS = 0;
	private static final int DATA_TOTAL = 1;
	private static final int DATA_BURN = 2;
	private static final int DATA_BURN_TOTAL = 3;

	private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);

	private int burnTime;
	private int burnDuration;
	private int cookingProgress;
	private int cookingTotal = 200;
	private float experience;

	public SmelterBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.SMELTER, pos, state);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, SmelterBlockEntity smelter) {
		if (level.isClientSide()) {
			return;
		}

		boolean wasLit = smelter.burnTime > 0;
		boolean changed = false;

		if (smelter.burnTime > 0) {
			smelter.burnTime--;
		}

		SmelterRecipes.SmeltRecipe recipe = SmelterRecipes.find(level, smelter.items);
		boolean canSmelt = recipe != null && smelter.canAccept(recipe);

		if (smelter.burnTime == 0 && canSmelt) {
			int fuel = SmelterRecipes.burnTime(smelter.items.get(FUEL_SLOT));

			if (fuel > 0) {
				smelter.burnTime = fuel;
				smelter.burnDuration = fuel;
				smelter.consumeFuel();
				changed = true;
			}
		}

		if (smelter.burnTime > 0 && canSmelt) {
			smelter.cookingTotal = recipe.time();
			smelter.cookingProgress++;

			if (smelter.cookingProgress >= smelter.cookingTotal) {
				smelter.cookingProgress = 0;
				smelter.craft(recipe);
				changed = true;
			}
		} else if (smelter.cookingProgress > 0) {
			smelter.cookingProgress = Math.max(0, smelter.cookingProgress - 2);
			changed = true;
		}

		boolean isLit = smelter.burnTime > 0;

		if (wasLit != isLit) {
			level.setBlock(pos, state.setValue(SmelterBlock.LIT, isLit), 3);
			changed = true;
		}

		if (changed) {
			setChanged(level, pos, state);
		}
	}

	private boolean canAccept(SmelterRecipes.SmeltRecipe recipe) {
		ItemStack output = this.items.get(OUTPUT_SLOT);

		if (output.isEmpty()) {
			return true;
		}

		if (!ItemStack.isSameItemSameComponents(output, recipe.result())) {
			return false;
		}

		return output.getCount() + recipe.result().getCount() <= output.getMaxStackSize();
	}

	private void consumeFuel() {
		ItemStack fuel = this.items.get(FUEL_SLOT);
		ItemStack remainder = fuelRemainder(fuel);

		fuel.shrink(1);

		if (!fuel.isEmpty()) {
			// container fuels stack to one, so this only happens for odd stacks; better to drop the
			// leftover than to swallow it
			if (!remainder.isEmpty() && this.level != null) {
				Block.popResource(this.level, this.worldPosition, remainder);
			}

			return;
		}

		this.items.set(FUEL_SLOT, remainder);
	}

	/** Lava buckets hand back their empty bucket, the same way a furnace does. */
	private static ItemStack fuelRemainder(ItemStack fuel) {
		ItemStackTemplate template = fuel.getCraftingRemainder();

		if (template != null) {
			return template.create();
		}

		return fuel.is(Items.LAVA_BUCKET) ? new ItemStack(Items.BUCKET) : ItemStack.EMPTY;
	}

	private void craft(SmelterRecipes.SmeltRecipe recipe) {
		int[] remaining = new int[recipe.inputs().size()];

		for (int j = 0; j < remaining.length; j++) {
			remaining[j] = recipe.count(j);
		}

		for (int i = 0; i < INPUT_SLOTS; i++) {
			ItemStack stack = this.items.get(i);

			if (stack.isEmpty()) {
				continue;
			}

			for (int j = 0; j < remaining.length; j++) {
				if (remaining[j] > 0 && stack.is(recipe.inputs().get(j)) && stack.getCount() >= remaining[j]) {
					stack.shrink(remaining[j]);
					remaining[j] = 0;
					break;
				}
			}
		}

		ItemStack output = this.items.get(OUTPUT_SLOT);

		if (output.isEmpty()) {
			this.items.set(OUTPUT_SLOT, recipe.result().copy());
		} else {
			output.grow(recipe.result().getCount());
		}

		this.experience += recipe.experience();
	}

	public void awardExperience(Player player) {
		int whole = Mth.floor(this.experience);
		float fraction = this.experience - whole;

		if (fraction > 0.0F && player.getRandom().nextFloat() < fraction) {
			whole++;
		}

		if (whole > 0) {
			player.giveExperiencePoints(whole);
		}

		this.experience = 0.0F;
	}

	// ---------------------------------------------------------------- Container

	@Override
	public int getContainerSize() {
		return SIZE;
	}

	@Override
	public boolean isEmpty() {
		for (ItemStack stack : this.items) {
			if (!stack.isEmpty()) {
				return false;
			}
		}

		return true;
	}

	@Override
	public ItemStack getItem(int slot) {
		return this.items.get(slot);
	}

	@Override
	public ItemStack removeItem(int slot, int amount) {
		return ContainerHelper.removeItem(this.items, slot, amount);
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		return ContainerHelper.takeItem(this.items, slot);
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		this.items.set(slot, stack);
		this.setChanged();
	}

	@Override
	public boolean stillValid(Player player) {
		if (this.level == null || this.level.getBlockEntity(this.worldPosition) != this) {
			return false;
		}

		return player.distanceToSqr(this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.5,
			this.worldPosition.getZ() + 0.5) <= 64.0;
	}

	@Override
	public void clearContent() {
		this.items.clear();
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		if (slot == OUTPUT_SLOT) {
			return false;
		}

		if (slot == FUEL_SLOT) {
			return SmelterRecipes.burnTime(stack) > 0;
		}

		return true;
	}

	// ---------------------------------------------------------------- hopper automation

	/**
	 * Slot access per face, mirroring the vanilla furnace.
	 *
	 * <p>Without this a hopper below pulled from <b>every</b> slot, so it happily drained the items
	 * that were waiting to be smelted. Now the bottom face can only take the output, the top face
	 * only feeds the input, and the sides feed the fuel slot first and may also top up the input.</p>
	 *
	 * <p>The side order matters: a hopper fills the first slot it is allowed to use, so the fuel
	 * slot has to come first. It is also why {@link #canPlaceItemThroughFace} keeps fuel items out
	 * of the input slots on the sides - coal is a real ingredient here (carbon steel), so without
	 * that split a side hopper emptied its whole load into the input and the fuel slot stayed
	 * empty, which made fuel impossible to automate from the side at all.</p>
	 */
	private static final int[] OUTPUT = {OUTPUT_SLOT};
	private static final int[] INPUT = {0, 1, 2, 3, 4};
	private static final int[] FUEL_AND_INPUT = {FUEL_SLOT, 0, 1, 2, 3, 4};

	@Override
	public int[] getSlotsForFace(Direction direction) {
		if (direction == Direction.DOWN) {
			return OUTPUT;
		}

		return direction == Direction.UP ? INPUT : FUEL_AND_INPUT;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction direction) {
		if (direction == Direction.DOWN) {
			return false;
		}

		if (direction == Direction.UP) {
			return slot != FUEL_SLOT && canPlaceItem(slot, stack);
		}

		// sided: anything that burns may only go to the fuel slot, everything else to the input
		if (SmelterRecipes.burnTime(stack) > 0) {
			return slot == FUEL_SLOT;
		}

		return slot != FUEL_SLOT && canPlaceItem(slot, stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
		return direction == Direction.DOWN && slot == OUTPUT_SLOT;
	}

	/** Redstone comparator output: output fullness first, otherwise the smelting progress. */
	public int comparatorSignal() {
		ItemStack output = this.items.get(OUTPUT_SLOT);

		if (!output.isEmpty()) {
			return 1 + Mth.floor(14.0F * output.getCount() / output.getMaxStackSize());
		}

		if (this.burnTime <= 0 || this.cookingTotal <= 0) {
			return 0;
		}

		return 1 + Mth.floor(14.0F * this.cookingProgress / this.cookingTotal);
	}

	@Override
	public int get(int index) {
		return switch (index) {
			case DATA_PROGRESS -> this.cookingProgress;
			case DATA_TOTAL -> this.cookingTotal;
			case DATA_BURN -> this.burnTime;
			case DATA_BURN_TOTAL -> this.burnDuration;
			default -> 0;
		};
	}

	@Override
	public void set(int index, int value) {
		switch (index) {
			case DATA_PROGRESS -> this.cookingProgress = value;
			case DATA_TOTAL -> this.cookingTotal = value;
			case DATA_BURN -> this.burnTime = value;
			case DATA_BURN_TOTAL -> this.burnDuration = value;
			default -> {
			}
		}
	}

	@Override
	public int getCount() {
		return 4;
	}

	// ---------------------------------------------------------------- saving

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);

		// one key per slot, and only for slots that actually hold something: the list codec
		// rejects empty stacks ("count must be within range") and spammed the log every save
		for (int i = 0; i < SIZE; i++) {
			ItemStack stack = this.items.get(i);

			if (!stack.isEmpty()) {
				output.store("Slot" + i, ItemStack.CODEC, stack);
			}
		}

		output.putInt("BurnTime", this.burnTime);
		output.putInt("BurnDuration", this.burnDuration);
		output.putInt("CookTime", this.cookingProgress);
		output.putInt("CookTotal", this.cookingTotal);
		output.putFloat("Experience", this.experience);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);

		for (int i = 0; i < SIZE; i++) {
			final int slot = i;
			input.read("Slot" + slot, ItemStack.CODEC).ifPresent(stack -> this.items.set(slot, stack));
		}

		// older saves used a single "Items" list that never wrote successfully, but read it
		// anyway so nothing from a test session is lost
		input.read("Items", ItemStack.CODEC.listOf()).ifPresent(list -> {
			for (int i = 0; i < Math.min(list.size(), SIZE); i++) {
				if (!list.get(i).isEmpty()) {
					this.items.set(i, list.get(i));
				}
			}
		});

		this.burnTime = input.getIntOr("BurnTime", 0);
		this.burnDuration = input.getIntOr("BurnDuration", 0);
		this.cookingProgress = input.getIntOr("CookTime", 0);
		this.cookingTotal = input.getIntOr("CookTotal", 200);
		this.experience = input.getFloatOr("Experience", 0.0F);
	}
}
