package com.nikohalfkino.oldboats.mixin;

import net.minecraft.client.model.BoatModel;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BoatModel.class)
public class BoatModelMixin {

    @Inject(method = "createBodyModel", at = @At("HEAD"), cancellable = true)
    private static void injectOldModel(CallbackInfoReturnable<LayerDefinition> cir) {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        // 1.8.9 boat geometry
        partdefinition.addOrReplaceChild("bottom", CubeListBuilder.create().texOffs(0, 8).addBox(-12.0F, -8.0F, -3.0F, 24.0F, 16.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 4.0F, 0.0F, ((float)Math.PI / 2F), 0.0F, 0.0F));
        partdefinition.addOrReplaceChild("back", CubeListBuilder.create().texOffs(0, 0).addBox(-10.0F, -7.0F, -1.0F, 20.0F, 6.0F, 2.0F), PartPose.offsetAndRotation(-11.0F, 4.0F, 0.0F, 0.0F, ((float)Math.PI * 1.5F), 0.0F));
        partdefinition.addOrReplaceChild("front", CubeListBuilder.create().texOffs(0, 0).addBox(-10.0F, -7.0F, -1.0F, 20.0F, 6.0F, 2.0F), PartPose.offsetAndRotation(11.0F, 4.0F, 0.0F, 0.0F, ((float)Math.PI / 2F), 0.0F));
        partdefinition.addOrReplaceChild("right", CubeListBuilder.create().texOffs(0, 0).addBox(-10.0F, -7.0F, -1.0F, 20.0F, 6.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 4.0F, -9.0F, 0.0F, (float)Math.PI, 0.0F));
        partdefinition.addOrReplaceChild("left", CubeListBuilder.create().texOffs(0, 0).addBox(-10.0F, -7.0F, -1.0F, 20.0F, 6.0F, 2.0F), PartPose.offset(0.0F, 4.0F, 9.0F));

        // dummy parts
        partdefinition.addOrReplaceChild("left_paddle", CubeListBuilder.create(), PartPose.ZERO);
        partdefinition.addOrReplaceChild("right_paddle", CubeListBuilder.create(), PartPose.ZERO);

        // water mask
        partdefinition.addOrReplaceChild("water_patch", CubeListBuilder.create().texOffs(0, 8).addBox(-12.0F, -8.0F, -3.0F, 24.0F, 16.0F, 4.0F), PartPose.offsetAndRotation(0.0F, -2.0F, 0.0F, ((float)Math.PI / 2F), 0.0F, 0.0F));

        // og resolution
        cir.setReturnValue(LayerDefinition.create(meshdefinition, 64, 32));
    }
}