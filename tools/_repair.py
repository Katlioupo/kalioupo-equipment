import re

p = "tools/make_equipment.py"
t = open(p, encoding="utf-8").read()

marker = '        write(ROOT + "/data/minecraft/tags/item/%s.json" % tag, {"values": values'
idx = t.find(marker)
if idx == -1:
    print("marker not found; file may already be healthy")
else:
    head = t[:idx] + marker + "})\n\n"
    tail = '''    print("materials", len(MATERIALS), "items", sum(len(v[1]) for v in MATERIALS.values()),
          "tags", len(vanilla_tags))


pieces = ["helmet", "chestplate", "leggings", "boots", "sword",
          "pickaxe", "axe", "shovel", "hoe", "spear"]
SMITHING["bedrock"] = {
    piece: ("kalioupo_equipment:bedrock_shard", "kalioupo_equipment:obsidian_" + piece) for piece in pieces
}

main()
'''
    t = head + tail
    open(p, "w", encoding="utf-8").write(t)
    print("repaired")

# make sure the bedrock material entry exists
if '"bedrock": (' not in t:
    anchor = '''        "compressed_obsidian_alloy",
    ),
}
'''
    block = '''        "compressed_obsidian_alloy",
    ),
    "bedrock": (
        [(0.00, (10, 10, 12)), (0.34, (44, 44, 50)), (0.60, (96, 96, 106)), (0.82, (160, 160, 172)), (1.00, (238, 238, 248))],
        ["helmet", "chestplate", "leggings", "boots", "sword", "pickaxe", "axe", "shovel", "hoe", "spear"],
        "bedrock_shard",
    ),
}
'''
    t = t.replace(anchor, block, 1)
    open(p, "w", encoding="utf-8").write(t)
    print("material added")
