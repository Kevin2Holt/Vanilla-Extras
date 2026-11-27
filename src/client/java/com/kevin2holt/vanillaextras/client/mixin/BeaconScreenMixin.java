package com.kevin2holt.vanillaextras.client.mixin;

import com.kevin2holt.vanillaextras.beacon.BeaconEffectTiers;
import com.kevin2holt.vanillaextras.client.beacon.BeaconScreenOptions;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.gui.screens.inventory.BeaconScreen;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

// Builds the beacon buttons from what the server said about this beacon: Saturation's icon takes
// Regeneration's place when every block of the base is diamond and/or netherite. Both share one
// slot on the server, so either pick is always accepted.
@Mixin(BeaconScreen.class)
public abstract class BeaconScreenMixin {

	@ModifyExpressionValue(
		method = "init",
		at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/block/entity/BeaconBlockEntity;BEACON_EFFECTS:Ljava/util/List;")
	)
	private List<List<Holder<MobEffect>>> useServerBeaconEffects(List<List<Holder<MobEffect>>> localBeaconEffects) {

		return BeaconEffectTiers.buildEffectTiers(BeaconScreenOptions.isSaturationOffered());
	}
}
