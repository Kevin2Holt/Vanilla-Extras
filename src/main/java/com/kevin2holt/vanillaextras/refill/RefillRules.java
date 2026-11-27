package com.kevin2holt.vanillaextras.refill;

import net.minecraft.world.item.ItemStack;

// When a held stack should refill and how big it should end up. Shared by the client (which
// decides when to ask) and the server (which checks the ask against its own inventory copy).
//
// triggerCount: refill once the stack is at or below this many items. A trigger at or above the
//     item's max stack size refills on every use, back to a full stack.
// refillAmount: above the trigger, the size to refill back to (capped at the max stack size).
//     At or below the trigger, how many items to add instead.
public final class RefillRules {
	public static final int TRIGGER_MIN = 0;
	public static final int TRIGGER_MAX = 63;
	public static final int TRIGGER_DEFAULT = 8;
	public static final int AMOUNT_MIN = 1;
	public static final int AMOUNT_MAX = 64;
	public static final int AMOUNT_DEFAULT = 64;// full stack


	private RefillRules() {
	}


	public static boolean shouldRefill(int currentCount, int maxStackSize, int triggerCount) {

		// Capping at maxStackSize - 1 makes a high trigger mean "any use" for small stacks
		return currentCount < maxStackSize && currentCount <= Math.min(triggerCount, maxStackSize - 1);
	}


	public static int getTargetCount(int currentCount, int maxStackSize, int triggerCount, int refillAmount) {

		if (triggerCount >= maxStackSize) {
			return maxStackSize;
		}

		if (refillAmount > triggerCount) {
			return Math.min(refillAmount, maxStackSize);
		}

		return Math.min(currentCount + refillAmount, maxStackSize);
	}


	// Stackables must match exactly (same potion, same name). Damageable items such as tools and
	// weapons match by item alone, so a fresh pickaxe replaces a broken one.
	public static boolean isRefillMatch(ItemStack template, ItemStack candidate) {

		if (template.isEmpty() || candidate.isEmpty()) {
			return false;
		}

		if (template.isDamageableItem()) {
			return ItemStack.isSameItem(template, candidate);
		}

		return ItemStack.isSameItemSameComponents(template, candidate);
	}


	public static int clampTrigger(int triggerCount) {

		return Math.clamp(triggerCount, TRIGGER_MIN, TRIGGER_MAX);
	}


	public static int clampAmount(int refillAmount) {

		return Math.clamp(refillAmount, AMOUNT_MIN, AMOUNT_MAX);
	}
}
