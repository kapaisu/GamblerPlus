package dev.mishka.gamblerplus.client;

import net.minecraft.client.gui.GuiGraphics;

public final class CircleDial {
	private CircleDial() {}

	public static void draw(GuiGraphics ctx, int cx, int cy, int r, float progress, int filled, int track) {
		float p = Math.max(0f, Math.min(1f, progress));
		int thick = Math.max(3, r / 3);
		int inner = r - thick;
		double outer = r + 0.5;
		double innerB = inner - 0.5;

		for (int dy = -r - 1; dy <= r; dy++) {
			for (int dx = -r - 1; dx <= r; dx++) {
				double px = dx + 0.5;
				double py = dy + 0.5;
				double dist = Math.sqrt(px * px + py * py);
				if (dist > outer) continue;
				if (dist < innerB) continue;
				double outerCov = Math.max(0.0, Math.min(1.0, outer - dist));
				double innerCov = Math.max(0.0, Math.min(1.0, dist - innerB));
				double coverage = Math.min(outerCov, innerCov);
				if (coverage <= 0.02) continue;
				double angle = Math.atan2(px, -py);
				double norm = angle / (2 * Math.PI);
				if (norm < 0) norm += 1;
				int base = norm <= p ? filled : track;
				int color = Theme.scaleAlpha(base, (float) coverage);
				ctx.fill(cx + dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
			}
		}
	}
}
