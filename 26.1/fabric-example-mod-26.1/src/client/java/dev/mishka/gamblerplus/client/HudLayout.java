package dev.mishka.gamblerplus.client;

import java.util.ArrayList;
import java.util.List;

public final class HudLayout {
	public static final float MIN_SCALE = 0.6f;
	public static final float MAX_SCALE = 2.4f;
	public static final float MIN_IMAGE_SCALE = 0.05f;
	public static final float MAX_IMAGE_SCALE = 15f;

	public int auctionX = 10;
	public int auctionY = 40;
	public float auctionScale = 1.0f;

	public int timerX = 10;
	public int timerY = 130;
	public float timerScale = 1.0f;

	public int graphX = 10;
	public int graphY = 150;
	public float graphScale = 1.0f;

	public int liveX = -1;
	public int liveY = -1;
	public float liveScale = 1.0f;

	public final List<TextEntry> textEntries = new ArrayList<>();
	public final List<ImageEntry> imageEntries = new ArrayList<>();

	public boolean editing = false;

	public static float clampScale(float v) {
		if (v < MIN_SCALE) return MIN_SCALE;
		if (v > MAX_SCALE) return MAX_SCALE;
		return v;
	}

	public static float clampImageScale(float v) {
		if (v < MIN_IMAGE_SCALE) return MIN_IMAGE_SCALE;
		if (v > MAX_IMAGE_SCALE) return MAX_IMAGE_SCALE;
		return v;
	}

	public static int clampByte(int v) {
		if (v < 0) return 0;
		if (v > 255) return 255;
		return v;
	}

	public static final class TextEntry {
		public String content = "";
		public int x = 10;
		public int y = 180;
		public float scale = 1.0f;
		public int r = 230;
		public int g = 230;
		public int b = 230;
		public int font = 0;

		public int argb() {
			return 0xFF000000 | (clampByte(r) << 16) | (clampByte(g) << 8) | clampByte(b);
		}
	}

	public static final class ImageEntry {
		public String file = "";
		public int x = 10;
		public int y = 220;
		public float scale = 1.0f;
	}
}
