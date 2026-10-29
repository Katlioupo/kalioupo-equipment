"""Material textures.

Ingots keep the vanilla iron-ingot silhouette so they read as ingots in the inventory,
but each material gets its own palette, lighting detail and - for the rare ones - a few
decorative particles / shards floating around the ingot inside the 16x16 cell.

The bedrock shard is a completely different silhouette: an irregular vertically stretched
prism filled with the black-and-white mottling of the bedrock block.
"""
from PIL import Image

SRC = Image.open("build/gui/iron_ingot.png").convert("RGBA")
BEDROCK = Image.open("build/gui/bedrock.png").convert("RGBA")
W, H = SRC.size

# ---------------------------------------------------------------- palettes
# (position, rgb) stops from darkest to brightest, sampled by luminance of the vanilla ingot
PALETTES = {
    "carbon_steel_ingot": [(0.0, (26, 30, 36)), (0.4, (62, 68, 78)), (0.7, (116, 125, 138)), (0.88, (170, 180, 194)), (1.0, (222, 230, 240))],
    "quartz_steel_ingot": [(0.0, (86, 74, 50)), (0.4, (176, 160, 118)), (0.72, (232, 220, 180)), (0.9, (252, 246, 224)), (1.0, (255, 255, 252))],
    "copper_alloy_ingot": [(0.0, (94, 44, 20)), (0.4, (178, 96, 48)), (0.7, (226, 142, 86)), (0.88, (246, 180, 128)), (1.0, (255, 216, 176))],
    "rose_gold_ingot": [(0.0, (104, 46, 44)), (0.4, (196, 112, 106)), (0.7, (238, 156, 148)), (0.88, (252, 194, 186)), (1.0, (255, 226, 220))],
    "prismarine_alloy_ingot": [(0.0, (16, 54, 54)), (0.4, (44, 130, 124)), (0.7, (104, 194, 184)), (0.88, (160, 232, 220)), (1.0, (216, 255, 246))],
    "amethyst_alloy_ingot": [(0.0, (46, 22, 74)), (0.4, (108, 66, 158)), (0.7, (162, 118, 214)), (0.88, (206, 172, 244)), (1.0, (242, 224, 255))],
    "phantom_ingot": [(0.0, (96, 86, 118)), (0.4, (166, 158, 190)), (0.7, (214, 208, 232)), (0.88, (238, 234, 248)), (1.0, (255, 253, 255))],
    "echo_alloy_ingot": [(0.0, (8, 14, 22)), (0.4, (26, 58, 76)), (0.7, (54, 140, 160)), (0.88, (98, 200, 214)), (1.0, (176, 246, 250))],
    "starlight_alloy_ingot": [(0.0, (92, 76, 42)), (0.36, (188, 164, 104)), (0.66, (240, 226, 168)), (0.86, (255, 250, 226)), (1.0, (255, 255, 255))],
}

# decorative details painted *inside* the ingot silhouette
INTERNAL = {
    "prismarine_alloy_ingot": "wave",
    "amethyst_alloy_ingot": "facet",
    "echo_alloy_ingot": "rings",
    "starlight_alloy_ingot": "star",
    "quartz_steel_ingot": "shine",
    "rose_gold_ingot": "shine",
    "phantom_ingot": "shine",
}

# floating particles *outside* the ingot - rare materials only
EXTERNAL = {
    "amethyst_alloy_ingot": [
        (13, 2, (214, 178, 255)), (15, 4, (168, 122, 226)), (2, 13, (196, 152, 246)), (12, 12, (150, 104, 210)),
    ],
    "phantom_ingot": [
        (13, 3, (238, 232, 252)), (15, 6, (198, 190, 224)), (2, 14, (222, 214, 242)), (11, 13, (186, 178, 214)),
    ],
    "echo_alloy_ingot": [
        (13, 2, (120, 232, 240)), (15, 5, (70, 168, 184)), (1, 13, (96, 210, 222)), (12, 12, (58, 148, 166)),
    ],
    "starlight_alloy_ingot": [
        (12, 1, (255, 255, 240)), (13, 2, (250, 232, 168)), (15, 5, (240, 214, 140)),
        (1, 12, (255, 250, 224)), (2, 13, (246, 224, 152)), (12, 13, (255, 246, 206)),
    ],
}


def shade(lum, stops):
    for i in range(len(stops) - 1):
        p0, c0 = stops[i]
        p1, c1 = stops[i + 1]
        if p0 <= lum <= p1:
            t = (lum - p0) / (p1 - p0)
            return tuple(int(round(c0[j] + (c1[j] - c0[j]) * t)) for j in range(3))
    return stops[-1][1] if lum > stops[-1][0] else stops[0][1]


