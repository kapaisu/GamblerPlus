package dev.mishka.gamblerplus.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class NetGraph {
	private static final DateTimeFormatter TIME_FMT =
			DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());
	private static final int MAX_POINTS = 200;

	private NetGraph() {}

	public static void draw(GuiGraphicsExtractor ctx, Font font,
	                        int x, int y, int w, int h,
	                        List<PaymentEvent> log, long startedAtMs, long nowMs,
	                        int mx, int my) {
		Theme.roundPanel(ctx, x, y, x + w, y + h, 4, Theme.SURFACE_ALT, Theme.PANEL_LINE);

		if (log.isEmpty()) {
			String s = "no payments yet";
			int sw = font.width(s);
			ctx.text(font, s, x + (w - sw) / 2, y + (h - font.lineHeight) / 2, Theme.TEXT_DIM, false);
			return;
		}

		List<PaymentEvent> chrono = new ArrayList<>(log);
		Collections.reverse(chrono);

		int total = chrono.size();
		int stride = Math.max(1, (total + MAX_POINTS - 1) / MAX_POINTS);
		List<PaymentEvent> down = new ArrayList<>();
		if (stride == 1) {
			down.addAll(chrono);
		} else {
			long net = 0;
			for (int i = 0; i < total; i++) {
				PaymentEvent e = chrono.get(i);
				net += e.incoming() ? e.amount() : -e.amount();
				if (i % stride == stride - 1 || i == total - 1) {
					down.add(new PaymentEvent(e.timestampMs(), e.player(), Math.abs(net), net >= 0));
					net = 0;
				}
			}
		}

		int n = down.size() + 1;
		long[] cum = new long[n];
		cum[0] = 0L;
		long running = 0;
		long minNet = 0, maxNet = 0;
		for (int i = 0; i < down.size(); i++) {
			PaymentEvent e = down.get(i);
			running += e.incoming() ? e.amount() : -e.amount();
			cum[i + 1] = running;
			if (running < minNet) minNet = running;
			if (running > maxNet) maxNet = running;
		}

		long range = Math.max(1L, maxNet - minNet);
		long padding = Math.max(1L, range / 10);
		long viewMin = minNet - padding;
		long viewMax = maxNet + padding;
		long vr = Math.max(1L, viewMax - viewMin);

		int padX = 32, padY = 10;
		int chartX = x + padX;
		int chartW = w - padX - 12;
		int chartY = y + padY;
		int chartH = h - padY * 2;

		int zeroY = chartY + chartH - (int) (((-viewMin) * (long) chartH) / vr);
		if (zeroY >= chartY && zeroY < chartY + chartH) {
			for (int gx = chartX; gx < chartX + chartW; gx += 4) {
				ctx.fill(gx, zeroY, gx + 2, zeroY + 1, 0x30FFFFFF);
			}
		}

		ctx.text(font, AmountFormat.signed(maxNet), x + 4, chartY - 1, Theme.TEXT_DIM, false);
		ctx.text(font, AmountFormat.signed(minNet), x + 4, chartY + chartH - font.lineHeight, Theme.TEXT_DIM, false);
		if (zeroY - chartY > font.lineHeight + 2 && chartY + chartH - zeroY > font.lineHeight + 2) {
			ctx.text(font, "0", x + 4, zeroY - font.lineHeight / 2, Theme.TEXT_DIM, false);
		}

		int[] xs = new int[n];
		int[] ys = new int[n];
		for (int i = 0; i < n; i++) {
			xs[i] = n == 1 ? chartX + chartW / 2 : chartX + (i * (chartW - 1)) / (n - 1);
			ys[i] = chartY + chartH - (int) (((cum[i] - viewMin) * (long) chartH) / vr);
		}

		int lastNet = cum[n - 1] > 0 ? 1 : (cum[n - 1] < 0 ? -1 : 0);
		int trendColor = lastNet > 0 ? Theme.GAIN : (lastNet < 0 ? Theme.LOSS : Theme.BRAND);
		int fillCol = (trendColor & 0x00FFFFFF) | 0x28000000;

		for (int i = 0; i < n - 1; i++) {
			int x0 = xs[i], y0 = ys[i], x1 = xs[i + 1], y1 = ys[i + 1];
			int lo = Math.min(x0, x1);
			int hi = Math.max(x0, x1);
			int spanW = Math.max(1, hi - lo);
			int dySeg = y1 - y0;
			for (int cx = lo; cx < hi; cx++) {
				int py = y0 + (dySeg * (cx - x0)) / spanW;
				int fillTop = Math.min(py, zeroY);
				int fillBot = Math.max(py, zeroY);
				if (fillTop < chartY) fillTop = chartY;
				if (fillBot > chartY + chartH) fillBot = chartY + chartH;
				if (fillBot > fillTop) ctx.fill(cx, fillTop, cx + 1, fillBot, fillCol);
			}
		}

		for (int i = 0; i < n - 1; i++) {
			drawThickLine(ctx, xs[i], ys[i], xs[i + 1], ys[i + 1], trendColor);
		}

		int hovered = -1;
		int dotSpacing = Math.max(1, n / 40);
		for (int i = 1; i < n; i++) {
			if (i != n - 1 && (i % dotSpacing) != 0) continue;
			int cxp = xs[i], cyp = ys[i];
			int color = down.get(i - 1).incoming() ? Theme.GAIN : Theme.LOSS;
			ctx.fill(cxp - 2, cyp - 2, cxp + 3, cyp + 3, Theme.SURFACE_SOLID);
			ctx.fill(cxp - 1, cyp - 1, cxp + 2, cyp + 2, color);
			if (mx >= cxp - 4 && mx <= cxp + 4 && my >= cyp - 4 && my <= cyp + 4) hovered = i;
		}

		if (hovered >= 1) {
			PaymentEvent e = down.get(hovered - 1);
			String time = TIME_FMT.format(Instant.ofEpochMilli(e.timestampMs()));
			String netStr = "net " + AmountFormat.signed(cum[hovered]);
			String delta = (stride > 1 ? "bucket " : (e.incoming() ? "+" : "-")) + AmountFormat.pretty(e.amount()) + (stride > 1 ? "" : " " + e.player());
			int lw = Math.max(font.width(time), Math.max(font.width(netStr), font.width(delta)));
			int tw = lw + 10;
			int th = font.lineHeight * 3 + 8;
			int tx = Math.max(x, Math.min(x + w - tw, mx + 8));
			int ty = Math.max(y, my - th - 4);
			Theme.roundPanel(ctx, tx, ty, tx + tw, ty + th, 3, Theme.BG_SOLID, trendColor);
			ctx.text(font, time, tx + 5, ty + 4, Theme.TEXT_DIM, false);
			ctx.text(font, netStr, tx + 5, ty + 4 + font.lineHeight, Theme.color(cum[hovered]), false);
			ctx.text(font, delta, tx + 5, ty + 4 + font.lineHeight * 2, e.incoming() ? Theme.GAIN : Theme.LOSS, false);
		}
	}

	private static void drawThickLine(GuiGraphicsExtractor ctx, int x0, int y0, int x1, int y1, int color) {
		int dx = Math.abs(x1 - x0);
		int dy = Math.abs(y1 - y0);
		int sx = x0 < x1 ? 1 : -1;
		int sy = y0 < y1 ? 1 : -1;
		int err = dx - dy;
		int x = x0, y = y0;
		int guard = 0;
		while (guard++ < 4096) {
			ctx.fill(x, y, x + 1, y + 2, color);
			ctx.fill(x, y + 1, x + 2, y + 2, color);
			if (x == x1 && y == y1) break;
			int e2 = 2 * err;
			if (e2 > -dy) { err -= dy; x += sx; }
			if (e2 < dx)  { err += dx; y += sy; }
		}
	}
}
