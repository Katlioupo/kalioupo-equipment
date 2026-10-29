"""Recipe unlock advancements.

Every recipe needs a hidden advancement that unlocks it in the recipe book, otherwise the
recipe only appears after the player has already crafted the item once.
"""
import json
import os

ROOT = "src/main/resources"
RECIPES = ROOT + "/data/kalioupo_equipment/recipe"
OUT = ROOT + "/data/kalioupo_equipment/advancement/recipes"
NS = "kalioupo_equipment"

# ingredients we never key an unlock on, they are too common to be a milestone
IGNORED = {"minecraft:stick", "minecraft:glass", "minecraft:glowstone"}


def ingredients(body):
    found = []

    if body.get("type") == "minecraft:crafting_shaped":
        for value in body.get("key", {}).values():
            if isinstance(value, str):
                found.append(value)
    elif body.get("type") == "minecraft:crafting_shapeless":
        for value in body.get("ingredients", []):
            if isinstance(value, str):
                found.append(value)
    elif body.get("type") == "minecraft:smithing_transform":
        for field in ("template", "base", "addition"):
            value = body.get(field)
            if isinstance(value, str):
                found.append(value)

    return [i for i in found if i not in IGNORED]


def main():
    os.makedirs(OUT, exist_ok=True)

    for name in os.listdir(OUT):
        if name.endswith(".json"):
            os.remove(os.path.join(OUT, name))

    written = 0

    for filename in sorted(os.listdir(RECIPES)):
        if not filename.endswith(".json"):
            continue

        with open(os.path.join(RECIPES, filename), encoding="utf-8") as f:
            body = json.load(f)

        items = ingredients(body)

        if not items:
            continue

        # prefer one of our own items, that is the real gate of the recipe
        own = [i for i in items if i.startswith(NS + ":")]
        gate = (own or items)[0]
        recipe_id = "%s:%s" % (NS, filename[:-5])

        advancement = {
            "parent": "minecraft:recipes/root",
            "criteria": {
                "has_ingredient": {
                    "trigger": "minecraft:inventory_changed",
                    "conditions": {"items": [{"items": gate}]}
                },
                "has_the_recipe": {
                    "trigger": "minecraft:recipe_unlocked",
                    "conditions": {"recipes": recipe_id}
                }
            },
            "requirements": [["has_the_recipe", "has_ingredient"]],
            "rewards": {"recipes": [recipe_id]}
        }

        with open(os.path.join(OUT, filename), "w", encoding="utf-8") as f:
            json.dump(advancement, f, indent=2, ensure_ascii=False)
            f.write("\n")

        written += 1

    print("recipe unlocks", written)


main()
