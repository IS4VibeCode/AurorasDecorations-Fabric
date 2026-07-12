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
import net.minecraft.recipe.Recipe;

import java.util.Objects;

/**
 * Replaces QSL's {@code org.quiltmc.qsl.recipe.api.builder.RecipeBuilder} -- same shape, no Fabric
 * equivalent needed since it never actually depended on Quilt itself, only on vanilla classes
 * (java/CLAUDE.md §3/Task #19).
 *
 * @param <SELF>   the type of the recipe builder
 * @param <RESULT> the type of the recipe
 */
public abstract class AuroraRecipeBuilder<SELF extends AuroraRecipeBuilder<SELF, RESULT>, RESULT extends Recipe<?>> {
	protected ItemStack output;

	@SuppressWarnings("unchecked")
	public SELF output(ItemStack stack) {
		this.output = stack;
		return (SELF) this;
	}

	protected void checkOutputItem() {
		Objects.requireNonNull(this.output, "The output stack cannot be null.");
	}

	public abstract RESULT build(String group);
}
