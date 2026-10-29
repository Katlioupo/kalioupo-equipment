package com.kalioupo.equipment.skill;

import com.kalioupo.equipment.registry.ModItems;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.core.Holder;

/**
 * Set passives that can be felt without reading anything: the higher tiers keep a couple of
 * vanilla effects running while the pieces are worn. Refreshed every second, no particles,
 * icon shown so the player can see what the set is giving them.
 *
 * <p>The health ladder climbs one step per tier, so an upgrade never costs health. A health
 * boost level is worth two hearts, which gives this table for a full set:</p>
 *
 * <table>
 *   <tr><th>tier</th><th>2-3 pieces</th><th>4 pieces</th><th>total hearts</th></tr>
 *   <tr><td>obsidian</td><td>+2 hearts</td><td>+2 hearts</td><td>12</td></tr>
 *   <tr><td>bedrock</td><td>+2 hearts</td><td>+4 hearts</td><td>14</td></tr>
 *   <tr><td>echo</td><td>+4 hearts, regeneration</td><td>+6 hearts, regeneration</td><td>16</td></tr>
 *   <tr><td>starlight</td><td>+6 hearts, resistance</td><td>+10 hearts, resistance</td><td>20</td></tr>
 * </table>
 *
 * <p>The starlight set tops out at 20 hearts, exactly two rows of the vanilla hud.</p>
 */
public final class ArmorBuffs {
	private static final int REFRESH_TICKS = 20;
	private static final int DURATION = 70;

	private ArmorBuffs() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(ArmorBuffs::tickServer);
	}

	private static void tickServer(MinecraftServer server) {
		long time = server.overworld() == null ? 0L : server.overworld().getGameTime();

		if (time % REFRESH_TICKS != 0) {
			return;
		}

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			int obsidian = count(player, ModItems.OBSIDIAN_HELMET, ModItems.OBSIDIAN_CHESTPLATE,
				ModItems.OBSIDIAN_LEGGINGS, ModItems.OBSIDIAN_BOOTS);
			int bedrock = count(player, ModItems.BEDROCK_HELMET, ModItems.BEDROCK_CHESTPLATE,
				ModItems.BEDROCK_LEGGINGS, ModItems.BEDROCK_BOOTS);
			int echo = count(player, ModItems.ECHO_HELMET, ModItems.ECHO_CHESTPLATE,
				ModItems.ECHO_LEGGINGS, ModItems.ECHO_BOOTS);
			int starlight = count(player, ModItems.STARLIGHT_HELMET, ModItems.STARLIGHT_CHESTPLATE,
				ModItems.STARLIGHT_LEGGINGS, ModItems.STARLIGHT_BOOTS);

			apply(player, obsidian, bedrock, echo, starlight);
		}
	}

	private static void apply(ServerPlayer player, int obsidian, int bedrock, int echo, int starlight) {
		// the best set wins, lower tiers do not stack on top of it. Health boost level n is
		// worth (n + 1) * 2 hearts, so the amplifier doubles as the rung on the ladder.
		if (starlight >= 2) {
			give(player, MobEffects.RESISTANCE, 0);
			give(player, MobEffects.HEALTH_BOOST, starlight >= 4 ? 4 : 3);
		} else if (echo >= 2) {
			give(player, MobEffects.REGENERATION, 0);
			give(player, MobEffects.HEALTH_BOOST, echo >= 4 ? 2 : 1);
		} else if (bedrock >= 2) {
			give(player, MobEffects.HEALTH_BOOST, bedrock >= 4 ? 1 : 0);
		} else if (obsidian >= 2) {
			give(player, MobEffects.HEALTH_BOOST, 0);
		}
	}

	private static void give(ServerPlayer player, Holder<MobEffect> effect, int amplifier) {
		player.addEffect(new MobEffectInstance(effect, DURATION, amplifier, true, false, true));
	}

	private static int count(ServerPlayer player, net.minecraft.world.item.Item helmet,
							 net.minecraft.world.item.Item chest,
							 net.minecraft.world.item.Item legs,
							 net.minecraft.world.item.Item boots) {
		int pieces = 0;

		if (player.getItemBySlot(EquipmentSlot.HEAD).is(helmet)) {
			pieces++;
		}
		if (player.getItemBySlot(EquipmentSlot.CHEST).is(chest)) {
			pieces++;
		}
		if (player.getItemBySlot(EquipmentSlot.LEGS).is(legs)) {
			pieces++;
		}
		if (player.getItemBySlot(EquipmentSlot.FEET).is(boots)) {
			pieces++;
		}

		return pieces;
	}
}
