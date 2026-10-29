package com.kalioupo.equipment.client;

import com.kalioupo.equipment.menu.BackpackMenu;
import com.kalioupo.equipment.menu.SmelterMenu;
import com.kalioupo.equipment.registry.ModMenus;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

public class KalioupoEquipmentClient implements ClientModInitializer {
	@Override
	@SuppressWarnings({"rawtypes", "unchecked"})
	public void onInitializeClient() {
		// MenuScreens.register is private in 26.3 and is made public by our access widener.
		MenuScreens.ScreenConstructor backpack =
			(menu, inventory, title) -> new BackpackScreen((BackpackMenu) menu, inventory, title);
		MenuScreens.register(ModMenus.BACKPACK, backpack);

		MenuScreens.ScreenConstructor smelter =
			(menu, inventory, title) -> new SmelterScreen((SmelterMenu) menu, inventory, title);
		MenuScreens.register(ModMenus.SMELTER, smelter);

		AmethystClient.init();
		GlideClient.init();
		PhantomChargeClient.init();
		ShieldCooldownClient.init();
		StarlightClient.init();
		ItemHints.init();
	}
}
