package com.kalioupo.equipment.block;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.crafting.SmokingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;

import com.kalioupo.equipment.registry.ModItems;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Smelter recipes. Each recipe lists the distinct materials it needs plus how many of each,
 * and the inputs may sit in any slot order. A single slot can supply several items, so a
 * recipe such as "4 obsidian + 1 shard + 2 diamonds" only occupies three slots.
 */
public final class SmelterRecipes {
	public record SmeltRecipe(List<Item> inputs, List<Integer> counts, ItemStack result, int time, float experience) {
		public SmeltRecipe(List<Item> inputs, ItemStack result, int time, float experience) {
			this(inputs, Collections.nCopies(inputs.size(), 1), result, time, experience);
		}

		public int count(int index) {
			return this.counts.get(index);
		}
	}

	private static final List<SmeltRecipe> RECIPES = List.of(
		// alloy recipes: one of every listed input, any slot order
		new SmeltRecipe(List.of(Items.IRON_INGOT, Items.COAL), new ItemStack(ModItems.CARBON_STEEL_INGOT), 200, 0.35F),
		new SmeltRecipe(List.of(Items.IRON_INGOT, Items.QUARTZ), new ItemStack(ModItems.QUARTZ_STEEL_INGOT), 200, 0.35F),
		new SmeltRecipe(List.of(Items.COPPER_INGOT, Items.IRON_NUGGET), new ItemStack(ModItems.COPPER_ALLOY_INGOT), 200, 0.35F),
		new SmeltRecipe(List.of(Items.COPPER_INGOT, Items.GOLD_NUGGET), new ItemStack(ModItems.COPPER_ALLOY_INGOT), 200, 0.35F),
		new SmeltRecipe(List.of(Items.GOLD_INGOT, Items.COPPER_INGOT), new ItemStack(ModItems.ROSE_GOLD_INGOT), 200, 0.5F),
		new SmeltRecipe(List.of(Items.PRISMARINE_SHARD, Items.IRON_INGOT), new ItemStack(ModItems.PRISMARINE_ALLOY_INGOT), 200, 0.5F),
		new SmeltRecipe(List.of(Items.PRISMARINE_SHARD, ModItems.CARBON_STEEL_INGOT), new ItemStack(ModItems.PRISMARINE_ALLOY_INGOT), 200, 0.5F),
		new SmeltRecipe(List.of(Items.AMETHYST_SHARD, Items.IRON_INGOT), new ItemStack(ModItems.AMETHYST_ALLOY_INGOT), 200, 0.5F),
		new SmeltRecipe(List.of(Items.AMETHYST_SHARD, ModItems.CARBON_STEEL_INGOT), new ItemStack(ModItems.AMETHYST_ALLOY_INGOT), 200, 0.5F),
		new SmeltRecipe(List.of(Items.PHANTOM_MEMBRANE, ModItems.CARBON_STEEL_INGOT, Items.DIAMOND), new ItemStack(ModItems.PHANTOM_INGOT, 2), 300, 0.8F),
		// compressed obsidian alloy: one obsidian, two carbon steel, one copper alloy, one diamond.
		// Obsidian is deliberately the small part: a full obsidian set used to cost about fifty
		// blocks of it, and obsidian is one of the slowest blocks in the game to mine. The bulk
		// of the cost moved to steel and copper, which are cheap to make and just need a smelter.
		// No bedrock shard either: the obsidian tier sits on the diamond/obsidian level.
		new SmeltRecipe(
			List.of(Items.OBSIDIAN, ModItems.CARBON_STEEL_INGOT, ModItems.COPPER_ALLOY_INGOT, Items.DIAMOND),
			List.of(1, 2, 1, 1),
			new ItemStack(ModItems.COMPRESSED_OBSIDIAN_ALLOY), 200, 1.2F),
		new SmeltRecipe(List.of(Items.ECHO_SHARD, ModItems.COPPER_ALLOY_INGOT, Items.OBSIDIAN, Items.DIAMOND, ModItems.CARBON_STEEL_INGOT), new ItemStack(ModItems.ECHO_ALLOY_INGOT), 400, 1.5F),
		// starlight: one wither star and one echo core make a batch of four ingots, so a full set
		// costs one star, and the set plus every tool costs three. One star per piece worked out
		// as ten withers, and a wither is three skulls - nobody farms that.
		new SmeltRecipe(
			List.of(Items.NETHER_STAR, ModItems.ECHO_CORE, Items.NETHERITE_INGOT, Items.AMETHYST_SHARD, Items.DIAMOND),
			List.of(1, 1, 2, 2, 2),
			new ItemStack(ModItems.STARLIGHT_ALLOY_INGOT, 4), 400, 2.5F),
		// fallback vanilla-style smelting, faster than a furnace but slower than a blast furnace
		new SmeltRecipe(List.of(Items.RAW_IRON), new ItemStack(Items.IRON_INGOT), 150, 0.7F),
		new SmeltRecipe(List.of(Items.RAW_GOLD), new ItemStack(Items.GOLD_INGOT), 150, 1.0F),
		new SmeltRecipe(List.of(Items.RAW_COPPER), new ItemStack(Items.COPPER_INGOT), 150, 0.7F),
		new SmeltRecipe(List.of(Items.SAND), new ItemStack(Items.GLASS), 150, 0.1F),
		new SmeltRecipe(List.of(Items.COBBLESTONE), new ItemStack(Items.STONE), 150, 0.1F)
	);

