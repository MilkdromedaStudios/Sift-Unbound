package com.milkdromedastudios.therift.client.model;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

/** A low four-legged sculk hunter with twitching sensor tendrils. */
public class SculkProwlerModel extends EntityModel<LivingEntityRenderState> {
	private final ModelPart head;
	private final ModelPart leftTendril;
	private final ModelPart rightTendril;
	private final ModelPart[] legs = new ModelPart[4];

	public SculkProwlerModel(ModelPart root) {
		super(root);
		head = root.getChild("head");
		leftTendril = head.getChild("left_tendril");
		rightTendril = head.getChild("right_tendril");
		for (int i = 0; i < 4; i++) {
			legs[i] = root.getChild("leg" + i);
		}
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -4.0F, -8.0F, 10, 8, 16), PartPose.offset(0.0F, 12.0F, 0.0F));
		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 24).addBox(-4.0F, -4.0F, -7.0F, 8, 7, 7),
			PartPose.offset(0.0F, 11.0F, -8.0F));
		head.addOrReplaceChild("left_tendril", CubeListBuilder.create().texOffs(44, 24).addBox(-0.5F, -5.0F, -0.5F, 1, 5, 1), PartPose.offset(-3.0F, -4.0F, -3.0F));
		head.addOrReplaceChild("right_tendril", CubeListBuilder.create().texOffs(44, 24).mirror().addBox(-0.5F, -5.0F, -0.5F, 1, 5, 1),
			PartPose.offset(3.0F, -4.0F, -3.0F));
		float[][] pos = {{-3.5F, -5.5F}, {3.5F, -5.5F}, {-3.5F, 5.5F}, {3.5F, 5.5F}};
		for (int i = 0; i < 4; i++) {
			root.addOrReplaceChild("leg" + i, CubeListBuilder.create().texOffs(30, 24).addBox(-1.5F, 0.0F, -1.5F, 3, 8, 3), PartPose.offset(pos[i][0], 16.0F, pos[i][1]));
		}
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(LivingEntityRenderState state) {
		super.setupAnim(state);
		head.yRot = state.yRot * Mth.DEG_TO_RAD;
		head.xRot = state.xRot * Mth.DEG_TO_RAD;
		float walk = state.walkAnimationPos;
		float speed = state.walkAnimationSpeed;
		legs[0].xRot = Mth.cos(walk * 0.6662F) * 1.4F * speed;
		legs[3].xRot = legs[0].xRot;
		legs[1].xRot = Mth.cos(walk * 0.6662F + Mth.PI) * 1.4F * speed;
		legs[2].xRot = legs[1].xRot;
		float twitch = Mth.sin(state.ageInTicks * 0.9F) * 0.15F;
		leftTendril.zRot = -0.35F + twitch;
		rightTendril.zRot = 0.35F - twitch;
	}
}
