package dev.mishka.gamblerplus.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Auction {
	public static final long LINGER_MS = 10_000L;

	public record Bid(String player, long amount) {}

	private volatile boolean active = false;
	private volatile String item = "";
	private volatile int itemCount = 1;
	private volatile long startedAtMs = 0L;
	private volatile long endsAtMs = 0L;
	private volatile long endedAtMs = 0L;
	private volatile Bid winner = null;
	private volatile boolean pendingCelebration = false;

	private final LinkedHashMap<String, Long> totals = new LinkedHashMap<>();

	public synchronized boolean start(String item, int count, long durationMs) {
		if (active) return false;
		this.item = item == null || item.isBlank() ? "unnamed" : item;
		this.itemCount = Math.max(1, count);
		this.startedAtMs = System.currentTimeMillis();
		this.endsAtMs = this.startedAtMs + Math.max(1_000L, durationMs);
		this.winner = null;
		this.totals.clear();
		this.active = true;
		return true;
	}

	public synchronized void stop() {
		if (!active) return;
		this.active = false;
		this.winner = topBid();
		this.endsAtMs = System.currentTimeMillis();
		this.endedAtMs = this.endsAtMs;
		this.pendingCelebration = this.winner != null;
	}

	public synchronized void cancel() {
		this.active = false;
		this.winner = null;
		this.endedAtMs = 0L;
		this.totals.clear();
	}

	public synchronized void tick() {
		if (!active) return;
		if (System.currentTimeMillis() >= endsAtMs) {
			this.active = false;
			this.winner = topBid();
			this.endedAtMs = endsAtMs;
			this.pendingCelebration = this.winner != null;
		}
	}

	public synchronized boolean consumeCelebration() {
		if (!pendingCelebration) return false;
		pendingCelebration = false;
		return true;
	}

	public synchronized boolean lingering() {
		if (active) return false;
		if (endedAtMs == 0L) return false;
		return System.currentTimeMillis() - endedAtMs < LINGER_MS;
	}

	public synchronized long endedAtMs() { return endedAtMs; }

	public synchronized void onIncoming(String player, long amount) {
		if (!active) return;
		if (player == null || player.isEmpty()) return;
		totals.merge(player, amount, Long::sum);
	}

	public synchronized boolean active()      { return active; }
	public synchronized String item()         { return item; }
	public synchronized int itemCount()       { return itemCount; }
	public synchronized long startedAtMs()    { return startedAtMs; }
	public synchronized long endsAtMs()       { return endsAtMs; }
	public synchronized Bid winner()          { return winner; }
	public synchronized long remainingMs() {
		if (!active) return 0L;
		return Math.max(0L, endsAtMs - System.currentTimeMillis());
	}

	public synchronized List<Bid> topBids(int limit) {
		List<Bid> out = new ArrayList<>(totals.size());
		for (Map.Entry<String, Long> e : totals.entrySet()) {
			out.add(new Bid(e.getKey(), e.getValue()));
		}
		out.sort(Comparator.comparingLong(Bid::amount).reversed());
		if (out.size() > limit) return out.subList(0, limit);
		return out;
	}

	public synchronized Bid topBid() {
		Bid best = null;
		for (Map.Entry<String, Long> e : totals.entrySet()) {
			if (best == null || e.getValue() > best.amount()) {
				best = new Bid(e.getKey(), e.getValue());
			}
		}
		return best;
	}

	public synchronized int bidderCount() {
		return totals.size();
	}
}
