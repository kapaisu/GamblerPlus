package dev.mishka.gamblerplus.client;

public final class AmountFormat {
	private AmountFormat() {}

	public static long parse(String raw) {
		if (raw == null) return -1;
		String s = raw.replaceAll("[,\\s$]", "").trim();
		if (s.isEmpty()) return -1;
		long mult = 1L;
		char last = Character.toUpperCase(s.charAt(s.length() - 1));
		switch (last) {
			case 'K' -> mult = 1_000L;
			case 'M' -> mult = 1_000_000L;
			case 'B' -> mult = 1_000_000_000L;
			case 'T' -> mult = 1_000_000_000_000L;
		}
		if (mult != 1L) s = s.substring(0, s.length() - 1);
		try {
			double d = Double.parseDouble(s);
			if (d < 0) return -1;
			return (long) Math.floor(d * mult);
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	public static String pretty(long v) {
		long abs = Math.abs(v);
		String sign = v < 0 ? "-" : "";
		if (abs >= 1_000_000_000_000L) return sign + trim(abs / 1_000_000_000_000.0) + "T";
		if (abs >= 1_000_000_000L)     return sign + trim(abs / 1_000_000_000.0)     + "B";
		if (abs >= 1_000_000L)         return sign + trim(abs / 1_000_000.0)         + "M";
		if (abs >= 1_000L)             return sign + trim(abs / 1_000.0)             + "K";
		return sign + Long.toString(abs);
	}

	public static String signed(long v) {
		if (v == 0) return "0";
		return (v > 0 ? "+" : "") + pretty(v);
	}

	private static String trim(double d) {
		if (d == Math.floor(d)) return Long.toString((long) d);
		String s = String.format("%.2f", d);
		if (s.endsWith("0")) s = s.substring(0, s.length() - 1);
		if (s.endsWith(".")) s = s.substring(0, s.length() - 1);
		return s;
	}
}
