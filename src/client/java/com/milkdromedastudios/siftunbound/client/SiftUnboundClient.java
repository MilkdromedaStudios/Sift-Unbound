package com.milkdromedastudios.siftunbound.client;

import com.milkdromedastudios.siftunbound.client.model.EchoGolemModel;
import com.milkdromedastudios.siftunbound.client.model.NesterModel;
import com.milkdromedastudios.siftunbound.client.model.PollinatorModel;
import com.milkdromedastudios.siftunbound.client.model.ScavengerModel;
import com.milkdromedastudios.siftunbound.client.model.SeedlingModel;
import com.milkdromedastudios.siftunbound.client.model.SentinelModel;
import com.milkdromedastudios.siftunbound.client.model.SproutModel;
import com.milkdromedastudios.siftunbound.entity.Scavenger;
import com.milkdromedastudios.siftunbound.entity.sifter.Harmonizer;
import com.milkdromedastudios.siftunbound.entity.sifter.Nester;
import com.milkdromedastudios.siftunbound.entity.sifter.Pollinator;
import com.milkdromedastudios.siftunbound.entity.sifter.Seedling;
import com.milkdromedastudios.siftunbound.entity.sifter.Sentinel;
import com.milkdromedastudios.siftunbound.entity.sifter.Sprout;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.util.Mth;
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
	public static final ModelLayerLocation SEEDLING_LAYER = new ModelLayerLocation(SiftUnbound.id("seedling"), "main");
	public static final ModelLayerLocation NESTER_LAYER = new ModelLayerLocation(SiftUnbound.id("nester"), "main");
	public static final ModelLayerLocation POLLINATOR_LAYER = new ModelLayerLocation(SiftUnbound.id("pollinator"), "main");
	public static final ModelLayerLocation SENTINEL_LAYER = new ModelLayerLocation(SiftUnbound.id("sentinel"), "main");
	public static final ModelLayerLocation SPROUT_LAYER = new ModelLayerLocation(SiftUnbound.id("sprout"), "main");
	public static final ModelLayerLocation SCAVENGER_LAYER = new ModelLayerLocation(SiftUnbound.id("scavenger"), "main");

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

		registerSifters();

		FluidRenderingRegistry.register(ModFluids.SOUL_WATER, ModFluids.FLOWING_SOUL_WATER, new FluidModel.Unbaked(
			new Material(SiftUnbound.id("block/soul_water_still")),
			new Material(SiftUnbound.id("block/soul_water_flow")),
			new Material(SiftUnbound.id("block/soul_water_overlay")),
			null));
	}

	/** Phase 3: the sifters, the scavenger and their projectiles. */
	private static void registerSifters() {
		ModelLayerRegistry.registerModelLayer(SEEDLING_LAYER, SeedlingModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(NESTER_LAYER, NesterModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(POLLINATOR_LAYER, PollinatorModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(SENTINEL_LAYER, SentinelModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(SPROUT_LAYER, SproutModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(SCAVENGER_LAYER, ScavengerModel::createBodyLayer);
		Identifier seedling = tex("seedling"), seedlingRage = tex("seedling_rage");
		EntityRendererRegistry.register(ModEntities.SEEDLING, ctx -> new SiftMobRenderer<Seedling, SeedlingModel>(ctx,
			new SeedlingModel(ctx.bakeLayer(SEEDLING_LAYER)), 0.35F, 1.0F, s -> seedling, s -> s.enraged ? seedlingRage : null, (e, s) -> {
				s.enraged = e.isEnraged();
				s.attack = e.biteTicks / 8.0F;
			}));
		Identifier nester = tex("nester"), nesterRage = tex("nester_rage");
		EntityRendererRegistry.register(ModEntities.NESTER, ctx -> new SiftMobRenderer<Nester, NesterModel>(ctx,
			new NesterModel(ctx.bakeLayer(NESTER_LAYER)), 0.6F, 1.0F, s -> nester, s -> s.enraged ? nesterRage : null, (e, s) -> {
				s.enraged = e.isEnraged();
				s.attack = e.biteTicks / 10.0F;
			}));
		Identifier pollinator = tex("pollinator"), pollinatorRage = tex("pollinator_rage");
		EntityRendererRegistry.register(ModEntities.POLLINATOR, ctx -> new SiftMobRenderer<Pollinator, PollinatorModel>(ctx,
			new PollinatorModel(ctx.bakeLayer(POLLINATOR_LAYER)), 0.55F, 1.0F, s -> pollinator, s -> s.enraged ? pollinatorRage : null, (e, s) -> {
				s.enraged = e.isEnraged();
				s.special = Math.min(1.0F, e.puffTicks / 6.0F);
				s.attack = e.tongueTicks / 8.0F;
			}));
		Identifier sentinel = tex("sentinel"), sentinelRage = tex("sentinel_rage");
		EntityRendererRegistry.register(ModEntities.SENTINEL, ctx -> new SiftMobRenderer<Sentinel, SentinelModel>(ctx,
			new SentinelModel(ctx.bakeLayer(SENTINEL_LAYER)), 0.5F, 1.0F, s -> sentinel, s -> s.enraged ? sentinelRage : null, (e, s) -> {
				s.enraged = e.isEnraged();
				s.special = Math.min(1.0F, e.shakeTicks / 4.0F);
			}));
		Identifier sprout = tex("sprout"), sproutGlow = tex("sprout_glow"), sproutRage = tex("sprout_rage");
		EntityRendererRegistry.register(ModEntities.SPROUT, ctx -> new SiftMobRenderer<Sprout, SproutModel>(ctx,
			new SproutModel(ctx.bakeLayer(SPROUT_LAYER)), 0.45F, 1.0F, s -> sprout,
			s -> s.enraged ? sproutRage : s.flag || s.special > 0 ? sproutGlow : null, (e, s) -> {
				s.enraged = e.isEnraged();
				s.flag = e.isAggressive();
				s.attack = e.whipTicks / 10.0F;
				s.special = e.isCasting() ? 1.0F : 0.0F;
				s.glowTint = e.isCasting() && !s.enraged ? Mth.hsvToArgb((s.ageInTicks * 0.05F) % 1.0F, 0.7F, 1.0F, 255) : -1; // flashing hues
			}));
		Identifier harmonizer = tex("harmonizer"), harmonizerGlow = tex("harmonizer_glow"), harmonizerRage = tex("harmonizer_rage");
		EntityRendererRegistry.register(ModEntities.HARMONIZER, ctx -> new SiftMobRenderer<Harmonizer, SproutModel>(ctx,
			new SproutModel(ctx.bakeLayer(SPROUT_LAYER)), 1.2F, 2.0F, s -> harmonizer, s -> s.enraged ? harmonizerRage : harmonizerGlow, (e, s) -> {
				s.enraged = e.isEnraged();
				s.attack = e.slapTicks / 12.0F;
				s.special = e.roarTicks > 0 ? 1.0F : 0.0F;
				s.extra = Math.min(1.0F, e.vortexTicks / 10.0F);
			}));
		Identifier scavenger = tex("scavenger"), scavengerGlow = tex("scavenger_glow");
		EntityRendererRegistry.register(ModEntities.SCAVENGER, ctx -> new SiftMobRenderer<Scavenger, ScavengerModel>(ctx,
			new ScavengerModel(ctx.bakeLayer(SCAVENGER_LAYER)), 0.6F, 1.0F, s -> scavenger, s -> scavengerGlow, (e, s) -> {
				s.attack = Math.min(1.0F, e.castTicks / 5.0F);
				s.special = e.dashTicks / 10.0F;
			}));
		EntityRendererRegistry.register(ModEntities.POLLEN_GLOB, ctx -> new ThrownItemRenderer<>(ctx, 1.0F, false));
		EntityRendererRegistry.register(ModEntities.SENTINEL_QUILL, ctx -> new ThrownItemRenderer<>(ctx, 0.8F, false));
		EntityRendererRegistry.register(ModEntities.SOUL_BOLT, ctx -> new ThrownItemRenderer<>(ctx, 1.2F, true));
	}
}
