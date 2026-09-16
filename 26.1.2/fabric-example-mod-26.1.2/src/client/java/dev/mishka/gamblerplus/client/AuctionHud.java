package dev.mishka.gamblerplus.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class AuctionHud {
	private static final int BASE_W = 200;
	private static final int BASE_H = 108;
	private static final int LINE_H = 10;
	private static final int RING_R = 16;
	private static final int RING_COL_W = RING_R * 2 + 8;

	private AuctionHud() {}

	public static int[] bounds(HudLayout layout) {
		int w = Math.round(BASE_W * layout.auctionScale);
		int h = Math.round(BASE_H * layout.auctionScale);
		return new int[]{layout.auctionX, layout.auctionY, layout.auctionX + w, layout.auctionY + h};
	}

	public static void draw(GuiGraphicsExtractor ctx, Font font, HudLayout layout, boolean editOutline) {
		Auction auction = GamblerPlusClient.AUCTION;
		boolean live = auction.active();
		boolean linger = auction.lingering();
		if (!live && !linger && !editOutline) return;

		int[] r = bounds(layout);
		int x = r[0], y = r[1], x2 = r[2], y2 = r[3];

		int edge = editOutline ? Theme.HUD_EDGE : (linger ? Theme.GAIN : Theme.PANEL_LINE);
		Theme.roundPanel(ctx, x, y, x2, y2, 6, Theme.HUD_BG, edge);

		var pose = ctx.pose();
		pose.pushMatrix();
		pose.translate(x + 8f, y + 6f);
		pose.scale(layout.auctionScale, layout.auctionScale);

		int innerW = BASE_W - 16;
		boolean numeric = GamblerPlusClient.CONFIG.numericTimer();
		int leftW = numeric || (!live && !linger) ? innerW : innerW - RING_COL_W;

		String bA = "Gambler ";
		String bB = "Plus";
		String bC = " auction";
		int wA = font.width(bA);
		int wB = font.width(bB);
		int wC = font.width(bC);
		int badgeTotal = wA + wB + wC;
		int badgeX = Math.max(0, (leftW - badgeTotal) / 2);
		ctx.text(font, bA, badgeX, 0, Theme.TEXT, false);
		ctx.text(font, bB, badgeX + wA, 0, Theme.BRAND, false);
		ctx.text(font, bC, badgeX + wA + wB, 0, Theme.TEXT_DIM, false);

		ctx.fill(0, LINE_H + 2, leftW, LINE_H + 3, Theme.PANEL_LINE);

		int y0 = LINE_H + 6;

		if (live) {
			ItemStack stack = auction.itemStack();
			int textX = 0;
			if (stack != null && !stack.isEmpty()) {
				ctx.item(stack, 0, y0 - 2);
				ctx.itemDecorations(font, stack, 0, y0 - 2);
				textX = 20;
			}
			String item = auction.item();
			if (auction.itemCount() > 1 && (stack == null || stack.isEmpty())) item = item + " x" + auction.itemCount();
			ctx.text(font, trim(font, item, leftW - textX), textX, y0, Theme.TEXT, false);

			if (numeric) {
				ctx.text(font, "ends " + TimeParse.prettyDuration(auction.remainingMs()),
						textX, y0 + LINE_H + 4, Theme.TEXT_MUTED, false);
			}

			List<Auction.Bid> bids = auction.topBids(3);
			int rowY = y0 + LINE_H * 2 + 6;
			if (bids.isEmpty()) {
				ctx.text(font, "waiting for bids", 0, rowY, Theme.TEXT_DIM, false);
			} else {
				for (int i = 0; i < bids.size(); i++) {
					Auction.Bid b = bids.get(i);
					int color = i == 0 ? Theme.GAIN : Theme.TEXT;
					String namePart = (i + 1) + "." + b.player();
					String amt = AmountFormat.pretty(b.amount());
					int aw = font.width(amt);
					int nameMax = leftW - aw - 6;
					ctx.text(font, trim(font, namePart, nameMax), 0, rowY, color, false);
					ctx.text(font, amt, leftW - aw - 2, rowY, color, false);
					rowY += LINE_H;
				}
			}

			if (!numeric) {
				float progress = 0f;
				long total = auction.endsAtMs() - auction.startedAtMs();
				if (total > 0) progress = 1f - Math.min(1f, (float) auction.remainingMs() / (float) total);
				int cx = leftW + RING_COL_W / 2;
				int cy = BASE_H / 2 - 6;
				CircleDial.draw(ctx, cx, cy, RING_R, progress, Theme.BRAND, Theme.SURFACE_ALT);
				String rem = TimeParse.prettyDuration(auction.remainingMs());
				int rw = font.width(rem);
				ctx.text(font, rem, cx - rw / 2, cy - font.lineHeight / 2 + 1, Theme.TEXT, false);
			}
		} else if (linger) {
			Auction.Bid w = auction.winner();
			ctx.text(font, "WINNER", 0, y0, Theme.GAIN, false);
			long remain = Auction.LINGER_MS - (System.currentTimeMillis() - auction.endedAtMs());
			String secs = (Math.max(0, remain) / 1000L) + "s";
			int sw = font.width(secs);
			ctx.text(font, secs, leftW - 2 - sw, y0, Theme.TEXT_DIM, false);
			if (w != null) {
				ctx.text(font, trim(font, w.player(), leftW), 0, y0 + LINE_H + 2, Theme.TEXT, false);
				ItemStack stack = auction.itemStack();
				int textX = 0;
				int row = y0 + LINE_H * 2 + 4;
				if (stack != null && !stack.isEmpty()) {
					ctx.item(stack, 0, row - 3);
					ctx.itemDecorations(font, stack, 0, row - 3);
					textX = 20;
				}
				String item = auction.item();
				if (auction.itemCount() > 1 && (stack == null || stack.isEmpty())) item = item + " x" + auction.itemCount();
				ctx.text(font, "won " + trim(font, item, leftW - textX - 18), textX, row, Theme.TEXT_MUTED, false);
				String amt = "for " + AmountFormat.pretty(w.amount());
				ctx.text(font, amt, 0, y0 + LINE_H * 3 + 8, Theme.GAIN, false);
			} else {
				ctx.text(font, "no bids", 0, y0 + LINE_H + 2, Theme.TEXT_DIM, false);
			}
		} else {
			ctx.text(font, "no auction", 0, y0, Theme.TEXT_DIM, false);
			ctx.text(font, "open Useful > Auction", 0, y0 + LINE_H, Theme.TEXT_MUTED, false);
		}

		pose.popMatrix();
	}

	private static String trim(Font font, String s, int maxW) {
		if (font.width(s) <= maxW) return s;
		while (s.length() > 1 && font.width(s + "...") > maxW) s = s.substring(0, s.length() - 1);
		return s + "...";
	}
}
