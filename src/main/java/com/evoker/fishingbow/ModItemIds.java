package com.evoker.fishingbow;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.resources.ResourceKey;

public final class ModItemIds {
    public static final ResourceKey<Item> FISHING_BOW = create("fishing_bow");

    private ModItemIds() {}

    public static ResourceKey<Item> create(String path) {
        return ResourceKey.create(Registries.ITEM, FishingBow.id(path));
    }

}
