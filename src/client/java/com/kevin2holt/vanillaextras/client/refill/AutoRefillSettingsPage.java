package com.kevin2holt.vanillaextras.client.refill;

import com.kevin2holt.vanillaextras.client.settings.SettingsMenu;
import com.kevin2holt.vanillaextras.refill.AutoRefillSettings;
import com.kevin2holt.vanillaextras.refill.RefillRules;
import net.minecraft.network.chat.Component;

// The Auto-Refill page of the in-game settings menu
public final class AutoRefillSettingsPage {
	private static final Component PAGE_TITLE = Component.literal("Auto-Refill");

	private static final Component TRIGGER_LABEL = Component.literal("Refill at");
	private static final Component TRIGGER_TOOLTIP = Component.literal(
		"Refill a held stack once it is down to this many items. "
		+ "Above 15, items that stack to 16 refill on every use.");

	private static final Component AMOUNT_LABEL = Component.literal("Refill");
	private static final Component AMOUNT_TOOLTIP = Component.literal(
		"Above the trigger: the stack size to refill back to (items that stack to 16 stop at 16). "
		+ "At or below the trigger: how many items to add each time.");

	private static final Component CONTAINERS_LABEL = Component.literal("Pull from containers");
	private static final Component CONTAINERS_TOOLTIP = Component.literal(
		"Also take items from shulker boxes and bundles in your inventory, and from your ender chest "
		+ "if you carry one. Only one layer deep, except shulker boxes in the ender chest named "
		+ "\"Refill...\" or \"Auto-Refill...\", which are searched too.");


	private AutoRefillSettingsPage() {
	}


	public static void register() {

		SettingsMenu.registerPage(PAGE_TITLE, page -> {
			page.addSlider(AutoRefillSettings.TRIGGER_COUNT, TRIGGER_LABEL, AutoRefillSettingsPage::describeTrigger, TRIGGER_TOOLTIP);
			page.addSlider(AutoRefillSettings.REFILL_AMOUNT, AMOUNT_LABEL, AutoRefillSettingsPage::describeAmount, AMOUNT_TOOLTIP);
			page.addToggle(AutoRefillSettings.FROM_CONTAINERS, CONTAINERS_LABEL, CONTAINERS_TOOLTIP);
		});
	}



	private static Component describeTrigger(int triggerCount) {

		if (triggerCount == RefillRules.TRIGGER_MIN) {
			return Component.literal("Empty");
		}

		return Component.literal(triggerCount + " left");
	}


	private static Component describeAmount(int refillAmount) {

		// Reads the trigger live so the wording flips as soon as either slider crosses the other
		if (refillAmount <= AutoRefillSettings.TRIGGER_COUNT.get()) {
			return Component.literal("Add " + refillAmount);
		}

		if (refillAmount == RefillRules.AMOUNT_MAX) {
			return Component.literal("Full stack");
		}

		return Component.literal("Up to " + refillAmount);
	}
}
