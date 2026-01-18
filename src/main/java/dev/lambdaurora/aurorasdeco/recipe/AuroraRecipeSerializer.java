package dev.lambdaurora.aurorasdeco.recipe;

import com.google.gson.JsonObject;

import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeSerializer;

public interface AuroraRecipeSerializer<T extends Recipe<?>> extends RecipeSerializer<T> {
	JsonObject toJson(T recipe);
}
