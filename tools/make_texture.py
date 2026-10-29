"""Generates the 16x16 backpack item texture and a 256x preview."""
from PIL import Image

W = H = 16
img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
px = img.load()

OUTLINE = (62, 42, 26, 255)
LEATHER = (150, 100, 56, 255)
LEATHER_LIGHT = (176, 124, 74, 255)
LEATHER_DARK = (110, 70, 38, 255)
IRON = (200, 200, 205, 255)
IRON_DARK = (140, 140, 148, 255)
STRING = (214, 196, 148, 255)

def put(x, y, c):
    if 0 <= x < W and 0 <= y < H:
        px[x, y] = c

def rect(x0, y0, x1, y1, c):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            put(x, y, c)

# handle
for x, y in [(6, 2), (7, 1), (8, 1), (9, 2)]:
    put(x, y, OUTLINE)

# body
rect(3, 3, 12, 14, LEATHER)
# flap
rect(3, 3, 12, 6, LEATHER_DARK)
# outline
for x in range(3, 13):
    put(x, 3, OUTLINE)
    put(x, 14, OUTLINE)
for y in range(3, 15):
    put(3, y, OUTLINE)
    put(12, y, OUTLINE)

# side shading
for y in range(7, 14):
    put(3, y, LEATHER_DARK)
    put(12, y, LEATHER_LIGHT)

# vertical straps
for y in range(7, 14):
    put(5, y, LEATHER_LIGHT)
    put(10, y, LEATHER_LIGHT)

# iron buckle in the middle of the flap
rect(7, 6, 8, 8, IRON)
put(7, 6, IRON_DARK)
put(8, 6, IRON_DARK)
put(7, 8, IRON_DARK)
put(8, 8, IRON_DARK)
put(7, 7, OUTLINE)
put(8, 7, IRON_DARK)

# string stitching along the bottom
for x in range(4, 12, 2):
    put(x, 13, STRING)

# pocket line
for x in range(4, 12):
    put(x, 11, STRING)
put(4, 11, OUTLINE)
put(11, 11, OUTLINE)

img.save("src/main/resources/assets/kalioupo_equipment/textures/item/backpack.png")
img.resize((256, 256), Image.NEAREST).save("build/backpack_preview.png")
print("ok")
