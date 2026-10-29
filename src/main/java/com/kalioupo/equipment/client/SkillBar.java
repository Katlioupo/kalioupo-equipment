package com.kalioupo.equipment.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * Shared look and layout for every skill bar. The charge bar, the shield cooldown bar and the
 * phantom charge cells are all stacked from the same anchor, so two of them can never end up
 * drawn on top of each other.
 */
public final class SkillBar {
	public static final int WIDTH = 120;
	public static final int HEIGHT = 6;
	/** vertical pitch between two stacked bars */
	public static final int ROW = 22;

	private static final int BASE_OFFSET = 80;
	private static final int LABEL_GAP = 11;
	private static final int STACK_GAP = 3;

	private SkillBar() {
	}

	/** Top edge of the bar on the given row, measured from the bottom of the screen. */
	public static int top(int guiHeight, int row) {
		return guiHeight - BASE_OFFSET - row * ROW;
	}

	/** How many bars currently want to be on screen. */
	public static int rowsInUse(Minecraft client) {
		int rows = 0;

		if (AmethystClient.chargeBarVisible(client)) {
			rows++;
		}

		if (ShieldCooldownClient.barVisible(client)) {
			rows++;
		}

		return rows;
	}

	/** Top edge for something of {@code height} that wants to sit clear of every visible bar. */
	public static int stackTop(Minecraft client, int guiHeight, int height) {
		int rows = rowsInUse(client);

		if (rows == 0) {
			return top(guiHeight, 0);
		}

		return top(guiHeight, rows - 1) - LABEL_GAP - height - STACK_GAP;
	}

	public static void draw(GuiGraphicsExtractor graphics, int y, float fraction,
							int backdrop, int track, int tint, int tintFull,
							Component label, boolean full) {
		int x = graphics.guiWidth() / 2 - WIDTH / 2;

		graphics.fill(x - 1, y - 1, x + WIDTH + 1, y + HEIGHT + 1, backdrop);
		graphics.fill(x, y, x + WIDTH, y + HEIGHT, track);

		float clamped = Math.max(0.0F, Math.min(1.0F, fraction));
		int filled = Math.round(WIDTH * clamped);

		if (filled > 0) {
			graphics.fill(x, y, x + filled, y + HEIGHT, full ? tintFull : tint);
		}

		var font = Minecraft.getInstance().font;
		graphics.text(font, label, x + (WIDTH - font.width(label)) / 2, y - LABEL_GAP, 0xFFFFFFFF, true);

		if (full) {
			graphics.fill(x, y, x + WIDTH, y + 1, 0xFFFFFFFF);
			graphics.fill(x, y + HEIGHT - 1, x + WIDTH, y + HEIGHT, 0xFFFFFFFF);
		}
	}
}
