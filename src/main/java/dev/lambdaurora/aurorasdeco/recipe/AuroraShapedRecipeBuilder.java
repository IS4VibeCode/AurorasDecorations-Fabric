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
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;

/**
 * Replaces QSL's {@code ShapedRecipeBuilder} -- same shape, no Fabric equivalent needed
 * (java/CLAUDE.md §3/Task #19).
 */
public class AuroraShapedRecipeBuilder extends AuroraRecipeBuilder<AuroraShapedRecipeBuilder, ShapedRecipe> {
	private final String[] pattern;
	private final int width;
	private final int height;
	private final Char2ObjectMap<Ingredient> ingredients = new Char2ObjectOpenHashMap<>();
	private CraftingRecipeCategory category = CraftingRecipeCategory.MISC;

	public AuroraShapedRecipeBuilder(String... pattern) {
		this.pattern = pattern;
		this.width = pattern[0].length();
		this.height = pattern.length;
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
	public ShapedRecipe build(Identifier id, String group) {
		this.checkOutputItem();

		var flattened = DefaultedList.<Ingredient>ofSize(this.width * this.height, Ingredient.EMPTY);
		for (int y = 0; y < this.height; y++) {
			for (int x = 0; x < this.width; x++) {
				char key = this.pattern[y].charAt(x);
				var ingredient = this.ingredients.get(key);
				if (ingredient == null) {
					throw new IllegalStateException("The pattern key '" + key + "' has no assigned ingredient.");
				}
				flattened.set(y * this.width + x, ingredient);
			}
		}

		return new ShapedRecipe(id, group, this.category, this.width, this.height, flattened, this.output);
	}
}
