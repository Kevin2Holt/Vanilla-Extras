package com.kevin2holt.vanillaextras.config;

// An on/off option for one feature, stored in config/vanilla-extras.json under
// "settings" -> featureKey -> settingKey
public record BooleanSetting(String featureKey, String settingKey, boolean defaultValue) {

	public boolean get() {

		return FeatureConfig.getBooleanSetting(this.featureKey, this.settingKey, this.defaultValue);
	}


	public void set(boolean value) {

		FeatureConfig.setBooleanSetting(this.featureKey, this.settingKey, value);
	}
}
