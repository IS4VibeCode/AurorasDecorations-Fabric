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

import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.ShapelessRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;

import java.util.HashSet;
import java.util.Set;

/**
 * Replaces QSL's {@code ShapelessRecipeBuilder} -- same shape, no Fabric equivalent needed
 * (java/CLAUDE.md §3/Task #19).
 */
public class AuroraShapelessRecipeBuilder extends AuroraRecipeBuilder<AuroraShapelessRecipeBuilder, ShapelessRecipe> {
	private final Set<Ingredient> ingredients = new HashSet<>();
	private CraftingRecipeCategory category = CraftingRecipeCategory.MISC;

	public AuroraShapelessRecipeBuilder(ItemStack output) {
		this.output = output;
	}

	public AuroraShapelessRecipeBuilder ingredient(Ingredient ingredient) {
		this.ingredients.add(ingredient);
		return this;
	}

	public AuroraShapelessRecipeBuilder ingredient(ItemConvertible... items) {
		return this.ingredient(Ingredient.ofItems(items));
	}

	public AuroraShapelessRecipeBuilder ingredient(TagKey<Item> tag) {
		return this.ingredient(Ingredient.fromTag(tag));
	}

	public AuroraShapelessRecipeBuilder ingredient(ItemStack... stacks) {
		return this.ingredient(Ingredient.ofStacks(stacks));
	}

	public AuroraShapelessRecipeBuilder category(CraftingRecipeCategory category) {
		this.category = category;
		return this;
	}

	@Override
	public ShapelessRecipe build(Identifier id, String group) {
		this.checkOutputItem();

		if (this.ingredients.isEmpty()) throw new IllegalStateException("Cannot build a recipe without ingredients.");

		var ingredients = DefaultedList.<Ingredient>ofSize(this.ingredients.size(), Ingredient.EMPTY);
		int i = 0;
		for (var ingredient : this.ingredients) {
			ingredients.set(i, ingredient);
			i++;
		}

		return new ShapelessRecipe(id, group, this.category, this.output, ingredients);
	}
}