	private SmelterRecipes() {
	}

	/** all alloy recipes, used by the JEI plugin */
	public static List<SmeltRecipe> all() {
		return RECIPES;
	}

	/**
	 * The most specific matching recipe wins.
	 *
	 * <p>Leftover items in other slots are ignored, which is what makes a smelter forgiving - but it
	 * also means a smaller recipe can match a bigger recipe's ingredients. The compressed obsidian
	 * alloy (obsidian + steel + copper + diamond) is a subset of the echo alloy recipe, so an echo
	 * batch used to produce obsidian alloy and leave the echo shard untouched. Picking the recipe
	 * that uses the most distinct inputs fixes that without making the smelter strict.</p>
	 */
	public static SmeltRecipe find(NonNullList<ItemStack> items) {
		SmeltRecipe best = null;
		int bestScore = -1;

		for (SmeltRecipe recipe : RECIPES) {
			if (!matches(recipe, items)) {
				continue;
			}

			int score = recipe.inputs().size() * 100 + totalCount(recipe);

			if (score > bestScore) {
				bestScore = score;
				best = recipe;
			}
		}

		return best;
	}

	private static int totalCount(SmeltRecipe recipe) {
		int total = 0;

		for (int i = 0; i < recipe.inputs().size(); i++) {
			total += recipe.count(i);
		}

		return total;
	}

	/**
	 * Alloy recipes first; otherwise fall back to the game's own furnace / blast furnace /
	 * smoker recipes, so every vanilla (and modded) cooking recipe works in the smelter too.
	 */
	public static SmeltRecipe find(Level level, NonNullList<ItemStack> items) {
		SmeltRecipe alloy = find(items);

		if (alloy != null) {
			return alloy;
		}

		int nonEmpty = 0;
		ItemStack single = null;

		for (int i = 0; i < SmelterBlockEntity.INPUT_SLOTS; i++) {
			ItemStack stack = items.get(i);

			if (!stack.isEmpty()) {
				nonEmpty++;
				single = stack;
			}
		}

		if (nonEmpty != 1 || single == null) {
			return null;
		}

		if (!(level instanceof ServerLevel serverLevel)) {
			return null;
		}

		return fromVanilla(serverLevel, single);
	}

	private static SmeltRecipe fromVanilla(ServerLevel level, ItemStack stack) {
		SingleRecipeInput input = new SingleRecipeInput(stack);

		Optional<RecipeHolder<SmeltingRecipe>> smelting =
			level.recipeAccess().getRecipeFor(RecipeType.SMELTING, input, level);

		if (smelting.isPresent()) {
			return toRecipe(smelting.get().value(), input, stack);
		}

		Optional<RecipeHolder<BlastingRecipe>> blasting =
			level.recipeAccess().getRecipeFor(RecipeType.BLASTING, input, level);

		if (blasting.isPresent()) {
			return toRecipe(blasting.get().value(), input, stack);
		}

		Optional<RecipeHolder<SmokingRecipe>> smoking =
			level.recipeAccess().getRecipeFor(RecipeType.SMOKING, input, level);

		if (smoking.isPresent()) {
			return toRecipe(smoking.get().value(), input, stack);
		}

		return null;
	}

