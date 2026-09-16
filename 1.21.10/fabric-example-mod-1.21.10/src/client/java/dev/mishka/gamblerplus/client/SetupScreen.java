package dev.mishka.gamblerplus.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

public final class SetupScreen extends Screen {
	private static final ResourceLocation ICON =
			ResourceLocation.fromNamespaceAndPath("gamblerplus", "textures/gui/donut.png");

	private static final int PANEL_W = 380;
	private static final int PANEL_H = 176;
	private static final int ICON_SIZE = 56;
	private static final long ANIM_MS = 320L;
	private static final long FLASH_MS = 700L;

	private final Config config;
	private long openedAtMs;
	private String flashKey;
	private long flashUntilMs;
	private String rejectMsg;
	private long rejectUntilMs;
	private boolean userBound;
	private boolean closing;
	private float uiScale = 1f;

	public SetupScreen(Config config) {
		super(Component.literal("Gambler Plus"));
		this.config = config;
	}

	@Override protected void init() {
		openedAtMs = System.currentTimeMillis();
		int margin = 16;
		float sw = (float) (width  - margin) / PANEL_W;
		float sh = (float) (height - margin) / PANEL_H;
		uiScale = Math.min(1f, Math.min(sw, sh));
	}
	@Override public boolean isPauseScreen() { return true; }
	@Override public boolean shouldCloseOnEsc() { return false; }

	@Override
	public void tick() {
		if (userBound && !closing && System.currentTimeMillis() >= flashUntilMs) {
			closing = true;
			Minecraft.getInstance().setScreenAndShow(null);
		}
	}

	@Override
	public void render(GuiGraphics ctx, int mx, int my, float delta) {
		long elapsed = System.currentTimeMillis() - openedAtMs;
		float t = Math.min(1f, elapsed / (float) ANIM_MS);
		float eased = Theme.easeOutCubic(t);
		int backdropA = (int) (0xE0 * eased);
		ctx.fill(0, 0, width, height, backdropA << 24);

		int virtualW = Math.round(width  / uiScale);
		int virtualH = Math.round(height / uiScale);
		int px = (virtualW  - PANEL_W) / 2;
		int py = (virtualH - PANEL_H) / 2;

		var pose = ctx.pose();
		pose.pushMatrix();
		pose.translate(0f, (1f - eased) * -20f);
		pose.scale(uiScale, uiScale);

		ctx.fill(px, py, px + PANEL_W, py + PANEL_H, Theme.BG);
		outline(ctx, px, py, px + PANEL_W, py + PANEL_H, Theme.PANEL_LINE);
		ctx.fill(px, py, px + PANEL_W, py + 2, Theme.BRAND);

		int cx = virtualW / 2;
		int iy = py + 18;
		ctx.blit(RenderPipelines.GUI_TEXTURED, ICON,
				cx - ICON_SIZE / 2, iy, 0f, 0f,
				ICON_SIZE, ICON_SIZE, 500, 500, 500, 500, 0xFFFFFFFF);

		String title = "welcome to gambler plus";
		int tw = font.width(title);
		ctx.drawString(font, title, cx - tw / 2, iy + ICON_SIZE + 10, Theme.TEXT, false);

		String sub;
		int subColor;
		if (rejectMsg != null && System.currentTimeMillis() < rejectUntilMs) {
			sub = rejectMsg;
			subColor = Theme.LOSS;
		} else if (flashKey != null) {
			sub = "bound to " + flashKey;
			subColor = Theme.GAIN;
		} else {
			sub = "press any key or mouse button to bind the tracker window";
			subColor = Theme.TEXT_MUTED;
		}
		int sw = font.width(sub);
		ctx.drawString(font, sub, cx - sw / 2, iy + ICON_SIZE + 24, subColor, false);

		String hint = "you can always rebind in Options > Controls";
		int hw = font.width(hint);
		ctx.drawString(font, hint, cx - hw / 2, py + PANEL_H - 18, Theme.TEXT_DIM, false);

		pose.popMatrix();
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (userBound) return true;
		int key = event.key();
		if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_UNKNOWN) return true;
		if (key == GLFW.GLFW_KEY_LEFT_SHIFT || key == GLFW.GLFW_KEY_RIGHT_SHIFT) {
			flashReject("shift is reserved");
			return true;
		}
		bind(InputConstants.getKey(event));
		return true;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (userBound) return true;
		if (event.button() == 0) {
			flashReject("left click is reserved");
			return true;
		}
		bind(InputConstants.Type.MOUSE.getOrCreate(event.button()));
		return true;
	}

	private void flashReject(String msg) {
		rejectMsg = msg;
		rejectUntilMs = System.currentTimeMillis() + 1200L;
	}

	private void bind(InputConstants.Key k) {
		Keybinds.openUi.setKey(k);
		KeyMapping.resetMapping();
		Minecraft.getInstance().options.save();
		flashKey = k.getDisplayName().getString();
		flashUntilMs = System.currentTimeMillis() + FLASH_MS;
		userBound = true;
		config.setSetupComplete(true);
	}

	@Override
	public void onClose() {
		Minecraft.getInstance().setScreenAndShow(null);
	}

	private void outline(GuiGraphics ctx, int x1, int y1, int x2, int y2, int c) {
		ctx.fill(x1, y1, x2, y1 + 1, c);
		ctx.fill(x1, y2 - 1, x2, y2, c);
		ctx.fill(x1, y1, x1 + 1, y2, c);
		ctx.fill(x2 - 1, y1, x2, y2, c);
	}
}
