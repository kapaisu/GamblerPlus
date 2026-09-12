package dev.mishka.gamblerplus.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

import java.net.URI;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class TrackerScreen extends Screen {
	private static final ResourceLocation ICON =
			ResourceLocation.fromNamespaceAndPath("gamblerplus", "textures/gui/donut.png");

	private static final int PANEL_W = 500;
	private static final int PANEL_H = 300;
	private static final int SIDE_W  = 168;
	private static final int SIDE_GAP = 8;
	private static final int ROW_H   = 12;
	private static final int SESSION_ROW_H = 22;
	private static final int ICON_SIZE = 20;
	private static final long ANIM_MS = 220L;

	private static final DateTimeFormatter TIME_FMT =
			DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());
	private static final DateTimeFormatter DATE_FMT =
			DateTimeFormatter.ofPattern("MMM d HH:mm").withZone(ZoneId.systemDefault());

	private final Config config;
	private final Stats  stats;
	private final SessionManager sessions;

	private int px, py;
	private int sessionsX, verifyX;
	private boolean sidePanelsFit;
	private int scroll;
	private int sessionScroll;
	private long openedAtMs;
	private float uiScale = 1f;
	private int virtualW, virtualH;

	public TrackerScreen(Config config, Stats stats) {
		super(Component.literal("Gambler Plus"));
		this.config = config;
		this.stats  = stats;
		this.sessions = GamblerPlusClient.SESSIONS;
	}

	@Override
	protected void init() {
		int margin = 16;
		int needWithSides = PANEL_W + 2 * (SIDE_W + SIDE_GAP);
		float sw = (float) (width  - margin) / needWithSides;
		float sh = (float) (height - margin) / (PANEL_H + 20);
		uiScale = Math.min(1f, Math.min(sw, sh));
		if (uiScale <= 0f) uiScale = 0.25f;
		sidePanelsFit = true;

		virtualW = Math.round(width  / uiScale);
		virtualH = Math.round(height / uiScale);
		px = (virtualW  - PANEL_W) / 2;
		py = (virtualH - PANEL_H) / 2;
		sessionsX = px - SIDE_GAP - SIDE_W;
		verifyX   = px + PANEL_W + SIDE_GAP;
		openedAtMs = System.currentTimeMillis();
	}

	@Override public boolean isPauseScreen() { return false; }

	@Override
	public void render(GuiGraphics ctx, int mx, int my, float delta) {
		float elapsed = System.currentTimeMillis() - openedAtMs;

		float mainE     = Theme.easeOutBack(Theme.smoothStep(0f,   260f, elapsed));
		float sessionsE = Theme.easeOutBack(Theme.smoothStep(220f, 500f, elapsed));
		float verifyE   = Theme.easeOutBack(Theme.smoothStep(300f, 580f, elapsed));
		float backdropE = Theme.smoothStep(0f, 200f, elapsed);

		int backdropA = (int) (0xB0 * backdropE);
		ctx.fill(0, 0, width, height, backdropA << 24);

		int lmx = Math.round(mx / uiScale);
		int lmy = Math.round(my / uiScale);

		if (draggingThreshold) {
			long window = Minecraft.getInstance().getWindow().handle();
			boolean down = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
			int[] track = hitboxes[HitId.THRESHOLD_TRACK.ordinal()];
			if (down && track != null) seekThreshold(lmx, track);
			else draggingThreshold = false;
		}

		var pose = ctx.pose();
		pose.pushMatrix();
		pose.scale(uiScale, uiScale);

		clearHitboxes();

		pose.pushMatrix();
		pose.translate(0f, (1f - mainE) * -32f);
		ctx.fill(px, py, px + PANEL_W, py + PANEL_H, Theme.BG);
		outline(ctx, px, py, px + PANEL_W, py + PANEL_H, Theme.PANEL_LINE);
		drawHeader(ctx);
		drawSummary(ctx, lmx, lmy);
		drawLog(ctx, lmx, lmy);
		drawFooter(ctx, lmx, lmy);
		drawDiscord(ctx, lmx, lmy, py + PANEL_H);
		drawArrowGameToggle(ctx, lmx, lmy, py + PANEL_H);
		String credits = "made by q3c on dc";
		int cw = font.width(credits);
		ctx.drawString(font, credits, px + (PANEL_W - cw) / 2, py + PANEL_H + 6, Theme.TEXT_DIM, false);
		pose.popMatrix();

		if (sidePanelsFit) {
			pose.pushMatrix();
			pose.translate(0f, (1f - sessionsE) * -40f);
			drawSessionsPanel(ctx, lmx, lmy);
			pose.popMatrix();

			pose.pushMatrix();
			pose.translate(0f, (1f - verifyE) * -40f);
			drawVerifyPanel(ctx, lmx, lmy);
			pose.popMatrix();
		}

		pose.popMatrix();
	}

	private void drawHeader(GuiGraphics ctx) {
		int hx = px, hy = py;
		int hh = 28;
		ctx.fill(hx, hy, hx + PANEL_W, hy + hh, Theme.SURFACE);
		ctx.fill(hx, hy + hh, hx + PANEL_W, hy + hh + 1, Theme.PANEL_LINE);

		int iconX = hx + 8;
		int iconY = hy + (hh - ICON_SIZE) / 2;
		ctx.blit(RenderPipelines.GUI_TEXTURED, ICON,
				iconX, iconY, 0f, 0f,
				ICON_SIZE, ICON_SIZE, 500, 500, 500, 500, 0xFFFFFFFF);

		int textX = iconX + ICON_SIZE + 8;
		int textY = hy + (hh - font.lineHeight) / 2;
		ctx.drawString(font, "Gambler",     textX, textY, Theme.TEXT, false);
		int nameW = font.width("Gambler ");
		ctx.drawString(font, "Plus", textX + nameW, textY, Theme.BRAND, false);

		String modeTag = config.gamblingMode() ? "TRACKING" : "PAUSED";
		int mw = font.width(modeTag);
		int rx = hx + PANEL_W - mw - 12;
		ctx.drawString(font, modeTag, rx, textY, config.gamblingMode() ? Theme.GAIN : Theme.TEXT_DIM, false);
	}

	private void drawArrowGameToggle(GuiGraphics ctx, int mx, int my, int belowPanelY) {
		int y = belowPanelY + 4;
		int x = px;
		toggle(ctx, "arrow game support", config.arrowGameSupport(), x, y, mx, my, HitId.ARROW_GAME);
	}

	private void drawDiscord(GuiGraphics ctx, int mx, int my, int belowPanelY) {
		int bw = 58, bh = 14;
		int bx = px + PANEL_W - bw;
		int by = belowPanelY + 4;
		boolean hover = mx >= bx && mx < bx + bw && my >= by && my < by + bh;
		int fill = hover ? DISCORD_BLURPLE : Theme.lerpColor(DISCORD_BLURPLE, Theme.BG, 0.55f);
		Theme.roundPanel(ctx, bx, by, bx + bw, by + bh, 4, fill, DISCORD_BLURPLE);
		String label = "discord";
		int lw = font.width(label);
		ctx.drawString(font, label, bx + (bw - lw) / 2, by + (bh - font.lineHeight) / 2 + 1, hover ? Theme.TEXT : 0xFFDDE1FF, false);
		hitboxes[HitId.OPEN_DISCORD.ordinal()] = new int[]{bx, by, bx + bw, by + bh};
	}

	private void drawSummary(GuiGraphics ctx, int mx, int my) {
		int sx = px + 14, sy = py + 40;
		int colW = 152;

		ctx.drawString(font, "SESSION",  sx,          sy, Theme.TEXT_DIM, false);
		ctx.drawString(font, "ALL TIME", sx + colW,   sy, Theme.TEXT_DIM, false);
		ctx.drawString(font, "STREAKS",  sx + colW*2, sy, Theme.TEXT_DIM, false);

		long snet = stats.sessionNet();
		long anet = stats.allTimeNet();

		ctx.drawString(font, AmountFormat.signed(snet), sx,          sy + 12, Theme.color(snet), false);
		ctx.drawString(font, AmountFormat.signed(anet), sx + colW,   sy + 12, Theme.color(anet), false);
		ctx.drawString(font, stats.lossStreak() + "L / " + stats.winStreak() + "W",
				sx + colW*2, sy + 12,
				stats.lossStreak() >= config.streakThreshold() ? Theme.LOSS : Theme.TEXT, false);

		subLine(ctx, sx,          sy + 26, "in",     AmountFormat.pretty(stats.sessionIn()),  Theme.GAIN);
		subLine(ctx, sx,          sy + 38, "out",    AmountFormat.pretty(stats.sessionOut()), Theme.LOSS);
		subLine(ctx, sx,          sy + 50, "trades", stats.sessionInCount() + " + " + stats.sessionOutCount(), Theme.TEXT_MUTED);

		subLine(ctx, sx + colW,   sy + 26, "in",     AmountFormat.pretty(stats.allTimeIn()),  Theme.GAIN);
		subLine(ctx, sx + colW,   sy + 38, "out",    AmountFormat.pretty(stats.allTimeOut()), Theme.LOSS);
		subLine(ctx, sx + colW,   sy + 50, "trades", stats.allTimeInCount() + " + " + stats.allTimeOutCount(), Theme.TEXT_MUTED);

		subLine(ctx, sx + colW*2, sy + 26, "worst",   stats.longestLossStreak() + " losses", Theme.LOSS);
		subLine(ctx, sx + colW*2, sy + 38, "best",    stats.longestWinStreak() + " wins",    Theme.GAIN);
		drawTriggerControl(ctx, sx + colW*2, sy + 50, mx, my);

		ctx.fill(px + 14, sy + 66, px + PANEL_W - 14, sy + 67, Theme.DIVIDER);
	}

	private void subLine(GuiGraphics ctx, int x, int y, String label, String value, int valueColor) {
		ctx.drawString(font, label, x, y, Theme.TEXT_DIM, false);
		int lw = font.width(label);
		ctx.drawString(font, value, x + lw + 6, y, valueColor, false);
	}

	private void drawTriggerControl(GuiGraphics ctx, int x, int y, int mx, int my) {
		ctx.drawString(font, "trigger", x, y, Theme.TEXT_DIM, false);
		int cursor = x + font.width("trigger") + 6;
		int btnW = 10, btnH = 11;
		int btnY = y - 2;
		int threshold = config.streakThreshold();
		boolean canDown = threshold > 2;
		boolean canUp   = threshold < 20;
		miniButton(ctx, "-", cursor, btnY, btnW, btnH, mx, my, HitId.STREAK_DOWN, canDown);
		cursor += btnW + 4;
		String val = String.valueOf(threshold);
		int vw = font.width(val);
		ctx.drawString(font, val, cursor, y, Theme.TEXT, false);
		cursor += vw + 4;
		miniButton(ctx, "+", cursor, btnY, btnW, btnH, mx, my, HitId.STREAK_UP, canUp);
	}

	private void miniButton(GuiGraphics ctx, String label, int x, int y, int w, int h, int mx, int my, HitId id, boolean enabled) {
		boolean hover = enabled && mx >= x && mx < x + w && my >= y && my < y + h;
		ctx.fill(x, y, x + w, y + h, hover ? Theme.SURFACE_ALT : Theme.SURFACE);
		outline(ctx, x, y, x + w, y + h, hover ? Theme.BRAND : Theme.PANEL_LINE);
		int lw = font.width(label);
		int color = enabled ? (hover ? Theme.BRAND : Theme.TEXT) : Theme.TEXT_DIM;
		ctx.drawString(font, label, x + (w - lw) / 2, y + (h - font.lineHeight) / 2 + 1, color, false);
		hitboxes[id.ordinal()] = enabled ? new int[]{x, y, x + w, y + h} : null;
	}

	private void drawLog(GuiGraphics ctx, int mx, int my) {
		int lx = px + 14;
		int ly = py + 116;
		int lw = PANEL_W - 28;
		int lh = 108;

		ctx.drawString(font, "RECENT PAYMENTS", lx, ly, Theme.TEXT_DIM, false);
		int listTop = ly + 12;
		int listBot = listTop + lh;
		Theme.roundRect(ctx, lx, listTop, lx + lw, listBot, 4, Theme.SURFACE_ALT);
		Theme.roundOutline(ctx, lx, listTop, lx + lw, listBot, 4, Theme.PANEL_LINE);

		logRemoveHits.clear();
		logRemoveRefs.clear();

		List<PaymentEvent> log = stats.snapshot();
		if (log.isEmpty()) {
			String empty = config.gamblingMode()
					? "waiting for the next payment..."
					: "tracking paused - turn it on to record payments";
			int ew = font.width(empty);
			ctx.drawString(font, empty, lx + (lw - ew) / 2, listTop + lh/2 - 4, Theme.TEXT_DIM, false);
			return;
		}

		int rows = lh / ROW_H;
		int max  = Math.min(rows, log.size());
		int start = Math.max(0, Math.min(scroll, log.size() - max));
		ctx.enableScissor(lx + 1, listTop + 1, lx + lw - 1, listBot - 1);
		for (int i = 0; i < max; i++) {
			PaymentEvent e = log.get(start + i);
			int rowY = listTop + 2 + i * ROW_H;
			if ((i & 1) == 1) ctx.fill(lx + 1, rowY - 2, lx + lw - 1, rowY + ROW_H - 2, 0x08FFFFFF);

			String time = TIME_FMT.format(Instant.ofEpochMilli(e.timestampMs()));
			String dir  = e.incoming() ? "IN " : "OUT";
			String amt  = (e.incoming() ? "+" : "-") + AmountFormat.pretty(e.amount());
			int dirColor = e.incoming() ? Theme.GAIN : Theme.LOSS;

			ctx.drawString(font, time, lx + 8,   rowY, Theme.TEXT_DIM, false);
			ctx.drawString(font, dir,  lx + 60,  rowY, dirColor, false);
			ctx.drawString(font, e.player(), lx + 90, rowY, Theme.TEXT, false);
			int aw = font.width(amt);
			int amtX = lx + lw - 20 - aw;
			ctx.drawString(font, amt, amtX, rowY, dirColor, false);

			int xBtnX = lx + lw - 14;
			int xBtnY = rowY - 1;
			boolean xHover = mx >= xBtnX - 2 && mx < xBtnX + 10 && my >= xBtnY && my < xBtnY + ROW_H - 1;
			ctx.drawString(font, "x", xBtnX, rowY, xHover ? Theme.LOSS : Theme.TEXT_DIM, false);
			logRemoveHits.add(new int[]{xBtnX - 2, xBtnY, xBtnX + 10, xBtnY + ROW_H - 1});
			logRemoveRefs.add(e);
		}
		ctx.disableScissor();

		if (log.size() > rows) {
			int barX = lx + lw - 4;
			int barTop = listTop + 2;
			int barLen = lh - 4;
			int thumb = Math.max(12, barLen * rows / log.size());
			int off   = (barLen - thumb) * start / Math.max(1, log.size() - rows);
			ctx.fill(barX, barTop, barX + 2, barTop + barLen, Theme.PANEL_LINE);
			ctx.fill(barX, barTop + off, barX + 2, barTop + off + thumb, Theme.TEXT_MUTED);
		}
	}

	private void drawFooter(GuiGraphics ctx, int mx, int my) {
		int fyTop = py + PANEL_H - 50;
		ctx.fill(px, fyTop, px + PANEL_W, fyTop + 1, Theme.PANEL_LINE);

		int row2Y = fyTop + 8;
		int x2 = px + 14;
		x2 = pill(ctx, "display", config.hudDisplay().name().toLowerCase(), x2, row2Y, mx, my, HitId.DISPLAY);
		x2 = pill(ctx, "anchor",  anchorLabel(),                             x2 + 8, row2Y, mx, my, HitId.ANCHOR);

		String keyName = Keybinds.openUi.getTranslatedKeyMessage().getString();
		String hint = "[" + keyName + "] toggle window";
		int kw = font.width(hint);
		ctx.drawString(font, hint, px + PANEL_W - 14 - kw, row2Y + 2, Theme.TEXT_DIM, false);

		int row1Y = fyTop + 28;
		int x1 = px + 14;
		x1 = toggle(ctx, "tracking",  config.gamblingMode(), x1,     row1Y, mx, my, HitId.GAMBLING);
		x1 = toggle(ctx, "hud bar",   config.hudBar(),       x1 + 14, row1Y, mx, my, HitId.HUD);
		x1 = toggle(ctx, "toast",     config.showToast(),    x1 + 14, row1Y, mx, my, HitId.TOAST);

		int uW = 66, uH = 16;
		int uX = px + PANEL_W - 14 - uW;
		int uY = row1Y - 1;
		Widgets.flatButton(ctx, font, "useful", uX, uY, uW, uH, mx, my, Theme.BRAND);
		hitboxes[HitId.OPEN_USEFUL.ordinal()] = new int[]{uX, uY, uX + uW, uY + uH};
	}

	private void drawSessionsPanel(GuiGraphics ctx, int mx, int my) {
		int sx = sessionsX;
		int sy = py;
		int sw = SIDE_W;
		int sh = PANEL_H;

		ctx.fill(sx, sy, sx + sw, sy + sh, Theme.BG);
		outline(ctx, sx, sy, sx + sw, sy + sh, Theme.PANEL_LINE);
		ctx.fill(sx, sy, sx + sw, sy + 24, Theme.SURFACE);
		ctx.fill(sx, sy + 24, sx + sw, sy + 25, Theme.PANEL_LINE);
		ctx.drawString(font, "SESSIONS", sx + 12, sy + 9, Theme.TEXT, false);

		int contentY = sy + 32;
		Session cur = sessions.snapshotCurrent(stats);

		int cardX = sx + 10;
		int cardW = sw - 20;

		if (cur != null) {
			int cardH = 58;
			boolean cardHover = mx >= cardX && mx < cardX + cardW && my >= contentY && my < contentY + 44;
			ctx.fill(cardX, contentY, cardX + cardW, contentY + cardH, cardHover ? Theme.SURFACE_ALT : Theme.SURFACE);
			outline(ctx, cardX, contentY, cardX + cardW, contentY + cardH, cardHover ? Theme.GAIN : Theme.PANEL_LINE);
			ctx.fill(cardX, contentY, cardX + 2, contentY + cardH, Theme.GAIN);
			ctx.drawString(font, cur.label(), cardX + 8, contentY + 5, Theme.TEXT, false);
			String durationStr = "for " + formatDuration(System.currentTimeMillis() - cur.startedAt());
			ctx.drawString(font, durationStr, cardX + 8, contentY + 17, Theme.TEXT_DIM, false);
			String netStr = AmountFormat.signed(cur.net());
			ctx.drawString(font, netStr, cardX + 8, contentY + 30, Theme.color(cur.net()), false);
			String recStr = cur.inCount() + "W / " + cur.outCount() + "L";
			int rw = font.width(recStr);
			ctx.drawString(font, recStr, cardX + cardW - 8 - rw, contentY + 30, Theme.TEXT_MUTED, false);
			hitboxes[HitId.CURRENT_SESSION_DETAIL.ordinal()] = new int[]{cardX, contentY, cardX + cardW, contentY + 44};
			button(ctx, "end session", cardX, contentY + 44, cardW, 12, mx, my, HitId.END_SESSION, Theme.LOSS);
			contentY += cardH + 8;
		} else {
			int cardH = 44;
			ctx.fill(cardX, contentY, cardX + cardW, contentY + cardH, Theme.SURFACE);
			outline(ctx, cardX, contentY, cardX + cardW, contentY + cardH, Theme.PANEL_LINE);
			String msg = "no active session";
			int mw = font.width(msg);
			ctx.drawString(font, msg, cardX + (cardW - mw) / 2, contentY + 8, Theme.TEXT_DIM, false);
			button(ctx, "start session", cardX + 6, contentY + 24, cardW - 12, 14, mx, my, HitId.NEW_SESSION, Theme.GAIN);
			contentY += cardH + 8;
		}

		if (cur != null) {
			button(ctx, "new session", cardX, contentY, cardW, 14, mx, my, HitId.NEW_SESSION, Theme.BRAND);
			contentY += 20;
		}

		ctx.drawString(font, "PAST", sx + 12, contentY, Theme.TEXT_DIM, false);
		contentY += 12;

		int listTop = contentY;
		int listBot = sy + sh - 8;
		int listH = listBot - listTop;

		pastSessionHits.clear();
		pastSessionRefs.clear();
		pastDeleteHits.clear();
		pastDeleteRefs.clear();
		List<Session> past = sessions.past();
		if (past.isEmpty()) {
			String s = "nothing yet";
			int w = font.width(s);
			ctx.drawString(font, s, sx + (sw - w) / 2, listTop + 6, Theme.TEXT_DIM, false);
		} else {
			ctx.enableScissor(sx + 1, listTop, sx + sw - 1, listBot);
			int rows = listH / SESSION_ROW_H;
			int start = Math.max(0, Math.min(sessionScroll, past.size() - rows));
			int max = Math.min(rows + 1, past.size() - start);
			for (int i = 0; i < max; i++) {
				Session s = past.get(start + i);
				int rowY = listTop + i * SESSION_ROW_H;
				int delX = sx + sw - 16;
				int delY = rowY - 1;
				int delW = 10;
				int delH = 10;
				boolean deleteHover = mx >= delX && mx < delX + delW && my >= delY && my < delY + delH;
				boolean rowHover = !deleteHover && mx >= sx + 4 && mx < sx + sw - 4 && my >= rowY - 2 && my < rowY + SESSION_ROW_H - 2;
				if (rowHover) ctx.fill(sx + 4, rowY - 2, sx + sw - 4, rowY + SESSION_ROW_H - 2, 0x18FFFFFF);
				ctx.drawString(font, DATE_FMT.format(Instant.ofEpochMilli(s.startedAt())), sx + 10, rowY, Theme.TEXT_DIM, false);
				String net = AmountFormat.signed(s.net());
				int nw = font.width(net);
				ctx.drawString(font, net, sx + sw - 22 - nw, rowY, Theme.color(s.net()), false);
				String meta = s.inCount() + "W/" + s.outCount() + "L - " + formatDuration(s.durationMs());
				ctx.drawString(font, meta, sx + 10, rowY + 10, Theme.TEXT_MUTED, false);

				ctx.drawString(font, "x", delX + 2, delY + 1, deleteHover ? Theme.LOSS : Theme.TEXT_DIM, false);

				pastSessionHits.add(new int[]{sx + 4, rowY - 2, sx + sw - 4, rowY + SESSION_ROW_H - 2});
				pastSessionRefs.add(s);
				pastDeleteHits.add(new int[]{delX - 2, delY - 2, delX + delW + 2, delY + delH + 2});
				pastDeleteRefs.add(s.id());
			}
			ctx.disableScissor();

			if (past.size() > rows) {
				int barX = sx + sw - 3;
				int barTop = listTop;
				int barLen = listH;
				int thumb = Math.max(10, barLen * rows / past.size());
				int off   = (barLen - thumb) * start / Math.max(1, past.size() - rows);
				ctx.fill(barX, barTop + off, barX + 2, barTop + off + thumb, Theme.TEXT_MUTED);
			}
		}
	}

	private void drawVerifyPanel(GuiGraphics ctx, int mx, int my) {
		int vx = verifyX;
		int vy = py;
		int vw = SIDE_W;
		int vh = PANEL_H;

		ctx.fill(vx, vy, vx + vw, vy + vh, Theme.BG);
		outline(ctx, vx, vy, vx + vw, vy + vh, Theme.PANEL_LINE);
		ctx.fill(vx, vy, vx + vw, vy + 24, Theme.SURFACE);
		ctx.fill(vx, vy + 24, vx + vw, vy + 25, Theme.PANEL_LINE);
		ctx.drawString(font, "SETTINGS", vx + 12, vy + 9, Theme.TEXT, false);

		int fieldX = vx + 12;
		int fieldW = vw - 24;
		int contentY = vy + 32;

		ctx.drawString(font, "verify large payments", fieldX, contentY, Theme.TEXT_DIM, false);
		contentY += 12;
		toggle(ctx, "enabled", config.verifyLargePayments(), fieldX, contentY, mx, my, HitId.VERIFY_ENABLE);
		contentY += 18;

		int fieldH = 20;
		Theme.roundRect(ctx, fieldX, contentY, fieldX + fieldW, contentY + fieldH, 3, Theme.SURFACE);
		Theme.roundOutline(ctx, fieldX, contentY, fieldX + fieldW, contentY + fieldH, 3, Theme.PANEL_LINE);
		String amt = AmountFormat.pretty(config.largePaymentThreshold());
		int aw = font.width(amt);
		ctx.drawString(font, amt, fieldX + (fieldW - aw) / 2, contentY + (fieldH - font.lineHeight) / 2, Theme.TEXT, false);
		contentY += fieldH + 6;

		int trackH = 8;
		int trackY = contentY + 2;
		Theme.roundRect(ctx, fieldX, trackY, fieldX + fieldW, trackY + trackH, 4, Theme.SURFACE);
		Theme.roundOutline(ctx, fieldX, trackY, fieldX + fieldW, trackY + trackH, 4, Theme.PANEL_LINE);
		float sPos = config.thresholdSliderPos();
		int filledX = fieldX + Math.round(sPos * fieldW);
		Theme.roundRect(ctx, fieldX, trackY, filledX, trackY + trackH, 4, Theme.BRAND);
		int knobR = 5;
		int knobX = filledX;
		int knobY = trackY + trackH / 2;
		fillCircle(ctx, knobX, knobY, knobR, Theme.TEXT);
		fillCircle(ctx, knobX, knobY, knobR - 1, Theme.BRAND);
		hitboxes[HitId.THRESHOLD_TRACK.ordinal()] = new int[]{fieldX - 4, trackY - 6, fieldX + fieldW + 4, trackY + trackH + 6};

		String lo = "1K";
		String hi = "10B";
		ctx.drawString(font, lo, fieldX, trackY + trackH + 3, Theme.TEXT_DIM, false);
		int hiW = font.width(hi);
		ctx.drawString(font, hi, fieldX + fieldW - hiW, trackY + trackH + 3, Theme.TEXT_DIM, false);
		contentY += 26;

		ctx.fill(vx + 12, contentY, vx + vw - 12, contentY + 1, Theme.DIVIDER);
		contentY += 8;

		ctx.drawString(font, "rakeback", fieldX, contentY, Theme.TEXT_DIM, false);
		contentY += 12;
		toggle(ctx, "enabled", config.rakebackEnabled(), fieldX, contentY, mx, my, HitId.RAKEBACK_ENABLE);
		contentY += 18;

		ctx.fill(fieldX, contentY, fieldX + fieldW, contentY + fieldH, Theme.SURFACE);
		outline(ctx, fieldX, contentY, fieldX + fieldW, contentY + fieldH, Theme.PANEL_LINE);
		String rateStr = formatPct(config.rakebackPct());
		int rw = font.width(rateStr);
		ctx.drawString(font, rateStr, fieldX + (fieldW - rw) / 2, contentY + (fieldH - font.lineHeight) / 2, Theme.TEXT, false);
		contentY += fieldH + 4;

		int rBtnH = 14;
		int rBtnW = (fieldW - 6) / 2;
		button(ctx, "-",  fieldX,              contentY, rBtnW, rBtnH, mx, my, HitId.RATE_DOWN, Theme.TEXT_MUTED);
		button(ctx, "+",  fieldX + rBtnW + 6,  contentY, rBtnW, rBtnH, mx, my, HitId.RATE_UP,   Theme.TEXT_MUTED);
		contentY += rBtnH + 4;

		button(ctx, "open list", fieldX, contentY, fieldW, 16, mx, my, HitId.OPEN_RAKEBACK, Theme.BRAND);
	}

	private String formatPct(double v) {
		if (v == Math.floor(v)) return String.format("%.0f%%", v);
		return String.format("%.1f%%", v);
	}

	private String anchorLabel() {
		return switch (config.hudAnchor()) {
			case TOP_LEFT   -> "left";
			case TOP_CENTER -> "center";
			case TOP_RIGHT  -> "right";
		};
	}

	private String formatDuration(long ms) {
		long s = ms / 1000L;
		long m = s / 60L;
		long h = m / 60L;
		if (h > 0) return h + "h " + (m % 60) + "m";
		if (m > 0) return m + "m " + (s % 60) + "s";
		return s + "s";
	}

	private int toggle(GuiGraphics ctx, String label, boolean on, int x, int y, int mx, int my, HitId id) {
		int trackW = 28;
		int trackH = 14;
		int r = trackH / 2;
		int lw = font.width(label);
		int totalW = trackW + 6 + lw;
		int rowH = trackH + 2;

		float pos = animPos(id, on);
		boolean hover = mx >= x && mx < x + totalW && my >= y && my < y + rowH;

		int trackColor = hover ? 0xFF262A32 : Theme.TOGGLE_BG;
		int knobColor  = Theme.lerpColor(Theme.TOGGLE_OFF, Theme.TOGGLE_ON, pos);

		fillPill(ctx, x, y, trackW, trackH, trackColor);
		int knobPad = 1;
		int knobR = r - knobPad;
		int knobCx = x + r + Math.round(pos * (trackW - trackH));
		int knobCy = y + r;
		fillCircle(ctx, knobCx, knobCy, knobR, knobColor);

		int labelY = y + (trackH - font.lineHeight) / 2 + 1;
		ctx.drawString(font, label, x + trackW + 6, labelY, on ? Theme.TEXT : Theme.TEXT_MUTED, false);
		hitboxes[id.ordinal()] = new int[]{x, y, x + totalW, y + rowH};
		return x + totalW;
	}

	private static final class ToggleAnim {
		boolean state;
		long changedAt;
		float posAtChange;
	}
	private final java.util.EnumMap<HitId, ToggleAnim> anims = new java.util.EnumMap<>(HitId.class);

	private float animPos(HitId id, boolean on) {
		long now = System.currentTimeMillis();
		ToggleAnim a = anims.get(id);
		if (a == null) {
			a = new ToggleAnim();
			a.state = on;
			a.posAtChange = on ? 1f : 0f;
			a.changedAt = now - 999_999L;
			anims.put(id, a);
		}
		float target = a.state ? 1f : 0f;
		float t = Math.min(1f, (now - a.changedAt) / 180f);
		float current = a.posAtChange + (target - a.posAtChange) * Theme.easeOutCubic(t);
		if (a.state != on) {
			a.posAtChange = current;
			a.state = on;
			a.changedAt = now;
			target = on ? 1f : 0f;
			t = 0f;
			current = a.posAtChange;
		}
		return current;
	}

	private void fillPill(GuiGraphics ctx, int x, int y, int w, int h, int color) {
		int r = h / 2;
		ctx.fill(x + r, y, x + w - r, y + h, color);
		fillCircle(ctx, x + r,     y + r, r, color);
		fillCircle(ctx, x + w - r, y + r, r, color);
	}

	private void fillCircle(GuiGraphics ctx, int cx, int cy, int r, int color) {
		for (int dy = -r; dy < r; dy++) {
			double h = dy + 0.5;
			int dx = (int) Math.round(Math.sqrt(r * r - h * h));
			if (dx > 0) ctx.fill(cx - dx, cy + dy, cx + dx, cy + dy + 1, color);
		}
	}

	private int pill(GuiGraphics ctx, String label, String value, int x, int y, int mx, int my, HitId id) {
		String composed = label + ": " + value;
		int lw = font.width(composed);
		int pw = lw + 14;
		int ph = 14;
		boolean hover = mx >= x && mx < x + pw && my >= y && my < y + ph;
		ctx.fill(x, y, x + pw, y + ph, hover ? 0xFF20232B : Theme.SURFACE);
		outline(ctx, x, y, x + pw, y + ph, Theme.PANEL_LINE);
		ctx.drawString(font, label + ":", x + 7, y + 3, Theme.TEXT_DIM, false);
		int labelW = font.width(label + ": ");
		ctx.drawString(font, value, x + 7 + labelW, y + 3, Theme.TEXT, false);
		hitboxes[id.ordinal()] = new int[]{x, y, x + pw, y + ph};
		return x + pw;
	}

	private void button(GuiGraphics ctx, String label, int x, int y, int w, int h, int mx, int my, HitId id, int accent) {
		boolean hover = mx >= x && mx < x + w && my >= y && my < y + h;
		ctx.fill(x, y, x + w, y + h, hover ? Theme.SURFACE_ALT : Theme.SURFACE);
		outline(ctx, x, y, x + w, y + h, hover ? accent : Theme.PANEL_LINE);
		int lw = font.width(label);
		ctx.drawString(font, label, x + (w - lw) / 2, y + (h - font.lineHeight) / 2, hover ? accent : Theme.TEXT, false);
		hitboxes[id.ordinal()] = new int[]{x, y, x + w, y + h};
	}

	private void outline(GuiGraphics ctx, int x1, int y1, int x2, int y2, int c) {
		ctx.fill(x1, y1, x2, y1 + 1, c);
		ctx.fill(x1, y2 - 1, x2, y2, c);
		ctx.fill(x1, y1, x1 + 1, y2, c);
		ctx.fill(x2 - 1, y1, x2, y2, c);
	}

	private enum HitId {
		GAMBLING, HUD, TOAST, DISPLAY, ANCHOR,
		NEW_SESSION, END_SESSION, CURRENT_SESSION_DETAIL,
		VERIFY_ENABLE, THRESHOLD_TRACK,
		STREAK_DOWN, STREAK_UP,
		RAKEBACK_ENABLE, RATE_DOWN, RATE_UP, OPEN_RAKEBACK,
		OPEN_USEFUL,
		OPEN_DISCORD,
		ARROW_GAME
	}
	private static final int DISCORD_BLURPLE = 0xFF5865F2;
	private static final String DISCORD_URL = "https://discord.gg/YnMQRpExwj";
	private final int[][] hitboxes = new int[HitId.values().length][];
	private final java.util.List<int[]> pastSessionHits = new java.util.ArrayList<>();
	private final java.util.List<Session> pastSessionRefs = new java.util.ArrayList<>();
	private final java.util.List<int[]> pastDeleteHits = new java.util.ArrayList<>();
	private final java.util.List<Long> pastDeleteRefs = new java.util.ArrayList<>();
	private final java.util.List<int[]> logRemoveHits = new java.util.ArrayList<>();
	private final java.util.List<PaymentEvent> logRemoveRefs = new java.util.ArrayList<>();
	private boolean draggingThreshold = false;

	private void clearHitboxes() {
		for (int i = 0; i < hitboxes.length; i++) hitboxes[i] = null;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		int mx = Math.round((float) event.x() / uiScale);
		int my = Math.round((float) event.y() / uiScale);
		for (int i = 0; i < logRemoveHits.size(); i++) {
			int[] h = logRemoveHits.get(i);
			if (mx >= h[0] && mx < h[2] && my >= h[1] && my < h[3]) {
				PaymentEvent p = logRemoveRefs.get(i);
				if (stats.remove(p)) {
					if (p.incoming()) sessions.onIncomingReversed(p.player(), p.amount());
					config.save();
				}
				return true;
			}
		}
		int[] track = hitboxes[HitId.THRESHOLD_TRACK.ordinal()];
		if (track != null && mx >= track[0] && mx < track[2] && my >= track[1] && my < track[3]) {
			draggingThreshold = true;
			seekThreshold(mx, track);
			return true;
		}
		for (int i = 0; i < hitboxes.length; i++) {
			if (i == HitId.THRESHOLD_TRACK.ordinal()) continue;
			int[] h = hitboxes[i];
			if (h == null) continue;
			if (mx >= h[0] && mx < h[2] && my >= h[1] && my < h[3]) {
				switch (HitId.values()[i]) {
					case GAMBLING       -> config.toggleGamblingMode();
					case HUD            -> config.toggleHudBar();
					case TOAST          -> config.toggleShowToast();
					case DISPLAY        -> config.cycleHudDisplay();
					case ANCHOR         -> config.cycleHudAnchor();
					case NEW_SESSION    -> sessions.startNew(stats);
					case END_SESSION    -> sessions.endCurrent(stats);
					case VERIFY_ENABLE  -> config.toggleVerify();
					case STREAK_DOWN    -> config.setStreakThreshold(config.streakThreshold() - 1);
					case STREAK_UP      -> config.setStreakThreshold(config.streakThreshold() + 1);
					case RAKEBACK_ENABLE -> config.toggleRakeback();
					case RATE_DOWN      -> config.stepRakebackPct(-1);
					case RATE_UP        -> config.stepRakebackPct(+1);
					case OPEN_RAKEBACK  -> Minecraft.getInstance().setScreenAndShow(new RakebackScreen(config, sessions));
					case OPEN_USEFUL    -> Minecraft.getInstance().setScreenAndShow(new UsefulScreen());
					case OPEN_DISCORD   -> Util.getPlatform().openUri(URI.create(DISCORD_URL));
					case ARROW_GAME     -> config.toggleArrowGameSupport();
					case CURRENT_SESSION_DETAIL -> Minecraft.getInstance().setScreenAndShow(
							new SessionDetailScreen(() -> sessions.snapshotCurrent(stats), true));
					default -> {}
				}
				return true;
			}
		}
		for (int i = 0; i < pastDeleteHits.size(); i++) {
			int[] h = pastDeleteHits.get(i);
			if (mx >= h[0] && mx < h[2] && my >= h[1] && my < h[3]) {
				sessions.deletePast(pastDeleteRefs.get(i));
				return true;
			}
		}
		for (int i = 0; i < pastSessionHits.size(); i++) {
			int[] h = pastSessionHits.get(i);
			if (mx >= h[0] && mx < h[2] && my >= h[1] && my < h[3]) {
				Session pick = pastSessionRefs.get(i);
				Minecraft.getInstance().setScreenAndShow(new SessionDetailScreen(() -> pick, false));
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	private void seekThreshold(int mx, int[] track) {
		int leftX = track[0] + 4;
		int rightX = track[2] - 4;
		int w = Math.max(1, rightX - leftX);
		float pos = (float) (mx - leftX) / w;
		if (pos < 0f) pos = 0f;
		if (pos > 1f) pos = 1f;
		config.setThresholdSliderPos(pos);
	}

	@Override
	public boolean mouseScrolled(double mxD, double myD, double h, double v) {
		int mx = Math.round((float) mxD / uiScale);
		int my = Math.round((float) myD / uiScale);
		int dir = v < 0 ? +1 : -1;
		if (sidePanelsFit && mx >= sessionsX && mx < sessionsX + SIDE_W) {
			sessionScroll = Math.max(0, sessionScroll + dir);
			return true;
		}
		scroll = Math.max(0, scroll + (v < 0 ? -1 : +1));
		return true;
	}
}
