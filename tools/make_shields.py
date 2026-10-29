"""Shield set generator: textures, 3D models, recipes and a Blockbench project.

Texture layout is our own box-UV unwrap (the same rule Minecraft uses for entity boxes), so the
project file can be painted in Blockbench in 3D and the result dropped straight back in.
"""
import base64
import json
import os
import uuid as uuidlib

from PIL import Image, ImageDraw

ROOT = "src/main/resources"
ASSETS = ROOT + "/assets/kalioupo_equipment"
DATA = ROOT + "/data/kalioupo_equipment"
NS = "kalioupo_equipment"
DESKTOP = os.path.expanduser("~") + "/Desktop/Kalioupo装备贴图模板"

# name -> (dark, mid, light, glow colour or None)
PALETTES = {
    "carbon_steel_shield": ((26, 30, 36), (104, 112, 126), (206, 214, 226), None),
    "obsidian_shield": ((8, 6, 14), (48, 34, 74), (128, 96, 178), None),
    "bedrock_shield": ((6, 6, 8), (72, 72, 80), (232, 232, 242), None),
    "carbon_steel_shield_glow": ((26, 30, 36), (128, 138, 152), (240, 248, 255), (255, 246, 196)),
    "obsidian_shield_glow": ((8, 6, 14), (60, 42, 92), (168, 132, 222), (214, 186, 255)),
    "bedrock_shield_glow": ((6, 6, 8), (90, 90, 98), (250, 250, 255), (255, 242, 176)),
}

# ---------------------------------------------------------------- geometry
# plate 12 wide x 20 tall x 1 thick, grip 2 x 6 x 6, four spikes on the front
PLATE_FROM = [2.0, -1.0, 7.4]
PLATE_TO = [14.0, 19.0, 8.6]
HANDLE_FROM = [7.0, 6.0, 8.6]
HANDLE_TO = [9.0, 12.0, 11.0]
SPIKE_SPOTS = [(3.2, 2.5), (10.0, 2.5), (3.2, 14.5), (10.0, 14.5)]

TEX = 64


def box_uv(u, v, w, h, d):
    """Standard Minecraft box unwrap, the same one Blockbench generates."""
    return {
        "up": (u + d, v, u + d + w, v + d),
        "down": (u + d + w, v, u + 2 * d + w, v + d),
        "east": (u, v + d, u + d, v + d + h),
        "front": (u + d, v + d, u + d + w, v + d + h),
        "west": (u + d + w, v + d, u + 2 * d + w, v + d + h),
        "back": (u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h),
    }


PLATE_UV = box_uv(0, 0, 12, 20, 1)
HANDLE_UV = box_uv(28, 0, 2, 6, 6)
SPIKE_UV = (48, 0, 52, 4)


def shade(c0, c1, t):
    return tuple(int(round(c0[i] + (c1[i] - c0[i]) * t)) for i in range(3))


def scale(color, factor):
    return tuple(max(0, min(255, int(c * factor))) for c in color)


def fill(img, region, color, alpha=255):
    x1, y1, x2, y2 = region
    for y in range(y1, y2):
        for x in range(x1, x2):
            img.putpixel((x, y), color + (alpha,))


def paint_face(img, region, dark, mid, light, glow, bright=1.0, boss=True):
    x1, y1, x2, y2 = region
    width, height = x2 - x1, y2 - y1

    for y in range(height):
        for x in range(width):
            edge = x == 0 or x == width - 1 or y == 0 or y == height - 1
            v = 1.0 - (y / max(1, height - 1))
            across = 1.0 - abs((x / max(1, width - 1)) - 0.5) * 2.0

            if edge:
                color = shade(dark, mid, 0.2 + 0.3 * v)
            else:
                color = shade(dark, light, min(1.0, 0.22 + 0.42 * v + 0.26 * across))

            color = scale(color, bright)
            img.putpixel((x1 + x, y1 + y), color + (255,))

    if boss:
        cx, cy = x1 + width // 2, y1 + height // 2
        for (dx, dy) in ((0, 0), (-1, 0), (1, 0), (0, -1), (0, 1)):
            img.putpixel((cx + dx, cy + dy), scale(light, bright) + (255,))

    if glow is not None:
        for y in range(y1, y2):
            img.putpixel((x1, y), glow + (255,))
            img.putpixel((x2 - 1, y), glow + (255,))

        for y in range(height):
            for x in range(width):
                if ((x1 + x) * 7 + (y1 + y) * 11) % 6 == 0:
                    img.putpixel((x1 + x, y1 + y), glow + (255,))


def build(name):
    dark, mid, light, glow = PALETTES[name]
    img = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 0))

    paint_face(img, PLATE_UV["front"], dark, mid, light, glow, 1.0)
    paint_face(img, PLATE_UV["back"], dark, mid, light, glow, 0.72, boss=False)

    for key, factor in (("east", 0.55), ("west", 0.45), ("up", 0.9), ("down", 0.4)):
        fill(img, PLATE_UV[key], scale(mid, factor))

    for key, factor in (("front", 0.5), ("back", 0.38), ("east", 0.32), ("west", 0.32),
                        ("up", 0.6), ("down", 0.28)):
        fill(img, HANDLE_UV[key], scale(dark, factor / 0.5))

    fill(img, SPIKE_UV, glow if glow is not None else scale(light, 1.0))
    return img


