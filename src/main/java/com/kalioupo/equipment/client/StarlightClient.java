package com.kalioupo.equipment.client;

import com.kalioupo.equipment.KalioupoEquipment;
import com.kalioupo.equipment.network.SwitchSkillPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;

/** B cycles the starlight sword between the sonic boom and the blink. */
public final class StarlightClient {
	public static final KeyMapping SWITCH_KEY = new KeyMapping(
		"key.kalioupo_equipment.switch_skill",
		InputConstants.Type.KEYBOARD,
		InputConstants.KEY_B,
		AmethystClient.CATEGORY
	);

	private StarlightClient() {
	}

	public static void init() {
		KeyMappingHelper.registerKeyMapping(SWITCH_KEY);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (SWITCH_KEY.consumeClick()) {
				ClientPlayNetworking.send(SwitchSkillPayload.INSTANCE);
			}
		});
	}
}
