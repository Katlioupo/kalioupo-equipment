package com.kalioupo.equipment.network;

import com.kalioupo.equipment.KalioupoEquipment;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** client -&gt; server : the player pressed the skill switch key */
public record SwitchSkillPayload() implements CustomPacketPayload {
	public static final SwitchSkillPayload INSTANCE = new SwitchSkillPayload();

	public static final CustomPacketPayload.Type<SwitchSkillPayload> TYPE =
		new CustomPacketPayload.Type<>(KalioupoEquipment.id("switch_skill"));

	public static final StreamCodec<RegistryFriendlyByteBuf, SwitchSkillPayload> CODEC =
		StreamCodec.unit(INSTANCE);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
