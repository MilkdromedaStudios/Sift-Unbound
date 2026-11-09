package com.milkdromedastudios.therift.client.render;

import com.milkdromedastudios.therift.TheRift;
import com.milkdromedastudios.therift.client.TheRiftClient;
import com.milkdromedastudios.therift.client.model.SoulWispModel;
import com.milkdromedastudios.therift.entity.SoulWisp;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

public class SoulWispRenderer extends MobRenderer<SoulWisp, LivingEntityRenderState, SoulWispModel> {
	private static final Identifier TEXTURE = TheRift.id("textures/entity/soul_wisp.png");
	private static final RenderType GLOW = RenderTypes.eyes(TEXTURE);

	public SoulWispRenderer(EntityRendererProvider.Context context) {
		super(context, new SoulWispModel(context.bakeLayer(TheRiftClient.SOUL_WISP_LAYER)), 0.0F);
		addLayer(new EyesLayer<>(this) {
			@Override
			public RenderType renderType() {
				return GLOW;
			}
		});
	}

	@Override
	protected int getBlockLightLevel(SoulWisp entity, BlockPos pos) {
		return 15;
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
