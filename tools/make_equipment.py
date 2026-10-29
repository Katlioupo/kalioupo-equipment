"""Equipment generator.

For every material tier this derives the whole item set from the vanilla iron equivalents:
item textures, item models, item definitions, shaped recipes, worn armour layers and the
equipment asset json. Adding a new tier only means adding one entry to MATERIALS.
"""
import bisect
import json
import os

from PIL import Image

ROOT = "src/main/resources"
ASSETS = ROOT + "/assets/kalioupo_equipment"
DATA = ROOT + "/data/kalioupo_equipment"
NS = "kalioupo_equipment"

try:
    BEDROCK_TEX = Image.open("build/gui/bedrock.png").convert("RGBA")
except Exception:
    BEDROCK_TEX = None

# material -> (palette stops, pieces, ingot item id)
MATERIALS = {
    "carbon_steel": (
        [(0.00, (26, 30, 36)), (0.34, (64, 70, 80)), (0.60, (112, 121, 134)), (0.82, (170, 180, 194)), (1.00, (228, 236, 246))],
        ["helmet", "chestplate", "leggings", "boots", "sword", "pickaxe", "axe", "shovel", "hoe", "spear"],
        "carbon_steel_ingot",
    ),
    "quartz_steel": (
        [(0.00, (92, 80, 54)), (0.34, (170, 154, 112)), (0.60, (222, 210, 168)), (0.82, (244, 236, 206)), (1.00, (255, 255, 250))],
        ["sword"],
        "quartz_steel_ingot",
    ),
    "copper_alloy": (
        [(0.00, (86, 40, 18)), (0.34, (166, 88, 44)), (0.60, (214, 132, 78)), (0.82, (240, 172, 120)), (1.00, (255, 214, 172))],
        ["helmet", "chestplate", "leggings", "boots"],
        "copper_alloy_ingot",
    ),
    "rose_gold": (
        [(0.00, (96, 42, 40)), (0.34, (186, 104, 98)), (0.60, (232, 148, 140)), (0.82, (250, 188, 180)), (1.00, (255, 226, 220))],
        ["sword", "chestplate"],
        "rose_gold_ingot",
    ),
    "prismarine": (
        [(0.00, (14, 50, 50)), (0.34, (40, 122, 116)), (0.60, (96, 186, 176)), (0.82, (156, 228, 216)), (1.00, (216, 255, 246))],
        ["helmet", "boots"],
        "prismarine_alloy_ingot",
    ),
    "amethyst": (
        [(0.00, (44, 20, 72)), (0.34, (102, 62, 152)), (0.60, (156, 112, 208)), (0.82, (202, 168, 240)), (1.00, (244, 228, 255))],
        ["sword"],
        "amethyst_alloy_ingot",
    ),
    "phantom": (
        [(0.00, (92, 82, 116)), (0.34, (158, 150, 184)), (0.60, (206, 200, 226)), (0.82, (234, 230, 246)), (1.00, (255, 253, 255))],
        ["helmet", "chestplate", "leggings", "boots"],
        "phantom_ingot",
    ),
    "obsidian": (
        [(0.00, (8, 6, 12)), (0.34, (28, 20, 44)), (0.60, (62, 44, 92)), (0.82, (110, 80, 150)), (1.00, (186, 150, 240))],
        ["helmet", "chestplate", "leggings", "boots", "sword", "pickaxe", "axe", "shovel", "hoe", "spear"],
        "compressed_obsidian_alloy",
    ),
    "bedrock": (
        [(0.00, (8, 8, 10)), (0.26, (34, 34, 40)), (0.52, (96, 96, 104)), (0.74, (162, 162, 172)), (0.90, (210, 210, 220)), (1.00, (246, 246, 252))],
        ["helmet", "chestplate", "leggings", "boots", "sword", "pickaxe", "axe", "shovel", "hoe", "spear"],
        "bedrock_shard",
    ),
    # sculk teal: near black shell, deep teal body, cyan glow on the edges
    "echo": (
        [(0.00, (6, 14, 20)), (0.26, (14, 44, 56)), (0.52, (28, 92, 106)), (0.74, (56, 148, 160)), (0.90, (110, 208, 216)), (1.00, (190, 250, 252))],
        ["helmet", "chestplate", "leggings", "boots", "sword", "pickaxe", "axe", "shovel", "hoe", "spear"],
        "echo_alloy_ingot",
    ),
    # nether star white gold, the last tier: clean plates, deep bronze shadows
    "starlight": (
        [(0.00, (46, 36, 18)), (0.30, (120, 98, 46)), (0.56, (186, 158, 78)), (0.76, (228, 206, 136)), (0.90, (250, 240, 200)), (1.00, (255, 253, 238))],
        ["helmet", "chestplate", "leggings", "boots", "sword", "pickaxe", "axe", "shovel", "hoe", "spear"],
        "starlight_alloy_ingot",
    ),
}

