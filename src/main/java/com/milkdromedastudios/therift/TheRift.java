package com.milkdromedastudios.therift;

import com.milkdromedastudios.therift.command.RiftCommand;
import com.milkdromedastudios.therift.registry.ModBlocks;
import com.milkdromedastudios.therift.registry.ModCreativeTab;
import com.milkdromedastudios.therift.registry.ModEffects;
import com.milkdromedastudios.therift.registry.ModEntities;
import com.milkdromedastudios.therift.registry.ModFluids;
import com.milkdromedastudios.therift.registry.ModGameplay;
import com.milkdromedastudios.therift.registry.ModItems;
import com.milkdromedastudios.therift.registry.ModPoi;
import com.milkdromedastudios.therift.registry.ModSounds;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Rift - an unofficial fan mod that opens the ancient city's Warden Portal into the Sift.
 * The Sift, the blub, sculk and the ancient city are Mojang Studios' creations; see CREDITS.md.
 */
public class TheRift implements ModInitializer {
	public static final String MOD_ID = "the_rift";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		ModSounds.init();
		ModFluids.init();
		ModBlocks.init();
		ModEffects.init();
		ModEntities.init();
		ModItems.init();
		ModPoi.init();
		ModCreativeTab.init();
		ModGameplay.init();
		RiftCommand.init();
		LOGGER.info("The Rift loaded - the Warden Portal is listening.");
	}
}
