package com.milkdromedastudios.siftunbound.client.model;

import com.milkdromedastudios.siftunbound.client.render.BlubRenderState;
import com.milkdromedastudios.siftunbound.entity.BlubVariant;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Original blub model: a jelly cube on four stubby feet with two upright ears (and an antenna light on meadows blubs).
 * Texture layout documented in tools/texture_pipeline/gen_textures.py (gen_blub).
 */
public class BlubModel extends EntityModel<BlubRenderState> {
	private final ModelPart body;
	private final ModelPart leftEar;
	private final ModelPart rightEar;
	private final ModelPart antenna;
	private final ModelPart[] legs = new ModelPart[4];

	public BlubModel(ModelPart root) {
		super(root);
		body = root.getChild("body");
		leftEar = body.getChild("left_ear");
		rightEar = body.getChild("right_ear");
		antenna = body.getChild("antenna");
		for (int i = 0; i < 4; i++) {
			legs[i] = root.getChild("leg" + i);
		}
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -7.0F, -4.0F, 8, 7, 8), PartPose.offset(0.0F, 22.0F, 0.0F));
		body.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(32, 0).addBox(-1.0F, -4.0F, -0.5F, 2, 4, 1), PartPose.offset(-2.0F, -7.0F, 0.5F));
		body.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(38, 0).addBox(-1.0F, -4.0F, -0.5F, 2, 4, 1), PartPose.offset(2.0F, -7.0F, 0.5F));
		PartDefinition antenna = body.addOrReplaceChild("antenna", CubeListBuilder.create().texOffs(44, 0).addBox(-0.5F, -5.0F, -0.5F, 1, 5, 1),
			PartPose.offsetAndRotation(1.5F, -7.0F, 2.5F, -0.35F, 0.0F, 0.0F));
		antenna.addOrReplaceChild("bulb", CubeListBuilder.create().texOffs(48, 0).addBox(-1.0F, -2.0F, -1.0F, 2, 2, 2), PartPose.offset(0.0F, -5.0F, 0.0F));
		float[][] feet = {{-2.5F, -2.5F}, {2.5F, -2.5F}, {-2.5F, 2.5F}, {2.5F, 2.5F}};
		for (int i = 0; i < 4; i++) {
			root.addOrReplaceChild("leg" + i, CubeListBuilder.create().texOffs(0, 15).addBox(-1.0F, 0.0F, -1.0F, 2, 2, 2),
				PartPose.offset(feet[i][0], 22.0F, feet[i][1]));
		}
		return LayerDefinition.create(mesh, 64, 32);
	}

	@Override
	public void setupAnim(BlubRenderState state) {
		super.setupAnim(state);
		float walk = state.walkAnimationPos;
		float speed = Math.min(1.0F, state.walkAnimationSpeed * 1.8F);
		float t = state.ageInTicks;
		// squishy hop: body bobs and squashes as it walks, and jiggles gently at rest
		float hop = Math.abs(Mth.sin(walk * 0.55F)) * speed;
		body.y = 22.0F - hop * 2.0F;
		float jiggle = 1.0F + Mth.sin(t * 0.25F) * 0.025F + hop * 0.06F;
		body.xScale = jiggle;
		body.zScale = jiggle;
		body.yScale = 2.0F - jiggle;
		body.yRot = state.yRot * Mth.DEG_TO_RAD * 0.3F;
		leftEar.zRot = -0.12F - Mth.sin(t * 0.2F) * 0.08F - hop * 0.3F;
		rightEar.zRot = 0.12F + Mth.sin(t * 0.2F + 1.0F) * 0.08F + hop * 0.3F;
		leftEar.xRot = rightEar.xRot = -hop * 0.4F;
		antenna.visible = state.variant == BlubVariant.MEADOWS;
		antenna.zRot = Mth.sin(t * 0.15F) * 0.12F;
		for (int i = 0; i < 4; i++) {
			legs[i].xRot = Mth.cos(walk * 1.2F + (i % 2 == 0 ? 0 : Mth.PI)) * 1.1F * speed;
			legs[i].y = 22.0F - hop * 1.5F;
		}
	}
}
