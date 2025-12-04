package com.mom.teammod;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class Items {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, TeamMod.MODID);

    public static final RegistryObject<Item> TEAM_COMPASS = ITEMS.register("team_compass",
            () -> new com.mom.teammod.items.TeamCompassItem(new Item.Properties().stacksTo(1)));
}