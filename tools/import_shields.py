"""Import the hand painted shield models and textures.

The artist paints in Blockbench and drops the .bbmodel files on the Desktop. This script turns
them into Minecraft item models.

Everything is placed exactly like the vanilla shield so the in hand result matches vanilla:
vanilla geometry is plate addBox(-6, -11, -2, 12, 22, 1) and handle addBox(-1, -3, -1, 2, 6, 6)
on a 64x64 texture, and vanilla applies the display transforms copied below. Item model space
is 0..16 with the item origin at (8, 8, 8), so vanilla -Z (the face the player looks at) is
min z and the texture atlas is 4x the 0..16 uv space.

* plate and handle cubes are taken verbatim (they are axis aligned) and shifted onto the vanilla
  box, so the shield sits on the hand instead of floating in front of it
* the spikes are rebuilt as clean axis aligned cubes on the far side of the plate, because
  vanilla item models only allow a single axis rotation and the painted spikes use free rotation
* the texture embedded in the .bbmodel is extracted as is
"""
import base64
import io
import json
import os
import zipfile

from PIL import Image

ROOT = "src/main/resources"
ASSETS = ROOT + "/assets/kalioupo_equipment"
DATA = ROOT + "/data/kalioupo_equipment"
NS = "kalioupo_equipment"
DESKTOP = os.path.expanduser("~") + "/Desktop"

SPIKE_UV = [12, 0, 13, 1]  # the 4x4 patch in the painted texture that the spikes sample, 0..16 uv

TEXTURE_PX = 64
UV_SCALE = TEXTURE_PX // 16  # 0..16 uv space -> texture pixels

# vanilla 26.3 assets/minecraft/models/item/shield.json
#
# These are copied verbatim. The shield geometry below is the vanilla box too, so with the same
# numbers our shield lands exactly where the vanilla shield lands, in every display context.
DISPLAY = {
    "thirdperson_righthand": {"rotation": [0, 90, 0], "translation": [10, 6, -4], "scale": [1, 1, 1]},
    "thirdperson_lefthand": {"rotation": [0, 90, 0], "translation": [10, 6, 12], "scale": [1, 1, 1]},
    "firstperson_righthand": {"rotation": [0, 180, 5], "translation": [-10, 1.75, -10],
                              "scale": [1.25, 1.25, 1.25]},
    "firstperson_lefthand": {"rotation": [0, 180, 5], "translation": [10, 0, -10],
                             "scale": [1.25, 1.25, 1.25]},
    "gui": {"rotation": [15, -25, -5], "translation": [2, 3, 0], "scale": [0.65, 0.65, 0.65]},
    "fixed": {"rotation": [0, 180, 0], "translation": [-4.5, 4.5, -5], "scale": [0.55, 0.55, 0.55]},
    "on_shelf": {"rotation": [0, 0, 0], "translation": [11, 18.5, 8.7], "scale": [1.4, 1.4, 1.4]},
    "ground": {"rotation": [0, 0, 0], "translation": [2, 4, 2], "scale": [0.25, 0.25, 0.25]},
}

# vanilla 26.3 assets/minecraft/models/item/shield_blocking.json
DISPLAY_BLOCKING = {
    "thirdperson_righthand": {"rotation": [45, 155, 0], "translation": [-3.49, 11, -2],
                              "scale": [1, 1, 1]},
    "thirdperson_lefthand": {"rotation": [45, 155, 0], "translation": [11.51, 7, 2.5],
                             "scale": [1, 1, 1]},
    "firstperson_righthand": {"rotation": [0, 180, -5], "translation": [-15, 3.25, -11],
                              "scale": [1.25, 1.25, 1.25]},
    "firstperson_lefthand": {"rotation": [0, 180, -5], "translation": [5, 5, -11],
                             "scale": [1.25, 1.25, 1.25]},
    "gui": {"rotation": [15, -25, -5], "translation": [2, 3, 0], "scale": [0.65, 0.65, 0.65]},
    "fixed": {"rotation": [0, 180, 0], "translation": [-4.5, 4.5, -5], "scale": [0.55, 0.55, 0.55]},
    "on_shelf": {"rotation": [0, 0, 0], "translation": [11, 18.5, 8.7], "scale": [1.4, 1.4, 1.4]},
    "ground": {"rotation": [0, 0, 0], "translation": [2, 4, 2], "scale": [0.25, 0.25, 0.25]},
}

