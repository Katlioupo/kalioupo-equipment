package com.kalioupo.equipment.network;

import com.kalioupo.equipment.KalioupoEquipment;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** server -&gt; client : shield shockwave cooldown, remaining and total in ticks */
public record ShieldCooldownPayload(int remaining, int total) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ShieldCooldownPayload> TYPE =
		new CustomPacketPayload.Type<>(KalioupoEquipment.id("shield_cooldown"));

	public static final StreamCodec<RegistryFriendlyByteBuf, ShieldCooldownPayload> CODEC =
		StreamCodec.of(
			(buf, payload) -> {
				buf.writeVarInt(payload.remaining());
				buf.writeVarInt(payload.total());
			},
			buf -> new ShieldCooldownPayload(buf.readVarInt(), buf.readVarInt())
		);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
