package dev.mishka.gamblerplus.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class TextScreen extends Screen {
	private static final int PANEL_W = 460;
	private static final int PANEL_H = 300;

	private int px, py;
	private float uiScale = 1f;
	private long openedAtMs;

	private int selected = 0;
	private boolean textFocused = true;
	private int dragChannel = -1;
	private int listScroll = 0;
	private int[] listBoundsRect;
	private boolean fontDropdownOpen = false;
	private long fontDropdownAt = 0L;

	private int[] fieldRect;
	private int[] closeRect;
	private int[] fontRect;
	private int[] addRect;
	private int[] removeRect;
	private int[][] sliderRects = new int[3][];
	private final List<int[]> entryRects = new ArrayList<>();
	private final List<int[]> fontOptionRects = new ArrayList<>();

	public TextScreen() {
		super(Minecraft.getInstance(), Minecraft.getInstance().font, Component.literal("Text on GUI"));
	}

	@Override protected void init() {
		int margin = 16;
		float sw = (float) (width - margin) / PANEL_W;
		float sh = (float) (height - margin) / PANEL_H;
		uiScale = Math.min(1f, Math.min(sw, sh));
		int vw = Math.round(width / uiScale);
		int vh = Math.round(height / uiScale);
		px = (vw - PANEL_W) / 2;
		py = (vh - PANEL_H) / 2;
		openedAtMs = System.currentTimeMillis();
		List<HudLayout.TextEntry> entries = GamblerPlusClient.CONFIG.hudLayout().textEntries;
		if (entries.isEmpty()) selected = -1;
		else if (selected >= entries.size()) selected = entries.size() - 1;
	}

	@Override public boolean isPauseScreen() { return false; }

	@Override
	public void render(GuiGraphics ctx, int mxRaw, int myRaw, float delta) {
		float t = Math.min(1f, (System.currentTimeMillis() - openedAtMs) / 200f);
		float e = Theme.easeOutCubic(t);
		ctx.fill(0, 0, width, height, (int) (0xB0 * e) << 24);

		int mx = Math.round(mxRaw / uiScale);
		int my = Math.round(myRaw / uiScale);

		var pose = ctx.pose();
		pose.pushMatrix();
		pose.translate(0f, (1f - e) * -14f);
		pose.scale(uiScale, uiScale);

		Widgets.panel(ctx, px, py, px + PANEL_W, py + PANEL_H);
		Widgets.header(ctx, font, px, py, PANEL_W, "Text on GUI");

		int cw = 62, ch = 14;
		int cx = px + PANEL_W - cw - 8, cy = py + 5;
		Widgets.flatButton(ctx, font, "close", cx, cy, cw, ch, mx, my, Theme.BRAND);
		closeRect = new int[]{cx, cy, cx + cw, cy + ch};

		HudLayout layout = GamblerPlusClient.CONFIG.hudLayout();
		List<HudLayout.TextEntry> entries = layout.textEntries;

		int listX = px + 12;
		int listY = py + 36;
		int listW = 140;
		int listH = PANEL_H - 88;
		Theme.roundPanel(ctx, listX, listY, listX + listW, listY + listH, 4, Theme.SURFACE_ALT, Theme.PANEL_LINE);
		ctx.drawString(font, "ENTRIES", listX + 6, listY + 4, Theme.TEXT_DIM, false);

		entryRects.clear();
		int rowH = 14;
		int listTopY = listY + 14;
		int listBotY = listY + listH - 20;
		int visibleRows = Math.max(1, (listBotY - listTopY) / (rowH + 2));
		int maxScroll = Math.max(0, entries.size() - visibleRows);
		if (listScroll > maxScroll) listScroll = maxScroll;
		if (listScroll < 0) listScroll = 0;

		listBoundsRect = new int[]{listX, listTopY, listX + listW, listBotY};
		ctx.enableScissor(listX + 1, listTopY, listX + listW - 1, listBotY);
		int rowY = listTopY + 2;
		for (int i = listScroll; i < entries.size(); i++) {
			if (rowY > listBotY - rowH) break;
			HudLayout.TextEntry en = entries.get(i);
			boolean isSel = i == selected;
			int fill = isSel ? Theme.SURFACE : (mx >= listX + 4 && mx < listX + listW - 4 && my >= rowY && my < rowY + rowH ? Theme.SURFACE : 0x00000000);
			if ((fill >>> 24) != 0) Theme.roundRect(ctx, listX + 4, rowY, listX + listW - 4, rowY + rowH, 3, fill);
			if (isSel) Theme.roundOutline(ctx, listX + 4, rowY, listX + listW - 4, rowY + rowH, 3, Theme.BRAND);
			String label = (i + 1) + ". " + (en.content.isEmpty() ? "empty" : trimName(en.content, 14));
			ctx.drawString(font, label, listX + 8, rowY + (rowH - font.lineHeight) / 2 + 1, isSel ? Theme.TEXT : Theme.TEXT_MUTED, false);
			entryRects.add(new int[]{listX + 4, rowY, listX + listW - 4, rowY + rowH});
			rowY += rowH + 2;
		}
		ctx.disableScissor();

		if (entries.isEmpty()) {
			ctx.drawString(font, "no entries yet", listX + 8, listY + 20, Theme.TEXT_DIM, false);
		}
		if (entries.size() > visibleRows) {
			int sbX = listX + listW - 4;
			int sbTop = listTopY + 2;
			int sbLen = listBotY - listTopY - 4;
			int thumb = Math.max(10, sbLen * visibleRows / entries.size());
			int off = maxScroll == 0 ? 0 : (sbLen - thumb) * listScroll / maxScroll;
			ctx.fill(sbX, sbTop, sbX + 2, sbTop + sbLen, Theme.PANEL_LINE);
			ctx.fill(sbX, sbTop + off, sbX + 2, sbTop + off + thumb, Theme.TEXT_MUTED);
		}

		int addBtnY = listY + listH - 16;
		Widgets.flatButton(ctx, font, "+ add", listX + 4, addBtnY, listW - 10, 14, mx, my, Theme.GAIN);
		addRect = new int[]{listX + 4, addBtnY, listX + listW - 6, addBtnY + 14};

		int editX = listX + listW + 10;
		int editW = PANEL_W - (editX - px) - 12;

		if (selected >= 0 && selected < entries.size()) {
			HudLayout.TextEntry en = entries.get(selected);
			int contentY = listY;
			ctx.drawString(font, "text", editX, contentY, Theme.TEXT_DIM, false);
			int fY = contentY + 12;
			int fH = 20;
			Widgets.textField(ctx, font, editX, fY, editW, fH, en.content, textFocused, "type something");
			fieldRect = new int[]{editX, fY, editX + editW, fY + fH};

			int r2Y = fY + fH + 8;
			ctx.drawString(font, "font", editX, r2Y, Theme.TEXT_DIM, false);
			int fontBtnX = editX + 40;
			int fontBtnW = 160;
			int fontBtnH = 14;
			String arrow = fontDropdownOpen ? " v" : " >";
			String fontLabel = TextHud.FONT_NAMES[en.font] + arrow;
			Widgets.flatButton(ctx, font, fontLabel, fontBtnX, r2Y - 3, fontBtnW, fontBtnH, mx, my, Theme.BRAND);
			fontRect = new int[]{fontBtnX, r2Y - 3, fontBtnX + fontBtnW, r2Y - 3 + fontBtnH};

			int sliderY = r2Y + 16;
			int sliderH = 6;
			int labelW = 14;
			int sliderW = editW - labelW - 40;
			String[] labels = {"R", "G", "B"};
			int[] chColors = {0xFFFF4444, 0xFF44FF44, 0xFF4488FF};
			int[] chVals = {en.r, en.g, en.b};
			for (int i = 0; i < 3; i++) {
				int y = sliderY + i * 16;
				ctx.drawString(font, labels[i], editX, y, Theme.TEXT_DIM, false);
				int trackX = editX + labelW;
				int trackY = y + 1;
				Theme.roundPanel(ctx, trackX, trackY, trackX + sliderW, trackY + sliderH, 3, Theme.SURFACE, Theme.PANEL_LINE);
				int filled = trackX + Math.round((chVals[i] / 255f) * sliderW);
				Theme.roundRect(ctx, trackX, trackY, filled, trackY + sliderH, 3, chColors[i]);
				int knobX = filled;
				int knobY = trackY + sliderH / 2;
				fillDot(ctx, knobX, knobY, 4, Theme.TEXT);
				fillDot(ctx, knobX, knobY, 3, chColors[i]);
				String val = String.valueOf(chVals[i]);
				ctx.drawString(font, val, trackX + sliderW + 8, y - 1, Theme.TEXT, false);
				sliderRects[i] = new int[]{trackX - 4, trackY - 5, trackX + sliderW + 4, trackY + sliderH + 5};
			}

			int previewY = sliderY + 3 * 16 + 6;
			int previewH = 34;
			Theme.roundPanel(ctx, editX, previewY, editX + editW, previewY + previewH, 5, Theme.SURFACE, Theme.PANEL_LINE);
			ctx.drawString(font, "PREVIEW", editX + 6, previewY + 3, Theme.TEXT_DIM, false);
			Component preview = en.content.isEmpty()
					? Component.literal("your text here").setStyle(TextHud.styleFor(en.font))
					: TextHud.componentFor(en);
			int pColor = en.content.isEmpty() ? Theme.TEXT_DIM : en.argb();
			ctx.enableScissor(editX + 4, previewY + 12, editX + editW - 4, previewY + previewH - 2);
			ctx.drawString(font, preview, editX + 8, previewY + 16, pColor, false);
			ctx.disableScissor();

			int removeY = py + PANEL_H - 24;
			Widgets.flatButton(ctx, font, "remove entry", editX, removeY, editW, 14, mx, my, Theme.LOSS);
			removeRect = new int[]{editX, removeY, editX + editW, removeY + 14};
		} else {
			ctx.drawString(font, "select or add an entry", editX, listY + 30, Theme.TEXT_DIM, false);
			fieldRect = null; fontRect = null; removeRect = null;
			for (int i = 0; i < 3; i++) sliderRects[i] = null;
		}

		if (dragChannel != -1) {
			long window = Minecraft.getInstance().getWindow().handle();
			boolean down = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
			if (!down) dragChannel = -1;
			else seekChannel(mx, dragChannel);
		}

		drawFontDropdown(ctx, mx, my);

		pose.popMatrix();
	}

	private void drawFontDropdown(GuiGraphics ctx, int mx, int my) {
		fontOptionRects.clear();
		if (!fontDropdownOpen || fontRect == null) return;
		float t = Math.min(1f, (System.currentTimeMillis() - fontDropdownAt) / 180f);
		float e = Theme.easeOutCubic(t);
		int itemH = 12;
		int totalH = TextHud.FONT_NAMES.length * itemH + 4;
		int shownH = Math.round(totalH * e);
		if (shownH < 3) return;
		int dX = fontRect[0];
		int dY = fontRect[3] + 2;
		int dW = fontRect[2] - fontRect[0];
		Theme.roundPanel(ctx, dX, dY, dX + dW, dY + shownH, 3, Theme.BG_SOLID, Theme.BRAND);
		ctx.enableScissor(dX + 1, dY + 1, dX + dW - 1, dY + shownH - 1);
		HudLayout layout = GamblerPlusClient.CONFIG.hudLayout();
		int curFont = -1;
		if (selected >= 0 && selected < layout.textEntries.size()) curFont = layout.textEntries.get(selected).font;
		for (int i = 0; i < TextHud.FONT_NAMES.length; i++) {
			int iy = dY + 2 + i * itemH;
			int[] rect = new int[]{dX + 2, iy, dX + dW - 2, iy + itemH};
			fontOptionRects.add(rect);
			boolean hover = mx >= rect[0] && mx < rect[2] && my >= rect[1] && my < rect[3];
			if (hover) ctx.fill(rect[0], rect[1], rect[2], rect[3], 0x30FFFFFF);
			if (i == curFont) ctx.fill(rect[0], rect[1], rect[0] + 2, rect[3], Theme.BRAND);
			int textColor = i == curFont ? Theme.BRAND : (hover ? Theme.TEXT : Theme.TEXT_MUTED);
			ctx.drawString(font, TextHud.FONT_NAMES[i], rect[0] + 8, iy + (itemH - font.lineHeight) / 2 + 1, textColor, false);
		}
		ctx.disableScissor();
	}

	private static String trimName(String s, int max) {
		if (s.length() <= max) return s;
		return s.substring(0, max - 1) + "..";
	}

	private void seekChannel(int mx, int ch) {
		int[] r = sliderRects[ch];
		if (r == null) return;
		List<HudLayout.TextEntry> entries = GamblerPlusClient.CONFIG.hudLayout().textEntries;
		if (selected < 0 || selected >= entries.size()) return;
		HudLayout.TextEntry en = entries.get(selected);
		int leftX = r[0] + 4;
		int rightX = r[2] - 4;
		int w = Math.max(1, rightX - leftX);
		float pos = (float) (mx - leftX) / w;
		if (pos < 0f) pos = 0f;
		if (pos > 1f) pos = 1f;
		int v = Math.round(pos * 255f);
		if (ch == 0) en.r = v;
		else if (ch == 1) en.g = v;
		else en.b = v;
		GamblerPlusClient.CONFIG.save();
	}

	private void fillDot(GuiGraphics ctx, int cx, int cy, int r, int color) {
		for (int dy = -r; dy < r; dy++) {
			double h = dy + 0.5;
			int dx = (int) Math.round(Math.sqrt(r * r - h * h));
			if (dx > 0) ctx.fill(cx - dx, cy + dy, cx + dx, cy + dy + 1, color);
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		int mx = Math.round((float) event.x() / uiScale);
		int my = Math.round((float) event.y() / uiScale);
		if (hit(closeRect, mx, my)) { onClose(); return true; }
		HudLayout layout = GamblerPlusClient.CONFIG.hudLayout();
		if (hit(addRect, mx, my)) {
			HudLayout.TextEntry t = new HudLayout.TextEntry();
			t.y = 180 + layout.textEntries.size() * 16;
			layout.textEntries.add(t);
			selected = layout.textEntries.size() - 1;
			textFocused = true;
			GamblerPlusClient.CONFIG.save();
			return true;
		}
		for (int i = 0; i < entryRects.size(); i++) {
			if (hit(entryRects.get(i), mx, my)) {
				selected = i;
				textFocused = true;
				return true;
			}
		}
		textFocused = hit(fieldRect, mx, my);
		if (fontDropdownOpen) {
			for (int i = 0; i < fontOptionRects.size(); i++) {
				if (hit(fontOptionRects.get(i), mx, my)) {
					if (selected >= 0 && selected < layout.textEntries.size()) {
						layout.textEntries.get(selected).font = i;
						GamblerPlusClient.CONFIG.save();
					}
					fontDropdownOpen = false;
					return true;
				}
			}
		}
		if (hit(fontRect, mx, my) && selected >= 0 && selected < layout.textEntries.size()) {
			fontDropdownOpen = !fontDropdownOpen;
			fontDropdownAt = System.currentTimeMillis();
			return true;
		}
		if (fontDropdownOpen) {
			fontDropdownOpen = false;
			return true;
		}
		for (int i = 0; i < 3; i++) {
			if (hit(sliderRects[i], mx, my)) {
				dragChannel = i;
				seekChannel(mx, i);
				return true;
			}
		}
		if (hit(removeRect, mx, my) && selected >= 0 && selected < layout.textEntries.size()) {
			layout.textEntries.remove(selected);
			if (selected >= layout.textEntries.size()) selected = layout.textEntries.size() - 1;
			GamblerPlusClient.CONFIG.save();
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	private static boolean hit(int[] r, int mx, int my) {
		return r != null && mx >= r[0] && mx < r[2] && my >= r[1] && my < r[3];
	}

	@Override
	public boolean mouseScrolled(double mxD, double myD, double hd, double vd) {
		int mx = Math.round((float) mxD / uiScale);
		int my = Math.round((float) myD / uiScale);
		if (hit(listBoundsRect, mx, my)) {
			if (vd < 0) listScroll++;
			else if (vd > 0) listScroll--;
			if (listScroll < 0) listScroll = 0;
			return true;
		}
		return super.mouseScrolled(mxD, myD, hd, vd);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		int key = event.key();
		if (key == GLFW.GLFW_KEY_ESCAPE) { onClose(); return true; }
		if (!textFocused) return super.keyPressed(event);
		HudLayout layout = GamblerPlusClient.CONFIG.hudLayout();
		if (selected < 0 || selected >= layout.textEntries.size()) return super.keyPressed(event);
		HudLayout.TextEntry en = layout.textEntries.get(selected);
		if (key == GLFW.GLFW_KEY_BACKSPACE && !en.content.isEmpty()) {
			en.content = en.content.substring(0, en.content.length() - 1);
			GamblerPlusClient.CONFIG.save();
			return true;
		}
		if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
			textFocused = false;
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		if (!textFocused) return false;
		HudLayout layout = GamblerPlusClient.CONFIG.hudLayout();
		if (selected < 0 || selected >= layout.textEntries.size()) return false;
		HudLayout.TextEntry en = layout.textEntries.get(selected);
		if (en.content.length() > 64) return true;
		int cp = event.codepoint();
		if (cp >= 32 && cp < 127) {
			en.content += (char) cp;
			GamblerPlusClient.CONFIG.save();
			return true;
		}
		return false;
	}

	@Override public void onClose() {
		Minecraft.getInstance().setScreenAndShow(new UsefulScreen());
	}
}
