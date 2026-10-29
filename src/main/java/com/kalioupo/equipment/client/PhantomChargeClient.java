package com.kalioupo.equipment.client;

import com.kalioupo.equipment.KalioupoEquipment;
import com.kalioupo.equipment.network.PhantomChargePayload;
import com.kalioupo.equipment.registry.ModItems;
import com.kalioupo.equipment.skill.PhantomChargeSkill;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.EquipmentSlot;

/** Shows the three phantom boot firework charges above the hotbar. */
public final class PhantomChargeClient {
	private static final int CELL = 12;
	private static final int GAP = 3;

	private static int charges = -1;

	private PhantomChargeClient() {
	}

	public static void init() {
		ClientPlayNetworking.registerGlobalReceiver(PhantomChargePayload.TYPE, (payload, context) ->
			context.client().execute(() -> charges = payload.charges()));

		HudElementRegistry.addLast(KalioupoEquipment.id("phantom_charges"), PhantomChargeClient::render);
	}

	private static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
		Minecraft client = Minecraft.getInstance();

		if (client.player == null || charges < 0) {
			return;
		}

		if (!client.player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.PHANTOM_BOOTS)) {
			return;
		}

		int total = PhantomChargeSkill.MAX_CHARGES * CELL + (PhantomChargeSkill.MAX_CHARGES - 1) * GAP;
		int x = graphics.guiWidth() / 2 - total / 2;
		// stack above the skill bars: a starlight sword and the phantom boots can be worn at once
		int y = SkillBar.stackTop(client, graphics.guiHeight(), CELL);
		boolean full = charges >= PhantomChargeSkill.MAX_CHARGES;

		for (int i = 0; i < PhantomChargeSkill.MAX_CHARGES; i++) {
			int cellX = x + i * (CELL + GAP);
			boolean filled = i < charges;

			graphics.fill(cellX - 1, y - 1, cellX + CELL + 1, y + CELL + 1, 0xCC140B22);
			graphics.fill(cellX, y, cellX + CELL, y + CELL, filled ? 0xFFD9C7FF : 0xFF2B1745);

			if (filled) {
				// little spark inside a charged cell
				graphics.fill(cellX + 3, y + 3, cellX + CELL - 3, y + CELL - 3, 0xFFFFFFFF);
			}
		}

		if (full) {
			int right = x + total;
			graphics.fill(x - 2, y - 2, right + 2, y - 1, 0xFFFFFFFF);
			graphics.fill(x - 2, y + CELL + 1, right + 2, y + CELL + 2, 0xFFFFFFFF);
			graphics.fill(x - 2, y - 2, x - 1, y + CELL + 2, 0xFFFFFFFF);
			graphics.fill(right + 1, y - 2, right + 2, y + CELL + 2, 0xFFFFFFFF);
			// the extra "ready" spark that only appears when everything is charged
			int sparkX = right + 5;
			graphics.fill(sparkX + 2, y + 3, sparkX + 3, y + CELL - 3, 0xFFFFFFFF);
			graphics.fill(sparkX, y + 6, sparkX + 5, y + 7, 0xFFFFFFFF);
		}
	}
}