# materials whose armour should also render a pair of wings on the player's back
WINGS = {"phantom"}

# smithing table upgrades: piece -> the vanilla base item it is upgraded from.
# the "template" slot takes one ingot and the "addition" slot takes another, so an upgrade
# always costs exactly two ingots and needs no smithing template item.
SMITHING = {
    "phantom": {
        "helmet": "minecraft:diamond_helmet",
        "chestplate": "minecraft:diamond_chestplate",
        "leggings": "minecraft:diamond_leggings",
        "boots": "minecraft:diamond_boots",
    },
    "obsidian": {
        "helmet": ("minecraft:obsidian", "minecraft:diamond_helmet"),
        "chestplate": ("minecraft:obsidian", "minecraft:diamond_chestplate"),
        "leggings": ("minecraft:obsidian", "minecraft:diamond_leggings"),
        "boots": ("minecraft:obsidian", "minecraft:diamond_boots"),
        "sword": ("minecraft:obsidian", "minecraft:diamond_sword"),
        "pickaxe": ("minecraft:obsidian", "minecraft:diamond_pickaxe"),
        "axe": ("minecraft:obsidian", "minecraft:diamond_axe"),
        "shovel": ("minecraft:obsidian", "minecraft:diamond_shovel"),
        "hoe": ("minecraft:obsidian", "minecraft:diamond_hoe"),
        "spear": ("minecraft:obsidian", "minecraft:diamond_spear"),
    },
}

ARMOR_PIECES = {"helmet", "chestplate", "leggings", "boots"}

# tiers that are upgrade only: they must go through the smithing table, so no shaped recipe.
# bedrock belongs here as well: a shard is cheap once you reach the bedrock layer, so letting it
# be crafted straight from shards skips the whole obsidian tier.
NO_CRAFT = {"bedrock", "echo", "starlight"}

# tiers that get a few nether star sparkles sprinkled over the palette shading
SPARKLE = {"starlight"}


def sparkle(image, step=13):
    """Deterministic white sparks so the top tier reads as catching light from every angle."""
    out = image.copy()
    for y in range(image.height):
        for x in range(image.width):
            r, g, b, a = out.getpixel((x, y))
            if a == 0:
                continue
            if (x * 31 + y * 17) % step != 0:
                continue
            lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
            if lum < 0.45:
                continue
            out.putpixel((x, y), (255, 255, 252, a))
    return out

# tiers that are drawn as "the parent tier with sculk creeping over it", so the family
# resemblance survives instead of looking like a blue diamond set
SCULK_FROM = {"echo": "bedrock"}

# tiers drawn as "the parent tier, purified": the sculk burns away into white gold starlight
PURIFY_FROM = {}

# tiers that get a polished 1px light rim on every silhouette edge
RIM = {"starlight"}


def rim_light(image, color=(255, 252, 226)):
    """Hilite the outer edge of every island, the way polished netherite plates read."""
    out = image.copy()
    opaque = {(x, y) for y in range(image.height) for x in range(image.width)
              if image.getpixel((x, y))[3] > 0}

    for (x, y) in opaque:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            if (x + dx, y + dy) not in opaque:
                out.putpixel((x, y), color + (255,))
                break

    return out

