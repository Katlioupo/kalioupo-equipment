package com.kalioupo.equipment.skill;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityTypes;

/**
 * A little ceremony for the big kills.
 *
 * <p>Killing a wither plays the challenge-complete jingle, the same sound a warden kill gets. No
 * loot, no buffs, purely the "that was an epic fight" feedback the vanilla game gives you for
 * advancements.</p>
 */
public final class BossSounds {
	private BossSounds() {
	}

	public static void init() {
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity.getType() != EntityTypes.WITHER || !(entity.level() instanceof ServerLevel level)) {
				return;
			}

			level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
				SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.2F, 1.0F);
		});
	}
}