# The vanilla shield is drawn by a SpecialModelRenderer, which renders the model in centred
# coordinates. A normal item model instead gets `translate(-0.5, -0.5, -0.5)` right before its
# quads are submitted, so vanilla's display numbers shove it half a block towards the camera.
# ItemStackRenderState applies the model's `transformation` AFTER the display transform and
# BEFORE that centring, so a +0.5 shift here cancels it exactly and the plain model ends up
# where the vanilla special renderer would put it.
CENTRE_COMPENSATION = {
    "left_rotation": [0.0, 0.0, 0.0, 1.0],
    "right_rotation": [0.0, 0.0, 0.0, 1.0],
    "scale": [1.0, 1.0, 1.0],
    "translation": [0.5, 0.5, 0.5],
}

VANILLA_PLATE_FRONT = 6.0  # vanilla plate spans z 6..7 in 0..16 item model space
VANILLA_CENTRE_Y = 8.0
# vanilla ShieldModel plate: addBox(-6, -11, -2, 12, 22, 1) in centred space, +8 to reach 0..16
VANILLA_PLATE = ((2.0, -3.0, 6.0), (14.0, 19.0, 7.0))

VANILLA_JAR = os.path.expanduser(
    "~") + "/.gradle/caches/fabric-loom/26.3/minecraft-merged.jar"


