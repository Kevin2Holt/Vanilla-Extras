package com.kevin2holt.vanillaextras.mixin;

import com.kevin2holt.vanillaextras.beacon.SaturationBeacon;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.inventory.BeaconMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// Server-side check for a submitted beacon choice. Saturation shares Regeneration's slot, so it
// passes under exactly the same rules; which of the two actually applies is decided live.
@Mixin(BeaconMenu.class)
public abstract class BeaconMenuMixin {

	@WrapOperation(
		method = "updateEffects",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/BeaconBlockEntity;validateEffects(Lnet/minecraft/core/Holder;Lnet/minecraft/core/Holder;I)Z")
	)
	private boolean validateSaturationAsRegeneration(Holder<MobEffect> primary, Holder<MobEffect> secondary, int levels, Operation<Boolean> original) {

		if (secondary != null && SaturationBeacon.isSaturation(secondary)) {
			return original.call(primary, MobEffects.REGENERATION, levels);
		}

		return original.call(primary, secondary, levels);
	}
}
