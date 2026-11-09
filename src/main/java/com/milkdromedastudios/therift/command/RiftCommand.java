package com.milkdromedastudios.therift.command;

import com.milkdromedastudios.therift.portal.SiftTravel;
import com.milkdromedastudios.therift.world.SiftTides;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/** {@code /rift credits} and {@code /rift tide}. */
public final class RiftCommand {
	public static void init() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
			Commands.literal("rift")
				.then(Commands.literal("credits").executes(ctx -> {
					ctx.getSource().sendSuccess(() -> Component.translatable("commands.the_rift.credits"), false);
					return 1;
				}))
				.then(Commands.literal("tide").executes(ctx -> {
					var level = ctx.getSource().getServer().getLevel(SiftTravel.THE_SIFT);
					if (level == null) {
						return 0;
					}
					SiftTides.Tide tide = SiftTides.current(level);
					ctx.getSource().sendSuccess(() -> Component.translatable("message.the_rift.tide", Component.translatable(tide.translationKey())), false);
					return tide.ordinal() + 1;
				}))
		));
	}

	private RiftCommand() {
	}
}
