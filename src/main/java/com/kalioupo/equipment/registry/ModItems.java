package com.kalioupo.equipment.registry;

import com.kalioupo.equipment.KalioupoEquipment;
import com.kalioupo.equipment.item.AmethystSwordItem;
import com.kalioupo.equipment.item.BackpackItem;
import com.kalioupo.equipment.item.EffectArmorItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Unit;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.equipment.ArmorType;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public final class ModItems {
	public static final Item BACKPACK = register("backpack", BackpackItem::new, p -> p.stacksTo(1), Rarity.COMMON);

	// ---------------------------------------------------------------- materials
	// common tier
	public static final Item CARBON_STEEL_INGOT = register("carbon_steel_ingot", Item::new, p -> p.stacksTo(64), Rarity.COMMON);
	public static final Item COPPER_ALLOY_INGOT = register("copper_alloy_ingot", Item::new, p -> p.stacksTo(64), Rarity.COMMON);

	// uncommon tier
	public static final Item QUARTZ_STEEL_INGOT = register("quartz_steel_ingot", Item::new, p -> p.stacksTo(64), Rarity.UNCOMMON);
	public static final Item ROSE_GOLD_INGOT = register("rose_gold_ingot", Item::new, p -> p.stacksTo(64), Rarity.UNCOMMON);
	public static final Item PRISMARINE_ALLOY_INGOT = register("prismarine_alloy_ingot", Item::new, p -> p.stacksTo(64), Rarity.UNCOMMON);

	// rare tier
	public static final Item AMETHYST_ALLOY_INGOT = register("amethyst_alloy_ingot", Item::new, p -> p.stacksTo(64), Rarity.RARE);
	public static final Item PHANTOM_INGOT = register("phantom_ingot", Item::new, p -> p.stacksTo(64), Rarity.RARE);
	public static final Item ECHO_ALLOY_INGOT = register("echo_alloy_ingot", Item::new, p -> p.stacksTo(64), Rarity.RARE);
	public static final Item BEDROCK_SHARD = register("bedrock_shard", Item::new, p -> p.stacksTo(64), Rarity.RARE);
	public static final Item COMPRESSED_OBSIDIAN_ALLOY = register("compressed_obsidian_alloy", Item::new, p -> p.stacksTo(64), Rarity.RARE);

	// warden drop - the reason to walk back into the ancient city
	public static final Item ECHO_CORE = register("echo_core", Item::new, p -> p.stacksTo(64), Rarity.EPIC);

	// epic tier - the endgame material shimmers like an enchanted item
	public static final Item STARLIGHT_ALLOY_INGOT = register("starlight_alloy_ingot", Item::new,
		p -> p.stacksTo(64).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true), Rarity.EPIC);

	// ---------------------------------------------------------------- carbon steel set
	public static final Item CARBON_STEEL_HELMET = register("carbon_steel_helmet", Item::new,
		p -> p.humanoidArmor(ModEquipment.CARBON_STEEL_ARMOR, ArmorType.HELMET), Rarity.COMMON);
	public static final Item CARBON_STEEL_CHESTPLATE = register("carbon_steel_chestplate", Item::new,
		p -> p.humanoidArmor(ModEquipment.CARBON_STEEL_ARMOR, ArmorType.CHESTPLATE), Rarity.COMMON);
	public static final Item CARBON_STEEL_LEGGINGS = register("carbon_steel_leggings", Item::new,
		p -> p.humanoidArmor(ModEquipment.CARBON_STEEL_ARMOR, ArmorType.LEGGINGS), Rarity.COMMON);
	public static final Item CARBON_STEEL_BOOTS = register("carbon_steel_boots", Item::new,
		p -> p.humanoidArmor(ModEquipment.CARBON_STEEL_ARMOR, ArmorType.BOOTS), Rarity.COMMON);

	public static final Item CARBON_STEEL_SWORD = register("carbon_steel_sword", Item::new,
		p -> p.sword(ModEquipment.CARBON_STEEL, 3.5F, -2.4F), Rarity.COMMON);
	public static final Item CARBON_STEEL_PICKAXE = register("carbon_steel_pickaxe", Item::new,
		p -> p.pickaxe(ModEquipment.CARBON_STEEL, 1.0F, -2.8F), Rarity.COMMON);
	public static final Item CARBON_STEEL_AXE = register("carbon_steel_axe", Item::new,
		p -> p.axe(ModEquipment.CARBON_STEEL, 6.0F, -3.1F), Rarity.COMMON);
	public static final Item CARBON_STEEL_SHOVEL = register("carbon_steel_shovel", Item::new,
		p -> p.shovel(ModEquipment.CARBON_STEEL, 1.5F, -3.0F), Rarity.COMMON);
	public static final Item CARBON_STEEL_HOE = register("carbon_steel_hoe", Item::new,
		p -> p.hoe(ModEquipment.CARBON_STEEL, -2.0F, 0.0F), Rarity.COMMON);
	public static final Item CARBON_STEEL_SPEAR = register("carbon_steel_spear", Item::new,
		p -> p.spear(ModEquipment.CARBON_STEEL, 0.95F, 0.95F, 0.6F, 2.5F, 11.0F, 6.75F, 5.1F, 11.25F, 4.6F), Rarity.COMMON);

	// ---------------------------------------------------------------- quartz steel (sword only)
	public static final Item QUARTZ_STEEL_SWORD = register("quartz_steel_sword", Item::new,
		p -> p.sword(ModEquipment.QUARTZ_STEEL, 6.0F, -2.6F), Rarity.UNCOMMON);

	// ---------------------------------------------------------------- copper alloy set
	public static final Item COPPER_ALLOY_HELMET = register("copper_alloy_helmet", Item::new,
		p -> p.humanoidArmor(ModEquipment.COPPER_ALLOY_ARMOR, ArmorType.HELMET), Rarity.COMMON);
	public static final Item COPPER_ALLOY_CHESTPLATE = register("copper_alloy_chestplate", Item::new,
		p -> p.humanoidArmor(ModEquipment.COPPER_ALLOY_ARMOR, ArmorType.CHESTPLATE), Rarity.COMMON);
	public static final Item COPPER_ALLOY_LEGGINGS = register("copper_alloy_leggings", Item::new,
		p -> p.humanoidArmor(ModEquipment.COPPER_ALLOY_ARMOR, ArmorType.LEGGINGS), Rarity.COMMON);
	public static final Item COPPER_ALLOY_BOOTS = register("copper_alloy_boots", Item::new,
		p -> p.humanoidArmor(ModEquipment.COPPER_ALLOY_ARMOR, ArmorType.BOOTS), Rarity.COMMON);

	// ---------------------------------------------------------------- rose gold (sword + chestplate)
	public static final Item ROSE_GOLD_SWORD = register("rose_gold_sword", Item::new,
		p -> p.sword(ModEquipment.ROSE_GOLD, 4.5F, -2.4F), Rarity.UNCOMMON);
	public static final Item ROSE_GOLD_CHESTPLATE = register("rose_gold_chestplate", Item::new,
		p -> p.humanoidArmor(ModEquipment.ROSE_GOLD_ARMOR, ArmorType.CHESTPLATE), Rarity.UNCOMMON);

	// ---------------------------------------------------------------- prismarine (helmet + boots)
	public static final Item PRISMARINE_HELMET = register("prismarine_helmet",
		p -> new EffectArmorItem(p, EquipmentSlot.HEAD, MobEffects.WATER_BREATHING),
		p -> p.humanoidArmor(ModEquipment.PRISMARINE_ARMOR, ArmorType.HELMET), Rarity.UNCOMMON);
	public static final Item PRISMARINE_BOOTS = register("prismarine_boots",
		p -> new EffectArmorItem(p, EquipmentSlot.FEET, MobEffects.DOLPHINS_GRACE),
		p -> p.humanoidArmor(ModEquipment.PRISMARINE_ARMOR, ArmorType.BOOTS), Rarity.UNCOMMON);

	// ---------------------------------------------------------------- amethyst (sword only)
	public static final Item AMETHYST_SWORD = register("amethyst_sword", AmethystSwordItem::new,
		p -> p.component(ModComponents.AMETHYST_CHARGE, 0).sword(ModEquipment.AMETHYST, 5.5F, -2.4F), Rarity.RARE);

	// ---------------------------------------------------------------- phantom gliding set
	public static final Item PHANTOM_HELMET = register("phantom_helmet",
		p -> new EffectArmorItem(p, EquipmentSlot.HEAD, MobEffects.NIGHT_VISION, MobEffects.RESISTANCE),
		p -> p.humanoidArmor(ModEquipment.PHANTOM_ARMOR, ArmorType.HELMET), Rarity.RARE);
	public static final Item PHANTOM_CHESTPLATE = register("phantom_chestplate", Item::new,
		p -> p.humanoidArmor(ModEquipment.PHANTOM_ARMOR, ArmorType.CHESTPLATE)
			.component(DataComponents.GLIDER, Unit.INSTANCE), Rarity.RARE);
	public static final Item PHANTOM_LEGGINGS = register("phantom_leggings", Item::new,
		p -> p.humanoidArmor(ModEquipment.PHANTOM_ARMOR, ArmorType.LEGGINGS), Rarity.RARE);
	public static final Item PHANTOM_BOOTS = register("phantom_boots", Item::new,
		p -> p.component(ModComponents.PHANTOM_CHARGES, 0)
			.humanoidArmor(ModEquipment.PHANTOM_ARMOR, ArmorType.BOOTS), Rarity.RARE);

	// ---------------------------------------------------------------- obsidian set
	public static final Item OBSIDIAN_HELMET = register("obsidian_helmet", Item::new,
		p -> p.humanoidArmor(ModEquipment.OBSIDIAN_ARMOR, ArmorType.HELMET), Rarity.EPIC);
	public static final Item OBSIDIAN_CHESTPLATE = register("obsidian_chestplate", Item::new,
		p -> p.humanoidArmor(ModEquipment.OBSIDIAN_ARMOR, ArmorType.CHESTPLATE), Rarity.EPIC);
	public static final Item OBSIDIAN_LEGGINGS = register("obsidian_leggings", Item::new,
		p -> p.humanoidArmor(ModEquipment.OBSIDIAN_ARMOR, ArmorType.LEGGINGS), Rarity.EPIC);
	public static final Item OBSIDIAN_BOOTS = register("obsidian_boots", Item::new,
		p -> p.humanoidArmor(ModEquipment.OBSIDIAN_ARMOR, ArmorType.BOOTS), Rarity.EPIC);
	public static final Item OBSIDIAN_SWORD = register("obsidian_sword", Item::new,
		p -> p.sword(ModEquipment.OBSIDIAN, 6.5F, -2.4F), Rarity.EPIC);
	public static final Item OBSIDIAN_PICKAXE = register("obsidian_pickaxe", Item::new,
		p -> p.pickaxe(ModEquipment.OBSIDIAN, 2.0F, -2.8F), Rarity.EPIC);
	public static final Item OBSIDIAN_AXE = register("obsidian_axe", Item::new,
		p -> p.axe(ModEquipment.OBSIDIAN, 6.5F, -3.1F), Rarity.EPIC);
	public static final Item OBSIDIAN_SHOVEL = register("obsidian_shovel", Item::new,
		p -> p.shovel(ModEquipment.OBSIDIAN, 2.5F, -3.0F), Rarity.EPIC);
	public static final Item OBSIDIAN_HOE = register("obsidian_hoe", Item::new,
		p -> p.hoe(ModEquipment.OBSIDIAN, -1.0F, 0.0F), Rarity.EPIC);
	public static final Item OBSIDIAN_SPEAR = register("obsidian_spear", Item::new,
		p -> p.spear(ModEquipment.OBSIDIAN, 0.95F, 0.95F, 0.6F, 2.5F, 11.0F, 6.75F, 5.1F, 11.25F, 4.6F), Rarity.EPIC);

	// ---------------------------------------------------------------- bedrock tier
	public static final Item BEDROCK_HELMET = register("bedrock_helmet", Item::new,
		p -> p.humanoidArmor(ModEquipment.BEDROCK_ARMOR, ArmorType.HELMET), Rarity.EPIC);
	public static final Item BEDROCK_CHESTPLATE = register("bedrock_chestplate", Item::new,
		p -> p.humanoidArmor(ModEquipment.BEDROCK_ARMOR, ArmorType.CHESTPLATE), Rarity.EPIC);
	public static final Item BEDROCK_LEGGINGS = register("bedrock_leggings", Item::new,
		p -> p.humanoidArmor(ModEquipment.BEDROCK_ARMOR, ArmorType.LEGGINGS), Rarity.EPIC);
	public static final Item BEDROCK_BOOTS = register("bedrock_boots", Item::new,
		p -> p.humanoidArmor(ModEquipment.BEDROCK_ARMOR, ArmorType.BOOTS), Rarity.EPIC);
	public static final Item BEDROCK_SWORD = register("bedrock_sword", Item::new,
		p -> p.sword(ModEquipment.BEDROCK, 7.5F, -2.4F), Rarity.EPIC);
	public static final Item BEDROCK_PICKAXE = register("bedrock_pickaxe", Item::new,
		p -> p.pickaxe(ModEquipment.BEDROCK, 3.0F, -2.8F), Rarity.EPIC);
	public static final Item BEDROCK_AXE = register("bedrock_axe", Item::new,
		p -> p.axe(ModEquipment.BEDROCK, 7.5F, -3.1F), Rarity.EPIC);
	public static final Item BEDROCK_SHOVEL = register("bedrock_shovel", Item::new,
		p -> p.shovel(ModEquipment.BEDROCK, 3.5F, -3.0F), Rarity.EPIC);
	public static final Item BEDROCK_HOE = register("bedrock_hoe", Item::new,
		p -> p.hoe(ModEquipment.BEDROCK, 0.0F, 0.0F), Rarity.EPIC);
	public static final Item BEDROCK_SPEAR = register("bedrock_spear", Item::new,
		p -> p.spear(ModEquipment.BEDROCK, 0.95F, 0.95F, 0.6F, 2.5F, 11.0F, 6.75F, 5.1F, 11.25F, 4.6F), Rarity.EPIC);

	// ---------------------------------------------------------------- echo tier
	public static final Item ECHO_HELMET = register("echo_helmet", Item::new,
		p -> p.humanoidArmor(ModEquipment.ECHO_ARMOR, ArmorType.HELMET), Rarity.EPIC);
	public static final Item ECHO_CHESTPLATE = register("echo_chestplate", Item::new,
		p -> p.humanoidArmor(ModEquipment.ECHO_ARMOR, ArmorType.CHESTPLATE), Rarity.EPIC);
	public static final Item ECHO_LEGGINGS = register("echo_leggings", Item::new,
		p -> p.humanoidArmor(ModEquipment.ECHO_ARMOR, ArmorType.LEGGINGS), Rarity.EPIC);
	public static final Item ECHO_BOOTS = register("echo_boots", Item::new,
		p -> p.humanoidArmor(ModEquipment.ECHO_ARMOR, ArmorType.BOOTS), Rarity.EPIC);
	public static final Item ECHO_SWORD = register("echo_sword", AmethystSwordItem::new,
		p -> p.component(ModComponents.AMETHYST_CHARGE, 0).sword(ModEquipment.ECHO, 8.5F, -2.4F), Rarity.EPIC);
	public static final Item ECHO_PICKAXE = register("echo_pickaxe", Item::new,
		p -> p.pickaxe(ModEquipment.ECHO, 4.0F, -2.8F), Rarity.EPIC);
	public static final Item ECHO_AXE = register("echo_axe", Item::new,
		p -> p.axe(ModEquipment.ECHO, 8.5F, -3.1F), Rarity.EPIC);
	public static final Item ECHO_SHOVEL = register("echo_shovel", Item::new,
		p -> p.shovel(ModEquipment.ECHO, 4.5F, -3.0F), Rarity.EPIC);
	public static final Item ECHO_HOE = register("echo_hoe", Item::new,
		p -> p.hoe(ModEquipment.ECHO, 1.0F, 0.0F), Rarity.EPIC);
	public static final Item ECHO_SPEAR = register("echo_spear", Item::new,
		p -> p.spear(ModEquipment.ECHO, 0.95F, 0.95F, 0.6F, 2.5F, 11.0F, 6.75F, 5.1F, 11.25F, 4.6F), Rarity.EPIC);

	// ---------------------------------------------------------------- starlight tier
	public static final Item STARLIGHT_HELMET = register("starlight_helmet", Item::new,
		p -> p.component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true).humanoidArmor(ModEquipment.STARLIGHT_ARMOR, ArmorType.HELMET), Rarity.EPIC);
	public static final Item STARLIGHT_CHESTPLATE = register("starlight_chestplate", Item::new,
		p -> p.component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true).humanoidArmor(ModEquipment.STARLIGHT_ARMOR, ArmorType.CHESTPLATE), Rarity.EPIC);
	public static final Item STARLIGHT_LEGGINGS = register("starlight_leggings", Item::new,
		p -> p.component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true).humanoidArmor(ModEquipment.STARLIGHT_ARMOR, ArmorType.LEGGINGS), Rarity.EPIC);
	public static final Item STARLIGHT_BOOTS = register("starlight_boots", Item::new,
		p -> p.component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true).humanoidArmor(ModEquipment.STARLIGHT_ARMOR, ArmorType.BOOTS), Rarity.EPIC);
	public static final Item STARLIGHT_SWORD = register("starlight_sword", AmethystSwordItem::new,
		p -> p.component(ModComponents.AMETHYST_CHARGE, 0)
			.component(ModComponents.STARLIGHT_MODE, 0)
			.sword(ModEquipment.STARLIGHT, 9.0F, -2.4F), Rarity.EPIC);
	public static final Item STARLIGHT_PICKAXE = register("starlight_pickaxe", Item::new,
		p -> p.component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true).pickaxe(ModEquipment.STARLIGHT, 5.0F, -2.8F), Rarity.EPIC);
	public static final Item STARLIGHT_AXE = register("starlight_axe", Item::new,
		p -> p.component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true).axe(ModEquipment.STARLIGHT, 9.0F, -3.1F), Rarity.EPIC);
	public static final Item STARLIGHT_SHOVEL = register("starlight_shovel", Item::new,
		p -> p.component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true).shovel(ModEquipment.STARLIGHT, 5.5F, -3.0F), Rarity.EPIC);
	public static final Item STARLIGHT_HOE = register("starlight_hoe", Item::new,
		p -> p.component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true).hoe(ModEquipment.STARLIGHT, 3.0F, 0.0F), Rarity.EPIC);
	public static final Item STARLIGHT_SPEAR = register("starlight_spear", Item::new,
		p -> p.component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true).spear(ModEquipment.STARLIGHT, 0.95F, 0.95F, 0.6F, 2.5F, 11.0F, 6.75F, 5.1F, 11.25F, 4.6F), Rarity.EPIC);

	// ---------------------------------------------------------------- shields
	// disableCooldownScale 0 means an axe hit can never knock the shield out of the player's hands
	public static final Item CARBON_STEEL_SHIELD = registerShield("carbon_steel_shield",
		1000, ModEquipment.CARBON_STEEL_INGOTS, Rarity.COMMON);
	public static final Item OBSIDIAN_SHIELD = registerShield("obsidian_shield",
		3000, ModEquipment.OBSIDIAN_ALLOY_INGOTS, Rarity.EPIC);
	public static final Item BEDROCK_SHIELD = registerShield("bedrock_shield",
		5000, ModEquipment.BEDROCK_SHARDS, Rarity.EPIC);
	public static final Item CARBON_STEEL_SHIELD_GLOW = registerShield("carbon_steel_shield_glow",
		1000, ModEquipment.CARBON_STEEL_INGOTS, Rarity.UNCOMMON, true);
	public static final Item OBSIDIAN_SHIELD_GLOW = registerShield("obsidian_shield_glow",
		3000, ModEquipment.OBSIDIAN_ALLOY_INGOTS, Rarity.EPIC, true);
	public static final Item BEDROCK_SHIELD_GLOW = registerShield("bedrock_shield_glow",
		5000, ModEquipment.BEDROCK_SHARDS, Rarity.EPIC, true);
	private ModItems() {
	}

	public static void init() {
	}

	private static Item registerShield(String name, int durability, TagKey<Item> repair, Rarity rarity) {
		return registerShield(name, durability, repair, rarity, false);
	}

	private static Item registerShield(String name, int durability, TagKey<Item> repair,
									   Rarity rarity, boolean glint) {
		return register(name, ShieldItem::new, p -> {
			p.durability(durability)
				.enchantable(14)
				.repairable(repair)
				.delayedComponent(DataComponents.BLOCKS_ATTACKS,
					provider -> shieldAttacks(provider, 0.0F));

			if (glint) {
				p.component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
			}

			return p;
		}, rarity);
	}

	private static BlocksAttacks shieldAttacks(HolderLookup.Provider provider, float disableScale) {
		return new BlocksAttacks(
			0.25F,
			disableScale,
			List.of(new BlocksAttacks.DamageReduction(90.0F, Optional.empty(), 0.0F, 1.0F)),
			new BlocksAttacks.ItemDamageFunction(3.0F, 1.0F, 1.0F),
			Optional.of(provider.getOrThrow(DamageTypeTags.BYPASSES_SHIELD)),
			Optional.of(SoundEvents.SHIELD_BLOCK),
			Optional.of(SoundEvents.SHIELD_BREAK));
	}

	private static Item register(String name, Function<Item.Properties, Item> factory,
								 UnaryOperator<Item.Properties> config, Rarity rarity) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, KalioupoEquipment.id(name));
		Item.Properties properties = config.apply(new Item.Properties().setId(key).rarity(rarity));
		Item item = factory.apply(properties);
		return Registry.register(BuiltInRegistries.ITEM, key, item);
	}
}
