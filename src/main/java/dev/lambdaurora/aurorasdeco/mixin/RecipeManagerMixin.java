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
import com.google.common.collect.Multimap;
import com.google.gson.JsonElement;
import dev.lambdaurora.aurorasdeco.resource.datagen.RecipeDatagen;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.recipe.RecipeType;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/**
 * Restored from the mod's own pre-Quilt Fabric history (git {@code 6a16db7~1}, written for MC
 * 1.18.2) -- this is exactly how the dynamically-generated recipes got injected before QSL's
 * {@code RecipeManagerHelper.registerStaticRecipe} replaced it, and Fabric has no equivalent
 * (confirmed: {@code fabric-recipe-api-v1} at 1.20.1 only has {@code CustomIngredient}-related
 * classes, no runtime static-recipe-registration helper -- java/CLAUDE.md §3/Task #19).
 * <p>
 * 1.21.1 re-verification: {@code RecipeManager.apply()}'s real bytecode was re-disassembled and a
 * mid-method local-capture injection (right before the vanilla method's own
 * {@code ImmutableMultimap.Builder.build()} call) was confirmed correct against the plain Yarn-mapped
 * game jar -- but crashed for real under Connector when actually opening a world
 * ({@code SugarApplicationException: Invalid implicit variable discriminator: Found 0 candidate
 * variables but exactly 1 is required}), meaning whatever Connector's mixin adapter does to bridge
 * this onto the real Mojang-mapped target class doesn't preserve that local the same way. Rather than
 * chase why (same "don't trust fragile mid-method local capture" lesson as {@code LivingEntityMixin}),
 * this instead injects at {@code TAIL} -- no locals needed -- and reads/replaces the two now-fully-
 * populated {@code RecipeManager} fields directly via {@code @Shadow}/{@code @Mutable}. The end result
 * is identical: the same dynamically-generated recipes end up merged into the same two structures,
 * just assembled after vanilla's own pass finishes instead of alongside it.
 */
@Mixin(RecipeManager.class)
public abstract class RecipeManagerMixin {
	@Shadow
	@Mutable
	private Multimap<RecipeType<?>, RecipeEntry<?>> recipesByType;

	@Shadow
	@Mutable
	private Map<Identifier, RecipeEntry<?>> recipesById;

	@Inject(
			method = "apply(Ljava/util/Map;Lnet/minecraft/resource/ResourceManager;Lnet/minecraft/util/profiler/Profiler;)V",
			at = @At("TAIL")
	)
	private void onReload(Map<Identifier, JsonElement> map, ResourceManager resourceManager, Profiler profiler,
			CallbackInfo ci) {
		var recipesByType = ImmutableMultimap.<RecipeType<?>, RecipeEntry<?>>builder().putAll(this.recipesByType);
		var recipesById = ImmutableMap.<Identifier, RecipeEntry<?>>builder().putAll(this.recipesById);

		RecipeDatagen.applyRecipes(map, recipesByType, recipesById);

		this.recipesByType = recipesByType.build();
		this.recipesById = recipesById.build();
	}
}
