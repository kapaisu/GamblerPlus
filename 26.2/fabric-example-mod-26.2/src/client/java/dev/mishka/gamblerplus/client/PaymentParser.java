package dev.mishka.gamblerplus.client;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PaymentParser {
	private static final Pattern INCOMING = Pattern.compile(
			"(?i)(\\S+)\\s+paid\\s+you\\s+\\$?\\s*([0-9][0-9,.]*\\s*[KMBTkmbt]?)");
	private static final Pattern OUTGOING = Pattern.compile(
			"(?i)You\\s+paid\\s+(\\S+)\\s+\\$?\\s*([0-9][0-9,.]*\\s*[KMBTkmbt]?)");
	private static final Pattern WHISPER = Pattern.compile(
			"(?i)(\\bwhispers?\\b|\\btells you\\b|\\bmsg\\s+(from|to)\\b|->\\s*(me|you)\\b|\\[MSG\\]|^\\s*(From|To)\\s+\\S+\\s*:)");

	private PaymentParser() {}

	public static PaymentEvent tryParse(String line, long nowMs) {
		if (line == null || line.isEmpty()) return null;
		if (WHISPER.matcher(line).find()) return null;
		Matcher out = OUTGOING.matcher(line);
		if (out.find()) {
			long amount = AmountFormat.parse(out.group(2));
			if (amount <= 0) return null;
			return new PaymentEvent(nowMs, cleanName(out.group(1)), amount, false);
		}
		Matcher in = INCOMING.matcher(line);
		if (in.find()) {
			String head = in.group(1);
			if (head.equalsIgnoreCase("You")) return null;
			long amount = AmountFormat.parse(in.group(2));
			if (amount <= 0) return null;
			return new PaymentEvent(nowMs, cleanName(head), amount, true);
		}
		return null;
	}

	private static String cleanName(String s) {
		String r = s.replaceAll("[^A-Za-z0-9_.]", "");
		if (r.length() > 32) r = r.substring(0, 32);
		return r.isEmpty() ? "?" : r;
	}
}
