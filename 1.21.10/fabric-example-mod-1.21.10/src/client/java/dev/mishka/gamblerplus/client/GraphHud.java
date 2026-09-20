package dev.mishka.gamblerplus.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

public final class GraphHud {
	private static final int BASE_W = 170;
	private static final int BASE_H = 122;

	private GraphHud() {}

	public static int[] bounds(HudLayout layout) {
		int w = Math.round(BASE_W * layout.graphScale);
		int h = Math.round(BASE_H * layout.graphScale);
		return new int[]{layout.graphX, layout.graphY, layout.graphX + w, layout.graphY + h};
	}

	public static void draw(GuiGraphics ctx, Font font, HudLayout layout, Config config, Stats stats, boolean editOutline) {
		if (!config.graphHudEnabled() && !editOutline) return;

		int[] r = bounds(layout);
		int x = r[0], y = r[1], x2 = r[2], y2 = r[3];

		int edge = editOutline ? Theme.HUD_EDGE : Theme.PANEL_LINE;
		Theme.roundPanel(ctx, x, y, x2, y2, 6, Theme.HUD_BG, edge);

		var pose = ctx.pose();
		pose.pushMatrix();
		pose.translate(x + 8f, y + 6f);
		pose.scale(layout.graphScale, layout.graphScale);

		int innerW = BASE_W - 16;

		long net = stats.sessionNet();
		String amount = AmountFormat.signed(net);
		int amountY = 0;
		pose.pushMatrix();
		pose.scale(1.4f, 1.4f);
		ctx.drawString(font, amount, 0, Math.round(amountY / 1.4f), Theme.color(net), false);
		pose.popMatrix();

		SessionManager sessions = GamblerPlusClient.SESSIONS;
		boolean hasActive = sessions.hasActive();
		long started = hasActive ? sessions.currentStartedAt() : System.currentTimeMillis();
		long durMs = hasActive ? System.currentTimeMillis() - started : 0L;
		String dur = formatDuration(durMs);
		int amountVisualH = Math.round(font.lineHeight * 1.4f);
		int durY = amountY + amountVisualH + 2;
		ctx.drawString(font, dur, 0, durY, Theme.TEXT_DIM, false);

		int chartY = durY + font.lineHeight + 6;
		int chartH = Math.max(20, BASE_H - 16 - chartY);
		List<PaymentEvent> snap = stats.snapshot();
		long now = System.currentTimeMillis();
		NetGraph.draw(ctx, font, 0, chartY, innerW, chartH, snap, started, now, -9999, -9999, config.graphStyle(), false);

		pose.popMatrix();
	}

	private static String formatDuration(long ms) {
		long s = ms / 1000L;
		long m = s / 60L;
		long h = m / 60L;
		if (h > 0) return h + "h " + (m % 60) + "m " + (s % 60) + "s";
		if (m > 0) return m + "m " + (s % 60) + "s";
		return s + "s";
	}
}
