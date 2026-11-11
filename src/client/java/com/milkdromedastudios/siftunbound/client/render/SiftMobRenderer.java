package com.milkdromedastudios.siftunbound.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;
import org.jspecify.annotations.Nullable;

/**
 * A renderer for the Sift's newer mobs: a texture chosen from the render state, an optional emissive (glowing) layer,
 * baby scaling, and a hook that copies entity fields into {@link SiftMobRenderState}.
 */
public class SiftMobRenderer<T extends Mob, M extends EntityModel<SiftMobRenderState>> extends MobRenderer<T, SiftMobRenderState, M> {
	private final Function<SiftMobRenderState, Identifier> texture;
	private final BiConsumer<T, SiftMobRenderState> extract;
	private final float scale;

	public SiftMobRenderer(EntityRendererProvider.Context context, M model, float shadow, float scale, Function<SiftMobRenderState, Identifier> texture,
		@Nullable Function<SiftMobRenderState, Identifier> glow, BiConsumer<T, SiftMobRenderState> extract) {
		super(context, model, shadow);
		this.texture = texture;
		this.extract = extract;
		this.scale = scale;
		if (glow != null) {
			addLayer(new GlowLayer<>(this, glow));
		}
	}

	@Override
	public Identifier getTextureLocation(SiftMobRenderState state) {
		return texture.apply(state);
	}

	@Override
	public SiftMobRenderState createRenderState() {
		return new SiftMobRenderState();
	}

	@Override
	public void extractRenderState(T entity, SiftMobRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		extract.accept(entity, state);
	}

	@Override
	protected void scale(SiftMobRenderState state, PoseStack poseStack) {
		float s = scale * (state.isBaby ? 0.5F : 1.0F);
		if (s != 1.0F) {
			poseStack.scale(s, s, s);
		}
	}

	private static final class GlowLayer<M extends EntityModel<SiftMobRenderState>> extends RenderLayer<SiftMobRenderState, M> {
		private final Function<SiftMobRenderState, Identifier> glow;

		GlowLayer(RenderLayerParent<SiftMobRenderState, M> parent, Function<SiftMobRenderState, Identifier> glow) {
			super(parent);
			this.glow = glow;
		}

		@Override
		public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, SiftMobRenderState state, float yRot, float xRot) {
			collector.order(1).submitModel(getParentModel(), state, poseStack, RenderTypes.eyes(glow.apply(state)), light, OverlayTexture.NO_OVERLAY,
				state.outlineColor);
		}
	}
}
