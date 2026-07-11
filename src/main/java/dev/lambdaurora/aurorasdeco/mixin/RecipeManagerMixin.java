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

package dev.lambdaurora.aurorasdeco.mixin;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import dev.lambdaurora.aurorasdeco.resource.datagen.RecipeDatagen;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.recipe.RecipeType;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Map;

/**
 * Restored from the mod's own pre-Quilt Fabric history (git {@code 6a16db7~1}, written for MC
 * 1.18.2) -- this is exactly how the dynamically-generated recipes got injected before QSL's
 * {@code RecipeManagerHelper.registerStaticRecipe} replaced it, and Fabric has no equivalent
 * (confirmed: {@code fabric-recipe-api-v1} at 1.20.1 only has {@code CustomIngredient}-related
 * classes, no runtime static-recipe-registration helper -- java/CLAUDE.md §3/Task #19).
 * <p>
 * The injection target ({@code Map.entrySet()}, ordinal 1, inside {@code RecipeManager.apply()}) was
 * verified against the real 1.20.1 bytecode before restoring this, not assumed to still match just
 * because it worked at 1.18.2: {@code apply()} still calls {@code entrySet()} exactly twice, and the
 * second call is on the same {@code Map<RecipeType<?>, ImmutableMap.Builder<Identifier, Recipe<?>>>}
 * local this mixin expects to capture, at the same point (right after all JSON-sourced recipes have
 * been parsed and organized by type, right before the final immutable structures get built).
 */
@Mixin(RecipeManager.class)
public class RecipeManagerMixin {
	@Inject(
			method = "apply",
			at = @At(value = "INVOKE", target = "Ljava/util/Map;entrySet()Ljava/util/Set;", ordinal = 1),
			locals = LocalCapture.CAPTURE_FAILHARD
	)
	private void onReload(Map<Identifier, JsonElement> map, ResourceManager resourceManager, Profiler profiler,
	                      CallbackInfo ci, Map<RecipeType<?>, ImmutableMap.Builder<Identifier, Recipe<?>>> builderMap) {
		RecipeDatagen.applyRecipes(map, builderMap);
	}
}
