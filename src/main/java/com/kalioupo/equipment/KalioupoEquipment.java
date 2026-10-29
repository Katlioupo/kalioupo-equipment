package com.kalioupo.equipment;

import com.kalioupo.equipment.config.ModConfig;
import com.kalioupo.equipment.network.ModNetworking;
import com.kalioupo.equipment.registry.ModBlockEntities;
import com.kalioupo.equipment.registry.ModBlocks;
import com.kalioupo.equipment.registry.ModComponents;
import com.kalioupo.equipment.registry.ModCreativeTabs;
import com.kalioupo.equipment.registry.ModItems;
import com.kalioupo.equipment.registry.ModMenus;
import com.kalioupo.equipment.skill.AdvancementTicker;
import com.kalioupo.equipment.skill.AmethystSkill;
import com.kalioupo.equipment.skill.ArmorBuffs;
import com.kalioupo.equipment.skill.BedrockChisel;
import com.kalioupo.equipment.skill.BedrockImmunity;
import com.kalioupo.equipment.skill.BossSounds;
import com.kalioupo.equipment.skill.EchoWarden;
import com.kalioupo.equipment.skill.GlowLight;
import com.kalioupo.equipment.skill.ObsidianDamageReduction;
import com.kalioupo.equipment.skill.PhantomChargeSkill;
import com.kalioupo.equipment.skill.PhantomSetBonus;
import com.kalioupo.equipment.skill.ShieldSkill;
import com.kalioupo.equipment.skill.SocketEffects;
import com.kalioupo.equipment.skill.StarlightAura;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KalioupoEquipment implements ModInitializer {
	public static final String MOD_ID = "kalioupo_equipment";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		ModConfig.load();
		ModMenus.init();
		ModComponents.init();
		ModItems.init();
		ModBlocks.init();
		ModBlockEntities.init();
		ModCreativeTabs.init();
		ModNetworking.init();

		ServerTickEvents.END_SERVER_TICK.register(AmethystSkill::tickServer);
		ServerTickEvents.END_SERVER_TICK.register(PhantomSetBonus::tickServer);
		ServerTickEvents.END_SERVER_TICK.register(PhantomChargeSkill::tickServer);
		PhantomSetBonus.init();
		ObsidianDamageReduction.init();
		BedrockChisel.init();
		BedrockImmunity.init();
		EchoWarden.init();
		ServerTickEvents.END_SERVER_TICK.register(BedrockImmunity::tickServer);
		ServerTickEvents.END_SERVER_TICK.register(StarlightAura::tickServer);
		ArmorBuffs.init();
		ShieldSkill.init();
		SocketEffects.init();
		BossSounds.init();
		AdvancementTicker.init();
		GlowLight.init();
		ServerTickEvents.END_SERVER_TICK.register(ShieldSkill::tickServer);

		LOGGER.info("Kalioupo's Interesting Equipment loaded (backpack capacity: {} slots)", ModConfig.get().capacity);
	}
}
