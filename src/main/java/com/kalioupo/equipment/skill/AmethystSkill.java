package com.kalioupo.equipment.skill;

import com.kalioupo.equipment.network.ChargeSyncPayload;
import com.kalioupo.equipment.registry.ModComponents;
import com.kalioupo.equipment.registry.ModItems;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Amethyst sword charge and lightning release, all server authoritative.
 *
 * <ul>
 *   <li>+1 charge every 8 ticks (0.4 s) while the sword is held: 40 s from empty to full</li>
 *   <li>+12 charge every time the sword hits a mob: about 9 hits</li>
 *   <li>at 100 the player can press the skill key: a real vanilla lightning bolt plus 12 magic
 *       damage to everything within 4 blocks</li>
 *   <li>releasing empties the charge and starts a 5 s cooldown</li>
 * </ul>
 */
public final class AmethystSkill {
	public static final int MAX_CHARGE = 100;
	public static final int HIT_CHARGE = 12;
	/** a socketed amethyst sword charges a little slower on hit than holding the real one */
	public static final int INHERITED_HIT_CHARGE = 8;

	private static final int PASSIVE_INTERVAL = 8;
	/** how far the bolt can reach when it is aimed at the ground or a distant mob */
	private static final double RANGE = 32.0;
	/** anything this close is targeted without aiming at all */
	private static final double NEAR_LOCK = 8.0;
	/** far targets only have to be within this many blocks of the crosshair */
	private static final double AIM_ASSIST = 2.5;
	private static final double AOE = 4.0;
	private static final float BASE_DAMAGE = 15.0F;
	private static final int COOLDOWN_TICKS = 100;

	/** last value sent per player, so we only spend bandwidth when it actually changes */
	private static final Map<UUID, Integer> LAST_SENT = new HashMap<>();

	/** leftover fraction of a charge gain, so socketed copper is not rounded away */
	private static final Map<UUID, Double> CHARGE_FRACTION = new HashMap<>();

	private AmethystSkill() {
	}

	public static int getCharge(ItemStack stack) {
		return stack.getOrDefault(ModComponents.AMETHYST_CHARGE, 0);
	}

	public static void setCharge(ItemStack stack, int value) {
		stack.set(ModComponents.AMETHYST_CHARGE, Mth.clamp(value, 0, MAX_CHARGE));
	}

	public static void addCharge(ItemStack stack, int amount) {
		setCharge(stack, getCharge(stack) + amount);
	}

