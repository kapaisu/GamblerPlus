package dev.mishka.gamblerplus.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class Keybinds {
	public static final KeyMapping.Category CATEGORY =
			KeyMapping.Category.register(Identifier.fromNamespaceAndPath("gamblerplus", "main"));

	public static KeyMapping openUi;
	public static KeyMapping toggleGambling;
	public static KeyMapping toggleHud;
	public static KeyMapping openAuction;

	private Keybinds() {}

	public static void register(Config config, Runnable openScreen, Runnable openAuctionScreen) {
		openUi = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.gamblerplus.open",
				InputConstants.Type.KEYSYM,
				InputConstants.UNKNOWN.getValue(),
				CATEGORY));

		toggleGambling = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.gamblerplus.toggle_mode",
				InputConstants.Type.KEYSYM,
				InputConstants.UNKNOWN.getValue(),
				CATEGORY));

		toggleHud = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.gamblerplus.toggle_hud",
				InputConstants.Type.KEYSYM,
				InputConstants.UNKNOWN.getValue(),
				CATEGORY));

		openAuction = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.gamblerplus.open_auction",
				InputConstants.Type.KEYSYM,
				InputConstants.UNKNOWN.getValue(),
				CATEGORY));

		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			while (openUi.consumeClick()) {
				if (Minecraft.getInstance().mouseHandler.isMouseGrabbed()) openScreen.run();
			}
			while (openAuction.consumeClick()) {
				if (Minecraft.getInstance().mouseHandler.isMouseGrabbed()) openAuctionScreen.run();
			}
			while (toggleGambling.consumeClick()) config.toggleGamblingMode();
			while (toggleHud.consumeClick())      config.toggleHudBar();
		});
	}
}
