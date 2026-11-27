package com.kevin2holt.vanillaextras.client.mixin;

import com.kevin2holt.vanillaextras.client.settings.SettingsMenu;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Adds a full-width "Extras" row under vanilla's option buttons when the config asks for it
@Mixin(OptionsScreen.class)
public abstract class OptionsScreenMixin {
	private static final int GRID_COLUMNS = 2;
	private static final int GRID_ROW_WIDTH = 308;// two 150-wide buttons plus vanilla's 8px gap


	@Inject(
		method = "init",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/layouts/HeaderAndFooterLayout;addToContents(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;")
	)
	private void addExtrasButton(CallbackInfo callbackInfo, @Local GridLayout.RowHelper rowHelper) {

		if (!SettingsMenu.SHOW_OPTIONS_SCREEN_BUTTON.get()) {
			return;
		}

		Screen optionsScreen = (Screen) (Object) this;
		Button extrasButton = Button.builder(SettingsMenu.OPTIONS_BUTTON_LABEL, button -> {
			SettingsMenu.openScreen(SettingsMenu.createRootScreen(optionsScreen));
		}).width(GRID_ROW_WIDTH).build();

		rowHelper.addChild(extrasButton, GRID_COLUMNS);
	}
}
