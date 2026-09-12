package dev.mishka.gamblerplus.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class Widgets {
	private Widgets() {}

	public static boolean button(GuiGraphicsExtractor ctx, Font font, String label,
	                             int x, int y, int w, int h, int mx, int my,
	                             int accent, float hoverT) {
		boolean hover = mx >= x && mx < x + w && my >= y && my < y + h;
		int fill = Theme.lerpColor(Theme.SURFACE, Theme.SURFACE_ALT, hover ? hoverT : 0f);
		int edge = hover ? Theme.lerpColor(Theme.PANEL_LINE, accent, hoverT) : Theme.PANEL_LINE;
		Theme.roundPanel(ctx, x, y, x + w, y + h, 4, fill, edge);
		int lw = font.width(label);
		int textColor = hover ? Theme.lerpColor(Theme.TEXT, accent, hoverT) : Theme.TEXT;
		ctx.text(font, label, x + (w - lw) / 2, y + (h - font.lineHeight) / 2, textColor, false);
		return hover;
	}

	public static boolean flatButton(GuiGraphicsExtractor ctx, Font font, String label,
	                                 int x, int y, int w, int h, int mx, int my, int accent) {
		boolean hover = mx >= x && mx < x + w && my >= y && my < y + h;
		int fill = hover ? Theme.SURFACE_ALT : Theme.SURFACE;
		int edge = hover ? accent : Theme.PANEL_LINE;
		Theme.roundPanel(ctx, x, y, x + w, y + h, 3, fill, edge);
		int lw = font.width(label);
		ctx.text(font, label, x + (w - lw) / 2, y + (h - font.lineHeight) / 2, hover ? accent : Theme.TEXT, false);
		return hover;
	}

	public static void panel(GuiGraphicsExtractor ctx, int x1, int y1, int x2, int y2) {
		Theme.roundPanel(ctx, x1, y1, x2, y2, 6, Theme.BG, Theme.PANEL_LINE);
	}

	public static void header(GuiGraphicsExtractor ctx, Font font, int x, int y, int w, String title) {
		Theme.roundRect(ctx, x, y, x + w, y + 24, 6, Theme.SURFACE);
		ctx.fill(x + 1, y + 23, x + w - 1, y + 24, Theme.PANEL_LINE);
		ctx.text(font, title, x + 12, y + 9, Theme.TEXT, false);
	}

	public static void textField(GuiGraphicsExtractor ctx, Font font, int x, int y, int w, int h,
	                             String value, boolean focused, String placeholder) {
		int fill = focused ? Theme.SURFACE_ALT : Theme.SURFACE;
		int edge = focused ? Theme.BRAND : Theme.PANEL_LINE;
		Theme.roundPanel(ctx, x, y, x + w, y + h, 3, fill, edge);
		String shown = value.isEmpty() ? placeholder : value;
		int color = value.isEmpty() ? Theme.TEXT_DIM : Theme.TEXT;
		int textY = y + (h - font.lineHeight) / 2 + 1;
		ctx.enableScissor(x + 1, y + 1, x + w - 1, y + h - 1);
		ctx.text(font, shown, x + 6, textY, color, false);
		if (focused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
			int cx = x + 6 + font.width(value);
			ctx.fill(cx, textY - 1, cx + 1, textY + font.lineHeight, Theme.BRAND);
		}
		ctx.disableScissor();
	}
}
