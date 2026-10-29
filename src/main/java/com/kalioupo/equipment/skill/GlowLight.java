package com.kalioupo.equipment.skill;

import com.kalioupo.equipment.registry.ModItems;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Dynamic light for the glowstone shields.
 *
 * <p>Java has no "shining item", so the light is a real {@code minecraft:light} block that is
 * moved to follow the wearer - the same trick the Bedrock edition mod uses. Only one block per
 * player is ever placed, it is removed as soon as the shield is put away, the player logs out or
 * the block gets washed away, and it is only ever placed into plain, dry air.</p>
 *
 * <p><b>Never into a fluid.</b> A waterlogged light block holds a water <i>source</i>, not flowing
 * water. Placing one inside a stream turns that position into a spring, and two springs next to
 * each other promote the flowing water between them into more springs, so walking along a river
 * with the shield out made the river grow. Fluids are skipped entirely now; a swimmer gets the
 * light in the first air block above their head instead.</p>
 */
public final class GlowLight {
	private static final int INTERVAL = 10;
	private static final int LIGHT_LEVEL = 15;
	/** how far up the search climbs when the player is standing in water */
	private static final int MAX_RISE = 4;

	/** player -> the light block we placed for them, and the dimension it is in */
	private static final Map<UUID, Placed> PLACED = new HashMap<>();

	private record Placed(ResourceKey<Level> dimension, BlockPos pos) {
	}

	private GlowLight() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(GlowLight::tickServer);
	}

	private static void tickServer(MinecraftServer server) {
		if (server.getTickCount() % INTERVAL != 0) {
			return;
		}

		cleanupOfflinePlayers(server);

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (!(player.level() instanceof ServerLevel level)) {
				continue;
			}

			Placed previous = PLACED.get(player.getUUID());

			if (!holdsGlowShield(player)) {
				clear(server, previous);
				PLACED.remove(player.getUUID());
				continue;
			}

			BlockPos target = findSpot(level, player.blockPosition());

			if (target == null) {
				clear(server, previous);
				PLACED.remove(player.getUUID());
				continue;
			}

			// re-place when the block is gone as well: flowing water can wash a light block away
			// (it is replaceable), and without this the light stayed missing until we moved
			if (previous != null && previous.dimension().equals(level.dimension())
				&& target.equals(previous.pos())
				&& level.getBlockState(target).is(Blocks.LIGHT)) {
				continue;
			}

			clear(server, previous);

			if (place(level, target)) {
				PLACED.put(player.getUUID(), new Placed(level.dimension(), target));
			} else {
				PLACED.remove(player.getUUID());
			}
		}
	}

	/**
	 * Where the light should go: the player's own space if it is plain air, otherwise the first air
	 * block above it (a swimmer gets the light just above the surface).
	 */
	private static BlockPos findSpot(ServerLevel level, BlockPos origin) {
		for (int dy = 0; dy <= MAX_RISE; dy++) {
			BlockPos pos = origin.above(dy);

			if (isPlainAir(level, pos)) {
				return pos;
			}
		}

		return null;
	}

	private static boolean isPlainAir(ServerLevel level, BlockPos pos) {
		// Plain air only. "Replaceable" is not enough: snow layers, grass, flowers, carpets and
		// rails are all replaceable, and dropping a light block into one of those deletes it
		// silently. Standing on a thin snow layer puts the player's feet inside the snow block
		// itself, which is why walking around used to clear every snow layer underfoot.
		return level.getBlockState(pos).isAir();
	}

	private static boolean place(ServerLevel level, BlockPos pos) {
		// re-check: someone else may have filled the spot since the search
		if (!isPlainAir(level, pos)) {
			return false;
		}

		level.setBlockAndUpdate(pos,
			Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, LIGHT_LEVEL));
		return true;
	}

	private static void clear(MinecraftServer server, Placed placed) {
		if (placed == null) {
			return;
		}

		ServerLevel level = server.getLevel(placed.dimension());

		if (level != null && level.getBlockState(placed.pos()).is(Blocks.LIGHT)) {
			level.removeBlock(placed.pos(), false);
		}
	}

	/** A player who logs out mid session must not leave a light block behind. */
	private static void cleanupOfflinePlayers(MinecraftServer server) {
		if (PLACED.isEmpty()) {
			return;
		}

		PLACED.entrySet().removeIf(entry -> {
			if (server.getPlayerList().getPlayer(entry.getKey()) != null) {
				return false;
			}

			clear(server, entry.getValue());
			return true;
		});
	}

	private static boolean holdsGlowShield(ServerPlayer player) {
		return isGlow(player.getMainHandItem()) || isGlow(player.getOffhandItem());
	}

	private static boolean isGlow(ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}

		Item item = stack.getItem();
		return item == ModItems.CARBON_STEEL_SHIELD_GLOW
			|| item == ModItems.OBSIDIAN_SHIELD_GLOW
			|| item == ModItems.BEDROCK_SHIELD_GLOW;
	}
}
