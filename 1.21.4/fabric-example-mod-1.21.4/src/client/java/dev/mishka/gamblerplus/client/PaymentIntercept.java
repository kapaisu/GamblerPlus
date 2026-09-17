package dev.mishka.gamblerplus.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PaymentIntercept {
	private static final Pattern PAY_CMD = Pattern.compile(
			"^\\s*pay\\s+(\\S+)\\s+(\\S+)(?:\\s+.*)?$",
			Pattern.CASE_INSENSITIVE);

	private static volatile boolean bypassOnce = false;
	private static volatile Screen pending;

	private PaymentIntercept() {}

	public static void register(Config config) {
		ClientSendMessageEvents.ALLOW_COMMAND.register(command -> {
			if (bypassOnce) { bypassOnce = false; return true; }
			if (!config.verifyLargePayments()) return true;
			Matcher m = PAY_CMD.matcher(command);
			if (!m.matches()) return true;
			long amount = AmountFormat.parse(m.group(2));
			if (amount <= 0) return true;
			if (amount < config.largePaymentThreshold()) return true;
			String recipient = m.group(1).replaceAll("[^A-Za-z0-9_.]", "");
			if (recipient.isEmpty()) return true;
			pending = new VerifyPaymentScreen(recipient, amount, command);
			return false;
		});
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			Screen p = pending;
			if (p != null) {
				pending = null;
				mc.setScreen(p);
			}
		});
	}

	public static void markConfirmed() {
		bypassOnce = true;
	}

	public static void openLater(Screen screen) {
		pending = screen;
	}
}
