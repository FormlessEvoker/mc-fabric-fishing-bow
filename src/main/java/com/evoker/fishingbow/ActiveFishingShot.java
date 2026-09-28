package com.evoker.fishingbow;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import com.evoker.fishingbow.entity.FishingBowArrow;
import com.evoker.fishingbow.entity.FishingBowHook;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Server authority for one fire/hook/reel cycle. An active shot need not have an arrow entity. */
public final class ActiveFishingShot {
    public enum State { FLYING, IN_BLOCK, HOOKED_CREATURE, AT_IMPACT, RETURNING }

    private static final AtomicLong NEXT_SHOT_ID = new AtomicLong(1);
    private static final double GROUND_ITEM_CAPTURE_RADIUS = 1.0;
    private static final int MAX_TICKS_AFTER_ARROW_RETURN = 80;
    private final long shotId = NEXT_SHOT_ID.getAndIncrement();
    private final Player owner;
    private final ItemStack firingBow;
    private final InteractionHand firingHand;
    private final float maxLineDistance;
    private final int initialArrowId;
    private final List<ItemEntity> items = new ArrayList<>();
    private State state = State.FLYING;
    private FishingBowArrow arrow;
    private FishingBowHook hook;
    private LivingEntity creature;
    private int ticksAfterArrowReturn;

    public ActiveFishingShot(Player owner, FishingBowArrow arrow, ItemStack firingBow, InteractionHand firingHand) {
        this.owner = owner;
        this.arrow = arrow;
        this.firingBow = firingBow;
        this.firingHand = firingHand;
        this.maxLineDistance = FishingBowConfig.maxLineDistance;
        this.initialArrowId = arrow.getId();
        FishingBow.debug("event=shot_started shotId={} player={} playerUuid={} arrowId={}",
                shotId, owner.getScoreboardName(), owner.getUUID(), arrow.getId());
    }

    public State state() { return state; }
    public long shotId() { return shotId; }

    public boolean canReel() {
        return state != State.RETURNING && !owner.isRemoved() && owner.isAlive();
    }

