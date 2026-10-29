package com.kalioupo.equipment.client;

import com.kalioupo.equipment.menu.SmelterMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class SmelterScreen extends AbstractContainerScreen<SmelterMenu> {
	private static final Identifier TEXTURE =
		Identifier.fromNamespaceAndPath("kalioupo_equipment", "textures/gui/smelter.png");
	private static final Identifier BURN_PROGRESS =
		Identifier.withDefaultNamespace("textures/gui/sprites/container/furnace/burn_progress.png");
	private static final Identifier LIT_PROGRESS =
		Identifier.withDefaultNamespace("textures/gui/sprites/container/furnace/lit_progress.png");

	public SmelterScreen(SmelterMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 176, 166);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos, this.topPos,
			0.0F, 0.0F, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);

		// flame shrinks from the top as the fuel burns down (vanilla behaviour)
		int burn = this.menu.getBurnTime();
		int burnTotal = Math.max(1, this.menu.getBurnDuration());

		if (burn > 0) {
			int height = Math.max(1, 14 * burn / burnTotal);
			graphics.blit(RenderPipelines.GUI_TEXTURED, LIT_PROGRESS,
				this.leftPos + 44, this.topPos + 35 + (14 - height),
				0.0F, (float) (14 - height), 14, height, 14, 14);
		}

		// progress fills the vanilla arrow from its base to the tip
		int progress = this.menu.getProgress();
		int total = Math.max(1, this.menu.getTotalTime());

		if (progress > 0) {
			int width = Math.max(1, 24 * progress / total);
			graphics.blit(RenderPipelines.GUI_TEXTURED, BURN_PROGRESS,
				this.leftPos + 66, this.topPos + 30,
				0.0F, 0.0F, width, 16, 24, 16);
		}
	}
}
