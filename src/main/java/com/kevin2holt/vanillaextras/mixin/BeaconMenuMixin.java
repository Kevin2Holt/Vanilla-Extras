package com.kevin2holt.vanillaextras.mixin;

import com.kevin2holt.vanillaextras.beacon.SaturationBeacon;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

// Server-side check for a submitted beacon choice. Vanilla rejects any effect outside its list
// (and disconnects the player), so Saturation is accepted here only as a tier-4 secondary on a
// beacon that currently offers it.
@Mixin(BeaconMenu.class)
public abstract class BeaconMenuMixin {
	@Shadow @Final
	private ContainerLevelAccess access;


	@WrapOperation(
		method = "updateEffects",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/BeaconBlockEntity;validateEffects(Lnet/minecraft/core/Holder;Lnet/minecraft/core/Holder;I)Z")
	)
	private boolean allowSaturationSecondary(Holder<MobEffect> primary, Holder<MobEffect> secondary, int levels, Operation<Boolean> original) {

		if (secondary == null || !SaturationBeacon.isSaturation(secondary)) {
			return original.call(primary, secondary, levels);
		}

		// Same rules as a vanilla secondary: needs a full pyramid and a valid primary below tier 4
		boolean isPrimaryValid = primary != null && original.call(primary, null, levels);
		boolean isSaturationOffered = this.access.evaluate(SaturationBeacon::isSaturationOffered, false);

		return levels >= 4 && isPrimaryValid && isSaturationOffered;
	}
}
