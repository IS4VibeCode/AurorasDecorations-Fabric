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

package dev.lambdaurora.aurorasdeco.resource.datagen;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonSyntaxException;
import dev.lambdaurora.aurorasdeco.AurorasDeco;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementCriterion;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementRequirements;
import net.minecraft.advancement.AdvancementRewards;
import net.minecraft.advancement.criterion.InventoryChangedCriterion;
import net.minecraft.advancement.criterion.RecipeUnlockedCriterion;
import net.minecraft.item.Item;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public final class AdvancementDatagen {
	private static final Map<Identifier, Supplier<Advancement.Builder>> ADVANCEMENT_BUILDERS = new Object2ObjectOpenHashMap<>();
	private static final Map<Identifier, Advancement.Builder> ADVANCEMENTS = new Object2ObjectOpenHashMap<>();
	private static final Pattern MISSING_TAG_REGEX = Pattern.compile("Unknown item tag '([a-z0-9_.-]+:[a-z0-9/._-]+)'");

	private AdvancementDatagen() {
		throw new UnsupportedOperationException("AdvancementDatagen only contains static definitions.");
	}

	/**
	 * 1.21.1 update: {@code ServerAdvancementLoader.apply()}'s per-JSON-entry parsing moved into a
	 * private lambda invoked via {@code Map.forEach}, so there is no longer an inline
	 * {@code Map<Identifier, Advancement.Builder>} local to capture mid-loop the way the old mixin did
	 * (re-verified against the real bytecode, not assumed). Instead {@code ServerAdvancementLoaderMixin}
	 * now redirects the single {@code ImmutableMap.Builder<Identifier, AdvancementEntry>.buildOrThrow()}
	 * call at the very end of {@code apply()} -- this method receives that still-open builder and adds
	 * our own entries (built via {@link Advancement.Builder#build(Identifier)}, which now needs the id
	 * passed in explicitly since recipes/advancements no longer carry it themselves) before the vanilla
	 * caller finalizes it.
	 */
	public static void applyAdvancements(ImmutableMap.Builder<Identifier, AdvancementEntry> builder) {
		AurorasDeco.debug("Applying advancement injection...");

		if (!ADVANCEMENT_BUILDERS.isEmpty()) {
			AurorasDeco.debug("Building {} advancements...", ADVANCEMENT_BUILDERS.size());
			var it = ADVANCEMENT_BUILDERS.entrySet().iterator();

			while (it.hasNext()) {
				var advancementBuilder = it.next();

				try {
					ADVANCEMENTS.put(advancementBuilder.getKey(), advancementBuilder.getValue().get());
					it.remove();
				} catch (JsonSyntaxException e) {
					var matcher = MISSING_TAG_REGEX.matcher(e.getMessage());

					if (matcher.find()) {
						var badTag = Identifier.of(matcher.group(1));
						AurorasDeco.error("Could not build advancement {} due to a missing item tag {}. " +
										"This probably means the mod {} is very likely to break Vanilla's expectations! Please report this issue!",
								advancementBuilder.getKey(), badTag, badTag.getNamespace());
					} else {
						throw e;
					}
				}
			}
		}

		ADVANCEMENTS.forEach((identifier, task) -> builder.put(identifier, task.build(identifier)));
	}

	public static Supplier<Advancement.Builder> register(Identifier id, Supplier<Advancement.Builder> advancement) {
		ADVANCEMENT_BUILDERS.put(id, advancement);
		return advancement;
	}

	public static Advancement.Builder simpleRecipeUnlock(Identifier id, Recipe<?> recipe) {
		var advancement = Advancement.Builder.create();

		advancement.parent(Identifier.of("recipes/root"));
		advancement.rewards(AdvancementRewards.Builder.recipe(id));
		advancement.criteriaMerger(AdvancementRequirements.CriterionMerger.OR);
		advancement.criterion("has_self", InventoryChangedCriterion.Conditions.items(recipe.createIcon().getItem()));
		advancement.criterion("has_the_recipe", RecipeUnlockedCriterion.create(id));

		int i = 0;
		for (var ingredient : recipe.getIngredients()) {
			if (ingredient.isEmpty())
				continue;
			advancement.criterion("has_" + i, inventoryChangedCriterion(ingredient));
			i++;
		}

		return advancement;
	}

	public static AdvancementCriterion<InventoryChangedCriterion.Conditions> inventoryChangedCriterion(Ingredient item) {
		var items = new java.util.LinkedHashSet<Item>();
		for (var stack : item.getMatchingStacks()) {
			items.add(stack.getItem());
		}
		return InventoryChangedCriterion.Conditions.items(items.toArray(new Item[0]));
	}
}
