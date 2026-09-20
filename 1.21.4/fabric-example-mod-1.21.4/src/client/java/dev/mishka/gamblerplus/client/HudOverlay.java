package dev.mishka.gamblerplus.client;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public final class HudOverlay {
	private static float hoverT = 0f;
	private static long lastFrameNs = 0L;

	private HudOverlay() {}

	public static void register(Config config, Stats stats) {
		HudRenderCallback.EVENT.register((ctx, tickCounter) -> render(ctx, config, stats));
	}

	private static void render(GuiGraphics ctx, Config config, Stats stats) {
		Font font = Minecraft.getInstance().font;
		int sw = ctx.guiWidth();
		if (config.hudBar()) drawBar(ctx, font, config, stats, sw, false);
		if (config.showToast()) drawToast(ctx, font, stats, sw);
		HudLayout layout = config.hudLayout();
		ImageHud.drawAll(ctx, font, layout, layout.editing);
		AuctionHud.draw(ctx, font, layout, layout.editing);
		TimerHud.draw(ctx, font, layout, layout.editing);
		GraphHud.draw(ctx, font, layout, config, stats, layout.editing);
		TextHud.drawAll(ctx, font, layout, layout.editing);
		WinnerBanner.draw(ctx, font);
		PurchasePopup.draw(ctx, font);
	}

	static int[] barBounds(Config config, Stats stats, Font font, int sw) {
		long net = stats.sessionNet();
		String amount;
		if (config.hudDisplay() == Config.HudDisplay.PERCENT) {
			double pct = stats.sessionProfitPct();
			amount = String.format("%s%.1f%%", pct > 0 ? "+" : "", pct);
		} else {
			amount = AmountFormat.signed(net);
		}
		String label = "LIVE";
		int labelW = font.width(label);
		int amountW = font.width(amount);
		int padX = 10;
		int padY = 5;
		int gap = 10;
		int baseW = padX + labelW + gap + amountW + padX;
		int baseH = font.lineHeight + padY * 2;
		HudLayout layout = config.hudLayout();
		int x, y;
		if (layout.liveX >= 0) {
			x = layout.liveX;
			y = layout.liveY;
		} else {
			y = 5;
			x = switch (config.hudAnchor()) { case TOP_LEFT -> 5; case TOP_CENTER -> (sw - baseW) / 2; case TOP_RIGHT -> sw - baseW - 5; };
		}
		int w = Math.round(baseW * layout.liveScale);
		int h = Math.round(baseH * layout.liveScale);
		return new int[]{x, y, x + w, y + h};
	}

	static void drawBar(GuiGraphics ctx, Font font, Config config, Stats stats, int sw, boolean editOutline) {
		long net = stats.sessionNet();
		String amount;
		if (config.hudDisplay() == Config.HudDisplay.PERCENT) {
			double pct = stats.sessionProfitPct();
			amount = String.format("%s%.1f%%", pct > 0 ? "+" : "", pct);
		} else {
			amount = AmountFormat.signed(net);
		}
		String label = "LIVE";
		int textColor = Theme.color(net);
		int labelW = font.width(label);
		int amountW = font.width(amount);
		int padX = 10;
		int padY = 5;
		int gap = 10;
		int baseW = padX + labelW + gap + amountW + padX;
		int baseH = font.lineHeight + padY * 2;
		int[] r = barBounds(config, stats, font, sw);
		int x = r[0], y = r[1];
		int w = r[2] - r[0];
		int h = r[3] - r[1];
		HudLayout layout = config.hudLayout();
		if (editOutline) {
			Theme.roundPanel(ctx, x - 1, y - 1, x + w + 1, y + h + 1, 3, 0x201155CC, Theme.HUD_EDGE);
		}
		float hoverScale = 1f;
		if (!editOutline) {
			Minecraft mc = Minecraft.getInstance();
			var window = mc.getWindow();
			double curX = mc.mouseHandler.xpos() * (double) window.getGuiScaledWidth()  / Math.max(1, window.getScreenWidth());
			double curY = mc.mouseHandler.ypos() * (double) window.getGuiScaledHeight() / Math.max(1, window.getScreenHeight());
			boolean hovered = curX >= x && curX < x + w && curY >= y && curY < y + h;
			long now = System.nanoTime();
			float dt = lastFrameNs == 0L ? 1f / 60f : Math.min(0.1f, (now - lastFrameNs) / 1_000_000_000f);
			lastFrameNs = now;
			float target = hovered ? 1f : 0f;
			hoverT += (target - hoverT) * Math.min(1f, dt * 12f);
			float t = Math.max(0f, Math.min(1f, hoverT));
			hoverScale = 1f + 0.04f * t;
		}
		var pose = ctx.pose();
		pose.pushPose();
		pose.translate(x, y, 0f);
		pose.scale(layout.liveScale, layout.liveScale, 1f);
		pose.pushPose();
		float pcx = baseW / 2f;
		float pcy = baseH / 2f;
		pose.translate(pcx, pcy, 0f);
		pose.scale(hoverScale, hoverScale, 1f);
		pose.translate(-pcx, -pcy, 0f);
		int bg = 0xB8000000;
		ctx.fill(0, 1, baseW, baseH - 1, bg);
		ctx.fill(1, 0, baseW - 1, 1, bg);
		ctx.fill(1, baseH - 1, baseW - 1, baseH, bg);
		int textY = padY;
		ctx.drawString(font, label, padX, textY, 0xFF8B8E96, false);
		ctx.drawString(font, amount, padX + labelW + gap, textY, textColor, false);
		pose.popPose();
		pose.popPose();
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
