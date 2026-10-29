package com.kalioupo.equipment.skill;

import com.kalioupo.equipment.network.PhantomChargePayload;
import com.kalioupo.equipment.registry.ModComponents;
import com.kalioupo.equipment.registry.ModItems;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Phantom boots store three firework charges.
 *
 * <ul>
 *   <li>+1 every 10 s while standing still, +1 every 2 s while gliding</li>
 *   <li>while gliding the set also repairs itself, which offsets the vanilla glider wear</li>
 *   <li>at least one charge + the skill key while gliding = a firework style boost</li>
 * </ul>
 */
public final class PhantomChargeSkill {
	public static final int MAX_CHARGES = 3;

	private static final int GLIDE_INTERVAL = 40;
	private static final int IDLE_INTERVAL = 200;
	private static final int REPAIR_INTERVAL = 40;
	private static final double BOOST_FORWARD = 1.15;
	private static final double BOOST_UP = 0.28;

	private static final Map<UUID, Integer> LAST_SENT = new HashMap<>();

	private PhantomChargeSkill() {
	}

	public static int getCharges(ItemStack stack) {
		return stack.getOrDefault(ModComponents.PHANTOM_CHARGES, 0);
	}

	public static void addCharges(ItemStack stack, int amount) {
		stack.set(ModComponents.PHANTOM_CHARGES, Mth.clamp(getCharges(stack) + amount, 0, MAX_CHARGES));
	}

	public static void tickServer(MinecraftServer server) {
		long time = server.overworld() == null ? 0L : server.overworld().getGameTime();

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
			boolean wearing = boots.is(ModItems.PHANTOM_BOOTS);

			if (wearing) {
				if (player.isFallFlying()) {
					if (time % GLIDE_INTERVAL == 0) {
						addCharges(boots, 1);
					}
					if (time % REPAIR_INTERVAL == 0) {
						repairMostDamaged(player);
					}
				} else if (isIdle(player) && time % IDLE_INTERVAL == 0) {
					addCharges(boots, 1);
				}
			}

			int current = wearing ? getCharges(boots) : -1;
			Integer previous = LAST_SENT.get(player.getUUID());

			if (previous == null || previous != current) {
				LAST_SENT.put(player.getUUID(), current);
				ServerPlayNetworking.send(player, new PhantomChargePayload(current));
			}
		}
	}

	public static boolean tryBoost(ServerPlayer player) {
		ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);

		if (!boots.is(ModItems.PHANTOM_BOOTS) || !player.isFallFlying() || getCharges(boots) <= 0) {
			return false;
		}

		addCharges(boots, -1);

		Vec3 look = player.getLookAngle();
		player.push(look.x * BOOST_FORWARD, look.y * BOOST_FORWARD * 0.5 + BOOST_UP, look.z * BOOST_FORWARD);
		player.connection.send(new ClientboundSetEntityMotionPacket(player));
		player.level().playSound(null, player.blockPosition(), SoundEvents.FIREWORK_ROCKET_LAUNCH,
			SoundSource.PLAYERS, 1.0F, 1.2F);

		return true;
	}

	private static boolean isIdle(ServerPlayer player) {
		return player.onGround() && player.getDeltaMovement().horizontalDistanceSqr() < 1.0E-4;
	}

	/** spread the wear: mend whichever phantom piece is hurting the most */
	private static void repairMostDamaged(ServerPlayer player) {
		ItemStack worst = ItemStack.EMPTY;
		int worstDamage = 0;

		for (EquipmentSlot slot : new EquipmentSlot[]{
			EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
			ItemStack stack = player.getItemBySlot(slot);

			if (stack.isEmpty() || !stack.isDamageableItem() || !stack.isDamaged()) {
				continue;
			}

			if (stack.getDamageValue() > worstDamage) {
				worstDamage = stack.getDamageValue();
				worst = stack;
			}
		}

		if (worst != ItemStack.EMPTY && worstDamage > 0) {
			worst.setDamageValue(worstDamage - 1);
		}
	}
}
