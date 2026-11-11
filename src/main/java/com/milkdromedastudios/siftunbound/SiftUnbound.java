package com.milkdromedastudios.siftunbound;

import com.milkdromedastudios.siftunbound.command.SiftCommand;
import com.milkdromedastudios.siftunbound.portal.NoteBlockMachine;
import com.milkdromedastudios.siftunbound.registry.ModBlocks;
import com.milkdromedastudios.siftunbound.registry.ModCreativeTab;
import com.milkdromedastudios.siftunbound.registry.ModCriteria;
import com.milkdromedastudios.siftunbound.registry.ModEffects;
import com.milkdromedastudios.siftunbound.registry.ModEntities;
import com.milkdromedastudios.siftunbound.registry.ModFeatures;
import com.milkdromedastudios.siftunbound.registry.ModFluids;
import com.milkdromedastudios.siftunbound.registry.ModGameplay;
import com.milkdromedastudios.siftunbound.registry.ModItems;
import com.milkdromedastudios.siftunbound.registry.ModPoi;
import com.milkdromedastudios.siftunbound.registry.ModSounds;
import com.milkdromedastudios.siftunbound.world.SiftTides;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Sift Unbound - an unofficial fan mod that opens the ancient city's Warden Portal into the Sift.
 * The Sift, the blub, sculk and the ancient city are Mojang Studios' creations; see CREDITS.md.
 */
public class SiftUnbound implements ModInitializer {
	public static final String MOD_ID = "sift_unbound";
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
		ModCriteria.init();
		ModFeatures.init();
		SiftCommand.init();
		SiftTides.init();
		NoteBlockMachine.init();
		LOGGER.info("The Sift Unbound loaded - the Warden Portal is listening.");
	}
}
