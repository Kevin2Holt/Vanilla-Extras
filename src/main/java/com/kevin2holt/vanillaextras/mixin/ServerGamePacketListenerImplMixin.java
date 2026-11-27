package com.kevin2holt.vanillaextras.mixin;

import com.kevin2holt.vanillaextras.VanillaExtras;
import com.kevin2holt.vanillaextras.feature.ModFeatures;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// Vanilla disconnects a player whose beacon choice fails validation. The beacon screen lets you
// press confirm on a base too small for the effects already set (e.g. after blocks were removed),
// which kicks the player, or drops a singleplayer world back to the title screen. Ignore the
// request instead; the payment item is handed back when the screen closes.
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {

	@WrapOperation(
		method = "handleSetBeaconPacket",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;disconnect(Lnet/minecraft/network/chat/Component;)V")
	)
	private void ignoreInvalidBeaconChoice(ServerGamePacketListenerImpl listener, Component reason, Operation<Void> original) {

		if (!ModFeatures.SATURATION_BEACON.isEnabled()) {
			original.call(listener, reason);
			return;
		}

		VanillaExtras.LOGGER.debug("Ignored a beacon choice the beacon's base can't support");
	}
}
