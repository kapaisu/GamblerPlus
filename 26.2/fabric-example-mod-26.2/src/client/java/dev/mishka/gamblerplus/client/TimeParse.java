package dev.mishka.gamblerplus.client;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TimeParse {
	private static final Pattern TOKEN = Pattern.compile("(?i)(\\d+(?:\\.\\d+)?)\\s*(h|m|s|ms|hr|hrs|min|mins|sec|secs)?");

	private TimeParse() {}

	public static long parseMs(String raw) {
		if (raw == null) return -1;
		String s = raw.trim().toLowerCase();
		if (s.isEmpty()) return -1;
		Matcher m = TOKEN.matcher(s);
		long total = 0;
		int last = 0;
		boolean any = false;
		while (m.find()) {
			if (m.start() != last) return -1;
			last = m.end();
			double n = Double.parseDouble(m.group(1));
			String u = m.group(2);
			long mult;
			if (u == null) {
				mult = 1000L;
			} else {
				mult = switch (u) {
					case "h", "hr", "hrs" -> 3_600_000L;
					case "m", "min", "mins" -> 60_000L;
					case "s", "sec", "secs" -> 1_000L;
					case "ms" -> 1L;
					default -> 1_000L;
				};
			}
			total += (long) Math.floor(n * mult);
			any = true;
		}
		if (!any || last != s.length()) return -1;
		if (total <= 0) return -1;
		return total;
	}

	public static String prettyDuration(long ms) {
		if (ms < 0) ms = 0;
		long s = ms / 1000L;
		long h = s / 3600L;
		long m = (s % 3600L) / 60L;
		long sec = s % 60L;
		if (h > 0) return String.format("%d:%02d:%02d", h, m, sec);
		return String.format("%d:%02d", m, sec);
	}
}
