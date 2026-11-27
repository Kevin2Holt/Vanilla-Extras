package com.kevin2holt.vanillaextras.mixin;

import com.kevin2holt.vanillaextras.beacon.SaturationBeacon;
import com.kevin2holt.vanillaextras.feature.ModFeatures;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.inventory.BeaconMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Optional;

// Server-side check for a submitted beacon choice. Saturation shares Regeneration's slot, so it
// passes under exactly the same rules; which of the two actually applies is decided live.
@Mixin(BeaconMenu.class)
public abstract class BeaconMenuMixin {
	private static final int SECONDARY_LAYERS_REQUIRED = 4;// vanilla only allows a secondary effect at 4 layers


	@Shadow
	public abstract int getLevels();


	// The screen resends the stored secondary with every confirm. After the base shrinks below
	// 4 layers that stale secondary makes vanilla reject the whole choice, so drop it and treat the
	// confirm as a fresh pick, the same as on a newly placed beacon
	@ModifyVariable(method = "updateEffects", at = @At("HEAD"), argsOnly = true, ordinal = 1)
	private Optional<Holder<MobEffect>> dropUnreachableSecondary(Optional<Holder<MobEffect>> secondary) {

		if (ModFeatures.SATURATION_BEACON.isEnabled() && this.getLevels() < SECONDARY_LAYERS_REQUIRED) {
			return Optional.empty();
		}

		return secondary;
	}


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
