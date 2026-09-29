package com.evoker.fishingbow;

import com.evoker.fishingbow.entity.FishingBowArrow;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A bow that fires a hooked arrow instead of a normal one: no ammo required, and whatever it hits (or
 * pops loose, or drops) can be reeled back in like a fishing line. See {@code docs/balancing.md} for the
 * damage/durability tuning behind the numbers below.
 */
public final class FishingBowItem extends BowItem {
    public FishingBowItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        // Read the synced flag, not the (server-only) entity attachment directly: use() runs on both sides,
        // and the client can't see an unsynced attachment. Without this, the client doesn't know an arrow is
        // out and lets the player draw the bow back (visually) while one is still in flight or reeling.
        // Raw nullable read, taken BEFORE getAttachedOrCreate() - that call writes a local default entry on
        // its first invocation, which would make hasAttached()/everSynced falsely read true afterward even if
        // no sync packet from the server was ever actually received. rawValue is the ground truth: null means
        // "no data at all yet" (never synced), non-null means an explicit value (ours or the server's) is set.
        Boolean rawValue = player.getAttached(ModAttachments.HAS_ACTIVE_ARROW);
        boolean hasActiveArrow = player.getAttachedOrCreate(ModAttachments.HAS_ACTIVE_ARROW);
        FishingBow.debug("use(): clientSide={}, rawValue={}, hasActiveArrow={}, player={}",
                level.isClientSide(), rawValue, hasActiveArrow, player.getScoreboardName());

        if (hasActiveArrow) {
            if (!level.isClientSide()) {
                ActiveFishingShot shot = player.getAttached(ModAttachments.ACTIVE_FISHING_SHOT);
                FishingBow.debug("event=shot_reel_requested shotId={} player={} state={} canReel={}",
                        shot == null ? -1 : shot.shotId(), player.getScoreboardName(),
                        shot == null ? "NONE" : shot.state(), shot != null && shot.canReel());
                if (shot != null && shot.startReeling()) {
                    player.getItemInHand(hand).hurtAndBreak(1, player, hand);
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.PLAYERS, 1.0F, 1.0F);
                }
            }
            return InteractionResult.CONSUME;
        }

        // Deliberately skip BowItem#use(): it requires a real arrow in the inventory before it will let the
        // player start drawing, and this bow needs no ammo at all. Start the draw ourselves instead.
        FishingBow.debug("use(): no active arrow, starting draw (clientSide={})", level.isClientSide());
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeCharged) {
        if (!(entity instanceof Player player)) {
            return false;
        }

        if (!level.isClientSide() && player.getAttached(ModAttachments.ACTIVE_FISHING_SHOT) != null) {
            // Already have an arrow out - reeling (via use()) is required before firing again.
            return false;
        }

        int ticksHeld = getUseDuration(stack, entity) - timeCharged;
        float power = BowItem.getPowerForTime(ticksHeld);
        if (power < FishingBowConfig.minimumDrawPower) {
            if (!level.isClientSide()) {
                // The client predicts fires with its own minimumDrawPower. If that is lower than the server's, the
                // client has already set HAS_ACTIVE_ARROW for a shot that never happened, and every later
                // right-click would be spent as a reel. Push an explicit false to undo that prediction. Remove
                // first so the write is a real change even though the server's own value is already false.
                FishingBow.debug("releaseUsing(): server rejected draw power={} min={}, resyncing flag, player={}",
                        power, FishingBowConfig.minimumDrawPower, player.getScoreboardName());
                player.removeAttached(ModAttachments.HAS_ACTIVE_ARROW);
                player.setAttached(ModAttachments.HAS_ACTIVE_ARROW, false);
            }
            return false;
        }

        // Set the synced flag unconditionally, on both sides - not just inside the ServerLevel branch below.
        // The client computes the exact same power/ticksHeld from the same inputs the server does, so it can
        // predict "yes, this release is about to fire" with zero network latency. Without this, the client's
        // only source of truth was the server's sync packet for this same setAttached() call, and a player who
        // clicks to reel fast enough could beat that round trip - the client would still think no arrow was
        // out and (visually) start drawing again, even though the server was already handling it correctly.
        player.setAttached(ModAttachments.HAS_ACTIVE_ARROW, true);

        if (level instanceof ServerLevel) {
            FishingBowArrow arrow = new FishingBowArrow(level, player, stack, player.getUsedItemHand());
            arrow.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
            arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F,
                    power * FishingBowConfig.projectileSpeedMultiplier, 1.0F);
            level.addFreshEntity(arrow);
            player.setAttached(ModAttachments.ACTIVE_FISHING_SHOT,
                    new ActiveFishingShot(player, arrow, stack, player.getUsedItemHand()));

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F,
                    1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + power * 0.5F);
        } else {
            FishingBow.debug("releaseUsing(): client-side predicted fire (rawValue now={}), no ServerLevel to spawn from",
                    player.getAttached(ModAttachments.HAS_ACTIVE_ARROW));
        }

        stack.hurtAndBreak(1, player, player.getUsedItemHand());
        return true;
    }
}
