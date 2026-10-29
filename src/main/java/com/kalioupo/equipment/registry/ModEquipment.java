package com.kalioupo.equipment.registry;

import com.kalioupo.equipment.KalioupoEquipment;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

import java.util.Map;

/**
 * Tier definitions. A tool tier is only a {@link ToolMaterial} record (no registry), an armour
 * tier is an {@link ArmorMaterial} record plus a data driven equipment asset json.
 *
 * <p>Armour durability is {@code base * (11 / 16 / 15 / 13)} for helmet / chest / legs / boots.</p>
 */
public final class ModEquipment {
	// ---------------------------------------------------------------- repair tags
	public static final TagKey<Item> CARBON_STEEL_INGOTS = tag("carbon_steel_ingots");
	public static final TagKey<Item> QUARTZ_STEEL_INGOTS = tag("quartz_steel_ingots");
	public static final TagKey<Item> COPPER_ALLOY_INGOTS = tag("copper_alloy_ingots");
	public static final TagKey<Item> ROSE_GOLD_INGOTS = tag("rose_gold_ingots");
	public static final TagKey<Item> PRISMARINE_INGOTS = tag("prismarine_ingots");
	public static final TagKey<Item> AMETHYST_INGOTS = tag("amethyst_ingots");
	public static final TagKey<Item> PHANTOM_INGOTS = tag("phantom_ingots");
	public static final TagKey<Item> OBSIDIAN_ALLOY_INGOTS = tag("obsidian_ingots");
	// the generator writes the shard tag as "<material>_ingots" for every tier
	public static final TagKey<Item> BEDROCK_SHARDS = tag("bedrock_ingots");
	public static final TagKey<Item> ECHO_INGOTS = tag("echo_ingots");
	public static final TagKey<Item> STARLIGHT_INGOTS = tag("starlight_ingots");

	// ---------------------------------------------------------------- equipment assets
	public static final ResourceKey<EquipmentAsset> CARBON_STEEL_ASSET = asset("carbon_steel");
	public static final ResourceKey<EquipmentAsset> COPPER_ALLOY_ASSET = asset("copper_alloy");
	public static final ResourceKey<EquipmentAsset> ROSE_GOLD_ASSET = asset("rose_gold");
	public static final ResourceKey<EquipmentAsset> PRISMARINE_ASSET = asset("prismarine");
	public static final ResourceKey<EquipmentAsset> PHANTOM_ASSET = asset("phantom");
	public static final ResourceKey<EquipmentAsset> OBSIDIAN_ASSET = asset("obsidian");
	public static final ResourceKey<EquipmentAsset> BEDROCK_ASSET = asset("bedrock");
	public static final ResourceKey<EquipmentAsset> ECHO_ASSET = asset("echo");
	public static final ResourceKey<EquipmentAsset> STARLIGHT_ASSET = asset("starlight");

	// ---------------------------------------------------------------- tool tiers
	/** iron mining level, 400 durability, +2 damage, enchantability 14 */
	public static final ToolMaterial CARBON_STEEL = new ToolMaterial(
		BlockTags.INCORRECT_FOR_IRON_TOOL, 400, 6.0F, 2.0F, 14, CARBON_STEEL_INGOTS);

	/** 300 durability, enchantability 18 - fragile but hits hard and slow */
	public static final ToolMaterial QUARTZ_STEEL = new ToolMaterial(
		BlockTags.INCORRECT_FOR_IRON_TOOL, 300, 6.0F, 2.0F, 18, QUARTZ_STEEL_INGOTS);

	/** 450 durability, enchantability 30 - the enchanter's blade */
	public static final ToolMaterial ROSE_GOLD = new ToolMaterial(
		BlockTags.INCORRECT_FOR_IRON_TOOL, 450, 6.0F, 2.0F, 30, ROSE_GOLD_INGOTS);

	/** 800 durability, enchantability 15 */
	public static final ToolMaterial AMETHYST = new ToolMaterial(
		BlockTags.INCORRECT_FOR_IRON_TOOL, 800, 6.0F, 2.0F, 15, AMETHYST_INGOTS);

	// ---------------------------------------------------------------- armour tiers
	/** base 25 -> 275 / 400 / 375 / 325 durability, defence 2/6/5/2, toughness 1 */
	public static final ArmorMaterial CARBON_STEEL_ARMOR = new ArmorMaterial(
		25,
		Map.of(
			ArmorType.HELMET, 2,
			ArmorType.CHESTPLATE, 6,
			ArmorType.LEGGINGS, 5,
			ArmorType.BOOTS, 2
		),
		14,
		SoundEvents.ARMOR_EQUIP_IRON,
		1.0F,
		0.0F,
		CARBON_STEEL_INGOTS,
		CARBON_STEEL_ASSET
	);

	/** base 31 -> 341 / 496 / 465 / 403 durability, defence 3/7/6/3, toughness 2, knockback 0.1 */
	public static final ArmorMaterial COPPER_ALLOY_ARMOR = new ArmorMaterial(
		31,
		Map.of(
			ArmorType.HELMET, 3,
			ArmorType.CHESTPLATE, 7,
			ArmorType.LEGGINGS, 6,
			ArmorType.BOOTS, 3
		),
		10,
		SoundEvents.ARMOR_EQUIP_IRON,
		2.0F,
		0.1F,
		COPPER_ALLOY_INGOTS,
		COPPER_ALLOY_ASSET
	);

	/** only the chestplate exists: base 28 -> 448 durability, defence 7, toughness 1, enchantability 25 */
	public static final ArmorMaterial ROSE_GOLD_ARMOR = new ArmorMaterial(
		28,
		Map.of(ArmorType.CHESTPLATE, 7),
		25,
		SoundEvents.ARMOR_EQUIP_IRON,
		1.0F,
		0.0F,
		ROSE_GOLD_INGOTS,
		ROSE_GOLD_ASSET
	);

