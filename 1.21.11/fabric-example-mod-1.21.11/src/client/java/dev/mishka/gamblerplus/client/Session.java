package dev.mishka.gamblerplus.client;

import java.util.List;

public record Session(
		long id,
		long startedAt,
		long endedAt,
		String label,
		long in,
		long out,
		int inCount,
		int outCount,
		int longestLossStreak,
		int longestWinStreak,
		List<PaymentEvent> payments
) {
	public long net() { return in - out; }
	public long durationMs() { return Math.max(0, endedAt - startedAt); }
}
