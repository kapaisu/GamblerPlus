package dev.mishka.gamblerplus.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;

public final class GamblerPlusCommands {
	private GamblerPlusCommands() {}

	public static void register() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
				dispatcher.register(ClientCommands.literal("gplus2x")
						.then(ClientCommands.argument("payload", StringArgumentType.greedyString())
								.executes(ctx -> {
									String payload = StringArgumentType.getString(ctx, "payload");
									int sp = payload.lastIndexOf(' ');
									if (sp < 0) return 0;
									String player = payload.substring(0, sp);
									long amount;
									try { amount = Long.parseLong(payload.substring(sp + 1)); }
									catch (NumberFormatException nfe) { return 0; }
									PaymentIntercept.openLater(new VerifyPaymentScreen(player, amount, "pay " + player + " " + amount));
									return 1;
								}))));
	}
}
