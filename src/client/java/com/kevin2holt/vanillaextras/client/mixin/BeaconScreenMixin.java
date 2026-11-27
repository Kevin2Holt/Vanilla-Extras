package com.kevin2holt.vanillaextras.client.mixin;

import com.kevin2holt.vanillaextras.beacon.BeaconEffectTiers;
import com.kevin2holt.vanillaextras.client.ServerFeatures;
import com.kevin2holt.vanillaextras.feature.ModFeatures;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.gui.screens.inventory.BeaconScreen;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

// Builds the beacon buttons from what the connected server allows, not from this client's
// own config. Picking Saturation on a server that doesn't allow it fails the server's check
// and disconnects the player, so the button only shows when the server announced the feature.
@Mixin(BeaconScreen.class)
public abstract class BeaconScreenMixin {

	@ModifyExpressionValue(
		method = "init",
		at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/block/entity/BeaconBlockEntity;BEACON_EFFECTS:Ljava/util/List;")
	)
	private List<List<Holder<MobEffect>>> useServerBeaconEffects(List<List<Holder<MobEffect>>> localBeaconEffects) {

		return BeaconEffectTiers.buildEffectTiers(ServerFeatures.isEnabledOnServer(ModFeatures.SATURATION_BEACON));
	}
}
