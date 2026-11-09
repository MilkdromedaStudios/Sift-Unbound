package com.milkdromedastudios.therift.client;

import com.milkdromedastudios.therift.TheRift;
import com.milkdromedastudios.therift.client.model.BlubModel;
import com.milkdromedastudios.therift.client.model.SculkProwlerModel;
import com.milkdromedastudios.therift.client.model.SoulWispModel;
import com.milkdromedastudios.therift.client.render.BlubRenderer;
import com.milkdromedastudios.therift.client.render.SculkProwlerRenderer;
import com.milkdromedastudios.therift.client.render.SoulWispRenderer;
import com.milkdromedastudios.therift.registry.ModEntities;
import com.milkdromedastudios.therift.registry.ModFluids;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;

public class TheRiftClient implements ClientModInitializer {
	public static final ModelLayerLocation BLUB_LAYER = new ModelLayerLocation(TheRift.id("blub"), "main");
	public static final ModelLayerLocation SOUL_WISP_LAYER = new ModelLayerLocation(TheRift.id("soul_wisp"), "main");
	public static final ModelLayerLocation SCULK_PROWLER_LAYER = new ModelLayerLocation(TheRift.id("sculk_prowler"), "main");

	@Override
	public void onInitializeClient() {
		ModelLayerRegistry.registerModelLayer(BLUB_LAYER, BlubModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(SOUL_WISP_LAYER, SoulWispModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(SCULK_PROWLER_LAYER, SculkProwlerModel::createBodyLayer);
		EntityRendererRegistry.register(ModEntities.BLUB, BlubRenderer::new);
		EntityRendererRegistry.register(ModEntities.SOUL_WISP, SoulWispRenderer::new);
		EntityRendererRegistry.register(ModEntities.SCULK_PROWLER, SculkProwlerRenderer::new);

		FluidRenderingRegistry.register(ModFluids.SOUL_WATER, ModFluids.FLOWING_SOUL_WATER, new FluidModel.Unbaked(
			new Material(TheRift.id("block/soul_water_still")),
			new Material(TheRift.id("block/soul_water_flow")),
			new Material(TheRift.id("block/soul_water_overlay")),
			null));
	}
}
