package com.evoker.fishingbow.entity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import com.evoker.fishingbow.ActiveFishingShot;
import com.evoker.fishingbow.FishingBowConfig;
import com.evoker.fishingbow.ModAttachments;
import com.evoker.fishingbow.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/** Projectile used only for the outbound and return flights, and for an embedded block hit. */
public final class FishingBowArrow extends AbstractArrow {
    private static final EntityDataAccessor<Float> MAX_LINE_DISTANCE = SynchedEntityData.defineId(FishingBowArrow.class, EntityDataSerializers.FLOAT);
    private boolean processingHit;
    private boolean returning;

    public FishingBowArrow(EntityType<? extends FishingBowArrow> type, Level level) { super(type, level); }

    public FishingBowArrow(Level level, LivingEntity owner, @Nullable ItemStack weapon) {
        super(ModEntities.FISHING_BOW_ARROW, owner, level, ItemStack.EMPTY, weapon);
        setBaseDamage(FishingBowConfig.arrowBaseDamage);
        setCritArrow(false);
        pickup = Pickup.DISALLOWED;
        entityData.set(MAX_LINE_DISTANCE, FishingBowConfig.maxLineDistance);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(MAX_LINE_DISTANCE, FishingBowConfig.maxLineDistance);
    }

    @Override public boolean shouldRenderAtSqrDistance(double distanceSq) {
        double range = entityData.get(MAX_LINE_DISTANCE) + FishingBowConfig.LINE_RENDER_MARGIN;
        return distanceSq < range * range;
    }

    @Override protected ItemStack getDefaultPickupItem() { return ItemStack.EMPTY; }

    public void startReturning() {
        returning = true;
        setNoPhysics(true);
        setInGround(false);
        setDeltaMovement(Vec3.ZERO);
    }

    @Override protected void onHitBlock(BlockHitResult hit) {
        if (returning) return;
        super.onHitBlock(hit);
        if (level() instanceof ServerLevel && getOwner() instanceof Player player) {
            ActiveFishingShot shot = player.getAttached(ModAttachments.ACTIVE_FISHING_SHOT);
            if (shot != null) shot.onBlockHit(this, hit.getLocation());
        }
    }

    @Override protected void onHitEntity(EntityHitResult hit) {
        if (returning) return;
        if (!(level() instanceof ServerLevel serverLevel)) { super.onHitEntity(hit); return; }

        Entity target = hit.getEntity();
        int arrowsBefore = target instanceof LivingEntity living ? living.getArrowCount() : 0;
        Set<Integer> before = nearbyItemIds(serverLevel, target.position());
        processingHit = true;
        try { super.onHitEntity(hit); }
        finally { processingHit = false; }

        // Vanilla adds a generic stuck-arrow visual. Our hook renders the one shot-specific arrow whose
        // attachment is also the line endpoint, so undo only the count added by this impact.
        if (target instanceof LivingEntity living && living.isAlive()
                && living.getArrowCount() > arrowsBefore) {
            living.setArrowCount(arrowsBefore);
        }

        List<ItemEntity> newItems = new ArrayList<>();
        for (ItemEntity item : nearbyItems(serverLevel, target.position())) {
            if (!before.contains(item.getId())) newItems.add(item);
        }
        if (getOwner() instanceof Player player) {
            ActiveFishingShot shot = player.getAttached(ModAttachments.ACTIVE_FISHING_SHOT);
            if (shot != null) shot.onEntityHit(this, target, hit.getLocation(), newItems);
        }
    }

    @Override public void remove(RemovalReason reason) {
        if (!processingHit && level() instanceof ServerLevel && getOwner() instanceof Player player) {
            ActiveFishingShot shot = player.getAttached(ModAttachments.ACTIVE_FISHING_SHOT);
            if (shot != null) shot.onArrowRemoved(this);
        }
        super.remove(reason);
    }

    private List<ItemEntity> nearbyItems(ServerLevel level, Vec3 position) {
        AABB area = new AABB(position, position).inflate(FishingBowConfig.itemCaptureRadius);
        List<ItemEntity> items = new ArrayList<>();
        for (Entity entity : level.getEntities((Entity) null, area, e -> e instanceof ItemEntity)) {
            items.add((ItemEntity) entity);
        }
        return items;
    }

    private Set<Integer> nearbyItemIds(ServerLevel level, Vec3 position) {
        Set<Integer> ids = new HashSet<>();
        for (ItemEntity item : nearbyItems(level, position)) ids.add(item.getId());
        return ids;
    }
}
