package dev.mishka.gamblerplus.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ImageStore {
	private final Path folder;
	private final Map<String, Entry> loaded = new HashMap<>();

	public record Entry(Identifier id, int width, int height) {}

	public ImageStore() {
		this.folder = FabricLoader.getInstance().getConfigDir().resolve("gamblerplus-images");
		try { Files.createDirectories(folder); } catch (Exception ignored) {}
	}

	public Path folder() { return folder; }

	public synchronized List<String> available() {
		List<String> out = new ArrayList<>();
		if (!Files.isDirectory(folder)) return out;
		try (DirectoryStream<Path> ds = Files.newDirectoryStream(folder)) {
			for (Path p : ds) {
				String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
				if (n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg")) {
					out.add(p.getFileName().toString());
				}
			}
		} catch (Exception ignored) {}
		out.sort(String::compareTo);
		return out;
	}

	public synchronized Entry get(String filename) {
		if (filename == null || filename.isEmpty()) return null;
		Entry e = loaded.get(filename);
		if (e != null) return e;
		return load(filename);
	}

	private Entry load(String filename) {
		Path p = folder.resolve(filename);
		if (!Files.isRegularFile(p)) return null;
		try (InputStream in = Files.newInputStream(p)) {
			NativeImage img = NativeImage.read(in);
			DynamicTexture tex = new DynamicTexture(() -> "gamblerplus/" + filename, img);
			String safe = filename.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "_");
			Identifier id = Identifier.fromNamespaceAndPath("gamblerplus", "user/" + safe);
			Minecraft.getInstance().getTextureManager().register(id, tex);
			Entry entry = new Entry(id, img.getWidth(), img.getHeight());
			loaded.put(filename, entry);
			return entry;
		} catch (Exception e) {
			return null;
		}
	}
}
