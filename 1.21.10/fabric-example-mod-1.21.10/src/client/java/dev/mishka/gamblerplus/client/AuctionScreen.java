package dev.mishka.gamblerplus.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class AuctionScreen extends Screen {
	private static final int PANEL_W = 420;
	private static final int PANEL_H = 320;
	private static final int PICKER_W = 220;
	private static final int PICKER_GAP = 10;

	private static final String[] QUICK_LABELS = {"30s", "1m", "2m", "5m", "10m"};

	private enum Focus { NONE, TIME, MIN, COUNT, SEARCH }

	private int px, py;
	private float uiScale = 1f;
	private long openedAtMs;

	private String timeText = "";
	private String minText = "";
	private String countText = "1";
	private String searchText = "";
	private Focus focus = Focus.TIME;
	private String feedback = "";

	private ItemStack selectedStack = ItemStack.EMPTY;
	private String selectedName = "";

	private int pickerScroll = 0;
	private List<ItemStack> filtered = new ArrayList<>();
	private String lastSearch = "￿";

	private int[] timeRect;
	private int[] minRect;
	private int[] countRect;
	private int[] snapRect;
	private int[] startRect;
	private int[] stopRect;
	private int[] closeRect;
	private int[] add10Rect;
	private int[] add30Rect;
	private int[] searchRect;
	private int[] pickerListRect;
	private final int[][] quickRects = new int[QUICK_LABELS.length][];
	private final List<int[]> pickerRects = new ArrayList<>();
	private final List<ItemStack> pickerRefs = new ArrayList<>();

	public AuctionScreen() {
		super(Component.literal("Auction"));
		grabHeld();
	}

	private void grabHeld() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return;
		ItemStack stack = mc.player.getMainHandItem();
		if (stack == null || stack.isEmpty()) return;
		selectedStack = stack.copy();
		selectedName = stack.getHoverName().getString();
		if (stack.getCount() > 1) countText = String.valueOf(stack.getCount());
	}

	private void setSelected(ItemStack stack) {
		if (stack == null || stack.isEmpty()) return;
		selectedStack = stack.copy();
		selectedStack.setCount(1);
		selectedName = stack.getHoverName().getString();
	}

	private void refreshFilter() {
		String q = searchText.trim().toLowerCase(Locale.ROOT);
		if (q.equals(lastSearch)) return;
		lastSearch = q;
		filtered.clear();
		int cap = 400;
		for (Item item : BuiltInRegistries.ITEM) {
			ItemStack s = new ItemStack(item);
			if (s.isEmpty()) continue;
			if (!q.isEmpty()) {
				String name = s.getHoverName().getString().toLowerCase(Locale.ROOT);
				String id = BuiltInRegistries.ITEM.getKey(item).toString().toLowerCase(Locale.ROOT);
				if (!name.contains(q) && !id.contains(q)) continue;
			}
			filtered.add(s);
			if (filtered.size() >= cap) break;
		}
		pickerScroll = 0;
	}

	@Override protected void init() {
		int totalW = PANEL_W + PICKER_GAP + PICKER_W;
		int margin = 16;
		float sw = (float) (width - margin) / totalW;
		float sh = (float) (height - margin) / PANEL_H;
		uiScale = Math.min(1f, Math.min(sw, sh));
		int vw = Math.round(width / uiScale);
		int vh = Math.round(height / uiScale);
		px = (vw - totalW) / 2;
		py = (vh - PANEL_H) / 2;
		openedAtMs = System.currentTimeMillis();
		refreshFilter();
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

		Theme.roundRect(ctx, px, py, px + PANEL_W, py + 24, 6, Theme.SURFACE);
		ctx.fill(px + 1, py + 23, px + PANEL_W - 1, py + 24, Theme.PANEL_LINE);
		String bA = "Gambler ";
		String bB = "Plus";
		String bC = "  Auction";
		int wA = font.width(bA);
		int wB = font.width(bB);
		int wC = font.width(bC);
		int totalW = wA + wB + wC;
		int bx = px + (PANEL_W - totalW) / 2;
		int by = py + (24 - font.lineHeight) / 2 + 1;
		ctx.drawString(font, bA, bx, by, Theme.TEXT, false);
		ctx.drawString(font, bB, bx + wA, by, Theme.BRAND, false);
		ctx.drawString(font, bC, bx + wA + wB, by, Theme.TEXT_DIM, false);

		int cw = 62, ch = 14;
		int cx = px + PANEL_W - cw - 8, cy = py + 5;
		Widgets.flatButton(ctx, font, "close", cx, cy, cw, ch, mx, my, Theme.BRAND);
		closeRect = new int[]{cx, cy, cx + cw, cy + ch};

		Auction auction = GamblerPlusClient.AUCTION;
		auction.tick();
		boolean live = auction.active();

		int contentX = px + 16;
		int contentY = py + 36;
		int fieldW = PANEL_W - 32;

		ctx.drawString(font, "item", contentX, contentY, Theme.TEXT_DIM, false);
		int itemBoxY = contentY + 12;
		int itemBoxH = 28;
		Theme.roundPanel(ctx, contentX, itemBoxY, contentX + fieldW, itemBoxY + itemBoxH, 3, Theme.SURFACE, Theme.PANEL_LINE);

		if (!selectedStack.isEmpty()) {
			int iconX = contentX + 6;
			int iconY = itemBoxY + (itemBoxH - 16) / 2;
			ctx.renderItem(selectedStack, iconX, iconY);
			ctx.drawString(font, selectedName, contentX + 30, itemBoxY + (itemBoxH - font.lineHeight) / 2, Theme.TEXT, false);
		} else {
			ctx.drawString(font, "pick an item on the right or hold one and press snapshot", contentX + 10, itemBoxY + (itemBoxH - font.lineHeight) / 2, Theme.TEXT_DIM, false);
		}
		int snapW = 90;
		int snapX = contentX + fieldW - snapW - 6;
		int snapY = itemBoxY + 5;
		int snapH = itemBoxH - 10;
		Widgets.flatButton(ctx, font, "snapshot held", snapX, snapY, snapW, snapH, mx, my, Theme.BRAND);
		snapRect = new int[]{snapX, snapY, snapX + snapW, snapY + snapH};

		int halfW = (fieldW - 8) / 2;
		int rowLabelY = itemBoxY + itemBoxH + 8;
		ctx.drawString(font, "time (30s, 2m, 1m30s)", contentX, rowLabelY, Theme.TEXT_DIM, false);
		ctx.drawString(font, "min bid (0 to disable)", contentX + halfW + 8, rowLabelY, Theme.TEXT_DIM, false);

		int fY = rowLabelY + 12;
		int fH = 20;
		Widgets.textField(ctx, font, contentX, fY, halfW, fH, timeText, focus == Focus.TIME, "duration");
		timeRect = new int[]{contentX, fY, contentX + halfW, fY + fH};
		Widgets.textField(ctx, font, contentX + halfW + 8, fY, halfW, fH, minText, focus == Focus.MIN, "e.g. 5M");
		minRect = new int[]{contentX + halfW + 8, fY, contentX + halfW + 8 + halfW, fY + fH};

		int quickY = fY + fH + 6;
		int quickH = 14;
		int quickGap = 4;
		int qWidth = (fieldW - quickGap * (QUICK_LABELS.length - 1)) / QUICK_LABELS.length;
		for (int i = 0; i < QUICK_LABELS.length; i++) {
			int qx = contentX + i * (qWidth + quickGap);
			Widgets.flatButton(ctx, font, QUICK_LABELS[i], qx, quickY, qWidth, quickH, mx, my, Theme.BRAND);
			quickRects[i] = new int[]{qx, quickY, qx + qWidth, quickY + quickH};
		}

		int countLabelY = quickY + quickH + 8;
		ctx.drawString(font, "count", contentX, countLabelY, Theme.TEXT_DIM, false);
		int countY = countLabelY + 12;
		int countBoxW = 100;
		Widgets.textField(ctx, font, contentX, countY, countBoxW, fH, countText, focus == Focus.COUNT, "1");
		countRect = new int[]{contentX, countY, contentX + countBoxW, countY + fH};

		int btnRow = countY + fH + 10;
		int btnW = (fieldW - 8) / 2;
		int btnH = 20;
		int startColor = live ? Theme.TEXT_DIM : Theme.GAIN;
		Widgets.button(ctx, font, live ? "running" : "start", contentX, btnRow, btnW, btnH, mx, my, startColor, 1f);
		startRect = new int[]{contentX, btnRow, contentX + btnW, btnRow + btnH};
		Widgets.button(ctx, font, "stop", contentX + btnW + 8, btnRow, btnW, btnH, mx, my, Theme.LOSS, 1f);
		stopRect = new int[]{contentX + btnW + 8, btnRow, contentX + fieldW, btnRow + btnH};

		int stateY = btnRow + btnH + 12;
		if (live) {
			String remain = "ends in " + TimeParse.prettyDuration(auction.remainingMs());
			ctx.drawString(font, remain, contentX, stateY, Theme.BRAND, false);
			int addW = 46, addH = 14;
			int add10X = contentX + fieldW - addW * 2 - 6;
			int add30X = contentX + fieldW - addW;
			Widgets.flatButton(ctx, font, "+10s", add10X, stateY - 3, addW, addH, mx, my, Theme.GAIN);
			add10Rect = new int[]{add10X, stateY - 3, add10X + addW, stateY - 3 + addH};
			Widgets.flatButton(ctx, font, "+30s", add30X, stateY - 3, addW, addH, mx, my, Theme.GAIN);
			add30Rect = new int[]{add30X, stateY - 3, add30X + addW, stateY - 3 + addH};
			if (auction.minBid() > 0) {
				ctx.drawString(font, "min bid: " + AmountFormat.pretty(auction.minBid()), contentX, stateY + 12, Theme.TEXT_MUTED, false);
			}
		} else {
			add10Rect = null;
			add30Rect = null;
			Auction.Bid w = auction.winner();
			if (w != null) {
				ctx.drawString(font, "winner " + w.player() + "  " + AmountFormat.pretty(w.amount()), contentX, stateY, Theme.GAIN, false);
			} else if (!feedback.isEmpty()) {
				ctx.drawString(font, feedback, contentX, stateY, Theme.LOSS, false);
			} else {
				ctx.drawString(font, "pick item, set time, then start", contentX, stateY, Theme.TEXT_MUTED, false);
			}
		}

		drawPicker(ctx, mx, my);

		pose.popMatrix();
	}

	private void drawPicker(GuiGraphics ctx, int mx, int my) {
		refreshFilter();
		int qx = px + PANEL_W + PICKER_GAP;
		int qy = py;
		int qh = PANEL_H;
		Widgets.panel(ctx, qx, qy, qx + PICKER_W, qy + qh);
		Theme.roundRect(ctx, qx, qy, qx + PICKER_W, qy + 24, 6, Theme.SURFACE);
		ctx.fill(qx + 1, qy + 23, qx + PICKER_W - 1, qy + 24, Theme.PANEL_LINE);
		ctx.drawString(font, "Items", qx + 12, qy + 9, Theme.TEXT, false);

		int sx = qx + 8, sy = qy + 30;
		int sw2 = PICKER_W - 16;
		Widgets.textField(ctx, font, sx, sy, sw2, 18, searchText, focus == Focus.SEARCH, "search");
		searchRect = new int[]{sx, sy, sx + sw2, sy + 18};

		int listX = qx + 8;
		int listTop = sy + 22;
		int listBot = qy + qh - 8;
		int listW = PICKER_W - 16;
		Theme.roundPanel(ctx, listX, listTop, listX + listW, listBot, 4, Theme.SURFACE_ALT, Theme.PANEL_LINE);
		pickerListRect = new int[]{listX, listTop, listX + listW, listBot};

		pickerRects.clear();
		pickerRefs.clear();

		if (filtered.isEmpty()) {
			String s = "no matches";
			int sww = font.width(s);
			ctx.drawString(font, s, listX + (listW - sww) / 2, listTop + 12, Theme.TEXT_DIM, false);
			return;
		}

		int rowH = 18;
		int visible = (listBot - listTop - 4) / rowH;
		int maxScroll = Math.max(0, filtered.size() - visible);
		if (pickerScroll > maxScroll) pickerScroll = maxScroll;
		if (pickerScroll < 0) pickerScroll = 0;

		ctx.enableScissor(listX + 1, listTop + 1, listX + listW - 1, listBot - 1);
		int rowY = listTop + 2;
		for (int i = pickerScroll; i < filtered.size(); i++) {
			if (rowY + rowH > listBot) break;
			ItemStack s = filtered.get(i);
			boolean hover = mx >= listX + 4 && mx < listX + listW - 4 && my >= rowY && my < rowY + rowH;
			boolean isSel = !selectedStack.isEmpty() && selectedStack.getItem() == s.getItem();
			if (isSel) Theme.roundRect(ctx, listX + 2, rowY, listX + listW - 2, rowY + rowH, 3, Theme.SURFACE);
			else if (hover) ctx.fill(listX + 2, rowY, listX + listW - 2, rowY + rowH, 0x14FFFFFF);
			ctx.renderItem(s, listX + 4, rowY + 1);
			ctx.drawString(font, s.getHoverName().getString(), listX + 24, rowY + (rowH - font.lineHeight) / 2 + 1,
					isSel ? Theme.BRAND : Theme.TEXT, false);
			pickerRects.add(new int[]{listX + 2, rowY, listX + listW - 2, rowY + rowH});
			pickerRefs.add(s);
			rowY += rowH;
		}
		ctx.disableScissor();

		if (filtered.size() > visible) {
			int sbX = listX + listW - 4;
			int sbTop = listTop + 2;
			int sbLen = listBot - listTop - 4;
			int thumb = Math.max(12, sbLen * visible / filtered.size());
			int off = maxScroll == 0 ? 0 : (sbLen - thumb) * pickerScroll / maxScroll;
			ctx.fill(sbX, sbTop, sbX + 2, sbTop + sbLen, Theme.PANEL_LINE);
			ctx.fill(sbX, sbTop + off, sbX + 2, sbTop + off + thumb, Theme.TEXT_MUTED);
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		int mx = Math.round((float) event.x() / uiScale);
		int my = Math.round((float) event.y() / uiScale);
		if (hit(closeRect, mx, my)) { onClose(); return true; }
		if (hit(timeRect, mx, my)) { focus = Focus.TIME; return true; }
		if (hit(minRect, mx, my)) { focus = Focus.MIN; return true; }
		if (hit(countRect, mx, my)) { focus = Focus.COUNT; return true; }
		if (hit(searchRect, mx, my)) { focus = Focus.SEARCH; return true; }
		for (int i = 0; i < pickerRects.size(); i++) {
			if (hit(pickerRects.get(i), mx, my)) {
				setSelected(pickerRefs.get(i));
				return true;
			}
		}
		focus = Focus.NONE;
		if (hit(snapRect, mx, my)) { grabHeld(); return true; }
		for (int i = 0; i < quickRects.length; i++) {
			if (hit(quickRects[i], mx, my)) {
				timeText = QUICK_LABELS[i];
				return true;
			}
		}
		Auction auction = GamblerPlusClient.AUCTION;
		if (hit(add10Rect, mx, my)) { auction.extend(10_000L); return true; }
		if (hit(add30Rect, mx, my)) { auction.extend(30_000L); return true; }
		if (hit(startRect, mx, my)) {
			if (auction.active()) return true;
			long dur = TimeParse.parseMs(timeText);
			if (dur <= 0) { feedback = "invalid time"; return true; }
			if (selectedStack.isEmpty()) { feedback = "pick an item first"; return true; }
			int count = 1;
			try {
				count = Math.max(1, Integer.parseInt(countText.trim()));
			} catch (NumberFormatException e) {
				feedback = "invalid count";
				return true;
			}
			long minBid = 0L;
			if (!minText.isBlank()) {
				long parsed = AmountFormat.parse(minText);
				if (parsed < 0) { feedback = "invalid min bid"; return true; }
				minBid = parsed;
			}
			ItemStack usedStack = selectedStack.copy();
			usedStack.setCount(count);
			auction.start(selectedName, count, dur, usedStack, minBid);
			feedback = "";
			Minecraft.getInstance().setScreenAndShow(null);
			return true;
		}
		if (hit(stopRect, mx, my)) { auction.stop(); return true; }
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseScrolled(double mxD, double myD, double h, double v) {
		int mx = Math.round((float) mxD / uiScale);
		int my = Math.round((float) myD / uiScale);
		if (hit(pickerListRect, mx, my)) {
			if (v < 0) pickerScroll++;
			else pickerScroll--;
			if (pickerScroll < 0) pickerScroll = 0;
			return true;
		}
		return super.mouseScrolled(mxD, myD, h, v);
	}

	private static boolean hit(int[] r, int mx, int my) {
		return r != null && mx >= r[0] && mx < r[2] && my >= r[1] && my < r[3];
	}

	private String activeText() {
		return switch (focus) {
			case MIN -> minText;
			case COUNT -> countText;
			case SEARCH -> searchText;
			case TIME -> timeText;
			default -> "";
		};
	}

	private void setActiveText(String s) {
		switch (focus) {
			case MIN -> minText = s;
			case COUNT -> countText = s;
			case SEARCH -> searchText = s;
			case TIME -> timeText = s;
			default -> {}
		}
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		int key = event.key();
		if (key == GLFW.GLFW_KEY_ESCAPE) { onClose(); return true; }
		if (focus == Focus.NONE) return super.keyPressed(event);
		String cur = activeText();
		if (key == GLFW.GLFW_KEY_BACKSPACE && !cur.isEmpty()) {
			setActiveText(cur.substring(0, cur.length() - 1));
			return true;
		}
		if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
			focus = Focus.NONE;
			return true;
		}
		if (key == GLFW.GLFW_KEY_TAB) {
			focus = switch (focus) {
				case TIME -> Focus.MIN;
				case MIN -> Focus.COUNT;
				case COUNT -> Focus.SEARCH;
				case SEARCH -> Focus.TIME;
				default -> Focus.TIME;
			};
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		if (focus == Focus.NONE) return false;
		String cur = activeText();
		int limit = focus == Focus.SEARCH ? 32 : 14;
		if (cur.length() > limit) return true;
		int cp = event.codepoint();
		if (cp < 32 || cp >= 127) return false;
		char c = (char) cp;
		if (focus == Focus.MIN) {
			char up = Character.toUpperCase(c);
			if (Character.isDigit(c) || c == '.' || c == ',' || up == 'K' || up == 'M' || up == 'B' || up == 'T') {
				setActiveText(cur + c);
				return true;
			}
			return false;
		}
		if (focus == Focus.COUNT) {
			if (Character.isDigit(c)) {
				setActiveText(cur + c);
				return true;
			}
			return false;
		}
		if (focus == Focus.SEARCH) {
			if (Character.isLetterOrDigit(c) || c == '_' || c == ' ' || c == ':') {
				setActiveText(cur + c);
				return true;
			}
			return false;
		}
		if (Character.isLetterOrDigit(c)) {
			setActiveText(cur + c);
			return true;
		}
		return false;
	}

	@Override public void onClose() {
		Minecraft.getInstance().setScreenAndShow(new UsefulScreen());
	}
}
