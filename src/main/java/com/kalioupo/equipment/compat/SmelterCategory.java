package com.kalioupo.equipment.compat;

import com.kalioupo.equipment.KalioupoEquipment;
import com.kalioupo.equipment.registry.ModBlocks;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** JEI category mirroring the smelter GUI: five unordered input slots, arrow, one output. */
public class SmelterCategory implements IRecipeCategory<SmelterJeiRecipe> {
	public static final IRecipeType<SmelterJeiRecipe> TYPE =
		IRecipeType.create(KalioupoEquipment.id("smelting"), SmelterJeiRecipe.class);

	private static final int[][] INPUT_SLOTS = {
		{2, 2}, {24, 2}, {46, 2}, {2, 24}, {24, 24}
	};
	private static final int OUTPUT_X = 100;
	private static final int OUTPUT_Y = 12;

	private final IDrawable icon;
	private final Component title;

	public SmelterCategory(IGuiHelper helper) {
		this.icon = helper.createDrawableItemStack(new ItemStack(ModBlocks.SMELTER_ITEM));
		this.title = Component.translatable("jei.kalioupo_equipment.smelting");
	}

	@Override
	public IRecipeType<SmelterJeiRecipe> getRecipeType() {
		return TYPE;
	}

	@Override
	public Component getTitle() {
		return this.title;
	}

	@Override
	public int getWidth() {
		return 124;
	}

	@Override
	public int getHeight() {
		return 48;
	}

	@Override
	public IDrawable getIcon() {
		return this.icon;
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, SmelterJeiRecipe recipe, IFocusGroup focuses) {
		for (int i = 0; i < recipe.inputs().size() && i < INPUT_SLOTS.length; i++) {
			builder.addInputSlot(INPUT_SLOTS[i][0], INPUT_SLOTS[i][1])
				.addItemStacks(recipe.inputs().get(i));
		}

		builder.setShapeless();
		builder.addOutputSlot(OUTPUT_X, OUTPUT_Y).addItemStack(recipe.result());
	}
}
