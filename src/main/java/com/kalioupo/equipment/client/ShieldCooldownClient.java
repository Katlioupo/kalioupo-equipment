package com.kalioupo.equipment.client;

import com.kalioupo.equipment.network.ShieldCooldownPayload;
import com.kalioupo.equipment.skill.ShieldSkill;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import com.kalioupo.equipment.KalioupoEquipment;

/**
 * Shield shockwave cooldown, drawn in the same bar style as the sword skills instead of the
 * vanilla item cooldown: the vanilla one greys the shield out and, worse, refuses to block for
 * the whole cooldown, which makes the shield unusable right after using its own skill.
 */
public final class ShieldCooldownClient {
	private static final int BACKDROP = 0xCC14101C;
	private static final int TRACK = 0xFF2C2438;
	private static final int TINT = 0xFF9B8FC4;
	private static final int TINT_FULL = 0xFFEDE4FF;

	private static int remaining;
	private static int total;

	private ShieldCooldownClient() {
	}

	public static void init() {
		ClientPlayNetworking.registerGlobalReceiver(ShieldCooldownPayload.TYPE, (payload, context) ->
			context.client().execute(() -> {
				remaining = payload.remaining();
				total = payload.total();
			}));

		// the server only sends a correction every few ticks, the bar animates in between
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (remaining > 0) {
				remaining--;
			}
		});

		HudElementRegistry.addLast(KalioupoEquipment.id("shield_cooldown"), ShieldCooldownClient::render);
	}

	/** Shown while a shield of ours is in either hand and the shockwave is recharging. */
	public static boolean barVisible(Minecraft client) {
		if (remaining <= 0 || total <= 0 || client.player == null) {
			return false;
		}

		return ShieldSkill.isShield(client.player.getMainHandItem().getItem())
			|| ShieldSkill.isShield(client.player.getOffhandItem().getItem());
	}

	private static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
		Minecraft client = Minecraft.getInstance();

		if (!barVisible(client)) {
			return;
		}

		int row = AmethystClient.chargeBarVisible(client) ? 1 : 0;
		Component label = Component.translatable("skill.kalioupo_equipment.shield");

		SkillBar.draw(graphics, SkillBar.top(graphics.guiHeight(), row),
			remaining / (float) total, BACKDROP, TRACK, TINT, TINT_FULL, label, false);
	}
}
