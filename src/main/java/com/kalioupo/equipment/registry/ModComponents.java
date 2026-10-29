package com.kalioupo.equipment.registry;

import com.kalioupo.equipment.KalioupoEquipment;
import com.kalioupo.equipment.skill.SocketedItem;
import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class ModComponents {
	/** 0 - 100 charge of the amethyst sword, stored on the stack so it survives inventory moves */
	public static final DataComponentType<Integer> AMETHYST_CHARGE = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		KalioupoEquipment.id("amethyst_charge"),
		DataComponentType.<Integer>builder()
			.persistent(Codec.INT)
			.networkSynchronized(ByteBufCodecs.VAR_INT)
			.build()
	);

	/** 0 - 3 firework charges of the phantom boots */
	public static final DataComponentType<Integer> PHANTOM_CHARGES = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		KalioupoEquipment.id("phantom_charges"),
		DataComponentType.<Integer>builder()
			.persistent(Codec.INT)
			.networkSynchronized(ByteBufCodecs.VAR_INT)
			.build()
	);

	/** which skill the starlight sword will fire: 0 = sonic boom, 1 = nether star blink */
	public static final DataComponentType<Integer> STARLIGHT_MODE = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		KalioupoEquipment.id("starlight_mode"),
		DataComponentType.<Integer>builder()
			.persistent(Codec.INT)
			.networkSynchronized(ByteBufCodecs.VAR_INT)
			.build()
	);

	/**
	 * The socketed items of a piece of gear.
	 *
	 * <p>A small id + wear record per socket, never a nested {@code ItemStack}: nesting stacks
	 * nests their components too (enchantments need registry lookups), and that was enough to make
	 * a save fail to load. The codec also accepts the older plain-string ids.</p>
	 */
	public static final DataComponentType<List<SocketedItem>> SOCKETS = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		KalioupoEquipment.id("sockets"),
		DataComponentType.<List<SocketedItem>>builder()
			.persistent(SocketedItem.LENIENT.listOf())
			.networkSynchronized(ByteBufCodecs.fromCodec(SocketedItem.LENIENT.listOf()))
			.build()
	);

	private ModComponents() {
	}

	public static void init() {
	}
}
