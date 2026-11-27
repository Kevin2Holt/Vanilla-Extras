package com.kevin2holt.vanillaextras.network;

import com.kevin2holt.vanillaextras.feature.Feature;
import com.kevin2holt.vanillaextras.feature.ModFeatures;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

// Tells modded clients which client-synced features the server has enabled when they join
public final class FeatureSync {

	private FeatureSync() {
	}


	public static void register() {

		PayloadTypeRegistry.clientboundPlay().register(EnabledFeaturesPayload.TYPE, EnabledFeaturesPayload.STREAM_CODEC);

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			sendEnabledFeatures(handler.player);
		});
	}


	private static void sendEnabledFeatures(ServerPlayer player) {

		// Vanilla clients have not registered the channel, so they are skipped here
		if (!ServerPlayNetworking.canSend(player, EnabledFeaturesPayload.TYPE)) {
			return;
		}

		List<String> enabledFeatureKeys = ModFeatures.ALL_FEATURES.stream()
			.filter(Feature::isSyncedToClient)
			.filter(Feature::isEnabled)
			.map(Feature::configKey)
			.toList();

		ServerPlayNetworking.send(player, new EnabledFeaturesPayload(enabledFeatureKeys));
	}
}
