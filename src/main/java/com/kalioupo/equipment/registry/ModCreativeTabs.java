package com.kalioupo.equipment.registry;

import com.kalioupo.equipment.KalioupoEquipment;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Creative tab order.
 *
 * The tab is always laid out the same way:
 * <ol>
 *   <li>utility blocks / tools (backpack, smelter)</li>
 *   <li>materials, weakest tier first, and anything on the same upgrade chain stays adjacent
 *       (base ingot -&gt; upgrade shards -&gt; upgraded alloys)</li>
 *   <li>equipment, weakest tier first; inside one tier the pieces are grouped as
 *       helmet / chestplate / leggings / boots / sword / pickaxe / axe / shovel / hoe / spear / shield</li>
 * </ol>
 * When new items are added they must be inserted at their power position, never appended at
 * the end.
 */
public final class ModCreativeTabs {
	public static final ResourceKey<CreativeModeTab> EQUIPMENT =
		ResourceKey.create(Registries.CREATIVE_MODE_TAB, KalioupoEquipment.id("equipment"));

	/** utility items - always first */
	private static final List<Item> UTILITY = List.of(
		ModItems.BACKPACK
	);

	/**
	 * materials from weakest to strongest. The upgrade chain
	 * bedrock shard -&gt; heated bedrock shard -&gt; echo alloy -&gt; starlight alloy stays contiguous.
	 */
	private static final List<Item> MATERIALS = List.of(
		ModItems.CARBON_STEEL_INGOT,      // 1 entry tier
		ModItems.QUARTZ_STEEL_INGOT,      // 2
		ModItems.COPPER_ALLOY_INGOT,      // 2
		ModItems.ROSE_GOLD_INGOT,         // 3
		ModItems.PRISMARINE_ALLOY_INGOT,  // 4
		ModItems.AMETHYST_ALLOY_INGOT,    // 5
		ModItems.PHANTOM_INGOT,           // 6
		ModItems.BEDROCK_SHARD,           // 7  upgrade chain
		ModItems.COMPRESSED_OBSIDIAN_ALLOY,
		ModItems.ECHO_ALLOY_INGOT,        // 8  upgrade chain
		ModItems.ECHO_CORE,               //    warden drop, feeds the echo upgrade
		ModItems.STARLIGHT_ALLOY_INGOT    // 9  upgrade chain
	);

	/** equipment from weakest to strongest; inside one tier helmet / chest / legs / boots / tools */
	private static final List<Item> EQUIPMENT_ITEMS = List.of(
		// carbon steel - the entry tier
		ModItems.CARBON_STEEL_HELMET,
		ModItems.CARBON_STEEL_CHESTPLATE,
		ModItems.CARBON_STEEL_LEGGINGS,
		ModItems.CARBON_STEEL_BOOTS,
		ModItems.CARBON_STEEL_SWORD,
		ModItems.CARBON_STEEL_PICKAXE,
		ModItems.CARBON_STEEL_AXE,
		ModItems.CARBON_STEEL_SHOVEL,
		ModItems.CARBON_STEEL_HOE,
		ModItems.CARBON_STEEL_SPEAR,
		// quartz steel
		ModItems.QUARTZ_STEEL_SWORD,
		// copper alloy
		ModItems.COPPER_ALLOY_HELMET,
		ModItems.COPPER_ALLOY_CHESTPLATE,
		ModItems.COPPER_ALLOY_LEGGINGS,
		ModItems.COPPER_ALLOY_BOOTS,
		// rose gold
		ModItems.ROSE_GOLD_CHESTPLATE,
		ModItems.ROSE_GOLD_SWORD,
		// prismarine
		ModItems.PRISMARINE_HELMET,
		ModItems.PRISMARINE_BOOTS,
		// amethyst
		ModItems.AMETHYST_SWORD,
		// phantom
		ModItems.PHANTOM_HELMET,
		ModItems.PHANTOM_CHESTPLATE,
		ModItems.PHANTOM_LEGGINGS,
		ModItems.PHANTOM_BOOTS,
		// obsidian
		ModItems.OBSIDIAN_HELMET,
		ModItems.OBSIDIAN_CHESTPLATE,
		ModItems.OBSIDIAN_LEGGINGS,
		ModItems.OBSIDIAN_BOOTS,
		ModItems.OBSIDIAN_SWORD,
		ModItems.OBSIDIAN_PICKAXE,
		ModItems.OBSIDIAN_AXE,
		ModItems.OBSIDIAN_SHOVEL,
		ModItems.OBSIDIAN_HOE,
		ModItems.OBSIDIAN_SPEAR,
		// bedrock
		ModItems.BEDROCK_HELMET,
		ModItems.BEDROCK_CHESTPLATE,
		ModItems.BEDROCK_LEGGINGS,
		ModItems.BEDROCK_BOOTS,
		ModItems.BEDROCK_SWORD,
		ModItems.BEDROCK_PICKAXE,
		ModItems.BEDROCK_AXE,
		ModItems.BEDROCK_SHOVEL,
		ModItems.BEDROCK_HOE,
		ModItems.BEDROCK_SPEAR,
		// echo
		ModItems.ECHO_HELMET,
		ModItems.ECHO_CHESTPLATE,
		ModItems.ECHO_LEGGINGS,
		ModItems.ECHO_BOOTS,
		ModItems.ECHO_SWORD,
		ModItems.ECHO_PICKAXE,
		ModItems.ECHO_AXE,
		ModItems.ECHO_SHOVEL,
		ModItems.ECHO_HOE,
		ModItems.ECHO_SPEAR,
		// starlight
		ModItems.STARLIGHT_HELMET,
		ModItems.STARLIGHT_CHESTPLATE,
		ModItems.STARLIGHT_LEGGINGS,
		ModItems.STARLIGHT_BOOTS,
		ModItems.STARLIGHT_SWORD,
		ModItems.STARLIGHT_PICKAXE,
		ModItems.STARLIGHT_AXE,
		ModItems.STARLIGHT_SHOVEL,
		ModItems.STARLIGHT_HOE,
		ModItems.STARLIGHT_SPEAR,
		// shields
		ModItems.CARBON_STEEL_SHIELD,
		ModItems.OBSIDIAN_SHIELD,
		ModItems.BEDROCK_SHIELD,
		ModItems.CARBON_STEEL_SHIELD_GLOW,
		ModItems.OBSIDIAN_SHIELD_GLOW,
		ModItems.BEDROCK_SHIELD_GLOW
	);
	private ModCreativeTabs() {
	}

	public static void init() {
		CreativeModeTab tab = FabricCreativeModeTab.builder()
			.title(Component.translatable("itemGroup.kalioupo_equipment.equipment"))
			.icon(() -> new ItemStack(ModItems.BACKPACK))
			.displayItems((parameters, output) -> {
				for (Item item : UTILITY) {
					output.accept(item);
				}
				output.accept(ModBlocks.SMELTER_ITEM);
				output.accept(ModBlocks.SOCKET_TABLE_ITEM);
				for (Item item : MATERIALS) {
					output.accept(item);
				}
				for (Item item : EQUIPMENT_ITEMS) {
					output.accept(item);
				}
			})
			.build();

		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, EQUIPMENT, tab);
	}
}
