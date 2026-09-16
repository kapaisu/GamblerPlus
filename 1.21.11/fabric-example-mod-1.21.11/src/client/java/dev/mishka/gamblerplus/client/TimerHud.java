package dev.mishka.gamblerplus.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public final class TimerHud {
	private static final int NUM_W = 84;
	private static final int NUM_H = 30;
	private static final int RING_SIZE = 56;
	private static final int RING_R = 22;

	private TimerHud() {}

	public static int[] bounds(HudLayout layout) {
		boolean numeric = GamblerPlusClient.CONFIG.numericTimer();
		int bw = numeric ? NUM_W : RING_SIZE;
		int bh = numeric ? NUM_H : RING_SIZE;
		int w = Math.round(bw * layout.timerScale);
		int h = Math.round(bh * layout.timerScale);
		return new int[]{layout.timerX, layout.timerY, layout.timerX + w, layout.timerY + h};
	}

	public static void draw(GuiGraphics ctx, Font font, HudLayout layout, boolean editOutline) {
		TimerState timer = GamblerPlusClient.TIMER;
		if (!timer.active() && !editOutline) return;

		boolean numeric = GamblerPlusClient.CONFIG.numericTimer();
		int[] r = bounds(layout);
		int x = r[0], y = r[1], x2 = r[2], y2 = r[3];
		int edge = editOutline ? Theme.HUD_EDGE : Theme.PANEL_LINE;
		Theme.roundPanel(ctx, x, y, x2, y2, numeric ? 5 : (int)(RING_SIZE * layout.timerScale / 2), Theme.HUD_BG, edge);

		var pose = ctx.pose();
		pose.pushMatrix();
		pose.translate(x, y);
		pose.scale(layout.timerScale, layout.timerScale);

		if (numeric) {
			ctx.drawString(font, "TIMER", 6, 4, Theme.BRAND, false);
			String time = timer.active() ? TimeParse.prettyDuration(timer.remainingMs()) : "0:00";
			int color = timer.active() ? Theme.TEXT : Theme.TEXT_DIM;
			ctx.drawString(font, time, 6, 16, color, false);
		} else {
			float progress = 0f;
			if (timer.active() && timer.lastDurationMs() > 0) {
				progress = 1f - Math.min(1f, (float) timer.remainingMs() / (float) timer.lastDurationMs());
			}
			int cx = RING_SIZE / 2;
			int cy = RING_SIZE / 2;
			CircleDial.draw(ctx, cx, cy, RING_R, progress, Theme.BRAND, Theme.SURFACE_ALT);
			String time = timer.active() ? TimeParse.prettyDuration(timer.remainingMs()) : "0:00";
			int tw = font.width(time);
			int color = timer.active() ? Theme.TEXT : Theme.TEXT_DIM;
			ctx.drawString(font, time, cx - tw / 2, cy - font.lineHeight / 2 + 1, color, false);
		}
		pose.popMatrix();
	}
}
