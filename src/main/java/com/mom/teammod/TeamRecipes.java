package com.mom.teammod;

import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class TeamRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, TeamMod.MODID);

    // Регистрация стандартного ShapedRecipe serializer
    public static final RegistryObject<RecipeSerializer<?>> CRAFTING_SHAPED = SERIALIZERS.register("crafting_shaped",
            () -> new net.minecraft.world.item.crafting.ShapedRecipe.Serializer());
}