def load_bbmodel(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def extract_texture(model):
    for texture in model.get("textures", []):
        source = texture.get("source", "")

        if not source.startswith("data:image/png;base64,"):
            continue

        raw = base64.b64decode(source.split(",", 1)[1])
        image = Image.open(io.BytesIO(raw)).convert("RGBA")
        return image

    raise RuntimeError("bbmodel has no embedded png")


def box_from_element(element):
    return {
        "from": [round(v, 4) for v in element["from"]],
        "to": [round(v, 4) for v in element["to"]],
        "faces": {face: {"uv": value.get("uv", [0, 0, 1, 1]), "texture": "#0"}
                  for face, value in element.get("faces", {}).items()},
    }


def plate_and_handle(model):
    """Axis aligned plate and handle.

    Minecraft item model uvs live in a 0..16 space and are scaled to the texture at bake time,
    which is exactly how Blockbench stores them, so they are copied verbatim.
    """
    boxes = {}

    for element in model.get("elements", []):
        name = element.get("name")

        if name in ("plate", "handle") and not element.get("rotation"):
            boxes[name] = box_from_element(element)

    if "plate" not in boxes:
        raise RuntimeError("no axis aligned plate in the model")

    return boxes


# ----------------------------------------------------------------- placement
def shift(box, dx, dy, dz):
    box["from"] = [round(box["from"][0] + dx, 3), round(box["from"][1] + dy, 3),
                   round(box["from"][2] + dz, 3)]
    box["to"] = [round(box["to"][0] + dx, 3), round(box["to"][1] + dy, 3),
                 round(box["to"][2] + dz, 3)]
    return box


def place_on_vanilla(plate, others):
    """Snap the painted plate onto the vanilla shield box, and everything else with it.

    Blockbench lets the artist build anywhere in the grid; the item model origin is fixed at
    (8, 8, 8) with the visible face at min z. The plate is set to exactly vanilla's box
    (2,-3,6)-(14,19,7) so the shield has vanilla's size on screen; the painted uv is kept, so the
    art is only stretched a little vertically.
    """
    dy = VANILLA_CENTRE_Y - (plate["from"][1] + plate["to"][1]) / 2.0
    dz = VANILLA_PLATE_FRONT - plate["from"][2]

    for box in [plate] + others:
        shift(box, 0.0, dy, dz)

    plate["from"] = [VANILLA_PLATE[0][0], VANILLA_PLATE[0][1], VANILLA_PLATE[0][2]]
    plate["to"] = [VANILLA_PLATE[1][0], VANILLA_PLATE[1][1], VANILLA_PLATE[1][2]]

    return plate


def handle_to_back(handle, plate):
    """Put the grip behind the plate, on the side the player does not look at."""
    shift(handle, 0.0, 0.0, plate["to"][2] - handle["from"][2])
    return handle


# ----------------------------------------------------------------- spikes
def spike_boxes(plate):
    """Four clean spikes on the outer face of the plate.

    The painted spikes use free rotation, which json item models cannot express, so the shape is
    rebuilt as stepped boxes. The outer face is the -Z face: it is the side that faces the enemy
    and the side the player looks at in first person, so the spikes grow towards -Z. They are kept
    short so that in first person they read as studs instead of poking the camera in the eye.
    """
    y_from, y_to = plate["from"][1], plate["to"][1]
    z_outer = plate["from"][2]
    x_from, x_to = plate["from"][0], plate["to"][0]

    boxes = []
    inset_x = (x_to - x_from) * 0.22
    inset_y = (y_to - y_from) * 0.18

    spots = [
        (x_from + inset_x, y_from + inset_y),
        (x_to - inset_x - 1.6, y_from + inset_y),
        (x_from + inset_x, y_to - inset_y - 1.6),
        (x_to - inset_x - 1.6, y_to - inset_y - 1.6),
    ]

    for (bx, by) in spots:
        # base shard, then a thin tip so it reads as a point
        boxes.append({
            "from": [bx, by, z_outer - 1.2],
            "to": [bx + 1.6, by + 1.6, z_outer + 0.05],
            "faces": {face: {"uv": SPIKE_UV, "texture": "#0"}
                      for face in ("north", "south", "east", "west", "up", "down")},
        })
        boxes.append({
            "from": [bx + 0.35, by + 0.35, z_outer - 2.0],
            "to": [bx + 1.25, by + 1.25, z_outer - 1.1],
            "faces": {face: {"uv": SPIKE_UV, "texture": "#0"}
                      for face in ("north", "south", "east", "west", "up", "down")},
        })

    return boxes


def shield_elements(model, spikes=False):
    boxes = plate_and_handle(model)
    plate = boxes["plate"]
    handle = boxes.get("handle")

    place_on_vanilla(plate, [handle] if handle else [])
    elements = [plate]

    if handle:
        elements.append(handle_to_back(handle, plate))

    if spikes:
        elements += spike_boxes(elements[0])

    return elements


# ----------------------------------------------------------------- models
def model_from(boxes, name, blocking=False):
    texture = "%s:item/%s" % (NS, name)

    return {
        "ambientocclusion": False,
        "gui_light": "front",
        "textures": {"0": texture, "particle": texture},
        "display": DISPLAY_BLOCKING if blocking else DISPLAY,
        "elements": boxes,
    }


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)

    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2)
        f.write("\n")


def write_shield(name, boxes, texture):
    texture.save(ASSETS + "/textures/item/%s.png" % name)

    write_json(ASSETS + "/models/item/%s.json" % name, model_from(boxes, name))
    write_json(ASSETS + "/models/item/%s_blocking.json" % name, model_from(boxes, name, blocking=True))

    # the shield is drawn by our own SpecialModelRenderer (same path as the vanilla shield), so
    # the display transforms of the two models above apply unchanged and the geometry in them is
    # only kept for the offline preview tool. RenderTypes.entitySolid wants the full texture path,
    # the same form TextureAtlas.LOCATION_BLOCKS uses.
    special = {"type": "%s:shield" % NS,
               "texture": "%s:textures/item/%s.png" % (NS, name)}

    write_json(ASSETS + "/items/%s.json" % name, {
        "model": {
            "type": "minecraft:condition",
            "property": "minecraft:using_item",
            "on_false": {
                "type": "minecraft:special",
                "base": "%s:item/%s" % (NS, name),
                "model": special,
            },
            "on_true": {
                "type": "minecraft:special",
                "base": "%s:item/%s_blocking" % (NS, name),
                "model": special,
            },
        }
    })


