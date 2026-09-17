package dev.mishka.gamblerplus.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class NetGraph {
	private static final DateTimeFormatter TIME_FMT =
			DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

	private static int cachedN = -1;
	private static long cachedFirstTs = 0L;
	private static long cachedLastTs = 0L;
	private static int cachedChartW = -1;
	private static int cachedChartH = -1;
	private static int cachedChartX;
	private static int cachedChartY;
	private static int cachedNumCandles;
	private static int cachedCellW;
	private static int cachedCandleW;
	private static int cachedZeroY;
	private static long cachedMinV;
	private static long cachedMaxV;
	private static long cachedViewMin;
	private static long cachedVr;
	private static long[] cachedOpens;
	private static long[] cachedHighs;
	private static long[] cachedLows;
	private static long[] cachedCloses;
	private static int[] cachedFirstIdx;
	private static int[] cachedLastIdx;

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

		int n = log.size();
		int padX = 44, padY = 10;
		int chartX = x + padX;
		int chartW = Math.max(2, w - padX - 12);
		int chartY = y + padY;
		int chartH = Math.max(2, h - padY * 2);
		int chartBottom = chartY + chartH;

		long firstTs = log.get(n - 1).timestampMs();
		long lastTs  = log.get(0).timestampMs();

		boolean cacheHit = cachedN == n
				&& cachedFirstTs == firstTs
				&& cachedLastTs == lastTs
				&& cachedChartW == chartW
				&& cachedChartH == chartH
				&& cachedChartX == chartX
				&& cachedChartY == chartY
				&& cachedOpens != null;

		int numCandles, cellW, candleW, zeroY;
		long minV, maxV, viewMin, vr;
		long[] opens, highs, lows, closes;
		int[] firstIdx, lastIdx;

		if (cacheHit) {
			numCandles = cachedNumCandles;
			cellW = cachedCellW;
			candleW = cachedCandleW;
			zeroY = cachedZeroY;
			minV = cachedMinV;
			maxV = cachedMaxV;
			viewMin = cachedViewMin;
			vr = cachedVr;
			opens = cachedOpens;
			highs = cachedHighs;
			lows = cachedLows;
			closes = cachedCloses;
			firstIdx = cachedFirstIdx;
			lastIdx = cachedLastIdx;
		} else {
			candleW = 7;
			cellW = 10;
			numCandles = Math.max(1, Math.min(chartW / cellW, n));
			opens = new long[numCandles];
			highs = new long[numCandles];
			lows = new long[numCandles];
			closes = new long[numCandles];
			firstIdx = new int[numCandles];
			lastIdx = new int[numCandles];
			long running = 0;
			long prevClose = 0;
			int currentBucket = -1;
			for (int i = 0; i < n; i++) {
				PaymentEvent e = log.get(n - 1 - i);
				int b = (int) ((long) i * numCandles / n);
				if (b >= numCandles) b = numCandles - 1;
				if (b != currentBucket) {
					currentBucket = b;
					opens[b] = prevClose;
					highs[b] = prevClose;
					lows[b] = prevClose;
					firstIdx[b] = i;
				}
				running += e.incoming() ? e.amount() : -e.amount();
				if (running > highs[b]) highs[b] = running;
				if (running < lows[b]) lows[b] = running;
				closes[b] = running;
				lastIdx[b] = i;
				prevClose = running;
			}
			long mn = 0, mx0 = 0;
			for (int b = 0; b < numCandles; b++) {
				if (highs[b] > mx0) mx0 = highs[b];
				if (lows[b] < mn) mn = lows[b];
			}
			minV = mn;
			maxV = mx0;
			long range = Math.max(1L, maxV - minV);
			long padV = Math.max(1L, range / 10);
			viewMin = minV - padV;
			long viewMax = maxV + padV;
			vr = Math.max(1L, viewMax - viewMin);
			zeroY = chartY + chartH - (int) (((-viewMin) * (long) chartH) / vr);
			cachedN = n;
			cachedFirstTs = firstTs;
			cachedLastTs = lastTs;
			cachedChartW = chartW;
			cachedChartH = chartH;
			cachedChartX = chartX;
			cachedChartY = chartY;
			cachedNumCandles = numCandles;
			cachedCellW = cellW;
			cachedCandleW = candleW;
			cachedZeroY = zeroY;
			cachedMinV = minV;
			cachedMaxV = maxV;
			cachedViewMin = viewMin;
			cachedVr = vr;
			cachedOpens = opens;
			cachedHighs = highs;
			cachedLows = lows;
			cachedCloses = closes;
			cachedFirstIdx = firstIdx;
			cachedLastIdx = lastIdx;
		}

		ctx.text(font, AmountFormat.signed(maxV), x + 4, chartY - 1, Theme.TEXT_DIM, false);
		ctx.text(font, AmountFormat.signed(minV), x + 4, chartY + chartH - font.lineHeight, Theme.TEXT_DIM, false);
		if (zeroY - chartY > font.lineHeight + 2 && chartBottom - zeroY > font.lineHeight + 2) {
			ctx.text(font, "0", x + 4, zeroY - font.lineHeight / 2, Theme.TEXT_DIM, false);
		}

		if (zeroY >= chartY && zeroY < chartBottom) {
			for (int gx = chartX; gx < chartX + chartW; gx += 4) {
				ctx.fill(gx, zeroY, gx + 2, zeroY + 1, 0x30FFFFFF);
			}
		}

		int hoverBucket = -1;
		for (int b = 0; b < numCandles; b++) {
			int cx = chartX + b * cellW;
			int oy = chartY + chartH - (int) (((opens[b] - viewMin) * (long) chartH) / vr);
			int cy = chartY + chartH - (int) (((closes[b] - viewMin) * (long) chartH) / vr);
			int hy = chartY + chartH - (int) (((highs[b] - viewMin) * (long) chartH) / vr);
			int ly = chartY + chartH - (int) (((lows[b] - viewMin) * (long) chartH) / vr);
			boolean up = closes[b] >= opens[b];
			int col = up ? Theme.GAIN : Theme.LOSS;
			int wickX = cx + candleW / 2;
			ctx.fill(wickX, hy, wickX + 1, ly + 1, col);
			int bodyTop = Math.min(oy, cy);
			int bodyBot = Math.max(oy, cy);
			if (bodyBot - bodyTop < 1) bodyBot = bodyTop + 1;
			ctx.fill(cx, bodyTop, cx + candleW, bodyBot, col);
			if (mx >= cx && mx < cx + cellW && my >= y && my < y + h) {
				hoverBucket = b;
			}
		}

		if (hoverBucket >= 0) {
			int b = hoverBucket;
			long o = opens[b], c = closes[b], hi = highs[b], lo = lows[b];
			int fi = firstIdx[b], li = lastIdx[b];
			PaymentEvent firstE = log.get(n - 1 - fi);
			PaymentEvent lastE = log.get(n - 1 - li);
			String timeStr = TIME_FMT.format(Instant.ofEpochMilli(firstE.timestampMs()));
			if (fi != li) timeStr = timeStr + " - " + TIME_FMT.format(Instant.ofEpochMilli(lastE.timestampMs()));
			String openStr  = "O " + AmountFormat.signed(o);
			String hiStr    = "H " + AmountFormat.signed(hi);
			String loStr    = "L " + AmountFormat.signed(lo);
			String closeStr = "C " + AmountFormat.signed(c);
			int lw = font.width(timeStr);
			if (font.width(openStr)  > lw) lw = font.width(openStr);
			if (font.width(hiStr)    > lw) lw = font.width(hiStr);
			if (font.width(loStr)    > lw) lw = font.width(loStr);
			if (font.width(closeStr) > lw) lw = font.width(closeStr);
			int tw = lw + 10;
			int th = font.lineHeight * 5 + 10;
			int tx = Math.max(x, Math.min(x + w - tw, mx + 8));
			int ty = Math.max(y, my - th - 4);
			int trend = c >= o ? Theme.GAIN : Theme.LOSS;
			Theme.roundPanel(ctx, tx, ty, tx + tw, ty + th, 3, Theme.BG_SOLID, trend);
			ctx.text(font, timeStr,  tx + 5, ty + 4, Theme.TEXT_DIM, false);
			ctx.text(font, openStr,  tx + 5, ty + 4 + font.lineHeight,     Theme.TEXT, false);
			ctx.text(font, hiStr,    tx + 5, ty + 4 + font.lineHeight * 2, Theme.GAIN, false);
			ctx.text(font, loStr,    tx + 5, ty + 4 + font.lineHeight * 3, Theme.LOSS, false);
			ctx.text(font, closeStr, tx + 5, ty + 4 + font.lineHeight * 4, trend, false);
		}
	}
}
