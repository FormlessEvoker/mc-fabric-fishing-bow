package com.evoker.fishingbow.entity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.evoker.fishingbow.FishingBow;
import com.evoker.fishingbow.ModAttachments;
import com.evoker.fishingbow.ModEntities;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The arrow fired by the Fishing Bow. Behaves like a normal arrow in flight and on impact, but can also
 * "hook" whatever it hits so it can be reeled back in later, the way a fishing rod reels in its bobber.
 * <p>
 * See {@code docs/balancing.md} for the damage/durability tuning rationale.
 */
public class FishingBowArrow extends AbstractArrow {
    /** Solved so fish (3 HP) die from ~75% draw onward and chickens (4 HP) die only at a true full draw. */
    public static final double BASE_DAMAGE = 1.05;

    private static final double ITEM_CAPTURE_RADIUS = 1.5;
    private static final double REEL_ARRIVE_DISTANCE = 1.25;
    private static final double REEL_PULL_STRENGTH = 0.45;
    private static final double REEL_DRAG = 0.85;

    /** A living entity this arrow hit and is still stuck in, waiting to be reeled in. */
    private LivingEntity hookedEntity;

    /**
     * This arrow's position relative to {@link #hookedEntity}'s at the moment it hooked - vanilla doesn't
     * actually keep a stuck arrow riding along with its target (the "arrows sticking out of a mob" you see
     * normally is a cosmetic render trick, not a followable entity), so without this the arrow would just sit
     * frozen in the air at the hit location while the mob wanders off. Reapplied every tick in {@link #tick}
     * to keep the arrow visually embedded as the target moves.
     */
    private Vec3 hookOffset = Vec3.ZERO;

    /** Items popped loose by this arrow's hit (item frame contents, painting, mob drops) awaiting reeling. */
    private final List<ItemEntity> attachedItems = new ArrayList<>();

    private boolean reeling;

    /**
     * Guards {@link #spawnReplacement()} so it only ever fires once per arrow, however it gets triggered
     * (see the comment there for why more than one call site needs to be able to trigger it).
     */
    private boolean replacementSpawned;

    /**
     * True while {@link #onHitEntity} is inside its call to {@code super.onHitEntity()}. On a killing hit,
     * vanilla discards this arrow *synchronously* from within that call - before our override gets a chance
     * to capture the kill's drops into {@link #attachedItems}. {@link #remove} checks this flag to defer its
     * replacement-vs-clear decision to {@link #onHitEntity}, which finishes it via
     * {@link #finishDeferredRemoval()} once {@code attachedItems} actually reflects what was captured.
     */
    private boolean processingHit;

    public FishingBowArrow(EntityType<? extends FishingBowArrow> type, Level level) {
        super(type, level);
    }

    public FishingBowArrow(Level level, LivingEntity owner, @Nullable ItemStack firedFromWeapon) {
        super(ModEntities.FISHING_BOW_ARROW, owner, level, ItemStack.EMPTY, firedFromWeapon);
        setBaseDamage(BASE_DAMAGE);
        setCritArrow(false);
        pickup = Pickup.DISALLOWED;
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        // Pickup is disallowed - the only way to get this arrow back is reeling it in.
        return ItemStack.EMPTY;
    }

    /** Starts pulling whatever this arrow hooked - and the arrow itself - back toward its owner. */
    public void startReeling() {
        FishingBow.debug("startReeling(): id={}", getId());
        this.reeling = true;
        // Disable normal projectile collision for the return trip - the same trick a loyalty trident uses to fly
        // back through terrain it already passed on the way out. Without this, forcing velocity on an arrow
        // that's embedded in a block (or flying back through blocks it grazed on the way in) fights the
        // engine's own "stuck in ground"/re-collision handling and the arrow visibly snaps back and loops.
        this.setNoPhysics(true);
        this.setInGround(false);
    }

    public boolean isReeling() {
        return reeling;
    }

