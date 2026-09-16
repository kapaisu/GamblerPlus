package dev.mishka.gamblerplus.client;

import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;

public final class GamblerPlusCommands {
	private GamblerPlusCommands() {}

	public static void register() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
				dispatcher.register(ClientCommands.literal("gplus2x")
						.then(ClientCommands.argument("player", StringArgumentType.word())
								.then(ClientCommands.argument("amount", LongArgumentType.longArg(1L))
										.executes(ctx -> {
											String player = StringArgumentType.getString(ctx, "player");
											long amount = LongArgumentType.getLong(ctx, "amount");
											PaymentIntercept.openLater(new VerifyPaymentScreen(player, amount, "pay " + player + " " + amount));
											return 1;
										})))));
	}
}