DISPLAY = {
    "thirdperson_righthand": {"rotation": [0, 90, 0], "translation": [12.5, 6, -4], "scale": [1, 1, 1]},
    "thirdperson_lefthand": {"rotation": [0, 90, 0], "translation": [7.5, 6, 12], "scale": [1, 1, 1]},
    "firstperson_righthand": {"rotation": [0, 180, 5], "translation": [-8, 1.75, -10], "scale": [1.25, 1.25, 1.25]},
    "firstperson_lefthand": {"rotation": [0, 180, 5], "translation": [12, 0, -10], "scale": [1.25, 1.25, 1.25]},
    "gui": {"rotation": [15, -25, -5], "translation": [2, 3, 0], "scale": [0.65, 0.65, 0.65]},
    "fixed": {"rotation": [0, 180, 0], "translation": [-4.5, 4.5, -5], "scale": [0.55, 0.55, 0.55]},
    "ground": {"rotation": [0, 0, 0], "translation": [2, 4, 2], "scale": [0.25, 0.25, 0.25]},
}

DISPLAY_BLOCKING = {
    "thirdperson_righthand": {"rotation": [45, 155, 0], "translation": [-1.5, 11, -2], "scale": [1, 1, 1]},
    "thirdperson_lefthand": {"rotation": [45, 155, 0], "translation": [13.5, 7, 2.5], "scale": [1, 1, 1]},
    "firstperson_righthand": {"rotation": [0, 180, -5], "translation": [-13, 3.25, -11], "scale": [1.25, 1.25, 1.25]},
    "firstperson_lefthand": {"rotation": [0, 180, -5], "translation": [7, 5, -11], "scale": [1.25, 1.25, 1.25]},
    "gui": {"rotation": [15, -25, -5], "translation": [2, 3, 0], "scale": [0.65, 0.65, 0.65]},
    "fixed": {"rotation": [0, 180, 0], "translation": [-4.5, 4.5, -5], "scale": [0.55, 0.55, 0.55]},
    "ground": {"rotation": [0, 0, 0], "translation": [2, 4, 2], "scale": [0.25, 0.25, 0.25]},
}


def face_uv(region):
    return [region[0], region[1], region[2], region[3]]


def plate_box():
    return {
        "from": PLATE_FROM,
        "to": PLATE_TO,
        "faces": {
            "north": {"uv": face_uv(PLATE_UV["front"]), "texture": "#0"},
            "south": {"uv": face_uv(PLATE_UV["back"]), "texture": "#0"},
            "east": {"uv": face_uv(PLATE_UV["east"]), "texture": "#0"},
            "west": {"uv": face_uv(PLATE_UV["west"]), "texture": "#0"},
            "up": {"uv": face_uv(PLATE_UV["up"]), "texture": "#0"},
            "down": {"uv": face_uv(PLATE_UV["down"]), "texture": "#0"},
        },
    }


def handle_box():
    return {
        "from": HANDLE_FROM,
        "to": HANDLE_TO,
        "faces": {
            "north": {"uv": face_uv(HANDLE_UV["front"]), "texture": "#0"},
            "south": {"uv": face_uv(HANDLE_UV["back"]), "texture": "#0"},
            "east": {"uv": face_uv(HANDLE_UV["east"]), "texture": "#0"},
            "west": {"uv": face_uv(HANDLE_UV["west"]), "texture": "#0"},
            "up": {"uv": face_uv(HANDLE_UV["up"]), "texture": "#0"},
            "down": {"uv": face_uv(HANDLE_UV["down"]), "texture": "#0"},
        },
    }


def spike_boxes():
    boxes = []

    for (bx, by) in SPIKE_SPOTS:
        boxes.append({
            "from": [bx, by, 8.6],
            "to": [bx + 1.6, by + 1.6, 10.4],
            "faces": {f: {"uv": face_uv(SPIKE_UV), "texture": "#0"}
                      for f in ("north", "south", "east", "west", "up", "down")},
        })
        boxes.append({
            "from": [bx + 0.35, by + 0.35, 10.4],
            "to": [bx + 1.25, by + 1.25, 11.4],
            "faces": {f: {"uv": face_uv(SPIKE_UV), "texture": "#0"}
                      for f in ("north", "south", "east", "west", "up", "down")},
        })

    return boxes


def shield_model(name, blocking=False):
    texture = "%s:item/%s" % (NS, name)

    return {
        "ambientocclusion": False,
        "gui_light": "front",
        "textures": {"0": texture, "particle": texture},
        "display": DISPLAY_BLOCKING if blocking else DISPLAY,
        "elements": [plate_box(), handle_box()] + spike_boxes(),
    }


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2)
        f.write("\n")


