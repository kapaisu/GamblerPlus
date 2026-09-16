package dev.mishka.gamblerplus.client;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;

public final class ChatListener {
	private ChatListener() {}

	public static void register(Config config, Stats stats, SessionManager sessions, Auction auction, AllTimeLog allTime, Runnable onChange) {
		ClientReceiveMessageEvents.MODIFY_GAME.register((message, overlay) -> {
			if (overlay) return message;
			String line = message.getString();
			PaymentEvent ev = PaymentParser.tryParse(line, System.currentTimeMillis());
			if (ev == null || !ev.incoming()) return message;
			long payback = ev.amount() * 2L;
			String cmd = "/gplus2x " + ev.player() + " " + payback;
			Component button = Component.literal(" [2x]").setStyle(
					Style.EMPTY
							.withColor(ChatFormatting.GOLD)
							.withBold(true)
							.withClickEvent(new ClickEvent.RunCommand(cmd))
							.withHoverEvent(new HoverEvent.ShowText(Component.literal("click to pay " + AmountFormat.pretty(payback) + " back")))
			);
			return message.copy().append(button);
		});

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
			allTime.record(ev);
			if (ev.incoming()) sessions.onIncoming(ev.player(), ev.amount());
			onChange.run();
		});
	}
}
