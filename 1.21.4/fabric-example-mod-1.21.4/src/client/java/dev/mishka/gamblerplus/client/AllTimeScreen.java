package dev.mishka.gamblerplus.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AllTimeScreen extends Screen {
	public enum Sort {
		RECENT("most recent"),
		NET_HIGH("best net"),
		NET_LOW("worst net"),
		VOLUME("most played");
		final String label;
		Sort(String s) { this.label = s; }
	}

	private static final int PANEL_W = 500;
	private static final int PANEL_H = 320;
	private static final int ROW_H = 16;

	private int px, py;
	private float uiScale = 1f;
	private long openedAtMs;

	private String search = "";
	private boolean searchFocused = false;
	private Sort sort = Sort.RECENT;
	private int scroll = 0;
	private boolean resetArmed = false;
	private long resetArmedAtMs = 0L;

	private int[] closeRect;
	private int[] resetRect;
	private int[] searchRect;
	private int[] sortRect;
	private int listX, listTop, listBot, listW;
	private final List<int[]> rowRects = new ArrayList<>();
	private final List<String> rowPlayers = new ArrayList<>();

	private static final class PlayerAgg {
		String player;
		long net, in, out;
		int wins, losses;
		long lastTs;
	}

	public AllTimeScreen() {
		super(Component.literal("All time"));
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
		pose.pushPose();
		pose.translate(0f, (1f - e) * -14f, 0f);
		pose.scale(uiScale, uiScale, 1f);

		Widgets.panel(ctx, px, py, px + PANEL_W, py + PANEL_H);
		Widgets.header(ctx, font, px, py, PANEL_W, "All time");

		int cw = 62, ch = 14;
		int cx = px + PANEL_W - cw - 8, cy = py + 5;
		Widgets.flatButton(ctx, font, "close", cx, cy, cw, ch, mx, my, Theme.BRAND);
		closeRect = new int[]{cx, cy, cx + cw, cy + ch};

		if (resetArmed && System.currentTimeMillis() - resetArmedAtMs > 4000L) resetArmed = false;
		String resetLabel = resetArmed ? "confirm reset?" : "reset all time";
		int resetW = font.width(resetLabel) + 16;
		int resetX = cx - resetW - 8;
		Widgets.flatButton(ctx, font, resetLabel, resetX, cy, resetW, ch, mx, my, Theme.LOSS);
		resetRect = new int[]{resetX, cy, resetX + resetW, cy + ch};

		int barY = py + 32;
		int barX = px + 12;
		int searchW = 220;
		int searchH = 16;
		Widgets.textField(ctx, font, barX, barY, searchW, searchH, search, searchFocused, "search player");
		searchRect = new int[]{barX, barY, barX + searchW, barY + searchH};

		int sortX = barX + searchW + 8;
		int sortW = 130;
		Widgets.flatButton(ctx, font, "sort: " + sort.label, sortX, barY, sortW, searchH, mx, my, Theme.BRAND);
		sortRect = new int[]{sortX, barY, sortX + sortW, barY + searchH};

		List<PaymentEvent> all = GamblerPlusClient.ALLTIME.snapshot();
		Map<String, PlayerAgg> byPlayer = new LinkedHashMap<>();
		for (PaymentEvent p : all) {
			PlayerAgg agg = byPlayer.computeIfAbsent(p.player(), k -> {
				PlayerAgg a = new PlayerAgg();
				a.player = k;
				return a;
			});
			if (p.incoming()) { agg.in += p.amount(); agg.wins++; }
			else { agg.out += p.amount(); agg.losses++; }
			agg.net = agg.in - agg.out;
			if (p.timestampMs() > agg.lastTs) agg.lastTs = p.timestampMs();
		}

		int countX = sortX + sortW + 8;
		String countLabel = byPlayer.size() + " players";
		ctx.drawString(font, countLabel, countX, barY + (searchH - font.lineHeight) / 2 + 1, Theme.TEXT_DIM, false);

		listX = px + 8;
		listW = PANEL_W - 16;
		listTop = barY + searchH + 8;
		listBot = py + PANEL_H - 12;
		Theme.roundPanel(ctx, listX, listTop, listX + listW, listBot, 4, Theme.SURFACE_ALT, Theme.PANEL_LINE);

		String q = search.trim().toLowerCase();
		List<PlayerAgg> filtered = new ArrayList<>();
		for (PlayerAgg a : byPlayer.values()) {
			if (q.isEmpty() || a.player.toLowerCase().contains(q)) filtered.add(a);
		}
		switch (sort) {
			case RECENT   -> filtered.sort(Comparator.comparingLong((PlayerAgg a) -> a.lastTs).reversed());
			case NET_HIGH -> filtered.sort(Comparator.comparingLong((PlayerAgg a) -> a.net).reversed());
			case NET_LOW  -> filtered.sort(Comparator.comparingLong((PlayerAgg a) -> a.net));
			case VOLUME   -> filtered.sort(Comparator.comparingLong((PlayerAgg a) -> a.in + a.out).reversed());
		}

		rowRects.clear();
		rowPlayers.clear();

		if (filtered.isEmpty()) {
			String s = q.isEmpty() ? "no payments yet" : "no matches for '" + search + "'";
			int sw = font.width(s);
			ctx.drawString(font, s, listX + (listW - sw) / 2, listTop + (listBot - listTop) / 2 - 4, Theme.TEXT_DIM, false);
			pose.popPose();
			return;
		}

		int rows = (listBot - listTop - 4) / ROW_H;
		int maxScroll = Math.max(0, filtered.size() - rows);
		if (scroll > maxScroll) scroll = maxScroll;
		if (scroll < 0) scroll = 0;

		ctx.enableScissor(listX + 1, listTop + 1, listX + listW - 1, listBot - 1);
		int drawY = listTop + 4;
		for (int i = scroll; i < filtered.size(); i++) {
			if (drawY + ROW_H > listBot) break;
			PlayerAgg a = filtered.get(i);
			boolean rowHover = mx >= listX + 4 && mx < listX + listW - 4 && my >= drawY - 1 && my < drawY + ROW_H - 1;
			if (rowHover) ctx.fill(listX + 2, drawY - 1, listX + listW - 2, drawY + ROW_H - 1, 0x14FFFFFF);

			int textY = drawY + (ROW_H - font.lineHeight) / 2;
			ctx.drawString(font, a.player, listX + 8, textY, Theme.TEXT, false);
			String rec = a.wins + "W / " + a.losses + "L";
			ctx.drawString(font, rec, listX + 200, textY, Theme.TEXT_MUTED, false);
			String netStr = AmountFormat.signed(a.net);
			int nw = font.width(netStr);
			ctx.drawString(font, netStr, listX + listW - 16 - nw, textY, Theme.color(a.net), false);

			rowRects.add(new int[]{listX + 4, drawY - 1, listX + listW - 4, drawY + ROW_H - 1});
			rowPlayers.add(a.player);
			drawY += ROW_H;
		}
		ctx.disableScissor();

		if (filtered.size() > rows) {
			int sbX = listX + listW - 4;
			int sbTop = listTop + 2;
			int sbLen = listBot - listTop - 4;
			int thumb = Math.max(12, sbLen * rows / filtered.size());
			int off = (sbLen - thumb) * scroll / Math.max(1, filtered.size() - rows);
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
		if (hit(resetRect, mx, my)) {
			if (resetArmed) {
				GamblerPlusClient.STATS.resetAllTime();
				GamblerPlusClient.ALLTIME.clearAll();
				GamblerPlusClient.CONFIG.save();
				resetArmed = false;
			} else {
				resetArmed = true;
				resetArmedAtMs = System.currentTimeMillis();
			}
			return true;
		}
		searchFocused = hit(searchRect, mx, my);
		if (hit(sortRect, mx, my)) {
			Sort[] all = Sort.values();
			sort = all[(sort.ordinal() + 1) % all.length];
			scroll = 0;
			return true;
		}
		for (int i = 0; i < rowRects.size(); i++) {
			if (hit(rowRects.get(i), mx, my)) {
				Minecraft.getInstance().setScreen(new PlayerAllTimeScreen(rowPlayers.get(i)));
				return true;
			}
		}
		return super.mouseClicked(_mx0, _my0, _btn);
	}

	@Override
	public boolean mouseScrolled(double mxD, double myD, double h, double v) {
		int step = 1;
		if (v < 0) scroll += step;
		else scroll -= step;
		if (scroll < 0) scroll = 0;
		return true;
	}

	@Override
	public boolean keyPressed(int keyCode, int _sc, int _md) {
		int key = keyCode;
		if (key == GLFW.GLFW_KEY_ESCAPE) { onClose(); return true; }
		if (!searchFocused) return super.keyPressed(keyCode, _sc, _md);
		if (key == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
			search = search.substring(0, search.length() - 1);
			scroll = 0;
			return true;
		}
		if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
			searchFocused = false;
			return true;
		}
		return super.keyPressed(keyCode, _sc, _md);
	}

	@Override
	public boolean charTyped(char _ch, int _mods) {
		if (!searchFocused) return false;
		if (search.length() > 24) return true;
		int cp = (int) _ch;
		if (cp < 32 || cp >= 127) return false;
		char c = (char) cp;
		if (Character.isLetterOrDigit(c) || c == '_') {
			search += c;
			scroll = 0;
			return true;
		}
		return false;
	}

	private static boolean hit(int[] r, int mx, int my) {
		return r != null && mx >= r[0] && mx < r[2] && my >= r[1] && my < r[3];
	}

	@Override public void onClose() {
		Minecraft.getInstance().setScreen(new TrackerScreen(GamblerPlusClient.CONFIG, GamblerPlusClient.STATS));
	}
}
