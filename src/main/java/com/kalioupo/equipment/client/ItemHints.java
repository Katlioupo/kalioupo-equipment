package com.kalioupo.equipment.client;

import com.kalioupo.equipment.registry.ModItems;
import com.kalioupo.equipment.skill.Sockets;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * Tooltip lines: where the world-drop materials come from, what a material is worth as a socket,
 * and which sockets a piece of gear already has. Kept on the client so the server never has to
 * send anything.
 */
@Environment(EnvType.CLIENT)
public final class ItemHints {
	private ItemHints() {
	}

	public static void init() {
		ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
			if (stack.is(ModItems.BEDROCK_SHARD)) {
				lines.add(Component.translatable("tooltip.kalioupo_equipment.bedrock_shard.1")
					.withStyle(ChatFormatting.GRAY));
				lines.add(Component.translatable("tooltip.kalioupo_equipment.bedrock_shard.2")
					.withStyle(ChatFormatting.DARK_GRAY));
			} else if (stack.is(ModItems.COMPRESSED_OBSIDIAN_ALLOY)) {
				lines.add(Component.translatable("tooltip.kalioupo_equipment.compressed_obsidian.1")
					.withStyle(ChatFormatting.GRAY));
				lines.add(Component.translatable("tooltip.kalioupo_equipment.compressed_obsidian.2")
					.withStyle(ChatFormatting.DARK_GRAY));
			} else if (stack.is(ModItems.ECHO_CORE)) {
				lines.add(Component.translatable("tooltip.kalioupo_equipment.echo_core.1")
					.withStyle(ChatFormatting.GRAY));
				lines.add(Component.translatable("tooltip.kalioupo_equipment.echo_core.2")
					.withStyle(ChatFormatting.DARK_GRAY));
			}

			addMaterialTrait(stack, lines);
			addSockets(stack, lines);
		});
	}

	/** "副位特性：技能充能 +10%" on the materials that actually do something in a socket. */
	private static void addMaterialTrait(ItemStack stack, List<Component> lines) {
		if (!Sockets.hasTrait(stack)) {
			return;
		}

		lines.add(Component.translatable("tooltip.kalioupo_equipment.material_trait", traitValue(stack))
			.withStyle(ChatFormatting.DARK_AQUA));
	}

	/** "副位 1/2" plus one line per socketed item. */
	private static void addSockets(ItemStack stack, List<Component> lines) {
		int limit = Sockets.slots(stack);

		if (limit <= 0) {
			return;
		}

		List<com.kalioupo.equipment.skill.SocketedItem> sockets = Sockets.get(stack);
		lines.add(Component.translatable("tooltip.kalioupo_equipment.sockets", sockets.size(), limit)
			.withStyle(sockets.isEmpty() ? ChatFormatting.DARK_GRAY : ChatFormatting.AQUA));

		for (com.kalioupo.equipment.skill.SocketedItem socket : sockets) {
			lines.add(Component.translatable("tooltip.kalioupo_equipment.socket.entry.trait",
					nameOf(socket.id()), traitOf(socket.id()))
				.withStyle(ChatFormatting.GRAY));
		}
	}

	private static Component traitValue(ItemStack stack) {
		String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
		return traitOf(id);
	}

	private static Component traitOf(String id) {
		boolean armor = id.endsWith("_helmet") || id.endsWith("_chestplate")
			|| id.endsWith("_leggings") || id.endsWith("_boots");
		String suffix = armor ? ".armor" : ".ingot";

		Sockets.Skill skill = Sockets.skillOf(id);

		if (skill != Sockets.Skill.NONE) {
			return Component.translatable("trait.kalioupo_equipment.skill."
				+ skill.name().toLowerCase(java.util.Locale.ROOT)).withStyle(ChatFormatting.GOLD);
		}

		if (Sockets.chargeOf(id) > 0) {
			return Component.translatable("trait.kalioupo_equipment.copper" + suffix)
				.withStyle(ChatFormatting.AQUA);
		}

		if (Sockets.repairOf(id) > 0) {
			return Component.translatable("trait.kalioupo_equipment.rose_gold" + suffix)
				.withStyle(ChatFormatting.LIGHT_PURPLE);
		}

		if (Sockets.waterOf(id)) {
			return Component.translatable("trait.kalioupo_equipment.prismarine" + suffix)
				.withStyle(ChatFormatting.BLUE);
		}

		if (Sockets.fallOf(id)) {
			return Component.translatable("trait.kalioupo_equipment.phantom" + suffix)
				.withStyle(ChatFormatting.GRAY);
		}

		if (Sockets.quartzOf(id)) {
			return Component.translatable("trait.kalioupo_equipment.quartz" + suffix)
				.withStyle(ChatFormatting.WHITE);
		}

		return Component.translatable("trait.kalioupo_equipment.none");
	}

	private static Component nameOf(String id) {
		Identifier key = Identifier.tryParse(id);

		if (key != null) {
			Item item = BuiltInRegistries.ITEM.get(key).map(holder -> holder.value()).orElse(null);

			if (item != null && item != Items.AIR) {
				return new ItemStack(item).getHoverName();
			}
		}

		return Component.literal(id);
	}
}
