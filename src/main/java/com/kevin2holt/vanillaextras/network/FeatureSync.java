package com.kevin2holt.vanillaextras.network;

import com.kevin2holt.vanillaextras.config.BooleanSetting;
import com.kevin2holt.vanillaextras.feature.Feature;
import com.kevin2holt.vanillaextras.feature.ModFeatures;
import com.kevin2holt.vanillaextras.torch.PickaxeTorchSettings;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

// Tells modded clients which client-synced features the server has enabled when they join,
// and again whenever a synced toggle changes while the server is running (e.g. from the
// in-game settings menu in singleplayer or LAN)
public final class FeatureSync {
	// Server options a modded client needs to know about; sent with the enabled features on join
	private static final List<BooleanSetting> SYNCED_SETTINGS = List.of(
		PickaxeTorchSettings.FROM_CONTAINERS
	);

	private static volatile MinecraftServer runningServer;


	private FeatureSync() {
	}


	public static void register() {

		PayloadTypeRegistry.clientboundPlay().register(EnabledFeaturesPayload.TYPE, EnabledFeaturesPayload.STREAM_CODEC);

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			sendEnabledFeatures(handler.player);
		});

		ServerLifecycleEvents.SERVER_STARTED.register(server -> runningServer = server);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> runningServer = null);

		Feature.addToggleListener(FeatureSync::resendIfSynced);
	}


	private static void resendIfSynced(Feature feature) {

		MinecraftServer server = runningServer;
		if (server == null || !feature.isSyncedToClient()) {
			return;
		}

		// Toggles can change from the client's render thread; packets go out from the server thread
		server.execute(() -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				sendEnabledFeatures(player);
			}
		});
	}


	private static void sendEnabledFeatures(ServerPlayer player) {

		// Vanilla clients have not registered the channel, so they are skipped here
		if (!ServerPlayNetworking.canSend(player, EnabledFeaturesPayload.TYPE)) {
			return;
		}

		List<String> enabledKeys = new ArrayList<>();

		ModFeatures.ALL_FEATURES.stream()
			.filter(Feature::isSyncedToClient)
			.filter(Feature::isEnabled)
			.map(Feature::configKey)
			.forEach(enabledKeys::add);

		SYNCED_SETTINGS.stream()
			.filter(BooleanSetting::get)
			.map(FeatureSync::getSyncKey)
			.forEach(enabledKeys::add);

		ServerPlayNetworking.send(player, new EnabledFeaturesPayload(enabledKeys));
	}


	// Synced settings share the feature list, as "featureKey.settingKey", so the packet format is unchanged
	public static String getSyncKey(BooleanSetting setting) {

		return setting.featureKey() + "." + setting.settingKey();
	}
}
