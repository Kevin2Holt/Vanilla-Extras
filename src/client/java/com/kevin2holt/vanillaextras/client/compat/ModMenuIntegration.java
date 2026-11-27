package com.kevin2holt.vanillaextras.client.compat;

import com.kevin2holt.vanillaextras.client.settings.SettingsMenu;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

// Mod Menu's "configure" button. Only loaded by Mod Menu, so the mod runs fine without it.
public class ModMenuIntegration implements ModMenuApi {

	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {

		return SettingsMenu::createRootScreen;
	}
}
