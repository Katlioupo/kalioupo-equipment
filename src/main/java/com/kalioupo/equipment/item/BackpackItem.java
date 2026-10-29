package com.kalioupo.equipment.item;

import com.kalioupo.equipment.config.ModConfig;
import com.kalioupo.equipment.menu.BackpackMenu;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;

public class BackpackItem extends Item {
	public BackpackItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (hand != InteractionHand.MAIN_HAND) {
			return InteractionResult.PASS;
		}

		ItemStack stack = player.getItemInHand(hand);
		int slot = player.getInventory().getSelectedSlot();
		ModConfig config = ModConfig.get();
		int capacity = config.capacity;
		int rows = Math.min(config.capacityRows(), config.visibleRows());

		if (!level.isClientSide()) {
			SimpleContainer container = new SimpleContainer(BackpackMenu.BACKING_SIZE);
			ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
			if (contents != null) {
				contents.copyInto(container.getItems());
			}

			player.openMenu(new ExtendedMenuProvider<Integer>() {
				@Override
				public Component getDisplayName() {
					return Component.translatable("container.kalioupo_equipment.backpack");
				}

				@Override
				public AbstractContainerMenu createMenu(int syncId, Inventory inventory, Player p) {
					return new BackpackMenu(syncId, inventory, container, stack, p, slot, capacity, rows);
				}

				@Override
				public Integer getScreenOpeningData(ServerPlayer p) {
					return BackpackMenu.pack(slot, capacity, rows);
				}
			});
		}

		// Tell the client the held stack simply transformed, so no "equip item" animation plays.
		return InteractionResult.SUCCESS.heldItemTransformedTo(stack);
	}
}
