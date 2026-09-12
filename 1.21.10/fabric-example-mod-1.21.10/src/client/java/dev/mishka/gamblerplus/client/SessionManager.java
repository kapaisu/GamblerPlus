package dev.mishka.gamblerplus.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SessionManager {
	public static final int MAX_HISTORY = 50;
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private final Path file;
	private final List<Session> past = new ArrayList<>();
	private int counter = 0;
	private boolean active = false;
	private long currentStartedAt = 0L;
	private String currentLabel = "";
	private final LinkedHashMap<String, Long> incomingByPlayer = new LinkedHashMap<>();

	public SessionManager() {
		this.file = FabricLoader.getInstance().getConfigDir().resolve("gambler-plus-sessions.json");
	}

	public synchronized boolean hasActive()    { return active; }
	public synchronized long currentStartedAt(){ return currentStartedAt; }
	public synchronized String currentLabel()  { return currentLabel; }
	public synchronized int    counter()       { return counter; }
	public synchronized List<Session> past()   { return Collections.unmodifiableList(new ArrayList<>(past)); }

	public synchronized Map<String, Long> incomingByPlayer() {
		return new LinkedHashMap<>(incomingByPlayer);
	}

	public synchronized void onIncoming(String player, long amount) {
		if (!active) return;
		incomingByPlayer.merge(player, amount, Long::sum);
		save();
	}

	public synchronized void onIncomingReversed(String player, long amount) {
		if (!active) return;
		Long current = incomingByPlayer.get(player);
		if (current == null) return;
		long left = current - amount;
		if (left <= 0) incomingByPlayer.remove(player);
		else incomingByPlayer.put(player, left);
		save();
	}

	public synchronized void markPaid(String player) {
		incomingByPlayer.remove(player);
		save();
	}

	public synchronized void clearAllRakeback() {
		incomingByPlayer.clear();
		save();
	}

	public synchronized boolean deletePast(long id) {
		boolean removed = past.removeIf(s -> s.id() == id);
		if (removed) save();
		return removed;
	}

	public synchronized void clearAllPast() {
		past.clear();
		save();
	}

	public synchronized void startNew(Stats stats) {
		if (active) archiveCurrent(stats);
		stats.resetSession();
		incomingByPlayer.clear();
		counter++;
		currentStartedAt = System.currentTimeMillis();
		currentLabel = "Session #" + counter;
		active = true;
		save();
	}

	public synchronized void endCurrent(Stats stats) {
		if (!active) return;
		archiveCurrent(stats);
		stats.resetSession();
		incomingByPlayer.clear();
		active = false;
		currentStartedAt = 0L;
		currentLabel = "";
		save();
	}

	private void archiveCurrent(Stats stats) {
		Session s = new Session(
				currentStartedAt,
				currentStartedAt,
				System.currentTimeMillis(),
				currentLabel,
				stats.sessionIn(),
				stats.sessionOut(),
				stats.sessionInCount(),
				stats.sessionOutCount(),
				stats.longestLossStreak(),
				stats.longestWinStreak(),
				new ArrayList<>(stats.snapshot())
		);
		past.add(0, s);
		while (past.size() > MAX_HISTORY) past.remove(past.size() - 1);
	}

	public synchronized Session snapshotCurrent(Stats stats) {
		if (!active) return null;
		return new Session(
				currentStartedAt,
				currentStartedAt,
				System.currentTimeMillis(),
				currentLabel,
				stats.sessionIn(),
				stats.sessionOut(),
				stats.sessionInCount(),
				stats.sessionOutCount(),
				stats.longestLossStreak(),
				stats.longestWinStreak(),
				stats.snapshot()
		);
	}

	public synchronized void load() {
		if (!Files.exists(file)) return;
		try (Reader r = Files.newBufferedReader(file)) {
			JsonObject j = JsonParser.parseReader(r).getAsJsonObject();
			if (j.has("counter")) counter = j.get("counter").getAsInt();
			if (j.has("current")) {
				JsonObject c = j.getAsJsonObject("current");
				active = c.has("active") && c.get("active").getAsBoolean();
				currentStartedAt = c.has("startedAt") ? c.get("startedAt").getAsLong() : 0L;
				currentLabel = c.has("label") ? c.get("label").getAsString() : "";
				if (c.has("incomingByPlayer")) {
					JsonObject m = c.getAsJsonObject("incomingByPlayer");
					for (var entry : m.entrySet()) {
						incomingByPlayer.put(entry.getKey(), entry.getValue().getAsLong());
					}
				}
			}
			if (j.has("sessions")) {
				JsonArray arr = j.getAsJsonArray("sessions");
				for (int i = 0; i < arr.size(); i++) {
					JsonObject o = arr.get(i).getAsJsonObject();
					List<PaymentEvent> payments = new ArrayList<>();
					if (o.has("payments")) {
						JsonArray pa = o.getAsJsonArray("payments");
						for (int k = 0; k < pa.size(); k++) {
							JsonObject po = pa.get(k).getAsJsonObject();
							payments.add(new PaymentEvent(
									po.get("t").getAsLong(),
									po.get("u").getAsString(),
									po.get("a").getAsLong(),
									po.get("i").getAsBoolean()));
						}
					}
					past.add(new Session(
							o.get("id").getAsLong(),
							o.get("startedAt").getAsLong(),
							o.get("endedAt").getAsLong(),
							o.get("label").getAsString(),
							o.get("in").getAsLong(),
							o.get("out").getAsLong(),
							o.get("inCount").getAsInt(),
							o.get("outCount").getAsInt(),
							o.get("longestLossStreak").getAsInt(),
							o.get("longestWinStreak").getAsInt(),
							payments));
				}
			}
		} catch (Exception ignored) {
		}
	}

	public synchronized void save() {
		JsonObject j = new JsonObject();
		j.addProperty("counter", counter);

		JsonObject c = new JsonObject();
		c.addProperty("active", active);
		c.addProperty("startedAt", currentStartedAt);
		c.addProperty("label", currentLabel);
		JsonObject m = new JsonObject();
		for (var e : incomingByPlayer.entrySet()) {
			m.addProperty(e.getKey(), e.getValue());
		}
		c.add("incomingByPlayer", m);
		j.add("current", c);

		JsonArray arr = new JsonArray();
		for (Session s : past) {
			JsonObject o = new JsonObject();
			o.addProperty("id", s.id());
			o.addProperty("startedAt", s.startedAt());
			o.addProperty("endedAt", s.endedAt());
			o.addProperty("label", s.label());
			o.addProperty("in", s.in());
			o.addProperty("out", s.out());
			o.addProperty("inCount", s.inCount());
			o.addProperty("outCount", s.outCount());
			o.addProperty("longestLossStreak", s.longestLossStreak());
			o.addProperty("longestWinStreak", s.longestWinStreak());
			JsonArray pa = new JsonArray();
			for (PaymentEvent p : s.payments()) {
				JsonObject po = new JsonObject();
				po.addProperty("t", p.timestampMs());
				po.addProperty("u", p.player());
				po.addProperty("a", p.amount());
				po.addProperty("i", p.incoming());
				pa.add(po);
			}
			o.add("payments", pa);
			arr.add(o);
		}
		j.add("sessions", arr);
		try {
			Files.createDirectories(file.getParent());
			try (Writer w = Files.newBufferedWriter(file)) {
				GSON.toJson(j, w);
			}
		} catch (IOException ignored) {
		}
	}
}
