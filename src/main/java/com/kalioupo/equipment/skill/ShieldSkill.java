package com.kalioupo.equipment.skill;

import com.kalioupo.equipment.network.ShieldCooldownPayload;
import com.kalioupo.equipment.registry.ModItems;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Shield behaviours.
 *
 * <ul>
 *   <li><b>Shockwave</b> - the skill key while holding a shield shoves everything around you away
 *       and hits it for a little damage. Ten second cooldown, tracked by the mod and drawn on the
 *       skill bar. It deliberately does <b>not</b> use the vanilla item cooldown: that one greys
 *       the shield out and stops it from blocking, so using the skill used to disarm you.</li>
 *   <li><b>Spikes</b> - obsidian, bedrock and their glowstone versions punish whatever hits the
 *       raised shield, the way a thorned rim should.</li>
 *   <li><b>Glow</b> - the glowstone versions shed light particles while held.</li>
 * </ul>
 */
public final class ShieldSkill {
	private static final double PUSH_RADIUS = 6.0;
	public static final int COOLDOWN_TICKS = 200;

	/** remaining cooldown per player, counted down once per server tick */
	private static final Map<UUID, Integer> COOLDOWN = new HashMap<>();

	private ShieldSkill() {
	}

	public static void init() {
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (!(entity instanceof ServerPlayer player) || !player.isBlocking()) {
				return true;
			}

			ItemStack shield = player.getUseItem();
			float reflect = spikes(shield.getItem());

			if (reflect <= 0.0F || !(source.getEntity() instanceof LivingEntity attacker)
				|| attacker == player || !(player.level() instanceof ServerLevel level)) {
				return true;
			}

			attacker.hurtServer(level, level.damageSources().thorns(player), reflect);

			Vec3 push = attacker.position().subtract(player.position()).normalize().scale(0.5);
			attacker.push(push.x, 0.25, push.z);

			level.sendParticles(ParticleTypes.CRIT,
				attacker.getX(), attacker.getY() + 1.0, attacker.getZ(), 6, 0.2, 0.2, 0.2, 0.05);
			return true;
		});
	}

	/** True when the pressed skill key did something. */
	public static boolean tryCast(ServerPlayer player) {
		ItemStack shield = findShield(player);

		if (shield.isEmpty() || !(player.level() instanceof ServerLevel level)) {
			return false;
		}

		if (COOLDOWN.getOrDefault(player.getUUID(), 0) > 0) {
			return false;
		}

		float damage = shockwaveDamage(shield.getItem());
		Vec3 center = player.position();

		level.playSound(null, center.x, center.y, center.z,
			SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.PLAYERS, 1.6F, 0.9F);

		for (int i = 0; i < 32; i++) {
			double angle = i * (Math.PI * 2.0 / 32.0);
			level.sendParticles(ParticleTypes.CLOUD,
				center.x + Math.cos(angle) * 1.2, center.y + 0.2, center.z + Math.sin(angle) * 1.2,
				1, Math.cos(angle) * 0.5, 0.1, Math.sin(angle) * 0.5, 0.08);
		}

		AABB area = new AABB(center, center).inflate(PUSH_RADIUS);

		for (Entity entity : level.getEntities(player, area)) {
			if (!(entity instanceof LivingEntity target) || target == player || !target.isAlive()) {
				continue;
			}

			Vec3 away = target.position().subtract(center);

			if (away.lengthSqr() < 1.0E-4) {
				away = new Vec3(0.0, 0.0, 1.0);
			}

			Vec3 push = away.normalize().scale(1.4);
			target.push(push.x, 0.55, push.z);

			if (damage > 0.0F) {
				target.hurtServer(level, level.damageSources().playerAttack(player), damage);
			}
		}

		// our own cooldown instead of the vanilla item cooldown, so the shield keeps blocking
		COOLDOWN.put(player.getUUID(), COOLDOWN_TICKS);
		sync(player, COOLDOWN_TICKS);

		player.swingAndResetAttackStrength(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
		return true;
	}

	/** Ambient glow for the glowstone shields plus the shockwave cooldown, once per server tick. */
	public static void tickServer(net.minecraft.server.MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			tickCooldown(player);

			ItemStack shield = findShield(player);

			if (shield.isEmpty() || !isGlow(shield.getItem())
				|| !(player.level() instanceof ServerLevel level)
				|| level.getGameTime() % 8 != 0) {
				continue;
			}

			level.sendParticles(ParticleTypes.END_ROD,
				player.getX() + (level.getRandom().nextDouble() - 0.5) * 1.4,
				player.getY() + 0.6 + level.getRandom().nextDouble() * 1.2,
				player.getZ() + (level.getRandom().nextDouble() - 0.5) * 1.4,
				1, 0.0, 0.0, 0.0, 0.0);
		}
	}

	private static void tickCooldown(ServerPlayer player) {
		Integer remaining = COOLDOWN.get(player.getUUID());

		if (remaining == null) {
			return;
		}

		int next = remaining - 1;

		if (next <= 0) {
			COOLDOWN.remove(player.getUUID());
			sync(player, 0);
			return;
		}

		COOLDOWN.put(player.getUUID(), next);

		// the client animates between packets, so a correction every quarter second is plenty
		if (next % 5 == 0) {
			sync(player, next);
		}
	}

	private static void sync(ServerPlayer player, int remaining) {
		ServerPlayNetworking.send(player, new ShieldCooldownPayload(remaining, COOLDOWN_TICKS));
	}

	private static ItemStack findShield(ServerPlayer player) {
		ItemStack main = player.getMainHandItem();

		if (isShield(main.getItem())) {
			return main;
		}

		ItemStack off = player.getOffhandItem();
		return isShield(off.getItem()) ? off : ItemStack.EMPTY;
	}

	/** Also used by the client to decide whether the cooldown bar applies. */
	public static boolean isShield(Item item) {
		return item == ModItems.CARBON_STEEL_SHIELD || item == ModItems.OBSIDIAN_SHIELD
			|| item == ModItems.BEDROCK_SHIELD || item == ModItems.CARBON_STEEL_SHIELD_GLOW
			|| item == ModItems.OBSIDIAN_SHIELD_GLOW || item == ModItems.BEDROCK_SHIELD_GLOW;
	}

	private static boolean isGlow(Item item) {
		return item == ModItems.CARBON_STEEL_SHIELD_GLOW
			|| item == ModItems.OBSIDIAN_SHIELD_GLOW
			|| item == ModItems.BEDROCK_SHIELD_GLOW;
	}

	/** spikes reflect damage, plain carbon steel is smooth. */
	private static float spikes(Item item) {
		if (item == ModItems.OBSIDIAN_SHIELD || item == ModItems.OBSIDIAN_SHIELD_GLOW) {
			return 3.0F;
		}

		if (item == ModItems.BEDROCK_SHIELD || item == ModItems.BEDROCK_SHIELD_GLOW) {
			return 5.0F;
		}

		return 0.0F;
	}

	private static float shockwaveDamage(Item item) {
		if (item == ModItems.BEDROCK_SHIELD || item == ModItems.BEDROCK_SHIELD_GLOW) {
			return 4.0F;
		}

		if (item == ModItems.OBSIDIAN_SHIELD || item == ModItems.OBSIDIAN_SHIELD_GLOW) {
			return 2.0F;
		}

		return 0.0F;
	}
}
