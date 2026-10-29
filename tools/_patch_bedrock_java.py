import json

# ---------------- ModEquipment ----------------
p = "src/main/java/com/kalioupo/equipment/registry/ModEquipment.java"
t = open(p, encoding="utf-8").read()

if "BEDROCK_SHARDS" not in t:
    t = t.replace(
        '\tpublic static final TagKey<Item> OBSIDIAN_ALLOY_INGOTS = tag("obsidian_ingots");',
        '\tpublic static final TagKey<Item> OBSIDIAN_ALLOY_INGOTS = tag("obsidian_ingots");\n'
        '\tpublic static final TagKey<Item> BEDROCK_SHARDS = tag("bedrock_shards");'
    )
    t = t.replace(
        '\tpublic static final ResourceKey<EquipmentAsset> OBSIDIAN_ASSET = asset("obsidian");',
        '\tpublic static final ResourceKey<EquipmentAsset> OBSIDIAN_ASSET = asset("obsidian");\n'
        '\tpublic static final ResourceKey<EquipmentAsset> BEDROCK_ASSET = asset("bedrock");'
    )
    block = '''	/** netherite mining level, 2000 durability, +4 damage, enchantability 22 */
	public static final ToolMaterial BEDROCK = new ToolMaterial(
		BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 2000, 11.0F, 4.0F, 22, BEDROCK_SHARDS);

	/** base 125 -> 1375 / 2000 / 1875 / 1625, defence 6/13/10/5, toughness 4, knockback 0.4 */
	public static final ArmorMaterial BEDROCK_ARMOR = new ArmorMaterial(
		125,
		Map.of(
			ArmorType.HELMET, 6,
			ArmorType.CHESTPLATE, 13,
			ArmorType.LEGGINGS, 10,
			ArmorType.BOOTS, 5
		),
		22,
		SoundEvents.ARMOR_EQUIP_IRON,
		4.0F,
		0.4F,
		BEDROCK_SHARDS,
		BEDROCK_ASSET
	);

	private ModEquipment() {
	}'''
    t = t.replace("\tprivate ModEquipment() {\n\t}", block, 1)
    open(p, "w", encoding="utf-8").write(t)
    print("ModEquipment patched")

# ---------------- ModItems ----------------
p = "src/main/java/com/kalioupo/equipment/registry/ModItems.java"
t = open(p, encoding="utf-8").read()

if "BEDROCK_HELMET" not in t:
    anchor = 'p -> p.spear(ModEquipment.OBSIDIAN, 0.95F, 0.95F, 0.6F, 2.5F, 11.0F, 6.75F, 5.1F, 11.25F, 4.6F), Rarity.EPIC);'
    block = anchor + '''

	// ---------------------------------------------------------------- bedrock tier
	public static final Item BEDROCK_HELMET = register("bedrock_helmet", Item::new,
		p -> p.humanoidArmor(ModEquipment.BEDROCK_ARMOR, ArmorType.HELMET), Rarity.EPIC);
	public static final Item BEDROCK_CHESTPLATE = register("bedrock_chestplate", Item::new,
		p -> p.humanoidArmor(ModEquipment.BEDROCK_ARMOR, ArmorType.CHESTPLATE), Rarity.EPIC);
	public static final Item BEDROCK_LEGGINGS = register("bedrock_leggings", Item::new,
		p -> p.humanoidArmor(ModEquipment.BEDROCK_ARMOR, ArmorType.LEGGINGS), Rarity.EPIC);
	public static final Item BEDROCK_BOOTS = register("bedrock_boots", Item::new,
		p -> p.humanoidArmor(ModEquipment.BEDROCK_ARMOR, ArmorType.BOOTS), Rarity.EPIC);
	public static final Item BEDROCK_SWORD = register("bedrock_sword", Item::new,
		p -> p.sword(ModEquipment.BEDROCK, 8.0F, -2.4F), Rarity.EPIC);
	public static final Item BEDROCK_PICKAXE = register("bedrock_pickaxe", Item::new,
		p -> p.pickaxe(ModEquipment.BEDROCK, 3.0F, -2.8F), Rarity.EPIC);
	public static final Item BEDROCK_AXE = register("bedrock_axe", Item::new,
		p -> p.axe(ModEquipment.BEDROCK, 8.0F, -3.1F), Rarity.EPIC);
	public static final Item BEDROCK_SHOVEL = register("bedrock_shovel", Item::new,
		p -> p.shovel(ModEquipment.BEDROCK, 3.5F, -3.0F), Rarity.EPIC);
	public static final Item BEDROCK_HOE = register("bedrock_hoe", Item::new,
		p -> p.hoe(ModEquipment.BEDROCK, 0.0F, 0.0F), Rarity.EPIC);
	public static final Item BEDROCK_SPEAR = register("bedrock_spear", Item::new,
		p -> p.spear(ModEquipment.BEDROCK, 0.95F, 0.95F, 0.6F, 2.5F, 11.0F, 6.75F, 5.1F, 11.25F, 4.6F), Rarity.EPIC);'''
    t = t.replace(anchor, block, 1)
    open(p, "w", encoding="utf-8").write(t)
    print("ModItems patched")

# ---------------- CreativeTabs ----------------
p = "src/main/java/com/kalioupo/equipment/registry/ModCreativeTabs.java"
t = open(p, encoding="utf-8").read()

if "BEDROCK_HELMET" not in t:
    t = t.replace(
        "\t\tModItems.OBSIDIAN_SPEAR\n\t);",
        "\t\tModItems.OBSIDIAN_SPEAR,\n"
        "\t\t// bedrock\n"
        "\t\tModItems.BEDROCK_HELMET,\n"
        "\t\tModItems.BEDROCK_CHESTPLATE,\n"
        "\t\tModItems.BEDROCK_LEGGINGS,\n"
        "\t\tModItems.BEDROCK_BOOTS,\n"
        "\t\tModItems.BEDROCK_SWORD,\n"
        "\t\tModItems.BEDROCK_PICKAXE,\n"
        "\t\tModItems.BEDROCK_AXE,\n"
        "\t\tModItems.BEDROCK_SHOVEL,\n"
        "\t\tModItems.BEDROCK_HOE,\n"
        "\t\tModItems.BEDROCK_SPEAR\n"
        "\t);"
    )
    open(p, "w", encoding="utf-8").write(t)
    print("CreativeTabs patched")

# ---------------- lang ----------------
zh = {"helmet": "基岩强化头盔", "chestplate": "基岩强化胸甲", "leggings": "基岩强化护腿",
      "boots": "基岩强化靴子", "sword": "基岩强化剑", "pickaxe": "基岩强化镐",
      "axe": "基岩强化斧", "shovel": "基岩强化锹", "hoe": "基岩强化锄", "spear": "基岩强化矛"}
en = {"helmet": "Bedrock Helmet", "chestplate": "Bedrock Chestplate", "leggings": "Bedrock Leggings",
      "boots": "Bedrock Boots", "sword": "Bedrock Sword", "pickaxe": "Bedrock Pickaxe",
      "axe": "Bedrock Axe", "shovel": "Bedrock Shovel", "hoe": "Bedrock Hoe", "spear": "Bedrock Spear"}

for name, table in (("zh_cn.json", zh), ("en_us.json", en)):
    path = "src/main/resources/assets/kalioupo_equipment/lang/" + name
    data = json.load(open(path, encoding="utf-8"))
    for key, value in table.items():
        data["item.kalioupo_equipment.bedrock_" + key] = value
    json.dump(data, open(path, "w", encoding="utf-8"), indent=2, ensure_ascii=False)

print("lang patched")
