/*
 * Copyright (c) 2021 - 2023 LambdAurora <email@lambdaurora.dev>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package dev.lambdaurora.aurorasdeco.resource.datagen;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.gson.JsonElement;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeType;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Restored from the mod's own pre-Quilt Fabric history (git {@code 6a16db7~1}) as the counterpart to
 * the restored {@code RecipeManagerMixin} -- see that class for why. Recipes registered here are held
 * until {@code RecipeManagerMixin} injects them into whatever {@code RecipeManager} is being (re)built.
 * <p>
 * 1.21.1 update: {@code Recipe} no longer carries its own {@code Identifier} (it moved out to the
 * separate {@link RecipeEntry} id+recipe pair, mirroring {@code RecipeManager.apply()}'s own real
 * structure -- see the class-level remarks on {@code RecipeManagerMixin}), so every entry point here
 * now takes the id explicitly instead of reading {@code recipe.getId()}.
 */
public final class RecipeDatagen {
	public static final Logger LOGGER = LogManager.getLogger("aurorasdeco:datagen/recipe");

	private static final Map<RecipeType<?>, List<RecipeEntry<?>>> RECIPES = new Object2ObjectOpenHashMap<>();

	private RecipeDatagen() {
		throw new UnsupportedOperationException("RecipeDatagen only contains static definitions.");
	}

	public static void applyRecipes(Map<Identifier, JsonElement> map,
			ImmutableMultimap.Builder<RecipeType<?>, RecipeEntry<?>> recipesByType,
			ImmutableMap.Builder<Identifier, RecipeEntry<?>> recipesById) {
		var recipeCount = new int[]{0};
		RECIPES.forEach((type, recipes) -> recipes.forEach(entry -> {
			if (!map.containsKey(entry.id())) {
				recipesByType.put(type, entry);
				recipesById.put(entry.id(), entry);
				recipeCount[0]++;
			}
		}));

		LOGGER.info("Loaded {} additional recipes", recipeCount[0]);
	}

	public static Recipe<?> registerRecipe(Identifier id, Recipe<?> recipe, String category) {
		var recipes = RECIPES.computeIfAbsent(recipe.getType(), recipeType -> new ArrayList<>());

		for (var other : recipes) {
			if (other.id().equals(id))
				return other.value();
		}

		recipes.add(new RecipeEntry<>(id, recipe));

		var advancementId = Identifier.of(id.getNamespace(), "recipes/" + category + "/" + id.getPath());
		AdvancementDatagen.register(advancementId, () -> AdvancementDatagen.simpleRecipeUnlock(id, recipe));

		return recipe;
	}
}
