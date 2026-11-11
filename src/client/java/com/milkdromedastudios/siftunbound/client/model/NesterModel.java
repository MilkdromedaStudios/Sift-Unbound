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
 * Original nester model: a tall plant-beast on four long stilt legs, a woven nest of twigs on its back, a long neck and a
 * snapping beak-like jaw. Its long legs gallop.
 */
public class NesterModel extends EntityModel<SiftMobRenderState> {
	private final ModelPart body;
	private final ModelPart neck;
	private final ModelPart head;
	private final ModelPart jaw;
	private final ModelPart[] legs = new ModelPart[4];

	public NesterModel(ModelPart root) {
		super(root);
		body = root.getChild("body");
		neck = root.getChild("neck");
		head = neck.getChild("head");
		jaw = head.getChild("jaw");
		for (int i = 0; i < 4; i++) {
			legs[i] = root.getChild("leg" + i);
		}
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -3.0F, -7.0F, 6, 6, 14), PartPose.offset(0.0F, 5.0F, 0.0F));
		body.addOrReplaceChild("nest", CubeListBuilder.create().texOffs(0, 20).addBox(-3.5F, -2.0F, -3.0F, 7, 2, 7), PartPose.offset(0.0F, -3.0F, 2.0F));
		PartDefinition neck = root.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(40, 0).addBox(-1.5F, -11.0F, -1.5F, 3, 11, 3),
			PartPose.offsetAndRotation(0.0F, 4.0F, -6.0F, -0.3F, 0.0F, 0.0F));
		PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create().texOffs(28, 20).addBox(-2.5F, -3.0F, -6.0F, 5, 3, 7),
			PartPose.offsetAndRotation(0.0F, -11.0F, 0.0F, 0.3F, 0.0F, 0.0F));
		head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(28, 30).addBox(-2.0F, 0.0F, -6.0F, 4, 1, 6), PartPose.offset(0.0F, 0.0F, 0.5F));
		for (int i = 0; i < 4; i++) {
			root.addOrReplaceChild("leg" + i, CubeListBuilder.create().texOffs(52, 0).addBox(-1.0F, 0.0F, -1.0F, 2, 16, 2),
				PartPose.offset(i % 2 == 0 ? -2.0F : 2.0F, 8.0F, i < 2 ? -5.5F : 5.5F));
		}
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(SiftMobRenderState state) {
		super.setupAnim(state);
		float walk = state.walkAnimationPos;
		float speed = state.walkAnimationSpeed;
		for (int i = 0; i < 4; i++) { // a gallop: front pair, then back pair
			float phase = (i < 2 ? 0.0F : Mth.PI) + (i % 2) * 0.35F;
			legs[i].xRot = Mth.cos(walk * 0.75F + phase) * 1.1F * speed;
		}
		body.xRot = Mth.sin(walk * 0.75F) * 0.08F * speed;
		neck.xRot = -0.3F + Mth.sin(walk * 0.75F) * 0.12F * speed + state.attack * 0.7F;
		neck.yRot = state.yRot * Mth.DEG_TO_RAD * 0.6F;
		head.xRot = 0.3F + state.xRot * Mth.DEG_TO_RAD * 0.5F;
		jaw.xRot = 0.05F + state.attack * 0.8F;
	}
}
