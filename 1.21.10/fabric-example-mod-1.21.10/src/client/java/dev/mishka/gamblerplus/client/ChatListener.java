package dev.mishka.gamblerplus.client;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;

public final class ChatListener {
	private ChatListener() {}

	public static void register(Config config, Stats stats, SessionManager sessions, Auction auction, Runnable onChange) {
		ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
			if (overlay) return;
			String line = message.getString();
			if (config.arrowGameSupport()) {
				PurchaseParser.Purchase p = PurchaseParser.tryParse(line);
				if (p != null && p.amount() >= PurchaseParser.MIN_AMOUNT) {
					PurchasePopup.trigger(p.count(), p.item(), p.amount());
				}
			}
			PaymentEvent ev = PaymentParser.tryParse(line, System.currentTimeMillis());
			if (ev == null) return;
			if (ev.incoming() && auction.active()) {
				auction.onIncoming(ev.player(), ev.amount());
			}
			if (!config.gamblingMode()) return;
			stats.record(ev, config.streakThreshold());
			if (ev.incoming()) sessions.onIncoming(ev.player(), ev.amount());
			onChange.run();
		});
	}
}
