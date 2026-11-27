package com.kevin2holt.vanillaextras.client.settings;

import com.kevin2holt.vanillaextras.feature.Feature;
import com.kevin2holt.vanillaextras.feature.ModFeatures;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

// Every feature's on/off toggle, split into features that switch live and ones that need a restart
public class FeatureTogglesScreen extends OptionsSubScreen {
	private static final Component LIVE_HEADER = Component.literal("Apply right away");
	private static final Component RESTART_HEADER = Component.literal("Apply after a restart");
	private static final Component REMOTE_SERVER_NOTE = Component.literal("These apply to your own worlds. This server uses its own config.").withStyle(ChatFormatting.GRAY);
	private static final Component RESTART_TOOLTIP = Component.literal("Saved right away, but only takes effect after you restart the game.");
	private static final Component PENDING_MARKER = Component.literal(" (restart)").withStyle(ChatFormatting.YELLOW);


	public FeatureTogglesScreen(Screen parentScreen, Component title) {

		super(parentScreen, Minecraft.getInstance().options, title);
	}


	@Override
	protected void addOptions() {

		if (isOnRemoteServer()) {
			this.list.addHeader(REMOTE_SERVER_NOTE);
		}

		List<AbstractWidget> liveToggles = new ArrayList<>();
		List<AbstractWidget> restartToggles = new ArrayList<>();

		for (Feature feature : ModFeatures.ALL_FEATURES) {
			if (feature.isRestartRequired()) {
				restartToggles.add(createFeatureToggle(feature));
			} else {
				liveToggles.add(createFeatureToggle(feature));
			}
		}

		this.addSection(LIVE_HEADER, liveToggles);
		this.addSection(RESTART_HEADER, restartToggles);
	}


	private void addSection(Component header, List<AbstractWidget> toggles) {

		if (toggles.isEmpty()) {
			return;
		}

		this.list.addHeader(header);
		this.list.addSmall(toggles);
	}


	private static AbstractWidget createFeatureToggle(Feature feature) {

		Component featureName = getFeatureName(feature);

		return SettingsWidgets.createToggle(
			SettingsWidgets.HALF_WIDTH,
			feature::isEnabledInConfig,
			feature::setEnabled,
			isOn -> buildToggleMessage(feature, featureName, isOn),
			feature.isRestartRequired() ? RESTART_TOOLTIP : null
		);
	}


	private static Component buildToggleMessage(Feature feature, Component featureName, boolean isOn) {

		Component message = SettingsWidgets.formatOnOff(featureName, isOn);
		if (feature.isRestartRequired() && isOn != feature.isEnabled()) {
			return message.copy().append(PENDING_MARKER);
		}

		return message;
	}


	// Uses a lang entry when one exists, otherwise turns "saturationBeacon" into "Saturation Beacon"
	public static Component getFeatureName(Feature feature) {

		String configKey = feature.configKey();
		StringBuilder readableName = new StringBuilder();

		for (int index = 0; index < configKey.length(); index++) {
			char character = configKey.charAt(index);
			if (index == 0) {
				readableName.append(Character.toUpperCase(character));
			} else {
				if (Character.isUpperCase(character)) {
					readableName.append(' ');
				}
				readableName.append(character);
			}
		}

		return Component.translatableWithFallback("vanilla-extras.feature." + configKey, readableName.toString());
	}


	private static boolean isOnRemoteServer() {

		Minecraft minecraft = Minecraft.getInstance();
		return minecraft.getConnection() != null && !minecraft.hasSingleplayerServer();
	}
}
