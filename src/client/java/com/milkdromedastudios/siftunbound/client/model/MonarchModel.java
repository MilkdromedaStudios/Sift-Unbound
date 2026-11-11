package com.milkdromedastudios.siftunbound.client.model;

import com.milkdromedastudios.siftunbound.client.render.SiftMobRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

/**
 * Original Monarch model: a tall, gaunt sculker on long legs, a crown of sculk horns on its skull, a mantle of feathers
 * hanging from its shoulders and very long arms ending in three long claws. Drawn translucent for the Monarch Echo.
 */
public class MonarchModel extends EntityModel<SiftMobRenderState> {
	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart mantle;
	private final ModelPart leftArm;
	private final ModelPart rightArm;
	private final ModelPart leftLeg;
	private final ModelPart rightLeg;

	public MonarchModel(ModelPart root, boolean spectral) {
		super(root, spectral ? RenderTypes::entityTranslucent : RenderTypes::entityCutout);
		body = root.getChild("body");
		head = body.getChild("head");
		mantle = body.getChild("mantle");
		leftArm = body.getChild("left_arm");
		rightArm = body.getChild("right_arm");
		leftLeg = root.getChild("left_leg");
		rightLeg = root.getChild("right_leg");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -16.0F, -3.0F, 10, 16, 6),
			PartPose.offset(0.0F, 2.0F, 0.0F));
		PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(32, 0).addBox(-3.5F, -7.0F, -4.5F, 7, 7, 8)
			.texOffs(62, 0).addBox(-3.5F, -12.0F, -1.0F, 7, 5, 0), PartPose.offset(0.0F, -16.0F, -0.5F));
		head.addOrReplaceChild("crown_left", CubeListBuilder.create().texOffs(62, 6).addBox(0.0F, -5.0F, -1.5F, 0, 5, 3),
			PartPose.offsetAndRotation(-3.0F, -7.0F, 0.0F, 0.0F, 0.0F, -0.35F));
		head.addOrReplaceChild("crown_right", CubeListBuilder.create().texOffs(62, 6).mirror().addBox(0.0F, -5.0F, -1.5F, 0, 5, 3),
			PartPose.offsetAndRotation(3.0F, -7.0F, 0.0F, 0.0F, 0.0F, 0.35F));
		body.addOrReplaceChild("mantle", CubeListBuilder.create().texOffs(64, 16).addBox(-7.0F, 0.0F, 0.0F, 14, 20, 0),
			PartPose.offsetAndRotation(0.0F, -15.0F, 3.2F, 0.12F, 0.0F, 0.0F));
		for (int side = -1; side <= 1; side += 2) {
			PartDefinition arm = body.addOrReplaceChild(side < 0 ? "left_arm" : "right_arm", CubeListBuilder.create().texOffs(0, 22).mirror(side > 0)
				.addBox(-1.5F, 0.0F, -1.5F, 3, 22, 3), PartPose.offset(side * 6.5F, -15.0F, 0.0F));
			for (int c = -1; c <= 1; c++) { // three long claws
				arm.addOrReplaceChild("claw" + (c + 1), CubeListBuilder.create().texOffs(12, 22).addBox(-0.5F, 0.0F, -0.5F, 1, 9, 1),
					PartPose.offsetAndRotation(c * 1.0F, 22.0F, -0.5F, -0.25F, 0.0F, c * 0.18F));
			}
		}
		root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(16, 22).addBox(-1.5F, 0.0F, -1.5F, 3, 22, 3), PartPose.offset(-2.5F, 2.0F, 0.0F));
		root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(16, 22).mirror().addBox(-1.5F, 0.0F, -1.5F, 3, 22, 3), PartPose.offset(2.5F, 2.0F, 0.0F));
		return LayerDefinition.create(mesh, 128, 64);
	}

	@Override
	public void setupAnim(SiftMobRenderState state) {
		super.setupAnim(state);
		float walk = state.walkAnimationPos;
		float speed = state.walkAnimationSpeed;
		float stride = Mth.cos(walk * 0.45F) * speed; // long, slow strides
		leftLeg.xRot = stride * 0.9F;
		rightLeg.xRot = -stride * 0.9F;
		float sway = Mth.sin(state.ageInTicks * 0.06F) * 0.06F;
		leftArm.xRot = -stride * 0.5F + sway - state.special * 2.4F;
		rightArm.xRot = stride * 0.5F - sway - state.special * 2.4F - state.attack * 1.8F;
		leftArm.zRot = 0.08F + state.special * 0.6F;
		rightArm.zRot = -0.08F - state.special * 0.6F;
		body.xRot = 0.08F + state.attack * 0.35F;
		body.yRot = state.extra * Mth.TWO_PI; // the twirl
		head.yRot = state.yRot * Mth.DEG_TO_RAD;
		head.xRot = state.xRot * Mth.DEG_TO_RAD - state.special * 0.5F;
		mantle.xRot = 0.12F + speed * 0.3F + state.attack * 0.5F + Mth.sin(state.ageInTicks * 0.1F) * 0.03F;
	}
}
