package com.kalioupo.equipment.skill;

import com.kalioupo.equipment.registry.ModComponents;
import com.kalioupo.equipment.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Starlight sword skill system.
 *
 * <p>Two modes share one charge bar; the mode is stored on the stack so it survives logout:</p>
 * <ol>
 *   <li><b>声波</b> - the echo beam, but stronger and longer.</li>
 *   <li><b>星辉瞬移</b> - nether star blink to the crosshair, capped at {@link #BLINK_RANGE}.</li>
 * </ol>
 */
public final class StarlightSkill {
	public static final int MODE_BOOM = 0;
	public static final int MODE_BLINK = 1;
	public static final int MODE_THUNDER = 2;
	public static final int MODE_BLESSING = 3;
	public static final int MODE_COUNT = 4;

	public static final double BEAM_RANGE = 28.0;
	/** the thunder mode reaches further than the amethyst bolt, it is the endgame version */
	public static final double THUNDER_RANGE = 40.0;
	public static final double BLINK_RANGE = 20.0;

	private static final float DAMAGE = 18.0F;
	private static final float THUNDER_DAMAGE = 22.0F;
	private static final double BEAM_RADIUS = 1.3;
	/** hostiles this close are hit without aiming, so a brawl in melee range does not miss */
	private static final double NEAR_LOCK = 6.0;
	/** further out the crosshair only has to pass within this many blocks of the target */
	private static final double AIM_ASSIST = 2.5;
	/** how much of the bar each skill demands */
	private static final int BLINK_CHARGE = 40;
	private static final int THUNDER_CHARGE = 60;
	private static final int BLESSING_CHARGE = 70;

	private static final String[] MESSAGES = {
		"message.kalioupo_equipment.mode_boom",
		"message.kalioupo_equipment.mode_blink",
		"message.kalioupo_equipment.mode_thunder",
		"message.kalioupo_equipment.mode_blessing"
	};

	private StarlightSkill() {
	}

	public static int getMode(ItemStack stack) {
		return stack.getOrDefault(ModComponents.STARLIGHT_MODE, MODE_BOOM);
	}

	/** The starlight sword itself, or any weapon with one socketed. */
	public static boolean hasSkill(ItemStack held) {
		return Sockets.effective(held) == Sockets.Skill.STARLIGHT;
	}

	/** Cycles the mode on the held sword; returns the new mode, or -1 when nothing was held. */
	public static int cycleMode(ServerPlayer player) {
		ItemStack held = player.getMainHandItem();

		if (!hasSkill(held)) {
			return -1;
		}

		int next = (getMode(held) + 1) % MODE_COUNT;
		held.set(ModComponents.STARLIGHT_MODE, next);
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
			next == MODE_BOOM ? SoundEvents.WARDEN_SONIC_CHARGE : SoundEvents.ENDER_EYE_LAUNCH,
			SoundSource.PLAYERS, 0.8F, 1.2F + next * 0.1F);
		player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable(MESSAGES[next]));
		return next;
	}

	public static boolean ready(ServerPlayer player) {
		ItemStack held = player.getMainHandItem();

		if (!hasSkill(held)) {
			return false;
		}

		return AmethystSkill.getCharge(held) >= chargeCost(getMode(held));
	}

	private static int chargeCost(int mode) {
		return switch (mode) {
			case MODE_BLINK -> BLINK_CHARGE;
			case MODE_THUNDER -> THUNDER_CHARGE;
			case MODE_BLESSING -> BLESSING_CHARGE;
			default -> AmethystSkill.MAX_CHARGE;
		};
	}

	public static boolean cast(ServerPlayer player) {
		if (!ready(player) || !(player.level() instanceof ServerLevel level)) {
			return false;
		}

		ItemStack held = player.getMainHandItem();

		return switch (getMode(held)) {
			case MODE_BLINK -> blink(player, level, held);
			case MODE_THUNDER -> thunder(player, level, held);
			case MODE_BLESSING -> blessing(player, level, held);
			default -> boom(player, level, held);
		};
	}

	/** Calls a real lightning bolt down on whatever the crosshair points at. */
	private static boolean thunder(ServerPlayer player, ServerLevel level, ItemStack held) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle();

		// aim at the entity first: a drowned under water is behind the fluid surface and the
		// block raycast would otherwise report the water as the target point
		LivingEntity target = aimAt(player, level, eye, look);

		Vec3 point;

		if (target != null) {
			point = target.position();
		} else {
			HitResult hit = player.pick(THUNDER_RANGE, 0.0F, false);
			point = hit.getType() == HitResult.Type.MISS
				? eye.add(look.scale(THUNDER_RANGE))
				: hit.getLocation();
		}

		LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);

		if (bolt != null) {
			bolt.setPos(point.x, point.y, point.z);
			// visual only: a real bolt would set the ancient city on fire
			bolt.setVisualOnly(true);
			bolt.setCause(player);
			level.addFreshEntity(bolt);
		}

		level.playSound(null, point.x, point.y, point.z, SoundEvents.LIGHTNING_BOLT_THUNDER,
			SoundSource.PLAYERS, 2.2F, 1.05F);

		DamageSource source = level.damageSources().indirectMagic(player, player);
		AABB area = new AABB(point.add(-3.0, -2.0, -3.0), point.add(3.0, 3.0, 3.0));

		for (Entity entity : level.getEntities(player, area)) {
			if (!(entity instanceof LivingEntity living) || living == player || !living.isAlive()) {
				continue;
			}

			float damage = EnchantmentHelper.modifyDamage(level, held, living, source, THUNDER_DAMAGE);
			living.hurtServer(level, source, damage);
		}

		finish(player, held);
		return true;
	}

	/** Nearest hostile close by needs no aiming at all; further out it has to be under the crosshair. */
	private static LivingEntity aimAt(ServerPlayer player, ServerLevel level, Vec3 eye, Vec3 look) {
		LivingEntity near = null;
		double nearScore = Double.MAX_VALUE;
		LivingEntity best = null;
		double bestScore = Double.MAX_VALUE;

		for (Entity entity : level.getEntities(player, player.getBoundingBox().inflate(THUNDER_RANGE))) {
			if (!(entity instanceof LivingEntity living) || living == player || !living.isAlive()) {
				continue;
			}

			// never the player's own pets, and never other players
			if (living instanceof TamableAnimal pet && pet.isOwnedBy(player)) {
				continue;
			}

			if (living instanceof Player) {
				continue;
			}

			Vec3 to = living.getBoundingBox().getCenter().subtract(eye);
			double distance = to.length();

			if (distance > THUNDER_RANGE || distance < 0.1) {
				continue;
			}

			double hostilePenalty = living instanceof Enemy ? 0.0 : 1000.0;

			if (distance <= NEAR_LOCK) {
				double score = distance + hostilePenalty;

				if (score < nearScore) {
					nearScore = score;
					near = living;
				}

				continue;
			}

			Vec3 direction = to.normalize();
			double along = direction.dot(look);

			if (along <= 0.0) {
				continue;
			}

			// distance from the crosshair line, so a target does not have to be dead centre
			double offAxis = Math.sqrt(Math.max(0.0, 1.0 - along * along)) * distance;

			if (offAxis > AIM_ASSIST) {
				continue;
			}

			double score = distance + offAxis * 2.0 + hostilePenalty;

			if (score < bestScore) {
				bestScore = score;
				best = living;
			}
		}

		// whatever the crosshair is actually on wins; the near lock only fills in when nothing is
		return best != null ? best : near;
	}

	/** Blessing: heals over time plus a short window of resistance and strength. */
	private static boolean blessing(ServerPlayer player, ServerLevel level, ItemStack held) {
		player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 400, 1, true, true, true));
		player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 600, 0, true, true, true));
		player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 400, 0, true, true, true));

		// nearby allies get a shorter version, so the support build is worth playing
		for (ServerPlayer ally : level.getServer().getPlayerList().getPlayers()) {
			if (ally == player || ally.level() != level || ally.distanceToSqr(player) > 100.0) {
				continue;
			}

			ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0, true, true, true));
			ally.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 300, 0, true, true, true));
		}

		level.playSound(null, player.getX(), player.getY(), player.getZ(),
			SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2F, 1.4F);

		for (int i = 0; i < 40; i++) {
			double angle = i * (Math.PI * 2.0 / 20.0);
			double radius = 0.4 + (i / 20.0) * 1.4;
			level.sendParticles(ParticleTypes.END_ROD,
				player.getX() + Math.cos(angle) * radius,
				player.getY() + (i % 20) * 0.1,
				player.getZ() + Math.sin(angle) * radius,
				1, 0.0, 0.03, 0.0, 0.0);
		}

		finish(player, held);
		return true;
	}

	private static boolean boom(ServerPlayer player, ServerLevel level, ItemStack held) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle();

		HitResult hit = player.pick(BEAM_RANGE, 0.0F, false);
		double length = hit.getType() == HitResult.Type.MISS ? BEAM_RANGE : hit.getLocation().distanceTo(eye);

		level.playSound(null, eye.x, eye.y, eye.z, SoundEvents.WARDEN_SONIC_BOOM,
			SoundSource.PLAYERS, 3.0F, 1.15F);

		for (double d = 0.0; d <= length; d += 0.7) {
			Vec3 point = eye.add(look.scale(d));
			level.sendParticles(ParticleTypes.SONIC_BOOM, point.x, point.y, point.z, 1, 0.0, 0.0, 0.0, 0.0);
		}
		for (double d = 0.0; d <= length; d += 1.6) {
			Vec3 point = eye.add(look.scale(d));
			level.sendParticles(ParticleTypes.END_ROD, point.x, point.y, point.z, 1, 0.06, 0.06, 0.06, 0.0);
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

			if (to.subtract(look.scale(along)).length() > BEAM_RADIUS) {
				continue;
			}

			float damage = EnchantmentHelper.modifyDamage(level, held, living, source, DAMAGE);
			living.hurtServer(level, source, damage);
			living.push(look.x * 1.3, 0.4, look.z * 1.3);
		}

		finish(player, held);
		return true;
	}

	private static boolean blink(ServerPlayer player, ServerLevel level, ItemStack held) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle();

		HitResult hit = player.pick(BLINK_RANGE, 0.0F, false);
		Vec3 from = player.position();

		// stay one block short of a wall so the player never lands inside a block
		Vec3 target = hit.getType() == HitResult.Type.MISS
			? eye.add(look.scale(BLINK_RANGE))
			: hit.getLocation().subtract(look.scale(0.8));
		target = new Vec3(target.x, Math.max(target.y, level.getMinY()), target.z);

		if (from.distanceTo(target) < 1.0) {
			return false;
		}

		Vec3 safe = findSafeSpot(level, target, player);
		if (safe == null) {
			level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6F, 0.6F);
			return false;
		}

		for (int i = 0; i < 24; i++) {
			level.sendParticles(ParticleTypes.PORTAL,
				from.x + (level.getRandom().nextDouble() - 0.5) * 0.8,
				from.y + level.getRandom().nextDouble() * 1.8,
				from.z + (level.getRandom().nextDouble() - 0.5) * 0.8,
				1, 0.0, 0.0, 0.0, 0.15);
		}

		player.teleportTo(safe.x, safe.y, safe.z);

		for (int i = 0; i < 30; i++) {
			level.sendParticles(ParticleTypes.END_ROD,
				safe.x + (level.getRandom().nextDouble() - 0.5) * 1.0,
				safe.y + level.getRandom().nextDouble() * 1.8,
				safe.z + (level.getRandom().nextDouble() - 0.5) * 1.0,
				1, 0.0, 0.02, 0.0, 0.02);
		}

		level.playSound(null, safe.x, safe.y, safe.z, SoundEvents.ENDERMAN_TELEPORT,
			SoundSource.PLAYERS, 1.0F, 1.4F);
		level.playSound(null, safe.x, safe.y, safe.z, SoundEvents.AMETHYST_BLOCK_CHIME,
			SoundSource.PLAYERS, 1.0F, 1.8F);

		// no fall damage for the arrival, otherwise blinking onto a roof hurts
		player.resetFallDistance();

		finish(player, held);
		return true;
	}

	/** Walks up/down a little to find a spot where the player actually fits. */
	private static Vec3 findSafeSpot(ServerLevel level, Vec3 target, ServerPlayer player) {
		for (int dy = 0; dy >= -3; dy--) {
			Vec3 candidate = target.add(0.0, dy, 0.0);
			AABB box = player.getDimensions(player.getPose()).makeBoundingBox(candidate);

			if (level.noCollision(player, box)) {
				return candidate;
			}
		}

		for (int dy = 1; dy <= 2; dy++) {
			Vec3 candidate = target.add(0.0, dy, 0.0);
			AABB box = player.getDimensions(player.getPose()).makeBoundingBox(candidate);

			if (level.noCollision(player, box)) {
				return candidate;
			}
		}

		return null;
	}

	private static void finish(ServerPlayer player, ItemStack held) {
		// charge alone is the gate: a cooldown on top of a charge bar is just dead time
		AmethystSkill.setCharge(held, 0);
		player.swingAndResetAttackStrength(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
	}
}
