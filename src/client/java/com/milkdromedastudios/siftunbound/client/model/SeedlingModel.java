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
 * Original seedling model: a small seed-pod that is mostly mouth, a hinged jaw full of teeth, two leaves sprouting from
 * its crown and four stubby root legs.
 */
public class SeedlingModel extends EntityModel<SiftMobRenderState> {
	private final ModelPart body;
	private final ModelPart jaw;
	private final ModelPart leftLeaf;
	private final ModelPart rightLeaf;
	private final ModelPart[] legs = new ModelPart[4];

	public SeedlingModel(ModelPart root) {
		super(root);
		body = root.getChild("body");
		jaw = root.getChild("jaw");
		leftLeaf = root.getChild("left_leaf");
		rightLeaf = root.getChild("right_leaf");
		for (int i = 0; i < 4; i++) {
			legs[i] = root.getChild("leg" + i);
		}
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, -5.0F, -3.5F, 7, 5, 7), PartPose.offset(0.0F, 20.0F, 0.0F));
		root.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(0, 12).addBox(-3.5F, 0.0F, -7.0F, 7, 2, 7), PartPose.offset(0.0F, 20.0F, 3.5F));
		root.addOrReplaceChild("left_leaf", CubeListBuilder.create().texOffs(32, 0).addBox(0.0F, -6.0F, -2.0F, 0, 6, 4), PartPose.offsetAndRotation(-1.5F, 15.0F, 0.0F, 0.0F, 0.0F, -0.45F));
		root.addOrReplaceChild("right_leaf", CubeListBuilder.create().texOffs(32, 0).mirror().addBox(0.0F, -6.0F, -2.0F, 0, 6, 4), PartPose.offsetAndRotation(1.5F, 15.0F, 0.0F, 0.0F, 0.0F, 0.45F));
		for (int i = 0; i < 4; i++) {
			root.addOrReplaceChild("leg" + i, CubeListBuilder.create().texOffs(28, 0).addBox(-0.5F, 0.0F, -0.5F, 1, 2, 1),
				PartPose.offset(i % 2 == 0 ? -2.0F : 2.0F, 22.0F, i < 2 ? -2.0F : 2.0F));
		}
		return LayerDefinition.create(mesh, 64, 32);
	}

	@Override
	public void setupAnim(SiftMobRenderState state) {
		super.setupAnim(state);
		float walk = state.walkAnimationPos;
		float speed = state.walkAnimationSpeed;
		float t = state.ageInTicks;
		for (int i = 0; i < 4; i++) {
			legs[i].xRot = Mth.cos(walk * 1.4F + (i == 0 || i == 3 ? 0 : Mth.PI)) * 1.3F * speed;
		}
		float chatter = Math.abs(Mth.sin(t * 0.5F)) * 0.08F + Math.abs(Mth.sin(walk * 0.9F)) * 0.25F * speed;
		jaw.xRot = chatter + state.attack * 0.95F;
		body.xRot = -state.attack * 0.25F;
		leftLeaf.zRot = -0.45F - Mth.sin(t * 0.12F) * 0.12F - speed * 0.3F;
		rightLeaf.zRot = 0.45F + Mth.sin(t * 0.12F + 1.0F) * 0.12F + speed * 0.3F;
	}
}
