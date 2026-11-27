package com.kevin2holt.vanillaextras.client.mixin;

import com.kevin2holt.vanillaextras.client.ServerFeatures;
import com.kevin2holt.vanillaextras.feature.ModFeatures;
import com.kevin2holt.vanillaextras.torch.PickaxeTorchPlacement;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Client half of pickaxe torch placement: predicts the torch the server is about to place, the
// same way vanilla predicts any block placement. Returning a success here also stops the client
// from going on to use the off-hand item, so a shield is not raised and an off-hand block is not
// placed. Only active when the connected server announced the feature.
@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {

	@Inject(method = "performUseItemOn", at = @At("RETURN"), cancellable = true)
	private void predictPickaxeTorch(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> callbackInfo) {

		if (callbackInfo.getReturnValue() != InteractionResult.PASS || !ServerFeatures.isEnabledOnServer(ModFeatures.PICKAXE_TORCHES)) {
			return;
		}

		InteractionResult placementResult = PickaxeTorchPlacement.tryPlaceTorch(player, hand, hitResult);
		if (placementResult != InteractionResult.PASS) {
			callbackInfo.setReturnValue(placementResult);
		}
	}
}
