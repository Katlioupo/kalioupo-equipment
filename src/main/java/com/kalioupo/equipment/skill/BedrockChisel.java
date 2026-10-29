package com.kalioupo.equipment.skill;

import com.kalioupo.equipment.registry.ModItems;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Bedrock shards are chiselled out of bedrock with a right click instead of being mined,
 * because bedrock is unbreakable and the server can never see "is mining" state.
 * The bedrock block itself is never removed.
 */
public final class BedrockChisel {
	/** right clicks needed with a bare pickaxe */
	private static final int REQUIRED_HITS = 30;
	private static final int MAX_DISTANCE_SQ = 25;

	private static final Map<UUID, Integer> PROGRESS = new HashMap<>();

	private BedrockChisel() {
	}

	public static void init() {
		UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
			if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
				return InteractionResult.PASS;
			}

			if (hand != InteractionHand.MAIN_HAND) {
				return InteractionResult.PASS;
			}

			BlockPos pos = hitResult.getBlockPos();

			if (!serverLevel.getBlockState(pos).is(Blocks.BEDROCK)) {
				return InteractionResult.PASS;
			}

			ItemStack tool = serverPlayer.getMainHandItem();

			// a diamond pickaxe (or better) is the vanilla gateway to obsidian, reuse that
			if (!tool.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState())) {
				return InteractionResult.PASS;
			}

			if (serverPlayer.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > MAX_DISTANCE_SQ) {
				return InteractionResult.PASS;
			}

			UUID id = serverPlayer.getUUID();
			int progress = PROGRESS.merge(id, 1, Integer::sum);

			serverLevel.sendParticles(ParticleTypes.CRIT,
				pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 6, 0.3, 0.2, 0.3, 0.02);
			serverLevel.playSound(null, pos, SoundEvents.STONE_HIT, SoundSource.BLOCKS, 0.6F, 0.6F);

			if (progress >= REQUIRED_HITS) {
				PROGRESS.remove(id);
				int count = 1;
				ItemStack shards = new ItemStack(ModItems.BEDROCK_SHARD, count);
				net.minecraft.world.entity.item.ItemEntity drop =
					new net.minecraft.world.entity.item.ItemEntity(serverLevel,
						pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, shards);
				drop.setDefaultPickUpDelay();
				serverLevel.addFreshEntity(drop);
				serverLevel.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.BLOCKS, 1.0F, 0.7F);
			}

			serverPlayer.swingAndResetAttackStrength(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
			return InteractionResult.SUCCESS;
		});
	}
}
