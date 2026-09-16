package dev.mishka.gamblerplus.client;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public final class HudOverlay {
	private HudOverlay() {}

	public static void register(Config config, Stats stats) {
		HudRenderCallback.EVENT.register((ctx, tickCounter) -> render(ctx, config, stats));
	}

	private static void render(GuiGraphics ctx, Config config, Stats stats) {
		Font font = Minecraft.getInstance().font;
		int sw = ctx.guiWidth();
		if (config.hudBar()) drawBar(ctx, font, config, stats, sw);
		if (config.showToast()) drawToast(ctx, font, stats, sw);
		HudLayout layout = config.hudLayout();
		ImageHud.drawAll(ctx, font, layout, layout.editing);
		AuctionHud.draw(ctx, font, layout, layout.editing);
		TimerHud.draw(ctx, font, layout, layout.editing);
		TextHud.drawAll(ctx, font, layout, layout.editing);
		WinnerBanner.draw(ctx, font);
		PurchasePopup.draw(ctx, font);
	}

	private static void drawBar(GuiGraphics ctx, Font font, Config config, Stats stats, int sw) {
		long net = stats.sessionNet();
		String label;
		if (config.hudDisplay() == Config.HudDisplay.PERCENT) {
			double pct = stats.sessionProfitPct();
			label = String.format("%s%.1f%%", pct > 0 ? "+" : "", pct);
		} else {
			label = AmountFormat.signed(net);
		}
		String tag = config.gamblingMode() ? "LIVE" : "PAUSED";
		int tagW = font.width(tag), labelW = font.width(label);
		int contentW = tagW + 8 + 1 + 8 + labelW;
		int w = contentW + 16, h = font.lineHeight + 10;
		int y = 4;
		int x = switch (config.hudAnchor()) { case TOP_LEFT -> 4; case TOP_CENTER -> (sw - w) / 2; case TOP_RIGHT -> sw - w - 4; };
		ctx.fill(x, y, x + w, y + h, Theme.SURFACE);
		ctx.fill(x, y, x + w, y + 1, Theme.PANEL_LINE);
		ctx.fill(x, y + h - 1, x + w, y + h, Theme.PANEL_LINE);
		ctx.fill(x, y, x + 1, y + h, Theme.PANEL_LINE);
		ctx.fill(x + w - 1, y, x + w, y + h, Theme.PANEL_LINE);
		int cursor = x + 8, textY = y + 5;
		int tagColor = config.gamblingMode() ? Theme.GAIN : Theme.TEXT_DIM;
		ctx.drawString(font, tag, cursor, textY, tagColor, false);
		cursor += tagW + 8;
		ctx.fill(cursor, y + 4, cursor + 1, y + h - 4, Theme.DIVIDER);
		cursor += 9;
		ctx.drawString(font, label, cursor, textY, Theme.color(net), false);
	}

	private static void drawToast(GuiGraphics ctx, Font font, Stats stats, int sw) {
		long until = stats.toastUntilMs();
		long now = System.currentTimeMillis();
		long remain = until - now;
		if (remain <= 0) return;
		float slideIn = Math.min(1f, (now - stats.toastShownAtMs()) / 260f);
		float easedIn = Theme.easeOutCubic(slideIn);
		float fade = remain > 500 ? 1f : remain / 500f;
		String title = "cold streak";
		String detail = stats.toastStreak() + " losses in a row";
		int w = Math.max(font.width(title), font.width(detail)) + 20;
		int h = font.lineHeight * 2 + 14;
		int x = sw - w - 8 + (int) ((1f - easedIn) * (w + 16));
		int y = 32;
		int bg = Theme.scaleAlpha(Theme.TOAST_BG, fade);
		int edge = Theme.scaleAlpha(Theme.TOAST_EDGE, fade);
		int line = Theme.scaleAlpha(Theme.PANEL_LINE, fade);
		int text = Theme.scaleAlpha(Theme.TEXT, fade);
		ctx.fill(x, y, x + w, y + h, bg);
		ctx.fill(x, y, x + 2, y + h, edge);
		ctx.fill(x, y, x + w, y + 1, line);
		ctx.fill(x, y + h - 1, x + w, y + h, line);
		ctx.fill(x + w - 1, y, x + w, y + h, line);
		ctx.drawString(font, title, x + 10, y + 5, Theme.scaleAlpha(Theme.LOSS, fade), false);
		ctx.drawString(font, detail, x + 10, y + 5 + font.lineHeight + 2, text, false);
	}
}
