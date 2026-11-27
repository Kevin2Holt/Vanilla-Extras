package com.kevin2holt.vanillaextras.feature;

import com.kevin2holt.vanillaextras.config.FeatureConfig;

// One toggleable feature. isSyncedToClient marks features a modded client must know the
// server has enabled (e.g. to show a new option in a vanilla screen without getting kicked).
public record Feature(String configKey, boolean isEnabledByDefault, boolean isSyncedToClient) {

	public boolean isEnabled() {

		return FeatureConfig.isFeatureEnabled(this.configKey, this.isEnabledByDefault);
	}
}
