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
 * Original slabber model: a flat slab-like shell on four stubby legs with a small head. Asleep, it tucks head and legs
 * in and settles flat on the ground, looking like a slab of mossy soulshale.
 */
public class SlabberModel extends EntityModel<SiftMobRenderState> {
	private final ModelPart shell;
	private final ModelPart head;
	private final ModelPart[] legs = new ModelPart[4];

	public SlabberModel(ModelPart root) {
		super(root);
		shell = root.getChild("shell");
		head = root.getChild("head");
		for (int i = 0; i < 4; i++) {
			legs[i] = root.getChild("leg" + i);
		}
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("shell", CubeListBuilder.create().texOffs(0, 0).addBox(-7.0F, -5.0F, -7.0F, 14, 5, 14), PartPose.offset(0.0F, 21.0F, 0.0F));
		root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 19).addBox(-2.0F, -2.0F, -5.0F, 4, 4, 5), PartPose.offset(0.0F, 19.5F, -6.0F));
		float[][] feet = {{-5.0F, -5.0F}, {5.0F, -5.0F}, {-5.0F, 5.0F}, {5.0F, 5.0F}};
		for (int i = 0; i < 4; i++) {
			root.addOrReplaceChild("leg" + i, CubeListBuilder.create().texOffs(18, 19).addBox(-1.5F, 0.0F, -1.5F, 3, 3, 3),
				PartPose.offset(feet[i][0], 21.0F, feet[i][1]));
		}
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(SiftMobRenderState state) {
		super.setupAnim(state);
		boolean asleep = state.flag;
		float walk = state.walkAnimationPos;
		float speed = state.walkAnimationSpeed;
		shell.y = asleep ? 24.0F : 21.0F + Mth.sin(walk * 0.5F) * 0.3F * speed;
		head.z = asleep ? -2.5F : -6.0F;
		head.y = asleep ? 22.5F : 19.5F;
		head.xRot = asleep ? 0.0F : state.xRot * Mth.DEG_TO_RAD;
		head.yRot = asleep ? 0.0F : state.yRot * Mth.DEG_TO_RAD;
		for (int i = 0; i < 4; i++) {
			legs[i].visible = !asleep;
			legs[i].zRot = Mth.cos(walk * 0.9F + (i % 3 == 0 ? 0.0F : Mth.PI)) * 0.4F * speed;
		}
	}
}
