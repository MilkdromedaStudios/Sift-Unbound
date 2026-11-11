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
 * Original sentinel model: a lanky plant-warrior with a crest of quills along its head, a long beard of hanging roots,
 * and long arms. It shakes its head before loosing its quills.
 */
public class SentinelModel extends EntityModel<SiftMobRenderState> {
	private final ModelPart head;
	private final ModelPart crest;
	private final ModelPart leftArm;
	private final ModelPart rightArm;
	private final ModelPart leftLeg;
	private final ModelPart rightLeg;

	public SentinelModel(ModelPart root) {
		super(root);
		head = root.getChild("head");
		crest = head.getChild("crest");
		leftArm = root.getChild("left_arm");
		rightArm = root.getChild("right_arm");
		leftLeg = root.getChild("left_leg");
		rightLeg = root.getChild("right_leg");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -10.0F, -2.5F, 8, 10, 5), PartPose.offset(0.0F, 10.0F, 0.0F));
		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(26, 0).addBox(-3.5F, -7.0F, -3.5F, 7, 7, 7), PartPose.offset(0.0F, 0.0F, 0.0F));
		head.addOrReplaceChild("beard", CubeListBuilder.create().texOffs(0, 15).addBox(-3.0F, 0.0F, -0.5F, 6, 9, 1), PartPose.offsetAndRotation(0.0F, 0.0F, -3.0F, -0.1F, 0.0F, 0.0F));
		head.addOrReplaceChild("crest", CubeListBuilder.create().texOffs(14, 15).addBox(0.0F, -7.0F, -5.0F, 0, 7, 10), PartPose.offset(0.0F, -7.0F, 0.5F));
		root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(36, 15).addBox(-1.5F, 0.0F, -1.5F, 3, 13, 3), PartPose.offset(-5.5F, 1.0F, 0.0F));
		root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(36, 15).mirror().addBox(-1.5F, 0.0F, -1.5F, 3, 13, 3), PartPose.offset(5.5F, 1.0F, 0.0F));
		root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(48, 15).addBox(-1.5F, 0.0F, -1.5F, 3, 14, 3), PartPose.offset(-2.0F, 10.0F, 0.0F));
		root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(48, 15).mirror().addBox(-1.5F, 0.0F, -1.5F, 3, 14, 3), PartPose.offset(2.0F, 10.0F, 0.0F));
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(SiftMobRenderState state) {
		super.setupAnim(state);
		float walk = state.walkAnimationPos;
		float speed = state.walkAnimationSpeed;
		float swing = Mth.cos(walk * 0.6662F) * speed;
		leftLeg.xRot = swing * 1.1F;
		rightLeg.xRot = -swing * 1.1F;
		leftArm.xRot = -swing * 0.8F - state.special * 1.4F;
		rightArm.xRot = swing * 0.8F - state.special * 1.4F;
		leftArm.zRot = 0.05F;
		rightArm.zRot = -0.05F;
		float shake = state.special > 0.0F ? Mth.sin(state.ageInTicks * 2.2F) * 0.55F * state.special : 0.0F;
		head.yRot = state.yRot * Mth.DEG_TO_RAD + shake;
		head.xRot = state.xRot * Mth.DEG_TO_RAD;
		crest.zScale = 1.0F + state.special * 0.35F;
		crest.yScale = 1.0F + state.special * 0.3F;
	}
}
