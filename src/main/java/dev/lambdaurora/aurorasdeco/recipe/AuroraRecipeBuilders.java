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

import net.minecraft.item.ItemStack;

/**
 * Replaces QSL's {@code VanillaRecipeBuilders} -- only the two factory methods this mod actually
 * used ({@code shapedRecipe}/{@code shapelessRecipe}) are ported, the cooking/stonecutting recipe
 * builders it also had were never called anywhere in this codebase (java/CLAUDE.md §3/Task #19).
 */
public final class AuroraRecipeBuilders {
	private AuroraRecipeBuilders() {
		throw new UnsupportedOperationException("AuroraRecipeBuilders only contains static definitions.");
	}

	public static AuroraShapedRecipeBuilder shapedRecipe(String... pattern) {
		return new AuroraShapedRecipeBuilder(pattern);
	}

	public static AuroraShapelessRecipeBuilder shapelessRecipe(ItemStack output) {
		return new AuroraShapelessRecipeBuilder(output);
	}
}
