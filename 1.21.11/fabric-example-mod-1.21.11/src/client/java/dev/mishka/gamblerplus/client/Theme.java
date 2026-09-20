package dev.mishka.gamblerplus.client;

import net.minecraft.client.gui.GuiGraphics;

public final class Theme {
	private static final int BG_RGB          = 0x0E1014;
	private static final int SURFACE_RGB     = 0x16181D;
	private static final int SURFACE_ALT_RGB = 0x1B1E24;
	private static final int HUD_BG_RGB      = 0x0E1014;
	private static final int BASE_PANEL_A    = 0xF2;
	private static final int BASE_HUD_A      = 0xE0;

	private static final int TEXT_RGB_BASE       = 0xE6E7EB;
	private static final int TEXT_MUTED_RGB_BASE = 0x7A8091;
	private static final int TEXT_DIM_RGB_BASE   = 0x4D5361;

	public static int BG          = 0xF2000000 | BG_RGB;
	public static int SURFACE     = 0xF2000000 | SURFACE_RGB;
	public static int SURFACE_ALT = 0xF2000000 | SURFACE_ALT_RGB;
	public static final int PANEL_LINE  = 0xFF262A32;
	public static final int DIVIDER     = 0xFF1F2229;
	public static final int BG_SOLID    = 0xFF0E1014;
	public static final int SURFACE_SOLID = 0xFF16181D;
	public static int HUD_BG    = 0xE0000000 | HUD_BG_RGB;
	public static final int HUD_EDGE    = 0xFF2B7BFF;

	public static int TEXT        = 0xFF000000 | TEXT_RGB_BASE;
	public static int TEXT_MUTED  = 0xFF000000 | TEXT_MUTED_RGB_BASE;
	public static int TEXT_DIM    = 0xFF000000 | TEXT_DIM_RGB_BASE;

	public static final int GAIN        = 0xFF4ADE80;
	public static final int LOSS        = 0xFFEF4444;
	public static final int WARN        = 0xFFF59E0B;
	public static int BRAND             = 0xFF2B7BFF;

	public static final int TOGGLE_ON   = 0xFF4ADE80;
	public static final int TOGGLE_OFF  = 0xFF3A3F4A;
	public static final int TOGGLE_BG   = 0xFF20232B;

	public static final int TOAST_BG    = 0xF01A0B0B;
	public static final int TOAST_EDGE  = 0xFFEF4444;

	private Theme() {}

	public static void applyAccent(int r, int g, int b) {
		int argb = 0xFF000000 | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
		BRAND = argb;
		TEXT = lerpColor(0xFF000000 | TEXT_RGB_BASE, argb, 0.16f);
		TEXT_MUTED = lerpColor(0xFF000000 | TEXT_MUTED_RGB_BASE, argb, 0.28f);
		TEXT_DIM = lerpColor(0xFF000000 | TEXT_DIM_RGB_BASE, argb, 0.28f);
	}

	public static void applyOpacity(int panelAlpha) {
		int a = Math.max(20, Math.min(255, panelAlpha));
		int hudA = Math.max(20, a - 18);
		BG = (a << 24) | BG_RGB;
		SURFACE = (a << 24) | SURFACE_RGB;
		SURFACE_ALT = (a << 24) | SURFACE_ALT_RGB;
		HUD_BG = (hudA << 24) | HUD_BG_RGB;
	}

	public static int hsvToRgb(float h, float s, float v) {
		h = h - (float) Math.floor(h);
		int i = (int) (h * 6f);
		float f = h * 6f - i;
		float p = v * (1f - s);
		float q = v * (1f - f * s);
		float t = v * (1f - (1f - f) * s);
		float r, g, b;
		switch (i % 6) {
			case 0 -> { r = v; g = t; b = p; }
			case 1 -> { r = q; g = v; b = p; }
			case 2 -> { r = p; g = v; b = t; }
			case 3 -> { r = p; g = q; b = v; }
			case 4 -> { r = t; g = p; b = v; }
			default -> { r = v; g = p; b = q; }
		}
		int ri = Math.round(r * 255f), gi = Math.round(g * 255f), bi = Math.round(b * 255f);
		return (ri << 16) | (gi << 8) | bi;
	}

	public static float[] rgbToHsv(int r, int g, int b) {
		float rf = r / 255f, gf = g / 255f, bf = b / 255f;
		float max = Math.max(rf, Math.max(gf, bf));
		float min = Math.min(rf, Math.min(gf, bf));
		float delta = max - min;
		float h;
		if (delta < 1e-6f) h = 0f;
		else if (max == rf) h = (((gf - bf) / delta) % 6f) / 6f;
		else if (max == gf) h = (((bf - rf) / delta) + 2f) / 6f;
		else h = (((rf - gf) / delta) + 4f) / 6f;
		if (h < 0f) h += 1f;
		float s = max <= 0f ? 0f : delta / max;
		return new float[]{h, s, max};
	}

	public static int color(long net) {
		if (net > 0) return GAIN;
		if (net < 0) return LOSS;
		return TEXT_MUTED;
	}

	public static int withAlpha(int argb, float a) {
		int alpha = Math.max(0, Math.min(255, (int) (a * 255f)));
		return (alpha << 24) | (argb & 0x00FFFFFF);
	}

