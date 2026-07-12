/*
 * Copyright (c) 2021 LambdAurora <email@lambdaurora.dev>
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

import dev.lambdaurora.aurorasdeco.registry.AurorasDecoRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CuttingRecipe;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.input.SingleStackRecipeInput;
import net.minecraft.world.World;

/**
 * Represents woodcutting recipes.
 *
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
public final class WoodcuttingRecipe extends CuttingRecipe {
	public WoodcuttingRecipe(String group, Ingredient input, ItemStack output) {
		super(AurorasDecoRegistry.WOODCUTTING_RECIPE_TYPE, AurorasDecoRegistry.WOODCUTTING_RECIPE_SERIALIZER,
				group, input, output);
	}

	@Override
	public boolean matches(SingleStackRecipeInput input, World world) {
		return this.ingredient.test(input.item());
	}

	@Override
	public ItemStack createIcon() {
		return new ItemStack(AurorasDecoRegistry.SAWMILL_BLOCK);
	}
}
