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

public final class Config {
	public enum HudDisplay { AMOUNT, PERCENT }
	public enum HudAnchor  { TOP_LEFT, TOP_CENTER, TOP_RIGHT }

	public static final long THRESHOLD_MIN = 1_000L;
	public static final long THRESHOLD_MAX = 10_000_000_000L;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private final Path file;
	private final Stats stats;

	private boolean gamblingMode = false;
	private boolean hudBar       = false;
	private boolean showToast    = false;
	private HudDisplay hudDisplay = HudDisplay.AMOUNT;
	private HudAnchor  hudAnchor  = HudAnchor.TOP_CENTER;
	private int  streakThreshold = 5;
	private boolean setupComplete = false;
	private boolean verifyLargePayments = false;
	private long largePaymentThreshold = 1_000_000L;
	private boolean rakebackEnabled = false;
	private double rakebackPct = 5.0;
	private boolean arrowGameSupport = false;
	private boolean numericTimer = false;
	private boolean showGraph = false;
	private boolean chatMultiplierButton = true;

	private final HudLayout hudLayout = new HudLayout();

	private static final double[] RAKEBACK_STEPS = {
		0.5, 1.0, 2.0, 3.0, 5.0, 7.5, 10.0, 12.5, 15.0, 20.0, 25.0, 33.0, 50.0
	};

	public Config(Stats stats) {
		this.stats = stats;
		this.file = FabricLoader.getInstance().getConfigDir().resolve("gambler-plus.json");
	}

	public boolean gamblingMode()   { return gamblingMode; }
	public boolean hudBar()         { return hudBar; }
	public boolean showToast()      { return showToast; }
	public HudDisplay hudDisplay()  { return hudDisplay; }
	public HudAnchor  hudAnchor()   { return hudAnchor; }
	public int  streakThreshold()   { return streakThreshold; }
	public boolean setupComplete()  { return setupComplete; }
	public boolean verifyLargePayments() { return verifyLargePayments; }
	public long largePaymentThreshold()  { return largePaymentThreshold; }
	public boolean rakebackEnabled()     { return rakebackEnabled; }
	public double  rakebackPct()         { return rakebackPct; }
	public boolean arrowGameSupport()    { return arrowGameSupport; }
	public boolean numericTimer()        { return numericTimer; }
	public boolean showGraph()           { return showGraph; }
	public void setShowGraph(boolean v)  { showGraph = v; save(); }
	public boolean chatMultiplierButton() { return chatMultiplierButton; }
	public HudLayout hudLayout()         { return hudLayout; }

	public void toggleGamblingMode() { gamblingMode = !gamblingMode; save(); }
	public void toggleHudBar()       { hudBar = !hudBar; save(); }
	public void toggleShowToast()    { showToast = !showToast; save(); }
	public void toggleVerify()       { verifyLargePayments = !verifyLargePayments; save(); }
	public void toggleRakeback()     { rakebackEnabled = !rakebackEnabled; save(); }
	public void toggleArrowGameSupport() { arrowGameSupport = !arrowGameSupport; save(); }
	public void toggleNumericTimer()     { numericTimer = !numericTimer; save(); }
	public void toggleChatMultiplierButton() { chatMultiplierButton = !chatMultiplierButton; save(); }

	public void stepRakebackPct(int direction) {
		int idx = 0;
		for (int i = 0; i < RAKEBACK_STEPS.length; i++) {
			if (rakebackPct >= RAKEBACK_STEPS[i] - 0.01) idx = i;
		}
		idx = Math.max(0, Math.min(RAKEBACK_STEPS.length - 1, idx + direction));
		rakebackPct = RAKEBACK_STEPS[idx];
		save();
	}
	public void setSetupComplete(boolean v) { setupComplete = v; save(); }

	public void setThresholdSliderPos(float pos) {
		float t = Math.max(0f, Math.min(1f, pos));
		double lo = Math.log(THRESHOLD_MIN);
		double hi = Math.log(THRESHOLD_MAX);
		long raw = (long) Math.round(Math.exp(lo + (hi - lo) * t));
		largePaymentThreshold = snapNice(Math.max(THRESHOLD_MIN, Math.min(THRESHOLD_MAX, raw)));
		save();
	}

	public void setThreshold(long v) {
		largePaymentThreshold = Math.max(THRESHOLD_MIN, Math.min(THRESHOLD_MAX, v));
		save();
	}

	public float thresholdSliderPos() {
		double lo = Math.log(THRESHOLD_MIN);
		double hi = Math.log(THRESHOLD_MAX);
		double v = Math.log(Math.max(THRESHOLD_MIN, largePaymentThreshold));
		return (float) ((v - lo) / (hi - lo));
	}

	private static long snapNice(long v) {
		long[] units = {1L, 10L, 100L, 1_000L, 10_000L, 100_000L, 1_000_000L, 10_000_000L, 100_000_000L, 1_000_000_000L};
		long unit = 1L;
		for (long u : units) if (v >= u * 10L) unit = u;
		long snapped = Math.round((double) v / unit) * unit;
		if (snapped < THRESHOLD_MIN) return THRESHOLD_MIN;
		if (snapped > THRESHOLD_MAX) return THRESHOLD_MAX;
		return snapped;
	}

