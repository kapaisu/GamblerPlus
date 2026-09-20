package dev.mishka.gamblerplus.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class PlayerAllTimeScreen extends Screen {
	private static final int PANEL_W = 500;
	private static final int PANEL_H = 340;
	private static final int ROW_H = 12;
	private static final long ANIM_MS = 220L;

	private static final DateTimeFormatter TIME_FMT =
			DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());
	private static final DateTimeFormatter STAMP_FMT =
			DateTimeFormatter.ofPattern("MMM d HH:mm").withZone(ZoneId.systemDefault());

	private final String player;
	private long openedAtMs;
	private int px, py;
	private float uiScale = 1f;
	private int scroll = 0;

	private int[] closeRect;
	private int listX, listTop, listBot, listW;
	private final List<int[]> removeRects = new ArrayList<>();
	private final List<PaymentEvent> rowRefs = new ArrayList<>();

	public PlayerAllTimeScreen(String player) {
		super(Component.literal(player));
		this.player = player;
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
		float t = Math.min(1f, (System.currentTimeMillis() - openedAtMs) / (float) ANIM_MS);
		float e = Theme.easeOutCubic(t);
		ctx.fill(0, 0, width, height, (int) (0xB0 * e) << 24);

		int mx = Math.round(mxRaw / uiScale);
		int my = Math.round(myRaw / uiScale);

		var pose = ctx.pose();
		pose.pushPose();
		pose.translate(0f, (1f - e) * -14f, 0f);
		pose.scale(uiScale, uiScale, 1f);

		Widgets.panel(ctx, px, py, px + PANEL_W, py + PANEL_H);
		Widgets.header(ctx, font, px, py, PANEL_W, player);

		int cw = 62, ch = 14;
		int cx = px + PANEL_W - cw - 8, cy = py + 5;
		Widgets.flatButton(ctx, font, "close", cx, cy, cw, ch, mx, my, Theme.BRAND);
		closeRect = new int[]{cx, cy, cx + cw, cy + ch};

		List<PaymentEvent> all = GamblerPlusClient.ALLTIME.snapshot();
		List<PaymentEvent> mine = new ArrayList<>();
		for (PaymentEvent p : all) if (p.player().equals(player)) mine.add(p);
		mine.sort(Comparator.comparingLong(PaymentEvent::timestampMs).reversed());

		if (mine.isEmpty()) {
			ctx.drawString(font, "no payments recorded with " + player, px + 14, py + 40, Theme.TEXT_DIM, false);
			pose.popPose();
			return;
		}

		long in = 0, out = 0;
		int wins = 0, losses = 0;
		for (PaymentEvent p : mine) {
			if (p.incoming()) { in += p.amount(); wins++; }
			else { out += p.amount(); losses++; }
		}
		long net = in - out;

		int metaY = py + 32;
		String meta = mine.size() + " payments  -  first " + STAMP_FMT.format(Instant.ofEpochMilli(mine.get(mine.size() - 1).timestampMs()))
				+ "  -  last " + STAMP_FMT.format(Instant.ofEpochMilli(mine.get(0).timestampMs()));
		ctx.drawString(font, meta, px + 14, metaY, Theme.TEXT_MUTED, false);

		int totalsY = metaY + 12;
		String netStr = AmountFormat.signed(net);
		ctx.drawString(font, "net", px + 14, totalsY, Theme.TEXT_DIM, false);
		ctx.drawString(font, netStr, px + 32, totalsY, Theme.color(net), false);
		int cursor = px + 32 + font.width(netStr) + 12;
		ctx.drawString(font, "in", cursor, totalsY, Theme.TEXT_DIM, false);
		ctx.drawString(font, AmountFormat.pretty(in), cursor + 12, totalsY, Theme.GAIN, false);
		cursor += 12 + font.width(AmountFormat.pretty(in)) + 12;
		ctx.drawString(font, "out", cursor, totalsY, Theme.TEXT_DIM, false);
		ctx.drawString(font, AmountFormat.pretty(out), cursor + 18, totalsY, Theme.LOSS, false);
		cursor += 18 + font.width(AmountFormat.pretty(out)) + 12;
		String recStr = wins + "W / " + losses + "L";
		ctx.drawString(font, recStr, cursor, totalsY, Theme.TEXT, false);

		ctx.fill(px + 14, totalsY + 12, px + PANEL_W - 14, totalsY + 13, Theme.DIVIDER);

		int graphY = totalsY + 20;
		int graphH = 100;
		long started = mine.get(mine.size() - 1).timestampMs();
		long now = System.currentTimeMillis();
		NetGraph.draw(ctx, font, px + 14, graphY, PANEL_W - 28, graphH, mine, started, now, mx, my, GamblerPlusClient.CONFIG.graphStyle());

		int listHeaderY = graphY + graphH + 10;
		ctx.drawString(font, "PAYMENTS", px + 20, listHeaderY, Theme.TEXT_DIM, false);

		listX = px + 8;
		listW = PANEL_W - 16;
		listTop = listHeaderY + 12;
		listBot = py + PANEL_H - 22;
		ctx.fill(listX, listTop, listX + listW, listBot, Theme.SURFACE_ALT);
		outline(ctx, listX, listTop, listX + listW, listBot, Theme.PANEL_LINE);

		removeRects.clear();
		rowRefs.clear();

		int rows = Math.max(1, (listBot - listTop - 4) / ROW_H);
		int maxScroll = Math.max(0, mine.size() - rows);
		if (scroll > maxScroll) scroll = maxScroll;
		if (scroll < 0) scroll = 0;

		ctx.enableScissor(listX + 1, listTop + 1, listX + listW - 1, listBot - 1);
		int drawY = listTop + 4;
		for (int i = scroll; i < mine.size(); i++) {
			if (drawY + ROW_H > listBot) break;
			PaymentEvent p = mine.get(i);
			String time = TIME_FMT.format(Instant.ofEpochMilli(p.timestampMs()));
			String dir = p.incoming() ? "IN " : "OUT";
			int dirColor = p.incoming() ? Theme.GAIN : Theme.LOSS;
			String amt = (p.incoming() ? "+" : "-") + AmountFormat.pretty(p.amount());
			int aw = font.width(amt);
			ctx.drawString(font, time, listX + 8, drawY, Theme.TEXT_DIM, false);
			ctx.drawString(font, dir, listX + 76, drawY, dirColor, false);
			int xBtnX = listX + listW - 16;
			int amtX = xBtnX - 8 - aw;
			ctx.drawString(font, amt, amtX, drawY, dirColor, false);
			String mult = Multiplier.labelFor(mine, i);
			if (mult != null) {
				int mw = font.width(mult);
				ctx.drawString(font, mult, amtX - mw - 4, drawY, Theme.WARN, false);
			}
			boolean xHover = mx >= xBtnX - 2 && mx < xBtnX + 10 && my >= drawY - 1 && my < drawY + ROW_H - 1;
			ctx.drawString(font, "x", xBtnX, drawY, xHover ? Theme.LOSS : Theme.TEXT_DIM, false);
			removeRects.add(new int[]{xBtnX - 2, drawY - 1, xBtnX + 10, drawY + ROW_H - 1});
			rowRefs.add(p);
			drawY += ROW_H;
		}
		ctx.disableScissor();

		if (mine.size() > rows) {
			int sbX = listX + listW - 4;
			int sbTop = listTop + 2;
			int sbLen = listBot - listTop - 4;
			int thumb = Math.max(10, sbLen * rows / mine.size());
			int off = (sbLen - thumb) * scroll / Math.max(1, mine.size() - rows);
			ctx.fill(sbX, sbTop, sbX + 2, sbTop + sbLen, Theme.PANEL_LINE);
			ctx.fill(sbX, sbTop + off, sbX + 2, sbTop + off + thumb, Theme.TEXT_MUTED);
		}

		pose.popPose();
	}

	@Override
	public boolean mouseClicked(double _mx0, double _my0, int _btn) {
		int mx = Math.round((float) _mx0 / uiScale);
		int my = Math.round((float) _my0 / uiScale);
		if (hit(closeRect, mx, my)) { onClose(); return true; }
		for (int i = 0; i < removeRects.size(); i++) {
			if (hit(removeRects.get(i), mx, my)) {
				PaymentEvent p = rowRefs.get(i);
				boolean fromSession = GamblerPlusClient.STATS.remove(p);
				if (!fromSession) GamblerPlusClient.STATS.reverseAllTime(p);
				GamblerPlusClient.ALLTIME.removeOne(p);
				GamblerPlusClient.CONFIG.save();
				return true;
			}
		}
		return super.mouseClicked(_mx0, _my0, _btn);
	}

	@Override
	public boolean mouseScrolled(double mxD, double myD, double h, double v) {
		if (v < 0) scroll++;
		else scroll--;
		if (scroll < 0) scroll = 0;
		return true;
	}

	@Override
	public boolean keyPressed(int keyCode, int _sc, int _md) {
		if (keyCode == GLFW.GLFW_KEY_ESCAPE) { onClose(); return true; }
		return super.keyPressed(keyCode, _sc, _md);
	}

	private static boolean hit(int[] r, int mx, int my) {
		return r != null && mx >= r[0] && mx < r[2] && my >= r[1] && my < r[3];
	}

	@Override public void onClose() {
		Minecraft.getInstance().setScreen(new AllTimeScreen());
	}

	private void outline(GuiGraphics ctx, int x1, int y1, int x2, int y2, int c) {
		ctx.fill(x1, y1, x2, y1 + 1, c);
		ctx.fill(x1, y2 - 1, x2, y2, c);
		ctx.fill(x1, y1, x1 + 1, y2, c);
		ctx.fill(x2 - 1, y1, x2, y2, c);
	}
}
