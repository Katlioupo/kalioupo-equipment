package com.kalioupo.equipment.skill;

import com.kalioupo.equipment.registry.ModItems;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlot;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Full obsidian set: heavy hits are blunted.
 * Up to 8 damage is reduced by 10%, everything above 8 is reduced by 35%.
 * Standing still and taking hits will still kill you, it is not invulnerability.
 */
public final class ObsidianDamageReduction {
	private static final float THRESHOLD = 8.0F;
	private static final float LOW_REDUCTION = 0.10F;
	private static final float HIGH_REDUCTION = 0.35F;

	/** players whose reduced hit is currently being applied, so we never re-enter ourselves */
	private static final Set<UUID> APPLYING = new HashSet<>();

	private ObsidianDamageReduction() {
	}

	public static void init() {
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (!(entity instanceof ServerPlayer player)) {
				return true;
			}

			// this is our own reduced hit coming back in - let it through untouched
			if (APPLYING.contains(player.getUUID())) {
				return true;
			}

			if (!hasFullSet(player) || !(player.level() instanceof ServerLevel level)) {
				return true;
			}

			// never touch damage that ignores armour entirely
			if (source.is(DamageTypeTags.BYPASSES_ARMOR)) {
				return true;
			}

			float reduced = reduce(amount);

			if (reduced >= amount - 1.0E-4F) {
				return true;
			}

			APPLYING.add(player.getUUID());

			try {
				player.hurtServer(level, source, reduced);
			} finally {
				APPLYING.remove(player.getUUID());
			}

			return false;
		});
	}

	public static float reduce(float amount) {
		float low = Math.min(amount, THRESHOLD);
		float high = Math.max(0.0F, amount - THRESHOLD);
		return low * (1.0F - LOW_REDUCTION) + high * (1.0F - HIGH_REDUCTION);
	}

	private static boolean hasFullSet(ServerPlayer player) {
		return player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.OBSIDIAN_HELMET)
			&& player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.OBSIDIAN_CHESTPLATE)
			&& player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.OBSIDIAN_LEGGINGS)
			&& player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.OBSIDIAN_BOOTS);
	}
}
