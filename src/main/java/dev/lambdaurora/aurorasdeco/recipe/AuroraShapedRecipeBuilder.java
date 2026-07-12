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

import it.unimi.dsi.fastutil.chars.Char2ObjectMap;
import it.unimi.dsi.fastutil.chars.Char2ObjectOpenHashMap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RawShapedRecipe;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.registry.tag.TagKey;

/**
 * Replaces QSL's {@code ShapedRecipeBuilder} -- same shape, no Fabric equivalent needed
 * (java/CLAUDE.md §3/Task #19).
 */
public class AuroraShapedRecipeBuilder extends AuroraRecipeBuilder<AuroraShapedRecipeBuilder, ShapedRecipe> {
	private final String[] pattern;
	private final Char2ObjectMap<Ingredient> ingredients = new Char2ObjectOpenHashMap<>();
	private CraftingRecipeCategory category = CraftingRecipeCategory.MISC;

	public AuroraShapedRecipeBuilder(String... pattern) {
		this.pattern = pattern;
		this.ingredients.put(' ', Ingredient.EMPTY);
	}

	public AuroraShapedRecipeBuilder ingredient(char key, Ingredient ingredient) {
		boolean success = false;

		for (String line : this.pattern) {
			for (int i = 0; i < line.length(); i++) {
				if (line.charAt(i) == key) {
					this.ingredients.put(key, ingredient);
					success = true;
					break;
				}
			}

			if (success) break;
		}

		if (!success) {
			throw new IllegalArgumentException("The pattern key '" + key + "' doesn't exist in the given pattern.");
		}

		return this;
	}

	public AuroraShapedRecipeBuilder ingredient(char key, ItemConvertible... items) {
		return this.ingredient(key, Ingredient.ofItems(items));
	}

	public AuroraShapedRecipeBuilder ingredient(char key, TagKey<Item> tag) {
		return this.ingredient(key, Ingredient.fromTag(tag));
	}

	public AuroraShapedRecipeBuilder ingredient(char key, ItemStack... stacks) {
		return this.ingredient(key, Ingredient.ofStacks(stacks));
	}

	public AuroraShapedRecipeBuilder category(CraftingRecipeCategory category) {
		this.category = category;
		return this;
	}

	@Override
	public ShapedRecipe build(String group) {
		this.checkOutputItem();

		var raw = RawShapedRecipe.create(this.ingredients, this.pattern);
		return new ShapedRecipe(group, this.category, raw, this.output);
	}
}
