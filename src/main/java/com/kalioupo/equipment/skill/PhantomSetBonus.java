package com.kalioupo.equipment.skill;

import com.kalioupo.equipment.KalioupoEquipment;
import com.kalioupo.equipment.registry.ModItems;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Wearing all four phantom pieces cuts fall damage by 80%, and no single fall can take more
 * than half of your health, so ramming a wall while gliding never kills you outright.
 */
public final class PhantomSetBonus {
	private static final Identifier FALL_MODIFIER_ID = KalioupoEquipment.id("phantom_set_fall");
	private static final AttributeModifier FALL_MODIFIER =
		new AttributeModifier(FALL_MODIFIER_ID, -0.8, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

	private PhantomSetBonus() {
	}

	public static void init() {
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (!(entity instanceof ServerPlayer player) || !source.is(DamageTypeTags.IS_FALL)) {
				return true;
			}

			if (!hasFullSet(player) || !(player.level() instanceof ServerLevel level)) {
				return true;
			}

			float cap = player.getMaxHealth() * 0.5F;

			if (amount <= cap) {
				return true;
			}

			// cancel the oversized hit and re-apply the capped amount on the next tick so we
			// never recurse back into this handler
			level.getServer().execute(() -> {
				if (player.isAlive()) {
					player.hurtServer(level, source, cap);
				}
			});

			return false;
		});
	}

	public static void tickServer(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			AttributeInstance attribute = player.getAttribute(Attributes.FALL_DAMAGE_MULTIPLIER);

			if (attribute == null) {
				continue;
			}

			boolean active = attribute.getModifier(FALL_MODIFIER_ID) != null;

			if (hasFullSet(player) && !active) {
				attribute.addOrUpdateTransientModifier(FALL_MODIFIER);
			} else if (!hasFullSet(player) && active) {
				attribute.removeModifier(FALL_MODIFIER_ID);
			}
		}
	}

	private static boolean hasFullSet(ServerPlayer player) {
		return player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.PHANTOM_HELMET)
			&& player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.PHANTOM_CHESTPLATE)
			&& player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.PHANTOM_LEGGINGS)
			&& player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.PHANTOM_BOOTS);
	}
}
