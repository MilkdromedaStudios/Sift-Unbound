package com.milkdromedastudios.siftunbound.client;

import com.milkdromedastudios.siftunbound.client.model.EchoGolemModel;
import com.milkdromedastudios.siftunbound.client.model.NuzzleModel;
import com.milkdromedastudios.siftunbound.client.model.SingerModel;
import com.milkdromedastudios.siftunbound.client.model.SlabberModel;
import com.milkdromedastudios.siftunbound.client.render.SiftMobRenderer;
import com.milkdromedastudios.siftunbound.entity.EchoGolem;
import com.milkdromedastudios.siftunbound.entity.Nuzzle;
import com.milkdromedastudios.siftunbound.entity.Singer;
import com.milkdromedastudios.siftunbound.entity.Slabber;
import net.minecraft.resources.Identifier;
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
	public static final ModelLayerLocation NUZZLE_LAYER = new ModelLayerLocation(SiftUnbound.id("nuzzle"), "main");
	public static final ModelLayerLocation SLABBER_LAYER = new ModelLayerLocation(SiftUnbound.id("slabber"), "main");
	public static final ModelLayerLocation ECHO_GOLEM_LAYER = new ModelLayerLocation(SiftUnbound.id("echo_golem"), "main");
	public static final ModelLayerLocation SINGER_LAYER = new ModelLayerLocation(SiftUnbound.id("singer"), "main");

	private static Identifier tex(String name) {
		return SiftUnbound.id("textures/entity/" + name + ".png");
	}

	@Override
	public void onInitializeClient() {
		ModelLayerRegistry.registerModelLayer(BLUB_LAYER, BlubModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(SOUL_WISP_LAYER, SoulWispModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(SCULK_PROWLER_LAYER, SculkProwlerModel::createBodyLayer);
		EntityRendererRegistry.register(ModEntities.BLUB, BlubRenderer::new);
		EntityRendererRegistry.register(ModEntities.SOUL_WISP, SoulWispRenderer::new);
		EntityRendererRegistry.register(ModEntities.SCULK_PROWLER, SculkProwlerRenderer::new);

		ModelLayerRegistry.registerModelLayer(NUZZLE_LAYER, NuzzleModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(SLABBER_LAYER, SlabberModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(ECHO_GOLEM_LAYER, EchoGolemModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(SINGER_LAYER, SingerModel::createBodyLayer);
		Identifier nuzzle = tex("nuzzle"), nuzzleSheared = tex("nuzzle_sheared");
		EntityRendererRegistry.register(ModEntities.NUZZLE, ctx -> new SiftMobRenderer<Nuzzle, NuzzleModel>(ctx,
			new NuzzleModel(ctx.bakeLayer(NUZZLE_LAYER)), 0.6F, 1.0F, s -> s.flag ? nuzzleSheared : nuzzle, null, (e, s) -> {
				s.flag = e.isSheared();
				s.progress = e.grazeProgress(0.5F);
			}));
		Identifier slabber = tex("slabber");
		EntityRendererRegistry.register(ModEntities.SLABBER, ctx -> new SiftMobRenderer<Slabber, SlabberModel>(ctx,
			new SlabberModel(ctx.bakeLayer(SLABBER_LAYER)), 0.7F, 1.0F, s -> slabber, null, (e, s) -> s.flag = e.isSlabberSleeping()));
		Identifier golem = tex("echo_golem"), golemGlow = tex("echo_golem_glow");
		EntityRendererRegistry.register(ModEntities.ECHO_GOLEM, ctx -> new SiftMobRenderer<EchoGolem, EchoGolemModel>(ctx,
			new EchoGolemModel(ctx.bakeLayer(ECHO_GOLEM_LAYER)), 0.6F, 1.0F, s -> golem, s -> golemGlow, (e, s) -> {
				s.progress = e.getCharge() / (float) EchoGolem.MAX_CHARGE;
				s.sing = Math.min(1.0F, e.singTicks / 10.0F);
			}));
		Identifier singer = tex("singer"), singerGlow = tex("singer_glow");
		EntityRendererRegistry.register(ModEntities.SINGER, ctx -> new SiftMobRenderer<Singer, SingerModel>(ctx,
			new SingerModel(ctx.bakeLayer(SINGER_LAYER)), 0.9F, 1.0F, s -> singer, s -> singerGlow,
			(e, s) -> s.sing = Math.min(1.0F, e.singTicks / 10.0F)));

		FluidRenderingRegistry.register(ModFluids.SOUL_WATER, ModFluids.FLOWING_SOUL_WATER, new FluidModel.Unbaked(
			new Material(SiftUnbound.id("block/soul_water_still")),
			new Material(SiftUnbound.id("block/soul_water_flow")),
			new Material(SiftUnbound.id("block/soul_water_overlay")),
			null));
	}
}
