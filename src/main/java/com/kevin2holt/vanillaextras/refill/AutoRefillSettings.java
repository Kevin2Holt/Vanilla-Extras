package com.kevin2holt.vanillaextras.refill;

import com.kevin2holt.vanillaextras.config.BooleanSetting;
import com.kevin2holt.vanillaextras.config.IntSetting;
import com.kevin2holt.vanillaextras.feature.ModFeatures;

// The player's own auto-refill preferences. Read on the client and sent with each refill
// request; a dedicated server never reads these.
public final class AutoRefillSettings {
	private static final String FEATURE_KEY = ModFeatures.AUTO_REFILL.configKey();

	public static final IntSetting TRIGGER_COUNT = new IntSetting(FEATURE_KEY, "triggerCount",
		RefillRules.TRIGGER_DEFAULT, RefillRules.TRIGGER_MIN, RefillRules.TRIGGER_MAX);
	public static final IntSetting REFILL_AMOUNT = new IntSetting(FEATURE_KEY, "refillAmount",
		RefillRules.AMOUNT_DEFAULT, RefillRules.AMOUNT_MIN, RefillRules.AMOUNT_MAX);
	public static final BooleanSetting FROM_CONTAINERS = new BooleanSetting(FEATURE_KEY, "fromContainers", true);


	private AutoRefillSettings() {
	}
}
