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
 * Original pollinator model: a squat, frog-like sifter with a wide flat head, bulging eyes, a throat sac that puffs up
 * before it spits, a long tongue, folded hind legs, and tufts of pollen-laden flowers on its back.
 */
public class PollinatorModel extends EntityModel<SiftMobRenderState> {
	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart throat;
	private final ModelPart tongue;
	private final ModelPart leftHind;
	private final ModelPart rightHind;
	private final ModelPart leftFront;
	private final ModelPart rightFront;

	public PollinatorModel(ModelPart root) {
		super(root);
		body = root.getChild("body");
		head = root.getChild("head");
		throat = head.getChild("throat");
		tongue = head.getChild("tongue");
		leftHind = root.getChild("left_hind_leg");
		rightHind = root.getChild("right_hind_leg");
		leftFront = root.getChild("left_front_leg");
		rightFront = root.getChild("right_front_leg");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -5.0F, -5.0F, 10, 5, 11), PartPose.offset(0.0F, 22.0F, 1.0F));
		body.addOrReplaceChild("flowers", CubeListBuilder.create().texOffs(42, 0).addBox(0.0F, -5.0F, -3.0F, 0, 5, 6).texOffs(42, 11).addBox(-3.0F, -5.0F, 0.0F, 6, 5, 0),
			PartPose.offset(0.0F, -5.0F, 1.5F));
		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 16).addBox(-5.5F, -4.0F, -6.0F, 11, 4, 6)
			.texOffs(34, 16).addBox(-5.0F, -6.0F, -5.0F, 3, 2, 3).texOffs(34, 16).mirror().addBox(2.0F, -6.0F, -5.0F, 3, 2, 3),
			PartPose.offset(0.0F, 18.5F, -3.5F));
		head.addOrReplaceChild("throat", CubeListBuilder.create().texOffs(0, 26).addBox(-3.5F, 0.0F, -2.5F, 7, 3, 5), PartPose.offset(0.0F, 0.0F, -3.0F));
		head.addOrReplaceChild("tongue", CubeListBuilder.create().texOffs(24, 26).addBox(-1.0F, -0.5F, -12.0F, 2, 1, 12), PartPose.offset(0.0F, -0.5F, -5.5F));
		root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create().texOffs(0, 34).addBox(-1.5F, 0.0F, -3.0F, 3, 4, 6), PartPose.offset(-5.5F, 20.0F, 4.0F));
		root.addOrReplaceChild("right_hind_leg", CubeListBuilder.create().texOffs(0, 34).mirror().addBox(-1.5F, 0.0F, -3.0F, 3, 4, 6), PartPose.offset(5.5F, 20.0F, 4.0F));
		root.addOrReplaceChild("left_front_leg", CubeListBuilder.create().texOffs(18, 34).addBox(-1.0F, 0.0F, -1.0F, 2, 4, 2), PartPose.offset(-4.0F, 20.0F, -3.0F));
		root.addOrReplaceChild("right_front_leg", CubeListBuilder.create().texOffs(18, 34).mirror().addBox(-1.0F, 0.0F, -1.0F, 2, 4, 2), PartPose.offset(4.0F, 20.0F, -3.0F));
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(SiftMobRenderState state) {
		super.setupAnim(state);
		float walk = state.walkAnimationPos;
		float speed = state.walkAnimationSpeed;
		float hop = Math.max(0.0F, Mth.sin(walk * 0.8F)) * speed;
		body.y = 22.0F - hop * 3.0F;
		head.y = 18.5F - hop * 3.0F;
		leftHind.xRot = rightHind.xRot = -hop * 1.2F;
		leftFront.xRot = rightFront.xRot = hop * 0.8F;
		head.yRot = state.yRot * Mth.DEG_TO_RAD * 0.4F;
		head.xRot = state.xRot * Mth.DEG_TO_RAD * 0.4F - state.special * 0.25F;
		float puff = 1.0F + state.special * 0.9F + Mth.sin(state.ageInTicks * 0.15F) * 0.04F;
		throat.xScale = puff;
		throat.yScale = puff;
		throat.zScale = puff;
		tongue.visible = state.attack > 0.0F;
		tongue.zScale = Mth.sin(state.attack * Mth.PI) * 1.0F + 0.01F;
	}
}
