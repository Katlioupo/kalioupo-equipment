"""Offline preview of an item model, so shield placement can be checked without launching the game.

It reproduces what Minecraft does:

    world = translation + rotation * scale * (p / 16 - 0.5)  +  hand offset

where p is the vertex in 0..16 item model space and the display transforms come from the item
model json (the ones Minecraft itself ships for the vanilla shield).

Usage:  python tools/preview_shield.py
Writes build/preview_shield.png with the vanilla shield next to ours.
"""
import json
import math
import os

import numpy as np
from PIL import Image, ImageDraw

W, H = 560, 380
FOV = 70.0
NEAR = 0.02

ROOT = "src/main/resources/assets/kalioupo_equipment"
VANILLA = os.path.join(os.path.dirname(os.getcwd()), "_vanilla_shield",
                       "assets/minecraft")

# the hand offset used by ItemInHandRenderer for a held item, calibrated against an in game
# screenshot of the 0.31.5 shield (centred, a bit below the crosshair)
HAND_TRANSLATE = (0.56, -0.52, -0.50)


# ----------------------------------------------------------------- math
def rotation(rx, ry, rz):
    """Minecraft builds the display rotation as JOML rotationXYZ (X, then Y, then Z)."""
    cx, cy, cz = (math.cos(math.radians(a)) for a in (rx, ry, rz))
    sx, sy, sz = (math.sin(math.radians(a)) for a in (rx, ry, rz))

    mx = np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]], dtype=float)
    my = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]], dtype=float)
    mz = np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]], dtype=float)

    return mz @ my @ mx


def face_corners(box, face):
    """The four corners of a box face, in texture order: bottom left, bottom right, top right, top left."""
    (x1, y1, z1), (x2, y2, z2) = box["from"], box["to"]

    if face == "north":
        return [(x1, y1, z1), (x2, y1, z1), (x2, y2, z1), (x1, y2, z1)]
    if face == "south":
        return [(x2, y1, z2), (x1, y1, z2), (x1, y2, z2), (x2, y2, z2)]
    if face == "east":
        return [(x2, y1, z2), (x2, y1, z1), (x2, y2, z1), (x2, y2, z2)]
    if face == "west":
        return [(x1, y1, z1), (x1, y1, z2), (x1, y2, z2), (x1, y2, z1)]
    if face == "up":
        return [(x1, y2, z2), (x2, y2, z2), (x2, y2, z1), (x1, y2, z1)]
    if face == "down":
        return [(x1, y1, z1), (x2, y1, z1), (x2, y1, z2), (x1, y1, z2)]

    raise ValueError(face)


def shade(face):
    return {"up": 1.0, "down": 0.45, "north": 0.86, "south": 0.72,
            "east": 0.78, "west": 0.92}[face]


def find_coeffs(dst, src):
    """Perspective coefficients mapping dst quad -> src quad (PIL output -> input)."""
    matrix = []
    for (dx, dy), (sx, sy) in zip(dst, src):
        matrix.append([dx, dy, 1, 0, 0, 0, -sx * dx, -sx * dy])
        matrix.append([0, 0, 0, dx, dy, 1, -sy * dx, -sy * dy])

    a = np.array(matrix, dtype=float)
    b = np.array(src, dtype=float).reshape(8)

    return np.linalg.solve(a, b)


# ----------------------------------------------------------------- render
FACE_COLOUR = {
    "north": (70, 120, 255, 255),   # the face the player sees
    "south": (230, 70, 70, 255),
    "east": (70, 200, 90, 255),
    "west": (235, 210, 70, 255),
    "up": (230, 90, 230, 255),
    "down": (70, 220, 220, 255),
}


def render(elements, texture, display, hand=(0.0, 0.0, 0.0), uv_scale=4.0, texture_px=64,
           flat=False, offset=(0.0, 0.0, 0.0), body=None):
    image = Image.new("RGBA", (W, H), (18, 20, 28, 255))

    rot = rotation(*display["rotation"])
    scale = np.array(display["scale"], dtype=float)
    translate = np.array(display["translation"], dtype=float) / 16.0
    hand = np.array(hand, dtype=float) + np.array(offset, dtype=float)

    half = np.array([8.0, 8.0, 8.0])
    f = (H / 2.0) / math.tan(math.radians(FOV) / 2.0)

    if body is not None:
        # a stand in for the player so third person placement can be judged
        (bx1, by1, bz1), (bx2, by2, bz2) = body
        corners = [(x, y, z) for x in (bx1, bx2) for y in (by1, by2) for z in (bz1, bz2)]
        projected = []
        for (x, y, z) in corners:
            sx = W / 2.0 + f * x / -z
            sy = H / 2.0 - f * y / -z
            projected.append((sx, sy))
        draw = ImageDraw.Draw(image)
        # only the three faces that look at the camera
        for quad in ((4, 5, 7, 6), (2, 3, 7, 6), (3, 1, 5, 7)):
            draw.polygon([projected[i] for i in quad], fill=(120, 124, 140, 255),
                         outline=(60, 62, 72, 255))

    quads = []

    for box in elements:
        for face, data in box["faces"].items():
            corners = face_corners(box, face)
            u1, v1, u2, v2 = data["uv"]

            screen = []
            depth = 0.0
            behind = False

            for corner in corners:
                k = np.array(corner, dtype=float)
                # the item definition carries transformation +0.5, which cancels the -0.5 the
                # game applies before submitting a normal item model, so the model ends up in
                # centred coordinates like a special renderer
                local = np.array([k[0], k[1], k[2]]) / 16.0
                world = translate + rot @ (scale * local) + hand

                if world[2] > -NEAR:
                    behind = True
                    break

                f = (H / 2.0) / math.tan(math.radians(FOV) / 2.0)
                sx = W / 2.0 + f * world[0] / -world[2]
                sy = H / 2.0 - f * world[1] / -world[2]
                screen.append((sx, sy))
                depth += -world[2]

            if behind:
                continue

            src = [(u1 * uv_scale, v1 * uv_scale), (u2 * uv_scale, v1 * uv_scale),
                   (u2 * uv_scale, v2 * uv_scale), (u1 * uv_scale, v2 * uv_scale)]

            quads.append((depth / 4.0, screen, src, face))

    quads.sort(key=lambda q: -q[0])

    for _, screen, src, face in quads:
        factor = shade(face)

        if flat:
            colour = tuple(int(c * factor) for c in FACE_COLOUR[face][:3]) + (255,)
            mask = Image.new("L", (W, H), 0)
            ImageDraw.Draw(mask).polygon(screen, fill=255)
            patch = Image.new("RGBA", (W, H), colour)
            image.paste(patch, (0, 0), mask)
            ImageDraw.Draw(image).line(screen + [screen[0]], fill=(0, 0, 0, 160), width=1)
            continue

        try:
            coeffs = find_coeffs(screen, src)
        except np.linalg.LinAlgError:
            continue

        warped = texture.transform((W, H), Image.PERSPECTIVE, coeffs,
                                   resample=Image.BILINEAR, fillcolor=(0, 0, 0, 0))

        mask = Image.new("L", (W, H), 0)
        ImageDraw.Draw(mask).polygon(screen, fill=255)

        rgb = warped.convert("RGBA")
        arr = np.array(rgb, dtype=np.float32)
        arr[:, :, :3] *= factor
        warped = Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA")

        image.paste(warped, (0, 0), Image.composite(warped.split()[3], Image.new("L", (W, H), 0), mask))

    return image


