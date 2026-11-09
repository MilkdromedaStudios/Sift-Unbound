package com.milkdromedastudios.siftunbound.client;

import com.milkdromedastudios.siftunbound.SiftUnbound;
import com.milkdromedastudios.siftunbound.client.model.BlubModel;
import com.milkdromedastudios.siftunbound.client.model.SculkProwlerModel;
import com.milkdromedastudios.siftunbound.client.model.SoulWispModel;
import com.milkdromedastudios.siftunbound.client.render.BlubRenderer;
import com.milkdromedastudios.siftunbound.client.render.SculkProwlerRenderer;
import com.milkdromedastudios.siftunbound.client.render.SoulWispRenderer;
import com.milkdromedastudios.siftunbound.registry.ModEntities;
import com.milkdromedastudios.siftunbound.registry.ModFluids;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;

public class SiftUnboundClient implements ClientModInitializer {
	public static final ModelLayerLocation BLUB_LAYER = new ModelLayerLocation(SiftUnbound.id("blub"), "main");
	public static final ModelLayerLocation SOUL_WISP_LAYER = new ModelLayerLocation(SiftUnbound.id("soul_wisp"), "main");
	public static final ModelLayerLocation SCULK_PROWLER_LAYER = new ModelLayerLocation(SiftUnbound.id("sculk_prowler"), "main");

	@Override
	public void onInitializeClient() {
		ModelLayerRegistry.registerModelLayer(BLUB_LAYER, BlubModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(SOUL_WISP_LAYER, SoulWispModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(SCULK_PROWLER_LAYER, SculkProwlerModel::createBodyLayer);
		EntityRendererRegistry.register(ModEntities.BLUB, BlubRenderer::new);
		EntityRendererRegistry.register(ModEntities.SOUL_WISP, SoulWispRenderer::new);
		EntityRendererRegistry.register(ModEntities.SCULK_PROWLER, SculkProwlerRenderer::new);

		FluidRenderingRegistry.register(ModFluids.SOUL_WATER, ModFluids.FLOWING_SOUL_WATER, new FluidModel.Unbaked(
			new Material(SiftUnbound.id("block/soul_water_still")),
			new Material(SiftUnbound.id("block/soul_water_flow")),
			new Material(SiftUnbound.id("block/soul_water_overlay")),
			null));
	}
}
