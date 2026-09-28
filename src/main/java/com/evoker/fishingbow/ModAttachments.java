package com.evoker.fishingbow;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.codec.ByteBufCodecs;

/**
 * Attachments used to track per-player state that doesn't belong on vanilla classes.
 */
public final class ModAttachments {
    /**
     * The server's active fire/hook/reel cycle. It remains set while the creature hook is rendered
     * without any arrow projectile. Only one shot can be active per player.
     * <p>
     * Server-only - the shot holds entity references. See {@link #HAS_ACTIVE_ARROW} for the
     * client-visible counterpart.
     */
    public static final AttachmentType<ActiveFishingShot> ACTIVE_FISHING_SHOT =
            AttachmentRegistry.create(FishingBow.id("active_fishing_shot"));

    /**
     * Synced mirror of "is {@link #ACTIVE_FISHING_SHOT} set", visible to the owning client. The item's
     * {@code use()} runs on both sides, and the client has no way to see the server-only shot attachment -
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
