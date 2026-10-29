package com.kalioupo.equipment.skill;

import com.kalioupo.equipment.KalioupoEquipment;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Traits that need a tick or a damage hook.
 *
 * <ul>
 *   <li><b>rose gold</b> - slowly repairs the gear it sits in, one point per socket every twenty
 *       seconds. Comfort, not infinite gear: far slower than an anvil.</li>
 *   <li><b>prismarine</b> - water breathing and dolphin's grace while worn or held.</li>
 *   <li><b>phantom</b> - fall damage, -50% for the first socket and -10% for every further one.</li>
 *   <li><b>quartz</b> - +2 attack damage per socket (max +6) while held, paid for with extra
 *       durability damage on every hit.</li>
 * </ul>
 */
public final class SocketEffects {
	/**
	 * One repair tick every five seconds (100 game ticks). With 1 point per tick an ingot socket
	 * healed a point a second, which made a socketed weapon practically unbreakable in combat;
	 * five seconds is a comfort trickle instead of a second health bar.
	 */
	private static final int REPAIR_INTERVAL = 100;
	private static final int QUARTZ_INTERVAL = 10;
	private static final int EFFECT_DURATION = 70;
	/** sockets are pulled out while the host still has this many durability left */
	private static final int EJECT_THRESHOLD = 3;

	private static final EquipmentSlot[] ARMOR = {
		EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
	};

	private static final Identifier QUARTZ_MODIFIER_ID = KalioupoEquipment.id("socket_quartz_damage");

	/** players whose already-reduced fall damage is being applied, so we never re-enter ourselves */
	private static final Set<UUID> APPLYING = new HashSet<>();

	/** quartz damage currently applied per player, so the attribute is only touched when it changes */
	private static final Map<UUID, Integer> QUARTZ_APPLIED = new HashMap<>();

	private SocketEffects() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(SocketEffects::tickServer);

		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			// quartz: the blade hits harder, so it also wears out faster
			if (source.getEntity() instanceof ServerPlayer attacker
				&& source.is(DamageTypeTags.IS_PLAYER_ATTACK)) {
				int quartz = Sockets.quartzSockets(attacker.getMainHandItem());

				if (quartz > 0) {
					attacker.getMainHandItem().hurtAndBreak(quartz, attacker, EquipmentSlot.MAINHAND);
				}

				// a socketed amethyst sword also charges on hit, a bit slower than holding one.
				// the real swords do this in AmethystSwordItem, so only plain hosts are handled here
				ItemStack held = attacker.getMainHandItem();

				if (!(held.getItem() instanceof com.kalioupo.equipment.item.AmethystSwordItem)
					&& Sockets.effective(held) != Sockets.Skill.NONE) {
					AmethystSkill.onInheritedHit(held, attacker);
				}
			}

			if (!(entity instanceof ServerPlayer player) || APPLYING.contains(player.getUUID())) {
				return true;
			}

			if (!source.is(DamageTypeTags.IS_FALL)) {
				return true;
			}

			int percent = Sockets.fallPercent(player);

			if (percent <= 0) {
				return true;
			}

			float reduced = amount * (1.0F - percent / 100.0F);

			if (!(player.level() instanceof ServerLevel level)) {
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

	private static void tickServer(MinecraftServer server) {
		int tick = server.getTickCount();

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			// a socketed sword must not vanish together with the host: pull the sockets out first
			ejectIfBreaking(player);

			if (tick % QUARTZ_INTERVAL == 0) {
				updateQuartz(player);
			}

			if (tick % REPAIR_INTERVAL != 0) {
				continue;
			}

			// every item the player carries repairs itself, wherever it sits: main hand, off hand,
			// armour or anywhere in the inventory. Leaving gear in the backpack is the whole point
			// of the trait, so the old main-hand-plus-armour sweep was far too narrow.
			boolean changed = false;

			for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
				changed |= repair(player.getInventory().getItem(slot));
			}

			if (changed) {
				player.inventoryMenu.broadcastChanges();
			}

			if (Sockets.hasWaterTrait(player)) {
				player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING,
					EFFECT_DURATION, 0, true, false, true));
				player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE,
					EFFECT_DURATION, 0, true, false, true));
			}
		}
	}

	/** Keeps the held weapon's quartz bonus in sync with its sockets. */
	private static void updateQuartz(ServerPlayer player) {		int sockets = Sockets.quartzSockets(player.getMainHandItem());
		int desired = sockets * Sockets.QUARTZ_DAMAGE;
		int applied = QUARTZ_APPLIED.getOrDefault(player.getUUID(), 0);

		if (desired == applied) {
			return;
		}

		AttributeInstance attribute = player.getAttribute(Attributes.ATTACK_DAMAGE);

		if (attribute == null) {
			return;
		}

		attribute.removeModifier(QUARTZ_MODIFIER_ID);

		if (desired > 0) {
			attribute.addOrUpdateTransientModifier(new AttributeModifier(
				QUARTZ_MODIFIER_ID, desired, AttributeModifier.Operation.ADD_VALUE));
		}

		QUARTZ_APPLIED.put(player.getUUID(), desired);
	}

	/**
	 * Pulls the sockets out of anything that is about to break, and hands them back to the player.
	 * Sockets live inside the host's components, so a host that breaks would take a socketed
	 * starlight sword down with it.
	 */
	private static void ejectIfBreaking(ServerPlayer player) {
		ejectIfBreaking(player, player.getMainHandItem());
		ejectIfBreaking(player, player.getOffhandItem());

		for (EquipmentSlot slot : ARMOR) {
			ejectIfBreaking(player, player.getItemBySlot(slot));
		}
	}

	private static void ejectIfBreaking(ServerPlayer player, ItemStack stack) {
		if (stack.isEmpty() || !stack.isDamageableItem() || !(player.level() instanceof ServerLevel level)) {
			return;
		}

		if (Sockets.get(stack).isEmpty()) {
			return;
		}

		int left = stack.getMaxDamage() - stack.getDamageValue();

		if (left > EJECT_THRESHOLD) {
			return;
		}

		List<ItemStack> ejected = Sockets.ejectAll(stack);

		for (ItemStack socket : ejected) {
			if (!player.getInventory().add(socket)) {
				player.spawnAtLocation(level, socket, 0.5F);
			}
		}

		player.inventoryMenu.broadcastChanges();
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
			SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.7F, 1.2F);
		player.sendOverlayMessage(Component.translatable(
			"message.kalioupo_equipment.socket_ejected", stack.getHoverName()));
	}

	private static boolean repair(ItemStack stack) {
		if (stack.isEmpty() || !stack.isDamageableItem()) {
			return false;
		}

		int repair = Sockets.repairPoints(stack);
		int damage = stack.getDamageValue();

		if (repair <= 0 || damage <= 0) {
			return false;
		}

		stack.setDamageValue(Math.max(0, damage - repair));
		return true;
	}
}
