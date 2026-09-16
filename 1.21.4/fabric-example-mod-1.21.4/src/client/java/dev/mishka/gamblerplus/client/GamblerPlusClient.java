package dev.mishka.gamblerplus.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

public final class GamblerPlusClient implements ClientModInitializer {
	public static Stats  STATS;
	public static Config CONFIG;
	public static SessionManager SESSIONS;
	public static Auction AUCTION;
	public static TimerState TIMER;
	public static AllTimeLog ALLTIME;
	public static ImageStore IMAGES;

	private static boolean promptShownThisSession = false;
	private static int inGameTicks = 0;

	@Override
	public void onInitializeClient() {
		STATS    = new Stats();
		CONFIG   = new Config(STATS);
		SESSIONS = new SessionManager();
		AUCTION  = new Auction();
		TIMER    = new TimerState();
		ALLTIME  = new AllTimeLog();
		IMAGES   = new ImageStore();
		CONFIG.load();
		SESSIONS.load();
		ALLTIME.load();
		for (HudLayout.ImageEntry ie : CONFIG.hudLayout().imageEntries) IMAGES.get(ie.file);

		ChatListener.register(CONFIG, STATS, SESSIONS, AUCTION, ALLTIME, CONFIG::save);
		HudOverlay.register(CONFIG, STATS);
		Keybinds.register(CONFIG, GamblerPlusClient::openUi, GamblerPlusClient::openAuction);
		PaymentIntercept.register(CONFIG);
		GamblerPlusCommands.register();

		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			AUCTION.tick();
			TIMER.tick();
			if (AUCTION.consumeCelebration()) {
				Auction.Bid w = AUCTION.winner();
				if (w != null) WinnerBanner.spawn(w.player());
			}
			if (promptShownThisSession) return;
			if (CONFIG.setupComplete()) return;
			if (mc.player == null || mc.level == null) return;
			if (!mc.mouseHandler.isMouseGrabbed()) { inGameTicks = 0; return; }
			if (++inGameTicks < 20) return;
			promptShownThisSession = true;
			mc.setScreen(new SetupScreen(CONFIG));
		});

		ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> {
			CONFIG.save();
			SESSIONS.save();
			ALLTIME.save();
		});
	}

	public static void openUi() {
		Minecraft mc = Minecraft.getInstance();
		mc.setScreen(new TrackerScreen(CONFIG, STATS));
	}

	public static void openAuction() {
		Minecraft mc = Minecraft.getInstance();
		mc.setScreen(new AuctionScreen());
	}
}
