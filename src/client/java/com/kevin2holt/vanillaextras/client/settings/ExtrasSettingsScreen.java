package com.kevin2holt.vanillaextras.client.settings;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

// Root of the settings menu: the feature toggles first, then one button per registered page
public class ExtrasSettingsScreen extends OptionsSubScreen {
	private static final Component FEATURE_TOGGLES_TITLE = Component.literal("Feature Toggles");


	public ExtrasSettingsScreen(Screen parentScreen) {

		super(parentScreen, Minecraft.getInstance().options, SettingsMenu.MENU_TITLE);
	}


	@Override
	protected void addOptions() {

		this.list.addBig(this.createPageButton(FEATURE_TOGGLES_TITLE, () -> new FeatureTogglesScreen(this, FEATURE_TOGGLES_TITLE)));

		for (SettingsMenu.SettingsPage page : SettingsMenu.getRegisteredPages()) {
			this.list.addBig(this.createPageButton(page.title(), () -> new SettingsPageScreen(this, page)));
		}
	}


	private Button createPageButton(Component title, Supplier<Screen> screenFactory) {

		return Button.builder(title, button -> SettingsMenu.openScreen(screenFactory.get()))
			.width(SettingsWidgets.FULL_WIDTH)
			.build();
	}
}
