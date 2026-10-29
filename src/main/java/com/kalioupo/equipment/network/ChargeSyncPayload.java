package com.kalioupo.equipment.network;

import com.kalioupo.equipment.KalioupoEquipment;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** server -&gt; client : current amethyst charge, -1 when the sword is not held */
public record ChargeSyncPayload(int charge) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ChargeSyncPayload> TYPE =
		new CustomPacketPayload.Type<>(KalioupoEquipment.id("charge_sync"));

	public static final StreamCodec<RegistryFriendlyByteBuf, ChargeSyncPayload> CODEC =
		StreamCodec.of(
			(buf, payload) -> buf.writeVarInt(payload.charge()),
			buf -> new ChargeSyncPayload(buf.readVarInt())
		);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
