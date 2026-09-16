package dev.mishka.gamblerplus.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Multiplier {
	private Multiplier() {}

	public static String labelFor(List<PaymentEvent> newestFirst, int i) {
		if (i < 0 || i >= newestFirst.size()) return null;
		PaymentEvent e = newestFirst.get(i);
		if (!e.incoming()) return null;
		for (int j = i + 1; j < newestFirst.size(); j++) {
			PaymentEvent p = newestFirst.get(j);
			if (p.incoming()) continue;
			if (!p.player().equals(e.player())) continue;
			if (p.amount() <= 0) return null;
			double ratio = (double) e.amount() / (double) p.amount();
			for (int step = 4; step <= 18; step++) {
				double m = step * 0.5;
				if (Math.abs(ratio - m) <= 0.01 * m) {
					return format(m);
				}
			}
			return null;
		}
		return null;
	}

	public static String labelForRatio(long incomingAmount, long originalAmount) {
		if (originalAmount <= 0) return null;
		double ratio = (double) incomingAmount / (double) originalAmount;
		for (int step = 4; step <= 18; step++) {
			double m = step * 0.5;
			if (Math.abs(ratio - m) <= 0.01 * m) {
				return format(m);
			}
		}
		return null;
	}

	public static Map<PaymentEvent, String> labelsFor(List<PaymentEvent> events) {
		List<PaymentEvent> sorted = new ArrayList<>(events);
		sorted.sort(Comparator.comparingLong(PaymentEvent::timestampMs).reversed());
		Map<PaymentEvent, String> out = new HashMap<>();
		for (int i = 0; i < sorted.size(); i++) {
			String label = labelFor(sorted, i);
			if (label != null) out.put(sorted.get(i), label);
		}
		return out;
	}

	private static String format(double m) {
		if (m == Math.floor(m)) return "(" + (int) m + "x)";
		return "(" + m + "x)";
	}
}
