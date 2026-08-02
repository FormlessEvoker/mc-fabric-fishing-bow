package com.evoker.fishingbow.mixin;

import com.evoker.fishingbow.ModItems;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FishingHook.class)
public class FishingHookMixin {
	private static final double FISHING_BOW_MAX_DISTANCE_SQUARED = 96.0 * 96.0;

	@Inject(method = "shouldStopFishing", at = @At("HEAD"), cancellable = true)
	private void fishingBow$keepFishingBowBobberAlive(
			Player player,
			CallbackInfoReturnable<Boolean> cir
	) {
		boolean holdingFishingBow =
				player.getMainHandItem().is(ModItems.FISHING_BOW)
						|| player.getOffhandItem().is(ModItems.FISHING_BOW);

		FishingHook hook = (FishingHook) (Object) this;

		if (holdingFishingBow
				&& hook.distanceToSqr(player) <= FISHING_BOW_MAX_DISTANCE_SQUARED) {
			cir.setReturnValue(false);
		}
	}
}