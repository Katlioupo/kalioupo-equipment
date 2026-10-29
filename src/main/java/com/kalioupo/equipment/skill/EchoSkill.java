package com.kalioupo.equipment.skill;

import com.kalioupo.equipment.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Echo set skill: a warden style sonic boom fired along the crosshair.
 *
 * <p>Charge is shared with the amethyst sword (same component, same HUD bar), but releasing it
 * needs the full echo set on top of the sword. The beam stops at the first block it meets and
 * damages everything in its path; against a warden the extra power comes from the stagger
 * rolls in {@link EchoWarden}, not from a bigger number here.</p>
 */
public final class EchoSkill {
	public static final double RANGE = 24.0;
	public static final int MAX_CHARGE = AmethystSkill.MAX_CHARGE;

	private static final float DAMAGE = 14.0F;
	private static final double BEAM_RADIUS = 1.3;
	private static final int COOLDOWN_TICKS = 120;

	private EchoSkill() {
	}

	public static boolean hasFullSet(ServerPlayer player) {
		return player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.ECHO_HELMET)
			&& player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.ECHO_CHESTPLATE)
			&& player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.ECHO_LEGGINGS)
			&& player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.ECHO_BOOTS);
	}

	/** A charged echo sword (or a weapon with one socketed) is all it takes. */
	public static boolean ready(ServerPlayer player) {
		ItemStack held = player.getMainHandItem();
		boolean hasSkill = Sockets.effective(held) == Sockets.Skill.ECHO;

		return hasSkill
			&& AmethystSkill.getCharge(held) >= MAX_CHARGE
			&& !player.getCooldowns().isOnCooldown(held);
	}

	public static boolean cast(ServerPlayer player) {
		if (!ready(player) || !(player.level() instanceof ServerLevel level)) {
			return false;
		}

		ItemStack held = player.getMainHandItem();
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle();

		// stop the beam at the first block, the same way the warden does
		HitResult hit = player.pick(RANGE, 0.0F, false);
		double length = hit.getType() == HitResult.Type.MISS ? RANGE : hit.getLocation().distanceTo(eye);

		level.playSound(null, eye.x, eye.y, eye.z, SoundEvents.WARDEN_SONIC_BOOM,
			SoundSource.PLAYERS, 3.0F, 1.0F);

		for (double d = 0.0; d <= length; d += 0.8) {
			Vec3 point = eye.add(look.scale(d));
			level.sendParticles(ParticleTypes.SONIC_BOOM, point.x, point.y, point.z, 1, 0.0, 0.0, 0.0, 0.0);
		}

		DamageSource source = level.damageSources().sonicBoom(player);
		AABB area = new AABB(eye, eye.add(look.scale(length))).inflate(BEAM_RADIUS);

		for (Entity entity : level.getEntities(player, area)) {
			if (!(entity instanceof LivingEntity living) || living == player || !living.isAlive()) {
				continue;
			}

			Vec3 to = living.getBoundingBox().getCenter().subtract(eye);
			double along = to.dot(look);

			if (along < 0.0 || along > length) {
				continue;
			}

			// distance from the beam axis, not from the player
			if (to.subtract(look.scale(along)).length() > BEAM_RADIUS) {
				continue;
			}

			// the warden bonus lives in EchoWarden, so the beam and a sword swing never stack it
			float damage = EnchantmentHelper.modifyDamage(level, held, living, source, DAMAGE);
			living.hurtServer(level, source, damage);

			living.push(look.x * 1.1, 0.32, look.z * 1.1);
		}

		AmethystSkill.setCharge(held, 0);
		player.getCooldowns().addCooldown(held, COOLDOWN_TICKS);
		player.swingAndResetAttackStrength(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
		return true;
	}

	/** Neutral mobs also take the hit, the beam just prefers whatever is closest. */
	public static boolean isHostile(LivingEntity entity) {
		return entity instanceof Enemy;
	}
}
