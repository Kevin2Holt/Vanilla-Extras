package com.kevin2holt.vanillaextras.mixin;

import com.kevin2holt.vanillaextras.feature.ModFeatures;
import com.kevin2holt.vanillaextras.network.EnabledFeaturesPayload;
import com.kevin2holt.vanillaextras.torch.PickaxeTorchPlacement;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Hooks the end of the server's right-click-on-block handling. By the time vanilla returns
// PASS, the block (chest, door, ...) and the held item have both declined the click, so the
// torch only goes down when nothing else would have happened. Mods that handle the click
// through Fabric's UseBlockCallback cancel before this point and are left alone.
@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {

	@Inject(method = "useItemOn", at = @At("RETURN"), cancellable = true)
	private void placeTorchWithPickaxe(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> callbackInfo) {

		if (callbackInfo.getReturnValue() != InteractionResult.PASS || !ModFeatures.PICKAXE_TORCHES.isEnabled()) {
			return;
		}

		BlockPos placementPos = PickaxeTorchPlacement.getPlacementPos(player, hand, hitResult);
		InteractionResult placementResult = PickaxeTorchPlacement.tryPlaceTorch(player, hand, hitResult);
		if (placementResult == InteractionResult.PASS) {
			return;
		}

		// A modded client predicted this placement itself (sound and arm swing included).
		// A vanilla client did not, so the server swings the arm and plays the sound for it.
		boolean isClientModded = ServerPlayNetworking.canSend(player, EnabledFeaturesPayload.TYPE);
		if (!isClientModded && placementResult.consumesAction()) {
			PickaxeTorchPlacement.sendPlaceSoundToPlayer(player, placementPos);
			placementResult = InteractionResult.SUCCESS_SERVER;
		}

		callbackInfo.setReturnValue(placementResult);
	}
}
