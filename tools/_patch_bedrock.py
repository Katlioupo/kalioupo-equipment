import re

p = "tools/make_equipment.py"
t = open(p, encoding="utf-8").read()

mat_block = '''    "bedrock": (
        [(0.00, (10, 10, 12)), (0.34, (44, 44, 50)), (0.60, (96, 96, 106)), (0.82, (160, 160, 172)), (1.00, (238, 238, 248))],
        ["helmet", "chestplate", "leggings", "boots", "sword", "pickaxe", "axe", "shovel", "hoe", "spear"],
        "bedrock_shard",
    ),
}
'''

anchor = '''        "compressed_obsidian_alloy",
    ),
}
'''
if mat_block.strip() not in t:
    t = t.replace(anchor, anchor[:-2] + mat_block, 1)

pieces = ["helmet", "chestplate", "leggings", "boots", "sword",
          "pickaxe", "axe", "shovel", "hoe", "spear"]

smith_lines = ["    \"bedrock\": {"]
for piece in pieces:
    smith_lines.append(
        '        "%s": ("kalioupo_equipment:bedrock_shard", "kalioupo_equipment:obsidian_%s"),' % (piece, piece)
    )
smith_lines.append("    },")
smith_block = "\n".join(smith_lines) + "\n}\n"

if '"bedrock": {' not in t:
    index = t.rstrip().rfind("}")
    t = t.rstrip()[:index] + smith_block

open(p, "w", encoding="utf-8").write(t)
print("generator patched")
