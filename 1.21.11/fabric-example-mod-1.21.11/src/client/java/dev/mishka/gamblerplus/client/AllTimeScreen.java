package dev.mishka.gamblerplus.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class AllTimeScreen extends Screen {
	public enum Sort {
		NEWEST("newest first"),
		OLDEST("oldest first"),
		HIGH_LOW("highest first"),
		LOW_HIGH("lowest first");
		final String label;
		Sort(String s) { this.label = s; }
	}

	private static final int PANEL_W = 500;
	private static final int PANEL_H = 320;
	private static final int ROW_H = 14;
	private static final DateTimeFormatter STAMP =
			DateTimeFormatter.ofPattern("MMM d HH:mm").withZone(ZoneId.systemDefault());

	private int px, py;
	private float uiScale = 1f;
	private long openedAtMs;

	private String search = "";
	private boolean searchFocused = false;
	private Sort sort = Sort.NEWEST;
	private int scroll = 0;

	private int[] closeRect;
	private int[] searchRect;
	private int[] sortRect;
	private int listX, listTop, listBot, listW;
	private final List<int[]> removeRects = new ArrayList<>();
	private final List<PaymentEvent> rowRefs = new ArrayList<>();

	public AllTimeScreen() {
		super(Minecraft.getInstance(), Minecraft.getInstance().font, Component.literal("All time"));
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
		Widgets.header(ctx, font, px, py, PANEL_W, "All time");

		int cw = 62, ch = 14;
		int cx = px + PANEL_W - cw - 8, cy = py + 5;
		Widgets.flatButton(ctx, font, "close", cx, cy, cw, ch, mx, my, Theme.BRAND);
		closeRect = new int[]{cx, cy, cx + cw, cy + ch};

		int barY = py + 32;
		int barX = px + 12;
		int searchW = 260;
		int searchH = 16;
		Widgets.textField(ctx, font, barX, barY, searchW, searchH, search, searchFocused, "search player");
		searchRect = new int[]{barX, barY, barX + searchW, barY + searchH};

		int sortX = barX + searchW + 8;
		int sortW = 140;
		Widgets.flatButton(ctx, font, "sort: " + sort.label, sortX, barY, sortW, searchH, mx, my, Theme.BRAND);
		sortRect = new int[]{sortX, barY, sortX + sortW, barY + searchH};

		int countX = sortX + sortW + 8;
		String countLabel = GamblerPlusClient.ALLTIME.size() + " events";
		ctx.drawString(font, countLabel, countX, barY + (searchH - font.lineHeight) / 2 + 1, Theme.TEXT_DIM, false);

		listX = px + 8;
		listW = PANEL_W - 16;
		listTop = barY + searchH + 8;
		listBot = py + PANEL_H - 12;
		Theme.roundPanel(ctx, listX, listTop, listX + listW, listBot, 4, Theme.SURFACE_ALT, Theme.PANEL_LINE);

		List<PaymentEvent> all = GamblerPlusClient.ALLTIME.snapshot();
		java.util.Map<PaymentEvent, String> multLabels = Multiplier.labelsFor(all);
		String q = search.trim().toLowerCase();
		List<PaymentEvent> filtered = new ArrayList<>();
		for (PaymentEvent p : all) {
			if (q.isEmpty() || p.player().toLowerCase().contains(q)) filtered.add(p);
		}
		switch (sort) {
			case NEWEST -> filtered.sort(Comparator.comparingLong(PaymentEvent::timestampMs).reversed());
			case OLDEST -> filtered.sort(Comparator.comparingLong(PaymentEvent::timestampMs));
			case HIGH_LOW -> filtered.sort(Comparator.comparingLong(PaymentEvent::amount).reversed());
			case LOW_HIGH -> filtered.sort(Comparator.comparingLong(PaymentEvent::amount));
		}

		removeRects.clear();
		rowRefs.clear();

		if (filtered.isEmpty()) {
			String s = q.isEmpty() ? "no payments yet" : "no matches for '" + search + "'";
			int sw = font.width(s);
			ctx.drawString(font, s, listX + (listW - sw) / 2, listTop + (listBot - listTop) / 2 - 4, Theme.TEXT_DIM, false);
			pose.popMatrix();
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
			PaymentEvent p = filtered.get(i);
			boolean rowHover = mx >= listX + 4 && mx < listX + listW - 4 && my >= drawY - 1 && my < drawY + ROW_H - 1;
			if (rowHover) ctx.fill(listX + 2, drawY - 1, listX + listW - 2, drawY + ROW_H - 1, 0x14FFFFFF);

			String time = STAMP.format(Instant.ofEpochMilli(p.timestampMs()));
			String dir = p.incoming() ? "IN " : "OUT";
			int dirColor = p.incoming() ? Theme.GAIN : Theme.LOSS;
			String amt = (p.incoming() ? "+" : "-") + AmountFormat.pretty(p.amount());
			int aw = font.width(amt);

			ctx.drawString(font, time, listX + 8, drawY, Theme.TEXT_DIM, false);
			ctx.drawString(font, dir, listX + 76, drawY, dirColor, false);
			ctx.drawString(font, p.player(), listX + 100, drawY, Theme.TEXT, false);
			int xBtnX = listX + listW - 16;
			int amtX = xBtnX - 8 - aw;
			ctx.drawString(font, amt, amtX, drawY, dirColor, false);
			String mult = multLabels.get(p);
			if (mult != null) {
				int mw = font.width(mult);
				ctx.drawString(font, mult, amtX - mw - 4, drawY, Theme.WARN, false);
			}
			boolean xHover = mx >= xBtnX - 2 && mx < xBtnX + 10 && my >= drawY - 1 && my < drawY + ROW_H - 2;
			ctx.drawString(font, "x", xBtnX, drawY, xHover ? Theme.LOSS : Theme.TEXT_DIM, false);
			removeRects.add(new int[]{xBtnX - 2, drawY - 1, xBtnX + 10, drawY + ROW_H - 2});
			rowRefs.add(p);
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

		pose.popMatrix();
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		int mx = Math.round((float) event.x() / uiScale);
		int my = Math.round((float) event.y() / uiScale);
		if (hit(closeRect, mx, my)) { onClose(); return true; }
		searchFocused = hit(searchRect, mx, my);
		if (hit(sortRect, mx, my)) {
			Sort[] all = Sort.values();
			sort = all[(sort.ordinal() + 1) % all.length];
			scroll = 0;
			return true;
		}
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
		return super.mouseClicked(event, doubleClick);
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
	public boolean keyPressed(KeyEvent event) {
		int key = event.key();
		if (key == GLFW.GLFW_KEY_ESCAPE) { onClose(); return true; }
		if (!searchFocused) return super.keyPressed(event);
		if (key == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
			search = search.substring(0, search.length() - 1);
			scroll = 0;
			return true;
		}
		if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
			searchFocused = false;
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		if (!searchFocused) return false;
		if (search.length() > 24) return true;
		int cp = event.codepoint();
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
		Minecraft.getInstance().setScreenAndShow(new TrackerScreen(GamblerPlusClient.CONFIG, GamblerPlusClient.STATS));
	}
}
