package dev.mishka.gamblerplus.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class ThemesScreen extends Screen {
	private static final int PANEL_W = 380;
	private static final int PANEL_H = 250;
	private static final int WHEEL_RADIUS = 38;

	private static final int[][] PRESETS = {
			{0x2B, 0x7B, 0xFF},
			{0xEF, 0x44, 0x44},
			{0x22, 0xC5, 0x5E},
			{0xA8, 0x55, 0xF7},
			{0xF5, 0x9E, 0x0B},
			{0xEC, 0x48, 0x99},
			{0x22, 0xD3, 0xEE},
			{0xE6, 0xE7, 0xEB},
	};

	private int px, py;
	private float uiScale = 1f;
	private long openedAtMs;

	private int wheelCx, wheelCy;
	private int[] closeRect;
	private int[] wheelRect;
	private int[] brightnessRect;
	private int[] opacityRect;
	private int[][] channelRects = new int[3][];
	private final List<int[]> presetRects = new ArrayList<>();

	private int dragTarget = 0;

	public ThemesScreen() {
		super(Component.literal("Themes"));
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
		Widgets.header(ctx, font, px, py, PANEL_W, "Themes");

		int cw = 62, ch = 14;
		int cx = px + PANEL_W - cw - 8, cy = py + 5;
		Widgets.flatButton(ctx, font, "close", cx, cy, cw, ch, mx, my, Theme.BRAND);
		closeRect = new int[]{cx, cy, cx + cw, cy + ch};

		Config config = GamblerPlusClient.CONFIG;
		int curR = config.themeR(), curG = config.themeG(), curB = config.themeB();
		float[] hsv = Theme.rgbToHsv(curR, curG, curB);

		int contentTop = py + 34;
		ctx.drawString(font, "presets", px + 16, contentTop, Theme.TEXT_DIM, false);

		presetRects.clear();
		int swatchW = 32, swatchH = 18, gap = 4;
		int swatchY = contentTop + 10;
		for (int i = 0; i < PRESETS.length; i++) {
			int sx = px + 16 + i * (swatchW + gap);
			int rgb = 0xFF000000 | (PRESETS[i][0] << 16) | (PRESETS[i][1] << 8) | PRESETS[i][2];
			boolean isCurrent = curR == PRESETS[i][0] && curG == PRESETS[i][1] && curB == PRESETS[i][2];
			int edge = isCurrent ? Theme.BRAND : Theme.PANEL_LINE;
			Theme.roundPanel(ctx, sx, swatchY, sx + swatchW, swatchY + swatchH, 3, rgb, edge);
			presetRects.add(new int[]{sx, swatchY, sx + swatchW, swatchY + swatchH});
		}

		int customTop = swatchY + swatchH + 12;

		int leftColX = px + 20;
		int leftColW = 120;
		wheelCx = leftColX + leftColW / 2;
		wheelCy = customTop + 4 + WHEEL_RADIUS;
		drawWheel(ctx, wheelCx, wheelCy, WHEEL_RADIUS, hsv[2]);
		drawWheelMarker(ctx, hsv[0], hsv[1]);
		wheelRect = new int[]{wheelCx - WHEEL_RADIUS, wheelCy - WHEEL_RADIUS, wheelCx + WHEEL_RADIUS, wheelCy + WHEEL_RADIUS};

		int previewW = 60, previewH = 16;
		int previewY = wheelCy + WHEEL_RADIUS + 10;
		int previewX = leftColX + (leftColW - previewW) / 2;
		Theme.roundPanel(ctx, previewX, previewY, previewX + previewW, previewY + previewH, 3, Theme.BRAND, Theme.PANEL_LINE);

		int rightColX = leftColX + leftColW + 20;
		int rightColW = px + PANEL_W - 16 - rightColX;

		ctx.drawString(font, "brightness", rightColX, customTop, Theme.TEXT_DIM, false);
		int brightTrackY = customTop + 10;
		int brightTrackH = 8;
		drawBrightnessTrack(ctx, rightColX, brightTrackY, rightColW, brightTrackH, hsv[0], hsv[1]);
		int knobX = rightColX + Math.round(hsv[2] * rightColW);
		int knobY = brightTrackY + brightTrackH / 2;
		fillDot(ctx, knobX, knobY, 4, Theme.TEXT);
		fillDot(ctx, knobX, knobY, 3, Theme.BRAND);
		brightnessRect = new int[]{rightColX - 4, brightTrackY - 5, rightColX + rightColW + 4, brightTrackY + brightTrackH + 5};

		int sliderY = brightTrackY + 18;
		int labelW = 14;
		int sliderW = rightColW - labelW - 40;
		String[] labels = {"R", "G", "B"};
		int[] chColors = {0xFFFF4444, 0xFF44FF44, 0xFF4488FF};
		int[] chVals = {curR, curG, curB};
		for (int i = 0; i < 3; i++) {
			int y = sliderY + i * 16;
			ctx.drawString(font, labels[i], rightColX, y, Theme.TEXT_DIM, false);
			int trackX = rightColX + labelW;
			int trackY = y + 1;
			Theme.roundPanel(ctx, trackX, trackY, trackX + sliderW, trackY + 6, 3, Theme.SURFACE, Theme.PANEL_LINE);
			int filled = trackX + Math.round((chVals[i] / 255f) * sliderW);
			Theme.roundRect(ctx, trackX, trackY, filled, trackY + 6, 3, chColors[i]);
			fillDot(ctx, filled, trackY + 3, 4, Theme.TEXT);
			fillDot(ctx, filled, trackY + 3, 3, chColors[i]);
			String val = String.valueOf(chVals[i]);
			ctx.drawString(font, val, trackX + sliderW + 8, y - 1, Theme.TEXT, false);
			channelRects[i] = new int[]{trackX - 4, trackY - 5, trackX + sliderW + 4, trackY + 11};
		}

		int leftBottom = previewY + previewH;
		int rightBottom = sliderY + 3 * 16 + 6;
		int opacityY = Math.max(leftBottom, rightBottom) + 16;

		ctx.drawString(font, "gui transparency", px + 16, opacityY, Theme.TEXT_DIM, false);
		String opPct = Math.round((config.guiAlpha() - 20) / (float) (255 - 20) * 100f) + "%";
		int opPctW = font.width(opPct);
		ctx.drawString(font, opPct, px + PANEL_W - 16 - opPctW, opacityY, Theme.TEXT, false);

		int opTrackY = opacityY + font.lineHeight + 6;
		int opTrackH = 8;
		int opTrackX = px + 16;
		int opTrackW = PANEL_W - 32;
		Theme.roundPanel(ctx, opTrackX, opTrackY, opTrackX + opTrackW, opTrackY + opTrackH, 3, Theme.SURFACE, Theme.PANEL_LINE);
		float opPos = (config.guiAlpha() - 20) / (float) (255 - 20);
		int opFilled = opTrackX + Math.round(opPos * opTrackW);
		Theme.roundRect(ctx, opTrackX, opTrackY, opFilled, opTrackY + opTrackH, 3, Theme.BRAND);
		fillDot(ctx, opFilled, opTrackY + opTrackH / 2, 4, Theme.TEXT);
		fillDot(ctx, opFilled, opTrackY + opTrackH / 2, 3, Theme.BRAND);
		opacityRect = new int[]{opTrackX - 4, opTrackY - 5, opTrackX + opTrackW + 4, opTrackY + opTrackH + 5};

		if (dragTarget != 0) {
			long window = Minecraft.getInstance().getWindow().handle();
			boolean down = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
			if (!down) dragTarget = 0;
			else applyDrag(mx, my);
		}

		pose.popMatrix();
	}

	private void drawWheel(GuiGraphics ctx, int cx, int cy, int radius, float value) {
		int step = 2;
		for (int dy = -radius; dy <= radius; dy += step) {
			for (int dx = -radius; dx <= radius; dx += step) {
				double dist = Math.sqrt((double) dx * dx + (double) dy * dy);
				if (dist > radius) continue;
				double angle = Math.atan2(dy, dx);
				float hue = (float) ((angle / (Math.PI * 2)) + 0.5);
				float sat = (float) Math.min(1.0, dist / radius);
				int rgb = Theme.hsvToRgb(hue, sat, value);
				ctx.fill(cx + dx, cy + dy, cx + dx + step, cy + dy + step, 0xFF000000 | rgb);
			}
		}
	}

	private void drawWheelMarker(GuiGraphics ctx, float hue, float sat) {
		double angle = (hue - 0.5) * (Math.PI * 2);
		int mkx = wheelCx + (int) Math.round(Math.cos(angle) * sat * WHEEL_RADIUS);
		int mky = wheelCy + (int) Math.round(Math.sin(angle) * sat * WHEEL_RADIUS);
		fillDot(ctx, mkx, mky, 4, 0xFFFFFFFF);
		fillDot(ctx, mkx, mky, 3, Theme.BRAND);
	}

	private void drawBrightnessTrack(GuiGraphics ctx, int x, int y, int w, int h, float hue, float sat) {
		int step = 2;
		for (int dx = 0; dx < w; dx += step) {
			float t = (float) dx / w;
			int rgb = Theme.hsvToRgb(hue, sat, t);
			ctx.fill(x + dx, y, x + dx + step, y + h, 0xFF000000 | rgb);
		}
	}

	private void fillDot(GuiGraphics ctx, int cx, int cy, int r, int color) {
		for (int dy = -r; dy < r; dy++) {
			double h = dy + 0.5;
			int dx = (int) Math.round(Math.sqrt(r * r - h * h));
			if (dx > 0) ctx.fill(cx - dx, cy + dy, cx + dx, cy + dy + 1, color);
		}
	}

	private void applyDrag(int mx, int my) {
		switch (dragTarget) {
			case 1 -> seekWheel(mx, my);
			case 2 -> seekBrightness(mx);
			case 3 -> seekChannel(mx, 0);
			case 4 -> seekChannel(mx, 1);
			case 5 -> seekChannel(mx, 2);
			case 6 -> seekOpacity(mx);
			default -> {}
		}
	}

	private void seekWheel(int mx, int my) {
		Config config = GamblerPlusClient.CONFIG;
		float v = Theme.rgbToHsv(config.themeR(), config.themeG(), config.themeB())[2];
		int dx = mx - wheelCx;
		int dy = my - wheelCy;
		double dist = Math.min(Math.sqrt((double) dx * dx + (double) dy * dy), WHEEL_RADIUS);
		double angle = Math.atan2(dy, dx);
		float hue = (float) ((angle / (Math.PI * 2)) + 0.5);
		float sat = (float) (dist / WHEEL_RADIUS);
		int rgb = Theme.hsvToRgb(hue, sat, v);
		config.setTheme((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
	}

	private void seekBrightness(int mx) {
		Config config = GamblerPlusClient.CONFIG;
		float[] hsv = Theme.rgbToHsv(config.themeR(), config.themeG(), config.themeB());
		int leftX = brightnessRect[0] + 4;
		int rightX = brightnessRect[2] - 4;
		int w = Math.max(1, rightX - leftX);
		float pos = (float) (mx - leftX) / w;
		if (pos < 0f) pos = 0f;
		if (pos > 1f) pos = 1f;
		int rgb = Theme.hsvToRgb(hsv[0], hsv[1], pos);
		config.setTheme((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
	}

	private void seekChannel(int mx, int ch) {
		Config config = GamblerPlusClient.CONFIG;
		int[] r = channelRects[ch];
		if (r == null) return;
		int leftX = r[0] + 4;
		int rightX = r[2] - 4;
		int w = Math.max(1, rightX - leftX);
		float pos = (float) (mx - leftX) / w;
		if (pos < 0f) pos = 0f;
		if (pos > 1f) pos = 1f;
		int v = Math.round(pos * 255f);
		int nr = config.themeR(), ng = config.themeG(), nb = config.themeB();
		if (ch == 0) nr = v;
		else if (ch == 1) ng = v;
		else nb = v;
		config.setTheme(nr, ng, nb);
	}

	private void seekOpacity(int mx) {
		Config config = GamblerPlusClient.CONFIG;
		int leftX = opacityRect[0] + 4;
		int rightX = opacityRect[2] - 4;
		int w = Math.max(1, rightX - leftX);
		float pos = (float) (mx - leftX) / w;
		if (pos < 0f) pos = 0f;
		if (pos > 1f) pos = 1f;
		int a = 20 + Math.round(pos * (255 - 20));
		config.setGuiAlpha(a);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		int mx = Math.round((float) event.x() / uiScale);
		int my = Math.round((float) event.y() / uiScale);
		if (hit(closeRect, mx, my)) { onClose(); return true; }
		for (int i = 0; i < presetRects.size(); i++) {
			if (hit(presetRects.get(i), mx, my)) {
				GamblerPlusClient.CONFIG.setTheme(PRESETS[i][0], PRESETS[i][1], PRESETS[i][2]);
				return true;
			}
		}
		if (hit(wheelRect, mx, my)) {
			int dx = mx - wheelCx, dy = my - wheelCy;
			if (dx * dx + dy * dy <= WHEEL_RADIUS * WHEEL_RADIUS) {
				dragTarget = 1;
				seekWheel(mx, my);
				return true;
			}
		}
		if (hit(brightnessRect, mx, my)) {
			dragTarget = 2;
			seekBrightness(mx);
			return true;
		}
		for (int i = 0; i < 3; i++) {
			if (hit(channelRects[i], mx, my)) {
				dragTarget = 3 + i;
				seekChannel(mx, i);
				return true;
			}
		}
		if (hit(opacityRect, mx, my)) {
			dragTarget = 6;
			seekOpacity(mx);
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	private static boolean hit(int[] r, int mx, int my) {
		return r != null && mx >= r[0] && mx < r[2] && my >= r[1] && my < r[3];
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.key() == GLFW.GLFW_KEY_ESCAPE) { onClose(); return true; }
		return super.keyPressed(event);
	}

	@Override public void onClose() {
		Minecraft.getInstance().setScreenAndShow(new TrackerScreen(GamblerPlusClient.CONFIG, GamblerPlusClient.STATS));
	}
}
