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

	private int[] fieldRect;
	private int[] closeRect;
	private int[] fontRect;
	private int[] addRect;
	private int[] removeRect;
	private int[][] sliderRects = new int[3][];
	private final List<int[]> entryRects = new ArrayList<>();

	public TextScreen() {
		super(Component.literal("Text on GUI"));
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
		int rowY = listY + 16;
		int rowH = 14;
		for (int i = 0; i < entries.size(); i++) {
			HudLayout.TextEntry en = entries.get(i);
			boolean isSel = i == selected;
			int fill = isSel ? Theme.SURFACE : (mx >= listX + 4 && mx < listX + listW - 4 && my >= rowY && my < rowY + rowH ? Theme.SURFACE : 0x00000000);
			if ((fill >>> 24) != 0) Theme.roundRect(ctx, listX + 4, rowY, listX + listW - 4, rowY + rowH, 3, fill);
			if (isSel) Theme.roundOutline(ctx, listX + 4, rowY, listX + listW - 4, rowY + rowH, 3, Theme.BRAND);
			String label = (i + 1) + ". " + (en.content.isEmpty() ? "empty" : trimName(en.content, 14));
			ctx.drawString(font, label, listX + 8, rowY + (rowH - font.lineHeight) / 2 + 1, isSel ? Theme.TEXT : Theme.TEXT_MUTED, false);
			entryRects.add(new int[]{listX + 4, rowY, listX + listW - 4, rowY + rowH});
			rowY += rowH + 2;
			if (rowY > listY + listH - 20) break;
		}
		if (entries.isEmpty()) {
			ctx.drawString(font, "no entries yet", listX + 8, listY + 20, Theme.TEXT_DIM, false);
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
			int fontBtnW = 120;
			int fontBtnH = 14;
			String fontLabel = "< " + TextHud.FONT_NAMES[en.font] + " >";
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

		pose.popMatrix();
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
		if (hit(fontRect, mx, my) && selected >= 0 && selected < layout.textEntries.size()) {
			HudLayout.TextEntry en = layout.textEntries.get(selected);
			en.font = (en.font + 1) % TextHud.FONT_NAMES.length;
			GamblerPlusClient.CONFIG.save();
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
