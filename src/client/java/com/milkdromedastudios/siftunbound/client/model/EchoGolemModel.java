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

/** Original echo golem model: a squishy body with long arms and short legs, a glowing soul block for a head. */
public class EchoGolemModel extends EntityModel<SiftMobRenderState> {
	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart leftArm;
	private final ModelPart rightArm;
	private final ModelPart leftLeg;
	private final ModelPart rightLeg;

	public EchoGolemModel(ModelPart root) {
		super(root);
		body = root.getChild("body");
		head = root.getChild("head");
		leftArm = root.getChild("left_arm");
		rightArm = root.getChild("right_arm");
		leftLeg = root.getChild("left_leg");
		rightLeg = root.getChild("right_leg");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -10.0F, -4.0F, 10, 10, 8), PartPose.offset(0.0F, 18.0F, 0.0F));
		root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(36, 0).addBox(-3.0F, -6.0F, -3.0F, 6, 6, 6), PartPose.offset(0.0F, 8.0F, 0.0F));
		root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(0, 18).addBox(-1.5F, 0.0F, -1.5F, 3, 10, 3), PartPose.offset(-6.5F, 9.0F, 0.0F));
		root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(0, 18).mirror().addBox(-1.5F, 0.0F, -1.5F, 3, 10, 3),
			PartPose.offset(6.5F, 9.0F, 0.0F));
		root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(12, 18).addBox(-1.5F, 0.0F, -1.5F, 3, 6, 3), PartPose.offset(-2.5F, 18.0F, 0.0F));
		root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(12, 18).mirror().addBox(-1.5F, 0.0F, -1.5F, 3, 6, 3),
			PartPose.offset(2.5F, 18.0F, 0.0F));
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(SiftMobRenderState state) {
		super.setupAnim(state);
		float walk = state.walkAnimationPos;
		float speed = state.walkAnimationSpeed;
		float t = state.ageInTicks;
		float squish = 1.0F + Mth.sin(t * 0.18F) * 0.03F + Math.abs(Mth.sin(walk * 0.6F)) * 0.05F * speed;
		body.xScale = squish;
		body.zScale = squish;
		body.yScale = 2.0F - squish;
		head.y = 8.0F + (1.0F - body.yScale) * 10.0F;
		head.yRot = state.yRot * Mth.DEG_TO_RAD;
		head.xRot = state.xRot * Mth.DEG_TO_RAD - state.sing * 0.3F;
		float swing = Mth.cos(walk * 0.6662F) * 1.0F * speed;
		leftArm.xRot = swing - state.sing * 2.6F;
		rightArm.xRot = -swing - state.sing * 2.6F;
		leftArm.zRot = 0.08F + state.sing * 0.3F;
		rightArm.zRot = -0.08F - state.sing * 0.3F;
		leftLeg.xRot = -swing * 1.2F;
		rightLeg.xRot = swing * 1.2F;
	}
}
