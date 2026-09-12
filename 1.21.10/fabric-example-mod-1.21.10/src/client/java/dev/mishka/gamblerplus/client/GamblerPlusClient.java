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

	private static boolean promptShownThisSession = false;
	private static int inGameTicks = 0;

	@Override
	public void onInitializeClient() {
		STATS    = new Stats();
		CONFIG   = new Config(STATS);
		SESSIONS = new SessionManager();
		AUCTION  = new Auction();
		TIMER    = new TimerState();
		CONFIG.load();
		SESSIONS.load();

		ChatListener.register(CONFIG, STATS, SESSIONS, AUCTION, CONFIG::save);
		HudOverlay.register(CONFIG, STATS);
		Keybinds.register(CONFIG, GamblerPlusClient::openUi);
		PaymentIntercept.register(CONFIG);

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
			mc.setScreenAndShow(new SetupScreen(CONFIG));
		});

		ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> {
			CONFIG.save();
			SESSIONS.save();
		});
	}

	public static void openUi() {
		Minecraft mc = Minecraft.getInstance();
		mc.setScreenAndShow(new TrackerScreen(CONFIG, STATS));
	}
}
