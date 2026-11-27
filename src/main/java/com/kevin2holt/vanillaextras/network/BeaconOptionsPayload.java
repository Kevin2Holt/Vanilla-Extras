package com.kevin2holt.vanillaextras.network;

import com.kevin2holt.vanillaextras.VanillaExtras;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// Server -> modded client, sent as a beacon screen opens: whether that beacon offers Saturation
// in place of Regeneration. Only sent to clients that registered the channel.
public record BeaconOptionsPayload(boolean isSaturationOffered) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<BeaconOptionsPayload> TYPE =
		new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(VanillaExtras.MOD_ID, "beacon_options"));

	public static final StreamCodec<RegistryFriendlyByteBuf, BeaconOptionsPayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.BOOL,
		BeaconOptionsPayload::isSaturationOffered,
		BeaconOptionsPayload::new
	);


	@Override
	public CustomPacketPayload.Type<BeaconOptionsPayload> type() {

		return TYPE;
	}
}
