package dev.mishka.gamblerplus.client;

import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.Minecraft;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PaymentIntercept {
	private static final Pattern PAY_CMD = Pattern.compile(
			"^\\s*pay\\s+(\\S+)\\s+(\\S+)(?:\\s+.*)?$",
			Pattern.CASE_INSENSITIVE);

	private static volatile boolean bypassOnce = false;

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
			String recipient = m.group(1).replaceAll("[^A-Za-z0-9_]", "");
			if (recipient.isEmpty()) return true;
			Minecraft mc = Minecraft.getInstance();
			mc.execute(() -> mc.setScreenAndShow(new VerifyPaymentScreen(recipient, amount, command)));
			return false;
		});
	}

	public static void markConfirmed() {
		bypassOnce = true;
	}
}
