package com.kalioupo.equipment.item;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Armour that keeps one or more mob effects on the wearer while it sits in its slot.
 * Used for the prismarine helmet (water breathing) and the phantom set (night vision, resistance).
 *
 * <p>The effect is kept well above {@link #MIN_REMAINING} ticks: vanilla starts flashing the night
 * vision overlay once ten seconds are left, and a short duration refreshed every tick sits right in
 * that window and flickers the whole time.</p>
 */
public class EffectArmorItem extends Item {
	/** below this many ticks left the effect is refreshed again */
	private static final int MIN_REMAINING = 260;
	/** how long each refresh lasts: comfortably above the 200 tick flicker threshold */
	private static final int DURATION_TICKS = 320;

	private final EquipmentSlot wornSlot;
	private final Holder<MobEffect>[] effects;

	@SafeVarargs
	public EffectArmorItem(Item.Properties properties, EquipmentSlot wornSlot, Holder<MobEffect>... effects) {
		super(properties);
		this.wornSlot = wornSlot;
		this.effects = effects;
	}

	@Override
	public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
		if (slot == this.wornSlot && entity instanceof LivingEntity living) {
			for (Holder<MobEffect> effect : this.effects) {
				MobEffectInstance active = living.getEffect(effect);

				if (active == null || active.getDuration() < MIN_REMAINING) {
					living.addEffect(new MobEffectInstance(effect, DURATION_TICKS, 0, false, false, true));
				}
			}
		}

		super.inventoryTick(stack, level, entity, slot);
	}
}
