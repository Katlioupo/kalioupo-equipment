package com.kalioupo.equipment.network;

import com.kalioupo.equipment.skill.AmethystSkill;
import com.kalioupo.equipment.skill.EchoSkill;
import com.kalioupo.equipment.skill.PhantomChargeSkill;
import com.kalioupo.equipment.skill.ShieldSkill;
import com.kalioupo.equipment.skill.Sockets;
import com.kalioupo.equipment.skill.StarlightSkill;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class ModNetworking {
	private ModNetworking() {
	}

	public static void init() {
		PayloadTypeRegistry.serverboundPlay().register(CastLightningPayload.TYPE, CastLightningPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(SwitchSkillPayload.TYPE, SwitchSkillPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(ChargeSyncPayload.TYPE, ChargeSyncPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(PhantomChargePayload.TYPE, PhantomChargePayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(ShieldCooldownPayload.TYPE, ShieldCooldownPayload.CODEC);

		// one shared skill key: the effective skill of the held weapon decides what happens. An
		// inherited skill always wins over the weapon's own, so socketing a starlight sword into
		// an echo sword really does replace the sonic boom.
		ServerPlayNetworking.registerGlobalReceiver(CastLightningPayload.TYPE, (payload, context) ->
			context.server().execute(() -> {
				boolean cast = switch (Sockets.effective(context.player().getMainHandItem())) {
					case AMETHYST -> AmethystSkill.cast(context.player());
					case ECHO -> EchoSkill.cast(context.player());
					case STARLIGHT -> StarlightSkill.cast(context.player());
					default -> false;
				};

				if (cast) {
					return;
				}

				if (ShieldSkill.tryCast(context.player())) {
					return;
				}

				PhantomChargeSkill.tryBoost(context.player());
			}));

		// the switch key only ever cycles the starlight mode
		ServerPlayNetworking.registerGlobalReceiver(SwitchSkillPayload.TYPE, (payload, context) ->
			context.server().execute(() -> StarlightSkill.cycleMode(context.player())));
	}
}