def recolor(image, stops):
    out = Image.new("RGBA", image.size, (0, 0, 0, 0))
    for y in range(image.height):
        for x in range(image.width):
            r, g, b, a = image.getpixel((x, y))
            if a == 0:
                continue
            lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
            R, G, B = shade(lum, stops)
            out.putpixel((x, y), (R, G, B, a))
    return out


def solid(img, x, y, color):
    if 0 <= x < img.width and 0 <= y < img.height and img.getpixel((x, y))[3] > 0:
        img.putpixel((x, y), color + (255,))


def alpha_put(img, x, y, color):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), color + (255,))


def detail(img, style):
    if style == "wave":
        for i in range(6):
            solid(img, 4 + i, 9, (198, 255, 246))
            solid(img, 4 + i, 7, (150, 236, 226))
    elif style == "facet":
        for i in range(5):
            solid(img, 4 + i, 10 - i, (240, 220, 255))
        solid(img, 7, 7, (255, 250, 255))
    elif style == "rings":
        for r in (2, 4):
            import math
            for angle in range(0, 360, 24):
                x = int(round(7 + r * math.cos(math.radians(angle))))
                y = int(round(9 + r * 0.45 * math.sin(math.radians(angle))))
                solid(img, x, y, (150, 245, 250))
    elif style == "star":
        # nether-star style four point sparkle sitting on the ingot face
        cx, cy = 7, 7
        for dx, dy in [(0, -2), (0, 2), (-2, 0), (2, 0)]:
            solid(img, cx + dx, cy + dy, (255, 250, 214))
        for dx, dy in [(0, -1), (0, 1), (-1, 0), (1, 0), (-1, -1), (1, -1), (-1, 1), (1, 1)]:
            solid(img, cx + dx, cy + dy, (255, 255, 255))
        for dx, dy in [(0, -3), (0, 3), (-3, 0), (3, 0)]:
            solid(img, cx + dx, cy + dy, (246, 226, 160))
    elif style == "shine":
        for i in range(5):
            solid(img, 5 + i, 6, (255, 252, 240))


# ---------------------------------------------------------------- bedrock shard
# irregular, vertically stretched rhombus prism: (row) -> (left, right) inclusive
SHARD_ROWS = [
    (2, 7, 8),
    (3, 6, 9),
    (4, 5, 10),
    (5, 5, 10),
    (6, 5, 10),
    (7, 5, 11),
    (8, 5, 11),
    (9, 6, 10),
    (10, 6, 10),
    (11, 6, 9),
    (12, 7, 9),
    (13, 7, 8),
]

RIDGE = 8
BED_MIN, BED_MAX = 0.13, 0.59


def bedrock_mottle(x, y):
    """0..1 black/white mottling taken from the vanilla bedrock block texture."""
    br, bg, bb, _ = BEDROCK.getpixel(((x + 2) % 16, (y + 3) % 16))
    lum = (0.299 * br + 0.587 * bg + 0.114 * bb) / 255.0
    lum = (lum - BED_MIN) / (BED_MAX - BED_MIN)
    lum = max(0.0, min(1.0, lum))
    # push it towards the harsh black / white patches bedrock is known for
    return max(0.0, min(1.0, (lum - 0.42) * 2.1 + 0.56))


def make_shard(stops, glow):
    out = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    mask = set()
    for y, x0, x1 in SHARD_ROWS:
        for x in range(x0, x1 + 1):
            mask.add((x, y))

    # 1px dark outline first, so the silhouette stays readable on any background
    dark = stops[0][1]
    for (x, y) in mask:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            if (x + dx, y + dy) not in mask:
                alpha_put(out, x + dx, y + dy, dark)

    for (x, y) in sorted(mask):
        lum = bedrock_mottle(x, y)
        if x > RIDGE:
            # right facet turns away from the light
            lum *= 0.66
        elif x == RIDGE:
            lum = min(1.0, lum * 1.2 + 0.22)
        if y <= 3:
            # top facet catches a bit less light
            lum *= 0.85
        r, g, b = shade(max(0.0, min(1.0, lum)), stops)
        out.putpixel((x, y), (r, g, b, 255))

    # lit left edge / ridge line
    edge = stops[-2][1]
    for y, x0, x1 in SHARD_ROWS:
        if y <= 8:
            solid(out, x0, y, edge)
        solid(out, x0 + 1, y, stops[-3][1])

    if glow:
        # molten veins: the brightest mottled patches turn white hot
        for (x, y) in mask:
            r, g, b, a = out.getpixel((x, y))
            if r > 200 and g > 110 and b < 140:
                out.putpixel((x, y), (255, 246, 190, 255))
    return out


