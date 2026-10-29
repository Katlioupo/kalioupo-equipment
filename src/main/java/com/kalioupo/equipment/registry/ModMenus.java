package com.kalioupo.equipment.registry;

import com.kalioupo.equipment.KalioupoEquipment;
import com.kalioupo.equipment.menu.BackpackMenu;
import com.kalioupo.equipment.menu.SmelterMenu;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;

public final class ModMenus {
	/**
	 * Extra data sent to the client is the inventory slot the backpack lives in, so the
	 * client-side menu can build the same slot layout and lock the same slot.
	 */
	public static final ExtendedMenuType<BackpackMenu, Integer> BACKPACK = Registry.register(
		BuiltInRegistries.MENU,
		KalioupoEquipment.id("backpack"),
		new ExtendedMenuType<>(
			(syncId, inventory, slot) -> new BackpackMenu(syncId, inventory, slot),
			ByteBufCodecs.VAR_INT));

	public static final ExtendedMenuType<SmelterMenu, BlockPos> SMELTER = Registry.register(
		BuiltInRegistries.MENU,
		KalioupoEquipment.id("smelter"),
		new ExtendedMenuType<>(
			(syncId, inventory, pos) -> new SmelterMenu(syncId, inventory, pos),
			BlockPos.STREAM_CODEC));

	private ModMenus() {
	}

	public static void init() {
	}
}