	public static int scaleAlpha(int argb, float mul) {
		int alpha = (argb >>> 24) & 0xFF;
		int scaled = Math.max(0, Math.min(255, (int) (alpha * mul)));
		return (scaled << 24) | (argb & 0x00FFFFFF);
	}

	public static float easeOutCubic(float t) {
		float u = 1f - Math.max(0f, Math.min(1f, t));
		return 1f - u * u * u;
	}

	public static float easeOutBack(float t) {
		float x = Math.max(0f, Math.min(1f, t));
		float c1 = 1.70158f;
		float c3 = c1 + 1f;
		float u = x - 1f;
		return 1f + c3 * u * u * u + c1 * u * u;
	}

	public static float smoothStep(float start, float end, float now) {
		if (end <= start) return now >= end ? 1f : 0f;
		float t = (now - start) / (end - start);
		return Math.max(0f, Math.min(1f, t));
	}

	public static void roundRect(GuiGraphics ctx, int x1, int y1, int x2, int y2, int radius, int color) {
		int r = Math.min(radius, Math.min((x2 - x1) / 2, (y2 - y1) / 2));
		if (r <= 0) { ctx.fill(x1, y1, x2, y2, color); return; }
		ctx.fill(x1 + r, y1, x2 - r, y2, color);
		ctx.fill(x1, y1 + r, x1 + r, y2 - r, color);
		ctx.fill(x2 - r, y1 + r, x2, y2 - r, color);
		fillQuarter(ctx, x1 + r, y1 + r, r, color, true, true);
		fillQuarter(ctx, x2 - r, y1 + r, r, color, false, true);
		fillQuarter(ctx, x1 + r, y2 - r, r, color, true, false);
		fillQuarter(ctx, x2 - r, y2 - r, r, color, false, false);
	}

	public static void roundPanel(GuiGraphics ctx, int x1, int y1, int x2, int y2, int radius, int fill, int edge) {
		roundRect(ctx, x1, y1, x2, y2, radius, edge);
		int innerR = Math.max(0, radius - 1);
		roundRect(ctx, x1 + 1, y1 + 1, x2 - 1, y2 - 1, innerR, fill);
	}

	public static void roundOutline(GuiGraphics ctx, int x1, int y1, int x2, int y2, int radius, int color) {
		int r = Math.min(radius, Math.min((x2 - x1) / 2, (y2 - y1) / 2));
		if (r <= 0) {
			ctx.fill(x1, y1, x2, y1 + 1, color);
			ctx.fill(x1, y2 - 1, x2, y2, color);
			ctx.fill(x1, y1, x1 + 1, y2, color);
			ctx.fill(x2 - 1, y1, x2, y2, color);
			return;
		}
		ctx.fill(x1 + r, y1, x2 - r, y1 + 1, color);
		ctx.fill(x1 + r, y2 - 1, x2 - r, y2, color);
		ctx.fill(x1, y1 + r, x1 + 1, y2 - r, color);
		ctx.fill(x2 - 1, y1 + r, x2, y2 - r, color);
		strokeQuarter(ctx, x1 + r, y1 + r, r, color, true, true);
		strokeQuarter(ctx, x2 - r, y1 + r, r, color, false, true);
		strokeQuarter(ctx, x1 + r, y2 - r, r, color, true, false);
		strokeQuarter(ctx, x2 - r, y2 - r, r, color, false, false);
	}

	private static void fillQuarter(GuiGraphics ctx, int cx, int cy, int r, int color, boolean left, boolean top) {
		for (int dy = 0; dy < r; dy++) {
			double h = dy + 0.5;
			int dx = (int) Math.ceil(Math.sqrt(r * r - h * h));
			if (dx > r) dx = r;
			int y = top ? cy - 1 - dy : cy + dy;
			int xl = left ? cx - dx : cx;
			int xr = left ? cx : cx + dx;
			ctx.fill(xl, y, xr, y + 1, color);
		}
	}

	private static void strokeQuarter(GuiGraphics ctx, int cx, int cy, int r, int color, boolean left, boolean top) {
		int prev = r + 1;
		for (int dy = 0; dy < r; dy++) {
			double h = dy + 0.5;
			int dx = (int) Math.ceil(Math.sqrt(r * r - h * h));
			if (dx > r) dx = r;
			if (dy == r - 1 && dx < 1) dx = 1;
			int y = top ? cy - 1 - dy : cy + dy;
			int lo = Math.min(prev, dx);
			int hi = Math.max(prev, dx);
			for (int d = lo; d <= hi; d++) {
				int x = left ? cx - d : cx + d - 1;
				ctx.fill(x, y, x + 1, y + 1, color);
			}
			prev = dx;
		}
	}

	public static int lerpColor(int c1, int c2, float t) {
		float k = Math.max(0f, Math.min(1f, t));
		int a1 = (c1 >>> 24) & 0xFF, r1 = (c1 >>> 16) & 0xFF, g1 = (c1 >>> 8) & 0xFF, b1 = c1 & 0xFF;
		int a2 = (c2 >>> 24) & 0xFF, r2 = (c2 >>> 16) & 0xFF, g2 = (c2 >>> 8) & 0xFF, b2 = c2 & 0xFF;
		int a = (int) (a1 + (a2 - a1) * k);
		int r = (int) (r1 + (r2 - r1) * k);
		int g = (int) (g1 + (g2 - g1) * k);
		int b = (int) (b1 + (b2 - b1) * k);
		return (a << 24) | (r << 16) | (g << 8) | b;
	}
}
