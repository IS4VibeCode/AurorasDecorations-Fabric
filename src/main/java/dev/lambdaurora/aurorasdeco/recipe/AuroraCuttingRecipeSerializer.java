/*
 * Copyright (c) 2021 - 2024 LambdAurora <email@lambdaurora.dev>
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

import net.minecraft.recipe.CuttingRecipe;

/**
 * Exposes the constructor of {@link CuttingRecipe.Serializer}, which vanilla keeps {@code protected}
 * (it's only ever instantiated from within {@code net.minecraft.recipe} itself, e.g. for
 * {@code StonecuttingRecipe}). Subclassing from a different package is enough to call {@code super},
 * since {@code protected} grants access to subclasses regardless of package -- this reuses vanilla's
 * whole {@link com.mojang.serialization.MapCodec}/{@link net.minecraft.network.codec.PacketCodec}
 * pair (group + ingredient + result) instead of hand-writing one, exactly like {@code StonecuttingRecipe}
 * itself does.
 *
 * @param <T> the recipe type
 */
public final class AuroraCuttingRecipeSerializer<T extends CuttingRecipe> extends CuttingRecipe.Serializer<T> {
	public AuroraCuttingRecipeSerializer(CuttingRecipe.RecipeFactory<T> recipeFactory) {
		super(recipeFactory);
	}
}
