package dev.mishka.gamblerplus.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;

public final class ImageHud {
	private ImageHud() {}

	public static int[] bounds(HudLayout.ImageEntry e) {
		ImageStore.Entry t = GamblerPlusClient.IMAGES.get(e.file);
		if (t == null) {
			int w = Math.round(60 * e.scale);
			int h = Math.round(20 * e.scale);
			return new int[]{e.x, e.y, e.x + w, e.y + h};
		}
		int w = Math.round(t.width() * e.scale);
		int h = Math.round(t.height() * e.scale);
		return new int[]{e.x, e.y, e.x + w, e.y + h};
	}

	public static void drawAll(GuiGraphics ctx, Font font, HudLayout layout, boolean editOutline) {
		for (HudLayout.ImageEntry e : layout.imageEntries) {
			drawEntry(ctx, font, e, editOutline);
		}
	}

	public static void drawEntry(GuiGraphics ctx, Font font, HudLayout.ImageEntry e, boolean editOutline) {
		ImageStore.Entry t = GamblerPlusClient.IMAGES.get(e.file);
		int[] r = bounds(e);
		if (editOutline) {
			Theme.roundPanel(ctx, r[0] - 1, r[1] - 1, r[2] + 1, r[3] + 1, 3, 0x201155CC, Theme.HUD_EDGE);
		}
		if (t == null) {
			if (editOutline) {
				String msg = e.file.isEmpty() ? "no file" : ("missing: " + e.file);
				ctx.drawString(font, msg, r[0] + 2, r[1] + 4, Theme.LOSS, false);
			}
			return;
		}
		var pose = ctx.pose();
		pose.pushPose();
		pose.translate(e.x, e.y, 0f);
		pose.scale(e.scale, e.scale, 1f);
		ctx.blit(RenderType::guiTextured, t.id(), 0, 0, 0f, 0f, t.width(), t.height(), t.width(), t.height());
		pose.popPose();
	}
}
