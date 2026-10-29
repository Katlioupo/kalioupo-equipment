package com.kalioupo.equipment.skill;

import com.kalioupo.equipment.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Starlight set passives.
 *
 * <ul>
 *   <li>every worn piece shortens how long the player keeps burning</li>
 *   <li>a full set leaves a faint trail of starlight so the wearer glows in the dark</li>
 * </ul>
 */
public final class StarlightAura {
	/**
	 * Vanilla already burns one tick per tick. A full set adds another 0.45 of a tick per tick,
	 * which works out to roughly -45% burn time, exactly like a fire protection passive.
	 */
	private static final double EXTRA_BURN_PER_PIECE = 0.1125;

	/** fractional burn ticks that have not been cashed in yet */
	private static final Map<UUID, Double> BURN_ACC = new HashMap<>();

	private StarlightAura() {
	}

	public static void tickServer(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			int pieces = countPieces(player);

			if (pieces == 0) {
				continue;
			}

			if (player.isOnFire() && player.getRemainingFireTicks() > 0) {
				double acc = BURN_ACC.getOrDefault(player.getUUID(), 0.0)
					+ EXTRA_BURN_PER_PIECE * pieces;

				if (acc >= 1.0) {
					int extra = (int) acc;
					acc -= extra;
					player.setRemainingFireTicks(
						Math.max(0, player.getRemainingFireTicks() - extra));
				}

				BURN_ACC.put(player.getUUID(), acc);
			} else {
				BURN_ACC.remove(player.getUUID());
			}

			if (pieces == 4 && player.level() instanceof ServerLevel level && level.getGameTime() % 10 == 0) {
				level.sendParticles(ParticleTypes.END_ROD,
					player.getX() + (level.getRandom().nextDouble() - 0.5) * 0.8,
					player.getY() + 0.3 + level.getRandom().nextDouble() * 1.6,
					player.getZ() + (level.getRandom().nextDouble() - 0.5) * 0.8,
					1, 0.0, 0.0, 0.0, 0.0);
			}
		}
	}

	private static int countPieces(ServerPlayer player) {
		int pieces = 0;

		if (player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.STARLIGHT_HELMET)) {
			pieces++;
		}
		if (player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.STARLIGHT_CHESTPLATE)) {
			pieces++;
		}
		if (player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.STARLIGHT_LEGGINGS)) {
			pieces++;
		}
		if (player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.STARLIGHT_BOOTS)) {
			pieces++;
		}

		return pieces;
	}
}
