package dev.mishka.gamblerplus.client;

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
import java.util.List;

public final class AllTimeLog {
	public static final int CAP = 10000;

	private final Path file;
	private final List<PaymentEvent> events = new ArrayList<>();

	public AllTimeLog() {
		this.file = FabricLoader.getInstance().getConfigDir().resolve("gambler-plus-alltime.json");
	}

	public synchronized void record(PaymentEvent e) {
		events.add(e);
		while (events.size() > CAP) events.remove(0);
		save();
	}

	public synchronized boolean removeOne(PaymentEvent e) {
		for (int i = 0; i < events.size(); i++) {
			PaymentEvent x = events.get(i);
			if (x.timestampMs() == e.timestampMs()
					&& x.amount() == e.amount()
					&& x.incoming() == e.incoming()
					&& x.player().equals(e.player())) {
				events.remove(i);
				save();
				return true;
			}
		}
		return false;
	}

	public synchronized List<PaymentEvent> snapshot() {
		return new ArrayList<>(events);
	}

	public synchronized int size() {
		return events.size();
	}

	public synchronized void load() {
		if (!Files.exists(file)) return;
		try (Reader r = Files.newBufferedReader(file)) {
			JsonObject j = JsonParser.parseReader(r).getAsJsonObject();
			if (j.has("events")) {
				JsonArray arr = j.getAsJsonArray("events");
				events.clear();
				for (int i = 0; i < arr.size(); i++) {
					JsonObject o = arr.get(i).getAsJsonObject();
					events.add(new PaymentEvent(
							o.get("t").getAsLong(),
							o.get("u").getAsString(),
							o.get("a").getAsLong(),
							o.get("i").getAsBoolean()));
				}
			}
		} catch (Exception ignored) {
		}
	}

	public synchronized void save() {
		try {
			Files.createDirectories(file.getParent());
			JsonObject j = new JsonObject();
			JsonArray arr = new JsonArray();
			for (PaymentEvent e : events) {
				JsonObject o = new JsonObject();
				o.addProperty("t", e.timestampMs());
				o.addProperty("u", e.player());
				o.addProperty("a", e.amount());
				o.addProperty("i", e.incoming());
				arr.add(o);
			}
			j.add("events", arr);
			try (Writer w = Files.newBufferedWriter(file)) {
				new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(j, w);
			}
		} catch (IOException ignored) {
		}
	}
}