RECIPES = {
    "carbon_steel_shield": {
        "type": "minecraft:crafting_shaped",
        "pattern": ["XGX", "XXX", " X "],
        "key": {"X": "kalioupo_equipment:carbon_steel_ingot", "G": "minecraft:glass"},
        "result": "kalioupo_equipment:carbon_steel_shield",
    },
    "obsidian_shield": {
        "type": "minecraft:crafting_shaped",
        "pattern": ["XXX", "XXX", " X "],
        "key": {"X": "kalioupo_equipment:compressed_obsidian_alloy"},
        "result": "kalioupo_equipment:obsidian_shield",
    },
    "bedrock_shield_smithing": {
        "type": "minecraft:smithing_transform",
        "template": "kalioupo_equipment:bedrock_shard",
        "base": "kalioupo_equipment:obsidian_shield",
        "addition": "kalioupo_equipment:bedrock_shard",
        "result": "kalioupo_equipment:bedrock_shield",
    },
}

for base in ("carbon_steel_shield", "obsidian_shield", "bedrock_shield"):
    RECIPES[base + "_glow"] = {
        "type": "minecraft:crafting_shapeless",
        "ingredients": ["kalioupo_equipment:" + base, "minecraft:glowstone"],
        "result": "kalioupo_equipment:" + base + "_glow",
    }

for name in PALETTES:
    build(name).save(ASSETS + "/textures/item/%s.png" % name)
    write(ASSETS + "/models/item/%s.json" % name, shield_model(name))
    write(ASSETS + "/models/item/%s_blocking.json" % name, shield_model(name, blocking=True))
    write(ASSETS + "/items/%s.json" % name, {
        "model": {
            "type": "minecraft:condition",
            "property": "minecraft:using_item",
            "on_false": {"type": "minecraft:model", "model": "%s:item/%s" % (NS, name)},
            "on_true": {"type": "minecraft:model", "model": "%s:item/%s_blocking" % (NS, name)},
        }
    })

for name, recipe in RECIPES.items():
    body = dict(recipe)
    result = body.pop("result")
    body["result"] = {"id": result, "count": 1}
    write(DATA + "/recipe/%s.json" % name, body)


# ---------------------------------------------------------------- blockbench project
def bbmodel(source_name):
    png = ASSETS + "/textures/item/%s.png" % source_name
    data = base64.b64encode(open(png, "rb").read()).decode("ascii")
    elements = []
    outliner = []

    def add(box, label):
        element = {
            "uuid": str(uuidlib.uuid4()),
            "type": "cube",
            "name": label,
            "from": box["from"],
            "to": box["to"],
            "origin": [8, 8, 8],
            "faces": {face: {"uv": value["uv"], "texture": 0} for face, value in box["faces"].items()},
        }
        elements.append(element)
        outliner.append(element["uuid"])

    add(plate_box(), "plate")
    add(handle_box(), "handle")

    for index, box in enumerate(spike_boxes()):
        add(box, "spike_%d" % index)

    return {
        "meta": {"format_version": "4.10", "model_format": "java_block", "box_uv": False},
        "name": "kalioupo_shield",
        "resolution": {"width": TEX, "height": TEX},
        "elements": elements,
        "outliner": outliner,
        "textures": [{
            "name": "%s.png" % source_name,
            "id": "0",
            "uv_width": TEX,
            "uv_height": TEX,
            "source": "data:image/png;base64," + data,
            "folder": "item",
        }],
        "display": DISPLAY,
    }


def template_sheet():
    img = Image.new("RGBA", (TEX, TEX), (24, 24, 28, 255))
    draw = ImageDraw.Draw(img)

    for region in PLATE_UV.values():
        draw.rectangle([region[0], region[1], region[2] - 1, region[3] - 1], fill=(96, 96, 104, 255))
    for region in HANDLE_UV.values():
        draw.rectangle([region[0], region[1], region[2] - 1, region[3] - 1], fill=(70, 70, 78, 255))
    draw.rectangle([SPIKE_UV[0], SPIKE_UV[1], SPIKE_UV[2] - 1, SPIKE_UV[3] - 1], fill=(200, 200, 210, 255))

    for region in list(PLATE_UV.values()) + list(HANDLE_UV.values()):
        draw.rectangle([region[0], region[1], region[2] - 1, region[3] - 1], outline=(255, 0, 255, 255))
    draw.rectangle([SPIKE_UV[0], SPIKE_UV[1], SPIKE_UV[2] - 1, SPIKE_UV[3] - 1], outline=(0, 255, 255, 255))
    return img


if os.path.isdir(DESKTOP):
    with open(os.path.join(DESKTOP, "盾牌模型_可直接绘画.bbmodel"), "w", encoding="utf-8") as f:
        json.dump(bbmodel("bedrock_shield"), f, indent=2)
        f.write("\n")

    template_sheet().resize((TEX * 8, TEX * 8), Image.NEAREST).save(
        os.path.join(DESKTOP, "盾牌UV模板_64x64_标注版.png"))

print("shields", len(PALETTES), "recipes", len(RECIPES))
