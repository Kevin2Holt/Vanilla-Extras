package com.kevin2holt.vanillaextras.feature;

import com.kevin2holt.vanillaextras.config.FeatureConfig;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

// One toggleable feature. isSyncedToClient marks features a modded client must know the
// server has enabled (e.g. to show a new option in a vanilla screen without getting kicked).
// isRestartRequired marks features that are wired up once at startup; changing their toggle
// is saved right away but only takes effect after a restart.
public record Feature(String configKey, boolean isEnabledByDefault, boolean isSyncedToClient, boolean isRestartRequired) {
	private static final Map<String, Boolean> STARTUP_VALUES = new ConcurrentHashMap<>();
	private static final List<Consumer<Feature>> TOGGLE_LISTENERS = new CopyOnWriteArrayList<>();


	// Live (no restart needed) feature
	public Feature(String configKey, boolean isEnabledByDefault, boolean isSyncedToClient) {

		this(configKey, isEnabledByDefault, isSyncedToClient, false);
	}


	// Whether the feature is active right now. Restart-required features keep the value they
	// had on first read, so the game never runs half-switched.
	public boolean isEnabled() {

		if (this.isRestartRequired) {
			return STARTUP_VALUES.computeIfAbsent(this.configKey, key -> this.isEnabledInConfig());
		}

		return this.isEnabledInConfig();
	}


	// The saved toggle, which can differ from isEnabled() until a restart
	public boolean isEnabledInConfig() {

		return FeatureConfig.isFeatureEnabled(this.configKey, this.isEnabledByDefault);
	}


	public boolean isPendingRestart() {

		return this.isRestartRequired && this.isEnabled() != this.isEnabledInConfig();
	}


	public void setEnabled(boolean isEnabled) {

		FeatureConfig.setFeatureEnabled(this.configKey, isEnabled);

		for (Consumer<Feature> listener : TOGGLE_LISTENERS) {
			listener.accept(this);
		}
	}


	// Called after any feature's toggle is changed through setEnabled
	public static void addToggleListener(Consumer<Feature> listener) {

		TOGGLE_LISTENERS.add(listener);
	}
}
