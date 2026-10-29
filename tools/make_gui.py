"""Compose taller backpack GUI textures from the vanilla generic_54 layout.

Vanilla generic_54 (256x256):
  y 0..17    : header (title bar)
  y 17..125  : 6 storage rows of 18px each
  y 126..222 : player inventory section (4 rows: 3 main + hotbar)
"""
from PIL import Image

SRC = r"build/gui/generic_54.png"
OUT_DIR = r"src/main/resources/assets/kalioupo_equipment/textures/gui"

src = Image.open(SRC).convert("RGBA")
W = 176
HEADER_H = 17
ROW_H = 18
BOTTOM_Y = 126
BOTTOM_H = 96

header = src.crop((0, 0, W, HEADER_H))
row = src.crop((0, 17, W, 17 + ROW_H))
bottom = src.crop((0, BOTTOM_Y, W, BOTTOM_Y + BOTTOM_H))

for rows in range(6, 17):
    height = HEADER_H + rows * ROW_H + BOTTOM_H
    img = Image.new("RGBA", (W, height), (0, 0, 0, 0))
    img.paste(header, (0, 0))
    for r in range(rows):
        img.paste(row, (0, HEADER_H + r * ROW_H))
    img.paste(bottom, (0, HEADER_H + rows * ROW_H))
    img.save(f"{OUT_DIR}/backpack_{rows}.png")
    print("wrote", rows, img.size)

# preview of the 8-row variant
preview = Image.open(f"{OUT_DIR}/backpack_16.png")
preview.resize((preview.width * 3, preview.height * 3), Image.NEAREST).save("build/gui/preview_16.png")
print("preview", preview.size)

