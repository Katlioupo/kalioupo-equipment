package com.kalioupo.equipment.registry;

import com.kalioupo.equipment.KalioupoEquipment;
import com.kalioupo.equipment.block.SmelterBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Set;

public final class ModBlockEntities {
	public static final BlockEntityType<SmelterBlockEntity> SMELTER = Registry.register(
		BuiltInRegistries.BLOCK_ENTITY_TYPE,
		KalioupoEquipment.id("smelter"),
		new BlockEntityType<>(SmelterBlockEntity::new, Set.of(ModBlocks.SMELTER)));

	private ModBlockEntities() {
	}

	public static void init() {
	}
}
