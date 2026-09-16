package dev.mishka.gamblerplus.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class Stats {
	public static final int LOG_CAP = 100;
	public static final long TOAST_DURATION_MS = 6_000L;

	private long sessionIn;
	private long sessionOut;
	private int sessionInCount;
	private int sessionOutCount;
	private int lossStreak;
	private int longestLossStreak;
	private int longestWinStreak;
	private int winStreak;

	private long allTimeIn;
	private long allTimeOut;
	private int allTimeInCount;
	private int allTimeOutCount;

	private final Deque<PaymentEvent> log = new ArrayDeque<>();

	private volatile long toastUntilMs;
	private volatile long toastShownAtMs;
	private volatile int toastStreak;

	public synchronized void loadPersisted(long inTotal, long outTotal, int inCount, int outCount) {
		this.allTimeIn = inTotal;
		this.allTimeOut = outTotal;
		this.allTimeInCount = inCount;
		this.allTimeOutCount = outCount;
	}

	public synchronized void record(PaymentEvent e, int streakThreshold) {
		log.addFirst(e);
		while (log.size() > LOG_CAP) log.removeLast();
		if (e.incoming()) {
			sessionIn += e.amount();
			sessionInCount++;
			allTimeIn += e.amount();
			allTimeInCount++;
			lossStreak = 0;
			winStreak++;
			if (winStreak > longestWinStreak) longestWinStreak = winStreak;
		} else {
			sessionOut += e.amount();
			sessionOutCount++;
			allTimeOut += e.amount();
			allTimeOutCount++;
			winStreak = 0;
			lossStreak++;
			if (lossStreak > longestLossStreak) longestLossStreak = lossStreak;
			if (lossStreak >= streakThreshold) {
				toastStreak = lossStreak;
				toastShownAtMs = System.currentTimeMillis();
				toastUntilMs = toastShownAtMs + TOAST_DURATION_MS;
			}
		}
	}

	public synchronized boolean remove(PaymentEvent e) {
		if (e == null) return false;
		if (!log.remove(e)) return false;
		if (e.incoming()) {
			sessionIn = Math.max(0, sessionIn - e.amount());
			sessionInCount = Math.max(0, sessionInCount - 1);
			allTimeIn = Math.max(0, allTimeIn - e.amount());
			allTimeInCount = Math.max(0, allTimeInCount - 1);
		} else {
			sessionOut = Math.max(0, sessionOut - e.amount());
			sessionOutCount = Math.max(0, sessionOutCount - 1);
			allTimeOut = Math.max(0, allTimeOut - e.amount());
			allTimeOutCount = Math.max(0, allTimeOutCount - 1);
		}
		return true;
	}

	public synchronized void reverseAllTime(PaymentEvent e) {
		if (e == null) return;
		if (e.incoming()) {
			allTimeIn = Math.max(0, allTimeIn - e.amount());
			allTimeInCount = Math.max(0, allTimeInCount - 1);
		} else {
			allTimeOut = Math.max(0, allTimeOut - e.amount());
			allTimeOutCount = Math.max(0, allTimeOutCount - 1);
		}
	}

	public synchronized void resetSession() {
		sessionIn = sessionOut = 0;
		sessionInCount = sessionOutCount = 0;
		lossStreak = winStreak = 0;
		longestLossStreak = longestWinStreak = 0;
		log.clear();
		toastUntilMs = 0;
	}

	public synchronized void resetAllTime() {
		allTimeIn = allTimeOut = 0;
		allTimeInCount = allTimeOutCount = 0;
	}

	public synchronized long sessionIn()  { return sessionIn; }
	public synchronized long sessionOut() { return sessionOut; }
	public synchronized long sessionNet() { return sessionIn - sessionOut; }
	public synchronized int  sessionInCount()  { return sessionInCount; }
	public synchronized int  sessionOutCount() { return sessionOutCount; }
	public synchronized int  lossStreak()      { return lossStreak; }
	public synchronized int  winStreak()       { return winStreak; }
	public synchronized int  longestLossStreak() { return longestLossStreak; }
	public synchronized int  longestWinStreak()  { return longestWinStreak; }

	public synchronized long allTimeIn()  { return allTimeIn; }
	public synchronized long allTimeOut() { return allTimeOut; }
	public synchronized long allTimeNet() { return allTimeIn - allTimeOut; }
	public synchronized int  allTimeInCount()  { return allTimeInCount; }
	public synchronized int  allTimeOutCount() { return allTimeOutCount; }

	public synchronized double sessionProfitPct() {
		if (sessionOut == 0) return 0.0;
		return (double) sessionNet() / (double) sessionOut * 100.0;
	}

	public synchronized List<PaymentEvent> snapshot() {
		return new ArrayList<>(log);
	}

	public long toastUntilMs()   { return toastUntilMs; }
	public long toastShownAtMs() { return toastShownAtMs; }
	public int  toastStreak()    { return toastStreak; }
	public void clearToast()     { toastUntilMs = 0; }
}
