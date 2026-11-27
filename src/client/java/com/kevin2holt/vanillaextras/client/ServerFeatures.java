package com.kevin2holt.vanillaextras.client;

import com.kevin2holt.vanillaextras.feature.Feature;
import com.kevin2holt.vanillaextras.network.EnabledFeaturesPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.Set;

// Which client-synced features the connected server has enabled. Empty on servers without
// the mod, so modded clients fall back to vanilla behavior there.
public final class ServerFeatures {
	private static volatile Set<String> enabledFeatureKeys = Set.of();


	private ServerFeatures() {
	}


	public static void register() {

		ClientPlayNetworking.registerGlobalReceiver(EnabledFeaturesPayload.TYPE, (payload, context) -> {
			enabledFeatureKeys = Set.copyOf(payload.enabledFeatureKeys());
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			enabledFeatureKeys = Set.of();
		});
	}


	public static boolean isEnabledOnServer(Feature feature) {

		return enabledFeatureKeys.contains(feature.configKey());
	}
}
