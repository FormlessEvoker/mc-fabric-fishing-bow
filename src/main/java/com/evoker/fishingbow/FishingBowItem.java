package com.evoker.fishingbow;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public final class FishingBowItem extends FishingRodItem {
    private static final double CAST_SPEED_MULTIPLIER = 3.0;

    public FishingBowItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        InteractionResult result = super.use(level, player, hand);

        if (level instanceof ServerLevel && player.fishing != null) {
            FishingHook hook = player.fishing;
            hook.setDeltaMovement(
                    hook.getDeltaMovement().scale(CAST_SPEED_MULTIPLIER)
            );
        }

        return result;
    }
}