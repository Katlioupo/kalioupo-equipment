"""Texture for the socket table block.

Sixteen pixels, drawn procedurally so it stays consistent if the palette is ever changed:
a dark stone slab with a copper inlay and three round sockets across it.
"""
import os

from PIL import Image

OUT = "src/main/resources/assets/kalioupo_equipment/textures/block/socket_table.png"

STONE_DARK = (58, 58, 60)
STONE = (86, 86, 88)
STONE_LIGHT = (112, 112, 114)
FRAME = (40, 40, 42)
COPPER_DARK = (122, 66, 30)
COPPER = (196, 112, 52)
COPPER_LIGHT = (232, 156, 88)
SOCKET = (18, 16, 20)

WIDTH = HEIGHT = 16


def noise(x, y):
    """Deterministic speckle, so the slab does not look like flat paint."""
    return (x * 7 + y * 13) % 5


def main():
    image = Image.new("RGBA", (WIDTH, HEIGHT), (0, 0, 0, 0))

    for y in range(HEIGHT):
        for x in range(WIDTH):
            if x == 0 or y == 0 or x == WIDTH - 1 or y == HEIGHT - 1:
                color = FRAME
            else:
                pick = noise(x, y)
                color = STONE_LIGHT if pick == 0 else STONE_DARK if pick >= 4 else STONE

            image.putpixel((x, y), color + (255,))

    # copper inlay: two rows across the middle, with a lighter top edge
    for x in range(2, WIDTH - 2):
        image.putpixel((x, 7), COPPER_LIGHT + (255,))
        image.putpixel((x, 8), COPPER + (255,))
        image.putpixel((x, 9), COPPER_DARK + (255,))

    # three sockets punched into the inlay
    for cx in (4, 8, 12):
        for dx, dy in ((0, 7), (0, 8), (0, 9), (-1, 8), (1, 8)):
            image.putpixel((cx + dx, dy), SOCKET + (255,))
        image.putpixel((cx, 6), COPPER_DARK + (255,))

    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    image.save(OUT)
    print("wrote", OUT, image.size)


if __name__ == "__main__":
    main()
