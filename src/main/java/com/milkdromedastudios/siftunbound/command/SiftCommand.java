package com.milkdromedastudios.siftunbound.command;

import com.milkdromedastudios.siftunbound.portal.SiftTravel;
import com.milkdromedastudios.siftunbound.world.SiftTides;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/** {@code /sift credits} and {@code /sift tide}. */
public final class SiftCommand {
	public static void init() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
			Commands.literal("sift")
				.then(Commands.literal("credits").executes(ctx -> {
					ctx.getSource().sendSuccess(() -> Component.translatable("commands.sift_unbound.credits"), false);
					return 1;
				}))
				.then(Commands.literal("tide").executes(ctx -> {
					var level = ctx.getSource().getServer().getLevel(SiftTravel.THE_SIFT);
					if (level == null) {
						return 0;
					}
					SiftTides.Tide tide = SiftTides.current(level);
					ctx.getSource().sendSuccess(() -> Component.translatable("message.sift_unbound.tide", Component.translatable(tide.translationKey())), false);
					return tide.ordinal() + 1;
				}))
		));
	}

	private SiftCommand() {
	}
}
