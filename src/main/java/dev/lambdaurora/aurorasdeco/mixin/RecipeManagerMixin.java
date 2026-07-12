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
import com.google.common.collect.ImmutableMultimap;
import com.google.gson.JsonElement;
import dev.lambdaurora.aurorasdeco.resource.datagen.RecipeDatagen;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.RegistryOps;
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
 * 1.21.1 re-verification: {@code RecipeManager.apply()}'s real bytecode was re-disassembled rather
 * than assuming the 1.20.1 injection point still holds. It no longer calls {@code Map.entrySet()}
 * twice -- there is now a single combined loop over the JSON map that builds both the
 * {@code ImmutableMultimap.Builder<RecipeType<?>, RecipeEntry<?>>} (by-type) and
 * {@code ImmutableMap.Builder<Identifier, RecipeEntry<?>>} (by-id) structures at once (recipes no
 * longer carry their own id -- {@link RecipeEntry} is now the separate id+recipe pair created inside
 * that loop). The injection point that matches the old intent ("right after all JSON-sourced recipes
 * have been organized, right before the final immutable structures get built") is now the single
 * {@code ImmutableMultimap.Builder.build()} call that follows the loop -- unique in the method, so no
 * ordinal is needed. At that point both builders are still open and both are captured.
 */
@Mixin(RecipeManager.class)
public class RecipeManagerMixin {
	@Inject(
			method = "apply(Ljava/util/Map;Lnet/minecraft/resource/ResourceManager;Lnet/minecraft/util/profiler/Profiler;)V",
			at = @At(value = "INVOKE",
					target = "Lcom/google/common/collect/ImmutableMultimap$Builder;build()Lcom/google/common/collect/ImmutableMultimap;"),
			locals = LocalCapture.CAPTURE_FAILHARD
	)
	private void onReload(Map<Identifier, JsonElement> map, ResourceManager resourceManager, Profiler profiler,
			CallbackInfo ci,
			ImmutableMultimap.Builder<RecipeType<?>, RecipeEntry<?>> recipesByType,
			ImmutableMap.Builder<Identifier, RecipeEntry<?>> recipesById,
			RegistryOps<JsonElement> registryOps) {
		RecipeDatagen.applyRecipes(map, recipesByType, recipesById);
	}
}
