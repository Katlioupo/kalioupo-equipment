package com.kalioupo.equipment.block;

import com.kalioupo.equipment.KalioupoEquipment;
import com.kalioupo.equipment.skill.Sockets;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 镶嵌台 / socket table.
 *
 * <p>Deliberately has no menu and no inventory of its own - the gear in your hands is the only
 * state, so there is nothing to duplicate or lose.</p>
 */
public class SocketTableBlock extends Block {
	public SocketTableBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
											   BlockHitResult hit) {
		if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)
			|| !(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.SUCCESS;
		}

		ItemStack host = serverPlayer.getMainHandItem();
		ItemStack gem = serverPlayer.getOffhandItem();

		// shift with something in the off hand swaps the newest socket instead of stacking on top
		if (!gem.isEmpty() && !Sockets.get(host).isEmpty() && serverPlayer.isShiftKeyDown()) {
			return swap(serverLevel, pos, serverPlayer, host, gem);
		}

		if (!gem.isEmpty()) {
			Sockets.Result result = Sockets.check(serverPlayer, host, gem);

			if (result == Sockets.Result.OK) {
				ItemStack inserted = gem.copyWithCount(1);
				Sockets.insert(serverPlayer, host, gem);
				gem.shrink(1);
				serverPlayer.inventoryMenu.broadcastChanges();
				awardFirstSocket(serverLevel, serverPlayer);
				serverLevel.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.7F, 1.5F);
				serverPlayer.sendOverlayMessage(Component.translatable("message.kalioupo_equipment.socketed",
					inserted.getHoverName(), Sockets.get(host).size(), Sockets.slots(host)));
				return InteractionResult.SUCCESS;
			}

			serverPlayer.sendOverlayMessage(messageFor(result, host, serverPlayer));

			if (result != Sockets.Result.NOT_SOCKETABLE) {
				return InteractionResult.SUCCESS;
			}
		}

		if (!Sockets.get(host).isEmpty() && gem.isEmpty()) {
			return extract(serverLevel, pos, serverPlayer, host);
		}

		int limit = Sockets.slots(host);

		if (limit > 0) {
			serverPlayer.sendOverlayMessage(Component.translatable(
				"message.kalioupo_equipment.socket_status",
				Sockets.get(host).size(), limit, Sockets.totalCharge(serverPlayer), Sockets.CHARGE_CAP));
		} else {
			serverPlayer.sendOverlayMessage(Component.translatable("message.kalioupo_equipment.socket_help"));
		}

		return InteractionResult.SUCCESS;
	}

	/**
	 * Shift + material in the off hand swaps the newest socket in one click, instead of making the
	 * player pull the old one out first. If the new material does not fit, the old socket is put
	 * back untouched.
	 */
	private InteractionResult swap(ServerLevel level, BlockPos pos, ServerPlayer player,
								   ItemStack host, ItemStack gem) {
		ItemStack old = Sockets.extractLast(host);

		Sockets.Result result = Sockets.check(player, host, gem);

		if (result != Sockets.Result.OK) {
			// put it back exactly as it was
			if (!old.isEmpty()) {
				Sockets.add(host, old);
			}

			player.inventoryMenu.broadcastChanges();
			player.sendOverlayMessage(messageFor(result, host, player));
			return InteractionResult.SUCCESS;
		}

		ItemStack inserted = gem.copyWithCount(1);
		Sockets.insert(player, host, gem);
		gem.shrink(1);

		if (!old.isEmpty() && !player.getInventory().add(old)) {
			player.spawnAtLocation(level, old, 0.5F);
		}

		player.inventoryMenu.broadcastChanges();
		awardFirstSocket(level, player);
		level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.7F, 1.2F);
		player.sendOverlayMessage(Component.translatable("message.kalioupo_equipment.socket_swapped",
			old.getHoverName(), inserted.getHoverName(), Sockets.get(host).size(), Sockets.slots(host)));
		return InteractionResult.SUCCESS;
	}

	private InteractionResult extract(ServerLevel level, BlockPos pos, ServerPlayer player, ItemStack host) {
		ItemStack removed = Sockets.extractLast(host);

		if (removed.isEmpty()) {
			return InteractionResult.SUCCESS;
		}

		if (!player.getInventory().add(removed)) {
			player.spawnAtLocation(level, removed, 0.5F);
		}

		player.inventoryMenu.broadcastChanges();
		level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.7F, 1.2F);
		player.sendOverlayMessage(Component.translatable("message.kalioupo_equipment.unsocketed",
			removed.getHoverName(), Sockets.get(host).size(), Sockets.slots(host)));
		return InteractionResult.SUCCESS;
	}

	private static Component messageFor(Sockets.Result result, ItemStack host, Player player) {
		return switch (result) {
			case NO_SPACE -> Component.translatable("message.kalioupo_equipment.socket_full",
				Sockets.get(host).size(), Sockets.slots(host));
			case NESTED -> Component.translatable("message.kalioupo_equipment.socket_nested");
			case NO_TRAIT -> Component.translatable("message.kalioupo_equipment.socket_no_trait");
			case CAP_REACHED -> Component.translatable("message.kalioupo_equipment.socket_capped",
				Sockets.totalCharge(player), Sockets.CHARGE_CAP);
			case ARMOR_SOCKET_USED -> Component.translatable("message.kalioupo_equipment.socket_armor_used");
			case CANNOT_PRESERVE -> Component.translatable("message.kalioupo_equipment.socket_cannot_preserve");
			default -> Component.translatable("message.kalioupo_equipment.socket_kind");
		};
	}

	/** "First socket" progress, awarded from code because the criteria is impossible. */
	private static void awardFirstSocket(ServerLevel level, ServerPlayer player) {
		AdvancementHolder holder = level.getServer().getAdvancements().get(
			KalioupoEquipment.id("first_socket"));

		if (holder != null) {
			player.getAdvancements().award(holder, "awarded");
		}
	}
}
