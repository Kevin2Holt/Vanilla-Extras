package com.kevin2holt.vanillaextras.client.beacon;

import com.kevin2holt.vanillaextras.network.BeaconOptionsPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

// What the server said about the beacon being opened. The server sends it right before the
// beacon screen opens; on servers without the mod it stays false and the screen stays vanilla.
public final class BeaconScreenOptions {
	private static volatile boolean isSaturationOffered = false;


	private BeaconScreenOptions() {
	}


	public static void register() {

		ClientPlayNetworking.registerGlobalReceiver(BeaconOptionsPayload.TYPE, (payload, context) -> {
			isSaturationOffered = payload.isSaturationOffered();
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			isSaturationOffered = false;
		});
	}


	public static boolean isSaturationOffered() {

		return isSaturationOffered;
	}
}
