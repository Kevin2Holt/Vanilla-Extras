package com.kevin2holt.vanillaextras.refill;

import com.kevin2holt.vanillaextras.feature.ModFeatures;
import com.kevin2holt.vanillaextras.inventory.CarriedItemSearch;
import com.kevin2holt.vanillaextras.inventory.ItemSource;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

// Server half of auto-refill. A modded client notices its held stack running low and sends a
// RefillRequestPayload; this moves matching items into that slot from the player's own items.
// Items are only moved, never created, so a forged request can at worst tidy the sender's inventory.
//
// Where items come from, and in what order, is CarriedItemSearch's job.
public final class AutoRefill {

	private AutoRefill() {
	}


	public static void register() {

		PayloadTypeRegistry.serverboundPlay().register(RefillRequestPayload.TYPE, RefillRequestPayload.STREAM_CODEC);

		ServerPlayNetworking.registerGlobalReceiver(RefillRequestPayload.TYPE, (payload, context) -> {
			handleRefillRequest(context.player(), payload);
		});
	}



	private static void handleRefillRequest(ServerPlayer player, RefillRequestPayload request) {

		if (!ModFeatures.AUTO_REFILL.isEnabled() || player.isCreative() || player.isSpectator()) {
			return;
		}

		int targetSlot = request.inventorySlot();
		if (!isRefillableSlot(targetSlot) || request.template().isEmpty()) {
			return;
		}

		Inventory inventory = player.getInventory();
		ItemStack template = request.template().copyWithCount(1);
		ItemStack targetStack = inventory.getItem(targetSlot);

		// The slot changed since the client asked (another mod refilled it, or the player swapped items)
		if (!targetStack.isEmpty() && !RefillRules.isRefillMatch(template, targetStack)) {
			return;
		}

		int currentCount = targetStack.getCount();
		int maxStackSize = template.getMaxStackSize();
		int triggerCount = RefillRules.clampTrigger(request.triggerCount());
		int refillAmount = RefillRules.clampAmount(request.refillAmount());

		// Rechecked against the server's copy: this is also where yielding to other refill mods pays off
		if (!RefillRules.shouldRefill(currentCount, maxStackSize, triggerCount)) {
			return;
		}

		int itemsWanted = RefillRules.getTargetCount(currentCount, maxStackSize, triggerCount, refillAmount) - currentCount;
		ItemStack pulledItems = pullItems(player, template, targetSlot, itemsWanted, request.isFromContainersAllowed());
		if (pulledItems.isEmpty()) {
			return;
		}

		if (targetStack.isEmpty()) {
			inventory.setItem(targetSlot, pulledItems);
		} else {
			targetStack.grow(pulledItems.getCount());
		}
		inventory.setChanged();
	}


	private static boolean isRefillableSlot(int inventorySlot) {

		return Inventory.isHotbarSlot(inventorySlot) || inventorySlot == Inventory.SLOT_OFFHAND;
	}


	// Returns everything gathered as one stack (empty if nothing matched)
	private static ItemStack pullItems(ServerPlayer player, ItemStack template, int targetSlot, int itemsWanted, boolean isFromContainersAllowed) {

		ItemStack gathered = ItemStack.EMPTY;

		for (ItemSource source : CarriedItemSearch.getSources(player, targetSlot, isFromContainersAllowed)) {
			// Keep taking from one source until it runs dry, since a container can hold several matching stacks
			while (gathered.getCount() < itemsWanted) {
				ItemStack gatheredSoFar = gathered;
				Predicate<ItemStack> isUsable = stack -> RefillRules.isRefillMatch(template, stack)
					&& (gatheredSoFar.isEmpty() || ItemStack.isSameItemSameComponents(gatheredSoFar, stack));

				ItemStack taken = source.take(isUsable, itemsWanted - gathered.getCount());
				if (taken.isEmpty()) {
					break;
				}

				if (gathered.isEmpty()) {
					gathered = taken;
				} else {
					gathered.grow(taken.getCount());
				}
			}

			if (gathered.getCount() >= itemsWanted) {
				break;
			}
		}

		return gathered;
	}
}
