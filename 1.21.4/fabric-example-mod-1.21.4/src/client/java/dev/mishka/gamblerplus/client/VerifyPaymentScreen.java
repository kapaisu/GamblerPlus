package dev.mishka.gamblerplus.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class VerifyPaymentScreen extends Screen {
	private static final int PANEL_W = 320;
	private static final int PANEL_H = 168;
	private static final long ANIM_MS = 220L;

	private final String recipient;
	private final long amount;
	private final String rawCommand;
	private long openedAtMs;
	private boolean acted;
	private float uiScale = 1f;

	private int yesX, yesY, yesW, yesH;
	private int noX, noY, noW, noH;

	public VerifyPaymentScreen(String recipient, long amount, String rawCommand) {
		super(Component.literal("Confirm payment"));
		this.recipient = recipient;
		this.amount = amount;
		this.rawCommand = rawCommand;
	}

	@Override protected void init() {
		openedAtMs = System.currentTimeMillis();
		int margin = 16;
		float sw = (float) (width  - margin) / PANEL_W;
		float sh = (float) (height - margin) / PANEL_H;
		uiScale = Math.min(1f, Math.min(sw, sh));
	}
	@Override public boolean isPauseScreen() { return true; }
	@Override public boolean shouldCloseOnEsc() { return false; }

	@Override
	public void render(GuiGraphics ctx, int mxRaw, int myRaw, float delta) {
		long elapsed = System.currentTimeMillis() - openedAtMs;
		float t = Math.min(1f, elapsed / (float) ANIM_MS);
		float eased = Theme.easeOutCubic(t);
		int backdropA = (int) (0xE0 * eased);
		ctx.fill(0, 0, width, height, backdropA << 24);

		int mx = Math.round(mxRaw / uiScale);
		int my = Math.round(myRaw / uiScale);

		int virtualW = Math.round(width  / uiScale);
		int virtualH = Math.round(height / uiScale);
		int px = (virtualW  - PANEL_W) / 2;
		int py = (virtualH - PANEL_H) / 2;

		var pose = ctx.pose();
		pose.pushPose();
		pose.translate(0f, (1f - eased) * -18f, 0f);
		pose.scale(uiScale, uiScale, 1f);

		ctx.fill(px, py, px + PANEL_W, py + PANEL_H, Theme.BG);
		outline(ctx, px, py, px + PANEL_W, py + PANEL_H, Theme.PANEL_LINE);
		ctx.fill(px, py, px + PANEL_W, py + 2, Theme.WARN);

		String title = "confirm large payment";
		int tw = font.width(title);
		ctx.drawString(font, title, px + (PANEL_W - tw) / 2, py + 14, Theme.TEXT, false);

		int rowY = py + 40;
		labelValue(ctx, px + 24, rowY,     "to",     recipient,                    Theme.TEXT);
		labelValue(ctx, px + 24, rowY + 18, "amount", AmountFormat.pretty(amount), Theme.LOSS);

		String warn = "this is above your verify threshold.";
		int ww = font.width(warn);
		ctx.drawString(font, warn, px + (PANEL_W - ww) / 2, py + PANEL_H - 60, Theme.TEXT_MUTED, false);

		yesW = 92; yesH = 22;
		noW  = 92; noH  = 22;
		int gap = 14;
		int totalW = yesW + gap + noW;
		yesX = px + (PANEL_W - totalW) / 2;
		yesY = py + PANEL_H - 32;
		noX  = yesX + yesW + gap;
		noY  = yesY;

		drawButton(ctx, "confirm", yesX, yesY, yesW, yesH, mx, my, Theme.GAIN);
		drawButton(ctx, "cancel",  noX,  noY,  noW,  noH,  mx, my, Theme.LOSS);

		pose.popPose();
	}

	private void labelValue(GuiGraphics ctx, int x, int y, String label, String value, int color) {
		ctx.drawString(font, label, x, y, Theme.TEXT_DIM, false);
		ctx.drawString(font, value, x + 42, y, color, false);
	}

	private void drawButton(GuiGraphics ctx, String label, int x, int y, int w, int h, int mx, int my, int accent) {
		boolean hover = mx >= x && mx < x + w && my >= y && my < y + h;
		int bg = hover ? Theme.SURFACE_ALT : Theme.SURFACE;
		ctx.fill(x, y, x + w, y + h, bg);
		outline(ctx, x, y, x + w, y + h, hover ? accent : Theme.PANEL_LINE);
		int lw = font.width(label);
		ctx.drawString(font, label, x + (w - lw) / 2, y + (h - font.lineHeight) / 2, hover ? accent : Theme.TEXT, false);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		int mx = Math.round((float) mouseX / uiScale);
		int my = Math.round((float) mouseY / uiScale);
		if (mx >= yesX && mx < yesX + yesW && my >= yesY && my < yesY + yesH) { confirm(); return true; }
		if (mx >= noX  && mx < noX  + noW  && my >= noY  && my < noY  + noH ) { cancel();  return true; }
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		int k = keyCode;
		if (k == GLFW.GLFW_KEY_ENTER || k == GLFW.GLFW_KEY_KP_ENTER || k == GLFW.GLFW_KEY_Y) { confirm(); return true; }
		if (k == GLFW.GLFW_KEY_ESCAPE || k == GLFW.GLFW_KEY_N) { cancel(); return true; }
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	private void confirm() {
		if (acted) return;
		acted = true;
		PaymentIntercept.markConfirmed();
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null) mc.player.connection.sendCommand(rawCommand);
		mc.setScreen(null);
	}

	private void cancel() {
		if (acted) return;
		acted = true;
		Minecraft.getInstance().setScreen(null);
	}

	@Override
	public void onClose() {
		if (!acted) cancel();
	}

	private void outline(GuiGraphics ctx, int x1, int y1, int x2, int y2, int c) {
		ctx.fill(x1, y1, x2, y1 + 1, c);
		ctx.fill(x1, y2 - 1, x2, y2, c);
		ctx.fill(x1, y1, x1 + 1, y2, c);
		ctx.fill(x2 - 1, y1, x2, y2, c);
	}
}