GOLD_DARK = (44, 36, 20)
BRONZE = (96, 80, 48)
GOLD_MID = (176, 152, 88)
GOLD_LIGHT = (244, 232, 186)
IVORY = (248, 240, 204)
GOLD_GLOW = (255, 253, 232)


def _lerp(c0, c1, t):
    return tuple(int(round(c0[i] + (c1[i] - c0[i]) * t)) for i in range(3))


def purify(image, dense=False):
    """Turn the sculk echo set into the purified starlight set."""
    out = image.copy()
    for y in range(image.height):
        for x in range(image.width):
            r, g, b, a = image.getpixel((x, y))
            if a == 0:
                continue

            lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
            # sculk reads as cyan: green and blue clearly above red
            sculk = g > r + 16 and b > r + 16

            if sculk:
                # the sculk burns away into pure glow
                color = _lerp(GOLD_LIGHT, GOLD_GLOW, min(1.0, 0.45 + lum))
            else:
                # the black bedrock body turns into gilded bronze, so the silhouette stays read
                lift = min(1.0, 0.15 + lum * 1.6)
                color = _lerp(BRONZE, IVORY, lift)
                if dense and (x * 5 + y * 11) % 7 == 0:
                    color = GOLD_GLOW

            out.putpixel((x, y), tuple(color) + (a,))
    return out

SCULK_BRIGHT = (126, 230, 240)
SCULK_MID = (44, 150, 166)
SCULK_DIM = (14, 62, 74)
SCULK_DEEP = (10, 38, 46)


def sculk_accent(image, dense=False):
    """Keep the parent silhouette, cover its rim in sculk and sprinkle veins inside."""
    out = image.copy()
    width, height = image.size
    opaque = {(x, y) for y in range(height) for x in range(width)
              if image.getpixel((x, y))[3] > 0}

    edge = set()
    for (x, y) in opaque:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            if (x + dx, y + dy) not in opaque:
                edge.add((x, y))
                break

    for (x, y) in edge:
        # bright at the top where the light hits, darker towards the feet
        t = 1.0 - (y / max(1, height - 1))
        if t > 0.55:
            color = SCULK_BRIGHT
        elif t > 0.25:
            color = SCULK_MID
        else:
            color = SCULK_DIM
        out.putpixel((x, y), color + (255,))

    # veins and spore dots creeping inwards
    for (x, y) in sorted(opaque):
        if (x, y) in edge:
            continue
        if (x * 7 + y * 13) % (5 if dense else 9) == 0:
            color = SCULK_MID if (x + y) % 3 else SCULK_BRIGHT
            out.putpixel((x, y), color + (255,))
        elif (x * 17 + y * 5) % 17 == 0:
            out.putpixel((x, y), SCULK_DEEP + (255,))
    return out

# vanilla trim materials and the colour we paint their accent with
TRIM_COLORS = {
    "quartz": (232, 225, 220),
    "iron": (190, 190, 190),
    "netherite": (78, 68, 68),
    "redstone": (172, 20, 20),
    "copper": (184, 92, 58),
    "gold": (222, 170, 42),
    "emerald": (17, 190, 80),
    "diamond": (87, 207, 207),
    "lapis": (38, 97, 156),
    "amethyst": (154, 92, 198),
    "resin": (200, 110, 60),
}

# a few pixels that read as "this armour has a trim", one shape per slot
TRIM_ACCENT = {
    "helmet": [(6, 6), (7, 6), (8, 6), (9, 6)],
    "chestplate": [(x, 8) for x in range(4, 12)] + [(x, 9) for x in range(5, 11)],
    "leggings": [(x, 9) for x in range(5, 11)],
    "boots": [(6, 10), (7, 9), (8, 9), (9, 10)],
}

