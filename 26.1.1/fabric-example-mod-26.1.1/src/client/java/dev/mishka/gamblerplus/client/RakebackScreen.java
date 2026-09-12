package dev.mishka.gamblerplus.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class RakebackScreen extends Screen {
	private static final int PANEL_W = 460;
	private static final int PANEL_H = 280;
	private static final int ROW_H   = 18;
	private static final int MAX_ROWS = 10;
	private static final long ANIM_MS = 220L;

	private final Config config;
	private final SessionManager sessions;
	private long openedAtMs;
	private int scroll;

	private int px, py;
	private int listTop, listBot, listX, listW;
	private int closeX, closeY, closeW, closeH;
	private final List<int[]> paidHitboxes = new ArrayList<>();
	private final List<String> paidPlayers = new ArrayList<>();
	private float uiScale = 1f;

	public RakebackScreen(Config config, SessionManager sessions) {
		super(Minecraft.getInstance(), Minecraft.getInstance().font, Component.literal("Rakeback"));
		this.config = config;
		this.sessions = sessions;
	}

	@Override protected void init() {
		openedAtMs = System.currentTimeMillis();
		int margin = 16;
		float sw = (float) (width  - margin) / PANEL_W;
		float sh = (float) (height - margin) / PANEL_H;
		uiScale = Math.min(1f, Math.min(sw, sh));
		int virtualW = Math.round(width  / uiScale);
		int virtualH = Math.round(height / uiScale);
		px = (virtualW  - PANEL_W) / 2;
		py = (virtualH - PANEL_H) / 2;
	}

	@Override public boolean isPauseScreen() { return false; }

	@Override
	public void extractRenderState(GuiGraphicsExtractor ctx, int mxRaw, int myRaw, float delta) {
		long elapsed = System.currentTimeMillis() - openedAtMs;
		float t = Math.min(1f, elapsed / (float) ANIM_MS);
		float eased = Theme.easeOutCubic(t);
		int backdropA = (int) (0xB0 * eased);
		ctx.fill(0, 0, width, height, backdropA << 24);

		int mx = Math.round(mxRaw / uiScale);
		int my = Math.round(myRaw / uiScale);

		var pose = ctx.pose();
		pose.pushMatrix();
		pose.translate(0f, (1f - eased) * -14f);
		pose.scale(uiScale, uiScale);

		ctx.fill(px, py, px + PANEL_W, py + PANEL_H, Theme.BG);
		outline(ctx, px, py, px + PANEL_W, py + PANEL_H, Theme.PANEL_LINE);
		ctx.fill(px, py, px + PANEL_W, py + 24, Theme.SURFACE);
		ctx.fill(px, py + 24, px + PANEL_W, py + 25, Theme.PANEL_LINE);
		ctx.text(font, "rakeback owed", px + 14, py + 9, Theme.TEXT, false);

		String rateStr = "rate " + formatPct(config.rakebackPct());
		int rw = font.width(rateStr);
		ctx.text(font, rateStr, px + PANEL_W - rw - 14, py + 9, Theme.BRAND, false);

		int headerY = py + 34;
		int col1 = px + 16;
		int col2 = px + 190;
		int col3 = px + 290;
		int col4 = px + PANEL_W - 90;

		ctx.text(font, "PLAYER",   col1, headerY, Theme.TEXT_DIM, false);
		ctx.text(font, "RECEIVED", col2, headerY, Theme.TEXT_DIM, false);
		ctx.text(font, "OWED",     col3, headerY, Theme.TEXT_DIM, false);

		listX   = px + 8;
		listW   = PANEL_W - 16;
		listTop = headerY + 12;
		listBot = py + PANEL_H - 36;

		ctx.fill(listX, listTop, listX + listW, listBot, Theme.SURFACE_ALT);
		outline(ctx, listX, listTop, listX + listW, listBot, Theme.PANEL_LINE);

		paidHitboxes.clear();
		paidPlayers.clear();

		Map<String, Long> map = sessions.incomingByPlayer();
		List<Map.Entry<String, Long>> entries = new ArrayList<>(map.entrySet());
		entries.sort(Comparator.comparingLong(Map.Entry<String, Long>::getValue).reversed());

		long totalOwed = 0L;
		for (Map.Entry<String, Long> e : entries) {
			totalOwed += (long) (e.getValue() * config.rakebackPct() / 100.0);
		}

		if (entries.isEmpty()) {
			String empty = sessions.hasActive()
					? "no incoming payments yet in this session"
					: "no active session - start one from the tracker";
			int ew = font.width(empty);
			ctx.text(font, empty, listX + (listW - ew) / 2, listTop + (listBot - listTop) / 2 - 4, Theme.TEXT_DIM, false);
		} else {
			int available = listBot - listTop - 4;
			int rows = Math.min(MAX_ROWS, available / ROW_H);
			int start = Math.max(0, Math.min(scroll, entries.size() - rows));
			int max = Math.min(rows, entries.size() - start);
			ctx.enableScissor(listX + 1, listTop + 1, listX + listW - 1, listBot - 1);
			for (int i = 0; i < max; i++) {
				Map.Entry<String, Long> e = entries.get(start + i);
				int rowY = listTop + 4 + i * ROW_H;
				if ((i & 1) == 1) ctx.fill(listX + 1, rowY - 2, listX + listW - 1, rowY + ROW_H - 2, 0x08FFFFFF);
				long received = e.getValue();
				long owed = (long) (received * config.rakebackPct() / 100.0);
				ctx.text(font, e.getKey(),                 col1, rowY, Theme.TEXT, false);
				ctx.text(font, AmountFormat.pretty(received), col2, rowY, Theme.GAIN, false);
				ctx.text(font, AmountFormat.pretty(owed),     col3, rowY, Theme.LOSS, false);

				int btnW = 60, btnH = 14;
				int btnX = col4;
				int btnY = rowY - 3;
				boolean hover = mx >= btnX && mx < btnX + btnW && my >= btnY && my < btnY + btnH;
				ctx.fill(btnX, btnY, btnX + btnW, btnY + btnH, hover ? Theme.SURFACE_ALT : Theme.SURFACE);
				outline(ctx, btnX, btnY, btnX + btnW, btnY + btnH, hover ? Theme.GAIN : Theme.PANEL_LINE);
				int lw = font.width("paid");
				ctx.text(font, "paid", btnX + (btnW - lw) / 2, btnY + (btnH - font.lineHeight) / 2 + 1,
						hover ? Theme.GAIN : Theme.TEXT, false);
				paidHitboxes.add(new int[]{btnX, btnY, btnX + btnW, btnY + btnH});
				paidPlayers.add(e.getKey());
			}
			ctx.disableScissor();

			if (entries.size() > rows) {
				int barX = listX + listW - 4;
				int barTop = listTop + 2;
				int barLen = listBot - listTop - 4;
				int thumb = Math.max(12, barLen * rows / entries.size());
				int off = (barLen - thumb) * start / Math.max(1, entries.size() - rows);
				ctx.fill(barX, barTop, barX + 2, barTop + barLen, Theme.PANEL_LINE);
				ctx.fill(barX, barTop + off, barX + 2, barTop + off + thumb, Theme.TEXT_MUTED);
			}
		}

		String totalStr = "total owed  " + AmountFormat.pretty(totalOwed);
		ctx.text(font, totalStr, px + 14, py + PANEL_H - 22, entries.isEmpty() ? Theme.TEXT_DIM : Theme.LOSS, false);

		closeW = 78; closeH = 16;
		closeX = px + PANEL_W - closeW - 14;
		closeY = py + PANEL_H - 24;
		boolean chover = mx >= closeX && mx < closeX + closeW && my >= closeY && my < closeY + closeH;
		ctx.fill(closeX, closeY, closeX + closeW, closeY + closeH, chover ? Theme.SURFACE_ALT : Theme.SURFACE);
		outline(ctx, closeX, closeY, closeX + closeW, closeY + closeH, chover ? Theme.BRAND : Theme.PANEL_LINE);
		int cw = font.width("close");
		ctx.text(font, "close", closeX + (closeW - cw) / 2,
				closeY + (closeH - font.lineHeight) / 2 + 1, chover ? Theme.BRAND : Theme.TEXT, false);

		pose.popMatrix();
	}

	private String formatPct(double v) {
		if (v == Math.floor(v)) return String.format("%.0f%%", v);
		return String.format("%.1f%%", v);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		int mx = Math.round((float) event.x() / uiScale);
		int my = Math.round((float) event.y() / uiScale);
		if (mx >= closeX && mx < closeX + closeW && my >= closeY && my < closeY + closeH) {
			onClose();
			return true;
		}
		for (int i = 0; i < paidHitboxes.size(); i++) {
			int[] h = paidHitboxes.get(i);
			if (mx >= h[0] && mx < h[2] && my >= h[1] && my < h[3]) {
				sessions.markPaid(paidPlayers.get(i));
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double h, double v) {
		if (v < 0) scroll = scroll + 1;
		else if (v > 0) scroll = Math.max(0, scroll - 1);
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.key() == GLFW.GLFW_KEY_ESCAPE) { onClose(); return true; }
		return super.keyPressed(event);
	}

	@Override
	public void onClose() {
		Minecraft.getInstance().setScreenAndShow(new TrackerScreen(config, GamblerPlusClient.STATS));
	}

	private void outline(GuiGraphicsExtractor ctx, int x1, int y1, int x2, int y2, int c) {
		ctx.fill(x1, y1, x2, y1 + 1, c);
		ctx.fill(x1, y2 - 1, x2, y2, c);
		ctx.fill(x1, y1, x1 + 1, y2, c);
		ctx.fill(x2 - 1, y1, x2, y2, c);
	}
}