# ----------------------------------------------------------------- models
def vanilla_elements():
    def box(frm, to, tex):
        size = [to[i] - frm[i] for i in range(3)]
        w, h, d = size
        u, v = tex

        def rect(x, y, rw, rh):
            return [x, y, x + rw, y + rh]

        return {
            "from": frm, "to": to,
            "faces": {
                "down": {"uv": rect(u + d, v, w, d)},
                "up": {"uv": rect(u + d + w, v, w, d)},
                "east": {"uv": rect(u, v + d, d, h)},
                "north": {"uv": rect(u + d, v + d, w, h)},
                "west": {"uv": rect(u + d + w, v + d, d, h)},
                "south": {"uv": rect(u + d + w + d, v + d, w, h)},
            },
        }

    # vanilla ShieldModel: plate addBox(-6,-11,-2,12,22,1), handle addBox(-1,-3,-1,2,6,6), +8 to centre
    return [box([2, -3, 6], [14, 19, 7], (0, 0)),
            box([7, 5, 7], [9, 11, 13], (26, 0))]


def our_elements(name):
    with open(os.path.join(ROOT, "models/item/%s.json" % name), encoding="utf-8") as f:
        return json.load(f)["elements"], json.load(f)["display"]


def display_of(name, blocking=False):
    suffix = "_blocking" if blocking else ""
    with open(os.path.join(ROOT, "models/item/%s%s.json" % (name, suffix)), encoding="utf-8") as f:
        return json.load(f)["display"][CONTEXT]


CONTEXT = "firstperson_righthand"


def load_model(name, blocking=False):
    suffix = "_blocking" if blocking else ""
    with open(os.path.join(ROOT, "models/item/%s%s.json" % (name, suffix)), encoding="utf-8") as f:
        return json.load(f)


def main():
    our_tex = Image.open(os.path.join(ROOT, "textures/item/obsidian_shield.png")).convert("RGBA")
    model = load_model("obsidian_shield", False)
    blocking = load_model("obsidian_shield", True)

    # candidate first person transforms, all rendered with the calibrated hand offset
    candidates = [
        ("1p hold", model["elements"], model["display"]["firstperson_righthand"], (0, 0, 0), None),
        ("1p block", blocking["elements"], blocking["display"]["firstperson_righthand"], (0, 0, 0), None),
    ]

    sheet = Image.new("RGBA", (W * 2 + 12, H + 6), (0, 0, 0, 255))

    for i, (title, elements, hold, offset, body) in enumerate(candidates):
        image = render(elements, our_tex, hold, HAND_TRANSLATE, offset=offset, body=body)
        x = (i % 2) * (W + 12)
        sheet.paste(image, (x, 0))
        ImageDraw.Draw(sheet).text((x + 8, 6), title, fill=(255, 220, 90, 255))

    sheet.save("build/preview_shield.png")

    # third person: same item, seen from behind the player
    third = model["display"]["thirdperson_righthand"]
    body_box = ((-0.3, -0.9, -2.6), (0.3, 1.0, -2.0))

    tp = Image.new("RGBA", (W * 2 + 12, H + 6), (0, 0, 0, 255))
    variants = [
        ("3p vanilla transform", {"rotation": [0, 90, 0], "translation": [10, 6, -4], "scale": [1, 1, 1]}),
        ("3p our transform", third),
    ]

    for i, (title, display) in enumerate(variants):
        image = render(model["elements"], our_tex, display, (0, 0, 0),
                       offset=(0.45, -0.2, -2.2), body=body_box)
        x = (i % 2) * (W + 12)
        tp.paste(image, (x, 0))
        ImageDraw.Draw(tp).text((x + 8, 6), title, fill=(255, 220, 90, 255))

    tp.save("build/preview_shield_3p.png")
    print("wrote build/preview_shield.png + build/preview_shield_3p.png")


main()
