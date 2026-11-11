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
 * Original scavenger model: a tall, hunched sculker whose torso is an open cage of ribs around a glowing soul, with a
 * long-jawed skull and long thin arms whose clawed hands ignite when it casts.
 */
public class ScavengerModel extends EntityModel<SiftMobRenderState> {
	private final ModelPart cage;
	private final ModelPart head;
	private final ModelPart leftArm;
	private final ModelPart rightArm;
	private final ModelPart leftLeg;
	private final ModelPart rightLeg;

	public ScavengerModel(ModelPart root) {
		super(root);
		cage = root.getChild("cage");
		head = root.getChild("head");
		leftArm = root.getChild("left_arm");
		rightArm = root.getChild("right_arm");
		leftLeg = root.getChild("left_leg");
		rightLeg = root.getChild("right_leg");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition cage = root.addOrReplaceChild("cage", CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, -13.0F, -3.0F, 9, 13, 6),
			PartPose.offsetAndRotation(0.0F, 10.0F, 0.0F, 0.22F, 0.0F, 0.0F));
		cage.addOrReplaceChild("soul", CubeListBuilder.create().texOffs(30, 0).addBox(-2.0F, -2.5F, -1.5F, 4, 5, 3), PartPose.offset(0.0F, -7.0F, 0.0F));
		root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 19).addBox(-2.5F, -5.0F, -6.0F, 5, 5, 7), PartPose.offset(0.0F, -2.0F, -2.5F));
		root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(44, 0).addBox(-1.0F, 0.0F, -1.0F, 2, 15, 2).texOffs(52, 0).addBox(-1.5F, 15.0F, -1.5F, 3, 3, 3),
			PartPose.offset(-5.5F, -0.5F, -1.5F));
		root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(44, 0).mirror().addBox(-1.0F, 0.0F, -1.0F, 2, 15, 2).texOffs(52, 0).mirror()
			.addBox(-1.5F, 15.0F, -1.5F, 3, 3, 3), PartPose.offset(5.5F, -0.5F, -1.5F));
		root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(24, 19).addBox(-1.5F, 0.0F, -1.5F, 3, 14, 3), PartPose.offset(-2.5F, 10.0F, 0.0F));
		root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(24, 19).mirror().addBox(-1.5F, 0.0F, -1.5F, 3, 14, 3), PartPose.offset(2.5F, 10.0F, 0.0F));
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(SiftMobRenderState state) {
		super.setupAnim(state);
		float walk = state.walkAnimationPos;
		float speed = state.walkAnimationSpeed;
		float swing = Mth.cos(walk * 0.6662F) * speed;
		leftLeg.xRot = swing * 1.0F;
		rightLeg.xRot = -swing * 1.0F;
		float sway = Mth.sin(state.ageInTicks * 0.08F) * 0.05F;
		leftArm.xRot = -swing * 0.6F + sway - state.attack * 1.5F;
		rightArm.xRot = swing * 0.6F - sway - state.attack * 1.5F;
		leftArm.zRot = 0.1F + state.attack * 0.25F;
		rightArm.zRot = -0.1F - state.attack * 0.25F;
		cage.xRot = 0.22F - state.special * 0.35F;
		head.yRot = state.yRot * Mth.DEG_TO_RAD;
		head.xRot = state.xRot * Mth.DEG_TO_RAD - state.special * 0.3F;
	}
}
