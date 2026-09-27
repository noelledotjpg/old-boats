package com.nikohalfkino.oldboats.mixin;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VehicleEntity.class)
public class VehicleEntityMixin {
    // crashing into walls has been removed and instead the boat will break when blown up
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void oldboats$hurt(DamageSource pSource, float pAmount, CallbackInfoReturnable<Boolean> cir) {
        if (!((VehicleEntity) (Object) this instanceof Boat self)) {
            return;
        }

        if (self.isRemoved()) {
            cir.setReturnValue(false);
            return;
        }

        if (!pSource.is(DamageTypeTags.IS_EXPLOSION)) {
            return;
        }

        if (!self.level().isClientSide) {
            this.oldboats$breakApart();
        }

        cir.setReturnValue(true);
    }

    @Unique
    private void oldboats$breakApart() {
        Boat self = (Boat) (Object) this;
        if (self.level() instanceof ServerLevel serverlevel) {
            serverlevel.playSound(null, self.getX(), self.getY(), self.getZ(),
                SoundType.WOOD.getBreakSound(), SoundSource.NEUTRAL, 1.0F, 1.0F);

            // block breaking particles because its cool
            BlockParticleOption breakParticle = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_PLANKS.defaultBlockState());

            serverlevel.sendParticles(
                breakParticle,
                self.getX(),
                self.getY(),
                self.getZ(),
                10,
                (self.getRandom().nextDouble() - 0.5D) * self.getBbWidth(),
                self.getRandom().nextDouble() * self.getBbHeight(),
                (self.getRandom().nextDouble() - 0.5D) * self.getBbWidth(),
                0.1
            );

            self.discard();

            if (serverlevel.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
                for (int j1 = 0; j1 < 3; ++j1) {
                    self.spawnAtLocation(self.getVariant().getPlanks());
                }
                for (int k1 = 0; k1 < 2; ++k1) {
                    self.spawnAtLocation(Items.STICK);
                }
            }
        }
    }
}
