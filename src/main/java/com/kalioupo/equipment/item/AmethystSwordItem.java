package com.kalioupo.equipment.item;

import com.kalioupo.equipment.skill.AmethystSkill;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Sword that stores charge and can call down lightning. See {@link AmethystSkill}. */
public class AmethystSwordItem extends Item {
	public AmethystSwordItem(Item.Properties properties) {
		super(properties);
	}

	@Override
	public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		AmethystSkill.onHit(stack, attacker);
		super.postHurtEnemy(stack, target, attacker);
	}
}
