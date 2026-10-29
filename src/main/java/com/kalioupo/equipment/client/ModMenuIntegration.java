package com.kalioupo.equipment.client;

import com.kalioupo.equipment.config.ModConfig;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;

public class ModMenuIntegration implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return parent -> {
			ModConfig config = ModConfig.get();

			ConfigBuilder builder = ConfigBuilder.create()
				.setParentScreen(parent)
				.setTitle(Component.translatable("config.kalioupo_equipment.title"))
				.setSavingRunnable(ModConfig::save);

			ConfigEntryBuilder entries = builder.entryBuilder();
			ConfigCategory category = builder.getOrCreateCategory(
				Component.translatable("config.kalioupo_equipment.category"));

			category.addEntry(entries
				.startIntSlider(
					Component.translatable("config.kalioupo_equipment.capacity"),
					config.capacity,
					ModConfig.MIN_CAPACITY,
					ModConfig.MAX_CAPACITY)
				.setDefaultValue(ModConfig.DEFAULT_CAPACITY)
				.setTextGetter(value -> Component.translatable("config.kalioupo_equipment.capacity.value", value, value / 9))
				.setSaveConsumer(config::setCapacity)
				.build());

			category.addEntry(entries
				.startIntSlider(
					Component.translatable("config.kalioupo_equipment.visible_rows"),
					config.visibleRows(),
					ModConfig.MIN_VISIBLE_ROWS,
					ModConfig.MAX_VISIBLE_ROWS)
				.setDefaultValue(ModConfig.DEFAULT_VISIBLE_ROWS)
				.setTextGetter(value -> Component.translatable("config.kalioupo_equipment.visible_rows.value", value))
				.setSaveConsumer(config::setVisibleRows)
				.build());

			return builder.build();
		};
	}
}
