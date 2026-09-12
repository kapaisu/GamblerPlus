package dev.mishka.gamblerplus.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

public final class AuctionHud {
	private static final int BASE_W = 160;
	private static final int BASE_H = 100;
	private static final int LINE_H = 10;

	private AuctionHud() {}

	public static int[] bounds(HudLayout layout) {
		int w = Math.round(BASE_W * layout.auctionScale);
		int h = Math.round(BASE_H * layout.auctionScale);
		return new int[]{layout.auctionX, layout.auctionY, layout.auctionX + w, layout.auctionY + h};
	}

	public static void draw(GuiGraphics ctx, Font font, HudLayout layout, boolean editOutline) {
		Auction auction = GamblerPlusClient.AUCTION;
		boolean live = auction.active();
		boolean linger = auction.lingering();
		if (!live && !linger && !editOutline) return;

		int[] r = bounds(layout);
		int x = r[0], y = r[1], x2 = r[2], y2 = r[3];

		int edge = editOutline ? Theme.HUD_EDGE : (linger ? Theme.GAIN : Theme.PANEL_LINE);
		Theme.roundPanel(ctx, x, y, x2, y2, 6, Theme.HUD_BG, edge);

		var pose = ctx.pose();
		pose.pushPose();
		pose.translate(x + 8f, y + 6f, 0f);
		pose.scale(layout.auctionScale, layout.auctionScale, 1f);

		ctx.drawString(font, "Gambler", 0, 0, Theme.TEXT, false);
		int brandW = font.width("Gambler ");
		ctx.drawString(font, "Plus", brandW, 0, Theme.BRAND, false);
		int psuffix = font.width("Gambler Plus ");
		ctx.drawString(font, "auction", psuffix, 0, Theme.TEXT_DIM, false);

		ctx.fill(0, LINE_H + 2, BASE_W - 16, LINE_H + 3, Theme.PANEL_LINE);

		int y0 = LINE_H + 6;

		if (live) {
			String item = auction.item();
			if (auction.itemCount() > 1) item = item + " x" + auction.itemCount();
			ctx.drawString(font, trim(font, item, BASE_W - 20), 0, y0, Theme.TEXT, false);
			ctx.drawString(font, "ends " + TimeParse.prettyDuration(auction.remainingMs()),
					0, y0 + LINE_H, Theme.TEXT_MUTED, false);

			List<Auction.Bid> bids = auction.topBids(3);
			int rowY = y0 + LINE_H * 2 + 4;
			if (bids.isEmpty()) {
				ctx.drawString(font, "waiting for bids", 0, rowY, Theme.TEXT_DIM, false);
			} else {
				for (int i = 0; i < bids.size(); i++) {
					Auction.Bid b = bids.get(i);
					int color = i == 0 ? Theme.GAIN : Theme.TEXT;
					ctx.drawString(font, (i + 1) + "." + b.player(), 0, rowY, color, false);
					String amt = AmountFormat.pretty(b.amount());
					int aw = font.width(amt);
					ctx.drawString(font, amt, BASE_W - 18 - aw, rowY, color, false);
					rowY += LINE_H;
				}
			}
		} else if (linger) {
			Auction.Bid w = auction.winner();
			ctx.drawString(font, "WINNER", 0, y0, Theme.GAIN, false);
			long remain = Auction.LINGER_MS - (System.currentTimeMillis() - auction.endedAtMs());
			String secs = (Math.max(0, remain) / 1000L) + "s";
			int sw = font.width(secs);
			ctx.drawString(font, secs, BASE_W - 18 - sw, y0, Theme.TEXT_DIM, false);
			if (w != null) {
				ctx.drawString(font, w.player(), 0, y0 + LINE_H + 2, Theme.TEXT, false);
				String item = auction.item();
				if (auction.itemCount() > 1) item = item + " x" + auction.itemCount();
				ctx.drawString(font, "won " + trim(font, item, BASE_W - 34), 0, y0 + LINE_H * 2 + 2, Theme.TEXT_MUTED, false);
				String amt = "for " + AmountFormat.pretty(w.amount());
				ctx.drawString(font, amt, 0, y0 + LINE_H * 3 + 4, Theme.GAIN, false);
			} else {
				ctx.drawString(font, "no bids", 0, y0 + LINE_H + 2, Theme.TEXT_DIM, false);
			}
		} else {
			ctx.drawString(font, "no auction", 0, y0, Theme.TEXT_DIM, false);
			ctx.drawString(font, "open Useful > Auction", 0, y0 + LINE_H, Theme.TEXT_MUTED, false);
		}

		pose.popPose();
	}

	private static String trim(Font font, String s, int maxW) {
		if (font.width(s) <= maxW) return s;
		while (s.length() > 1 && font.width(s + "...") > maxW) s = s.substring(0, s.length() - 1);
		return s + "...";
	}
}
