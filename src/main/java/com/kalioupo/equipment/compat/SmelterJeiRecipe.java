package com.kalioupo.equipment.compat;

import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * JEI only needs to know what goes in and what comes out. The smelter accepts its inputs in
 * any slot order, which is why this is a flat list instead of a shaped grid.
 */
public record SmelterJeiRecipe(List<List<ItemStack>> inputs, ItemStack result, int time, float experience) {
}