	public static void tickServer(MinecraftServer server) {
		long time = server.overworld() == null ? 0L : server.overworld().getGameTime();

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			ItemStack held = player.getMainHandItem();
			// the echo and starlight swords share this component, so one bar serves every skill.
			// a weapon with one of those swords socketed inherits the skill and the bar as well.
			boolean holding = Sockets.effective(held) != Sockets.Skill.NONE;

			if (holding && time % PASSIVE_INTERVAL == 0) {
				// the starlight blade charges four times faster, its skills are gated by charge alone.
				// socketed copper raises the rate further, so the fraction is carried over between
				// intervals instead of being rounded away.
				boolean starlight = Sockets.effective(held) == Sockets.Skill.STARLIGHT;
				double base = starlight ? 4.0D : 1.0D;
				int whole = takeWhole(player, base * Sockets.chargeMultiplier(player));

				if (whole > 0) {
					addCharge(held, whole);
				}
			}

			int current = holding ? getCharge(held) : -1;
			Integer previous = LAST_SENT.get(player.getUUID());

			if (previous == null || previous != current) {
				LAST_SENT.put(player.getUUID(), current);
				ServerPlayNetworking.send(player, new ChargeSyncPayload(current));
			}
		}
	}

	/**
	 * Charge added by a hit. Socketed copper speeds this up as well - the user-facing promise is
	 * "charge faster", and hitting things is how most players actually charge.
	 */
	public static void onHit(ItemStack stack, LivingEntity attacker) {
		addHitCharge(stack, attacker, HIT_CHARGE);
	}

	/** A socketed amethyst sword charges on hit too, just a bit slower than the real thing. */
	public static void onInheritedHit(ItemStack stack, LivingEntity attacker) {
		addHitCharge(stack, attacker, INHERITED_HIT_CHARGE);
	}

	private static void addHitCharge(ItemStack stack, LivingEntity attacker, int base) {
		double gain = base * Sockets.chargeMultiplier(attacker);
		int whole = attacker instanceof ServerPlayer player
			? takeWhole(player, gain)
			: (int) Math.round(gain);

		if (whole > 0) {
			addCharge(stack, whole);
		}
	}

	/** Carries the fractional part of a charge gain so +20% is not lost to rounding. */
	private static int takeWhole(ServerPlayer player, double gain) {
		double accumulated = CHARGE_FRACTION.getOrDefault(player.getUUID(), 0.0D) + gain;
		int whole = (int) Math.floor(accumulated);
		CHARGE_FRACTION.put(player.getUUID(), accumulated - whole);
		return whole;
	}

	public static boolean cast(ServerPlayer player) {
		ItemStack held = player.getMainHandItem();

		// the amethyst sword itself, or any weapon with an amethyst sword socketed
		if (Sockets.effective(held) != Sockets.Skill.AMETHYST || getCharge(held) < MAX_CHARGE) {
			return false;
		}

		if (player.getCooldowns().isOnCooldown(held)) {
			return false;
		}

		if (!(player.level() instanceof ServerLevel level)) {
			return false;
		}

		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle();
		LivingEntity target = findTarget(player, level, eye, look);

		Vec3 point;
		if (target != null) {
			point = target.position();
		} else {
			HitResult hit = player.pick(RANGE, 0.0F, false);
			point = hit.getType() == HitResult.Type.MISS ? eye.add(look.scale(RANGE)) : hit.getLocation();
		}

		LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
		if (bolt != null) {
			bolt.setPos(point.x, point.y, point.z);
			// visual only: a real bolt would set the ground (and every dropped item) on fire
			bolt.setVisualOnly(true);
			bolt.setCause(player);
			level.addFreshEntity(bolt);
		}

		level.playSound(null, point.x, point.y, point.z, SoundEvents.LIGHTNING_BOLT_THUNDER,
			SoundSource.WEATHER, 2.0F, 1.0F);

		for (int i = 0; i < 24; i++) {
			double offsetX = (level.getRandom().nextDouble() - 0.5) * 3.0;
			double offsetY = level.getRandom().nextDouble() * 2.0;
			double offsetZ = (level.getRandom().nextDouble() - 0.5) * 3.0;
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
				point.x + offsetX, point.y + offsetY, point.z + offsetZ, 1, 0.0, 0.0, 0.0, 0.05);
		}

		// player attributed magic damage: counts as the player's kill, so looting, drops,
		// advancements and xp all behave exactly like a normal sword kill
		DamageSource source = level.damageSources().indirectMagic(player, player);
		AABB area = new AABB(point.add(-AOE, -AOE, -AOE), point.add(AOE, AOE, AOE));

		for (Entity entity : level.getEntities(player, area)) {
			if (entity instanceof LivingEntity living && living != player && living.isAlive()) {
				float damage = EnchantmentHelper.modifyDamage(level, held, living, source, BASE_DAMAGE);
				living.hurtServer(level, source, damage);
			}
		}

		setCharge(held, 0);
		player.getCooldowns().addCooldown(held, COOLDOWN_TICKS);
		player.swingAndResetAttackStrength(InteractionHand.MAIN_HAND,
			net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
		return true;
	}

	/**
	 * Who the bolt lands on.
	 *
	 * <p>Two ranges, two rules:</p>
	 * <ul>
	 *   <li>within {@link #NEAR_LOCK} blocks anything hostile is picked automatically, no aiming
	 *       needed - being surrounded should not require pixel-perfect aim</li>
	 *   <li>further out it has to be under the crosshair, with {@link #AIM_ASSIST} blocks of slack,
	 *       so casting down a cliff at a mob on the ground below works</li>
	 * </ul>
	 *
	 * <p>Hostiles always win over passive mobs, players are never picked.</p>
	 */
	private static LivingEntity findTarget(ServerPlayer player, ServerLevel level, Vec3 eye, Vec3 look) {
		LivingEntity near = null;
		double nearScore = Double.MAX_VALUE;
		LivingEntity aimed = null;
		double aimedScore = Double.MAX_VALUE;

		for (Entity entity : level.getEntities(player, player.getBoundingBox().inflate(RANGE))) {
			if (!(entity instanceof LivingEntity living) || living.isSpectator() || !living.isAlive()
				|| living == player) {
				continue;
			}

			// never zap the player's own pets, and never other players
			if (living instanceof TamableAnimal pet && pet.isOwnedBy(player)) {
				continue;
			}

			if (living instanceof Player) {
				continue;
			}

			Vec3 to = living.getBoundingBox().getCenter().subtract(eye);
			double distance = to.length();

			if (distance < 0.1 || distance > RANGE) {
				continue;
			}

			// hostiles always win over neutral / passive mobs, distance only breaks ties
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

			double score = distance * 0.5 + offAxis * 2.0 + hostilePenalty;

			if (score < aimedScore) {
				aimedScore = score;
				aimed = living;
			}
		}

		// whatever the crosshair is actually on wins; the near lock only fills in when nothing is
		return aimed != null ? aimed : near;
	}
}
