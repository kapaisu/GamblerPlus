package dev.mishka.gamblerplus.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class HudEditScreen extends Screen {
	private enum TargetKind { NONE, AUCTION, TIMER, TEXT, IMAGE }

	private TargetKind dragKind = TargetKind.NONE;
	private int dragTextIndex = -1;
	private int dragImageIndex = -1;
	private int dragOffX, dragOffY;
	private boolean prevMouseDown = false;

	public HudEditScreen() {
		super(Component.literal("Edit HUD"));
		GamblerPlusClient.CONFIG.hudLayout().editing = true;
	}

	@Override public boolean isPauseScreen() { return false; }
	@Override public boolean shouldCloseOnEsc() { return true; }

	@Override
	public void render(GuiGraphics ctx, int mx, int my, float delta) {
		ctx.fill(0, 0, width, height, 0x66000000);

		HudLayout layout = GamblerPlusClient.CONFIG.hudLayout();

		Minecraft mc = Minecraft.getInstance();
		long window = mc.getWindow().handle();
		boolean down = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

		if (down && dragKind == TargetKind.NONE && !prevMouseDown) {
			int[] a = AuctionHud.bounds(layout);
			int[] tR = TimerHud.bounds(layout);
			if (inRect(mx, my, a)) {
				dragKind = TargetKind.AUCTION;
				dragOffX = mx - layout.auctionX;
				dragOffY = my - layout.auctionY;
			} else if (inRect(mx, my, tR)) {
				dragKind = TargetKind.TIMER;
				dragOffX = mx - layout.timerX;
				dragOffY = my - layout.timerY;
			} else {
				boolean grabbed = false;
				for (int i = 0; i < layout.textEntries.size(); i++) {
					HudLayout.TextEntry e = layout.textEntries.get(i);
					int[] eR = TextHud.bounds(this.font, e);
					if (inRect(mx, my, eR)) {
						dragKind = TargetKind.TEXT;
						dragTextIndex = i;
						dragOffX = mx - e.x;
						dragOffY = my - e.y;
						grabbed = true;
						break;
					}
				}
				if (!grabbed) {
					for (int i = 0; i < layout.imageEntries.size(); i++) {
						HudLayout.ImageEntry ie = layout.imageEntries.get(i);
						int[] eR = ImageHud.bounds(ie);
						if (inRect(mx, my, eR)) {
							dragKind = TargetKind.IMAGE;
							dragImageIndex = i;
							dragOffX = mx - ie.x;
							dragOffY = my - ie.y;
							break;
						}
					}
				}
			}
		}
		if (down && dragKind != TargetKind.NONE) {
			int nx = mx - dragOffX;
			int ny = my - dragOffY;
			int maxX = width - 40;
			int maxY = height - 20;
			nx = Math.max(0, Math.min(maxX, nx));
			ny = Math.max(0, Math.min(maxY, ny));
			switch (dragKind) {
				case AUCTION -> { layout.auctionX = nx; layout.auctionY = ny; }
				case TIMER   -> { layout.timerX   = nx; layout.timerY   = ny; }
				case TEXT -> {
					if (dragTextIndex >= 0 && dragTextIndex < layout.textEntries.size()) {
						HudLayout.TextEntry e = layout.textEntries.get(dragTextIndex);
						e.x = nx;
						e.y = ny;
					}
				}
				case IMAGE -> {
					if (dragImageIndex >= 0 && dragImageIndex < layout.imageEntries.size()) {
						HudLayout.ImageEntry ie = layout.imageEntries.get(dragImageIndex);
						ie.x = nx;
						ie.y = ny;
					}
				}
				default -> {}
			}
		}
		if (!down && dragKind != TargetKind.NONE) {
			dragKind = TargetKind.NONE;
			dragTextIndex = -1;
			dragImageIndex = -1;
			GamblerPlusClient.CONFIG.save();
		}
		prevMouseDown = down;

		ImageHud.drawAll(ctx, font, layout, true);
		AuctionHud.draw(ctx, font, layout, true);
		TimerHud.draw(ctx, font, layout, true);
		TextHud.drawAll(ctx, font, layout, true);

		String hint = "drag to move  |  scroll while hovering to resize  |  esc to finish";
		int hw = font.width(hint);
		ctx.drawString(font, hint, (width - hw) / 2, height - 18, Theme.TEXT, false);
	}

	@Override
	public boolean mouseScrolled(double mxD, double myD, double hx, double vy) {
		int mx = (int) mxD;
		int my = (int) myD;
		HudLayout layout = GamblerPlusClient.CONFIG.hudLayout();
		int[] a = AuctionHud.bounds(layout);
		int[] t = TimerHud.bounds(layout);
		float step = vy > 0 ? 0.05f : -0.05f;
		if (inRect(mx, my, a)) {
			layout.auctionScale = HudLayout.clampScale(layout.auctionScale + step);
			GamblerPlusClient.CONFIG.save();
			return true;
		}
		if (inRect(mx, my, t)) {
			layout.timerScale = HudLayout.clampScale(layout.timerScale + step);
			GamblerPlusClient.CONFIG.save();
			return true;
		}
		for (HudLayout.TextEntry e : layout.textEntries) {
			int[] eR = TextHud.bounds(this.font, e);
			if (inRect(mx, my, eR)) {
				e.scale = HudLayout.clampScale(e.scale + step);
				GamblerPlusClient.CONFIG.save();
				return true;
			}
		}
		for (HudLayout.ImageEntry ie : layout.imageEntries) {
			int[] eR = ImageHud.bounds(ie);
			if (inRect(mx, my, eR)) {
				ie.scale = HudLayout.clampScale(ie.scale + step);
				GamblerPlusClient.CONFIG.save();
				return true;
			}
		}
		return true;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.key() == GLFW.GLFW_KEY_ESCAPE) { onClose(); return true; }
		return super.keyPressed(event);
	}

	@Override
	public void onClose() {
		GamblerPlusClient.CONFIG.hudLayout().editing = false;
		GamblerPlusClient.CONFIG.save();
		Minecraft.getInstance().setScreenAndShow(new UsefulScreen());
	}

	private static boolean inRect(int x, int y, int[] r) {
		return r != null && x >= r[0] && x < r[2] && y >= r[1] && y < r[3];
	}
}
