package com.nikohalfkino.oldboats.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Boat.class)
public abstract class BoatMixin extends Entity {

    @Shadow private boolean inputLeft;
    @Shadow private boolean inputRight;
    @Shadow private boolean inputUp;
    @Shadow private boolean inputDown;
    @Shadow private void tickLerp() { throw new UnsupportedOperationException(); }

    @Unique
    private double oldboats$speedMultiplier = 0.07D;

    @Unique
    private double oldboats$fallDistance = 0.0D;

    @Unique
    private static final float OLDBOATS$FALL_BREAK_DISTANCE = 5.0F;

    // 1.8.9 boat hitbox
    private static final EntityDimensions OLDBOATS$DIMENSIONS = EntityDimensions.fixed(1.5F, 0.6F);

    public EntityDimensions getDimensions(Pose pose) {
        return OLDBOATS$DIMENSIONS;
    }

    public BoatMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void oldboats$tick(CallbackInfo ci) {
        Boat self = (Boat) (Object) this;

        super.tick();

        if (self.getHurtTime() > 0) {
            self.setHurtTime(self.getHurtTime() - 1);
        }

        if (self.getDamage() > 0.0F) {
            self.setDamage(self.getDamage() - 1.0F);
        }

        this.tickLerp();

        boolean localControl = this.isControlledByLocalInstance();
        boolean unridden = !this.isVehicle();

        if (!localControl && !unridden) {
            this.setDeltaMovement(Vec3.ZERO);
            ci.cancel();
            return;
        }

        double motionX = this.getDeltaMovement().x;
        double motionY = this.getDeltaMovement().y;
        double motionZ = this.getDeltaMovement().z;

        int layers = 9;
        double submergedFraction = 0.0D;
        AABB box = this.getBoundingBox();

        for (int j = 0; j < layers; ++j) {
            double y0 = box.minY + (box.maxY - box.minY) * (double) (j) / (double) layers - 0.125D;
            double y1 = box.minY + (box.maxY - box.minY) * (double) (j + 1) / (double) layers - 0.125D;
            AABB slab = new AABB(box.minX, y0, box.minZ, box.maxX, y1, box.maxZ);
            if (this.oldboats$aabbTouchesWater(slab)) {
                submergedFraction += 1.0D / (double) layers;
            }
        }

        boolean touchingWater = submergedFraction > 0.0D;

        double speedBefore = Math.sqrt(motionX * motionX + motionZ * motionZ);

        double waterlineY = box.maxY - (1.0D - Math.min(submergedFraction, 1.0D)) * (box.maxY - box.minY);

        if (touchingWater && speedBefore > 0.22D) {
            double forwardX = -Math.sin(this.getYRot() * Math.PI / 180.0D);
            double forwardZ = Math.cos(this.getYRot() * Math.PI / 180.0D);
            double lateralX = Math.cos(this.getYRot() * Math.PI / 180.0D);
            double lateralZ = Math.sin(this.getYRot() * Math.PI / 180.0D);

            double forwardSpread = 0.8D;
            double lateralSpread = 0.6D;

            int splashes = (int) (1.0D + speedBefore * 30.0D);
            for (int k = 0; k < splashes; ++k) {
                double forwardOffset = (this.random.nextDouble() * 2.0D - 1.0D) * forwardSpread;
                double lateralOffset = (this.random.nextDouble() * 2.0D - 1.0D) * lateralSpread;

                double px = this.getX() + forwardX * forwardOffset + lateralX * lateralOffset;
                double pz = this.getZ() + forwardZ * forwardOffset + lateralZ * lateralOffset;

                this.level().addParticle(ParticleTypes.SPLASH, px, waterlineY, pz, motionX, motionY, motionZ);
            }
        }

        if (submergedFraction < 1.0D) {
            double restingDepth = 0.72D;
            double lift = 2.0D * (submergedFraction - restingDepth);
            motionY += 0.015D * lift;
            motionY *= 0.995D;
        } else {
            if (motionY < 0.0D) {
                motionY /= 2.0D;
            }
            motionY += 0.007D;
        }

        boolean turnedInPlace = false;

        if (localControl) {
            LivingEntity controller = self.getControllingPassenger();
            if (controller != null) {
                float forward = 0.0F;
                float strafe = 0.0F;
                if (this.inputUp) forward += 1.0F;
                if (this.inputDown) forward -= 1.0F;

                if (this.inputRight) strafe -= 1.0F;
                if (this.inputLeft) strafe += 1.0F;

                if (touchingWater && forward == 0.0F && strafe != 0.0F) {
                    float oldboats$turnSpeed = 3.0F;
                    self.setYRot(self.getYRot() - strafe * oldboats$turnSpeed);
                    turnedInPlace = true;
                }

                if (touchingWater) {
                    float f = controller.getYRot() + -strafe * 90.0F;
                    motionX += -Math.sin(f * (float) Math.PI / 180.0F) * this.oldboats$speedMultiplier * forward * 0.05D;
                    motionZ += Math.cos(f * (float) Math.PI / 180.0F) * this.oldboats$speedMultiplier * forward * 0.05D;
                }
            }
        }

        double speedNow = Math.sqrt(motionX * motionX + motionZ * motionZ);
        if (speedNow > 0.35D) {
            double scale = 0.35D / speedNow;
            motionX *= scale;
            motionZ *= scale;
            speedNow = 0.35D;
        }

        if (speedNow > speedBefore && this.oldboats$speedMultiplier < 0.35D) {
            this.oldboats$speedMultiplier += (0.35D - this.oldboats$speedMultiplier) / 70.0D;
            if (this.oldboats$speedMultiplier > 0.35D) {
                this.oldboats$speedMultiplier = 0.35D;
            }
        } else {
            this.oldboats$speedMultiplier -= (this.oldboats$speedMultiplier - 0.07D) / 35.0D;
            if (this.oldboats$speedMultiplier < 0.07D) {
                this.oldboats$speedMultiplier = 0.07D;
            }
        }

        for (int i1 = 0; i1 < 4; ++i1) {
            int bx = Mth.floor(this.getX() + ((double) (i1 % 2) - 0.5D) * 0.8D);
            int bz = Mth.floor(this.getZ() + ((double) (i1 / 2) - 0.5D) * 0.8D);

            for (int j2 = 0; j2 < 2; ++j2) {
                int by = Mth.floor(this.getY()) + j2;
                BlockPos pos = new BlockPos(bx, by, bz);
                Block block = this.level().getBlockState(pos).getBlock();

                if (block == Blocks.SNOW) {
                    this.level().removeBlock(pos, false);
                    this.horizontalCollision = false;
                } else if (block == Blocks.LILY_PAD) {
                    this.level().destroyBlock(pos, true);
                    this.horizontalCollision = false;
                }
            }
        }

        if (this.onGround()) {
            motionX *= 0.5D;
            motionY *= 0.5D;
            motionZ *= 0.5D;
        }

        double driftYawRad = this.getYRot() * Math.PI / 180.0D;
        double fwdX = -Math.sin(driftYawRad);
        double fwdZ = Math.cos(driftYawRad);
        double latX = Math.cos(driftYawRad);
        double latZ = Math.sin(driftYawRad);

        double forwardVel = motionX * fwdX + motionZ * fwdZ;
        double lateralVel = motionX * latX + motionZ * latZ;

        double grip = 0.90D;

        double speedForGrip = Math.sqrt(motionX * motionX + motionZ * motionZ);
        if (speedForGrip < 0.1D) {
            grip *= speedForGrip / 0.1D;
        }

        lateralVel *= grip;

        motionX = fwdX * forwardVel + latX * lateralVel;
        motionZ = fwdZ * forwardVel + latZ * lateralVel;

        if (touchingWater) {
            AABB predictedBox = this.getBoundingBox().move(motionX, 0.0D, motionZ);
            if (!this.oldboats$aabbTouchesWater(predictedBox)) {
                motionX = 0.0D;
                motionZ = 0.0D;
            }
        }

        this.setDeltaMovement(motionX, motionY, motionZ);

        double oldboats$yBeforeMove = this.getY();

        this.move(MoverType.SELF, this.getDeltaMovement());

        // accumulate fall distance out of water
        double oldboats$droppedThisTick = oldboats$yBeforeMove - this.getY();
        if (!touchingWater && oldboats$droppedThisTick > 0.0D) {
            this.oldboats$fallDistance += oldboats$droppedThisTick;
        }

        // destroy the boat if it lands on solid ground
        if (!this.level().isClientSide
                && this.onGround()
                && !touchingWater
                && this.oldboats$fallDistance > OLDBOATS$FALL_BREAK_DISTANCE) {
            this.oldboats$breakApart();
            ci.cancel();
            return;
        }

        // reset tracker if it lands safely
        if (this.onGround() || touchingWater) {
            this.oldboats$fallDistance = 0.0D;
        }

        Vec3 postMove = this.getDeltaMovement();
        this.setDeltaMovement(postMove.x * 0.9900000095367432D, postMove.y * 0.949999988079071D, postMove.z * 0.9900000095367432D);

        this.setXRot(0.0F);
        double targetYaw = this.getYRot();
        double velSpeed = Math.sqrt(motionX * motionX + motionZ * motionZ);

        if (!turnedInPlace && velSpeed > 0.02D) {
            targetYaw = (double) ((float) (Mth.atan2(-motionX, motionZ) * 180.0D / Math.PI));
        }

        double yawDelta = Mth.wrapDegrees(targetYaw - (double) this.getYRot());
        double oldboats$yawTurnRate = Mth.clamp(4.0D + velSpeed * 60.0D, 4.0D, 14.0D);
        yawDelta = Mth.clamp(yawDelta, -oldboats$yawTurnRate, oldboats$yawTurnRate);
        this.setYRot((float) ((double) this.getYRot() + yawDelta));
        this.setRot(this.getYRot(), this.getXRot());

        if (!this.level().isClientSide) {
            for (Entity entity : this.level().getEntities(this, this.getBoundingBox().inflate(0.2D, -0.01D, 0.2D))) {
                if (entity == self.getFirstPassenger() || entity.hasPassenger(self)) {
                    continue;
                }
                if (entity.isPushable()) {
                    self.push(entity);
                }
            }
        }

        ci.cancel();
    }

