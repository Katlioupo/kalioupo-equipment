package com.kalioupo.equipment.skill;

import com.kalioupo.equipment.KalioupoEquipment;
import com.kalioupo.equipment.registry.ModItems;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;

import java.util.List;

/**
 * Awards the progression advancements from code.
 *
 * <p>The json criteria are {@code minecraft:impossible}, so nothing can fire on its own. Every
 * second the inventory is scanned instead, which also covers items the player already owned
 * before the advancement was added - the reason the vanilla inventory trigger kept missing.</p>
 */
public final class AdvancementTicker {
	private record Check(String id, List<Item> items) {
	}

	private static final List<Check> CHECKS = List.of(
		new Check("better_than_iron", List.of(ModItems.CARBON_STEEL_INGOT)),
		new Check("stronger_metal", List.of(
			ModItems.QUARTZ_STEEL_INGOT, ModItems.COPPER_ALLOY_INGOT, ModItems.ROSE_GOLD_INGOT,
			ModItems.PRISMARINE_ALLOY_INGOT, ModItems.AMETHYST_ALLOY_INGOT, ModItems.PHANTOM_INGOT,
			ModItems.COMPRESSED_OBSIDIAN_ALLOY, ModItems.ECHO_ALLOY_INGOT, ModItems.STARLIGHT_ALLOY_INGOT)),
		new Check("quartz_usable", List.of(ModItems.QUARTZ_STEEL_INGOT)),
		new Check("copper_tough", List.of(ModItems.COPPER_ALLOY_INGOT)),
		new Check("rose_gold_what", List.of(ModItems.ROSE_GOLD_INGOT)),
		new Check("specialised", List.of(ModItems.PRISMARINE_ALLOY_INGOT)),
		new Check("magic_power", List.of(ModItems.AMETHYST_SWORD)),
		new Check("aerodynamics", List.of(ModItems.PHANTOM_INGOT)),
		new Check("obsidian_power", List.of(
			ModItems.OBSIDIAN_HELMET, ModItems.OBSIDIAN_CHESTPLATE,
			ModItems.OBSIDIAN_LEGGINGS, ModItems.OBSIDIAN_BOOTS)),
		new Check("bedrock_chiseler", List.of(ModItems.BEDROCK_SHARD)),
		new Check("warden_slayer", List.of(ModItems.ECHO_ALLOY_INGOT)),
		new Check("road_to_godhood", List.of(
			ModItems.BEDROCK_HELMET, ModItems.BEDROCK_CHESTPLATE, ModItems.BEDROCK_LEGGINGS,
			ModItems.BEDROCK_BOOTS, ModItems.BEDROCK_SWORD, ModItems.BEDROCK_PICKAXE, ModItems.BEDROCK_AXE,
			ModItems.BEDROCK_SHOVEL, ModItems.BEDROCK_HOE, ModItems.BEDROCK_SPEAR)),
		new Check("peak", List.of(
			ModItems.ECHO_HELMET, ModItems.ECHO_CHESTPLATE, ModItems.ECHO_LEGGINGS,
			ModItems.ECHO_BOOTS, ModItems.ECHO_SWORD, ModItems.ECHO_PICKAXE, ModItems.ECHO_AXE,
			ModItems.ECHO_SHOVEL, ModItems.ECHO_HOE, ModItems.ECHO_SPEAR)),
		new Check("king_of_the_world", List.of(
			ModItems.STARLIGHT_HELMET, ModItems.STARLIGHT_CHESTPLATE, ModItems.STARLIGHT_LEGGINGS,
			ModItems.STARLIGHT_BOOTS, ModItems.STARLIGHT_SWORD, ModItems.STARLIGHT_PICKAXE,
			ModItems.STARLIGHT_AXE, ModItems.STARLIGHT_SHOVEL, ModItems.STARLIGHT_HOE,
			ModItems.STARLIGHT_SPEAR))
	);

	private AdvancementTicker() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(AdvancementTicker::tick);
	}

	private static void tick(MinecraftServer server) {
		if (server.getTickCount() % 20 != 0) {
			return;
		}

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			// one pass over the slots, then a set lookup per check: ~46 iterations a second,
			// the same cost as a single slot hover. No per-tick scanning of anything else.
			var carried = carriedItems(player);

			if (carried.isEmpty()) {
				continue;
			}

			for (Check check : CHECKS) {
				AdvancementHolder holder = server.getAdvancements().get(KalioupoEquipment.id(check.id()));

				if (holder == null || player.getAdvancements().getOrStartProgress(holder).isDone()) {
					continue;
				}

				if (check.items().stream().anyMatch(carried::contains)) {
					player.getAdvancements().award(holder, "awarded");
				}
			}
		}
	}

	private static java.util.Set<Item> carriedItems(ServerPlayer player) {
		java.util.Set<Item> items = new java.util.HashSet<>();

		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			var stack = player.getInventory().getItem(slot);

			if (!stack.isEmpty()) {
				items.add(stack.getItem());
			}
		}

		return items;
	}
}
