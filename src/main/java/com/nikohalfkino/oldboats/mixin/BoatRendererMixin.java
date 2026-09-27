package com.nikohalfkino.oldboats.mixin;

import com.nikohalfkino.oldboats.OldBoats;
import net.minecraft.client.renderer.entity.BoatRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.vehicle.Boat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BoatRenderer.class)
public class BoatRendererMixin {
    @Inject(method = "getTextureLocation*", at = @At("HEAD"), cancellable = true)
    private static void injectOldTexture(Boat.Type pType, boolean pChestBoat, CallbackInfoReturnable<ResourceLocation> cir) {
        if (!pChestBoat) {
            cir.setReturnValue(ResourceLocation.fromNamespaceAndPath(OldBoats.MODID, "textures/entity/boat.png"));
        }
    }
}