    @Override
    public void tick() {
        super.tick();

        if (level() instanceof ServerLevel) {
            if (reeling) {
                reelTick();
            } else if (hookedEntity != null) {
                if (!hookedEntity.isAlive()) {
                    FishingBow.debug("tick(): id={}, hookedEntity {} no longer alive - releasing hook",
                            getId(), hookedEntity);
                    hookedEntity = null;
                } else {
                    // Keep the arrow embedded in the target as it moves, rather than sitting frozen where it
                    // first hit - see hookOffset's javadoc for why this is needed at all.
                    setNoPhysics(true);
                    setInGround(false);
                    setDeltaMovement(Vec3.ZERO);
                    setPos(hookedEntity.position().add(hookOffset));
                }
            }
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        Entity target = hitResult.getEntity();

        if (!(level() instanceof ServerLevel serverLevel)) {
            super.onHitEntity(hitResult);
            return;
        }

        Set<Integer> itemIdsBefore = nearbyItemIds(serverLevel, target.position());
        boolean targetWasAliveBefore = !(target instanceof LivingEntity before) || before.isAlive();

        // super.onHitEntity() is what actually deals the damage - and on a killing hit, it also discards this
        // arrow *synchronously*, from inside this very call, before we get back here to capture the kill's
        // drops. remove() checks processingHit to detect that case and defer its decision to
        // finishDeferredRemoval() below, instead of wrongly concluding (from an still-empty attachedItems)
        // that there's nothing worth preserving.
        processingHit = true;
        try {
            super.onHitEntity(hitResult);
        } finally {
            processingHit = false;
        }

        // Capture anything the hit just spawned nearby: an item frame's contents popping out, a painting's
        // drop, or a mob's death loot. This is deliberately generic rather than special-casing each entity
        // type - vanilla's own hurtServer() logic already decides what (if anything) gets dropped.
        int capturedCount = 0;
        for (ItemEntity item : nearbyItems(serverLevel, target.position())) {
            if (!itemIdsBefore.contains(item.getId())) {
                attachedItems.add(item);
                capturedCount++;
            }
        }

        boolean isAliveAfter = !(target instanceof LivingEntity after) || after.isAlive();
        boolean killedTarget = targetWasAliveBefore && target instanceof LivingEntity && !isAliveAfter;

        if (target instanceof LivingEntity living && living.isAlive()) {
            hookedEntity = living;
            hookOffset = position().subtract(living.position());
            FishingBow.debug("onHitEntity(): hooked living entity {} (survived the hit), hookOffset={}",
                    living, hookOffset);
        }

        FishingBow.debug(
                "onHitEntity(): target={}, wasAliveBefore={}, isAliveAfter={}, itemsCaptured={}, "
                        + "totalAttachedItems={}, selfRemovedDuringHit={}, killedTarget={}",
                target, targetWasAliveBefore, isAliveAfter, capturedCount, attachedItems.size(), isRemoved(),
                killedTarget);

        if (isRemoved()) {
            // super.onHitEntity() discards this arrow synchronously on ANY hit, not just a killing one -
            // almost certainly because pickup = Pickup.DISALLOWED gives vanilla no reason to leave an
            // unpickupable arrow stuck in the world. Force a replacement here regardless of drops/kill/hook,
            // so hitting something never costs the player their one arrow - see finishDeferredRemoval(false)
            // in remove() for the (rarer) non-hit removal paths, which still gate on actually holding loot.
            finishDeferredRemoval(true);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        FishingBow.debug(
                "remove(): id={}, reason={}, reeling={}, attachedItems={}, replacementSpawned={}, processingHit={}",
                getId(), reason, reeling, attachedItems.size(), replacementSpawned, processingHit);

        if (processingHit) {
            // Still inside onHitEntity()'s call to super.onHitEntity() - attachedItems doesn't reflect this
            // hit's drops yet (those get captured right after that call returns). Let the real removal happen,
            // but leave the replacement-vs-clear decision to finishDeferredRemoval().
            super.remove(reason);
            return;
        }

        finishDeferredRemoval(false);
        super.remove(reason);
    }

    /**
     * Decides what happens to the owner's "arrow out" tracking once this arrow is actually gone: spawn a
     * stand-in carrying whatever loot it was holding (item frame contents, painting drops, mob death loot -
     * see {@link #attachedItems}), or just clear the tracking so the bow can fire again. Called from
     * {@link #remove} directly for ordinary removals (reeled in, despawned, chunk unload, etc.), and from
     * {@link #onHitEntity} for the killing-hit case where vanilla discarded this arrow synchronously partway
     * through - see {@link #processingHit}.
     *
     * @param forceReplacement spawn a replacement even if {@link #attachedItems} is empty - used whenever
     *                         vanilla discarded this arrow synchronously during a hit (see {@link #onHitEntity}),
     *                         so a no-drop kill or a hooked-but-surviving target still leaves the player
     *                         something to reel/re-fire, instead of losing their only arrow to a good hit.
     */
    private void finishDeferredRemoval(boolean forceReplacement) {
        if (!reeling && !replacementSpawned && (forceReplacement || !attachedItems.isEmpty())) {
            spawnReplacement();
        } else if (getOwner() instanceof Player player
                && player.getAttached(ModAttachments.ACTIVE_FISHING_BOW_ARROW) == this) {
            FishingBow.debug("finishDeferredRemoval(): id={}, nothing to preserve - clearing owner's active-arrow attachment",
                    getId());
            player.setAttached(ModAttachments.ACTIVE_FISHING_BOW_ARROW, null);
            player.setAttached(ModAttachments.HAS_ACTIVE_ARROW, false);
        }
    }

    /**
     * Spawns a replacement arrow at this arrow's current position, carrying over whatever loot it had
     * captured, and re-points the owner's "active arrow" tracking at it. See {@link #remove} for why this
     * is needed at all. Idempotent - only takes effect once per arrow.
     */
    private void spawnReplacement() {
        replacementSpawned = true;

        if (!(level() instanceof ServerLevel serverLevel) || !(getOwner() instanceof Player player)) {
            FishingBow.debug("spawnReplacement(): aborted - level is server={}, owner is player={}",
                    level() instanceof ServerLevel, getOwner() instanceof Player);
            return;
        }

        FishingBowArrow replacement = new FishingBowArrow(serverLevel, player, null);
        replacement.setPos(position());
        replacement.setNoPhysics(true);
        replacement.attachedItems.addAll(attachedItems);
        replacement.hookedEntity = hookedEntity;
        replacement.hookOffset = hookOffset;
        serverLevel.addFreshEntity(replacement);

        player.setAttached(ModAttachments.ACTIVE_FISHING_BOW_ARROW, replacement);
        player.setAttached(ModAttachments.HAS_ACTIVE_ARROW, true);

        FishingBow.debug("spawnReplacement(): spawned id={} at {} carrying {} items, hookedEntity={}",
                replacement.getId(), position(), replacement.attachedItems.size(), replacement.hookedEntity);
    }

    private void reelTick() {
        if (!(getOwner() instanceof Player player) || !player.isAlive()) {
            discard();
            return;
        }

        Vec3 target = player.position().add(0, player.getEyeHeight() * 0.5, 0);

        if (hookedEntity != null) {
            if (!hookedEntity.isAlive()) {
                FishingBow.debug("reelTick(): id={}, hookedEntity {} no longer alive - releasing hook",
                        getId(), hookedEntity);
                hookedEntity = null;
            } else {
                Vec3 beforePull = hookedEntity.position();
                pullToward(hookedEntity, target);
                FishingBow.debug(
                        "reelTick(): id={}, pulling hookedEntity={}, pos={}, deltaMovement={}, distanceToTarget={}",
                        getId(), hookedEntity, beforePull, hookedEntity.getDeltaMovement(),
                        target.subtract(beforePull).length());
            }
        }

        attachedItems.removeIf(item -> !item.isAlive());
        for (ItemEntity item : attachedItems) {
            pullToward(item, target);
        }

        if (pullToward(this, target)) {
            // Leave `reeling` true rather than clearing it here - remove() uses it (still true at this point)
            // to tell "arrived after a normal, player-initiated reel" apart from "vanilla discarded this out
            // from under us mid-flight", and this arrow has no more ticks left for the value to matter for
            // anything else.
            discard();
        }
    }

    /** Nudges {@code entity} toward {@code target}; returns true once it's close enough to have arrived. */
    private boolean pullToward(Entity entity, Vec3 target) {
        Vec3 toTarget = target.subtract(entity.position());
        double distance = toTarget.length();
        if (distance <= REEL_ARRIVE_DISTANCE) {
            return true;
        }

        Vec3 pull = toTarget.scale(REEL_PULL_STRENGTH / distance);
        entity.setDeltaMovement(entity.getDeltaMovement().scale(REEL_DRAG).add(pull));
        entity.hurtMarked = true;
        return false;
    }

    private List<ItemEntity> nearbyItems(ServerLevel level, Vec3 pos) {
        AABB area = new AABB(pos, pos).inflate(ITEM_CAPTURE_RADIUS);
        List<ItemEntity> items = new ArrayList<>();
        for (Entity entity : level.getEntities((Entity) null, area, e -> e instanceof ItemEntity)) {
            items.add((ItemEntity) entity);
        }
        return items;
    }

    private Set<Integer> nearbyItemIds(ServerLevel level, Vec3 pos) {
        Set<Integer> ids = new HashSet<>();
        for (ItemEntity item : nearbyItems(level, pos)) {
            ids.add(item.getId());
        }
        return ids;
    }
}
