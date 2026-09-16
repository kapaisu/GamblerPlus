package dev.mishka.gamblerplus.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public final class SessionDetailScreen extends Screen {
	private static final int PANEL_W = 500;
	private static final int PANEL_H = 340;
	private static final int PLAYER_ROW_H = 18;
	private static final int PAYMENT_ROW_H = 12;
	private static final long ANIM_MS = 220L;

	private static final DateTimeFormatter TIME_FMT =
			DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());
	private static final DateTimeFormatter STAMP_FMT =
			DateTimeFormatter.ofPattern("MMM d HH:mm").withZone(ZoneId.systemDefault());

	private final Supplier<Session> sessionSupplier;
	private final boolean live;
	private final Set<String> expanded = new HashSet<>();
	private long openedAtMs;
	private int px, py;
	private int scroll;

	private int closeX, closeY, closeW, closeH;
	private int listTop, listBot, listX, listW;
	private final List<int[]> playerHitRects = new ArrayList<>();
	private final List<String> playerHitNames = new ArrayList<>();
	private final List<int[]> paymentHitRects = new ArrayList<>();
	private final List<PaymentEvent> paymentHitEvents = new ArrayList<>();
	private float uiScale = 1f;

	public SessionDetailScreen(Supplier<Session> sessionSupplier, boolean live) {
		super(Minecraft.getInstance(), Minecraft.getInstance().font, Component.literal("Session details"));
		this.sessionSupplier = sessionSupplier;
		this.live = live;
	}

	@Override protected void init() {
		openedAtMs = System.currentTimeMillis();
		int margin = 16;
		float sw = (float) (width  - margin) / PANEL_W;
		float sh = (float) (height - margin) / PANEL_H;
		uiScale = Math.min(1f, Math.min(sw, sh));
		int virtualW = Math.round(width  / uiScale);
		int virtualH = Math.round(height / uiScale);
		px = (virtualW  - PANEL_W) / 2;
		py = (virtualH - PANEL_H) / 2;
	}

	@Override public boolean isPauseScreen() { return false; }

	@Override
	public void render(GuiGraphics ctx, int mxRaw, int myRaw, float delta) {
		long elapsed = System.currentTimeMillis() - openedAtMs;
		float t = Math.min(1f, elapsed / (float) ANIM_MS);
		float eased = Theme.easeOutCubic(t);
		int backdropA = (int) (0xB0 * eased);
		ctx.fill(0, 0, width, height, backdropA << 24);

		int mx = Math.round(mxRaw / uiScale);
		int my = Math.round(myRaw / uiScale);

		var pose = ctx.pose();
		pose.pushMatrix();
		pose.translate(0f, (1f - eased) * -14f);
		pose.scale(uiScale, uiScale);

		ctx.fill(px, py, px + PANEL_W, py + PANEL_H, Theme.BG);
		outline(ctx, px, py, px + PANEL_W, py + PANEL_H, Theme.PANEL_LINE);
		ctx.fill(px, py, px + PANEL_W, py + 24, Theme.SURFACE);
		ctx.fill(px, py + 24, px + PANEL_W, py + 25, Theme.PANEL_LINE);

		Session s = sessionSupplier.get();
		if (s == null) {
			ctx.drawString(font, "no session", px + 14, py + 9, Theme.TEXT_DIM, false);
			drawCloseButton(ctx, mx, my);
			pose.popMatrix();
			return;
		}

		String title = s.label();
		if (live) {
			ctx.fill(px + 14, py + 12, px + 18, py + 16, Theme.GAIN);
			ctx.drawString(font, title, px + 22, py + 9, Theme.TEXT, false);
			int lw = font.width(title);
			ctx.drawString(font, "live", px + 22 + lw + 6, py + 9, Theme.GAIN, false);
		} else {
			ctx.drawString(font, title, px + 14, py + 9, Theme.TEXT, false);
		}

		drawCloseButton(ctx, mx, my);

		int metaY = py + 32;
		String meta = STAMP_FMT.format(Instant.ofEpochMilli(s.startedAt()));
		if (!live) {
			meta += " - " + DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
					.format(Instant.ofEpochMilli(s.endedAt()));
		}
		meta += "  -  " + formatDuration(live ? (System.currentTimeMillis() - s.startedAt()) : s.durationMs());
		ctx.drawString(font, meta, px + 14, metaY, Theme.TEXT_MUTED, false);

		int totalsY = metaY + 12;
		long net = s.net();
		String netStr = AmountFormat.signed(net);
		ctx.drawString(font, "net",     px + 14,  totalsY, Theme.TEXT_DIM, false);
		ctx.drawString(font, netStr,    px + 32,  totalsY, Theme.color(net), false);
		int cursor = px + 32 + font.width(netStr) + 12;
		ctx.drawString(font, "in",      cursor,   totalsY, Theme.TEXT_DIM, false);
		ctx.drawString(font, AmountFormat.pretty(s.in()), cursor + 14, totalsY, Theme.GAIN, false);
		cursor += 14 + font.width(AmountFormat.pretty(s.in())) + 12;
		ctx.drawString(font, "out",     cursor,   totalsY, Theme.TEXT_DIM, false);
		ctx.drawString(font, AmountFormat.pretty(s.out()), cursor + 18, totalsY, Theme.LOSS, false);
		cursor += 18 + font.width(AmountFormat.pretty(s.out())) + 12;
		String recStr = s.inCount() + "W / " + s.outCount() + "L";
		ctx.drawString(font, recStr, cursor, totalsY, Theme.TEXT, false);

		ctx.fill(px + 14, totalsY + 12, px + PANEL_W - 14, totalsY + 13, Theme.DIVIDER);

		int headerY = totalsY + 20;
		ctx.drawString(font, "PLAYER",     px + 20, headerY, Theme.TEXT_DIM, false);
		ctx.drawString(font, "RECORD",     px + 180, headerY, Theme.TEXT_DIM, false);
		ctx.drawString(font, "NET",        px + 260, headerY, Theme.TEXT_DIM, false);

		listX = px + 8;
		listW = PANEL_W - 16;
		listTop = headerY + 12;
		listBot = py + PANEL_H - 34;

		ctx.fill(listX, listTop, listX + listW, listBot, Theme.SURFACE_ALT);
		outline(ctx, listX, listTop, listX + listW, listBot, Theme.PANEL_LINE);

		playerHitRects.clear();
		playerHitNames.clear();
		paymentHitRects.clear();
		paymentHitEvents.clear();

		Map<String, List<PaymentEvent>> byPlayer = groupByPlayer(s.payments());
		if (byPlayer.isEmpty()) {
			String empty = "no payments recorded in this session";
			int ew = font.width(empty);
			ctx.drawString(font, empty, listX + (listW - ew) / 2, listTop + (listBot - listTop) / 2 - 4, Theme.TEXT_DIM, false);
			drawCloseInfo(ctx, py + PANEL_H - 22);
			pose.popMatrix();
			return;
		}

		List<Map.Entry<String, List<PaymentEvent>>> entries = new ArrayList<>(byPlayer.entrySet());
		entries.sort(Comparator.comparingLong(this::totalActivity).reversed());

		ctx.enableScissor(listX + 1, listTop + 1, listX + listW - 1, listBot - 1);

		int drawY = listTop + 4 - scroll;
		int contentHeight = 0;
		for (Map.Entry<String, List<PaymentEvent>> e : entries) {
			String player = e.getKey();
			List<PaymentEvent> events = e.getValue();
			long pIn = 0, pOut = 0;
			int pInC = 0, pOutC = 0;
			for (PaymentEvent p : events) {
				if (p.incoming()) { pIn += p.amount(); pInC++; }
				else              { pOut += p.amount(); pOutC++; }
			}
			long pNet = pIn - pOut;
			boolean isExpanded = expanded.contains(player);

			if (drawY + PLAYER_ROW_H > listTop && drawY < listBot) {
				drawPlayerRow(ctx, drawY, player, pInC, pOutC, pNet, isExpanded, mx, my);
			}
			playerHitRects.add(new int[]{listX + 4, drawY, listX + listW - 4, drawY + PLAYER_ROW_H});
			playerHitNames.add(player);
			drawY += PLAYER_ROW_H;
			contentHeight += PLAYER_ROW_H;

			if (isExpanded) {
				List<PaymentEvent> sorted = new ArrayList<>(events);
				sorted.sort(Comparator.comparingLong(PaymentEvent::timestampMs).reversed());
				for (int pi = 0; pi < sorted.size(); pi++) {
					PaymentEvent p = sorted.get(pi);
					if (drawY + PAYMENT_ROW_H > listTop && drawY < listBot) {
						drawPaymentRow(ctx, drawY, p, sorted, pi);
						if (live) drawRemoveButton(ctx, drawY, p, mx, my);
					}
					if (live) {
						int bx2 = listX + listW - 10;
						int bx1 = bx2 - 12;
						int by1 = drawY - 1;
						int by2 = drawY + PAYMENT_ROW_H - 1;
						paymentHitRects.add(new int[]{bx1, by1, bx2, by2});
						paymentHitEvents.add(p);
					}
					drawY += PAYMENT_ROW_H;
					contentHeight += PAYMENT_ROW_H;
				}
				if (drawY + 4 > listTop && drawY < listBot) {
					ctx.fill(listX + 20, drawY + 1, listX + listW - 20, drawY + 2, Theme.DIVIDER);
				}
				drawY += 6;
				contentHeight += 6;
			}
		}
		ctx.disableScissor();

		int viewH = listBot - listTop;
		if (contentHeight > viewH) {
			int barX = listX + listW - 4;
			int barTop = listTop + 2;
			int barLen = viewH - 4;
			int thumb = Math.max(16, barLen * viewH / contentHeight);
			int maxScroll = contentHeight - viewH;
			int off = maxScroll <= 0 ? 0 : (barLen - thumb) * scroll / maxScroll;
			ctx.fill(barX, barTop, barX + 2, barTop + barLen, Theme.PANEL_LINE);
			ctx.fill(barX, barTop + off, barX + 2, barTop + off + thumb, Theme.TEXT_MUTED);
		}

		drawCloseInfo(ctx, py + PANEL_H - 22);

		pose.popMatrix();
	}

	private void drawPlayerRow(GuiGraphics ctx, int y, String player, int wins, int losses, long net, boolean isExpanded, int mx, int my) {
		boolean hover = mx >= listX + 4 && mx < listX + listW - 4 && my >= y && my < y + PLAYER_ROW_H;
		if (hover) ctx.fill(listX + 2, y, listX + listW - 2, y + PLAYER_ROW_H, 0x14FFFFFF);

		String indicator = isExpanded ? "v" : ">";
		ctx.drawString(font, indicator, listX + 8, y + (PLAYER_ROW_H - font.lineHeight) / 2 + 1, Theme.BRAND, false);
		ctx.drawString(font, player, listX + 20, y + (PLAYER_ROW_H - font.lineHeight) / 2 + 1, Theme.TEXT, false);
		String rec = wins + "W / " + losses + "L";
		ctx.drawString(font, rec, listX + 172, y + (PLAYER_ROW_H - font.lineHeight) / 2 + 1, Theme.TEXT_MUTED, false);
		ctx.drawString(font, AmountFormat.signed(net), listX + 252, y + (PLAYER_ROW_H - font.lineHeight) / 2 + 1, Theme.color(net), false);
	}

	private void drawPaymentRow(GuiGraphics ctx, int y, PaymentEvent p, List<PaymentEvent> sorted, int idx) {
		String time = TIME_FMT.format(Instant.ofEpochMilli(p.timestampMs()));
		String dir  = p.incoming() ? "IN " : "OUT";
		String amt  = (p.incoming() ? "+" : "-") + AmountFormat.pretty(p.amount());
		int color = p.incoming() ? Theme.GAIN : Theme.LOSS;
		ctx.drawString(font, time, listX + 30, y, Theme.TEXT_DIM, false);
		ctx.drawString(font, dir,  listX + 80, y, color, false);
		ctx.drawString(font, amt,  listX + 110, y, color, false);
		String mult = Multiplier.labelFor(sorted, idx);
		if (mult != null) {
			ctx.drawString(font, mult, listX + 110 + font.width(amt) + 6, y, Theme.WARN, false);
		}
	}

	private void drawRemoveButton(GuiGraphics ctx, int y, PaymentEvent p, int mx, int my) {
		int bx2 = listX + listW - 10;
		int bx1 = bx2 - 12;
		int by1 = y - 1;
		int by2 = y + PAYMENT_ROW_H - 1;
		boolean hover = mx >= bx1 && mx < bx2 && my >= by1 && my < by2;
		int color = hover ? Theme.LOSS : Theme.TEXT_MUTED;
		int tw = font.width("x");
		ctx.drawString(font, "x", bx1 + (12 - tw) / 2, y + (PAYMENT_ROW_H - font.lineHeight) / 2, color, false);
	}

	private void drawCloseButton(GuiGraphics ctx, int mx, int my) {
		closeW = 62; closeH = 14;
		closeX = px + PANEL_W - closeW - 8;
		closeY = py + 5;
		boolean hover = mx >= closeX && mx < closeX + closeW && my >= closeY && my < closeY + closeH;
		ctx.fill(closeX, closeY, closeX + closeW, closeY + closeH, hover ? Theme.SURFACE_ALT : Theme.SURFACE);
		outline(ctx, closeX, closeY, closeX + closeW, closeY + closeH, hover ? Theme.BRAND : Theme.PANEL_LINE);
		int cw = font.width("close");
		ctx.drawString(font, "close", closeX + (closeW - cw) / 2,
				closeY + (closeH - font.lineHeight) / 2 + 1, hover ? Theme.BRAND : Theme.TEXT, false);
	}

	private void drawCloseInfo(GuiGraphics ctx, int y) {
		String hint = live
				? "click a player to expand - x to remove a payment"
				: "click a player to expand - scroll to navigate";
		ctx.drawString(font, hint, px + 14, y, Theme.TEXT_DIM, false);
	}

	private long totalActivity(Map.Entry<String, List<PaymentEvent>> e) {
		long sum = 0;
		for (PaymentEvent p : e.getValue()) sum += p.amount();
		return sum;
	}

	private Map<String, List<PaymentEvent>> groupByPlayer(List<PaymentEvent> events) {
		LinkedHashMap<String, List<PaymentEvent>> map = new LinkedHashMap<>();
		for (PaymentEvent e : events) map.computeIfAbsent(e.player(), k -> new ArrayList<>()).add(e);
		return map;
	}

	private String formatDuration(long ms) {
		long s = ms / 1000L;
		long m = s / 60L;
		long h = m / 60L;
		if (h > 0) return h + "h " + (m % 60) + "m";
		if (m > 0) return m + "m " + (s % 60) + "s";
		return s + "s";
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		int mx = Math.round((float) event.x() / uiScale);
		int my = Math.round((float) event.y() / uiScale);
		if (mx >= closeX && mx < closeX + closeW && my >= closeY && my < closeY + closeH) {
			onClose();
			return true;
		}
		if (mx >= listX && mx < listX + listW && my >= listTop && my < listBot) {
			for (int i = 0; i < paymentHitRects.size(); i++) {
				int[] h = paymentHitRects.get(i);
				if (mx >= h[0] && mx < h[2] && my >= h[1] && my < h[3]) {
					PaymentEvent p = paymentHitEvents.get(i);
					if (live && GamblerPlusClient.STATS.remove(p)) {
						if (p.incoming()) GamblerPlusClient.SESSIONS.onIncomingReversed(p.player(), p.amount());
						GamblerPlusClient.CONFIG.save();
					}
					return true;
				}
			}
			for (int i = 0; i < playerHitRects.size(); i++) {
				int[] h = playerHitRects.get(i);
				if (mx >= h[0] && mx < h[2] && my >= h[1] && my < h[3]) {
					String name = playerHitNames.get(i);
					if (expanded.contains(name)) expanded.remove(name);
					else expanded.add(name);
					return true;
				}
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double h, double v) {
		int step = 14;
		if (v < 0) scroll = Math.max(0, scroll + step);
		else       scroll = Math.max(0, scroll - step);
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.key() == GLFW.GLFW_KEY_ESCAPE) { onClose(); return true; }
		return super.keyPressed(event);
	}

	@Override
	public void onClose() {
		Minecraft.getInstance().setScreenAndShow(new TrackerScreen(GamblerPlusClient.CONFIG, GamblerPlusClient.STATS));
	}

	private void outline(GuiGraphics ctx, int x1, int y1, int x2, int y2, int c) {
		ctx.fill(x1, y1, x2, y1 + 1, c);
		ctx.fill(x1, y2 - 1, x2, y2, c);
		ctx.fill(x1, y1, x1 + 1, y2, c);
		ctx.fill(x2 - 1, y1, x2, y2, c);
	}
}