	@SuppressWarnings("rawtypes")
	private static SmeltRecipe toRecipe(Recipe recipe, SingleRecipeInput input, ItemStack stack) {
		ItemStack result = recipe.assemble(input);

		if (result.isEmpty()) {
			return null;
		}

		float experience = recipe instanceof AbstractCookingRecipe cooking ? cooking.experience() : 0.3F;
		return new SmeltRecipe(List.of(stack.getItem()), result.copy(), 150, experience);
	}

	/**
	 * One slot may supply a whole stack, so a requirement of 4 obsidian is satisfied by a
	 * single slot holding four obsidian. Leftover items in other slots are ignored.
	 */
	private static boolean matches(SmeltRecipe recipe, NonNullList<ItemStack> items) {
		int[] remaining = remaining(recipe);

		for (int i = 0; i < SmelterBlockEntity.INPUT_SLOTS; i++) {
			ItemStack stack = items.get(i);

			if (stack.isEmpty()) {
				continue;
			}

			for (int j = 0; j < remaining.length; j++) {
				if (remaining[j] > 0 && stack.is(recipe.inputs().get(j)) && stack.getCount() >= remaining[j]) {
					remaining[j] = 0;
					break;
				}
			}
		}

		for (int value : remaining) {
			if (value > 0) {
				return false;
			}
		}

		return true;
	}

	/** consume the required amount from the matching slots */
	public static List<Item> consumed(SmeltRecipe recipe, NonNullList<ItemStack> items) {
		int[] remaining = remaining(recipe);
		List<Item> taken = new ArrayList<>();

		for (int i = 0; i < SmelterBlockEntity.INPUT_SLOTS; i++) {
			ItemStack stack = items.get(i);

			if (stack.isEmpty()) {
				continue;
			}

			for (int j = 0; j < remaining.length; j++) {
				if (remaining[j] > 0 && stack.is(recipe.inputs().get(j)) && stack.getCount() >= remaining[j]) {
					taken.add(recipe.inputs().get(j));
					remaining[j] = 0;
					break;
				}
			}
		}

		return taken;
	}

	private static int[] remaining(SmeltRecipe recipe) {
		int[] remaining = new int[recipe.inputs().size()];

		for (int i = 0; i < remaining.length; i++) {
			remaining[i] = recipe.count(i);
		}

		return remaining;
	}

	public static int burnTime(ItemStack stack) {
		if (stack.isEmpty()) {
			return 0;
		}

		if (stack.is(Items.COAL) || stack.is(Items.CHARCOAL)) {
			return 1600;
		}

		if (stack.is(Items.COAL_BLOCK)) {
			return 16000;
		}

		if (stack.is(Items.BLAZE_ROD)) {
			return 2400;
		}

		if (stack.is(Items.LAVA_BUCKET)) {
			return 20000;
		}

		if (stack.is(Items.STICK)) {
			return 100;
		}

		if (stack.is(Items.DRIED_KELP_BLOCK)) {
			return 4001;
		}

		if (stack.is(Items.OAK_PLANKS) || stack.is(Items.SPRUCE_PLANKS) || stack.is(Items.BIRCH_PLANKS)
			|| stack.is(Items.JUNGLE_PLANKS) || stack.is(Items.ACACIA_PLANKS) || stack.is(Items.DARK_OAK_PLANKS)
			|| stack.is(Items.MANGROVE_PLANKS) || stack.is(Items.CHERRY_PLANKS) || stack.is(Items.BAMBOO_PLANKS)
			|| stack.is(Items.CRIMSON_PLANKS) || stack.is(Items.WARPED_PLANKS)) {
			return 300;
		}

		if (stack.is(Items.OAK_LOG) || stack.is(Items.SPRUCE_LOG) || stack.is(Items.BIRCH_LOG)
			|| stack.is(Items.JUNGLE_LOG) || stack.is(Items.ACACIA_LOG) || stack.is(Items.DARK_OAK_LOG)
			|| stack.is(Items.MANGROVE_LOG) || stack.is(Items.CHERRY_LOG)) {
			return 300;
		}

		return 0;
	}
}

