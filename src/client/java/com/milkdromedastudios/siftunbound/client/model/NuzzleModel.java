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

/** Original nuzzle model: a fluffy body on four legs, a round head with drooping ears and a tuft, lowered to graze. */
public class NuzzleModel extends EntityModel<SiftMobRenderState> {
	private final ModelPart head;
	private final ModelPart leftEar;
	private final ModelPart rightEar;
	private final ModelPart[] legs = new ModelPart[4];

	public NuzzleModel(ModelPart root) {
		super(root);
		head = root.getChild("head");
		leftEar = head.getChild("left_ear");
		rightEar = head.getChild("right_ear");
		for (int i = 0; i < 4; i++) {
			legs[i] = root.getChild("leg" + i);
		}
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -4.0F, -6.0F, 8, 8, 12), PartPose.offset(0.0F, 13.0F, 1.0F));
		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 20).addBox(-3.0F, -3.0F, -6.0F, 6, 6, 6),
			PartPose.offset(0.0F, 10.0F, -4.0F));
		head.addOrReplaceChild("tuft", CubeListBuilder.create().texOffs(44, 20).addBox(-2.0F, -2.0F, -2.0F, 4, 2, 4), PartPose.offset(0.0F, -3.0F, -3.0F));
		head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(36, 20).addBox(-0.5F, 0.0F, -1.5F, 1, 5, 3),
			PartPose.offsetAndRotation(-3.0F, -2.0F, -2.5F, 0.0F, 0.0F, 0.55F));
		head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(36, 20).mirror().addBox(-0.5F, 0.0F, -1.5F, 1, 5, 3),
			PartPose.offsetAndRotation(3.0F, -2.0F, -2.5F, 0.0F, 0.0F, -0.55F));
		float[][] feet = {{-2.5F, -3.0F}, {2.5F, -3.0F}, {-2.5F, 5.0F}, {2.5F, 5.0F}};
		for (int i = 0; i < 4; i++) {
			root.addOrReplaceChild("leg" + i, CubeListBuilder.create().texOffs(24, 20).addBox(-1.5F, 0.0F, -1.5F, 3, 7, 3),
				PartPose.offset(feet[i][0], 17.0F, feet[i][1]));
		}
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(SiftMobRenderState state) {
		super.setupAnim(state);
		float walk = state.walkAnimationPos;
		float speed = state.walkAnimationSpeed;
		float graze = state.progress;
		head.xRot = state.xRot * Mth.DEG_TO_RAD * (1 - graze) + graze * 0.9F + (graze > 0.5F ? Mth.sin(state.ageInTicks * 0.9F) * 0.08F : 0.0F);
		head.yRot = state.yRot * Mth.DEG_TO_RAD * (1 - graze);
		head.y = 10.0F + graze * 5.0F;
		float flop = Mth.sin(walk * 0.6662F) * 0.2F * speed;
		leftEar.zRot = 0.55F + flop;
		rightEar.zRot = -0.55F - flop;
		for (int i = 0; i < 4; i++) {
			legs[i].xRot = Mth.cos(walk * 0.6662F + (i == 1 || i == 2 ? Mth.PI : 0.0F)) * 1.2F * speed;
		}
	}
}
