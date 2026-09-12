package dev.mishka.gamblerplus.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public final class PurchasePopup {
	public static final long LIFE_MS = 5_000L;

	private static volatile String line = null;
	private static volatile long spawnedAtMs = 0L;

	private PurchasePopup() {}

	public static void trigger(int count, String item, long amount) {
		line = "You bought " + count + " " + item + " for " + AmountFormat.pretty(amount);
		spawnedAtMs = System.currentTimeMillis();
	}

	public static void draw(GuiGraphics ctx, Font font) {
		String l = line;
		if (l == null) return;
		long elapsed = System.currentTimeMillis() - spawnedAtMs;
		if (elapsed >= LIFE_MS) { line = null; return; }

		Minecraft mc = Minecraft.getInstance();
		int sw = mc.getWindow().getGuiScaledWidth();

		float t = elapsed / (float) LIFE_MS;
		float slide = t < 0.15f ? Theme.easeOutCubic(t / 0.15f) : 1f;
		float alpha = t < 0.75f ? 1f : Math.max(0f, 1f - (t - 0.75f) / 0.25f);

		int textW = font.width(l);
		int padX = 10, padY = 6;
		int w = textW + padX * 2;
		int h = font.lineHeight + padY * 2;
		int targetY = 46;
		int y = Math.round(targetY - (1f - slide) * 20f);
		int x = (sw - w) / 2;

		int a = Math.max(0, Math.min(255, (int) (alpha * 235)));
		int aLine = Math.max(0, Math.min(255, (int) (alpha * 255)));
		int fill = (a << 24) | 0x000E1014;
		int edge = (aLine << 24) | 0x00CCA044;
		Theme.roundPanel(ctx, x, y, x + w, y + h, 5, fill, edge);

		int text = (Math.max(0, Math.min(255, (int) (alpha * 255))) << 24) | 0x00FFD68A;
		ctx.drawString(font, l, x + padX, y + padY, text, true);
	}
}
