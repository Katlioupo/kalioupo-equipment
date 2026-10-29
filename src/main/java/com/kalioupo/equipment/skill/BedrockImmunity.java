package com.kalioupo.equipment.skill;

import com.kalioupo.equipment.registry.ModItems;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * Full bedrock set - and everything upgraded from it: the warden's darkness is shaken off and
 * its sonic boom does nothing, so the ancient city can be looted head on.
 */
public final class BedrockImmunity {
	private BedrockImmunity() {
	}

	public static void init() {
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (!(entity instanceof ServerPlayer player) || !hasFullSet(player)) {
				return true;
			}

			// sonic boom: no damage at all
			return !source.is(DamageTypes.SONIC_BOOM);
		});
	}

	public static void tickServer(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (hasFullSet(player) && player.hasEffect(MobEffects.DARKNESS)) {
				player.removeEffect(MobEffects.DARKNESS);
			}
		}
	}

	public static boolean hasFullSet(ServerPlayer player) {
		return hasBedrock(player) || hasEcho(player) || hasStarlight(player);
	}

	private static boolean hasBedrock(ServerPlayer player) {
		return player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.BEDROCK_HELMET)
			&& player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.BEDROCK_CHESTPLATE)
			&& player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.BEDROCK_LEGGINGS)
			&& player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.BEDROCK_BOOTS);
	}

	private static boolean hasEcho(ServerPlayer player) {
		return player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.ECHO_HELMET)
			&& player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.ECHO_CHESTPLATE)
			&& player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.ECHO_LEGGINGS)
			&& player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.ECHO_BOOTS);
	}

	private static boolean hasStarlight(ServerPlayer player) {
		return player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.STARLIGHT_HELMET)
			&& player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.STARLIGHT_CHESTPLATE)
			&& player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.STARLIGHT_LEGGINGS)
			&& player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.STARLIGHT_BOOTS);
	}
}
