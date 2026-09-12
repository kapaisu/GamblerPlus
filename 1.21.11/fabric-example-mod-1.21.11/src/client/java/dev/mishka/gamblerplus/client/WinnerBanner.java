package dev.mishka.gamblerplus.client;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class WinnerBanner {
	public static final long LIFE_MS = 4_000L;

	private static volatile String name = null;
	private static volatile double wx, wy, wz;
	private static volatile long spawnedAtMs = 0L;

	private WinnerBanner() {}

	public static void spawn(String player) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) return;
		Player p = mc.player;
		Vec3 eye = p.getEyePosition();
		Vec3 look = p.getViewVector(1f);
		Vec3 anchor = eye.add(look.x * 2.5, 0.4, look.z * 2.5);
		name = player;
		wx = anchor.x;
		wy = anchor.y;
		wz = anchor.z;
		spawnedAtMs = System.currentTimeMillis();
		burstFireworks(mc, anchor);
	}

	private static void burstFireworks(Minecraft mc, Vec3 at) {
		if (mc.level == null) return;
		java.util.Random rng = new java.util.Random();
		for (int i = 0; i < 40; i++) {
			double a = rng.nextDouble() * Math.PI * 2;
			double b = (rng.nextDouble() - 0.5) * Math.PI;
			double sp = 0.25 + rng.nextDouble() * 0.35;
			double vx = Math.cos(a) * Math.cos(b) * sp;
			double vy = Math.sin(b) * sp + 0.15;
			double vz = Math.sin(a) * Math.cos(b) * sp;
			mc.level.addParticle(ParticleTypes.FIREWORK, at.x, at.y, at.z, vx, vy, vz);
		}
		for (int i = 0; i < 3; i++) {
			mc.level.addParticle(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 0, 0, 0);
		}
	}

	public static void draw(GuiGraphics ctx, Font font) {
		String n = name;
		if (n == null) return;
		long elapsed = System.currentTimeMillis() - spawnedAtMs;
		if (elapsed >= LIFE_MS) { name = null; return; }

		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null) return;

		Camera cam = mc.gameRenderer.getMainCamera();
		Vec3 camPos = cam.position();
		double relX = wx - camPos.x, relY = wy - camPos.y, relZ = wz - camPos.z;
		Vector3f v = new Vector3f((float) relX, (float) relY, (float) relZ);
		Quaternionf inv = new Quaternionf(cam.rotation()).conjugate();
		inv.transform(v);
		if (v.z >= -0.05f) return;

		double fovDeg = mc.options.fov().get();
		double halfHRad = Math.toRadians(fovDeg * 0.5);
		float halfH = (float) Math.tan(halfHRad);
		float aspect = (float) mc.getWindow().getWidth() / (float) mc.getWindow().getHeight();
		float halfW = halfH * aspect;
		float ndcX = v.x / (-v.z * halfW);
		float ndcY = v.y / (-v.z * halfH);
		int guiW = mc.getWindow().getGuiScaledWidth();
		int guiH = mc.getWindow().getGuiScaledHeight();
		int screenX = Math.round((ndcX * 0.5f + 0.5f) * guiW);
		int screenY = Math.round((-ndcY * 0.5f + 0.5f) * guiH);

		float t = elapsed / (float) LIFE_MS;
		float alpha = t < 0.75f ? 1f : Math.max(0f, 1f - (t - 0.75f) / 0.25f);
		int rainbow = rainbow(elapsed / 30f);
		int color = (rainbow & 0x00FFFFFF) | ((int) (alpha * 255) << 24);
		int shadow = 0x60000000 & ((int) (alpha * 96) << 24);

		var pose = ctx.pose();
		pose.pushMatrix();
		pose.translate((float) screenX, (float) screenY);
		float lift = Math.min(1f, t * 3f);
		pose.translate(0f, -8f * lift - 4f);
		float grow = 1.4f + 0.4f * Math.min(1f, t * 4f);
		pose.scale(grow, grow);
		int nameW = font.width(n);
		String won = "won the auction";
		int wonW = font.width(won);
		ctx.drawString(font, n, -nameW / 2, -font.lineHeight, color, true);
		int subColor = (0xFFCCCCCC & 0x00FFFFFF) | ((int) (alpha * 220) << 24);
		ctx.drawString(font, won, -wonW / 2, 2, subColor, true);
		pose.popMatrix();
	}

	private static int rainbow(float hueSeed) {
		float h = ((hueSeed % 360f) + 360f) % 360f / 360f;
		float s = 1f;
		float b = 1f;
		int i = (int) (h * 6f);
		float f = h * 6f - i;
		float p = b * (1 - s);
		float q = b * (1 - f * s);
		float tt = b * (1 - (1 - f) * s);
		float r, g, bl;
		switch (i % 6) {
			case 0 -> { r = b; g = tt; bl = p; }
			case 1 -> { r = q; g = b;  bl = p; }
			case 2 -> { r = p; g = b;  bl = tt; }
			case 3 -> { r = p; g = q;  bl = b; }
			case 4 -> { r = tt; g = p; bl = b; }
			default -> { r = b; g = p; bl = q; }
		}
		int ri = (int) (r * 255);
		int gi = (int) (g * 255);
		int bi = (int) (bl * 255);
		return 0xFF000000 | (ri << 16) | (gi << 8) | bi;
	}
}
