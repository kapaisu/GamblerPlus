package dev.mishka.gamblerplus.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class ImagesScreen extends Screen {
	private static final int PANEL_W = 460;
	private static final int PANEL_H = 300;
	private static final int ROW_H = 14;

	private int px, py;
	private float uiScale = 1f;
	private long openedAtMs;

	private int scroll = 0;
	private int selected = -1;

	private int[] closeRect;
	private int[] openFolderRect;
	private int[] refreshRect;
	private int[] removeRect;
	private int listX, listTop, listBot, listW;
	private final List<int[]> addRects = new ArrayList<>();
	private final List<String> addRefs = new ArrayList<>();
	private final List<int[]> entryRects = new ArrayList<>();

	public ImagesScreen() {
		super(Minecraft.getInstance(), Minecraft.getInstance().font, Component.literal("Images"));
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
	public void extractRenderState(GuiGraphicsExtractor ctx, int mxRaw, int myRaw, float delta) {
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
		Widgets.header(ctx, font, px, py, PANEL_W, "Images");

		int cw = 62, ch = 14;
		int cx = px + PANEL_W - cw - 8, cy = py + 5;
		Widgets.flatButton(ctx, font, "close", cx, cy, cw, ch, mx, my, Theme.BRAND);
		closeRect = new int[]{cx, cy, cx + cw, cy + ch};

		HudLayout layout = GamblerPlusClient.CONFIG.hudLayout();

		int barY = py + 32;
		int folderW = 130;
		Widgets.flatButton(ctx, font, "open folder", px + 12, barY, folderW, 16, mx, my, Theme.BRAND);
		openFolderRect = new int[]{px + 12, barY, px + 12 + folderW, barY + 16};
		Widgets.flatButton(ctx, font, "refresh", px + 12 + folderW + 6, barY, 64, 16, mx, my, Theme.BRAND);
		refreshRect = new int[]{px + 12 + folderW + 6, barY, px + 12 + folderW + 6 + 64, barY + 16};
		ctx.text(font, "drop PNG/JPG files in the folder, then refresh",
				px + 12 + folderW + 6 + 64 + 10, barY + 4, Theme.TEXT_DIM, false);

		int leftX = px + 12;
		int leftY = barY + 22;
		int leftW = 220;
		int listH = PANEL_H - (leftY - py) - 12;
		Theme.roundPanel(ctx, leftX, leftY, leftX + leftW, leftY + listH, 4, Theme.SURFACE_ALT, Theme.PANEL_LINE);
		ctx.text(font, "IN FOLDER", leftX + 6, leftY + 3, Theme.TEXT_DIM, false);

		addRects.clear();
		addRefs.clear();
		List<String> files = GamblerPlusClient.IMAGES.available();
		int rowY = leftY + 14;
		if (files.isEmpty()) {
			ctx.text(font, "no images found", leftX + 8, leftY + 22, Theme.TEXT_DIM, false);
		} else {
			ctx.enableScissor(leftX + 1, leftY + 12, leftX + leftW - 1, leftY + listH - 1);
			for (String f : files) {
				if (rowY + ROW_H > leftY + listH - 4) break;
				boolean hover = mx >= leftX + 4 && mx < leftX + leftW - 4 && my >= rowY && my < rowY + ROW_H;
				if (hover) ctx.fill(leftX + 4, rowY, leftX + leftW - 4, rowY + ROW_H, 0x18FFFFFF);
				ctx.text(font, f, leftX + 8, rowY + (ROW_H - font.lineHeight) / 2 + 1, Theme.TEXT, false);
				ctx.text(font, "+ add", leftX + leftW - 32, rowY + (ROW_H - font.lineHeight) / 2 + 1, hover ? Theme.GAIN : Theme.TEXT_DIM, false);
				addRects.add(new int[]{leftX + 4, rowY, leftX + leftW - 4, rowY + ROW_H});
				addRefs.add(f);
				rowY += ROW_H + 1;
			}
			ctx.disableScissor();
		}

		int rightX = leftX + leftW + 10;
		int rightW = px + PANEL_W - rightX - 12;
		Theme.roundPanel(ctx, rightX, leftY, rightX + rightW, leftY + listH, 4, Theme.SURFACE_ALT, Theme.PANEL_LINE);
		ctx.text(font, "ON YOUR HUD", rightX + 6, leftY + 3, Theme.TEXT_DIM, false);

		entryRects.clear();
		int erY = leftY + 14;
		if (layout.imageEntries.isEmpty()) {
			ctx.text(font, "no images added yet", rightX + 8, leftY + 22, Theme.TEXT_DIM, false);
		} else {
			ctx.enableScissor(rightX + 1, leftY + 12, rightX + rightW - 1, leftY + listH - 24);
			for (int i = 0; i < layout.imageEntries.size(); i++) {
				HudLayout.ImageEntry en = layout.imageEntries.get(i);
				if (erY + ROW_H > leftY + listH - 26) break;
				boolean isSel = i == selected;
				boolean rowHover = mx >= rightX + 4 && mx < rightX + rightW - 4 && my >= erY && my < erY + ROW_H;
				if (isSel) Theme.roundRect(ctx, rightX + 4, erY, rightX + rightW - 4, erY + ROW_H, 3, Theme.SURFACE);
				else if (rowHover) ctx.fill(rightX + 4, erY, rightX + rightW - 4, erY + ROW_H, 0x14FFFFFF);
				ctx.text(font, (i + 1) + ". " + en.file, rightX + 8, erY + (ROW_H - font.lineHeight) / 2 + 1, isSel ? Theme.TEXT : Theme.TEXT_MUTED, false);
				entryRects.add(new int[]{rightX + 4, erY, rightX + rightW - 4, erY + ROW_H});
				erY += ROW_H + 1;
			}
			ctx.disableScissor();
			int remW = rightW - 8;
			Widgets.flatButton(ctx, font, selected >= 0 && selected < layout.imageEntries.size() ? "remove" : "select to remove",
					rightX + 4, leftY + listH - 20, remW, 14, mx, my, Theme.LOSS);
			removeRect = new int[]{rightX + 4, leftY + listH - 20, rightX + 4 + remW, leftY + listH - 20 + 14};
		}

		pose.popMatrix();
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		int mx = Math.round((float) event.x() / uiScale);
		int my = Math.round((float) event.y() / uiScale);
		if (hit(closeRect, mx, my)) { onClose(); return true; }
		if (hit(openFolderRect, mx, my)) {
			openFolder(GamblerPlusClient.IMAGES.folder());
			return true;
		}
		if (hit(refreshRect, mx, my)) return true;
		HudLayout layout = GamblerPlusClient.CONFIG.hudLayout();
		for (int i = 0; i < addRects.size(); i++) {
			if (hit(addRects.get(i), mx, my)) {
				HudLayout.ImageEntry e = new HudLayout.ImageEntry();
				e.file = addRefs.get(i);
				e.y = 220 + layout.imageEntries.size() * 40;
				layout.imageEntries.add(e);
				GamblerPlusClient.IMAGES.get(e.file);
				GamblerPlusClient.CONFIG.save();
				return true;
			}
		}
		for (int i = 0; i < entryRects.size(); i++) {
			if (hit(entryRects.get(i), mx, my)) {
				selected = i;
				return true;
			}
		}
		if (hit(removeRect, mx, my) && selected >= 0 && selected < layout.imageEntries.size()) {
			layout.imageEntries.remove(selected);
			if (selected >= layout.imageEntries.size()) selected = layout.imageEntries.size() - 1;
			GamblerPlusClient.CONFIG.save();
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	private static boolean hit(int[] r, int mx, int my) {
		return r != null && mx >= r[0] && mx < r[2] && my >= r[1] && my < r[3];
	}

	private static void openFolder(java.nio.file.Path path) {
		String os = System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT);
		String p = path.toAbsolutePath().toString();
		String[] cmd;
		if (os.contains("win")) cmd = new String[]{"explorer", p};
		else if (os.contains("mac")) cmd = new String[]{"open", p};
		else cmd = new String[]{"xdg-open", p};
		try {
			new ProcessBuilder(cmd).redirectErrorStream(true).start();
			return;
		} catch (Exception ignored) {}
		try {
			java.awt.Desktop.getDesktop().open(path.toFile());
		} catch (Exception ignored) {}
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.key() == GLFW.GLFW_KEY_ESCAPE) { onClose(); return true; }
		return super.keyPressed(event);
	}

	@Override public void onClose() {
		Minecraft.getInstance().setScreenAndShow(new UsefulScreen());
	}
}