SHARDS = {
    "bedrock_shard": ([(0.0, (10, 10, 12)), (0.3, (50, 50, 56)), (0.55, (110, 110, 118)), (0.8, (176, 176, 186)), (1.0, (238, 238, 248))], False),
    "heated_bedrock_shard": ([(0.0, (78, 20, 8)), (0.3, (168, 52, 12)), (0.58, (238, 108, 24)), (0.82, (255, 186, 70)), (1.0, (255, 250, 216))], True),
}

# ---------------------------------------------------------------- echo core
# The warden drop. A sculk heart: dark shell, glowing cyan vents, tendrils.
CORE_ROWS = [
    (3, 6, 9),
    (4, 5, 10),
    (5, 4, 11),
    (6, 4, 11),
    (7, 4, 11),
    (8, 4, 11),
    (9, 4, 11),
    (10, 5, 10),
    (11, 6, 9),
]
CORE_TENDRILS = [(2, 7), (2, 8), (12, 7), (12, 8), (7, 3), (8, 3), (7, 12), (8, 12)]
CORE_STOPS = [(0.0, (4, 8, 12)), (0.34, (10, 30, 40)), (0.62, (20, 66, 78)), (0.84, (48, 132, 146)), (1.0, (170, 244, 250))]


def make_core():
    out = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    mask = set()
    for y, x0, x1 in CORE_ROWS:
        for x in range(x0, x1 + 1):
            mask.add((x, y))

    # tendrils: single pixels that reach out of the shell
    for (x, y) in CORE_TENDRILS:
        alpha_put(out, x, y, CORE_STOPS[1][1])

    dark = CORE_STOPS[0][1]
    for (x, y) in mask:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            if (x + dx, y + dy) not in mask:
                alpha_put(out, x + dx, y + dy, dark)

    cx, cy = 7.5, 7.0
    for (x, y) in sorted(mask):
        # spherical shading from the upper left, plus a slow ripple across the shell
        d = ((x - cx) ** 2 + ((y - cy) * 1.15) ** 2) ** 0.5
        lum = 0.85 - d * 0.16
        if (x + y) % 5 == 0:
            lum -= 0.14
        if (x * 3 + y * 5) % 7 == 0:
            lum += 0.12
        lum = max(0.0, min(1.0, lum))
        r, g, b = shade(lum, CORE_STOPS)
        out.putpixel((x, y), (r, g, b, 255))

    # glowing vents: a bright cross in the middle and four cyan cracks
    for (x, y) in ((7, 7), (8, 7), (7, 8), (8, 8)):
        out.putpixel((x, y), (198, 252, 255, 255))
    for (x, y) in ((6, 6), (9, 6), (6, 9), (9, 9)):
        if (x, y) in mask:
            out.putpixel((x, y), (96, 208, 218, 255))
    for (x, y) in ((5, 7), (10, 8), (7, 5), (8, 10)):
        if (x, y) in mask:
            out.putpixel((x, y), (58, 158, 172, 255))
    return out


sheet_items = []

for name, stops in PALETTES.items():
    img = recolor(SRC, stops)
    if name in INTERNAL:
        detail(img, INTERNAL[name])
    for (px, py, c) in EXTERNAL.get(name, []):
        alpha_put(img, px, py, c)
    img.save("src/main/resources/assets/kalioupo_equipment/textures/item/%s.png" % name)
    sheet_items.append((name, img))

for name, (stops, glow) in SHARDS.items():
    img = make_shard(stops, glow)
    img.save("src/main/resources/assets/kalioupo_equipment/textures/item/%s.png" % name)
    sheet_items.append((name, img))

core = make_core()
core.save("src/main/resources/assets/kalioupo_equipment/textures/item/echo_core.png")
sheet_items.append(("echo_core", core))

cell = 16
pad = 2
sheet = Image.new("RGBA", (len(sheet_items) * (cell + pad) + pad, cell + pad * 2), (48, 48, 52, 255))
for i, (_, img) in enumerate(sheet_items):
    sheet.alpha_composite(img, (pad + i * (cell + pad), pad))
sheet.resize((sheet.width * 6, sheet.height * 6), Image.NEAREST).save("build/gui/materials_sheet.png")
print("done", len(sheet_items))
