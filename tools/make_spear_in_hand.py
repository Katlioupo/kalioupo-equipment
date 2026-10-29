"""Give every spear a vanilla style in-hand sprite.

Vanilla 26.3 draws spears with a second, much longer sprite and a dedicated display transform:

    items/<spear>.json          select on minecraft:display_context
                                gui/ground/fixed/on_shelf -> the 16x16 icon
                                anything else             -> <spear>_in_hand
    models/item/spear_in_hand   the in hand rotation/translation/scale

The in hand sprite is not a bigger version of the icon: it is 32x32 and the spear is drawn
diagonally from the top left tip to the bottom right butt, so the blade sits at the top left
corner and the shaft runs corner to corner. Drawing it horizontally instead makes the item
read as a stick lying across the screen, so this script recolours the vanilla sprite with each
tier palette instead of inventing new geometry. The recolour pipeline is the exact same one
make_equipment.py uses for the 16x16 icons, so icon and in-hand sprite always match.
"""
import glob
import json
import os
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import make_equipment as me  # noqa: E402  (import after sys.path so the tool runs from anywhere)

ASSETS = "src/main/resources/assets/kalioupo_equipment"
ICONS = ASSETS + "/textures/item"
MODELS = ASSETS + "/models/item"
ITEMS = ASSETS + "/items"
NS = "kalioupo_equipment"

# the vanilla 32x32 held sprite, extracted from the 26.3 client jar
VANILLA_IN_HAND = "build/gui/vanilla_items/iron_spear_in_hand.png"

# gui-ish contexts keep the plain icon, everything else uses the long in hand sprite
ICON_CONTEXTS = ["gui", "ground", "fixed", "on_shelf"]


SCULK_BRIGHT = (126, 230, 240)
SCULK_DEEP = (14, 62, 74)


def sculk_glow(image, every=11):
    """A few sculk glow spots on an already recoloured sprite.

    The icon pipeline paints the whole rim in sculk colours, which works on a chunky 16x16 icon
    but ruins a 32x32 spear: the diagonal is almost entirely "rim", so every pixel ended up either
    bright cyan or near black and the spear read as a blue and black stick. Here the tier palette
    keeps the shape and sculk only adds sparse spores.
    """
    out = image.copy()

    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = out.getpixel((x, y))

            if a == 0:
                continue

            if (x * 7 + y * 13) % every == 0:
                out.putpixel((x, y), SCULK_BRIGHT + (255,))
            elif (x * 17 + y * 5) % (every * 3) == 0:
                out.putpixel((x, y), SCULK_DEEP + (255,))

    return out


def in_hand_sprite(material):
    """The held sprite for one tier, built with the same branches as the item icons."""
    src = Image.open(VANILLA_IN_HAND).convert("RGBA")
    stops = me.MATERIALS[material][0]

    if material in me.SCULK_FROM:
        # recolour with the tier palette, then sprinkle sculk spores on top (see sculk_glow)
        return sculk_glow(me.recolor(src, stops, material))

    if material in me.PURIFY_FROM:
        parent = in_hand_sprite(me.PURIFY_FROM[material])
        return me.sparkle(me.purify(parent))

    img = me.recolor(src, stops, material)
    if material in me.SPARKLE:
        img = me.sparkle(img, step=17)
    if material in me.RIM:
        img = me.rim_light(img)
    return img


def main():
    icons = sorted(glob.glob(ICONS + "/*_spear.png"))
    made = 0

    for path in icons:
        item = os.path.basename(path)[:-4]
        material = item[:-len("_spear")]

        if material not in me.MATERIALS:
            continue

        in_hand_sprite(material).save("%s/%s_in_hand.png" % (ICONS, item))
        made += 1

        me.write("%s/%s_in_hand.json" % (MODELS, item), {
            "parent": "minecraft:item/spear_in_hand",
            "textures": {"layer0": "%s:item/%s_in_hand" % (NS, item)},
        })

        # the plain icon model uses the normal item transforms, the hand poses come from the
        # in hand model
        me.write("%s/%s.json" % (MODELS, item), {
            "parent": "minecraft:item/generated",
            "textures": {"layer0": "%s:item/%s" % (NS, item)},
        })

        me.write("%s/%s.json" % (ITEMS, item), {
            "model": {
                "type": "minecraft:select",
                "property": "minecraft:display_context",
                "cases": [{
                    "when": ICON_CONTEXTS,
                    "model": {"type": "minecraft:model", "model": "%s:item/%s" % (NS, item)},
                }],
                "fallback": {"type": "minecraft:model",
                             "model": "%s:item/%s_in_hand" % (NS, item)},
            },
            "swap_animation_scale": 1.95,
        })

    print("spears updated:", made)


if __name__ == "__main__":
    main()
