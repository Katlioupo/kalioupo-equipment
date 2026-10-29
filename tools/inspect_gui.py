from PIL import Image

a = Image.open("build/gui/furnace.png").convert("RGBA")
b = Image.open("src/main/resources/assets/kalioupo_equipment/textures/gui/smelter.png").convert("RGBA")


def sym(p):
    if p[3] == 0:
        return "."
    r = p[0]
    if r < 120:
        return "#"
    if r < 180:
        return "o"
    if r < 215:
        return "+"
    if r >= 250:
        return "W"
    return " "


def show(img, x0, x1, y0, y1, label):
    print("---", label)
    for y in range(y0, y1):
        print("%3d %s" % (y, "".join(sym(img.getpixel((x, y))) for x in range(x0, x1))))


show(a, 54, 76, 15, 37, "vanilla slot")
show(b, 6, 28, 15, 37, "ours slot")
show(b, 40, 76, 30, 56, "ours flame+arrow area")