# ----------------------------------------------------------------- materials
def vanilla_texture(name):
    with zipfile.ZipFile(VANILLA_JAR) as jar:
        raw = jar.read("assets/minecraft/textures/block/%s.png" % name)

    return Image.open(io.BytesIO(raw)).convert("RGBA")


def plate_rects(elements):
    """Pixel rectangles of the two big painted faces, in texture pixels."""
    plate = elements[0]
    rects = {}

    for face in ("north", "south"):
        uv = plate["faces"][face]["uv"]
        rects[face] = tuple(int(round(v * UV_SCALE)) for v in uv)

    return rects


def tile_obsidian():
    """The vanilla obsidian block texture, tiled over the whole shield unwrap."""
    obsidian = vanilla_texture("obsidian")
    image = Image.new("RGBA", (TEXTURE_PX, TEXTURE_PX), (0, 0, 0, 0))

    for y in range(TEXTURE_PX):
        for x in range(TEXTURE_PX):
            r, g, b, a = obsidian.getpixel((x % obsidian.width, y % obsidian.height))
            # obsidian reads almost black in game, lift the purple so the shape still shows
            image.putpixel((x, y), (min(255, r + 12), min(255, g + 8),
                                    min(255, int(b * 1.25) + 14), 255))

    return image


def outline(image, region, colour):
    x1, y1, x2, y2 = region

    for x in range(x1, x2):
        image.putpixel((x, y1), colour)
        image.putpixel((x, min(y2 - 1, TEXTURE_PX - 1)), colour)

    for y in range(y1, y2):
        image.putpixel((x1, y), colour)
        image.putpixel((min(x2 - 1, TEXTURE_PX - 1), y), colour)


def fill_patch(image, uv, colour):
    x1, y1, x2, y2 = (int(round(v * UV_SCALE)) for v in uv)

    for y in range(y1, y2):
        for x in range(x1, x2):
            image.putpixel((x, y), colour)


def build_obsidian_texture(elements):
    """Obsidian shield: real obsidian block texture, dark frame, lavender crystal spikes."""
    image = tile_obsidian()

    for region in plate_rects(elements).values():
        # a bevel so the flat plate still reads as a shield and not as a wall of obsidian
        outline(image, region, (58, 32, 92, 255))
        x1, y1, x2, y2 = region
        outline(image, (x1 + 1, y1 + 1, x2 - 1, y2 - 1), (6, 3, 14, 255))

    fill_patch(image, SPIKE_UV, (188, 162, 255, 255))
    return image


def build_glow_texture(base):
    """Glowstone edition: a glowing border wrapped around the shield face."""
    image = base.copy()

    # brighten everything a little, the way an embedded glowstone would
    for y in range(image.height):
        for x in range(image.width):
            r, g, b, a = image.getpixel((x, y))

            if a == 0:
                continue

            image.putpixel((x, y), (min(255, int(r * 1.12) + 20), min(255, int(g * 1.12) + 18),
                                    min(255, int(b * 1.05) + 8), a))

    return image


def frame_region(image, region, tile, thickness=2):
    x1, y1, x2, y2 = region

    for y in range(y1, y2):
        for x in range(x1, x2):
            edge = (x - x1 < thickness or x2 - 1 - x < thickness
                    or y - y1 < thickness or y2 - 1 - y < thickness)

            if not edge:
                continue

            r, g, b, a = tile.getpixel(((x - x1) % tile.width, (y - y1) % tile.height))
            image.putpixel((x, y), (r, g, b, 255))


def glow_variant(base_texture, elements):
    image = build_glow_texture(base_texture)
    glow = vanilla_texture("glowstone")

    # the glowstone rim goes on both big faces, so it does not matter which side you look at
    for key, region in plate_rects(elements).items():
        frame_region(image, region, glow)

    return image


