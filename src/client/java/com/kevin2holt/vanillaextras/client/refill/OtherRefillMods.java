package com.kevin2holt.vanillaextras.client.refill;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kevin2holt.vanillaextras.VanillaExtras;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

// Detects whether another installed mod has its own auto-refill switched on, so ours can stand
// down. A mod that is installed with its refill turned off does not count.
//
// Each check first asks the running mod (reflection, so it follows in-game hotkey toggles) and
// falls back to reading its config file. If neither works, the mod is treated as off and the
// yield delay in AutoRefillClient is what keeps the two from both refilling.
public final class OtherRefillMods {
	private static final String TWEAKEROO_MOD_ID = "tweakeroo";
	private static final String TWEAKEROO_TOGGLE_CLASS = "fi.dy.masa.tweakeroo.config.FeatureToggle";
	private static final String TWEAKEROO_TOGGLE_NAME = "TWEAK_HAND_RESTOCK";
	private static final String TWEAKEROO_CONFIG_FILE = "tweakeroo.json";
	private static final String TWEAKEROO_CONFIG_CATEGORY = "TweakToggles";
	private static final String TWEAKEROO_CONFIG_KEY = "tweakHandRestock";

	private static final String IPN_MOD_ID = "inventoryprofilesnext";
	private static final String IPN_SETTINGS_CLASS = "org.anti_ad.mc.ipnext.config.ModSettings";
	private static final String IPN_SETTING_GETTER = "getENABLE_AUTO_REFILL";
	private static final String IPN_CONFIG_FILE = "inventoryprofilesnext/inventoryprofiles.json";
	private static final String IPN_CONFIG_KEY = "enable_auto_refill";

	// Method names config libraries use for "is this toggle on"
	private static final List<String> BOOLEAN_GETTER_NAMES = List.of("getBooleanValue", "getValue", "isEnabled");

	private static final Set<String> warnedModIds = new HashSet<>();


	private OtherRefillMods() {
	}


	public static boolean isAnyOtherRefillActive() {

		return isTweakerooRestockActive() || isInventoryProfilesRefillActive();
	}



	private static boolean isTweakerooRestockActive() {

		if (!FabricLoader.getInstance().isModLoaded(TWEAKEROO_MOD_ID)) {
			return false;
		}

		Optional<Boolean> liveValue = readBooleanOption(() -> {
			Class<?> toggleClass = Class.forName(TWEAKEROO_TOGGLE_CLASS);
			for (Object toggle : toggleClass.getEnumConstants()) {
				if (((Enum<?>) toggle).name().equals(TWEAKEROO_TOGGLE_NAME)) {
					return toggle;
				}
			}
			return null;
		});
		if (liveValue.isPresent()) {
			return liveValue.get();
		}

		Optional<Boolean> fileValue = readConfigBoolean(TWEAKEROO_CONFIG_FILE, TWEAKEROO_CONFIG_CATEGORY, TWEAKEROO_CONFIG_KEY);
		return resolveUnknown(TWEAKEROO_MOD_ID, fileValue);
	}


	private static boolean isInventoryProfilesRefillActive() {

		if (!FabricLoader.getInstance().isModLoaded(IPN_MOD_ID)) {
			return false;
		}

		// IPN settings are Kotlin objects: a static INSTANCE with a getter per option
		Optional<Boolean> liveValue = readBooleanOption(() -> {
			Class<?> settingsClass = Class.forName(IPN_SETTINGS_CLASS);
			Object settings = settingsClass.getField("INSTANCE").get(null);
			return settingsClass.getMethod(IPN_SETTING_GETTER).invoke(settings);
		});
		if (liveValue.isPresent()) {
			return liveValue.get();
		}

		Optional<Boolean> fileValue = readConfigBoolean(IPN_CONFIG_FILE, null, IPN_CONFIG_KEY);
		return resolveUnknown(IPN_MOD_ID, fileValue);
	}


	private static boolean resolveUnknown(String modId, Optional<Boolean> value) {

		if (value.isPresent()) {
			return value.get();
		}

		if (warnedModIds.add(modId)) {
			VanillaExtras.LOGGER.warn("Could not tell whether {}'s auto-refill is on; relying on the refill delay to avoid double refills", modId);
		}
		return false;
	}



	@FunctionalInterface
	private interface OptionLookup {
		Object find() throws ReflectiveOperationException;
	}


	// Finds another mod's option object and asks it for its on/off value
	private static Optional<Boolean> readBooleanOption(OptionLookup lookup) {

		try {
			Object option = lookup.find();
			if (option == null) {
				return Optional.empty();
			}

			for (String getterName : BOOLEAN_GETTER_NAMES) {
				Optional<Method> getter = findNoArgMethod(option.getClass(), getterName);
				if (getter.isPresent()) {
					getter.get().trySetAccessible();// public method on a non-public class otherwise refuses invoke
					Object value = getter.get().invoke(option);
					if (value instanceof Boolean isOn) {
						return Optional.of(isOn);
					}
				}
			}
		} catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
			// The other mod changed its internals; the config file fallback handles it
		}

		return Optional.empty();
	}


	private static Optional<Method> findNoArgMethod(Class<?> type, String methodName) {

		try {
			return Optional.of(type.getMethod(methodName));
		} catch (NoSuchMethodException exception) {
			return Optional.empty();
		}
	}



	// Reads a boolean from another mod's JSON config. With a category, looks only in that
	// top-level object; without one, searches the whole file for the key.
	private static Optional<Boolean> readConfigBoolean(String relativePath, String category, String key) {

		Path configPath = FabricLoader.getInstance().getConfigDir().resolve(relativePath);
		if (!Files.exists(configPath)) {
			return Optional.empty();
		}

		try {
			JsonElement root = JsonParser.parseString(Files.readString(configPath, StandardCharsets.UTF_8));
			if (category != null) {
				if (!root.isJsonObject() || !root.getAsJsonObject().has(category)) {
					return Optional.empty();
				}
				root = root.getAsJsonObject().get(category);
			}
			return findBooleanByKey(root, key);
		} catch (IOException | RuntimeException exception) {
			return Optional.empty();
		}
	}


	// Depth-first search for key (case-insensitive). Accepts a plain boolean or an object that
	// wraps one under a "value"-like name, since config libraries store toggles both ways.
	private static Optional<Boolean> findBooleanByKey(JsonElement element, String key) {

		if (!element.isJsonObject()) {
			return Optional.empty();
		}

		for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
			if (entry.getKey().equalsIgnoreCase(key)) {
				Optional<Boolean> value = toBoolean(entry.getValue());
				if (value.isPresent()) {
					return value;
				}
			}

			Optional<Boolean> nestedValue = findBooleanByKey(entry.getValue(), key);
			if (nestedValue.isPresent()) {
				return nestedValue;
			}
		}

		return Optional.empty();
	}


	private static Optional<Boolean> toBoolean(JsonElement element) {

		if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean()) {
			return Optional.of(element.getAsBoolean());
		}

		if (element.isJsonObject()) {
			JsonObject wrapper = element.getAsJsonObject();
			for (String valueKey : List.of("value", "booleanValue", "enabled")) {
				if (wrapper.has(valueKey)) {
					return toBoolean(wrapper.get(valueKey));
				}
			}
		}

		return Optional.empty();
	}
}
