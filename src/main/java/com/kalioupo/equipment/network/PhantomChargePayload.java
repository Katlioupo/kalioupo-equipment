package com.kalioupo.equipment.network;

import com.kalioupo.equipment.KalioupoEquipment;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** server -&gt; client : firework charges left on the phantom boots, -1 when not worn */
public record PhantomChargePayload(int charges) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<PhantomChargePayload> TYPE =
		new CustomPacketPayload.Type<>(KalioupoEquipment.id("phantom_charges"));

	public static final StreamCodec<RegistryFriendlyByteBuf, PhantomChargePayload> CODEC =
		StreamCodec.of(
			(buf, payload) -> buf.writeVarInt(payload.charges()),
			buf -> new PhantomChargePayload(buf.readVarInt())
		);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
