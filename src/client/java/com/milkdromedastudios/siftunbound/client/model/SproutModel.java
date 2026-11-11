package com.milkdromedastudios.siftunbound.client.model;

import com.milkdromedastudios.siftunbound.client.render.SiftMobRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Original sprout model: a squid-like bud of a mantle with a frilled collar, drifting above six dangling, two-jointed
 * tendrils. Used at twice the size for the Harmonizer.
 */
public class SproutModel extends EntityModel<SiftMobRenderState> {
	private static final int TENDRILS = 6;
	private final ModelPart mantle;
	private final ModelPart[] upper = new ModelPart[TENDRILS];
	private final ModelPart[] lower = new ModelPart[TENDRILS];

	public SproutModel(ModelPart root) {
		super(root);
		mantle = root.getChild("mantle");
		for (int i = 0; i < TENDRILS; i++) {
			upper[i] = mantle.getChild("tendril" + i);
			lower[i] = upper[i].getChild("tip");
		}
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition mantle = root.addOrReplaceChild("mantle", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -9.0F, -4.0F, 8, 9, 8)
			.texOffs(0, 17).addBox(-5.0F, -3.0F, -5.0F, 10, 1, 10), PartPose.offset(0.0F, 4.0F, 0.0F));
		for (int i = 0; i < TENDRILS; i++) {
			float a = i * Mth.TWO_PI / TENDRILS;
			PartDefinition t = mantle.addOrReplaceChild("tendril" + i, CubeListBuilder.create().texOffs(32, 0).addBox(-0.75F, 0.0F, -0.75F, 1.5F, 8.0F, 1.5F),
				PartPose.offset(Mth.cos(a) * 2.6F, 0.0F, Mth.sin(a) * 2.6F));
			t.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(38, 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 8.0F, 1.0F), PartPose.offset(0.0F, 8.0F, 0.0F));
		}
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(SiftMobRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks;
		float speed = state.walkAnimationSpeed;
		mantle.y = 4.0F + Mth.sin(t * 0.09F) * 1.0F;
		mantle.yRot = state.extra * t * 0.35F;
		float pulse = 1.0F + state.special * 0.12F * Mth.sin(t * 0.8F);
		mantle.xScale = pulse;
		mantle.zScale = pulse;
		for (int i = 0; i < TENDRILS; i++) {
			float a = i * Mth.TWO_PI / TENDRILS;
			float wave = Mth.sin(t * 0.15F + i * 1.1F);
			float spread = 0.12F + speed * 0.25F + state.extra * 0.9F;
			upper[i].xRot = Mth.sin(a) * spread + wave * 0.12F;
			upper[i].zRot = -Mth.cos(a) * spread + Mth.cos(t * 0.13F + i) * 0.1F;
			lower[i].xRot = wave * 0.25F;
			float retract = 1.0F - state.special * 0.4F;
			upper[i].yScale = retract;
			lower[i].yScale = retract;
		}
		upper[0].xRot -= state.attack * 2.2F; // the whip (or the harmonizer's slap)
		lower[0].xRot -= state.attack * 0.6F;
	}
}
