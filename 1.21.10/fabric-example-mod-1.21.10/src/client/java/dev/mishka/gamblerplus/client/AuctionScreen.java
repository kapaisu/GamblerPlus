package dev.mishka.gamblerplus.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public final class AuctionScreen extends Screen {
	private static final int PANEL_W = 380;
	private static final int PANEL_H = 260;

	private int px, py;
	private float uiScale = 1f;
	private long openedAtMs;

	private String timeText = "";
	private boolean timeFocused = true;
	private String feedback = "";

	private String heldName = "";
	private int heldCount = 0;

	private int[] fieldRect;
	private int[] snapRect;
	private int[] startRect;
	private int[] stopRect;
	private int[] closeRect;

	public AuctionScreen() {
		super(Component.literal("Auction"));
		grabHeld();
	}

	private void grabHeld() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) { heldName = "no item"; heldCount = 0; return; }
		ItemStack stack = mc.player.getMainHandItem();
		if (stack == null || stack.isEmpty()) { heldName = "empty hand"; heldCount = 0; return; }
		heldName = stack.getHoverName().getString();
		heldCount = stack.getCount();
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
		pose.pushMatrix();
		pose.translate(0f, (1f - e) * -14f);
		pose.scale(uiScale, uiScale);

		Widgets.panel(ctx, px, py, px + PANEL_W, py + PANEL_H);
		Widgets.header(ctx, font, px, py, PANEL_W, "Auction");

		int cw = 62, ch = 14;
		int cx = px + PANEL_W - cw - 8, cy = py + 5;
		Widgets.flatButton(ctx, font, "close", cx, cy, cw, ch, mx, my, Theme.BRAND);
		closeRect = new int[]{cx, cy, cx + cw, cy + ch};

		Auction auction = GamblerPlusClient.AUCTION;
		auction.tick();

		int contentX = px + 16;
		int contentY = py + 38;
		int fieldW = PANEL_W - 32;

		ctx.drawString(font, "item", contentX, contentY, Theme.TEXT_DIM, false);
		int itemBoxY = contentY + 12;
		int itemBoxH = 26;
		Theme.roundRect(ctx, contentX, itemBoxY, contentX + fieldW, itemBoxY + itemBoxH, 3, Theme.SURFACE);
		Theme.roundOutline(ctx, contentX, itemBoxY, contentX + fieldW, itemBoxY + itemBoxH, 3, Theme.PANEL_LINE);

		String heldLabel = heldCount > 1 ? (heldName + " x" + heldCount) : heldName;
		ctx.drawString(font, heldLabel, contentX + 10, itemBoxY + (itemBoxH - font.lineHeight) / 2, Theme.TEXT, false);
		int snapW = 84;
		int snapX = contentX + fieldW - snapW - 6;
		int snapY = itemBoxY + 4;
		int snapH = itemBoxH - 8;
		Widgets.flatButton(ctx, font, "snapshot held", snapX, snapY, snapW, snapH, mx, my, Theme.BRAND);
		snapRect = new int[]{snapX, snapY, snapX + snapW, snapY + snapH};

		int timeLabelY = itemBoxY + itemBoxH + 10;
		ctx.drawString(font, "time (e.g. 30s, 2m, 1m30s)", contentX, timeLabelY, Theme.TEXT_DIM, false);
		int fY = timeLabelY + 12;
		int fH = 22;
		Widgets.textField(ctx, font, contentX, fY, fieldW, fH, timeText, timeFocused, "duration");
		fieldRect = new int[]{contentX, fY, contentX + fieldW, fY + fH};

		int btnRow = fY + fH + 12;
		int btnW = (fieldW - 8) / 2;
		int btnH = 20;
		boolean live = auction.active();
		int startColor = live ? Theme.TEXT_DIM : Theme.GAIN;
		Widgets.button(ctx, font, live ? "running" : "start", contentX, btnRow, btnW, btnH, mx, my, startColor, 1f);
		startRect = new int[]{contentX, btnRow, contentX + btnW, btnRow + btnH};
		Widgets.button(ctx, font, "stop", contentX + btnW + 8, btnRow, btnW, btnH, mx, my, Theme.LOSS, 1f);
		stopRect = new int[]{contentX + btnW + 8, btnRow, contentX + fieldW, btnRow + btnH};

		int stateY = btnRow + btnH + 12;
		if (live) {
			String remain = "ends in " + TimeParse.prettyDuration(auction.remainingMs());
			ctx.drawString(font, remain, contentX, stateY, Theme.BRAND, false);
			int bidsY = stateY + 14;
			ctx.drawString(font, "TOP BIDS", contentX, bidsY, Theme.TEXT_DIM, false);
			List<Auction.Bid> bids = auction.topBids(5);
			int rowY = bidsY + 12;
			if (bids.isEmpty()) {
				ctx.drawString(font, "no bids yet", contentX, rowY, Theme.TEXT_MUTED, false);
			} else {
				for (Auction.Bid b : bids) {
					int accent = b == bids.get(0) ? Theme.GAIN : Theme.TEXT;
					ctx.drawString(font, b.player(), contentX, rowY, accent, false);
					String amt = AmountFormat.pretty(b.amount());
					int aw = font.width(amt);
					ctx.drawString(font, amt, contentX + fieldW - aw, rowY, accent, false);
					rowY += 12;
				}
			}
		} else {
			Auction.Bid w = auction.winner();
			if (w != null) {
				ctx.drawString(font, "winner", contentX, stateY, Theme.TEXT_DIM, false);
				ctx.drawString(font, w.player() + "  " + AmountFormat.pretty(w.amount()),
						contentX, stateY + 12, Theme.GAIN, false);
				ctx.drawString(font, "for " + auction.item() + (auction.itemCount() > 1 ? " x" + auction.itemCount() : ""),
						contentX, stateY + 26, Theme.TEXT_MUTED, false);
			} else if (!feedback.isEmpty()) {
				ctx.drawString(font, feedback, contentX, stateY, Theme.LOSS, false);
			} else {
				ctx.drawString(font, "set a duration then start", contentX, stateY, Theme.TEXT_MUTED, false);
			}
		}

		pose.popMatrix();
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		int mx = Math.round((float) event.x() / uiScale);
		int my = Math.round((float) event.y() / uiScale);
		if (hit(closeRect, mx, my)) { onClose(); return true; }
		timeFocused = hit(fieldRect, mx, my);
		if (hit(snapRect, mx, my)) { grabHeld(); return true; }
		Auction auction = GamblerPlusClient.AUCTION;
		if (hit(startRect, mx, my)) {
			if (auction.active()) return true;
			long dur = TimeParse.parseMs(timeText);
			if (dur <= 0) { feedback = "invalid time"; return true; }
			if (heldName == null || heldName.isEmpty() || heldName.equals("empty hand") || heldName.equals("no item")) {
				feedback = "hold an item first";
				return true;
			}
			auction.start(heldName, Math.max(1, heldCount), dur);
			feedback = "";
			return true;
		}
		if (hit(stopRect, mx, my)) {
			auction.stop();
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	private static boolean hit(int[] r, int mx, int my) {
		return r != null && mx >= r[0] && mx < r[2] && my >= r[1] && my < r[3];
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		int key = event.key();
		if (key == GLFW.GLFW_KEY_ESCAPE) { onClose(); return true; }
		if (!timeFocused) return super.keyPressed(event);
		if (key == GLFW.GLFW_KEY_BACKSPACE && !timeText.isEmpty()) {
			timeText = timeText.substring(0, timeText.length() - 1);
			return true;
		}
		if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
			timeFocused = false;
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		if (!timeFocused) return false;
		if (timeText.length() > 12) return true;
		int cp = event.codepoint();
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
		Minecraft.getInstance().setScreenAndShow(new UsefulScreen());
	}
}
