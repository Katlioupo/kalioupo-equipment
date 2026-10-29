package com.kalioupo.equipment.skill;

import com.kalioupo.equipment.registry.ModItems;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Warden interaction and the stun ladder.
 *
 * <ul>
 *   <li><b>Bedrock sword</b> - one guaranteed stagger per ten seconds; between those, every hit
 *       also rolls a low chance with no cooldown, so a patient player gets lucky staggers.</li>
 *   <li><b>Echo set</b> - 45% stagger per hit on a short internal cooldown.</li>
 *   <li><b>Starlight</b> - inherits the echo stagger against wardens and additionally rolls a
 *       smaller chance to stun any other mob (or player) it hits.</li>
 * </ul>
 */
public final class EchoWarden {
	// bedrock sword
	private static final int BEDROCK_GUARANTEE_TICKS = 200;
	private static final int BEDROCK_GUARANTEE_STUN = 40;
	private static final float BEDROCK_LUCK_CHANCE = 0.15F;
	private static final int BEDROCK_LUCK_STUN = 20;

	// echo / starlight against a warden
	private static final float WARDEN_STUN_CHANCE = 0.45F;
	private static final int WARDEN_STUN_TICKS = 40;
	private static final int WARDEN_STUN_COOLDOWN = 45;

	// starlight against everything else
	private static final float GENERAL_STUN_CHANCE = 0.25F;
	private static final int GENERAL_STUN_TICKS = 20;
	private static final int GENERAL_STUN_COOLDOWN = 30;

	private static final float ECHO_REDUCTION = 0.25F;
	private static final float STARLIGHT_REDUCTION = 0.35F;
	private static final float ECHO_CORE_CHANCE = 0.25F;

	/** players whose cut down hit is being applied, so we never re-enter our own hurt call */
	private static final Set<UUID> REDUCING = new HashSet<>();
	/** next game time each player's tier-specific stun is ready */
	private static final Map<String, Long> STUN_READY = new HashMap<>();
	/** next game time the bedrock sword's guaranteed stagger is back */
	private static final Map<UUID, Long> BEDROCK_READY = new HashMap<>();

	private EchoWarden() {
	}

