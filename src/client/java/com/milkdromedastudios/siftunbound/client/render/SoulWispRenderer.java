package com.milkdromedastudios.siftunbound.client.render;

import com.milkdromedastudios.siftunbound.SiftUnbound;
import com.milkdromedastudios.siftunbound.client.SiftUnboundClient;
import com.milkdromedastudios.siftunbound.client.model.SoulWispModel;
import com.milkdromedastudios.siftunbound.entity.SoulWisp;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

public class SoulWispRenderer extends MobRenderer<SoulWisp, LivingEntityRenderState, SoulWispModel> {
	private static final Identifier TEXTURE = SiftUnbound.id("textures/entity/soul_wisp.png");
	private static final RenderType GLOW = RenderTypes.eyes(TEXTURE);

	public SoulWispRenderer(EntityRendererProvider.Context context) {
		super(context, new SoulWispModel(context.bakeLayer(SiftUnboundClient.SOUL_WISP_LAYER)), 0.0F);
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
