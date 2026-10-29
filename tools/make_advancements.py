"""Advancement generator.

Fourteen advancements following the tier ladder, trigger driven by vanilla predicates so no
server code has to award them.
"""
import json
import os

ROOT = "src/main/resources/data/kalioupo_equipment/advancement"
NS = "kalioupo_equipment"
PREFIX = "advancements.kalioupo_equipment."

ALL_ALLOYS = [
    "quartz_steel_ingot", "copper_alloy_ingot", "rose_gold_ingot", "prismarine_alloy_ingot",
    "amethyst_alloy_ingot", "phantom_ingot", "compressed_obsidian_alloy",
    "echo_alloy_ingot", "starlight_alloy_ingot",
]

ARMOR = ["helmet", "chestplate", "leggings", "boots"]
TOOLS = ["sword", "pickaxe", "axe", "shovel", "hoe", "spear"]


def pieces(material):
    return ["%s_%s" % (material, p) for p in ARMOR + TOOLS]


# id -> (parent, icon, frame, [items])
ADVANCEMENTS = {
    "better_than_iron": (None, "carbon_steel_ingot", "task", ["carbon_steel_ingot"]),
    "stronger_metal": ("better_than_iron", "copper_alloy_ingot", "goal", ALL_ALLOYS),
    "quartz_usable": ("stronger_metal", "quartz_steel_sword", "task", ["quartz_steel_ingot"]),
    "copper_tough": ("stronger_metal", "copper_alloy_chestplate", "task", ["copper_alloy_ingot"]),
    "rose_gold_what": ("stronger_metal", "rose_gold_chestplate", "task", ["rose_gold_ingot"]),
    "specialised": ("stronger_metal", "prismarine_helmet", "task", ["prismarine_alloy_ingot"]),
    "magic_power": ("stronger_metal", "amethyst_sword", "goal", ["amethyst_sword"]),
    "aerodynamics": ("stronger_metal", "phantom_chestplate", "goal", ["phantom_ingot"]),
    "obsidian_power": ("stronger_metal", "obsidian_chestplate", "goal",
                       ["obsidian_%s" % p for p in ARMOR]),
    "bedrock_chiseler": ("obsidian_power", "bedrock_shard", "task", ["bedrock_shard"]),
    "warden_slayer": ("bedrock_chiseler", "echo_alloy_ingot", "goal", ["echo_alloy_ingot"]),
    "road_to_godhood": ("bedrock_chiseler", "bedrock_chestplate", "goal", pieces("bedrock")),
    "peak": ("road_to_godhood", "echo_chestplate", "challenge", pieces("echo")),
    "king_of_the_world": ("peak", "starlight_sword", "challenge", pieces("starlight")),
}

TITLES = {
    "better_than_iron": ("比铁好", "Better Than Iron"),
    "stronger_metal": ("更强大的金属", "A Stronger Metal"),
    "quartz_usable": ("这...能用嘛？", "This... Works?"),
    "copper_tough": ("韧性提升！", "Toughness Up!"),
    "rose_gold_what": ("！？玫瑰金？！", "!? Rose Gold?!"),
    "specialised": ("特化材料", "Specialised Material"),
    "magic_power": ("是魔法的力量！", "It's Magic!"),
    "aerodynamics": ("空气动力学", "Aerodynamics"),
    "obsidian_power": ("黑曜石力量", "Obsidian Power"),
    "bedrock_chiseler": ("基岩凿取者", "Bedrock Chiseler"),
    "warden_slayer": ("坚守者杀手", "Warden Slayer"),
    "road_to_godhood": ("登神之路", "Road to Godhood"),
    "peak": ("登峰造极！", "Peak Perfection!"),
    "king_of_the_world": ("君临天下", "King of the World"),
}

