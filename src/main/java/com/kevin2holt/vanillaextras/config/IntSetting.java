package com.kevin2holt.vanillaextras.config;

// A whole-number option for one feature, stored in config/vanilla-extras.json under
// "settings" -> featureKey -> settingKey and always kept within [minValue, maxValue]
public record IntSetting(String featureKey, String settingKey, int defaultValue, int minValue, int maxValue) {

	public int get() {

		return FeatureConfig.getIntSetting(this.featureKey, this.settingKey, this.defaultValue, this.minValue, this.maxValue);
	}


	public void set(int value) {

		FeatureConfig.setIntSetting(this.featureKey, this.settingKey, Math.clamp(value, this.minValue, this.maxValue));
	}
}
