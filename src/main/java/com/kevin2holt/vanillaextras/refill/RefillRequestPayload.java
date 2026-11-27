package com.kevin2holt.vanillaextras.refill;

import com.kevin2holt.vanillaextras.VanillaExtras;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

// Modded client -> server: "the stack in this slot ran low, please refill it with this item".
// The player's own refill settings ride along, since they are per-player preferences. Vanilla
// clients never send this, so they never get refills.
//
// inventorySlot: 0-8 for a hotbar slot (the main hand), or Inventory.SLOT_OFFHAND
public record RefillRequestPayload(int inventorySlot, ItemStack template, int triggerCount, int refillAmount, boolean isFromContainersAllowed) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<RefillRequestPayload> TYPE =
		new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(VanillaExtras.MOD_ID, "refill_request"));

	public static final StreamCodec<RegistryFriendlyByteBuf, RefillRequestPayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, RefillRequestPayload::inventorySlot,
		ItemStack.STREAM_CODEC, RefillRequestPayload::template,
		ByteBufCodecs.VAR_INT, RefillRequestPayload::triggerCount,
		ByteBufCodecs.VAR_INT, RefillRequestPayload::refillAmount,
		ByteBufCodecs.BOOL, RefillRequestPayload::isFromContainersAllowed,
		RefillRequestPayload::new
	);


	@Override
	public CustomPacketPayload.Type<RefillRequestPayload> type() {

		return TYPE;
	}
}