	public void cycleHudDisplay() {
		hudDisplay = hudDisplay == HudDisplay.AMOUNT ? HudDisplay.PERCENT : HudDisplay.AMOUNT;
		save();
	}
	public void cycleHudAnchor() {
		hudAnchor = switch (hudAnchor) {
			case TOP_LEFT   -> HudAnchor.TOP_CENTER;
			case TOP_CENTER -> HudAnchor.TOP_RIGHT;
			case TOP_RIGHT  -> HudAnchor.TOP_LEFT;
		};
		save();
	}
	public void setStreakThreshold(int v) {
		streakThreshold = Math.max(2, Math.min(20, v));
		save();
	}

	public void load() {
		if (!Files.exists(file)) { save(); return; }
		try (Reader r = Files.newBufferedReader(file)) {
			JsonObject j = JsonParser.parseReader(r).getAsJsonObject();
			if (j.has("gamblingMode"))    gamblingMode    = j.get("gamblingMode").getAsBoolean();
			if (j.has("hudBar"))          hudBar          = j.get("hudBar").getAsBoolean();
			if (j.has("showToast"))       showToast       = j.get("showToast").getAsBoolean();
			if (j.has("streakThreshold")) streakThreshold = j.get("streakThreshold").getAsInt();
			if (j.has("hudDisplay"))      hudDisplay      = HudDisplay.valueOf(j.get("hudDisplay").getAsString());
			if (j.has("hudAnchor"))       hudAnchor       = HudAnchor.valueOf(j.get("hudAnchor").getAsString());
			if (j.has("setupComplete"))         setupComplete         = j.get("setupComplete").getAsBoolean();
			if (j.has("verifyLargePayments"))   verifyLargePayments   = j.get("verifyLargePayments").getAsBoolean();
			if (j.has("largePaymentThreshold")) largePaymentThreshold = j.get("largePaymentThreshold").getAsLong();
			if (j.has("rakebackEnabled"))       rakebackEnabled       = j.get("rakebackEnabled").getAsBoolean();
			if (j.has("rakebackPct"))           rakebackPct           = j.get("rakebackPct").getAsDouble();
			if (j.has("arrowGameSupport"))      arrowGameSupport      = j.get("arrowGameSupport").getAsBoolean();
			if (j.has("numericTimer"))          numericTimer          = j.get("numericTimer").getAsBoolean();
			if (j.has("showGraph"))             showGraph             = j.get("showGraph").getAsBoolean();
			if (j.has("chatMultiplierButton")) chatMultiplierButton = j.get("chatMultiplierButton").getAsBoolean();
			if (largePaymentThreshold < THRESHOLD_MIN) largePaymentThreshold = THRESHOLD_MIN;
			if (largePaymentThreshold > THRESHOLD_MAX) largePaymentThreshold = THRESHOLD_MAX;

			if (j.has("hudLayout")) {
				JsonObject h = j.getAsJsonObject("hudLayout");
				if (h.has("auctionX")) hudLayout.auctionX = h.get("auctionX").getAsInt();
				if (h.has("auctionY")) hudLayout.auctionY = h.get("auctionY").getAsInt();
				if (h.has("auctionScale")) hudLayout.auctionScale = HudLayout.clampScale(h.get("auctionScale").getAsFloat());
				if (h.has("timerX")) hudLayout.timerX = h.get("timerX").getAsInt();
				if (h.has("timerY")) hudLayout.timerY = h.get("timerY").getAsInt();
				if (h.has("timerScale")) hudLayout.timerScale = HudLayout.clampScale(h.get("timerScale").getAsFloat());
				hudLayout.imageEntries.clear();
				if (h.has("imageEntries")) {
					JsonArray arr = h.getAsJsonArray("imageEntries");
					for (int i = 0; i < arr.size(); i++) {
						JsonObject o = arr.get(i).getAsJsonObject();
						HudLayout.ImageEntry ie = new HudLayout.ImageEntry();
						if (o.has("file")) ie.file = o.get("file").getAsString();
						if (o.has("x")) ie.x = o.get("x").getAsInt();
						if (o.has("y")) ie.y = o.get("y").getAsInt();
						if (o.has("scale")) ie.scale = HudLayout.clampScale(o.get("scale").getAsFloat());
						hudLayout.imageEntries.add(ie);
					}
				}
				hudLayout.textEntries.clear();
				if (h.has("textEntries")) {
					JsonArray arr = h.getAsJsonArray("textEntries");
					for (int i = 0; i < arr.size(); i++) {
						JsonObject e = arr.get(i).getAsJsonObject();
						HudLayout.TextEntry t = new HudLayout.TextEntry();
						if (e.has("content")) t.content = e.get("content").getAsString();
						if (e.has("x")) t.x = e.get("x").getAsInt();
						if (e.has("y")) t.y = e.get("y").getAsInt();
						if (e.has("scale")) t.scale = HudLayout.clampScale(e.get("scale").getAsFloat());
						if (e.has("r")) t.r = HudLayout.clampByte(e.get("r").getAsInt());
						if (e.has("g")) t.g = HudLayout.clampByte(e.get("g").getAsInt());
						if (e.has("b")) t.b = HudLayout.clampByte(e.get("b").getAsInt());
						if (e.has("font")) t.font = Math.max(0, Math.min(TextHud.FONT_NAMES.length - 1, e.get("font").getAsInt()));
						hudLayout.textEntries.add(t);
					}
				} else if (h.has("textContent")) {
					HudLayout.TextEntry t = new HudLayout.TextEntry();
					t.content = h.get("textContent").getAsString();
					if (h.has("textX")) t.x = h.get("textX").getAsInt();
					if (h.has("textY")) t.y = h.get("textY").getAsInt();
					if (h.has("textScale")) t.scale = HudLayout.clampScale(h.get("textScale").getAsFloat());
					if (h.has("textR")) t.r = HudLayout.clampByte(h.get("textR").getAsInt());
					if (h.has("textG")) t.g = HudLayout.clampByte(h.get("textG").getAsInt());
					if (h.has("textB")) t.b = HudLayout.clampByte(h.get("textB").getAsInt());
					if (h.has("textFont")) t.font = Math.max(0, Math.min(4, h.get("textFont").getAsInt()));
					if (!t.content.isEmpty()) hudLayout.textEntries.add(t);
				}
			}

			long inTotal  = j.has("allTimeIn")       ? j.get("allTimeIn").getAsLong()       : 0L;
			long outTotal = j.has("allTimeOut")      ? j.get("allTimeOut").getAsLong()      : 0L;
			int  inCount  = j.has("allTimeInCount")  ? j.get("allTimeInCount").getAsInt()   : 0;
			int  outCount = j.has("allTimeOutCount") ? j.get("allTimeOutCount").getAsInt()  : 0;
			stats.loadPersisted(inTotal, outTotal, inCount, outCount);
		} catch (Exception ignored) {
		}
	}