	/** only helmet and boots: base 28 -> 308 / 364 durability, defence 3 / 2, toughness 1 */
	public static final ArmorMaterial PRISMARINE_ARMOR = new ArmorMaterial(
		28,
		Map.of(
			ArmorType.HELMET, 3,
			ArmorType.BOOTS, 2
		),
		12,
		SoundEvents.ARMOR_EQUIP_IRON,
		1.0F,
		0.0F,
		PRISMARINE_INGOTS,
		PRISMARINE_ASSET
	);

	/** helmet / chest / legs / boots: base 35 -> 385 / 560 / 525 / 455, defence 3/8/6/3, toughness 2.5 */
	public static final ArmorMaterial PHANTOM_ARMOR = new ArmorMaterial(
		35,
		Map.of(
			ArmorType.HELMET, 3,
			ArmorType.CHESTPLATE, 8,
			ArmorType.LEGGINGS, 6,
			ArmorType.BOOTS, 3
		),
		20,
		SoundEvents.ARMOR_EQUIP_IRON,
		2.5F,
		0.0F,
		PHANTOM_INGOTS,
		PHANTOM_ASSET
	);

	/** netherite mining level, 3655 durability, +3 damage, enchantability 20, slightly faster than netherite */
	public static final ToolMaterial OBSIDIAN = new ToolMaterial(
		BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 2100, 10.0F, 3.0F, 20, OBSIDIAN_ALLOY_INGOTS);

	/** base 228 -> 2508 / 3648 / 3420 / 2964, defence 5/14/11/5, toughness 3 */
	public static final ArmorMaterial OBSIDIAN_ARMOR = new ArmorMaterial(
	80,
		Map.of(
			ArmorType.HELMET, 4,
			ArmorType.CHESTPLATE, 9,
			ArmorType.LEGGINGS, 7,
			ArmorType.BOOTS, 3
		),
		20,
		SoundEvents.ARMOR_EQUIP_IRON,
		4.0F,
		0.15F,
		OBSIDIAN_ALLOY_INGOTS,
		OBSIDIAN_ASSET
	);

	/** netherite mining level, 2000 durability, +4 damage, enchantability 22 */
	public static final ToolMaterial BEDROCK = new ToolMaterial(
		BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 2600, 11.0F, 4.0F, 22, BEDROCK_SHARDS);

	/** base 125 -> 1375 / 2000 / 1875 / 1625, defence 6/13/10/5, toughness 4, knockback 0.4 */
	public static final ArmorMaterial BEDROCK_ARMOR = new ArmorMaterial(
		125,
		Map.of(
			ArmorType.HELMET, 6,
			ArmorType.CHESTPLATE, 13,
			ArmorType.LEGGINGS, 10,
			ArmorType.BOOTS, 5
		),
		22,
		SoundEvents.ARMOR_EQUIP_IRON,
		4.0F,
		0.4F,
		BEDROCK_SHARDS,
		BEDROCK_ASSET
	);

	/**
	 * netherite mining level, 3000 durability, +4 damage, enchantability 24.
	 *
	 * <p>Damage is deliberately held at the bedrock level: the echo tier is meant to be earned
	 * by the warden mechanics, not by one shotting ordinary mobs. Everything else (durability,
	 * enchantability, the stagger rolls) is where the upgrade shows.</p>
	 */
	public static final ToolMaterial ECHO = new ToolMaterial(
		BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 3000, 12.0F, 4.0F, 24, ECHO_INGOTS);

	/**
	 * base 160 -> 1760 / 2560 / 2400 / 2080 durability, defence 7/15/12/6 (total 40),
	 * toughness 5, knockback 0.5, enchantability 24. The ancient city graduation set.
	 */
	public static final ArmorMaterial ECHO_ARMOR = new ArmorMaterial(
		160,
		Map.of(
			ArmorType.HELMET, 7,
			ArmorType.CHESTPLATE, 15,
			ArmorType.LEGGINGS, 12,
			ArmorType.BOOTS, 6
		),
		24,
		SoundEvents.ARMOR_EQUIP_IRON,
		5.0F,
		0.5F,
		ECHO_INGOTS,
		ECHO_ASSET
	);

	/** netherite mining level, 4000 durability, +5 damage, enchantability 26, final tier */
	public static final ToolMaterial STARLIGHT = new ToolMaterial(
		BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 4000, 14.0F, 5.0F, 26, STARLIGHT_INGOTS);

	/**
	 * base 300 -> 3300 / 4800 / 4500 / 3900 durability, defence 8/17/13/7 (total 45),
	 * toughness 6, knockback 0.6, enchantability 26. The endgame set.
	 */
	public static final ArmorMaterial STARLIGHT_ARMOR = new ArmorMaterial(
		300,
		Map.of(
			ArmorType.HELMET, 8,
			ArmorType.CHESTPLATE, 17,
			ArmorType.LEGGINGS, 13,
			ArmorType.BOOTS, 7
		),
		26,
		SoundEvents.ARMOR_EQUIP_IRON,
		6.0F,
		0.6F,
		STARLIGHT_INGOTS,
		STARLIGHT_ASSET
	);

	private ModEquipment() {
	}

	public static void init() {
	}

	private static TagKey<Item> tag(String name) {
		return TagKey.create(Registries.ITEM, KalioupoEquipment.id(name));
	}

	private static ResourceKey<EquipmentAsset> asset(String name) {
		return ResourceKey.create(EquipmentAssets.ROOT_ID, KalioupoEquipment.id(name));
	}
}
