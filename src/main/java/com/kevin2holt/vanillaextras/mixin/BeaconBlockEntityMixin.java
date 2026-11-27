package com.kevin2holt.vanillaextras.mixin;

import com.kevin2holt.vanillaextras.beacon.BeaconEffectTiers;
import com.kevin2holt.vanillaextras.feature.ModFeatures;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// Swaps in the effect list with Saturation in tier 3. Vanilla reads BEACON_EFFECTS for the
// server-side selection check (validateEffects) and VALID_EFFECTS for loading/syncing, so
// replacing both is all the server needs.
@Mixin(BeaconBlockEntity.class)
public abstract class BeaconBlockEntityMixin {
	@Shadow @Final @Mutable
	public static List<List<Holder<MobEffect>>> BEACON_EFFECTS;

	@Shadow @Final @Mutable
	private static Set<Holder<MobEffect>> VALID_EFFECTS;


	@Inject(method = "<clinit>", at = @At("TAIL"))
	private static void replaceBeaconEffects(CallbackInfo callbackInfo) {

		if (!ModFeatures.SATURATION_BEACON.isEnabled()) {
			return;
		}

		BEACON_EFFECTS = BeaconEffectTiers.buildEffectTiers(true);
		VALID_EFFECTS = BEACON_EFFECTS.stream().flatMap(Collection::stream).collect(Collectors.toSet());
	}
}