DESCRIPTIONS = {
    "better_than_iron": ("冶炼出第一块碳钢锭，比铁好一点，就一点点。",
                         "Smelt your first carbon steel ingot. Better than iron, by a little."),
    "stronger_metal": ("第一次合成任意一种合金锭。",
                       "Craft any alloy ingot for the first time."),
    "quartz_usable": ("合成石英钢。耐久很低，但真的很疼。",
                      "Craft quartz steel. Fragile, but it really bites."),
    "copper_tough": ("合成铜合金，装甲更扎实了。",
                     "Craft copper alloy and your plating gets sturdier."),
    "rose_gold_what": ("合成玫瑰金。好看，而且附魔性极高。",
                       "Craft rose gold. Pretty, and absurdly enchantable."),
    "specialised": ("合成海晶合金，水下呼吸不再是问题。",
                    "Craft prismarine alloy and breathing underwater stops being a problem."),
    "magic_power": ("打造紫水晶剑，充能后按 V 引下雷电。",
                    "Forge the amethyst sword, charge it, then press V for lightning."),
    "aerodynamics": ("冶炼幻翼锭，滑翔的起点。",
                     "Smelt a phantom ingot, the start of gliding."),
    "obsidian_power": ("锻造任意一件黑曜石装备。",
                       "Forge any piece of obsidian gear."),
    "bedrock_chiseler": ("用钻石镐凿击基岩三十次，得到一颗基岩碎片。",
                         "Chisel bedrock thirty times with a diamond pickaxe."),
    "warden_slayer": ("冶炼出回响合金锭，古城的回报。",
                      "Smelt an echo alloy ingot, the ancient city's reward."),
    "road_to_godhood": ("锻造任意一件基岩强化装备。",
                        "Forge any piece of bedrock reinforced gear."),
    "peak": ("锻造任意一件回响强化装备。",
             "Forge any piece of echo reinforced gear."),
    "king_of_the_world": ("锻造任意一件星辉装备，你站到了终点。",
                          "Forge any piece of starlight gear. You reached the end."),
}


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def main():
    os.makedirs(ROOT, exist_ok=True)

    # drop files from the previous layout so no stale advancement lingers
    for name in os.listdir(ROOT):
        if name.endswith(".json"):
            os.remove(os.path.join(ROOT, name))

    for adv, (parent, icon, frame, items) in ADVANCEMENTS.items():
        # impossible on purpose: the mod awards these from code, which also covers items the
        # player already owned before the advancement existed
        criteria = {"awarded": {"trigger": "minecraft:impossible"}}

        display = {
            "icon": {"id": "%s:%s" % (NS, icon)},
            "title": {"translate": PREFIX + adv + ".title"},
            "description": {"translate": PREFIX + adv + ".description"},
            "frame": frame,
            "show_toast": True,
            "announce_to_chat": frame == "challenge",
            "hidden": False,
        }

        body = {
            "criteria": criteria,
            "display": display,
            "sends_telemetry_event": False,
        }

        if parent is None:
            display["background"] = "minecraft:gui/advancements/backgrounds/stone"
        else:
            body["parent"] = "%s:%s" % (NS, parent)

        write(os.path.join(ROOT, adv + ".json"), body)

    for path, index in (("src/main/resources/assets/kalioupo_equipment/lang/zh_cn.json", 0),
                        ("src/main/resources/assets/kalioupo_equipment/lang/en_us.json", 1)):
        with open(path, encoding="utf-8") as f:
            lang = json.load(f)

        # clear the old set first
        for key in [k for k in lang if k.startswith(PREFIX)]:
            del lang[key]

        for adv in ADVANCEMENTS:
            lang[PREFIX + adv + ".title"] = TITLES[adv][index]
            lang[PREFIX + adv + ".description"] = DESCRIPTIONS[adv][index]

        with open(path, "w", encoding="utf-8") as f:
            json.dump(lang, f, indent=2, ensure_ascii=False)
            f.write("\n")

    print("advancements", len(ADVANCEMENTS))


main()
