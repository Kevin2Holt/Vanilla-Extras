package com.kevin2holt.vanillaextras.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kevin2holt.vanillaextras.VanillaExtras;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

// Per-feature on/off toggles stored in config/vanilla-extras.json under "features", plus
// per-feature settings under "settings" -> feature key -> setting key.
// Loads lazily on first lookup, so features read during class initialization (before the
// mod's entrypoint runs) still see the file. Unknown keys are added with their default.
public final class FeatureConfig {
	private static final String CONFIG_FILE_NAME = VanillaExtras.MOD_ID + ".json";
	private static final String FEATURES_KEY = "features";
	private static final String SETTINGS_KEY = "settings";
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private static JsonObject configRoot;
	private static boolean isSaveAllowed = true;// false when the file exists but could not be read, so it is never overwritten


	private FeatureConfig() {
	}


	public static synchronized boolean isFeatureEnabled(String featureKey, boolean defaultEnabled) {

		JsonObject features = getFeaturesObject();
		JsonElement storedValue = features.get(featureKey);

		if (storedValue != null && storedValue.isJsonPrimitive() && storedValue.getAsJsonPrimitive().isBoolean()) {
			return storedValue.getAsBoolean();
		}

		// Missing or malformed entry: record the default so the toggle shows up in the file
		features.addProperty(featureKey, defaultEnabled);
		saveConfig();
		return defaultEnabled;
	}



	public static synchronized void setFeatureEnabled(String featureKey, boolean isEnabled) {

		getFeaturesObject().addProperty(featureKey, isEnabled);
		saveConfig();
	}


	public static synchronized int getIntSetting(String featureKey, String settingKey, int defaultValue, int minValue, int maxValue) {

		JsonObject featureSettings = getFeatureSettingsObject(featureKey);
		JsonElement storedValue = featureSettings.get(settingKey);

		if (storedValue != null && storedValue.isJsonPrimitive() && storedValue.getAsJsonPrimitive().isNumber()) {
			return Math.clamp(storedValue.getAsInt(), minValue, maxValue);
		}

		featureSettings.addProperty(settingKey, defaultValue);
		saveConfig();
		return defaultValue;
	}


	public static synchronized void setIntSetting(String featureKey, String settingKey, int value) {

		getFeatureSettingsObject(featureKey).addProperty(settingKey, value);
		saveConfig();
	}


	public static synchronized boolean getBooleanSetting(String featureKey, String settingKey, boolean defaultValue) {

		JsonObject featureSettings = getFeatureSettingsObject(featureKey);
		JsonElement storedValue = featureSettings.get(settingKey);

		if (storedValue != null && storedValue.isJsonPrimitive() && storedValue.getAsJsonPrimitive().isBoolean()) {
			return storedValue.getAsBoolean();
		}

		featureSettings.addProperty(settingKey, defaultValue);
		saveConfig();
		return defaultValue;
	}


	public static synchronized void setBooleanSetting(String featureKey, String settingKey, boolean value) {

		getFeatureSettingsObject(featureKey).addProperty(settingKey, value);
		saveConfig();
	}



	private static JsonObject getRootObject() {

		if (configRoot == null) {
			configRoot = loadConfig();
		}

		return configRoot;
	}


	// Returns the child object stored under key, replacing a missing or non-object value
	private static JsonObject getChildObject(JsonObject parent, String key) {

		JsonElement child = parent.get(key);
		if (child == null || !child.isJsonObject()) {
			child = new JsonObject();
			parent.add(key, child);
		}

		return child.getAsJsonObject();
	}


	private static JsonObject getFeaturesObject() {

		return getChildObject(getRootObject(), FEATURES_KEY);
	}


	private static JsonObject getFeatureSettingsObject(String featureKey) {

		return getChildObject(getChildObject(getRootObject(), SETTINGS_KEY), featureKey);
	}


	private static Path getConfigPath() {

		return FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE_NAME);
	}


	private static JsonObject loadConfig() {

		Path configPath = getConfigPath();
		if (!Files.exists(configPath)) {
			return new JsonObject();
		}

		try {
			JsonElement parsed = JsonParser.parseString(Files.readString(configPath, StandardCharsets.UTF_8));
			if (parsed.isJsonObject()) {
				return parsed.getAsJsonObject();
			}
			VanillaExtras.LOGGER.warn("{} is not a JSON object; using defaults", configPath);
			isSaveAllowed = false;
		} catch (IOException | RuntimeException exception) {
			// A broken file should not stop the game; fall back to defaults and leave the file for the user to fix
			VanillaExtras.LOGGER.error("Could not read {}; using defaults", configPath, exception);
			isSaveAllowed = false;
		}

		return new JsonObject();
	}


	private static void saveConfig() {

		if (!isSaveAllowed) {
			return;
		}

		Path configPath = getConfigPath();
		try {
			Files.createDirectories(configPath.getParent());
			Files.writeString(configPath, GSON.toJson(configRoot), StandardCharsets.UTF_8);
		} catch (IOException exception) {
			VanillaExtras.LOGGER.error("Could not write {}", configPath, exception);
		}
	}
}
