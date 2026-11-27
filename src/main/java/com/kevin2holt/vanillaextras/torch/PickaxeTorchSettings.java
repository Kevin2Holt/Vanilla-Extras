package com.kevin2holt.vanillaextras.torch;

import com.kevin2holt.vanillaextras.config.BooleanSetting;
import com.kevin2holt.vanillaextras.feature.ModFeatures;

// Server-side options for pickaxe torches. The server's value decides; it is synced to modded
// clients so they know whether to expect a placement from a container.
public final class PickaxeTorchSettings {
	public static final BooleanSetting FROM_CONTAINERS = new BooleanSetting(ModFeatures.PICKAXE_TORCHES.configKey(), "fromContainers", true);


	private PickaxeTorchSettings() {
	}
}
