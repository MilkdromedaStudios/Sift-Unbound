package com.milkdromedastudios.therift.client.render;

import com.milkdromedastudios.therift.TheRift;
import com.milkdromedastudios.therift.client.TheRiftClient;
import com.milkdromedastudios.therift.client.model.SculkProwlerModel;
import com.milkdromedastudios.therift.entity.SculkProwler;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public class SculkProwlerRenderer extends MobRenderer<SculkProwler, LivingEntityRenderState, SculkProwlerModel> {
	private static final Identifier TEXTURE = TheRift.id("textures/entity/sculk_prowler.png");
	private static final RenderType GLOW = RenderTypes.eyes(TheRift.id("textures/entity/sculk_prowler_glow.png"));

	public SculkProwlerRenderer(EntityRendererProvider.Context context) {
		super(context, new SculkProwlerModel(context.bakeLayer(TheRiftClient.SCULK_PROWLER_LAYER)), 0.6F);
		addLayer(new EyesLayer<>(this) {
			@Override
			public RenderType renderType() {
				return GLOW;
			}
		});
	}

	@Override
	public Identifier getTextureLocation(LivingEntityRenderState state) {
		return TEXTURE;
	}

	@Override
	public LivingEntityRenderState createRenderState() {
		return new LivingEntityRenderState();
	}
}
