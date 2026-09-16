package dev.mishka.gamblerplus.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

public final class TextHud {
	public static final String[] FONT_NAMES = {
			"regular", "bold", "italic", "bold italic",
			"underlined", "bold underlined", "italic underlined", "strikethrough",
			"bold strikethrough", "obfuscated"
	};
	private static final int MIN_W = 40;

	private TextHud() {}

	public static Style styleFor(int fontIndex) {
		return switch (fontIndex) {
			case 1 -> Style.EMPTY.withBold(true);
			case 2 -> Style.EMPTY.withItalic(true);
			case 3 -> Style.EMPTY.withBold(true).withItalic(true);
			case 4 -> Style.EMPTY.withUnderlined(true);
			case 5 -> Style.EMPTY.withBold(true).withUnderlined(true);
			case 6 -> Style.EMPTY.withItalic(true).withUnderlined(true);
			case 7 -> Style.EMPTY.withStrikethrough(true);
			case 8 -> Style.EMPTY.withBold(true).withStrikethrough(true);
			case 9 -> Style.EMPTY.withObfuscated(true);
			default -> Style.EMPTY;
		};
	}

	public static Component componentFor(HudLayout.TextEntry entry) {
		MutableComponent c = Component.literal(entry.content);
		c.setStyle(styleFor(entry.font));
		return c;
	}

	public static int[] bounds(Font font, HudLayout.TextEntry entry) {
		String text = entry.content.isEmpty() ? "text" : entry.content;
		int textW = Math.max(MIN_W, font.width(text));
		int w = Math.round((textW + 8) * entry.scale);
		int h = Math.round((font.lineHeight + 6) * entry.scale);
		return new int[]{entry.x, entry.y, entry.x + w, entry.y + h};
	}

	public static void drawAll(GuiGraphicsExtractor ctx, Font font, HudLayout layout, boolean editOutline) {
		for (HudLayout.TextEntry e : layout.textEntries) {
			drawEntry(ctx, font, e, editOutline);
		}
		if (editOutline && layout.textEntries.isEmpty()) {
			int x = 10, y = 180, w = 100, h = 18;
			Theme.roundPanel(ctx, x, y, x + w, y + h, 4, 0x400E1014, Theme.HUD_EDGE);
			ctx.text(font, "no text entries", x + 4, y + 5, Theme.TEXT_DIM, false);
		}
	}

	public static void drawEntry(GuiGraphicsExtractor ctx, Font font, HudLayout.TextEntry entry, boolean editOutline) {
		boolean visible = !entry.content.isEmpty();
		if (!visible && !editOutline) return;

		int[] r = bounds(font, entry);
		int x = r[0], y = r[1], x2 = r[2], y2 = r[3];

		if (editOutline) {
			Theme.roundPanel(ctx, x, y, x2, y2, 4, 0x400E1014, Theme.HUD_EDGE);
		}

		var pose = ctx.pose();
		pose.pushMatrix();
		pose.translate(x + 4f, y + 3f);
		pose.scale(entry.scale, entry.scale);

		Component c = visible ? componentFor(entry) : Component.literal("empty").setStyle(Style.EMPTY.withItalic(true));
		int color = visible ? entry.argb() : Theme.TEXT_DIM;
		ctx.text(font, c, 0, 0, color, false);

		pose.popMatrix();
	}
}
