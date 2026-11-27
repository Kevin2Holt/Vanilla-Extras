package com.kevin2holt.vanillaextras.client.settings;

import com.kevin2holt.vanillaextras.config.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

// Entry point for the in-game settings menu. Features with options call registerPage once from
// onInitializeClient; each page becomes a button on the root screen, after the feature toggles.
public final class SettingsMenu {
	public static final Component MENU_TITLE = Component.literal("Vanilla Extras");
	public static final Component OPTIONS_BUTTON_LABEL = Component.literal("Extras");

	// Config-only: adds an "Extras" button to the vanilla Options screen
	public static final BooleanSetting SHOW_OPTIONS_SCREEN_BUTTON = new BooleanSetting("settingsMenu", "showOptionsScreenButton", false);

	private static final List<SettingsPage> REGISTERED_PAGES = new ArrayList<>();


	private SettingsMenu() {
	}


	// contents runs every time the page opens, so it always shows current values
	public static void registerPage(Component title, Consumer<SettingsPageBuilder> contents) {

		REGISTERED_PAGES.add(new SettingsPage(title, contents));
	}


	public static List<SettingsPage> getRegisteredPages() {

		return List.copyOf(REGISTERED_PAGES);
	}


	public static Screen createRootScreen(Screen parentScreen) {

		return new ExtrasSettingsScreen(parentScreen);
	}


	public static void openScreen(Screen screen) {

		Minecraft.getInstance().gui.setScreen(screen);
	}



	public record SettingsPage(Component title, Consumer<SettingsPageBuilder> contents) {
	}
}
