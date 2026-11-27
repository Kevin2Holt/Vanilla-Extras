package com.kevin2holt.vanillaextras.client;

import com.kevin2holt.vanillaextras.client.beacon.BeaconScreenOptions;
import com.kevin2holt.vanillaextras.client.refill.AutoRefillClient;
import com.kevin2holt.vanillaextras.client.refill.AutoRefillSettingsPage;
import com.kevin2holt.vanillaextras.client.torch.PickaxeTorchSettingsPage;
import net.fabricmc.api.ClientModInitializer;

public class VanillaExtrasClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {

		// Client-only setup (renderers, keybinds, screens) goes here
		ServerFeatures.register();
		AutoRefillClient.register();
		AutoRefillSettingsPage.register();
		PickaxeTorchSettingsPage.register();
		BeaconScreenOptions.register();
	}
}