	public static void init() {
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (!(source.getEntity() instanceof ServerPlayer player)
				|| !(entity instanceof LivingEntity target)
				|| !(entity.level() instanceof ServerLevel level)) {
				return true;
			}

			ItemStack held = player.getMainHandItem();
			boolean warden = entity.getType() == EntityTypes.WARDEN;

			if (held.is(ModItems.BEDROCK_SWORD)) {
				if (warden && entity instanceof Mob mob) {
					bedrockStagger(player, mob, level);
				}
				return true;
			}

			if (held.is(ModItems.STARLIGHT_SWORD)) {
				if (warden && entity instanceof Mob mob) {
					playImpact(level, mob, amount);
					if (player.getRandom().nextFloat() < WARDEN_STUN_CHANCE
						&& stunReady(player, "warden", level)) {
						markStun(player, "warden", WARDEN_STUN_COOLDOWN, level);
						stagger(level, mob, WARDEN_STUN_TICKS, false);
					}
				} else if (target.isAlive() && stunReady(player, "general", level)
					&& player.getRandom().nextFloat() < GENERAL_STUN_CHANCE) {
					markStun(player, "general", GENERAL_STUN_COOLDOWN, level);
					stunLiving(level, player, target, GENERAL_STUN_TICKS);
				}
				return true;
			}

			boolean echoWeapon = held.is(ModItems.ECHO_SWORD)
				|| held.is(ModItems.ECHO_AXE)
				|| held.is(ModItems.ECHO_SPEAR)
				|| hasFullEcho(player)
				|| hasFullStarlight(player);

			if (echoWeapon && warden && entity instanceof Mob mob) {
				playImpact(level, mob, amount);

				if (player.getRandom().nextFloat() < WARDEN_STUN_CHANCE
					&& stunReady(player, "warden", level)) {
					markStun(player, "warden", WARDEN_STUN_COOLDOWN, level);
					stagger(level, mob, WARDEN_STUN_TICKS, false);
				}
			}

			return true;
		});

		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity.getType() != EntityTypes.WARDEN || !(entity.level() instanceof ServerLevel level)) {
				return;
			}

			// rare, one at a time: the material the starlight tier is built from
			if (level.getRandom().nextFloat() < ECHO_CORE_CHANCE) {
				entity.spawnAtLocation(level, new ItemStack(ModItems.ECHO_CORE), 1.0F);
			}

			level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
				SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
		});

		// the sets blunt whatever the warden lands itself (sonic boom stays a full cancel)
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (!(entity instanceof ServerPlayer player) || REDUCING.contains(player.getUUID())) {
				return true;
			}

			if (source.is(DamageTypes.SONIC_BOOM)
				|| !isWarden(source.getDirectEntity()) && !isWarden(source.getEntity())) {
				return true;
			}

			float reduction = hasFullStarlight(player) ? STARLIGHT_REDUCTION
				: hasFullEcho(player) ? ECHO_REDUCTION : 0.0F;

			if (reduction <= 0.0F || !(player.level() instanceof ServerLevel level)) {
				return true;
			}

			REDUCING.add(player.getUUID());

			try {
				player.hurtServer(level, source, amount * (1.0F - reduction));
			} finally {
				REDUCING.remove(player.getUUID());
			}

			return false;
		});
	}

	/** Guaranteed stagger on a ten second cycle, lucky staggers in between. */
	private static void bedrockStagger(ServerPlayer player, Mob warden, ServerLevel level) {
		long now = level.getGameTime();
		long ready = BEDROCK_READY.getOrDefault(player.getUUID(), 0L);

		if (now >= ready) {
			BEDROCK_READY.put(player.getUUID(), now + BEDROCK_GUARANTEE_TICKS);
			stagger(level, warden, BEDROCK_GUARANTEE_STUN, true);
			return;
		}

		if (player.getRandom().nextFloat() < BEDROCK_LUCK_CHANCE) {
			stagger(level, warden, BEDROCK_LUCK_STUN, true);
		}
	}

	private static void playImpact(ServerLevel level, Mob warden, float amount) {
		double x = warden.getX();
		double y = warden.getY();
		double z = warden.getZ();
		boolean heavy = amount >= 8.0F || level.getRandom().nextFloat() < 0.3F;

		SoundEvent sound;
		float pitch;

		if (heavy) {
			sound = SoundEvents.MACE_SMASH_GROUND_HEAVY;
			pitch = 1.0F;
		} else {
			int pick = level.getRandom().nextInt(3);
			if (pick == 0) {
				sound = SoundEvents.MACE_SMASH_GROUND;
				pitch = 1.1F;
			} else if (pick == 1) {
				sound = SoundEvents.MACE_SMASH_AIR;
				pitch = 1.25F;
			} else {
				sound = SoundEvents.MACE_SMASH_GROUND_HEAVY;
				pitch = 1.55F;
			}
		}

		level.playSound(null, x, y, z, sound, SoundSource.PLAYERS, 1.4F, pitch);
	}

	private static boolean stunReady(ServerPlayer player, String tier, ServerLevel level) {
		Long ready = STUN_READY.get(player.getUUID() + "|" + tier);
		return ready == null || level.getGameTime() >= ready;
	}

	private static void markStun(ServerPlayer player, String tier, int cooldown, ServerLevel level) {
		STUN_READY.put(player.getUUID() + "|" + tier, level.getGameTime() + cooldown);
	}

	private static void stagger(ServerLevel level, Mob warden, int ticks, boolean bedrock) {
		// a real interrupt: drop the target so it stops chasing, then root it in place
		warden.setTarget(null);
		warden.getNavigation().stop();
		warden.setDeltaMovement(0.0, warden.getDeltaMovement().y, 0.0);
		warden.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, 6, false, false, true));
		warden.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 2, false, false, true));

		// the pop is the reward sound; the bedrock version only gets anvils
		if (!bedrock) {
			level.playSound(null, warden.getX(), warden.getY(), warden.getZ(),
				SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.2F, 1.35F);
		}

		showQuake(level, warden, bedrock);
	}

	/** Starlight can also wrench ordinary mobs and players out of their swing. */
	private static void stunLiving(ServerLevel level, ServerPlayer player, LivingEntity target, int ticks) {
		target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, 6, false, false, true));
		target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 2, false, false, true));

		if (target instanceof Player) {
			target.addEffect(new MobEffectInstance(MobEffects.NAUSEA, ticks * 4, 0, false, false, true));
		}

		if (target instanceof Mob mob) {
			mob.getNavigation().stop();
		}

		target.setDeltaMovement(0.0, target.getDeltaMovement().y, 0.0);

		double x = target.getX();
		double y = target.getY();
		double z = target.getZ();

		level.playSound(null, x, y, z, SoundEvents.MACE_SMASH_GROUND, SoundSource.PLAYERS, 1.0F, 1.2F);

		for (int i = 0; i < 10; i++) {
			double angle = i * (Math.PI * 2.0 / 10.0);
			level.sendParticles(ParticleTypes.END_ROD,
				x + Math.cos(angle) * 0.7, y + 1.6, z + Math.sin(angle) * 0.7, 1, 0.0, 0.0, 0.0, 0.0);
		}
	}

	/** A hammer slam plus a ring of dust, so the stagger reads without any text. */
	private static void showQuake(ServerLevel level, Mob warden, boolean bedrock) {
		double x = warden.getX();
		double y = warden.getY();
		double z = warden.getZ();

		if (bedrock) {
			level.playSound(null, x, y, z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.9F, 0.65F);
			level.playSound(null, x, y, z, SoundEvents.ANVIL_HIT, SoundSource.PLAYERS, 0.7F, 1.4F);
		} else {
			level.playSound(null, x, y, z, SoundEvents.MACE_SMASH_GROUND_HEAVY,
				SoundSource.PLAYERS, 1.8F, 1.0F);
			level.sendParticles(ParticleTypes.SONIC_BOOM, x, y + 1.4, z, 1, 0.0, 0.0, 0.0, 0.0);
			level.sendParticles(ParticleTypes.EXPLOSION, x, y + 1.2, z, 1, 0.0, 0.0, 0.0, 0.0);
		}

		for (int i = 0; i < 28; i++) {
			double angle = i * (Math.PI * 2.0 / 28.0);
			level.sendParticles(ParticleTypes.CLOUD,
				x + Math.cos(angle) * 1.9, y + 0.15, z + Math.sin(angle) * 1.9,
				1, Math.cos(angle) * 0.25, 0.05, Math.sin(angle) * 0.25, 0.02);
		}

		if (!bedrock) {
			for (int i = 0; i < 14; i++) {
				double ox = (level.getRandom().nextDouble() - 0.5) * 1.2;
				double oz = (level.getRandom().nextDouble() - 0.5) * 1.2;
				level.sendParticles(ParticleTypes.SCULK_SOUL,
					x + ox, y + level.getRandom().nextDouble() * 2.2, z + oz,
					1, 0.0, 0.04, 0.0, 0.01);
			}
		}

		int sparks = bedrock ? 8 : 16;
		for (int i = 0; i < sparks; i++) {
			double angle = i * (Math.PI * 2.0 / sparks);
			level.sendParticles(ParticleTypes.CRIT,
				x + Math.cos(angle) * 0.75, y + 2.5, z + Math.sin(angle) * 0.75,
				1, 0.0, 0.0, 0.0, 0.0);
		}
	}

	private static boolean isWarden(Entity entity) {
		return entity != null && entity.getType() == EntityTypes.WARDEN;
	}

	public static boolean hasFullEcho(LivingEntity player) {
		return player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.ECHO_HELMET)
			&& player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.ECHO_CHESTPLATE)
			&& player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.ECHO_LEGGINGS)
			&& player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.ECHO_BOOTS);
	}

	public static boolean hasFullStarlight(LivingEntity player) {
		return player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.STARLIGHT_HELMET)
			&& player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.STARLIGHT_CHESTPLATE)
			&& player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.STARLIGHT_LEGGINGS)
			&& player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.STARLIGHT_BOOTS);
	}
}
