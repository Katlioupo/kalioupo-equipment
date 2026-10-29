from PIL import Image
import sys

for name in ["iron_ingot", "nether_star", "amethyst_shard", "echo_shard", "bedrock", "phantom_membrane"]:
    img = Image.open("build/gui/%s.png" % name).convert("RGBA")
    print("=== %s %s" % (name, img.size))
    ramp = " .:-=+*#%@"
    for y in range(img.height):
        row = ""
        for x in range(img.width):
            r, g, b, a = img.getpixel((x, y))
            if a < 40:
                row += " "
            else:
                lum = int((0.299 * r + 0.587 * g + 0.114 * b) / 255 * 9)
                row += ramp[max(0, min(9, lum))]
        print("%2d|%s|" % (y, row))
