package com.kevin2holt.vanillaextras.client.torch;

import com.kevin2holt.vanillaextras.client.settings.SettingsMenu;
import com.kevin2holt.vanillaextras.torch.PickaxeTorchSettings;
import net.minecraft.network.chat.Component;

// The Pickaxe Torches page of the in-game settings menu
public final class PickaxeTorchSettingsPage {
	private static final Component PAGE_TITLE = Component.literal("Pickaxe Torches");

	private static final Component CONTAINERS_LABEL = Component.literal("Pull from containers");
	private static final Component CONTAINERS_TOOLTIP = Component.literal(
		"With no loose torches, take one from a shulker box or bundle in your inventory, or from your "
		+ "ender chest if you carry one (including \"Refill\" shulker boxes inside it). "
		+ "The server's setting decides; on a remote server this only edits your own file.");


	private PickaxeTorchSettingsPage() {
	}


	public static void register() {

		SettingsMenu.registerPage(PAGE_TITLE, page -> {
			page.addToggle(PickaxeTorchSettings.FROM_CONTAINERS, CONTAINERS_LABEL, CONTAINERS_TOOLTIP);
		});
	}
}
