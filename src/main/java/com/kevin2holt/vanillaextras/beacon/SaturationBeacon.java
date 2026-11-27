package com.kevin2holt.vanillaextras.beacon;

import com.kevin2holt.vanillaextras.feature.ModFeatures;
import com.kevin2holt.vanillaextras.network.BeaconOptionsPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

// Saturation and Regeneration share the beacon's tier-4 "healing" secondary slot. Which one a
// beacon gives is decided live from its base each time effects apply: a full 4-layer base made
// only of diamond and/or netherite blocks gives Saturation, anything else gives Regeneration.
// Either effect can be stored as the choice (vanilla clients only ever send Regeneration).
public final class SaturationBeacon {
	private static final int SECONDARY_LAYERS_REQUIRED = 4;// vanilla only gives a secondary effect at 4 layers


	private SaturationBeacon() {
	}


	public static void register() {

		PayloadTypeRegistry.clientboundPlay().register(BeaconOptionsPayload.TYPE, BeaconOptionsPayload.STREAM_CODEC);
	}


	public static boolean isSaturation(Holder<MobEffect> effect) {

		return MobEffects.SATURATION.equals(effect);
	}


	public static boolean isHealingSlotEffect(Holder<MobEffect> effect) {

		return MobEffects.REGENERATION.equals(effect) || MobEffects.SATURATION.equals(effect);
	}


	// The effect the healing slot gives right now, or the stored effect unchanged for other secondaries
	public static Holder<MobEffect> resolveSecondaryEffect(Holder<MobEffect> storedSecondary, Level level, BlockPos beaconPos, int levels) {

		if (storedSecondary == null || !isHealingSlotEffect(storedSecondary)) {
			return storedSecondary;
		}

		boolean isSaturationEarned = ModFeatures.SATURATION_BEACON.isEnabled()
			&& levels >= SECONDARY_LAYERS_REQUIRED
			&& isSaturationBase(level, beaconPos, SECONDARY_LAYERS_REQUIRED);

		return isSaturationEarned ? MobEffects.SATURATION : MobEffects.REGENERATION;
	}


	// Whether the beacon screen shows Saturation in place of Regeneration: the base's completed
	// layers, however many there are, are all diamond and/or netherite
	public static boolean isSaturationShown(Level level, BlockPos beaconPos, int levels) {

		return ModFeatures.SATURATION_BEACON.isEnabled() && levels >= 1 && isSaturationBase(level, beaconPos, levels);
	}


	// Checks every block of the given number of layers (3x3, 5x5, 7x7, 9x9 going down)
	public static boolean isSaturationBase(Level level, BlockPos beaconPos, int layerCount) {

		for (int layer = 1; layer <= layerCount; layer++) {
			int layerY = beaconPos.getY() - layer;
			if (layerY < level.getMinY()) {
				return false;
			}

			for (int x = beaconPos.getX() - layer; x <= beaconPos.getX() + layer; x++) {
				for (int z = beaconPos.getZ() - layer; z <= beaconPos.getZ() + layer; z++) {
					if (!isSaturationBaseBlock(level.getBlockState(new BlockPos(x, layerY, z)))) {
						return false;
					}
				}
			}
		}

		return true;
	}


	private static boolean isSaturationBaseBlock(BlockState state) {

		return state.is(Blocks.DIAMOND_BLOCK) || state.is(Blocks.NETHERITE_BLOCK);
	}


	// Tells a modded client which healing-slot icon to show; sent just before the beacon screen opens
	public static void sendBeaconOptions(ServerPlayer player, Level level, BlockPos beaconPos, int levels) {

		// Vanilla clients have not registered the channel, so they are skipped here
		if (!ServerPlayNetworking.canSend(player, BeaconOptionsPayload.TYPE)) {
			return;
		}

		ServerPlayNetworking.send(player, new BeaconOptionsPayload(isSaturationShown(level, beaconPos, levels)));
	}
}
