package com.kevin2holt.vanillaextras.client.settings;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;

// One feature's options page, built from what the feature registered with SettingsMenu
public class SettingsPageScreen extends OptionsSubScreen {
	private final SettingsMenu.SettingsPage page;


	public SettingsPageScreen(Screen parentScreen, SettingsMenu.SettingsPage page) {

		super(parentScreen, Minecraft.getInstance().options, page.title());
		this.page = page;
	}


	@Override
	protected void addOptions() {

		SettingsPageBuilder builder = new SettingsPageBuilder();
		this.page.contents().accept(builder);

		for (SettingsPageBuilder.SettingsEntry entry : builder.getEntries()) {
			switch (entry) {
				case SettingsPageBuilder.SettingsEntry.Header header -> this.list.addHeader(header.text());
				case SettingsPageBuilder.SettingsEntry.Slider slider -> this.list.addBig(SettingsWidgets.createSlider(
					SettingsWidgets.FULL_WIDTH, slider.setting(), slider.label(), slider.valueText(), slider.tooltip()));
				case SettingsPageBuilder.SettingsEntry.Toggle toggle -> this.list.addBig(SettingsWidgets.createToggle(
					SettingsWidgets.FULL_WIDTH, toggle.setting()::get, toggle.setting()::set,
					isOn -> SettingsWidgets.formatOnOff(toggle.label(), isOn), toggle.tooltip()));
			}
		}
	}
}
