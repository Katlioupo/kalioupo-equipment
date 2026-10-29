package com.kalioupo.equipment.client;

import com.kalioupo.equipment.registry.ModItems;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.Vec3;

/**
 * Phantom leggings speed up gliding. Elytra physics is simulated on the client, so the boost is
 * applied there: a gentle push along the view direction, hard capped so it can never run away.
 */
public final class GlideClient {
	private static final double BOOST = 0.012;
	private static final double VERTICAL_FACTOR = 0.25;
	private static final double MIN_SPEED = 0.05;
	private static final double MAX_SPEED = 1.15;

	private GlideClient() {
	}

	public static void init() {
		ClientTickEvents.END_CLIENT_TICK.register(GlideClient::tick);
	}

	private static void tick(Minecraft client) {
		LocalPlayer player = client.player;

		if (player == null || !player.isFallFlying()) {
			return;
		}

		if (!player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.PHANTOM_LEGGINGS)) {
			return;
		}

		Vec3 velocity = player.getDeltaMovement();
		double speed = velocity.length();

		if (speed < MIN_SPEED || speed > MAX_SPEED) {
			return;
		}

		Vec3 look = player.getLookAngle();
		player.setDeltaMovement(velocity.add(
			look.x * BOOST,
			look.y * BOOST * VERTICAL_FACTOR,
			look.z * BOOST
		));
	}
}
