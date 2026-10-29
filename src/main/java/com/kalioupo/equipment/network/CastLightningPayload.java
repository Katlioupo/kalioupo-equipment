package com.kalioupo.equipment.network;

import com.kalioupo.equipment.KalioupoEquipment;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** client -&gt; server : the player pressed the skill key */
public record CastLightningPayload() implements CustomPacketPayload {
	public static final CastLightningPayload INSTANCE = new CastLightningPayload();

	public static final CustomPacketPayload.Type<CastLightningPayload> TYPE =
		new CustomPacketPayload.Type<>(KalioupoEquipment.id("cast_lightning"));

	public static final StreamCodec<RegistryFriendlyByteBuf, CastLightningPayload> CODEC =
		StreamCodec.unit(INSTANCE);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