    // crashing into walls has been removed and instead the boat will break when blown up
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void oldboats$hurt(DamageSource pSource, float pAmount, CallbackInfoReturnable<Boolean> cir) {
        if (this.isRemoved()) {
            cir.setReturnValue(false);
            return;
        }

        if (!pSource.is(DamageTypeTags.IS_EXPLOSION)) {
            return;
        }

        if (!this.level().isClientSide) {
            this.oldboats$breakApart();
        }

        cir.setReturnValue(true);
    }

    @Unique
    private void oldboats$breakApart() {
        Boat self = (Boat) (Object) this;

        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundType.WOOD.getBreakSound(), SoundSource.NEUTRAL, 1.0F, 1.0F);

        // block breaking particles because its cool
        BlockParticleOption breakParticle = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_PLANKS.defaultBlockState());

        for (int i = 0; i < 10; ++i) {
            this.level().addParticle(breakParticle,
                    this.getX() + (this.random.nextDouble() - 0.5D) * this.getBbWidth(),
                    this.getY() + this.random.nextDouble() * this.getBbHeight(),
                    this.getZ() + (this.random.nextDouble() - 0.5D) * this.getBbWidth(),
                    0.0D, 0.0D, 0.0D);
        }

        this.discard();

        if (this.level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            for (int j1 = 0; j1 < 3; ++j1) {
                this.spawnAtLocation(self.getVariant().getPlanks());
            }
            for (int k1 = 0; k1 < 2; ++k1) {
                this.spawnAtLocation(Items.STICK);
            }
        }
    }

    @Unique
    private boolean oldboats$aabbTouchesWater(AABB aabb) {
        int minX = Mth.floor(aabb.minX);
        int maxX = Mth.ceil(aabb.maxX);
        int minY = Mth.floor(aabb.minY);
        int maxY = Mth.ceil(aabb.maxY);
        int minZ = Mth.floor(aabb.minZ);
        int maxZ = Mth.ceil(aabb.maxZ);

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = minX; x < maxX; ++x) {
            for (int y = minY; y < maxY; ++y) {
                for (int z = minZ; z < maxZ; ++z) {
                    cursor.set(x, y, z);
                    FluidState fluid = this.level().getFluidState(cursor);
                    if (fluid.is(FluidTags.WATER)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Unique
    private static final int OLDBOATS$DISMOUNT_ANGLE_STEPS = 8;

    @Unique
    private Vec3 oldboats$findLandDismount(LivingEntity passenger) {
        Boat self = (Boat) (Object) this;

        double innerRadius = (self.getBbWidth() / 2.0D) + (passenger.getBbWidth() / 2.0D) + 0.15D;
        double outerRadius = innerRadius + 1.0D;

        Vec3 bestCandidate = null;
        double bestDistSq = Double.MAX_VALUE;

        double[] radii = new double[] { innerRadius, outerRadius };

        for (double radius : radii) {
            for (int i = 0; i < OLDBOATS$DISMOUNT_ANGLE_STEPS; i++) {
                double angle = (2.0D * Math.PI * i) / OLDBOATS$DISMOUNT_ANGLE_STEPS;
                double dx = Math.cos(angle) * radius;
                double dz = Math.sin(angle) * radius;

                double testX = self.getX() + dx;
                double testZ = self.getZ() + dz;

                BlockPos topPos = BlockPos.containing(testX, self.getBoundingBox().maxY, testZ);
                BlockPos belowPos = topPos.below();

                if (self.level().isWaterAt(belowPos)) {
                    continue;
                }

                Vec3 candidate = null;

                double floorHeightTop = self.level().getBlockFloorHeight(topPos);
                if (DismountHelper.isBlockFloorValid(floorHeightTop)) {
                    candidate = new Vec3(testX, (double) topPos.getY() + floorHeightTop, testZ);
                } else {
                    double floorHeightBelow = self.level().getBlockFloorHeight(belowPos);
                    if (DismountHelper.isBlockFloorValid(floorHeightBelow)) {
                        candidate = new Vec3(testX, (double) belowPos.getY() + floorHeightBelow, testZ);
                    }
                }

                if (candidate == null) {
                    continue;
                }

                for (Pose pose : passenger.getDismountPoses()) {
                    if (DismountHelper.canDismountTo(self.level(), candidate, passenger, pose)) {
                        double distSq = candidate.distanceToSqr(self.position());
                        if (distSq < bestDistSq) {
                            bestDistSq = distSq;
                            bestCandidate = candidate;
                            passenger.setPose(pose);
                        }
                        break;
                    }
                }
            }
            if (bestCandidate != null) {
                break;
            }
        }

        return bestCandidate;
    }

    @Overwrite
    public Vec3 getDismountLocationForPassenger(LivingEntity pLivingEntity) {
        Boat self = (Boat) (Object) this;

        Vec3 landCandidate = this.oldboats$findLandDismount(pLivingEntity);
        if (landCandidate != null) {
            return landCandidate;
        }

        // dismount player to the side if no valid land spot found
        double sideOffset = (self.getBbWidth() / 2.0D) + (pLivingEntity.getBbWidth() / 2.0D) + 0.15D;
        float yawRad = self.getYRot() * (float) Math.PI / 180.0F;
        double perpX = Math.cos(yawRad);
        double perpZ = Math.sin(yawRad);

        return new Vec3(
                self.getX() + perpX * sideOffset,
                self.getY() + 0.1D,
                self.getZ() + perpZ * sideOffset
        );
    }

    @Overwrite
    protected int getMaxPassengers() {
        return 1;
    }

    @Overwrite
    public double getPassengersRidingOffset() {
        return 0D;
    }

    @Overwrite
    protected float getSinglePassengerXOffset() {
        return -0.4F;
    }

    @Overwrite
    public boolean canBeCollidedWith() {
        return false;
    }

    @Overwrite
    protected void clampRotation(Entity pEntityToUpdate) {
        pEntityToUpdate.setYBodyRot(this.getYRot());

        float yawDiff = Mth.wrapDegrees(pEntityToUpdate.getYRot() - this.getYRot());

        float clampedYaw = Mth.clamp(yawDiff, -75.0F, 75.0F);

        pEntityToUpdate.yRotO += clampedYaw - yawDiff;
        pEntityToUpdate.setYRot(pEntityToUpdate.getYRot() + clampedYaw - yawDiff);
        pEntityToUpdate.setYHeadRot(pEntityToUpdate.getYRot());
    }
}