# vanilla item tags our items must join, otherwise the enchanting table (and the
# enchantment supported_items tags that derive from them) ignores them entirely
VANILLA_TAG = {
    "helmet": "head_armor",
    "chestplate": "chest_armor",
    "leggings": "leg_armor",
    "boots": "foot_armor",
    "sword": "swords",
    "pickaxe": "pickaxes",
    "axe": "axes",
    "shovel": "shovels",
    "hoe": "hoes",
    "spear": "spears",
}

RECIPES = {
    "helmet": ["XXX", "X X"],
    "chestplate": ["X X", "XXX", "XXX"],
    "leggings": ["XXX", "X X", "X X"],
    "boots": ["X X", "X X"],
    "sword": ["X", "X", "#"],
    "pickaxe": ["XXX", " # ", " # "],
    "axe": ["XX", "X#", " #"],
    "shovel": ["X", "#", "#"],
    "hoe": ["XX", " #", " #"],
    "spear": ["  X", " # ", "#  "],
}


try:
    OBSIDIAN_TEX = Image.open("build/gui/obsidian.png").convert("RGBA")
except Exception:
    OBSIDIAN_TEX = None


# Hand drawn bedrock set. When these 16x16 icons are present they become the bedrock item
# textures verbatim, and their colours drive the worn armour layers so the 3D set matches.
ART_DIR = "build/gui/bedrock_art"


def art_icon(piece):
    path = os.path.join(ART_DIR, "%s.png" % piece)
    if not os.path.exists(path):
        return None
    return Image.open(path).convert("RGBA")


def art_pixels():
    pixels = []
    for piece in sorted(ART_ITEMS):
        img = art_icon(piece)
        if img is None:
            continue
        for y in range(img.height):
            for x in range(img.width):
                r, g, b, a = img.getpixel((x, y))
                if a > 0:
                    pixels.append((r, g, b))
    return pixels


def art_palette(pixels):
    """Six luminance stops sampled from the hand drawn icons."""
    if len(pixels) < 64:
        return None
    ordered = sorted(pixels, key=lambda c: 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2])
    stops = []
    for t in (0.00, 0.30, 0.52, 0.72, 0.88, 1.00):
        stops.append((t, ordered[min(len(ordered) - 1, int(t * (len(ordered) - 1)))]))

    # the ramp has to climb strictly, otherwise shading collapses into a flat blob
    floor = 0
    for i in range(len(stops)):
        t, color = stops[i]
        color = tuple(max(floor, c) for c in color)
        floor = max(color) + 4
        stops[i] = (t, color)
    return stops


def art_noise(pixels):
    """A 16x16 tile with the art's own colour histogram, scrambled so no icon shape repeats."""
    if len(pixels) < 256:
        return None
    tile = Image.new("RGBA", (16, 16))
    index = 12345
    for y in range(16):
        for x in range(16):
            index = (index * 1103515245 + 12345) & 0x7FFFFFFF
            r, g, b = pixels[(index >> 8) % len(pixels)]
            tile.putpixel((x, y), (r, g, b, 255))
    return tile


ART_ITEMS = {"helmet", "chestplate", "leggings", "boots",
             "sword", "pickaxe", "axe", "shovel", "hoe", "spear"}
_ART_PIXELS = art_pixels()
ART_SORTED = sorted(_ART_PIXELS, key=lambda c: 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]) or None
ART_STOPS = art_palette(_ART_PIXELS)
ART_NOISE = art_noise(_ART_PIXELS)
if ART_STOPS is not None:
    MATERIALS["bedrock"] = (ART_STOPS, MATERIALS["bedrock"][1], MATERIALS["bedrock"][2])


