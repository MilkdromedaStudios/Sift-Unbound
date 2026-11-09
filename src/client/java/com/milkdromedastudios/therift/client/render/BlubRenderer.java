package com.milkdromedastudios.therift.client.render;

import com.milkdromedastudios.therift.TheRift;
import com.milkdromedastudios.therift.client.TheRiftClient;
import com.milkdromedastudios.therift.client.model.BlubModel;
import com.milkdromedastudios.therift.entity.Blub;
import com.milkdromedastudios.therift.entity.BlubVariant;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public class BlubRenderer extends MobRenderer<Blub, BlubRenderState, BlubModel> {
	private static final Map<BlubVariant, Identifier> TEXTURES = new EnumMap<>(BlubVariant.class);
	private static final Map<BlubVariant, Identifier> GLOW = new EnumMap<>(BlubVariant.class);

	static {
		for (BlubVariant v : BlubVariant.values()) {
			String name = v == BlubVariant.SOUL ? "blub" : "blub_" + v.getSerializedName();
			TEXTURES.put(v, TheRift.id("textures/entity/blub/" + name + ".png"));
			GLOW.put(v, TheRift.id("textures/entity/blub/" + name + "_glow.png"));
		}
	}

	public BlubRenderer(EntityRendererProvider.Context context) {
		super(context, new BlubModel(context.bakeLayer(TheRiftClient.BLUB_LAYER)), 0.3F);
		addLayer(new GlowLayer(this));
	}

	@Override
	public Identifier getTextureLocation(BlubRenderState state) {
		return TEXTURES.get(state.variant);
	}

	@Override
	public BlubRenderState createRenderState() {
		return new BlubRenderState();
	}

	@Override
	public void extractRenderState(Blub entity, BlubRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.variant = entity.getVariant();
	}

	@Override
	protected void scale(BlubRenderState state, PoseStack poseStack) {
		if (state.isBaby) {
			poseStack.scale(0.55F, 0.55F, 0.55F);
		}
	}

	/** The soul-engorged blub glows in the dark. */
	private static class GlowLayer extends RenderLayer<BlubRenderState, BlubModel> {
		GlowLayer(RenderLayerParent<BlubRenderState, BlubModel> parent) {
			super(parent);
		}

		@Override
		public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, BlubRenderState state, float yRot, float xRot) {
			collector.order(1).submitModel(getParentModel(), state, poseStack, RenderTypes.eyes(GLOW.get(state.variant)), light, OverlayTexture.NO_OVERLAY,
				state.outlineColor);
		}
	}
}
