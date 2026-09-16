package dev.mishka.gamblerplus.client;

import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;

public final class GamblerPlusCommands {
	private GamblerPlusCommands() {}

	public static void register() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
				dispatcher.register(ClientCommandManager.literal("gplus2x")
						.then(ClientCommandManager.argument("player", StringArgumentType.word())
								.then(ClientCommandManager.argument("amount", LongArgumentType.longArg(1L))
										.executes(ctx -> {
											String player = StringArgumentType.getString(ctx, "player");
											long amount = LongArgumentType.getLong(ctx, "amount");
											PaymentIntercept.openLater(new VerifyPaymentScreen(player, amount, "pay " + player + " " + amount));
											return 1;
										})))));
	}
}
