package com.kevin2holt.vanillaextras.network;

import com.kevin2holt.vanillaextras.VanillaExtras;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

// Server -> modded client: the client-synced features this server has turned on.
// Only sent to clients that announced they can receive it, so vanilla clients never see it.
public record EnabledFeaturesPayload(List<String> enabledFeatureKeys) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<EnabledFeaturesPayload> TYPE =
		new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(VanillaExtras.MOD_ID, "enabled_features"));

	public static final StreamCodec<RegistryFriendlyByteBuf, EnabledFeaturesPayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
		EnabledFeaturesPayload::enabledFeatureKeys,
		EnabledFeaturesPayload::new
	);


	@Override
	public CustomPacketPayload.Type<EnabledFeaturesPayload> type() {

		return TYPE;
	}
}