	public void save() {
		JsonObject j = new JsonObject();
		j.addProperty("gamblingMode", gamblingMode);
		j.addProperty("hudBar", hudBar);
		j.addProperty("showToast", showToast);
		j.addProperty("streakThreshold", streakThreshold);
		j.addProperty("hudDisplay", hudDisplay.name());
		j.addProperty("hudAnchor", hudAnchor.name());
		j.addProperty("setupComplete", setupComplete);
		j.addProperty("verifyLargePayments", verifyLargePayments);
		j.addProperty("largePaymentThreshold", largePaymentThreshold);
		j.addProperty("rakebackEnabled", rakebackEnabled);
		j.addProperty("rakebackPct", rakebackPct);
		j.addProperty("arrowGameSupport", arrowGameSupport);
		j.addProperty("numericTimer", numericTimer);
		j.addProperty("showGraph", showGraph);
		j.addProperty("chatMultiplierButton", chatMultiplierButton);

		JsonObject h = new JsonObject();
		h.addProperty("auctionX", hudLayout.auctionX);
		h.addProperty("auctionY", hudLayout.auctionY);
		h.addProperty("auctionScale", hudLayout.auctionScale);
		h.addProperty("timerX", hudLayout.timerX);
		h.addProperty("timerY", hudLayout.timerY);
		h.addProperty("timerScale", hudLayout.timerScale);
		JsonArray entries = new JsonArray();
		for (HudLayout.TextEntry t : hudLayout.textEntries) {
			JsonObject e = new JsonObject();
			e.addProperty("content", t.content);
			e.addProperty("x", t.x);
			e.addProperty("y", t.y);
			e.addProperty("scale", t.scale);
			e.addProperty("r", t.r);
			e.addProperty("g", t.g);
			e.addProperty("b", t.b);
			e.addProperty("font", t.font);
			entries.add(e);
		}
		h.add("textEntries", entries);
		JsonArray images = new JsonArray();
		for (HudLayout.ImageEntry ie : hudLayout.imageEntries) {
			JsonObject o = new JsonObject();
			o.addProperty("file", ie.file);
			o.addProperty("x", ie.x);
			o.addProperty("y", ie.y);
			o.addProperty("scale", ie.scale);
			images.add(o);
		}
		h.add("imageEntries", images);
		j.add("hudLayout", h);

		j.addProperty("allTimeIn", stats.allTimeIn());
		j.addProperty("allTimeOut", stats.allTimeOut());
		j.addProperty("allTimeInCount", stats.allTimeInCount());
		j.addProperty("allTimeOutCount", stats.allTimeOutCount());
		try {
			Files.createDirectories(file.getParent());
			try (Writer w = Files.newBufferedWriter(file)) {
				GSON.toJson(j, w);
			}
		} catch (IOException ignored) {
		}
	}
}
