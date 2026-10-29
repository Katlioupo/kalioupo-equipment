package com.kalioupo.equipment.compat;

import com.kalioupo.equipment.KalioupoEquipment;
import com.kalioupo.equipment.block.SmelterRecipes;
import com.kalioupo.equipment.registry.ModBlocks;
import com.kalioupo.equipment.registry.ModItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Registered from fabric.mod.json under "jei_mod_plugin", so it is only loaded when JEI exists. */
public class SmelterJeiPlugin implements IModPlugin {
	private static final Identifier UID = KalioupoEquipment.id("jei/smelter");

	@Override
	public Identifier getPluginUid() {
		return UID;
	}

	@Override
	public void registerCategories(IRecipeCategoryRegistration registration) {
		registration.addRecipeCategories(new SmelterCategory(registration.getJeiHelpers().getGuiHelper()));
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		registration.addRecipes(SmelterCategory.TYPE, buildRecipes());

		// "use" page for the two drops nobody can guess the source of
		registration.addItemStackInfo(new ItemStack(ModItems.BEDROCK_SHARD),
			Component.translatable("jei.kalioupo_equipment.bedrock_shard.1"),
			Component.translatable("jei.kalioupo_equipment.bedrock_shard.2"));
		registration.addItemStackInfo(new ItemStack(ModItems.COMPRESSED_OBSIDIAN_ALLOY),
			Component.translatable("jei.kalioupo_equipment.compressed_obsidian.1"),
			Component.translatable("jei.kalioupo_equipment.compressed_obsidian.2"));
		registration.addItemStackInfo(new ItemStack(ModItems.ECHO_CORE),
			Component.translatable("jei.kalioupo_equipment.echo_core.1"),
			Component.translatable("jei.kalioupo_equipment.echo_core.2"));
		registration.addItemStackInfo(new ItemStack(ModBlocks.SOCKET_TABLE_ITEM),
			Component.translatable("jei.kalioupo_equipment.socket_table.1"),
			Component.translatable("jei.kalioupo_equipment.socket_table.2"),
			Component.translatable("jei.kalioupo_equipment.socket_table.3"));
	}

	@Override
	public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
		registration.addRecipeCatalyst(new ItemStack(ModBlocks.SMELTER_ITEM), SmelterCategory.TYPE);
	}

	private static List<SmelterJeiRecipe> buildRecipes() {
		List<SmelterJeiRecipe> recipes = new ArrayList<>();

		for (SmelterRecipes.SmeltRecipe recipe : SmelterRecipes.all()) {
			List<List<ItemStack>> inputs = new ArrayList<>();

			for (int i = 0; i < recipe.inputs().size(); i++) {
				inputs.add(List.of(new ItemStack(recipe.inputs().get(i), recipe.count(i))));
			}

			recipes.add(new SmelterJeiRecipe(inputs, recipe.result().copy(), recipe.time(), recipe.experience()));
		}

		return recipes;
	}
}
