package com.evoker.fishingbow;

import com.evoker.fishingbow.entity.FishingBowArrow;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.codec.ByteBufCodecs;

/**
 * Attachments used to track per-player state that doesn't belong on vanilla classes.
 */
public final class ModAttachments {
    /**
     * The single {@link FishingBowArrow} a player currently has out, if any. Mirrors how vanilla tracks
     * {@code Player.fishing} for the regular fishing rod: only one can be active at a time, and firing
     * again is blocked until it's reeled back in (or otherwise removed).
     * <p>
     * Server-only - entities aren't a syncable attachment type. See {@link #HAS_ACTIVE_ARROW} for the
     * client-visible counterpart.
     */
    public static final AttachmentType<FishingBowArrow> ACTIVE_FISHING_BOW_ARROW =
            AttachmentRegistry.create(FishingBow.id("active_fishing_bow_arrow"));

    /**
     * Synced mirror of "is {@link #ACTIVE_FISHING_BOW_ARROW} set", visible to the owning client. The item's
     * {@code use()} runs on both sides, and the client has no way to see the (unsynced) entity attachment -
     * without this, the client would always think no arrow is out and let the player draw the bow back
     * (visually) even while one is still in flight or being reeled in.
     */
    public static final AttachmentType<Boolean> HAS_ACTIVE_ARROW = AttachmentRegistry.create(
            FishingBow.id("has_active_fishing_bow_arrow"),
            builder -> builder
                    .initializer(() -> Boolean.FALSE)
                    .syncWith(ByteBufCodecs.BOOL, AttachmentSyncPredicate.targetOnly())
    );

    private ModAttachments() {
    }
}
