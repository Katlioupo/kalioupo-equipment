package com.kalioupo.equipment.client;

import com.kalioupo.equipment.menu.BackpackMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * Chest-like screen with a configurable number of visible rows, a scrollbar and a capacity
 * counter. The background texture is generated per row count by {@code tools/make_gui.py}.
 */
public class BackpackScreen extends AbstractContainerScreen<BackpackMenu> {
	private static final Identifier[] TEXTURES = new Identifier[17];

	static {
		for (int rows = 6; rows <= 16; rows++) {
			TEXTURES[rows] = Identifier.fromNamespaceAndPath("kalioupo_equipment",
				"textures/gui/backpack_" + rows + ".png");
		}
	}

	public BackpackScreen(BackpackMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 176, 114 + menu.getRowCount() * 18);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (this.menu.canScroll() && isOverStorage(mouseX, mouseY)) {
			int button = scrollY > 0 ? 0 : 1;
			this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, button);
			return true;
		}

		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	private boolean isOverStorage(double mouseX, double mouseY) {
		return mouseX >= this.leftPos && mouseX <= this.leftPos + this.imageWidth
			&& mouseY >= this.topPos + 17 && mouseY <= this.topPos + 17 + this.menu.getRowCount() * 18;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		int rows = this.menu.getRowCount();
		Identifier texture = TEXTURES[Math.max(6, Math.min(16, rows))];
		int textureHeight = 17 + rows * 18 + 96;

		graphics.blit(RenderPipelines.GUI_TEXTURED, texture, this.leftPos, this.topPos,
			0.0F, 0.0F, this.imageWidth, textureHeight, this.imageWidth, textureHeight);

		if (this.menu.canScroll()) {
			int trackX = this.leftPos + this.imageWidth - 6;
			int trackY = this.topPos + 18;
			int trackHeight = rows * 18 - 2;

			graphics.fill(trackX, trackY, trackX + 4, trackY + trackHeight, 0xFF1A1A1A);

			int totalRows = this.menu.getTotalRows();
			int maxScroll = Math.max(1, totalRows - rows);
			int thumbHeight = Math.max(12, trackHeight * rows / totalRows);
			int thumbY = trackY + (trackHeight - thumbHeight) * this.menu.getScrollRow() / maxScroll;

			graphics.fill(trackX, thumbY, trackX + 4, thumbY + thumbHeight, 0xFFC6C6C6);
		}

		// Capacity counter, right-aligned on the inventory label row (inside the frame).
		String counter = this.menu.getUsedSlots() + "/" + this.menu.getCapacity();
		int textX = this.leftPos + this.imageWidth - 7 - this.font.width(counter);
		int textY = this.topPos + 17 + rows * 18 + 2;

		graphics.text(this.font, counter, textX, textY, 0xFF8B8B8B, false);
	}
}
