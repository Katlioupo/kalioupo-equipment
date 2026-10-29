package com.kalioupo.equipment.registry;

import com.kalioupo.equipment.KalioupoEquipment;
import com.kalioupo.equipment.block.SmelterBlock;
import com.kalioupo.equipment.block.SocketTableBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Function;

public final class ModBlocks {
	public static final Block SMELTER = register("smelter", SmelterBlock::new,
		BlockBehaviour.Properties.of()
			.strength(3.5F, 6.0F)
			.requiresCorrectToolForDrops()
			.sound(SoundType.STONE));

	public static final Item SMELTER_ITEM = registerItem("smelter", SMELTER);

	public static final Block SOCKET_TABLE = register("socket_table", SocketTableBlock::new,
		BlockBehaviour.Properties.of()
			.strength(3.0F, 6.0F)
			.requiresCorrectToolForDrops()
			.sound(SoundType.STONE));

	public static final Item SOCKET_TABLE_ITEM = registerItem("socket_table", SOCKET_TABLE);

	private ModBlocks() {
	}

	public static void init() {
	}

	private static Block register(String name, Function<BlockBehaviour.Properties, Block> factory,
								  BlockBehaviour.Properties properties) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, KalioupoEquipment.id(name));
		return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
	}

	private static Item registerItem(String name, Block block) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, KalioupoEquipment.id(name));
		Item.Properties properties = new Item.Properties().setId(key).useBlockDescriptionPrefix();
		return Registry.register(BuiltInRegistries.ITEM, key, new BlockItem(block, properties));
	}
}
