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

package dev.lambdaurora.aurorasdeco.recipe;

import com.google.gson.JsonObject;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeSerializer;

/**
 * Replaces QSL's {@code QuiltRecipeSerializer} (a bare {@code RecipeSerializer} extension adding
 * {@code toJson}, for dumping dynamically-generated recipes back out as JSON -- no Fabric equivalent
 * needed, this is just the same interface without the Quilt dependency).
 *
 * @param <T> the recipe type
 */
public interface JsonSerializableRecipeSerializer<T extends Recipe<?>> extends RecipeSerializer<T> {
	/**
	 * Serializes the recipe to JSON.
	 *
	 * @param recipe the recipe
	 * @return the serialized recipe
	 */
	JsonObject toJson(T recipe);
}
