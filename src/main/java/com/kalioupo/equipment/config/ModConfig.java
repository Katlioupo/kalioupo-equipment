package com.kalioupo.equipment.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.kalioupo.equipment.KalioupoEquipment;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Simple JSON config.
 *
 * <p>{@code capacity} is the number of storage slots a backpack exposes; the backing container
 * always keeps 256 slots so lowering the capacity never destroys items.
 * <p>{@code visibleRows} is how many rows the GUI shows at once (scrolling covers the rest).
 */
public final class ModConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("kalioupo_equipment.json");

	public static final int MIN_CAPACITY = 9;
	public static final int MAX_CAPACITY = 144;
	public static final int DEFAULT_CAPACITY = 54;

	public static final int MIN_VISIBLE_ROWS = 6;
	public static final int MAX_VISIBLE_ROWS = 16;
	public static final int DEFAULT_VISIBLE_ROWS = 8;

	private static ModConfig instance;

	/** Number of storage slots, always a multiple of 9 between 9 and 144. */
	public int capacity = DEFAULT_CAPACITY;

	/** Number of rows the GUI shows at once, between 6 and 10. */
	public int visibleRows = DEFAULT_VISIBLE_ROWS;

	public static ModConfig get() {
		if (instance == null) {
			load();
		}

		return instance;
	}

	public int capacityRows() {
		return clampCapacity(capacity) / 9;
	}

	public int visibleRows() {
		return clampRows(visibleRows);
	}

	public void setCapacity(int value) {
		this.capacity = clampCapacity(value);
	}

	public void setVisibleRows(int value) {
		this.visibleRows = clampRows(value);
	}

	public static int clampCapacity(int value) {
		int rounded = Math.round(value / 9.0f) * 9;
		return Math.max(MIN_CAPACITY, Math.min(MAX_CAPACITY, rounded));
	}

	public static int clampRows(int value) {
		return Math.max(MIN_VISIBLE_ROWS, Math.min(MAX_VISIBLE_ROWS, value));
	}

	public static void load() {
		ModConfig config = new ModConfig();

		if (Files.exists(PATH)) {
			try {
				String json = Files.readString(PATH, StandardCharsets.UTF_8);
				ModConfig read = GSON.fromJson(json, ModConfig.class);
				if (read != null) {
					config.capacity = clampCapacity(read.capacity);
					config.visibleRows = clampRows(read.visibleRows);
				}
			} catch (IOException | JsonSyntaxException e) {
				KalioupoEquipment.LOGGER.warn("Failed to read config, using defaults", e);
			}
		}

		instance = config;
		save();
	}

	public static void save() {
		if (instance == null) {
			return;
		}

		try {
			Files.createDirectories(PATH.getParent());
			Files.writeString(PATH, GSON.toJson(instance), StandardCharsets.UTF_8);
		} catch (IOException e) {
			KalioupoEquipment.LOGGER.warn("Failed to write config", e);
		}
	}
}