    public void onBlockHit(FishingBowArrow hitArrow, Vec3 impact) {
        if (state == State.FLYING && arrow == hitArrow) {
            if (owner.level() instanceof ServerLevel level) {
                AABB area = new AABB(impact, impact).inflate(GROUND_ITEM_CAPTURE_RADIUS);
                for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, area)) {
                    if (item.position().distanceToSqr(impact)
                            <= GROUND_ITEM_CAPTURE_RADIUS * GROUND_ITEM_CAPTURE_RADIUS) {
                        items.add(item);
                    }
                }
            }
            FishingBow.debug("event=shot_impact shotId={} player={} impactType=block arrowId={}",
                    shotId, owner.getScoreboardName(), hitArrow.getId());
            changeState(State.IN_BLOCK);
        }
    }

    public void onEntityHit(FishingBowArrow hitArrow, Entity target, Vec3 impact,
                            List<ItemEntity> newItems) {
        if (state != State.FLYING || arrow != hitArrow || !(owner.level() instanceof ServerLevel level)) return;

        items.addAll(newItems);
        creature = target instanceof LivingEntity living && living.isAlive() ? living : null;
        hook = new FishingBowHook(level, owner, creature, impact, hitArrow.getYRot(), hitArrow.getXRot(),
                target instanceof LivingEntity && creature == null);
        level.addFreshEntity(hook);
        State nextState = creature == null ? State.AT_IMPACT : State.HOOKED_CREATURE;
        arrow = null;
        if (!hitArrow.isRemoved()) hitArrow.discard();
        FishingBow.debug("event=shot_impact shotId={} player={} impactType=entity arrowId={} targetType={} targetId={} x={} y={} z={} state={} hookId={} captured={}",
                shotId, owner.getScoreboardName(), hitArrow.getId(), target.getType(), target.getId(),
                impact.x, impact.y, impact.z, nextState, hook.getId(), items.size());
        changeState(nextState);
    }

    public void onArrowRemoved(FishingBowArrow removed) {
        if (arrow == removed) {
            arrow = null;
            finish(state == State.RETURNING ? "return_arrow_removed" : "outbound_arrow_removed");
        }
    }

    public boolean startReeling() {
        if (!canReel() || !(owner.level() instanceof ServerLevel level)) return false;
        if (state == State.HOOKED_CREATURE || state == State.AT_IMPACT) {
            if (hook == null || hook.isRemoved()) {
                finish("hook_missing_before_reel");
                return false;
            }
            Vec3 start = hook.anchorPosition();
            arrow = new FishingBowArrow(level, owner, null);
            arrow.setPos(start);
            arrow.setYRot(hook.worldYaw());
            arrow.setXRot(hook.getXRot());
            level.addFreshEntity(arrow);
            hook.discard();
            hook = null;
        } else if (arrow == null || arrow.isRemoved()) {
            finish("arrow_missing_before_reel");
            return false;
        }
        changeState(State.RETURNING);
        arrow.startReturning();
        FishingBow.debug("event=shot_returning shotId={} player={} originalArrowId={} activeArrowId={}",
                shotId, owner.getScoreboardName(), initialArrowId, arrow.getId());
        return true;
    }

    public void tick() {
        if (!owner.isAlive() || owner.isRemoved()) { finish("owner_unavailable"); return; }
        Vec3 lineEnd = arrow != null && !arrow.isRemoved() ? arrow.position()
                : hook != null && !hook.isRemoved() ? hook.anchorPosition() : null;
        if (lineEnd != null) {
            double distanceSq = owner.position().distanceToSqr(lineEnd);
            if (distanceSq > maxLineDistance * maxLineDistance) {
                FishingBow.debug("event=shot_line_broken shotId={} player={} state={} distance={} maxDistance={} "
                                + "arrowId={} hookId={}",
                        shotId, owner.getScoreboardName(), state, Math.sqrt(distanceSq), maxLineDistance,
                        arrow == null ? -1 : arrow.getId(), hook == null ? -1 : hook.getId());
                finish("line_broken_distance");
                firingBow.hurtAndBreak(1, owner, firingHand);
                return;
            }
        }
        switch (state) {
            case FLYING, IN_BLOCK -> {
                if (arrow == null || arrow.isRemoved()) finish("outbound_arrow_missing");
            }
            case HOOKED_CREATURE -> {
                if (hook == null || hook.isRemoved()) { finish("hook_removed"); return; }
                if (creature == null || !creature.isAlive() || creature.isRemoved()
                        || creature.level() != owner.level()) {
                    hook.releaseTarget();
                    creature = null;
                    FishingBow.debug("event=shot_target_lost shotId={} player={} hookId={}",
                            shotId, owner.getScoreboardName(), hook.getId());
                    changeState(State.AT_IMPACT);
                } else {
                    hook.followTarget();
                }
            }
            case AT_IMPACT -> {
                if (hook == null || hook.isRemoved()) finish("hook_removed");
            }
            case RETURNING -> reelTick();
        }
    }

    private void reelTick() {
        if (arrow != null && arrow.isRemoved()) { finish("return_arrow_missing"); return; }
        Vec3 destination = owner.position().add(0, owner.getEyeHeight() * 0.5, 0);
        boolean allArrived = true;
        if (creature != null) {
            if (creature.isAlive() && !creature.isRemoved() && creature.level() == owner.level()) {
                allArrived &= pullToward(creature, destination);
            } else creature = null;
        }
        items.removeIf(item -> !item.isAlive());
        for (ItemEntity item : items) allArrived &= pullToward(item, destination);
        if (arrow != null && pullToward(arrow, destination)) {
            FishingBowArrow arrived = arrow;
            arrow = null;
            arrived.discard();
        }
        if (arrow == null) {
            if (allArrived) finish("returned");
            else if (++ticksAfterArrowReturn >= MAX_TICKS_AFTER_ARROW_RETURN) finish("return_timeout");
        }
    }

    private static boolean pullToward(Entity entity, Vec3 destination) {
        Vec3 delta = destination.subtract(entity.position());
        double distance = delta.length();
        if (distance <= FishingBowConfig.arrivalDistance) return true;
        entity.setDeltaMovement(entity.getDeltaMovement().scale(FishingBowConfig.drag)
                .add(delta.scale(FishingBowConfig.pullStrength / distance)));
        entity.hurtMarked = true;
        return false;
    }

    private void changeState(State nextState) {
        if (state == nextState) return;
        State previousState = state;
        state = nextState;
        FishingBow.debug("event=shot_state_changed shotId={} player={} from={} to={}",
                shotId, owner.getScoreboardName(), previousState, nextState);
    }

    private void finish(String reason) {
        if (owner.getAttached(ModAttachments.ACTIVE_FISHING_SHOT) != this) return;
        owner.setAttached(ModAttachments.ACTIVE_FISHING_SHOT, null);
        owner.setAttached(ModAttachments.HAS_ACTIVE_ARROW, false);
        if (hook != null && !hook.isRemoved()) hook.discard();
        if (arrow != null && !arrow.isRemoved()) arrow.discard();
        FishingBow.debug("event=shot_finished shotId={} player={} playerUuid={} originalArrowId={} finalArrowId={} hookId={} finalState={} reason={}",
                shotId, owner.getScoreboardName(), owner.getUUID(), initialArrowId,
                arrow == null ? -1 : arrow.getId(), hook == null ? -1 : hook.getId(), state, reason);
    }

    public void cancel() { finish("disconnected"); }
}
