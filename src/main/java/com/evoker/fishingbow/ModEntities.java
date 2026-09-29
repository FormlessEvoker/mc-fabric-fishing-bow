package com.evoker.fishingbow;

import com.evoker.fishingbow.entity.FishingBowArrow;
import com.evoker.fishingbow.entity.FishingBowHook;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
    public static final ResourceKey<EntityType<?>> FISHING_BOW_ARROW_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE, FishingBow.id("fishing_bow_arrow"));

    public static final EntityType<FishingBowArrow> FISHING_BOW_ARROW = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            FISHING_BOW_ARROW_KEY,
            EntityType.Builder.<FishingBowArrow>of(FishingBowArrow::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(8)
                    .updateInterval(20)
                    .noSave()
                    .build(FISHING_BOW_ARROW_KEY)
    );

    public static final EntityType<FishingBowHook> FISHING_BOW_HOOK = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceKey.create(Registries.ENTITY_TYPE, FishingBow.id("fishing_bow_hook")),
            EntityType.Builder.<FishingBowHook>of(FishingBowHook::new, MobCategory.MISC)
                    .sized(0.1F, 0.1F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .noSave()
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, FishingBow.id("fishing_bow_hook")))
    );

    private ModEntities() {
    }

    public static void initialize() {
        // Registration above runs on class load; this just forces that to happen from FishingBow#onInitialize.
    }
}
