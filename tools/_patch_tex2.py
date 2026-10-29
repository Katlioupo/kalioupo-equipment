import re
import zipfile
import shutil

# grab the vanilla obsidian block texture
jar = r"C:\Program Files\PCL 正式版 2.12.2\.minecraft\versions\26.3-Fabric 0.19.5\26.3-Fabric 0.19.5.jar"
z = zipfile.ZipFile(jar)
with z.open("assets/minecraft/textures/block/obsidian.png") as f, open("build/gui/obsidian.png", "wb") as o:
    shutil.copyfileobj(f, o)

p = "tools/make_equipment.py"
t = open(p, encoding="utf-8").read()

# load both block textures
if "OBSIDIAN_TEX" not in t:
    t = t.replace(
        'def recolor(image, stops, mottled=False):',
        'try:\n'
        '    OBSIDIAN_TEX = Image.open("build/gui/obsidian.png").convert("RGBA")\n'
        'except Exception:\n'
        '    OBSIDIAN_TEX = None\n\n\n'
        'def recolor(image, stops, mottled=False):'
    )

# replace the mottle block with a version that honours the material
old = re.search(r"\n            if mottled and BEDROCK_TEX is not None:.*?lum = max\(0\.0, min\(1\.0, \(lum - 0\.5\) \* 1\.45 \+ 0\.5\)\)\n", t, re.S)
new = """
            if mottled and BEDROCK_TEX is not None:
                if mottled == "obsidian" and OBSIDIAN_TEX is not None:
                    tex, weight, contrast = OBSIDIAN_TEX, 0.42, 1.30
                else:
                    tex, weight, contrast = BEDROCK_TEX, 0.55, 1.35
                # stretch the sample horizontally: bedrock reads as horizontal bands
                sx = x % 16
                sy = (y // 2 + x % 3) % 16
                br, bg, bb, _ = tex.getpixel((sx, sy))
                bl = (0.299 * br + 0.587 * bg + 0.114 * bb) / 255.0
                bl = max(0.0, min(1.0, (bl - 0.11) / 0.55))
                lum = (1.0 - weight) * lum + weight * bl
                lum = max(0.0, min(1.0, (lum - 0.5) * contrast + 0.5))
"""
if old:
    t = t[:old.start()] + new + t[old.end():]

# pass the material name through instead of a boolean
t = t.replace('material == "bedrock")', 'material)')
t = t.replace('def recolor(image, stops, mottled=False):', 'def recolor(image, stops, mottled=None):')
t = t.replace('if mottled and BEDROCK_TEX is not None:', 'if mottled in ("bedrock", "obsidian") and BEDROCK_TEX is not None:')

# heavier, less white bedrock palette; add obsidian palette tuning
t = re.sub(
    r'"bedrock": \(\n\s*\[\(0\.00.*?\),\n',
    '"bedrock": (\n        [(0.00, (8, 8, 10)), (0.26, (34, 34, 40)), (0.52, (96, 96, 104)), '
    '(0.74, (162, 162, 172)), (0.90, (210, 210, 220)), (1.00, (246, 246, 252))],\n',
    t, count=1, flags=re.S,
)

open(p, "w", encoding="utf-8").write(t)
print("texture patch applied")
