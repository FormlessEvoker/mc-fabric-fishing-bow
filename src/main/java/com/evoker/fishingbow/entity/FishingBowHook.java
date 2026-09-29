package com.evoker.fishingbow.entity;

import org.joml.Vector3f;
import org.joml.Vector3fc;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import com.evoker.fishingbow.ModEntities;
import com.evoker.fishingbow.FishingBowConfig;

/** Non-colliding, non-arrow presentation anchor for a waiting shot. */
public final class FishingBowHook extends Entity {
    private static final double FALL_ACCELERATION = 0.05;
    private static final double MAX_FALL_SPEED = 0.6;
    private static final EntityDataAccessor<Integer> OWNER = SynchedEntityData.defineId(FishingBowHook.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TARGET = SynchedEntityData.defineId(FishingBowHook.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Vector3fc> OFFSET = SynchedEntityData.defineId(FishingBowHook.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Float> MAX_LINE_DISTANCE = SynchedEntityData.defineId(FishingBowHook.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> OFF_HAND = SynchedEntityData.defineId(FishingBowHook.class, EntityDataSerializers.BOOLEAN);
    private boolean falling;
    private double fallSpeed;

    public FishingBowHook(EntityType<? extends FishingBowHook> type, Level level) { super(type, level); }

    public FishingBowHook(ServerLevel level, Player owner, InteractionHand firingHand, LivingEntity target,
                          Vec3 impact, float yaw, float pitch, boolean fallAtImpact) {
        this(ModEntities.FISHING_BOW_HOOK, level);
        entityData.set(OWNER, owner.getId());
        entityData.set(TARGET, target == null ? -1 : target.getId());
        entityData.set(MAX_LINE_DISTANCE, FishingBowConfig.maxLineDistance);
        entityData.set(OFF_HAND, firingHand == InteractionHand.OFF_HAND);
        if (target != null) {
            float modelYaw = modelYaw(target.getPreciseBodyRotation(1.0F));
            Vec3 local = impact.subtract(target.position())
                    .yRot((float) Math.toRadians(-modelYaw));
            entityData.set(OFFSET, local.toVector3f());
        }
        setPos(impact);
        setYRot(target == null ? yaw : yaw - modelYaw(target.getPreciseBodyRotation(1.0F)));
        setXRot(pitch);
        setNoGravity(true);
        noPhysics = true;
        falling = fallAtImpact;
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(OWNER, -1);
        builder.define(TARGET, -1);
        builder.define(OFFSET, new Vector3f());
        builder.define(MAX_LINE_DISTANCE, FishingBowConfig.maxLineDistance);
        builder.define(OFF_HAND, false);
    }

    public int ownerId() { return entityData.get(OWNER); }
    public int targetId() { return entityData.get(TARGET); }
    public Vec3 localOffset() { return new Vec3(entityData.get(OFFSET)); }
    public float maxLineDistance() { return entityData.get(MAX_LINE_DISTANCE); }
    public InteractionHand firingHand() {
        return entityData.get(OFF_HAND) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    @Override public boolean shouldRenderAtSqrDistance(double distanceSq) {
        double range = maxLineDistance() + FishingBowConfig.LINE_RENDER_MARGIN;
        return distanceSq < range * range;
    }

    /** Living entity models rotate by 180 degrees minus their interpolated body yaw. */
    public static float modelYaw(float bodyYaw) { return 180.0F - bodyYaw; }

    public float worldYaw() {
        Entity target = level().getEntity(targetId());
        return target instanceof LivingEntity living
                ? getYRot() + modelYaw(living.getPreciseBodyRotation(1.0F)) : getYRot();
    }

    public Vec3 anchorPosition() {
        Entity target = level().getEntity(targetId());
        return target instanceof LivingEntity living
                ? living.position().add(localOffset().yRot((float) Math.toRadians(
                        modelYaw(living.getPreciseBodyRotation(1.0F)))))
                : position();
    }

    public void followTarget() { setPos(anchorPosition()); }

    public void releaseTarget() {
        setPos(anchorPosition());
        setYRot(worldYaw());
        entityData.set(TARGET, -1);
        falling = true;
        fallSpeed = 0;
    }

    @Override public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel) || !falling) return;

        fallSpeed = Math.min(fallSpeed + FALL_ACCELERATION, MAX_FALL_SPEED);
        Vec3 next = position().add(0, -fallSpeed, 0);
        BlockHitResult hit = level().clip(new ClipContext(position(), next,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (hit.getType() == HitResult.Type.BLOCK) {
            setPos(hit.getLocation());
            falling = false;
            fallSpeed = 0;
        } else {
            setPos(next);
        }
    }
    @Override public boolean hurtServer(ServerLevel level, DamageSource source, float amount) { return false; }
    @Override protected void readAdditionalSaveData(ValueInput input) { }
    @Override protected void addAdditionalSaveData(ValueOutput output) { }
}
