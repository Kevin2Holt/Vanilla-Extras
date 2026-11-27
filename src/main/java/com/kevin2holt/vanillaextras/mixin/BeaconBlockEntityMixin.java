package com.kevin2holt.vanillaextras.mixin;

import com.kevin2holt.vanillaextras.beacon.SaturationBeacon;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.Set;

@Mixin(BeaconBlockEntity.class)
public abstract class BeaconBlockEntityMixin {
	@Shadow @Final @Mutable
	private static Set<Holder<MobEffect>> VALID_EFFECTS;


	// VALID_EFFECTS only gates loading and syncing a saved choice, so Saturation is always allowed
	// there; a saved Saturation pick then survives the feature being toggled off and back on
	@Inject(method = "<clinit>", at = @At("TAIL"))
	private static void allowSavedSaturation(CallbackInfo callbackInfo) {

		Set<Holder<MobEffect>> validEffects = new HashSet<>(VALID_EFFECTS);
		validEffects.add(MobEffects.SATURATION);
		VALID_EFFECTS = Set.copyOf(validEffects);
	}


	// Drops a Saturation secondary while the feature is off or the pyramid has non-diamond/netherite blocks
	@WrapOperation(
		method = "tick",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/BeaconBlockEntity;applyEffects(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;ILnet/minecraft/core/Holder;Lnet/minecraft/core/Holder;)V")
	)
	private static void skipUnearnedSaturation(Level level, BlockPos beaconPos, int levels, Holder<MobEffect> primaryPower, Holder<MobEffect> secondaryPower, Operation<Void> original) {

		if (secondaryPower != null && SaturationBeacon.isSaturation(secondaryPower) && !SaturationBeacon.isSaturationOffered(level, beaconPos)) {
			secondaryPower = null;
		}

		original.call(level, beaconPos, levels, primaryPower, secondaryPower);
	}


	@Inject(method = "createMenu", at = @At("RETURN"))
	private void sendBeaconOptions(int containerId, Inventory inventory, Player player, CallbackInfoReturnable<AbstractContainerMenu> callbackInfo) {

		BlockEntity beacon = (BlockEntity) (Object) this;
		if (callbackInfo.getReturnValue() != null && player instanceof ServerPlayer serverPlayer && beacon.getLevel() != null) {
			SaturationBeacon.sendBeaconOptions(serverPlayer, beacon.getLevel(), beacon.getBlockPos());
		}
	}
}
