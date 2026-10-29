import re

# ---------- 1. armour layers: sample bedrock at 64x32 proportions ----------
p = "tools/make_equipment.py"
t = open(p, encoding="utf-8").read()

if "armour_layer" not in t:
    t = t.replace(
        "def recolor(image, stops, mottled=None):",
        "def recolor(image, stops, mottled=None, armour_layer=False):"
    )
    t = t.replace(
        "                sx = x % 16\n                sy = (y // 2 + x % 3) % 16",
        "                if armour_layer:\n"
        "                    # armour sheets are 64x32, keep the mottling block sized\n"
        "                    sx = (x * 16 // max(1, image.width)) % 16\n"
        "                    sy = (y * 16 // max(1, image.height) + x % 2) % 16\n"
        "                    weight = weight * 0.72\n"
        "                else:\n"
        "                    sx = x % 16\n"
        "                    sy = (y // 2 + x % 3) % 16"
    )
    t = t.replace(
        'recolor(src, stops, material).save(out)',
        'recolor(src, stops, material, armour_layer=True).save(out)'
    )
    open(p, "w", encoding="utf-8").write(t)
    print("armour layer sampling patched")

# ---------- 2. bedrock immunity ----------
java = """package com.kalioupo.equipment.skill;

import com.kalioupo.equipment.registry.ModItems;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * Full bedrock set: the warden's darkness is shaken off and its sonic boom does nothing,
 * so the ancient city can be looted head on.
 */
public final class BedrockImmunity {
	private BedrockImmunity() {
	}

	public static void init() {
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (!(entity instanceof ServerPlayer player) || !hasFullSet(player)) {
				return true;
			}

			// sonic boom: no damage at all
			return !source.is(DamageTypes.SONIC_BOOM);
		});
	}

	public static void tickServer(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (hasFullSet(player) && player.hasEffect(MobEffects.DARKNESS)) {
				player.removeEffect(MobEffects.DARKNESS);
			}
		}
	}

	private static boolean hasFullSet(ServerPlayer player) {
		return player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.BEDROCK_HELMET)
			&& player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.BEDROCK_CHESTPLATE)
			&& player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.BEDROCK_LEGGINGS)
			&& player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.BEDROCK_BOOTS);
	}
}
"""
open("src/main/java/com/kalioupo/equipment/skill/BedrockImmunity.java", "w", encoding="utf-8").write(java)

# ---------- 3. register it ----------
p = "src/main/java/com/kalioupo/equipment/KalioupoEquipment.java"
t = open(p, encoding="utf-8").read()

if "BedrockImmunity" not in t:
    t = t.replace(
        "import com.kalioupo.equipment.skill.BedrockChisel;",
        "import com.kalioupo.equipment.skill.BedrockChisel;\nimport com.kalioupo.equipment.skill.BedrockImmunity;"
    )
    t = t.replace(
        "\t\tBedrockChisel.init();",
        "\t\tBedrockChisel.init();\n\t\tBedrockImmunity.init();\n"
        "\t\tServerTickEvents.END_SERVER_TICK.register(BedrockImmunity::tickServer);"
    )
    open(p, "w", encoding="utf-8").write(t)
    print("BedrockImmunity registered")

# unused import guard
print("done")
