package dev.mishka.gamblerplus.client;

public final class TimerState {
	private volatile boolean active = false;
	private volatile long endsAtMs = 0L;
	private volatile long lastDurationMs = 60_000L;

	public synchronized boolean start(long durationMs) {
		if (durationMs <= 0) return false;
		this.lastDurationMs = durationMs;
		this.endsAtMs = System.currentTimeMillis() + durationMs;
		this.active = true;
		return true;
	}

	public synchronized void stop() {
		this.active = false;
	}

	public synchronized void tick() {
		if (active && System.currentTimeMillis() >= endsAtMs) {
			active = false;
		}
	}

	public synchronized boolean active() { return active; }
	public synchronized long endsAtMs()  { return endsAtMs; }
	public synchronized long lastDurationMs() { return lastDurationMs; }
	public synchronized long remainingMs() {
		if (!active) return 0L;
		return Math.max(0L, endsAtMs - System.currentTimeMillis());
	}
}
