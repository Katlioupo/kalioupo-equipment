package com.kalioupo.equipment.client;

import com.kalioupo.equipment.KalioupoEquipment;
import com.kalioupo.equipment.network.CastLightningPayload;
import com.kalioupo.equipment.network.ChargeSyncPayload;
import com.kalioupo.equipment.registry.ModItems;
import com.kalioupo.equipment.skill.AmethystSkill;
import com.kalioupo.equipment.skill.Sockets;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;

/** Client side of the amethyst sword: skill key, charge HUD and the fully charged hand sparks. */
public final class AmethystClient {
	public static final KeyMapping.Category CATEGORY =
		KeyMapping.Category.register(KalioupoEquipment.id("equipment"));

	public static final KeyMapping SKILL_KEY = new KeyMapping(
		"key.kalioupo_equipment.skill",
		InputConstants.Type.KEYBOARD,
		InputConstants.KEY_V,
		CATEGORY
	);

	private static int charge = -1;

	private AmethystClient() {
	}

	public static void init() {
		KeyMappingHelper.registerKeyMapping(SKILL_KEY);

		ClientPlayNetworking.registerGlobalReceiver(ChargeSyncPayload.TYPE, (payload, context) ->
			context.client().execute(() -> charge = payload.charge()));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (SKILL_KEY.consumeClick()) {
				ClientPlayNetworking.send(CastLightningPayload.INSTANCE);
			}
			spawnHandSparks(client);
			spawnStarlightSparkle(client);
		});

		HudElementRegistry.addLast(KalioupoEquipment.id("amethyst_charge"), AmethystClient::renderCharge);
	}

	public static int getCharge() {
		return charge;
	}

	private static boolean holdingAmethyst(Minecraft client) {
		return client.player != null
			&& Sockets.effective(client.player.getMainHandItem()) == Sockets.Skill.AMETHYST;
	}

	private static boolean holdingEcho(Minecraft client) {
		return client.player != null
			&& Sockets.effective(client.player.getMainHandItem()) == Sockets.Skill.ECHO;
	}

	private static boolean holdingStarlight(Minecraft client) {
		return client.player != null
			&& Sockets.effective(client.player.getMainHandItem()) == Sockets.Skill.STARLIGHT;
	}

	private static boolean holdingChargeable(Minecraft client) {
		return holdingAmethyst(client) || holdingEcho(client) || holdingStarlight(client);
	}

	/** True while the charge bar is on screen, so other hud elements can stack above it. */
	public static boolean chargeBarVisible(Minecraft client) {
		return client.player != null && holdingChargeable(client) && charge >= 0;
	}

	private static void renderCharge(GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker delta) {
		Minecraft client = Minecraft.getInstance();

		if (!chargeBarVisible(client)) {
			return;
		}

		boolean echo = holdingEcho(client);
		boolean starlight = holdingStarlight(client);
		// starlight shows its current mode through the bar colour
		int mode = starlight
			? client.player.getMainHandItem()
				.getOrDefault(com.kalioupo.equipment.registry.ModComponents.STARLIGHT_MODE, 0)
			: -1;

		int backdrop;
		int track;
		int tintDim;
		int tintFull;

		if (starlight) {
			// gold = boom, ice = blink, storm violet = thunder, warm green = blessing
			switch (mode) {
				case 1 -> {
					backdrop = 0xCC0E2430; track = 0xFF12384A; tintDim = 0xFF69C8E8; tintFull = 0xFFD6FBFF;
				}
				case 2 -> {
					backdrop = 0xCC1A1030; track = 0xFF2A1A4A; tintDim = 0xFF9A7BFF; tintFull = 0xFFD9C8FF;
				}
				case 3 -> {
					backdrop = 0xCC102A18; track = 0xFF1C4028; tintDim = 0xFF7BD68C; tintFull = 0xFFD7FFE0;
				}
				default -> {
					backdrop = 0xCC3A2E10; track = 0xFF4A3A14; tintDim = 0xFFE0BC55; tintFull = 0xFFFFF4C4;
				}
			}
		} else if (echo) {
			backdrop = 0xCC07161C; track = 0xFF0B2C34; tintDim = 0xFF3BB6C4; tintFull = 0xFFB4FCFF;
		} else {
			backdrop = 0xCC140B22; track = 0xFF2B1745; tintDim = 0xFF9B5DE5; tintFull = 0xFFE6A8FF;
		}

		boolean full = charge >= AmethystSkill.MAX_CHARGE;

		// which skill this bar belongs to, so four modes are not guesswork
		net.minecraft.network.chat.Component label;

		if (starlight) {
			label = net.minecraft.network.chat.Component.translatable(
				"skill.kalioupo_equipment.starlight." + mode);
		} else if (echo) {
			label = net.minecraft.network.chat.Component.translatable("skill.kalioupo_equipment.echo");
		} else {
			label = net.minecraft.network.chat.Component.translatable("skill.kalioupo_equipment.amethyst");
		}

		SkillBar.draw(graphics, SkillBar.top(graphics.guiHeight(), 0),
			charge / (float) AmethystSkill.MAX_CHARGE, backdrop, track, tintDim, tintFull, label, full);
	}

	private static void spawnHandSparks(Minecraft client) {
		if (charge < AmethystSkill.MAX_CHARGE || !holdingChargeable(client) || client.level == null) {
			return;
		}

		if (client.player.tickCount % 2 != 0) {
			return;
		}

		var spark = holdingEcho(client) ? ParticleTypes.SCULK_SOUL : ParticleTypes.ELECTRIC_SPARK;

		Vec3 look = client.player.getLookAngle();
		Vec3 side = new Vec3(-look.z, 0.0, look.x);
		side = side.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : side.normalize();

		Vec3 hand = client.player.getEyePosition()
			.add(look.scale(0.55))
			.add(side.scale(0.3))
			.add(0.0, -0.35, 0.0);

		for (int i = 0; i < 2; i++) {
			client.level.addParticle(
				spark,
				hand.x + (client.player.getRandom().nextDouble() - 0.5) * 0.4,
				hand.y + (client.player.getRandom().nextDouble() - 0.5) * 0.4,
				hand.z + (client.player.getRandom().nextDouble() - 0.5) * 0.4,
				0.0, 0.0, 0.0
			);
		}
	}

	/**
	 * The glint is invisible under some shader packs, so the blade also sheds starlight.
	 *
	 * <p>Sparse on purpose: the emitter sits just off the blade tip instead of in front of the
	 * camera, and one particle every 8 ticks is enough to read as "this thing glows" without
	 * parking a bright dot in the middle of the screen while you aim at something below you.</p>
	 */
	private static void spawnStarlightSparkle(Minecraft client) {
		if (!holdingStarlight(client) || client.level == null) {
			return;
		}

		if (client.player.tickCount % 8 != 0) {
			return;
		}

		Vec3 look = client.player.getLookAngle();
		Vec3 side = new Vec3(-look.z, 0.0, look.x);
		side = side.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : side.normalize();

		Vec3 hand = client.player.getEyePosition()
			.add(look.scale(0.95))
			.add(side.scale(0.46))
			.add(0.0, -0.46, 0.0);

		client.level.addParticle(
			ParticleTypes.END_ROD,
			hand.x + (client.player.getRandom().nextDouble() - 0.5) * 0.15,
			hand.y + (client.player.getRandom().nextDouble() - 0.5) * 0.15,
			hand.z + (client.player.getRandom().nextDouble() - 0.5) * 0.15,
			0.0, 0.01, 0.0
		);
	}
}
