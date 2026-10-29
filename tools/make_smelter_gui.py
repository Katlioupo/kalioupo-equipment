"""Smelter GUI background.

Starts from the vanilla furnace texture so every element (panel, slot frames, arrow
placeholder, the unlit flame silhouette baked into the vanilla sheet) looks identical,
then lays the smelter's own slot grid on top.

The lit flame and the smelting arrow are drawn at runtime by SmelterScreen, exactly the
same way the vanilla furnace screen does it, so the flame is always visible as an empty
silhouette and only the fuel part of it is filled in orange.
"""
from PIL import Image

src = Image.open("build/gui/furnace.png").convert("RGBA")
W, H = 176, 166
panel = src.getpixel((30, 60))

# vanilla slot frame is 18x18 and sits one pixel up/left of the Slot's x/y
slot = src.crop((55, 16, 73, 34))
flame = src.crop((56, 36, 70, 50))
arrow = src.crop((79, 35, 103, 51))

INPUTS = [(8, 17), (26, 17), (44, 17), (8, 35), (26, 35)]
OUTPUT = (110, 35)
FUEL = (44, 53)
ARROW_AT = (66, 30)
FLAME_AT = (44, 35)

img = src.crop((0, 0, W, H)).copy()

# clear only the machine area, keep every border and the inventory section untouched
for y in range(4, 83):
    for x in range(4, 172):
        img.putpixel((x, y), panel)

for pos in INPUTS + [OUTPUT, FUEL]:
    img.paste(slot, (pos[0] - 1, pos[1] - 1))

img.paste(flame, FLAME_AT)
img.paste(arrow, ARROW_AT)

img.save("src/main/resources/assets/kalioupo_equipment/textures/gui/smelter.png")
img.resize((W * 4, H * 4), Image.NEAREST).save("build/gui/smelter_preview.png")
print("ok")
