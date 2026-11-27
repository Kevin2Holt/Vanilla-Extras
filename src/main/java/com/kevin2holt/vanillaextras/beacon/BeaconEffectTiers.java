package com.kevin2holt.vanillaextras.beacon;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;

import java.util.List;

// Builds the beacon screen's effect list (one inner list per pyramid tier, matching
// BeaconBlockEntity.BEACON_EFFECTS). Tier 4 only lists the extra secondary effects; vanilla adds
// the "primary II" upgrade on its own. On a saturation pyramid, Saturation replaces Regeneration.
public final class BeaconEffectTiers {

	private BeaconEffectTiers() {
	}


	public static List<List<Holder<MobEffect>>> buildEffectTiers(boolean isSaturationOffered) {

		List<Holder<MobEffect>> tierFourEffects = isSaturationOffered
			? List.of(MobEffects.SATURATION)
			: List.of(MobEffects.REGENERATION);

		return List.of(
			List.of(MobEffects.SPEED, MobEffects.HASTE),
			List.of(MobEffects.RESISTANCE, MobEffects.JUMP_BOOST),
			List.of(MobEffects.STRENGTH),
			tierFourEffects
		);
	}
}
