package dev.mishka.gamblerplus.client;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PurchaseParser {
	public static final long MIN_AMOUNT = 10_000_000L;

	private static final Pattern BOUGHT = Pattern.compile(
			"(?i)^\\s*You\\s+(?:bought|purchased)\\s+(\\d+)\\s+(.+?)\\s+for\\s+\\$?\\s*([0-9][0-9,.]*\\s*[KMBTkmbt]?)\\s*[.!]?\\s*$");
	private static final Pattern WHISPER = Pattern.compile(
			"(?i)(\\bwhispers?\\b|\\btells you\\b|\\bmsg\\s+(from|to)\\b|->\\s*(me|you)\\b|\\[MSG\\]|^\\s*(From|To)\\s+\\S+\\s*:)");

	public record Purchase(int count, String item, long amount) {}

	private PurchaseParser() {}

	public static Purchase tryParse(String line) {
		if (line == null || line.isEmpty()) return null;
		if (WHISPER.matcher(line).find()) return null;
		Matcher m = BOUGHT.matcher(line);
		if (!m.matches()) return null;
		int count;
		try {
			count = Integer.parseInt(m.group(1));
		} catch (NumberFormatException e) {
			return null;
		}
		if (count <= 0) return null;
		String item = m.group(2).trim();
		if (item.isEmpty()) return null;
		long amount = AmountFormat.parse(m.group(3));
		if (amount <= 0) return null;
		return new Purchase(count, item, amount);
	}
}
