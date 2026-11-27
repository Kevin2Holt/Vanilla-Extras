package com.kevin2holt.vanillaextras.client.settings;

import com.kevin2holt.vanillaextras.config.BooleanSetting;
import com.kevin2holt.vanillaextras.config.IntSetting;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

// Collects the rows of one settings page, top to bottom. Tooltips are optional (null for none).
public final class SettingsPageBuilder {
	private final List<SettingsEntry> entries = new ArrayList<>();


	public SettingsPageBuilder addHeader(Component text) {

		this.entries.add(new SettingsEntry.Header(text));
		return this;
	}


	public SettingsPageBuilder addSlider(IntSetting setting, Component label) {

		return this.addSlider(setting, label, value -> Component.literal(Integer.toString(value)), null);
	}


	public SettingsPageBuilder addSlider(IntSetting setting, Component label, Component tooltip) {

		return this.addSlider(setting, label, value -> Component.literal(Integer.toString(value)), tooltip);
	}


	// valueText turns the current number into what the slider shows after "label: "
	public SettingsPageBuilder addSlider(IntSetting setting, Component label, IntFunction<Component> valueText) {

		return this.addSlider(setting, label, valueText, null);
	}


	public SettingsPageBuilder addSlider(IntSetting setting, Component label, IntFunction<Component> valueText, Component tooltip) {

		this.entries.add(new SettingsEntry.Slider(setting, label, valueText, tooltip));
		return this;
	}


	public SettingsPageBuilder addToggle(BooleanSetting setting, Component label) {

		return this.addToggle(setting, label, null);
	}


	public SettingsPageBuilder addToggle(BooleanSetting setting, Component label, Component tooltip) {

		this.entries.add(new SettingsEntry.Toggle(setting, label, tooltip));
		return this;
	}


	List<SettingsEntry> getEntries() {

		return List.copyOf(this.entries);
	}



	sealed interface SettingsEntry {
		record Header(Component text) implements SettingsEntry {
		}

		record Slider(IntSetting setting, Component label, IntFunction<Component> valueText, Component tooltip) implements SettingsEntry {
		}

		record Toggle(BooleanSetting setting, Component label, Component tooltip) implements SettingsEntry {
		}
	}
}
