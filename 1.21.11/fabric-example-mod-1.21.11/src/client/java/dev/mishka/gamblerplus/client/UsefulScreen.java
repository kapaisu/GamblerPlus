package dev.mishka.gamblerplus.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class UsefulScreen extends Screen {
	private static final int PANEL_W = 320;
	private static final int PANEL_H = 250;
	private static final long ANIM_MS = 200L;

	private int px, py;
	private float uiScale = 1f;
	private long openedAtMs;

	private int[] auctionRect;
	private int[] timerRect;
	private int[] textRect;
	private int[] imagesRect;
	private int[] editRect;
	private int[] closeRect;

	public UsefulScreen() {
		super(Minecraft.getInstance(), Minecraft.getInstance().font, Component.literal("Useful"));
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
		float t = Math.min(1f, (System.currentTimeMillis() - openedAtMs) / (float) ANIM_MS);
		float e = Theme.easeOutCubic(t);
		ctx.fill(0, 0, width, height, (int) (0xB0 * e) << 24);

		int mx = Math.round(mxRaw / uiScale);
		int my = Math.round(myRaw / uiScale);

		var pose = ctx.pose();
		pose.pushMatrix();
		pose.translate(0f, (1f - e) * -14f);
		pose.scale(uiScale, uiScale);

		Widgets.panel(ctx, px, py, px + PANEL_W, py + PANEL_H);
		Widgets.header(ctx, font, px, py, PANEL_W, "Useful");

		int cw = 62, ch = 14;
		int cx = px + PANEL_W - cw - 8, cy = py + 5;
		Widgets.flatButton(ctx, font, "close", cx, cy, cw, ch, mx, my, Theme.BRAND);
		closeRect = new int[]{cx, cy, cx + cw, cy + ch};

		int contentTop = py + 36;
		int btnW = PANEL_W - 60;
		int btnH = 28;
		int gap = 10;

		int bx = px + 30;
		int by = contentTop;
		Widgets.button(ctx, font, "auction", bx, by, btnW, btnH, mx, my, Theme.BRAND, 1f);
		auctionRect = new int[]{bx, by, bx + btnW, by + btnH};
		by += btnH + gap;

		Widgets.button(ctx, font, "timer", bx, by, btnW, btnH, mx, my, Theme.BRAND, 1f);
		timerRect = new int[]{bx, by, bx + btnW, by + btnH};
		by += btnH + gap;

		Widgets.button(ctx, font, "text on gui", bx, by, btnW, btnH, mx, my, Theme.BRAND, 1f);
		textRect = new int[]{bx, by, bx + btnW, by + btnH};
		by += btnH + gap;

		Widgets.button(ctx, font, "images", bx, by, btnW, btnH, mx, my, Theme.BRAND, 1f);
		imagesRect = new int[]{bx, by, bx + btnW, by + btnH};

		int eW = 130, eH = 18;
		int ex = px + (PANEL_W - eW) / 2;
		int ey = py + PANEL_H - eH - 12;
		HudLayout layout = GamblerPlusClient.CONFIG.hudLayout();
		String label = layout.editing ? "exit edit hud" : "edit hud";
		Widgets.button(ctx, font, label, ex, ey, eW, eH, mx, my, layout.editing ? Theme.LOSS : Theme.BRAND, 1f);
		editRect = new int[]{ex, ey, ex + eW, ey + eH};

		pose.popMatrix();
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		int mx = Math.round((float) event.x() / uiScale);
		int my = Math.round((float) event.y() / uiScale);
		Minecraft mc = Minecraft.getInstance();
		if (hit(closeRect, mx, my)) { onClose(); return true; }
		if (hit(auctionRect, mx, my)) {
			mc.setScreenAndShow(new AuctionScreen());
			return true;
		}
		if (hit(timerRect, mx, my)) {
			mc.setScreenAndShow(new TimerScreen());
			return true;
		}
		if (hit(textRect, mx, my)) {
			mc.setScreenAndShow(new TextScreen());
			return true;
		}
		if (hit(imagesRect, mx, my)) {
			mc.setScreenAndShow(new ImagesScreen());
			return true;
		}
		if (hit(editRect, mx, my)) {
			HudLayout layout = GamblerPlusClient.CONFIG.hudLayout();
			if (layout.editing) {
				layout.editing = false;
				GamblerPlusClient.CONFIG.save();
			} else {
				mc.setScreenAndShow(new HudEditScreen());
			}
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	private static boolean hit(int[] r, int mx, int my) {
		return r != null && mx >= r[0] && mx < r[2] && my >= r[1] && my < r[3];
	}

	@Override public boolean keyPressed(KeyEvent event) {
		if (event.key() == GLFW.GLFW_KEY_ESCAPE) { onClose(); return true; }
		return super.keyPressed(event);
	}

	@Override public void onClose() {
		Minecraft.getInstance().setScreenAndShow(new TrackerScreen(GamblerPlusClient.CONFIG, GamblerPlusClient.STATS));
	}
}
