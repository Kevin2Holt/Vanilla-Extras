package com.kevin2holt.vanillaextras.feature;

import java.util.List;

// Every feature in the mod; each one gets an on/off entry in config/vanilla-extras.json.
// Arguments: config key, enabled by default, synced to modded clients, needs a restart to change.
public final class ModFeatures {
	public static final Feature SATURATION_BEACON = new Feature("saturationBeacon", true, false);
	public static final Feature PICKAXE_TORCHES = new Feature("pickaxeTorches", true, true);
	public static final Feature AUTO_REFILL = new Feature("autoRefill", true, true);

	public static final List<Feature> ALL_FEATURES = List.of(
		SATURATION_BEACON,
		PICKAXE_TORCHES,
		AUTO_REFILL
	);


	private ModFeatures() {
	}


	// Reads every toggle once so the config file lists all features after the first launch
	public static void loadAllToggles() {

		for (Feature feature : ALL_FEATURES) {
			feature.isEnabled();
		}
	}
}
