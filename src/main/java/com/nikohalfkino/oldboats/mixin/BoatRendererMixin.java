package com.nikohalfkino.oldboats.mixin;

import net.minecraft.client.renderer.entity.BoatRenderer;
import net.minecraft.world.entity.vehicle.Boat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BoatRenderer.class)
public class BoatRendererMixin {

    @Inject(method = "getTextureLocation(Lnet/minecraft/world/entity/vehicle/Boat$Type;Z)Ljava/lang/String;", at = @At("HEAD"), cancellable = true)
    private static void injectOldTexture(Boat.Type pType, boolean pChestBoat, CallbackInfoReturnable<String> cir) {
        if (!pChestBoat) {
            cir.setReturnValue("oldboats:textures/entity/boat.png");
        }
    }
}