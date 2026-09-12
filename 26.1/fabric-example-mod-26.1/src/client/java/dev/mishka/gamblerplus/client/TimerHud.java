package dev.mishka.gamblerplus.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class TimerHud {
	private static final int BASE_W = 84;
	private static final int BASE_H = 30;

	private TimerHud() {}

	public static int[] bounds(HudLayout layout) {
		int w = Math.round(BASE_W * layout.timerScale);
		int h = Math.round(BASE_H * layout.timerScale);
		return new int[]{layout.timerX, layout.timerY, layout.timerX + w, layout.timerY + h};
	}

	public static void draw(GuiGraphicsExtractor ctx, Font font, HudLayout layout, boolean editOutline) {
		TimerState timer = GamblerPlusClient.TIMER;
		if (!timer.active() && !editOutline) return;

		int[] r = bounds(layout);
		int x = r[0], y = r[1], x2 = r[2], y2 = r[3];
		int edge = editOutline ? Theme.HUD_EDGE : Theme.PANEL_LINE;
		Theme.roundPanel(ctx, x, y, x2, y2, 5, Theme.HUD_BG, edge);

		var pose = ctx.pose();
		pose.pushMatrix();
		pose.translate(x + 6f, y + 4f);
		pose.scale(layout.timerScale, layout.timerScale);
		ctx.text(font, "TIMER", 0, 0, Theme.BRAND, false);
		String time = timer.active()
				? TimeParse.prettyDuration(timer.remainingMs())
				: "0:00";
		int color = timer.active() ? Theme.TEXT : Theme.TEXT_DIM;
		ctx.text(font, time, 0, 12, color, false);
		pose.popMatrix();
	}
}
