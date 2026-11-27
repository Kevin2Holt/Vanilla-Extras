package com.kevin2holt.vanillaextras.client.refill;

import com.kevin2holt.vanillaextras.client.ServerFeatures;
import com.kevin2holt.vanillaextras.feature.ModFeatures;
import com.kevin2holt.vanillaextras.refill.AutoRefillSettings;
import com.kevin2holt.vanillaextras.refill.RefillRequestPayload;
import com.kevin2holt.vanillaextras.refill.RefillRules;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

// Client half of auto-refill: watches the main-hand and off-hand stacks, and when one drops to
// the player's trigger, asks the server to refill it. The server does the actual moving, because
// only it can see inside shulker boxes, bundles, and the ender chest.
//
// Yielding: after a stack drops low we wait YIELD_TICKS before asking. If another mod (or the
// player) refilled it in the meantime, the request is dropped.
public final class AutoRefillClient {
	private static final int YIELD_TICKS = 2;

	private static final HandWatcher mainHandWatcher = new HandWatcher();
	private static final HandWatcher offHandWatcher = new HandWatcher();
	private static long clientTick;


	private AutoRefillClient() {
	}


	public static void register() {

		ClientTickEvents.END_CLIENT_TICK.register(AutoRefillClient::onClientTick);

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			mainHandWatcher.reset();
			offHandWatcher.reset();
		});
	}



	private static void onClientTick(Minecraft client) {

		clientTick++;
		LocalPlayer player = client.player;

		if (player == null || !isRefillAvailable(player)) {
			mainHandWatcher.reset();
			offHandWatcher.reset();
			return;
		}

		// With a screen open, stack changes are the player rearranging items, not using them
		boolean isScreenOpen = client.gui.screen() != null;
		Inventory inventory = player.getInventory();

		mainHandWatcher.update(inventory.getSelectedSlot(), inventory.getSelectedItem(), isScreenOpen);
		offHandWatcher.update(Inventory.SLOT_OFFHAND, inventory.getItem(Inventory.SLOT_OFFHAND), isScreenOpen);
	}


	// Vanilla servers and servers with the feature off never get requests
	private static boolean isRefillAvailable(LocalPlayer player) {

		return ModFeatures.AUTO_REFILL.isEnabled()
			&& ServerFeatures.isEnabledOnServer(ModFeatures.AUTO_REFILL)
			&& ClientPlayNetworking.canSend(RefillRequestPayload.TYPE)
			&& !player.isCreative()
			&& !player.isSpectator();
	}


	private static void sendRefillRequest(int inventorySlot, ItemStack template) {

		// Checked only when a refill is due, so the reflection and file reads stay rare
		if (OtherRefillMods.isAnyOtherRefillActive()) {
			return;
		}

		ClientPlayNetworking.send(new RefillRequestPayload(
			inventorySlot,
			template.copyWithCount(1),
			AutoRefillSettings.TRIGGER_COUNT.get(),
			AutoRefillSettings.REFILL_AMOUNT.get(),
			AutoRefillSettings.FROM_CONTAINERS.get()
		));
	}



	// Tracks one hand between ticks to spot a stack shrinking through use
	private static final class HandWatcher {
		private int lastSlot = -1;
		private ItemStack lastStack = ItemStack.EMPTY;
		private ItemStack pendingTemplate = ItemStack.EMPTY;
		private long pendingDueTick;


		void reset() {

			this.lastSlot = -1;
			this.lastStack = ItemStack.EMPTY;
			this.pendingTemplate = ItemStack.EMPTY;
		}


		void update(int slot, ItemStack currentStack, boolean isScreenOpen) {

			// Switching hotbar slots or opening a screen is never a "use"
			if (slot != this.lastSlot || isScreenOpen) {
				this.remember(slot, currentStack);
				this.pendingTemplate = ItemStack.EMPTY;
				return;
			}

			int triggerCount = AutoRefillSettings.TRIGGER_COUNT.get();

			if (this.isUsedDown(currentStack) && RefillRules.shouldRefill(currentStack.getCount(), this.lastStack.getMaxStackSize(), triggerCount)) {
				this.pendingTemplate = this.lastStack.copyWithCount(1);
				this.pendingDueTick = clientTick + YIELD_TICKS;
			}

			this.remember(slot, currentStack);

			if (this.pendingTemplate.isEmpty() || clientTick < this.pendingDueTick) {
				return;
			}

			ItemStack template = this.pendingTemplate;
			this.pendingTemplate = ItemStack.EMPTY;

			// Still low after the wait: nothing else refilled it, so it's our turn
			boolean isStillSameItem = currentStack.isEmpty() || RefillRules.isRefillMatch(template, currentStack);
			if (isStillSameItem && RefillRules.shouldRefill(currentStack.getCount(), template.getMaxStackSize(), triggerCount)) {
				sendRefillRequest(slot, template);
			}
		}


		// The same item got smaller, or the last of it was used up
		private boolean isUsedDown(ItemStack currentStack) {

			if (this.lastStack.isEmpty() || currentStack.getCount() >= this.lastStack.getCount()) {
				return false;
			}

			return currentStack.isEmpty() || RefillRules.isRefillMatch(this.lastStack, currentStack);
		}


		private void remember(int slot, ItemStack currentStack) {

			this.lastSlot = slot;
			this.lastStack = currentStack.copy();
		}
	}
}
