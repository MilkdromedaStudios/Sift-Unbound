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
 * Original singer model: a tall, healthy warden. Long arms, a broad head with two curling horn-tendrils, and a soul glowing
 * in its chest. It raises its arms and head when it sings.
 */
public class SingerModel extends EntityModel<SiftMobRenderState> {
	private final ModelPart head;
	private final ModelPart leftArm;
	private final ModelPart rightArm;
	private final ModelPart leftLeg;
	private final ModelPart rightLeg;
	private final ModelPart leftHorn;
	private final ModelPart rightHorn;

	public SingerModel(ModelPart root) {
		super(root);
		head = root.getChild("head");
		leftHorn = head.getChild("left_horn");
		rightHorn = head.getChild("right_horn");
		leftArm = root.getChild("left_arm");
		rightArm = root.getChild("right_arm");
		leftLeg = root.getChild("left_leg");
		rightLeg = root.getChild("right_leg");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("torso", CubeListBuilder.create().texOffs(0, 0).addBox(-6.0F, -16.0F, -3.5F, 12, 16, 7), PartPose.offset(0.0F, 11.0F, 0.0F));
		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(38, 0).addBox(-5.0F, -10.0F, -4.0F, 10, 10, 8),
			PartPose.offset(0.0F, -5.0F, 0.0F));
		head.addOrReplaceChild("left_horn", CubeListBuilder.create().texOffs(106, 0).addBox(-1.5F, -8.0F, -0.5F, 3, 8, 1),
			PartPose.offsetAndRotation(-4.5F, -8.0F, 0.0F, 0.0F, 0.0F, -0.6F));
		head.addOrReplaceChild("right_horn", CubeListBuilder.create().texOffs(106, 0).mirror().addBox(-1.5F, -8.0F, -0.5F, 3, 8, 1),
			PartPose.offsetAndRotation(4.5F, -8.0F, 0.0F, 0.0F, 0.0F, 0.6F));
		root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(74, 0).addBox(-2.0F, 0.0F, -2.0F, 4, 18, 4), PartPose.offset(-8.0F, -3.0F, 0.0F));
		root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(74, 0).mirror().addBox(-2.0F, 0.0F, -2.0F, 4, 18, 4),
			PartPose.offset(8.0F, -3.0F, 0.0F));
		root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(90, 0).addBox(-2.0F, 0.0F, -2.0F, 4, 13, 4), PartPose.offset(-3.5F, 11.0F, 0.0F));
		root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(90, 0).mirror().addBox(-2.0F, 0.0F, -2.0F, 4, 13, 4),
			PartPose.offset(3.5F, 11.0F, 0.0F));
		return LayerDefinition.create(mesh, 128, 64);
	}

	@Override
	public void setupAnim(SiftMobRenderState state) {
		super.setupAnim(state);
		float walk = state.walkAnimationPos;
		float speed = state.walkAnimationSpeed;
		float t = state.ageInTicks;
		float sing = state.sing;
		head.yRot = state.yRot * Mth.DEG_TO_RAD;
		head.xRot = state.xRot * Mth.DEG_TO_RAD - sing * 0.45F;
		float sway = Mth.sin(t * 0.08F) * 0.05F;
		leftHorn.zRot = -0.6F + sway - sing * 0.15F;
		rightHorn.zRot = 0.6F - sway + sing * 0.15F;
		float swing = Mth.cos(walk * 0.5F) * 0.8F * speed;
		leftArm.xRot = swing - sing * 0.9F;
		rightArm.xRot = -swing - sing * 0.9F;
		leftArm.zRot = 0.05F + sway + sing * 1.1F;
		rightArm.zRot = -0.05F - sway - sing * 1.1F;
		leftLeg.xRot = -swing;
		rightLeg.xRot = swing;
	}
}
