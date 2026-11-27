package com.kevin2holt.vanillaextras.beacon;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;

import java.util.List;

// Builds the beacon's effect list (one inner list per pyramid tier, matching
// BeaconBlockEntity.BEACON_EFFECTS). Tier 4 only lists the extra secondary effects; vanilla adds
// the "primary II" upgrade on its own, so Saturation as primary gives Regeneration or Saturation II.
public final class BeaconEffectTiers {

	private BeaconEffectTiers() {
	}


	public static List<List<Holder<MobEffect>>> buildEffectTiers(boolean isSaturationIncluded) {

		List<Holder<MobEffect>> tierThreeEffects = isSaturationIncluded
			? List.of(MobEffects.STRENGTH, MobEffects.SATURATION)
			: List.of(MobEffects.STRENGTH);

		return List.of(
			List.of(MobEffects.SPEED, MobEffects.HASTE),
			List.of(MobEffects.RESISTANCE, MobEffects.JUMP_BOOST),
			tierThreeEffects,
			List.of(MobEffects.REGENERATION)
		);
	}
}