RECIPES = {
    "obsidian_shield": {
        "type": "minecraft:crafting_shaped",
        "pattern": ["XOX", "XXX", " X "],
        "key": {"X": "minecraft:obsidian", "O": "%s:compressed_obsidian_alloy" % NS},
        "result": "%s:obsidian_shield" % NS,
    },
    "carbon_steel_shield": {
        "type": "minecraft:crafting_shaped",
        "pattern": ["XGX", "XXX", " X "],
        "key": {"X": "%s:carbon_steel_ingot" % NS, "G": "minecraft:glass"},
        "result": "%s:carbon_steel_shield" % NS,
    },
    "bedrock_shield_smithing": {
        "type": "minecraft:smithing_transform",
        "template": "%s:bedrock_shard" % NS,
        "base": "%s:obsidian_shield" % NS,
        "addition": "%s:bedrock_shard" % NS,
        "result": "%s:bedrock_shield" % NS,
    },
    # the glowstone edition upgrades as well: without this the only smithing recipe takes the
    # plain obsidian shield, so a glow shield had no way up and the glow was silently lost
    "bedrock_shield_glow_smithing": {
        "type": "minecraft:smithing_transform",
        "template": "%s:bedrock_shard" % NS,
        "base": "%s:obsidian_shield_glow" % NS,
        "addition": "%s:bedrock_shard" % NS,
        "result": "%s:bedrock_shield_glow" % NS,
    },
}

for base in ("carbon_steel_shield", "obsidian_shield", "bedrock_shield"):
    RECIPES[base + "_glow"] = {
        "type": "minecraft:crafting_shapeless",
        "ingredients": ["%s:%s" % (NS, base), "minecraft:glowstone"],
        "result": "%s:%s_glow" % (NS, base),
    }

SHIELD_NAMES = ["carbon_steel_shield", "obsidian_shield", "bedrock_shield",
                "carbon_steel_shield_glow", "obsidian_shield_glow", "bedrock_shield_glow"]


def main():
    carbon = load_bbmodel(os.path.join(DESKTOP, "碳钢盾牌.bbmodel"))
    bedrock = load_bbmodel(os.path.join(DESKTOP, "基岩盾牌.bbmodel"))

    carbon_texture = extract_texture(carbon)
    bedrock_texture = extract_texture(bedrock)

    carbon_elements = shield_elements(carbon)
    bedrock_elements = shield_elements(bedrock, spikes=True)
    obsidian_elements = list(carbon_elements) + spike_boxes(carbon_elements[0])

    obsidian_texture = build_obsidian_texture(obsidian_elements)

    write_shield("carbon_steel_shield", carbon_elements, carbon_texture)
    write_shield("obsidian_shield", obsidian_elements, obsidian_texture)
    write_shield("bedrock_shield", bedrock_elements, bedrock_texture)

    write_shield("carbon_steel_shield_glow", carbon_elements,
                 glow_variant(carbon_texture, carbon_elements))
    write_shield("obsidian_shield_glow", obsidian_elements,
                 glow_variant(obsidian_texture, obsidian_elements))
    write_shield("bedrock_shield_glow", bedrock_elements,
                 glow_variant(bedrock_texture, bedrock_elements))

    for name, recipe in RECIPES.items():
        body = dict(recipe)
        result = body.pop("result")
        body["result"] = {"id": result, "count": 1}
        write_json(DATA + "/recipe/%s.json" % name, body)

    # the vanilla shield is the only armour-like item in these tags, and the enchanting table and
    # every enchantment's supported_items list read them, so our shields have to join both
    values = ["%s:%s" % (NS, name) for name in SHIELD_NAMES]
    write_json(ROOT + "/data/minecraft/tags/item/durability.json", {"values": values})
    write_json(ROOT + "/data/minecraft/tags/item/enchantable/durability.json", {"values": values})

    print("carbon", carbon_elements[0]["from"], carbon_elements[0]["to"])
    print("bedrock", len(bedrock_elements), "elements")


main()
