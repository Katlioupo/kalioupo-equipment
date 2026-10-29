import re

p = "tools/make_equipment.py"
t = open(p, encoding="utf-8").read()

# 1. load the bedrock block texture and blend it into the palette mapping
if "BEDROCK_TEX" not in t:
    t = t.replace(
        'NS = "kalioupo_equipment"',
        'NS = "kalioupo_equipment"\n\n'
        'try:\n'
        '    BEDROCK_TEX = Image.open("build/gui/bedrock.png").convert("RGBA")\n'
        'except Exception:\n'
        '    BEDROCK_TEX = None'
    )

    t = t.replace(
        "def recolor(image, stops):",
        "def recolor(image, stops, mottled=False):"
    )
    t = t.replace(
        "            lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0\n",
        "            lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0\n"
        "            if mottled and BEDROCK_TEX is not None:\n"
        "                br, bg, bb, _ = BEDROCK_TEX.getpixel(((x * 5 + 3) % 16, (y * 7 + 5) % 16))\n"
        "                bl = (0.299 * br + 0.587 * bg + 0.114 * bb) / 255.0\n"
        "                bl = max(0.0, min(1.0, (bl - 0.13) / 0.46))\n"
        "                lum = 0.55 * lum + 0.45 * bl\n"
        "                lum = max(0.0, min(1.0, (lum - 0.5) * 1.45 + 0.5))\n"
    )

# 2. pass the flag for the bedrock material everywhere a texture is recoloured
t = t.replace('img = recolor(src, stops)', 'img = recolor(src, stops, material == "bedrock")')
t = t.replace('wings = recolor(Image.open("build/gui/vanilla_items/elytra_item.png").convert("RGBA"), stops)',
              'wings = recolor(Image.open("build/gui/vanilla_items/elytra_item.png").convert("RGBA"), stops)')
t = t.replace('recolor(src, stops).save(out)', 'recolor(src, stops, material == "bedrock").save(out)')
t = t.replace('wings_img = recolor(wings_src, stops)', 'wings_img = recolor(wings_src, stops, material == "bedrock")')

# 3. hard black / white palette for bedrock
t = re.sub(
    r'"bedrock": \(\n\s*\[\(0\.00, \(10, 10, 12\)\).*?\),',
    '"bedrock": (\n        [(0.00, (6, 6, 8)), (0.22, (26, 26, 30)), (0.48, (96, 96, 104)), '
    '(0.72, (188, 188, 198)), (0.90, (236, 236, 244)), (1.00, (255, 255, 255))],',
    t,
    count=1,
    flags=re.S,
)

open(p, "w", encoding="utf-8").write(t)
print("mottled bedrock textures enabled")
