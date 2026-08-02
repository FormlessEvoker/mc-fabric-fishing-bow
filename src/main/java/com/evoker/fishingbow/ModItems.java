package com.evoker.fishingbow;

import java.util.function.Function;


import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.resources.ResourceKey;

public class ModItems {
    public static final Item FISHING_BOW = register(
            ModItemIds.FISHING_BOW,
            FishingBowItem::new,
            new Item.Properties().durability(64)
    );

    private ModItems() {
    }

    private static Item register(
            ResourceKey<Item> key,
            Function<Item.Properties, Item> factory,
            Item.Properties properties
    ) {
        return Registry.register(
                BuiltInRegistries.ITEM,
                key,
                factory.apply(properties.setId(key))
        );
    }

    public static void initialize() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register(entries -> entries.accept(FISHING_BOW));
    }
}