def recolor(image, stops, mottled=None, armour_layer=False):
    out = Image.new("RGBA", image.size, (0, 0, 0, 0))

    # The drawn bedrock icons are a four tone speckle (black, near black, grey, light grey).
    # Matching the armour sheet onto their exact histogram makes the worn set use the same
    # language: black plates with grey and white grain, instead of a smeared dark ramp.
    hist_src, hist_dst, hist_lums = None, None, None
    if armour_layer and mottled == "bedrock" and ART_SORTED:
        hist_lums = sorted(0.299 * r + 0.587 * g + 0.114 * b
                           for r, g, b, a in image.getdata() if a > 0)
        hist_src, hist_dst = hist_lums, ART_SORTED

    for y in range(image.height):
        for x in range(image.width):
            r, g, b, a = image.getpixel((x, y))
            if a == 0:
                continue
            lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
            if hist_src:
                # rank this pixel inside the sheet, then take the drawn colour at the same rank
                rank = bisect.bisect_left(hist_src, (0.299 * r + 0.587 * g + 0.114 * b))
                frac = rank / max(1, len(hist_src) - 1)
                R, G, B = hist_dst[int(frac * (len(hist_dst) - 1))]
                out.putpixel((x, y), (R, G, B, a))
                continue
            if mottled == "bedrock":
                # bedrock already has its own hand drawn speckle; reuse that histogram so the
                # worn armour carries the same grain instead of being stretched into bands
                if ART_NOISE is not None:
                    tex, weight, contrast = ART_NOISE, (0.34 if armour_layer else 0.62), 1.15
                    sx = (x * 7 + y * 13) % 16
                    sy = (x * 11 + y * 5) % 16
                else:
                    tex, weight, contrast = BEDROCK_TEX, 0.55, 1.35
                    sx = x % 16
                    sy = (y // 2 + x % 3) % 16
            elif mottled == "obsidian":
                tex, weight, contrast = OBSIDIAN_TEX, 0.42, 1.30
                sx = x % 16
                sy = (y // 2 + x % 3) % 16
            else:
                tex = None

            if tex is not None:
                br, bg, bb, _ = tex.getpixel((sx, sy))
                bl = (0.299 * br + 0.587 * bg + 0.114 * bb) / 255.0
                bl = max(0.0, min(1.0, (bl - 0.11) / 0.55))
                lum = (1.0 - weight) * lum + weight * bl
                lum = max(0.0, min(1.0, (lum - 0.5) * contrast + 0.5))
            R, G, B = stops[0][1]
            for i in range(len(stops) - 1):
                p0, c0 = stops[i]
                p1, c1 = stops[i + 1]
                if p0 <= lum <= p1:
                    t = (lum - p0) / (p1 - p0)
                    R = int(round(c0[0] + (c1[0] - c0[0]) * t))
                    G = int(round(c0[1] + (c1[1] - c0[1]) * t))
                    B = int(round(c0[2] + (c1[2] - c0[2]) * t))
                    break
                R, G, B = c1
            out.putpixel((x, y), (R, G, B, a))
    return out


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2)
        f.write("\n")


def with_wings(chestplate, stops):
    """Item icon: the elytra wings tucked behind the chestplate."""
    wings = recolor(Image.open("build/gui/vanilla_items/elytra_item.png").convert("RGBA"), stops)
    out = Image.new("RGBA", (16, 16), (0, 0, 0, 0))

    # push the wings outward by one pixel so they peek out beside the torso
    for y in range(16):
        for x in range(16):
            r, g, b, a = wings.getpixel((x, y))
            if a == 0:
                continue
            target_x = x - 2 if x < 8 else x + 2
            if 0 <= target_x < 16:
                out.putpixel((target_x, y), (int(r * 0.86), int(g * 0.86), int(b * 0.92), a))

    out.alpha_composite(chestplate)
    return out


def main():
    sheet_rows = []
    vanilla_tags = {}
    for material, (stops, pieces, ingot) in MATERIALS.items():
        row = []
        for piece in pieces:
            item = "%s_%s" % (material, piece)
            vanilla_tags.setdefault(VANILLA_TAG[piece], []).append("%s:%s" % (NS, item))
            art = art_icon(piece) if material == "bedrock" else None
            if material in SCULK_FROM:
                parent_icon = "%s/textures/item/%s_%s.png" % (ASSETS, SCULK_FROM[material], piece)
                img = sculk_accent(Image.open(parent_icon).convert("RGBA"))
            elif material in PURIFY_FROM:
                parent_icon = "%s/textures/item/%s_%s.png" % (ASSETS, PURIFY_FROM[material], piece)
                img = sparkle(purify(Image.open(parent_icon).convert("RGBA")))
            elif art is not None:
                # hand drawn icon wins over the generated one, no recolour applied
                img = art
            else:
                src = Image.open("build/gui/vanilla_items/iron_%s.png" % piece).convert("RGBA")
                img = recolor(src, stops, material)
                if material in SPARKLE:
                    img = sparkle(img, step=17)
                if material in RIM:
                    img = rim_light(img)

            if material in WINGS and piece == "chestplate":
                img = with_wings(img, stops)

            img.save(ASSETS + "/textures/item/%s.png" % item)

            if piece in ARMOR_PIECES:
                cases = []

                for trim, color in TRIM_COLORS.items():
                    # vanilla builds the trimmed icon as two layers: our armour, plus the trim
                    # overlay sprite that the game recolours from the trim mask and palette. That
                    # gives the real vanilla trim shape and colours instead of a recoloured blob.
                    variant_name = "%s_%s_trim" % (item, trim)
                    write(ASSETS + "/models/item/%s.json" % variant_name, {
                        "parent": "minecraft:item/generated",
                        "textures": {
                            "layer0": "%s:item/%s" % (NS, item),
                            "layer1": "minecraft:trims/items/%s_trim_%s" % (piece, trim),
                        }
                    })
                    cases.append({
                        "when": "minecraft:%s" % trim,
                        "model": {"type": "minecraft:model", "model": "%s:item/%s" % (NS, variant_name)}
                    })

                write(ASSETS + "/items/%s.json" % item, {
                    "model": {
                        "type": "minecraft:select",
                        "property": "minecraft:trim_material",
                        "fallback": {"type": "minecraft:model", "model": "%s:item/%s" % (NS, item)},
                        "cases": cases
                    }
                })
            else:
                write(ASSETS + "/items/%s.json" % item, {
                    "model": {"type": "minecraft:model", "model": "%s:item/%s" % (NS, item)}
                })
            write(ASSETS + "/models/item/%s.json" % item, {
                # tools and weapons must be handheld, otherwise the third person pose is
                # the flat two dimensional "item on a stick" transform
                "parent": "minecraft:item/handheld" if piece not in ARMOR_PIECES
                          else "minecraft:item/generated",
                "textures": {"layer0": "%s:item/%s" % (NS, item)}
            })

            pattern = RECIPES[piece]
            used = set("".join(pattern))
            key = {"X": "%s:%s" % (NS, ingot)}
            if "#" in used:
                key["#"] = "minecraft:stick"
            if material not in NO_CRAFT:
                write(DATA + "/recipe/%s.json" % item, {
                    "type": "minecraft:crafting_shaped",
                    "category": "equipment",
                    "key": key,
                    "pattern": pattern,
                    "result": {"id": "%s:%s" % (NS, item), "count": 1}
                })
            row.append(img)

        for piece, base in SMITHING.get(material, {}).items():
            if isinstance(base, tuple):
                template_id, base_item = base
            else:
                template_id, base_item = "%s:%s" % (NS, ingot), base
            write(DATA + "/recipe/%s_%s_smithing.json" % (material, piece), {
                "type": "minecraft:smithing_transform",
                "template": template_id,
                "base": base_item,
                "addition": "%s:%s" % (NS, ingot),
                "result": {"id": "%s:%s_%s" % (NS, material, piece), "count": 1}
            })

        # worn armour layers (only for tiers that actually have armour pieces)
        if pieces and any(p in ARMOR_PIECES for p in pieces):
            for layer in ("humanoid", "humanoid_leggings"):
                out = ASSETS + "/textures/entity/equipment/%s/%s.png" % (layer, material)
                os.makedirs(os.path.dirname(out), exist_ok=True)
                if material in SCULK_FROM:
                    parent_layer = "%s/textures/entity/equipment/%s/%s.png" % (
                        ASSETS, layer, SCULK_FROM[material])
                    sculk_accent(Image.open(parent_layer).convert("RGBA"), dense=True).save(out)
                elif material in PURIFY_FROM:
                    parent_layer = "%s/textures/entity/equipment/%s/%s.png" % (
                        ASSETS, layer, PURIFY_FROM[material])
                    sparkle(purify(Image.open(parent_layer).convert("RGBA"), dense=True)).save(out)
                else:
                    src = Image.open("build/gui/vanilla_items/iron_%s.png" % layer).convert("RGBA")
                    img = recolor(src, stops, material, armour_layer=True)
                    if material in RIM:
                        img = rim_light(img)
                        img = sparkle(img, step=23)
                    img.save(out)
            write(ASSETS + "/equipment/%s.json" % material, {
                "layers": dict({
                    "humanoid": [{"texture": "%s:%s" % (NS, material)}],
                    "humanoid_leggings": [{"texture": "%s:%s" % (NS, material)}],
                }, **({"wings": [{"texture": "%s:%s" % (NS, material)}]}
                      if material in WINGS else {}))
            })

            if material in WINGS:
                wings_src = Image.open("build/gui/vanilla_items/elytra_wings.png").convert("RGBA")
                wings_img = recolor(wings_src, stops, material)
                wings_out = ASSETS + "/textures/entity/equipment/wings/%s.png" % material
                os.makedirs(os.path.dirname(wings_out), exist_ok=True)
                wings_img.save(wings_out)

        write(DATA + "/tags/item/%s_ingots.json" % material, {
            "values": ["%s:%s" % (NS, ingot)]
        })
        sheet_rows.append((material, row))

    # standalone drops that only need a flat icon and no recipe of their own
    for extra in ("echo_core",):
        write(ASSETS + "/models/item/%s.json" % extra, {
            "parent": "minecraft:item/generated",
            "textures": {"layer0": "%s:item/%s" % (NS, extra)}
        })
        write(ASSETS + "/items/%s.json" % extra, {
            "model": {"type": "minecraft:model", "model": "%s:item/%s" % (NS, extra)}
        })

    width = 10
    sheet = Image.new("RGBA", (width * 18 + 2, len(sheet_rows) * 18 + 2), (139, 139, 139, 255))
    for r, (_, row) in enumerate(sheet_rows):
        for c, img in enumerate(row):
            sheet.alpha_composite(img, (c * 18 + 1, r * 18 + 1))
    sheet.resize((sheet.width * 7, sheet.height * 7), Image.NEAREST).save("build/gui/equipment_sheet.png")

    # datapack tags merge across packs, so listing only our items here appends to vanilla
    for tag, values in vanilla_tags.items():
        write(ROOT + "/data/minecraft/tags/item/%s.json" % tag, {"values": values})

    print("materials", len(MATERIALS), "items", sum(len(v[1]) for v in MATERIALS.values()),
          "tags", len(vanilla_tags))


pieces = ["helmet", "chestplate", "leggings", "boots", "sword",
          "pickaxe", "axe", "shovel", "hoe", "spear"]
SMITHING["bedrock"] = {
    piece: ("kalioupo_equipment:bedrock_shard", "kalioupo_equipment:obsidian_" + piece) for piece in pieces
}

# the echo step: one echo ingot per piece, the template is a plain diamond so tools stay affordable
SMITHING["echo"] = {
    piece: ("minecraft:diamond", "kalioupo_equipment:bedrock_" + piece) for piece in pieces
}

# the last step: the rare drops (wither star, echo core) go into the smelter, not into every
# single piece. Four ingots per batch means the whole set costs one star instead of four, and the
# smithing template is a plain diamond so tools stay affordable.
SMITHING["starlight"] = {
    piece: ("minecraft:diamond", "kalioupo_equipment:echo_" + piece) for piece in pieces
}

if __name__ == "__main__":
    main()
