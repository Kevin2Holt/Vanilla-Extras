package com.kevin2holt.vanillaextras.client.settings;

import com.kevin2holt.vanillaextras.config.IntSetting;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

// Widget factories shared by every settings screen
public final class SettingsWidgets {
	public static final int FULL_WIDTH = 310;// matches vanilla's wide option rows
	public static final int HALF_WIDTH = 150;// matches vanilla's two-column option rows
	public static final int ROW_HEIGHT = 20;


	private SettingsWidgets() {
	}


	public static Component formatLabeledValue(Component label, Component value) {

		return CommonComponents.optionNameValue(label, value);
	}


	public static Component formatOnOff(Component label, boolean isOn) {

		return formatLabeledValue(label, isOn ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF);
	}


	public static void applyTooltip(AbstractWidget widget, Component tooltip) {

		if (tooltip != null) {
			widget.setTooltip(Tooltip.create(tooltip));
		}
	}


	// An on/off button. messageForValue builds the button text, so callers can add markers
	// (like a restart note) without a separate widget.
	public static Button createToggle(int width, Supplier<Boolean> currentValue, Consumer<Boolean> valueSetter, Function<Boolean, Component> messageForValue, Component tooltip) {

		Button toggle = Button.builder(messageForValue.apply(currentValue.get()), button -> {
			boolean newValue = !currentValue.get();
			valueSetter.accept(newValue);
			button.setMessage(messageForValue.apply(newValue));
		}).width(width).build();

		applyTooltip(toggle, tooltip);
		return toggle;
	}


	public static AbstractWidget createSlider(int width, IntSetting setting, Component label, IntFunction<Component> valueText, Component tooltip) {

		IntSettingSlider slider = new IntSettingSlider(width, setting, label, valueText);
		applyTooltip(slider, tooltip);
		return slider;
	}



	// Slider bound to an IntSetting; saves whenever the whole-number value changes
	private static final class IntSettingSlider extends AbstractSliderButton {
		private final IntSetting setting;
		private final Component label;
		private final IntFunction<Component> valueText;
		private int savedValue;


		private IntSettingSlider(int width, IntSetting setting, Component label, IntFunction<Component> valueText) {

			super(0, 0, width, ROW_HEIGHT, Component.empty(), convertToSliderPosition(setting, setting.get()));
			this.setting = setting;
			this.label = label;
			this.valueText = valueText;
			this.savedValue = setting.get();
			this.updateMessage();
		}


		private static double convertToSliderPosition(IntSetting setting, int value) {

			int range = setting.maxValue() - setting.minValue();
			if (range <= 0) {
				return 0;
			}

			return (double) (value - setting.minValue()) / range;
		}


		private int getSelectedValue() {

			int range = this.setting.maxValue() - this.setting.minValue();
			return this.setting.minValue() + (int) Math.round(this.value * range);
		}


		@Override
		protected void updateMessage() {

			this.setMessage(formatLabeledValue(this.label, this.valueText.apply(this.getSelectedValue())));
		}


		@Override
		protected void applyValue() {

			int selectedValue = this.getSelectedValue();
			if (selectedValue != this.savedValue) {
				this.setting.set(selectedValue);
				this.savedValue = selectedValue;
			}
		}
	}
}
