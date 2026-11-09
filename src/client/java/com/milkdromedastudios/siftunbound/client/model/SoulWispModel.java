package com.milkdromedastudios.siftunbound.client.model;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

/** A glowing core inside a slowly turning, see-through halo. */
public class SoulWispModel extends EntityModel<LivingEntityRenderState> {
	private final ModelPart core;
	private final ModelPart halo;

	public SoulWispModel(ModelPart root) {
		super(root, RenderTypes::entityTranslucent);
		core = root.getChild("core");
		halo = root.getChild("halo");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("core", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -2.0F, -2.0F, 4, 4, 4), PartPose.offset(0.0F, 20.0F, 0.0F));
		root.addOrReplaceChild("halo", CubeListBuilder.create().texOffs(0, 8).addBox(-3.0F, -3.0F, -3.0F, 6, 6, 6), PartPose.offset(0.0F, 20.0F, 0.0F));
		return LayerDefinition.create(mesh, 32, 32);
	}

	@Override
	public void setupAnim(LivingEntityRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks;
		float bob = Mth.sin(t * 0.12F) * 1.2F;
		core.y = 20.0F + bob;
		halo.y = 20.0F + bob;
		core.yRot = t * 0.05F;
		halo.yRot = -t * 0.03F;
		halo.xRot = Mth.sin(t * 0.04F) * 0.4F;
		float pulse = 1.0F + Mth.sin(t * 0.3F) * 0.08F;
		halo.xScale = halo.yScale = halo.zScale = pulse;
	}
}
