package dev.mishka.gamblerplus.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class TimerScreen extends Screen {
	private static final int PANEL_W = 320;
	private static final int PANEL_H = 180;

	private int px, py;
	private float uiScale = 1f;
	private long openedAtMs;

	private String timeText = "";
	private boolean focused = true;
	private String feedback = "";

	private int[] fieldRect;
	private int[] startRect;
	private int[] stopRect;
	private int[] closeRect;
	private int[] styleRect;

	public TimerScreen() {
		super(Component.literal("Timer"));
	}

	@Override protected void init() {
		int margin = 16;
		float sw = (float) (width  - margin) / PANEL_W;
		float sh = (float) (height - margin) / PANEL_H;
		uiScale = Math.min(1f, Math.min(sw, sh));
		int vw = Math.round(width  / uiScale);
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
		pose.pushPose();
		pose.translate(0f, (1f - e) * -14f, 0f);
		pose.scale(uiScale, uiScale, 1f);

		Widgets.panel(ctx, px, py, px + PANEL_W, py + PANEL_H);
		Widgets.header(ctx, font, px, py, PANEL_W, "Timer");

		int cw = 62, ch = 14;
		int cx = px + PANEL_W - cw - 8, cy = py + 5;
		Widgets.flatButton(ctx, font, "close", cx, cy, cw, ch, mx, my, Theme.BRAND);
		closeRect = new int[]{cx, cy, cx + cw, cy + ch};

		TimerState timer = GamblerPlusClient.TIMER;
		timer.tick();

		int contentX = px + 16;
		int contentY = py + 40;
		int fieldW = PANEL_W - 32;

		ctx.drawString(font, "duration (e.g. 30s, 5m, 1h)", contentX, contentY, Theme.TEXT_DIM, false);
		int fY = contentY + 12;
		int fH = 22;
		Widgets.textField(ctx, font, contentX, fY, fieldW, fH, timeText, focused, "duration");
		fieldRect = new int[]{contentX, fY, contentX + fieldW, fY + fH};

		int btnRow = fY + fH + 12;
		int btnW = (fieldW - 8) / 2;
		int btnH = 20;
		Widgets.button(ctx, font, timer.active() ? "running" : "start", contentX, btnRow, btnW, btnH, mx, my,
				timer.active() ? Theme.TEXT_DIM : Theme.GAIN, 1f);
		startRect = new int[]{contentX, btnRow, contentX + btnW, btnRow + btnH};
		Widgets.button(ctx, font, "stop", contentX + btnW + 8, btnRow, btnW, btnH, mx, my, Theme.LOSS, 1f);
		stopRect = new int[]{contentX + btnW + 8, btnRow, contentX + fieldW, btnRow + btnH};

		int stateY = btnRow + btnH + 12;
		if (timer.active()) {
			ctx.drawString(font, "remaining " + TimeParse.prettyDuration(timer.remainingMs()),
					contentX, stateY, Theme.BRAND, false);
		} else if (!feedback.isEmpty()) {
			ctx.drawString(font, feedback, contentX, stateY, Theme.LOSS, false);
		} else {
			ctx.drawString(font, "set a duration then start", contentX, stateY, Theme.TEXT_MUTED, false);
		}

		int styleY = py + PANEL_H - 24;
		boolean numeric = GamblerPlusClient.CONFIG.numericTimer();
		String styleLabel = "style: " + (numeric ? "numbers" : "circle");
		int stW = 130, stH = 14;
		Widgets.flatButton(ctx, font, styleLabel, contentX, styleY, stW, stH, mx, my, Theme.BRAND);
		styleRect = new int[]{contentX, styleY, contentX + stW, styleY + stH};

		pose.popPose();
	}

	@Override
	public boolean mouseClicked(double _mx0, double _my0, int _btn) {
		int mx = Math.round((float) _mx0 / uiScale);
		int my = Math.round((float) _my0 / uiScale);
		if (hit(closeRect, mx, my)) { onClose(); return true; }
		if (hit(styleRect, mx, my)) {
			GamblerPlusClient.CONFIG.toggleNumericTimer();
			return true;
		}
		focused = hit(fieldRect, mx, my);
		TimerState timer = GamblerPlusClient.TIMER;
		if (hit(startRect, mx, my)) {
			if (timer.active()) return true;
			long dur = TimeParse.parseMs(timeText);
			if (dur <= 0) { feedback = "invalid time"; return true; }
			timer.start(dur);
			feedback = "";
			return true;
		}
		if (hit(stopRect, mx, my)) {
			timer.stop();
			return true;
		}
		return super.mouseClicked(_mx0, _my0, _btn);
	}

	private static boolean hit(int[] r, int mx, int my) {
		return r != null && mx >= r[0] && mx < r[2] && my >= r[1] && my < r[3];
	}

	@Override
	public boolean keyPressed(int keyCode, int _sc, int _md) {
		int key = keyCode;
		if (key == GLFW.GLFW_KEY_ESCAPE) { onClose(); return true; }
		if (!focused) return super.keyPressed(keyCode, _sc, _md);
		if (key == GLFW.GLFW_KEY_BACKSPACE && !timeText.isEmpty()) {
			timeText = timeText.substring(0, timeText.length() - 1);
			return true;
		}
		if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
			focused = false;
			return true;
		}
		return super.keyPressed(keyCode, _sc, _md);
	}

	@Override
	public boolean charTyped(char _ch, int _mods) {
		if (!focused) return false;
		if (timeText.length() > 12) return true;
		int cp = (int) _ch;
		if (cp >= 32 && cp < 127) {
			char c = (char) cp;
			if (Character.isLetterOrDigit(c)) {
				timeText += c;
				return true;
			}
		}
		return false;
	}

	@Override public void onClose() {
		Minecraft.getInstance().setScreen(new UsefulScreen());
	}
}
